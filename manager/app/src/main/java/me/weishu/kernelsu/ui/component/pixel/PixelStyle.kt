package me.weishu.kernelsu.ui.component.pixel

import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.util.InterfaceStyleTheme

const val PIXEL_STYLE_KEY = "pixel_style"
const val PIXEL_CARD_MOTION_ENABLED_KEY = "pixel_card_motion_enabled"
const val DEFAULT_PIXEL_CARD_MOTION_ENABLED = true

enum class PixelStyle(
    val value: String,
    @StringRes val labelRes: Int,
    @StringRes val summaryRes: Int,
    @ColorInt val keyColor: Int,
) {
    ClassicHandheld("classic_handheld", R.string.pixel_style_classic_handheld, R.string.pixel_style_classic_handheld_summary, 0xFF556B3F.toInt()),
    NeonArcade("neon_arcade", R.string.pixel_style_neon_arcade, R.string.pixel_style_neon_arcade_summary, 0xFF007F7B.toInt()),
    PastoralFields("pastoral_fields", R.string.pixel_style_pastoral_fields, R.string.pixel_style_pastoral_fields_summary, 0xFF5E7650.toInt()),
    StarVoyage("star_voyage", R.string.pixel_style_star_voyage, R.string.pixel_style_star_voyage_summary, 0xFF526AC7.toInt()),
    InkJade("ink_jade", R.string.pixel_style_ink_jade, R.string.pixel_style_ink_jade_summary, 0xFF3D705E.toInt()),
    RustWasteland("rust_wasteland", R.string.pixel_style_rust_wasteland, R.string.pixel_style_rust_wasteland_summary, 0xFF825038.toInt()),
    OceanDepths("ocean_depths", R.string.pixel_style_ocean_depths, R.string.pixel_style_ocean_depths_summary, 0xFF167A8A.toInt()),
    CyberHacker("cyber_hacker", R.string.pixel_style_cyber_hacker, R.string.pixel_style_cyber_hacker_summary, 0xFF9B5CFF.toInt()),
    ThreeKingdoms("three_kingdoms", R.string.pixel_style_three_kingdoms, R.string.pixel_style_three_kingdoms_summary, 0xFF5C694F.toInt()),
    BianliangMarket("bianliang_market", R.string.pixel_style_bianliang_market, R.string.pixel_style_bianliang_market_summary, 0xFF73558F.toInt()),
    FishingHarbor("fishing_harbor", R.string.pixel_style_fishing_harbor, R.string.pixel_style_fishing_harbor_summary, 0xFF6957A5.toInt()),
    TribalJungle("tribal_jungle", R.string.pixel_style_tribal_jungle, R.string.pixel_style_tribal_jungle_summary, 0xFF526D45.toInt()),
    LavaValley("lava_valley", R.string.pixel_style_lava_valley, R.string.pixel_style_lava_valley_summary, 0xFFB8442C.toInt()),
    DunhuangDesert("dunhuang_desert", R.string.pixel_style_dunhuang_desert, R.string.pixel_style_dunhuang_desert_summary, 0xFF7B5C8F.toInt()),
    VikingSnowfield("viking_snowfield", R.string.pixel_style_viking_snowfield, R.string.pixel_style_viking_snowfield_summary, 0xFF4F6F8A.toInt()),
    JiangnanWatertown("jiangnan_watertown", R.string.pixel_style_jiangnan_watertown, R.string.pixel_style_jiangnan_watertown_summary, 0xFF58756F.toInt()),
    CloudTown("cloud_town", R.string.pixel_style_cloud_town, R.string.pixel_style_cloud_town_summary, 0xFF6C7E99.toInt());

    companion object {
        const val DEFAULT_VALUE = "classic_handheld"

        fun fromValue(value: String?): PixelStyle = when (value) {
            LEGACY_FOREST_QUEST_VALUE -> PastoralFields
            else -> entries.firstOrNull { it.value == value } ?: ClassicHandheld
        }

        fun fromIndex(index: Int): PixelStyle = entries.getOrElse(index) { ClassicHandheld }

        fun selectedIndex(value: String?): Int = entries.indexOf(fromValue(value))

        private const val LEGACY_FOREST_QUEST_VALUE = "forest_quest"
    }
}

val LocalPixelStyle = staticCompositionLocalOf { PixelStyle.ClassicHandheld }

@Immutable
data class PixelPalette(
    val background: Color,
    val backgroundAlt: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val primary: Color,
    val secondary: Color,
    val outline: Color,
    val highlight: Color,
    val shadow: Color,
)

fun pixelPalette(
    style: PixelStyle,
    dark: Boolean,
    theme: InterfaceStyleTheme? = null,
): PixelPalette {
    val source = theme?.takeIf { it.engine == "pixel" && it.variant == style.value }
        ?.let { if (dark || it.forceDark) it.darkPalette else it.lightPalette }
    if (source != null) {
        return PixelPalette(
            background = Color(source.background),
            backgroundAlt = Color(source.backgroundAlt),
            surface = Color(source.surface),
            surfaceAlt = Color(source.surfaceAlt),
            primary = Color(source.primary),
            secondary = Color(source.secondary),
            outline = Color(source.outline),
            highlight = Color(source.highlight),
            shadow = Color(source.shadow),
        )
    }
    return fallbackPixelPalette(style, dark)
}

private fun fallbackPixelPalette(style: PixelStyle, dark: Boolean): PixelPalette {
    val accent = Color(style.keyColor)
    return if (dark) {
        PixelPalette(
            background = lerp(Color.Black, accent, 0.14f),
            backgroundAlt = lerp(Color(0xFF08090B), accent, 0.22f),
            surface = lerp(Color(0xFF121416), accent, 0.28f),
            surfaceAlt = lerp(Color(0xFF0A0C0E), accent, 0.20f),
            primary = lerp(accent, Color.White, 0.38f),
            secondary = lerp(accent, Color(0xFFFFA45D), 0.52f),
            outline = lerp(accent, Color(0xFF9CB2C0), 0.46f),
            highlight = Color(0xFFF4FAFF),
            shadow = Color.Black,
        )
    } else {
        PixelPalette(
            background = lerp(Color.White, accent, 0.12f),
            backgroundAlt = lerp(Color.White, accent, 0.22f),
            surface = lerp(Color.White, accent, 0.05f),
            surfaceAlt = lerp(Color.White, accent, 0.16f),
            primary = accent,
            secondary = lerp(accent, Color(0xFF9E4E2A), 0.48f),
            outline = lerp(accent, Color(0xFF5D6670), 0.52f),
            highlight = Color.White,
            shadow = lerp(accent, Color.Black, 0.62f),
        )
    }
}
