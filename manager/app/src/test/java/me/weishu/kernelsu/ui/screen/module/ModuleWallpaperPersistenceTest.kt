package me.weishu.kernelsu.ui.screen.module

import android.content.SharedPreferences
import java.io.File
import java.lang.reflect.Proxy
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import me.weishu.kernelsu.ui.util.CustomWallpaperCrop
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_WALLPAPER_CROP
import me.weishu.kernelsu.ui.util.MediaMotionStyle
import me.weishu.kernelsu.ui.util.MediaVisualSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleWallpaperPersistenceTest {
    @Test
    fun savingAndReloadingPreservesImagesCropsAndBothVariants() {
        val prefs = MemoryPreferences()
        val day = entry("day").copy(
            crop = CustomWallpaperCrop(0.2f, 0.3f, 0.8f, 0.9f),
            visualSettings = MediaVisualSettings(opacity = 0.7f, motionStyle = MediaMotionStyle.SlowPan),
            autoContrast = false,
        )
        val expected = ModuleCardWallpaperSnapshot(emptyList(), false)
            .withCollection(ModuleWallpaperVariant.Day, ModuleWallpaperCollection(entries = listOf(day)))
            .withCollection(
                ModuleWallpaperVariant.Night,
                ModuleWallpaperCollection(
                    entries = listOf(entry("night-1"), entry("night-2")),
                    carouselEnabled = true,
                    carouselOrder = ModuleWallpaperCarouselOrder.Random,
                    intervalMillis = 9_000L,
                    selectedIndex = 1,
                ),
            )

        assertTrue(prefs.value.edit().putModuleCardWallpaperSnapshot("demo", expected).commit())

        assertEquals(expected, readModuleCardWallpaperSnapshot(prefs.value, "demo"))
        assertTrue(readModuleCardWallpaperSnapshot(prefs.value, "another-module").allEntries().isEmpty())
        assertTrue(readModuleCardWallpaperSnapshot(prefs.value, kpmCardWallpaperId("demo")).allEntries().isEmpty())
    }

    @Test
    fun nightOnlyWallpaperSurvivesReloadAndClearingDoesNotReviveLegacyImage() {
        val prefs = MemoryPreferences()
        val both = ModuleCardWallpaperSnapshot(listOf(entry("day")), false, nightEntries = listOf(entry("night")))
        prefs.value.edit().putModuleCardWallpaperSnapshot("demo", both).commit()
        val nightOnly = both.withCollection(ModuleWallpaperVariant.Day, ModuleWallpaperCollection())
        prefs.value.edit().putModuleCardWallpaperSnapshot("demo", nightOnly).commit()
        assertEquals(nightOnly, readModuleCardWallpaperSnapshot(prefs.value, "demo"))

        prefs.value.edit().putModuleCardWallpaperSnapshot("demo", ModuleCardWallpaperSnapshot(emptyList(), false)).commit()
        assertTrue(readModuleCardWallpaperSnapshot(prefs.value, "demo").allEntries().isEmpty())
        assertTrue(prefs.values.isEmpty())
    }

    @Test
    fun legacyImageCollectionsStillReload() {
        val prefs = MemoryPreferences()
        prefs.values["module_card_wallpaper_demo_entries"] = """[{"uri":"legacy"}]"""
        assertEquals(listOf(entry("legacy")), readModuleCardWallpaperSnapshot(prefs.value, "demo").entries)
    }

    @Test
    fun editorAndCardChooseTheSameActiveDayNightCollection() {
        val day = ModuleCardWallpaperSnapshot(listOf(entry("day")), false)
        val both = day.copy(nightEntries = listOf(entry("night")))
        assertEquals(ModuleWallpaperVariant.Day, activeModuleWallpaperVariant(false, both))
        assertEquals(ModuleWallpaperVariant.Night, activeModuleWallpaperVariant(true, both))
        assertEquals(ModuleWallpaperVariant.Day, activeModuleWallpaperVariant(true, day))
    }

    @Test
    fun importingCommitsBeforeReturningToTheCancellableUiScope() {
        val source = File("src/main/java/me/weishu/kernelsu/ui/screen/module/ModuleCardWallpaper.kt").readText()
        val operation = source.substringAfter("internal fun importAndSaveModuleWallpapers(")
            .substringBefore("internal fun duplicateModuleWallpaperSnapshotForTarget(")
        assertTrue(operation.contains("importModuleWallpaperEntries(context, moduleId, variant, uris).getOrThrow()"))
        assertTrue(operation.contains("updateModuleCardWallpaperSnapshot(context, moduleId) { latest ->"))
        assertTrue(operation.contains("checkNotNull("))
        assertTrue(operation.contains("releaseModuleWallpaperSnapshot(context, ModuleCardWallpaperSnapshot(imported, false))"))
        val editor = File("src/main/java/me/weishu/kernelsu/ui/screen/module/ModuleWallpaperEditorScreen.kt")
            .readText().replace("\r\n", "\n")
        val picker = editor.substringAfter("val imagePicker =").substringBefore("LaunchedEffect(Unit)")
        assertTrue(picker.contains("withContext(Dispatchers.IO) {\n                importAndSaveModuleWallpapers("))
        assertFalse(picker.contains("replaceModuleCardWallpaperSnapshot("))
    }

    @Test
    fun pickerRequestAndTargetVariantSurviveActivityRecreation() {
        val pager = File("src/main/java/me/weishu/kernelsu/ui/screen/module/ModuleScreen.kt").readText()
        val editor = File("src/main/java/me/weishu/kernelsu/ui/screen/module/ModuleWallpaperEditorScreen.kt").readText()
        assertTrue(pager.contains("wallpaperEditorRequest by rememberSaveable"))
        assertTrue(editor.contains("variant by rememberSaveable(moduleId)"))
        assertTrue(editor.contains("importMode by rememberSaveable(moduleId)"))
    }

    @Test
    fun severalModulesPreserveIndependentImagesAfterReload() {
        val prefs = MemoryPreferences()
        val ids = listOf("first", "second", "third", "fourth", kpmCardWallpaperId("first"))
        ids.forEach { id ->
            updateModuleCardWallpaperSnapshot(prefs.value, id, { latest ->
                latest.withCollection(ModuleWallpaperVariant.Day, ModuleWallpaperCollection(listOf(entry("$id-day"))))
            })
            updateModuleCardWallpaperSnapshot(prefs.value, id, { latest ->
                latest.withCollection(ModuleWallpaperVariant.Night, ModuleWallpaperCollection(listOf(entry("$id-night"))))
            })
        }
        val reloaded = MemoryPreferences().also { it.values.putAll(prefs.values) }
        ids.forEach { id ->
            val snapshot = readModuleCardWallpaperSnapshot(reloaded.value, id)
            assertEquals(listOf(entry("$id-day")), snapshot.entries)
            assertEquals(listOf(entry("$id-night")), snapshot.nightEntries)
        }
    }

    @Test
    fun delayedCropOfReplacedImageCannotResurrectOldImageOrDeleteNewImage() {
        val prefs = MemoryPreferences()
        val old = ModuleCardWallpaperSnapshot(listOf(entry("old")), false)
        prefs.value.edit().putModuleCardWallpaperSnapshot("first", old).commit()
        val replacement = old.copy(entries = listOf(entry("new")), nightEntries = listOf(entry("night")))
        prefs.value.edit().putModuleCardWallpaperSnapshot("first", replacement).commit()
        val retired = mutableListOf<String>()

        val saved = updateModuleCardWallpaperSnapshot(
            prefs.value,
            "first",
            transform = { latest ->
                updateModuleWallpaperEntry(latest, ModuleWallpaperVariant.Day, "old") {
                    it.copy(crop = CustomWallpaperCrop(0f, 0f, 1f, 1f))
                }
            },
            onReplaced = { previous, next ->
                retired += (previous.allEntries() - next.allEntries().toSet()).map { it.uriString }
            },
        )

        assertEquals(replacement, saved)
        assertEquals(replacement, readModuleCardWallpaperSnapshot(prefs.value, "first"))
        assertTrue(retired.isEmpty())
    }

    @Test
    fun editingExistingImageKeepsImagesAddedAfterEditorOpened() {
        val prefs = MemoryPreferences()
        val first = entry("first")
        val added = entry("added")
        prefs.value.edit().putModuleCardWallpaperSnapshot(
            "demo", ModuleCardWallpaperSnapshot(listOf(first, added), false, nightEntries = listOf(entry("night"))),
        ).commit()
        val crop = CustomWallpaperCrop(0.2f, 0.2f, 0.8f, 0.8f)
        updateModuleCardWallpaperSnapshot(prefs.value, "demo", { latest ->
            updateModuleWallpaperEntry(latest, ModuleWallpaperVariant.Day, first.uriString) { it.copy(crop = crop) }
        })
        val reloaded = readModuleCardWallpaperSnapshot(prefs.value, "demo")
        assertEquals(listOf(first.copy(crop = crop), added), reloaded.entries)
        assertEquals(listOf(entry("night")), reloaded.nightEntries)
    }

    @Test
    fun concurrentImageAddsDoNotOverwriteEachOther() {
        val prefs = MemoryPreferences()
        val executor = Executors.newFixedThreadPool(4)
        try {
            val saves = (0 until 24).map { index ->
                executor.submit {
                    updateModuleCardWallpaperSnapshot(prefs.value, "demo", { latest ->
                        val day = latest.collection(ModuleWallpaperVariant.Day)
                        latest.withCollection(ModuleWallpaperVariant.Day, day.copy(entries = day.entries + entry("image-$index")))
                    })
                }
            }
            saves.forEach { it.get(10, TimeUnit.SECONDS) }
            val saved = readModuleCardWallpaperSnapshot(prefs.value, "demo")
            assertEquals((0 until 24).map { "image-$it" }.toSet(), saved.entries.map { it.uriString }.toSet())
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun replacingOneModuleCannotDeleteImagesStillUsedByAnotherModuleOrSavedSlot() {
        val prefs = MemoryPreferences()
        prefs.value.edit().putModuleCardWallpaperSnapshot("first", ModuleCardWallpaperSnapshot(listOf(entry("shared")), false)).commit()
        prefs.value.edit().putModuleCardWallpaperSnapshot("second", ModuleCardWallpaperSnapshot(listOf(entry("shared")), false)).commit()
        prefs.values["module_wallpaper_saved_slot_first_0"] = """{"day":{"wallpapers":[{"uri":"slot-image"}]}}"""
        prefs.value.edit().putModuleCardWallpaperSnapshot("first", ModuleCardWallpaperSnapshot(listOf(entry("new")), false)).commit()
        assertEquals(setOf("unused"), unreferencedModuleWallpaperUris(prefs.value, setOf("shared", "slot-image", "unused")))

        prefs.value.edit().putModuleCardWallpaperSnapshot("second", ModuleCardWallpaperSnapshot(emptyList(), false)).commit()
        assertEquals(setOf("shared"), unreferencedModuleWallpaperUris(prefs.value, setOf("shared", "slot-image")))
    }

    @Test
    fun failedDiskWriteRestoresPreviousImagesAndDoesNotRetireThem() {
        val prefs = MemoryPreferences()
        val previous = ModuleCardWallpaperSnapshot(listOf(entry("previous")), false)
        prefs.value.edit().putModuleCardWallpaperSnapshot("demo", previous).commit()
        prefs.commitSucceeds = false
        var retired = false
        val saved = updateModuleCardWallpaperSnapshot(
            prefs.value,
            "demo",
            transform = { it.copy(entries = listOf(entry("failed-import"))) },
            onReplaced = { _, _ -> retired = true },
        )
        assertNull(saved)
        assertFalse(retired)
        assertEquals(previous, readModuleCardWallpaperSnapshot(prefs.value, "demo"))
    }

    private fun entry(id: String) = ModuleCardWallpaperEntry(id, DEFAULT_CUSTOM_WALLPAPER_CROP)

    private class MemoryPreferences {
        val values = mutableMapOf<String, Any>()
        var commitSucceeds = true
        val value: SharedPreferences = Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java),
        ) { _, method, args ->
            when (method.name) {
                "getAll" -> values.toMap()
                "getString", "getBoolean", "getFloat" -> values[args!![0]] ?: args[1]
                "edit" -> editor()
                else -> error("Unexpected preference call: ${method.name}")
            }
        } as SharedPreferences

        private fun editor(): SharedPreferences.Editor {
            val pending = mutableMapOf<String, Any?>()
            return Proxy.newProxyInstance(
                SharedPreferences.Editor::class.java.classLoader,
                arrayOf(SharedPreferences.Editor::class.java),
            ) { proxy, method, args ->
                when {
                    method.name.startsWith("put") -> {
                        pending[args!![0] as String] = args[1]
                        proxy
                    }
                    method.name == "remove" -> {
                        pending[args!![0] as String] = null
                        proxy
                    }
                    method.name == "commit" -> {
                        pending.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
                        commitSucceeds
                    }
                    else -> error("Unexpected editor call: ${method.name}")
                }
            } as SharedPreferences.Editor
        }
    }
}
