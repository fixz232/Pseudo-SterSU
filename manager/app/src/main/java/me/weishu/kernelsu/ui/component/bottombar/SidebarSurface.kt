package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import me.weishu.kernelsu.ui.util.SidebarPalette

internal data class SidebarColors(
    val background: Color,
    val selection: Color,
    val content: Color,
    val divider: Color,
    val backgroundEnd: Color = background,
    val highlight: Color = Color.White,
    val shade: Color = Color.Black,
    val tintAlpha: Float = 0.90f,
    val highlightStrength: Float = 1f,
    val reliefStrength: Float = 1f,
)

private val LightSidebarColors = SidebarColors(
    background = Color(0xFFEAEAEA),
    selection = Color(0xFFCDCDCD),
    content = Color(0xFF161616),
    divider = Color(0xFFCBCBCB),
)
private val DarkSidebarColors = SidebarColors(
    background = Color(0xFF202020),
    selection = Color(0xFF444444),
    content = Color(0xFFF5F5F5),
    divider = Color(0xFF3C3C3C),
)

internal fun sidebarColors(dark: Boolean): SidebarColors =
    if (dark) DarkSidebarColors else LightSidebarColors

internal fun sidebarColors(palette: SidebarPalette): SidebarColors = SidebarColors(
    background = Color(palette.background.toInt()),
    backgroundEnd = Color(palette.backgroundEnd.toInt()),
    selection = Color(palette.selection.toInt()),
    content = Color(palette.content.toInt()),
    highlight = Color(palette.highlight.toInt()),
    shade = Color(palette.shade.toInt()),
    tintAlpha = palette.tintAlpha,
    highlightStrength = palette.highlightStrength,
    reliefStrength = palette.reliefStrength,
    divider = lerp(Color(palette.background.toInt()), Color(palette.content.toInt()), 0.16f),
)

internal fun sidebarContrast(ink: Color, surface: Color): Float =
    (maxOf(ink.luminance(), surface.luminance()) + 0.05f) /
        (minOf(ink.luminance(), surface.luminance()) + 0.05f)

/** Estimate the least readable point, including wallpaper extremes and selected states. */
internal fun sidebarMinimumContrast(colors: SidebarColors, glass: Boolean): Float {
    var minimum = Float.MAX_VALUE
    for (step in 0..10) {
        val gradient = lerp(colors.background, colors.backgroundEnd, step / 10f)
        for (wallpaper in listOf(Color.Black, Color.White)) {
            val base = if (glass) lerp(wallpaper, gradient, colors.tintAlpha) else gradient
            for (selection in listOf(0f, 0.45f, 0.7f, 0.85f, 1f)) {
                val surface = lerp(base, colors.selection, selection)
                minimum = minOf(minimum, sidebarContrast(colors.content, surface))
            }
        }
    }
    return minimum
}
