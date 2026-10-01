package me.weishu.kernelsu.ui.component.rain

import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.util.InterfaceStyleTheme
import kotlin.random.Random

const val RAIN_STYLE_KEY = "rain_style"
const val RAIN_CARD_MOTION_ENABLED_KEY = "rain_card_motion_enabled"
const val DEFAULT_RAIN_CARD_MOTION_ENABLED = true

enum class RainStyle(
    val value: String,
    @StringRes val labelRes: Int,
    @StringRes val summaryRes: Int,
    @StringRes val mottoRes: Int,
    @ColorInt val keyColor: Int,
) {
    LightRain("light_rain", R.string.rain_style_light, R.string.rain_style_light_summary, R.string.rain_motto_light, 0xFF5E84A6.toInt()),
    MediumRain("medium_rain", R.string.rain_style_medium, R.string.rain_style_medium_summary, R.string.rain_motto_medium, 0xFF4F7397.toInt()),
    HeavyRain("heavy_rain", R.string.rain_style_heavy, R.string.rain_style_heavy_summary, R.string.rain_motto_heavy, 0xFF3B566F.toInt()),
    Thunderstorm("thunderstorm", R.string.rain_style_thunderstorm, R.string.rain_style_thunderstorm_summary, R.string.rain_motto_thunderstorm, 0xFF666FA8.toInt()),
    AfterRain("after_rain", R.string.rain_style_after_rain, R.string.rain_style_after_rain_summary, R.string.rain_motto_after_rain, 0xFF719C9B.toInt());

    companion object {
        const val DEFAULT_VALUE = "light_rain"

        fun fromValue(value: String?): RainStyle = entries.firstOrNull { it.value == value } ?: LightRain

        fun fromIndex(index: Int): RainStyle = entries.getOrElse(index) { LightRain }

        fun selectedIndex(value: String?): Int = entries.indexOf(fromValue(value)).coerceAtLeast(0)
    }
}

val LocalRainStyle = staticCompositionLocalOf { RainStyle.LightRain }

@Immutable
data class RainPalette(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val fog: Color,
    val cloud: Color,
    val rain: Color,
    val rainAccent: Color,
    val ripple: Color,
    val surfaceTop: Color,
    val surfaceBottom: Color,
    val outline: Color,
    val highlight: Color,
    val content: Color,
    val shadow: Color,
)

fun rainPalette(theme: InterfaceStyleTheme?, style: RainStyle, dark: Boolean): RainPalette {
    val source = theme?.takeIf { it.engine == "rain" && it.variant == style.value }
        ?.let { if (dark || it.forceDark) it.darkPalette else it.lightPalette }
    if (source != null) {
        return RainPalette(
            backgroundTop = Color(source.background),
            backgroundBottom = Color(source.backgroundAlt),
            fog = Color(source.muted),
            cloud = Color(source.surfaceAlt),
            rain = Color(source.primary),
            rainAccent = Color(source.secondary),
            ripple = Color(source.outline),
            surfaceTop = Color(source.surface),
            surfaceBottom = Color(source.surfaceAlt),
            outline = Color(source.outline),
            highlight = Color(source.highlight),
            content = Color(source.content),
            shadow = Color(source.shadow),
        )
    }
    return fallbackRainPalette(style, dark)
}

fun rainPalette(style: RainStyle, dark: Boolean): RainPalette = fallbackRainPalette(style, dark)

private fun fallbackRainPalette(style: RainStyle, dark: Boolean): RainPalette {
    val accent = Color(style.keyColor)
    val backgroundTop = if (dark) lerp(Color(0xFF101820), accent, 0.28f) else lerp(Color.White, accent, 0.58f)
    val backgroundBottom = if (dark) lerp(Color.Black, accent, 0.22f) else lerp(Color.White, accent, 0.35f)
    val surfaceTop = if (dark) lerp(Color(0xFF172027), accent, 0.24f) else Color.White.copy(alpha = 0.76f)
    val surfaceBottom = if (dark) lerp(Color(0xFF10171C), accent, 0.18f) else lerp(Color.White, accent, 0.14f).copy(alpha = 0.68f)
    val content = if (dark) Color(0xFFE8F2F7) else Color(0xFF172F39)
    return RainPalette(
        backgroundTop = backgroundTop,
        backgroundBottom = backgroundBottom,
        fog = lerp(backgroundBottom, content, if (dark) 0.28f else 0.18f),
        cloud = lerp(backgroundTop, content, if (dark) 0.34f else 0.20f),
        rain = if (dark) Color(0xFFD8F1FF) else Color.White,
        rainAccent = lerp(accent, Color(0xFFBDF4EA), 0.55f),
        ripple = lerp(accent, content, 0.32f),
        surfaceTop = surfaceTop,
        surfaceBottom = surfaceBottom,
        outline = lerp(accent, content, if (dark) 0.42f else 0.24f),
        highlight = if (dark) Color(0xFFF2FAFF) else Color.White,
        content = content,
        shadow = if (dark) Color.Black else lerp(accent, Color.Black, 0.58f),
    )
}

internal data class RainSceneSpec(
    val dropCount: Int,
    val rippleCount: Int,
    val cycleMillis: Int,
    val minLengthDp: Float,
    val maxLengthDp: Float,
    val minStrokeDp: Float,
    val maxStrokeDp: Float,
    val windRatio: Float,
    val minAlpha: Float,
    val maxAlpha: Float,
) {
    companion object {
        fun fromTheme(theme: InterfaceStyleTheme?): RainSceneSpec {
            val scene = theme?.takeIf { it.engine == "rain" }?.scene
            return if (scene != null) {
                RainSceneSpec(
                    scene.primaryCount,
                    scene.secondaryCount,
                    scene.cycleMillis,
                    scene.minLengthDp,
                    scene.maxLengthDp,
                    scene.minStrokeDp,
                    scene.maxStrokeDp,
                    scene.angle,
                    scene.minAlpha,
                    scene.maxAlpha,
                )
            } else {
                RainSceneSpec(48, 6, 14_000, 8f, 18f, 0.4f, 0.8f, 0.08f, 0.16f, 0.38f)
            }
        }
    }
}

internal fun forceRainDarkTheme(style: RainStyle, theme: InterfaceStyleTheme? = null): Boolean =
    theme?.takeIf { it.engine == "rain" && it.variant == style.value }?.forceDark == true

internal fun isRainLightningEnabled(
    theme: InterfaceStyleTheme?,
    dark: Boolean,
    animationsEnabled: Boolean,
): Boolean = theme?.scene?.lightning == true && dark && animationsEnabled

internal fun nextLightningDelayMillis(random: Random): Long = random.nextLong(3_000L, 8_001L)

internal fun nextLightningFlashDurationMillis(random: Random): Long = random.nextLong(50L, 101L)

internal fun rainCycleAlpha(clearOnCycle: Boolean, progress: Float): Float {
    if (!clearOnCycle) return 1f
    return (1f - progress.coerceIn(0f, 1f) * 0.82f).coerceAtLeast(0.18f)
}

internal const val AFTER_RAIN_CLEARING_DURATION_MILLIS = 14_000
