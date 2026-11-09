package me.weishu.kernelsu.ui.screen.module

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

internal val moduleWallpaperPersistenceLock = Any()

/** Read, update and retire images as one transaction, including background imports. */
internal fun updateModuleCardWallpaperSnapshot(
    context: Context,
    moduleId: String,
    transform: (ModuleCardWallpaperSnapshot) -> ModuleCardWallpaperSnapshot,
): ModuleCardWallpaperSnapshot? = updateModuleCardWallpaperSnapshot(
    prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE),
    moduleId = moduleId,
    transform = transform,
    onReplaced = { previous, next -> releaseRemovedModuleWallpaperEntries(context, previous, next) },
)

internal fun updateModuleCardWallpaperSnapshot(
    prefs: SharedPreferences,
    moduleId: String,
    transform: (ModuleCardWallpaperSnapshot) -> ModuleCardWallpaperSnapshot,
    onReplaced: (ModuleCardWallpaperSnapshot, ModuleCardWallpaperSnapshot) -> Unit = { _, _ -> },
): ModuleCardWallpaperSnapshot? = synchronized(moduleWallpaperPersistenceLock) {
    val previous = readModuleCardWallpaperSnapshot(prefs, moduleId)
    val next = transform(previous).normalized()
    if (previous == next) return@synchronized previous
    if (!prefs.edit().putModuleCardWallpaperSnapshot(moduleId, next).commit()) {
        // SharedPreferences updates memory even when its disk write fails.
        prefs.edit().putModuleCardWallpaperSnapshot(moduleId, previous).commit()
        return@synchronized null
    }
    onReplaced(previous, next)
    next
}

/** A delayed crop/slider callback must not replace a different, newly selected image. */
internal fun updateModuleWallpaperEntry(
    snapshot: ModuleCardWallpaperSnapshot,
    variant: ModuleWallpaperVariant,
    uriString: String,
    transform: (ModuleCardWallpaperEntry) -> ModuleCardWallpaperEntry,
): ModuleCardWallpaperSnapshot {
    val collection = snapshot.collection(variant)
    return snapshot.withCollection(
        variant,
        collection.copy(entries = collection.entries.map { entry ->
            if (entry.uriString == uriString) transform(entry) else entry
        }),
    )
}

/** Older configurations can share an image with another module or a saved slot. */
internal fun unreferencedModuleWallpaperUris(
    prefs: SharedPreferences,
    candidates: Set<String>,
): Set<String> {
    if (candidates.isEmpty()) return emptySet()
    val retained = mutableSetOf<String>()
    fun collect(value: Any?) {
        when (value) {
            is JSONObject -> value.keys().forEach { key ->
                if (key == "uri") retained += value.optString(key)
                else collect(value.opt(key))
            }
            is JSONArray -> for (index in 0 until value.length()) collect(value.opt(index))
        }
    }
    prefs.all.forEach { (key, value) ->
        if (value !is String || value.isBlank()) return@forEach
        val currentImage = key.startsWith("module_card_wallpaper_") && key.endsWith("_uri")
        val collection = key.startsWith("module_card_wallpaper_") && key.endsWith("_entries")
        val savedSlot = key.startsWith("module_wallpaper_saved_slot_")
        if (currentImage) retained += value
        if (collection || savedSlot) {
            try {
                val raw = value.trimStart()
                collect(if (raw.startsWith("[")) JSONArray(raw) else JSONObject(raw))
            } catch (_: Exception) {
                // Keep files if a stored configuration cannot be inspected safely.
                return emptySet()
            }
        }
    }
    return candidates - retained
}
