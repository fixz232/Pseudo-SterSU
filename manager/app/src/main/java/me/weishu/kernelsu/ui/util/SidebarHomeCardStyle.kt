package me.weishu.kernelsu.ui.util

import org.json.JSONObject

internal enum class SidebarHomeCardId(val value: String) {
    MaterialStatus("material_status"), Superuser("superuser"), Module("module"),
    MaterialSecurity("material_security"), MaterialInfo("material_info"),
    StatusHero("status_hero"), StatusMetrics("status_metrics"),
    StatusSecurity("status_security"), StatusVersions("status_versions"),
    Support("support"), Learn("learn");
}

internal fun sidebarHomeCardIds(layout: SidebarHomeLayout): List<SidebarHomeCardId> = when (layout) {
    SidebarHomeLayout.Material -> listOf(
        SidebarHomeCardId.MaterialStatus, SidebarHomeCardId.Superuser, SidebarHomeCardId.Module,
        SidebarHomeCardId.MaterialSecurity, SidebarHomeCardId.MaterialInfo,
    )
    SidebarHomeLayout.StatusCards -> listOf(
        SidebarHomeCardId.StatusHero, SidebarHomeCardId.StatusMetrics,
        SidebarHomeCardId.StatusSecurity, SidebarHomeCardId.StatusVersions,
    )
} + listOf(SidebarHomeCardId.Support, SidebarHomeCardId.Learn)

enum class SidebarCardMaterial(val value: String) {
    Default("default"), LiquidGlass("liquid"), NeumorphicGlass("neumorphic");
    companion object {
        fun fromValue(value: String?): SidebarCardMaterial = entries.firstOrNull { it.value == value } ?: Default
    }
}

enum class SidebarCardGradient(val value: String) {
    Solid("solid"), Vertical("vertical"), Horizontal("horizontal"),
    Diagonal("diagonal"), ReverseDiagonal("reverse_diagonal"), Radial("radial"), Sweep("sweep");
    companion object {
        fun fromValue(value: String?): SidebarCardGradient = entries.firstOrNull { it.value == value } ?: Vertical
    }
}

/** Each card/day-night pair is independent from the rail and other interface styles. */
data class SidebarHomeCardStyle(
    val material: SidebarCardMaterial = SidebarCardMaterial.Default,
    val gradient: SidebarCardGradient = SidebarCardGradient.Vertical,
    val start: Long = 0xFFB9D4FF,
    val middle: Long = 0xFFE5EEFB,
    val end: Long = 0xFFF7F9FC,
    val content: Long = 0xFF121820,
    val highlight: Long = 0xFFFFFFFF,
    val tint: Float = 0.72f,
    val imageUri: String? = null,
    val crop: CustomWallpaperCrop = DEFAULT_CUSTOM_WALLPAPER_CROP,
    val inheritImage: Boolean = true,
) {
    fun normalized(dark: Boolean): SidebarHomeCardStyle {
        val fallback = defaultSidebarHomeCardStyle(dark)
        fun Long.safe(default: Long) = takeIf { it in 0xFF000000L..0xFFFFFFFFL } ?: default
        return copy(
            start = start.safe(fallback.start), middle = middle.safe(fallback.middle), end = end.safe(fallback.end),
            content = content.safe(fallback.content), highlight = highlight.safe(fallback.highlight),
            tint = if (tint.isFinite()) tint.coerceIn(0.15f, 1f) else fallback.tint,
            imageUri = imageUri?.trim()?.takeIf {
                it.length <= 4096 && (it.startsWith("content://") || it.startsWith("file://"))
            },
            crop = sanitizeCustomWallpaperCrop(crop),
        )
    }
}

fun defaultSidebarHomeCardStyle(dark: Boolean): SidebarHomeCardStyle = if (dark) {
    SidebarHomeCardStyle(start = 0xFF233C60, middle = 0xFF253140, end = 0xFF191F28, content = 0xFFF5F7FC, highlight = 0xFFBDCEEA)
} else SidebarHomeCardStyle()

data class SidebarHomeCards(val overrides: Map<String, SidebarHomeCardStyle> = emptyMap()) {
    internal fun style(id: SidebarHomeCardId, dark: Boolean): SidebarHomeCardStyle =
        overrides[key(id, dark)]?.normalized(dark) ?: defaultSidebarHomeCardStyle(dark)

    internal fun withStyle(id: SidebarHomeCardId, dark: Boolean, style: SidebarHomeCardStyle): SidebarHomeCards =
        copy(overrides = overrides + (key(id, dark) to style.normalized(dark)))

    internal fun reset(id: SidebarHomeCardId, dark: Boolean): SidebarHomeCards = copy(overrides = overrides - key(id, dark))

    /** Applying a finish must never replace another card's picture or crop. */
    internal fun applyFinish(layout: SidebarHomeLayout, dark: Boolean, finish: SidebarHomeCardStyle): SidebarHomeCards {
        var result = this
        sidebarHomeCardIds(layout).forEach { id ->
            val previous = style(id, dark)
            result = result.withStyle(id, dark, finish.copy(imageUri = previous.imageUri, crop = previous.crop, inheritImage = previous.inheritImage))
        }
        return result
    }

    fun normalized(): SidebarHomeCards = SidebarHomeCards(buildMap {
        SidebarHomeCardId.entries.forEach { id ->
            listOf(false, true).forEach { dark ->
                overrides[key(id, dark)]?.let { put(key(id, dark), it.normalized(dark)) }
            }
        }
    })

    fun needsBackdrop(dark: Boolean): Boolean = overrides.any { (key, style) ->
        key.endsWith(if (dark) ".dark" else ".light") && style.material != SidebarCardMaterial.Default
    }
}

private fun key(id: SidebarHomeCardId, dark: Boolean) = "${id.value}.${if (dark) "dark" else "light"}"

internal fun encodeSidebarHomeCards(cards: SidebarHomeCards): String {
    val root = JSONObject()
    cards.normalized().overrides.forEach { (key, value) ->
        root.put(key, JSONObject().apply {
            put("material", value.material.value); put("gradient", value.gradient.value)
            put("start", value.start); put("middle", value.middle); put("end", value.end)
            put("content", value.content); put("highlight", value.highlight); put("tint", value.tint)
            put("image", value.imageUri); put("inheritImage", value.inheritImage)
            put("left", value.crop.left); put("top", value.crop.top)
            put("right", value.crop.right); put("bottom", value.crop.bottom)
        })
    }
    return root.toString()
}

internal fun decodeSidebarHomeCards(raw: String?): SidebarHomeCards {
    if (raw.isNullOrBlank() || raw.length > 128 * 1024) return SidebarHomeCards()
    return runCatching {
        val root = JSONObject(raw)
        SidebarHomeCards(buildMap {
            SidebarHomeCardId.entries.forEach { id ->
                listOf(false, true).forEach theme@{ dark ->
                    val key = key(id, dark)
                    val json = root.optJSONObject(key) ?: return@theme
                    val fallback = defaultSidebarHomeCardStyle(dark)
                    put(key, SidebarHomeCardStyle(
                        material = SidebarCardMaterial.fromValue(json.optString("material")),
                        gradient = SidebarCardGradient.fromValue(json.optString("gradient")),
                        start = json.optLong("start", fallback.start), middle = json.optLong("middle", fallback.middle),
                        end = json.optLong("end", fallback.end), content = json.optLong("content", fallback.content),
                        highlight = json.optLong("highlight", fallback.highlight), tint = json.optDouble("tint", fallback.tint.toDouble()).toFloat(),
                        imageUri = json.optString("image").takeIf { it.isNotBlank() },
                        inheritImage = json.optBoolean("inheritImage", true),
                        crop = CustomWallpaperCrop(
                            json.optDouble("left", fallback.crop.left.toDouble()).toFloat(), json.optDouble("top", fallback.crop.top.toDouble()).toFloat(),
                            json.optDouble("right", fallback.crop.right.toDouble()).toFloat(), json.optDouble("bottom", fallback.crop.bottom.toDouble()).toFloat(),
                        ),
                    ).normalized(dark))
                }
            }
        })
    }.getOrDefault(SidebarHomeCards())
}

internal const val SIDEBAR_HOME_CARDS_KEY = "sidebar_home_card_styles_v1"
