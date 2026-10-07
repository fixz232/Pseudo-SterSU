package me.weishu.kernelsu.ui.util

import org.json.JSONObject

enum class SidebarMaterial(val value: String) {
    LiquidGlass("liquid_glass"),
    NeumorphicGlass("neumorphic_glass"),
    Flat("flat");

    companion object {
        fun fromValue(value: String?, legacyGlassEnabled: Boolean = true): SidebarMaterial =
            entries.firstOrNull { it.value == value }
                ?: if (legacyGlassEnabled) LiquidGlass else Flat
    }
}

/** Opaque user colours; translucency belongs to the renderer, not to saved text colours. */
data class SidebarPalette(
    val background: Long,
    val backgroundEnd: Long,
    val selection: Long,
    val content: Long,
    val highlight: Long,
    val shade: Long = 0xFF000000,
    val tintAlpha: Float = 0.90f,
    val highlightStrength: Float = 1f,
    val reliefStrength: Float = 1f,
) {
    fun normalized(fallback: SidebarPalette): SidebarPalette = SidebarPalette(
        background.validOr(fallback.background),
        backgroundEnd.validOr(fallback.backgroundEnd),
        selection.validOr(fallback.selection),
        content.validOr(fallback.content),
        highlight.validOr(fallback.highlight),
        shade.validOr(fallback.shade),
        tintAlpha.validOr(fallback.tintAlpha, 0.75f..1f),
        highlightStrength.validOr(fallback.highlightStrength, 0f..1.5f),
        reliefStrength.validOr(fallback.reliefStrength, 0f..1.5f),
    )
}

private fun Long.validOr(fallback: Long): Long = takeIf { it in 0xFF000000L..0xFFFFFFFFL } ?: fallback
private fun Float.validOr(fallback: Float, range: ClosedFloatingPointRange<Float>): Float =
    takeIf(Float::isFinite)?.coerceIn(range) ?: fallback

fun defaultSidebarPalette(material: SidebarMaterial, dark: Boolean): SidebarPalette = when {
    material == SidebarMaterial.Flat && dark -> SidebarPalette(0xFF202020, 0xFF202020, 0xFF444444, 0xFFF5F5F5, 0xFF777777)
    material == SidebarMaterial.Flat -> SidebarPalette(0xFFEAEAEA, 0xFFEAEAEA, 0xFFCDCDCD, 0xFF161616, 0xFFFFFFFF)
    dark -> SidebarPalette(0xFF303030, 0xFF202020, 0xFF505050, 0xFFF5F5F5, 0xFFBDBDBD)
    else -> SidebarPalette(0xFFE5E5E5, 0xFFF7F7F7, 0xFFCDCDCD, 0xFF161616, 0xFFFFFFFF)
}

/** Separate slots for every material and day/night pair. Unknown slots are never retained. */
data class SidebarPalettes(val overrides: Map<String, SidebarPalette> = emptyMap()) {
    fun palette(material: SidebarMaterial, dark: Boolean): SidebarPalette {
        val fallback = defaultSidebarPalette(material, dark)
        return overrides[paletteKey(material, dark)]?.normalized(fallback) ?: fallback
    }

    fun withPalette(material: SidebarMaterial, dark: Boolean, palette: SidebarPalette): SidebarPalettes =
        copy(overrides = overrides + (paletteKey(material, dark) to palette.normalized(defaultSidebarPalette(material, dark))))

    fun reset(material: SidebarMaterial, dark: Boolean): SidebarPalettes =
        copy(overrides = overrides - paletteKey(material, dark))

    fun normalized(): SidebarPalettes {
        val known = buildMap {
            SidebarMaterial.entries.forEach { material ->
                listOf(false, true).forEach { dark ->
                    val key = paletteKey(material, dark)
                    overrides[key]?.let { put(key, it.normalized(defaultSidebarPalette(material, dark))) }
                }
            }
        }
        return copy(overrides = known)
    }
}

private fun paletteKey(material: SidebarMaterial, dark: Boolean): String =
    "${material.value}.${if (dark) "dark" else "light"}"

internal fun decodeSidebarPalettes(value: String?): SidebarPalettes {
    if (value.isNullOrBlank() || value.length > 8192) return SidebarPalettes()
    return runCatching {
        val json = JSONObject(value).optJSONObject("overrides") ?: return SidebarPalettes()
        val palettes = buildMap {
            SidebarMaterial.entries.forEach { material ->
                listOf(false, true).forEach { dark ->
                    val key = paletteKey(material, dark)
                    val palette = json.optJSONObject(key)
                    if (palette != null) {
                        val fallback = defaultSidebarPalette(material, dark)
                        put(key, SidebarPalette(
                            palette.optLong("background", fallback.background),
                            palette.optLong("backgroundEnd", fallback.backgroundEnd),
                            palette.optLong("selection", fallback.selection),
                            palette.optLong("content", fallback.content),
                            palette.optLong("highlight", fallback.highlight),
                            palette.optLong("shade", fallback.shade),
                            palette.optDouble("tintAlpha", fallback.tintAlpha.toDouble()).toFloat(),
                            palette.optDouble("highlightStrength", fallback.highlightStrength.toDouble()).toFloat(),
                            palette.optDouble("reliefStrength", fallback.reliefStrength.toDouble()).toFloat(),
                        ).normalized(fallback))
                    }
                }
            }
        }
        SidebarPalettes(palettes)
    }.getOrDefault(SidebarPalettes())
}

internal fun encodeSidebarPalettes(value: SidebarPalettes): String {
    val overrides = JSONObject()
    value.normalized().overrides.forEach { (key, palette) ->
        overrides.put(key, JSONObject().apply {
            put("background", palette.background)
            put("backgroundEnd", palette.backgroundEnd)
            put("selection", palette.selection)
            put("content", palette.content)
            put("highlight", palette.highlight)
            put("shade", palette.shade)
            put("tintAlpha", palette.tintAlpha.toDouble())
            put("highlightStrength", palette.highlightStrength.toDouble())
            put("reliefStrength", palette.reliefStrength.toDouble())
        })
    }
    return JSONObject().put("overrides", overrides).toString()
}

/** Accept only explicit RGB, so malformed or transparent colours cannot hide navigation. */
internal fun parseSidebarRgb(value: String): Long? {
    val hex = value.trim().removePrefix("#")
    if (hex.length != 6 || hex.any { it !in "0123456789abcdefABCDEF" }) return null
    return hex.toLongOrNull(16)?.or(0xFF000000L)
}

internal fun formatSidebarRgb(value: Long): String =
    "#" + (value and 0xFFFFFFL).toString(16).padStart(6, '0').uppercase(java.util.Locale.ROOT)

internal const val SIDEBAR_MATERIAL_KEY = "sidebar_widget_material"
internal const val SIDEBAR_PALETTES_KEY = "sidebar_widget_palettes_v1"
