package me.weishu.kernelsu.ui.component.bottombar

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.component.liquid.lens
import me.weishu.kernelsu.ui.util.SidebarMaterial
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/** Capture only wallpaper, never the rail itself or an AndroidView/video surface. */
internal val LocalSidebarGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }
internal enum class SidebarGlassMode { Fallback, Frosted, Refractive }
// lens() accepts CornerBasedShape only. Zero radii keep the visible pane strictly rectangular.
internal val SidebarGlassLensShape = RoundedCornerShape(0.dp)

internal fun sidebarGlassMode(
    material: SidebarMaterial,
    sdk: Int,
    hardwareAccelerated: Boolean,
    hasBackdrop: Boolean,
): SidebarGlassMode = when {
    material == SidebarMaterial.Flat || !hardwareAccelerated || !hasBackdrop || sdk < 31 -> SidebarGlassMode.Fallback
    material == SidebarMaterial.LiquidGlass && sdk >= 34 -> SidebarGlassMode.Refractive
    else -> SidebarGlassMode.Frosted
}

internal fun canCaptureSidebarWallpaper(
    sidebarActive: Boolean,
    material: SidebarMaterial,
    videoUri: String?,
    pagerHasVideo: Boolean,
    homeCardsNeedGlass: Boolean = false,
): Boolean = sidebarActive && (material != SidebarMaterial.Flat || homeCardsNeedGlass) && videoUri.isNullOrBlank() && !pagerHasVideo

@Composable
internal fun rememberSidebarGlassBackdrop(enabled: Boolean): LayerBackdrop? {
    if (!enabled || LocalInspectionMode.current || !LocalView.current.isHardwareAccelerated || Build.VERSION.SDK_INT < 31) {
        return null
    }
    return rememberLayerBackdrop { drawContent() }
}

internal fun Modifier.sidebarGlassUnderlay(dark: Boolean): Modifier = drawWithCache {
    val base = Brush.verticalGradient(
        if (dark) listOf(Color(0xFF353535), Color(0xFF151515), Color(0xFF252525))
        else listOf(Color(0xFFBDBDBD), Color(0xFFE7E7E7), Color(0xFFFAFAFA))
    )
    onDrawBehind { drawRect(base) }
}

/** Only the sampled wallpaper is blurred; text, icons and touch targets remain unchanged. */
internal fun Modifier.sidebarMaterialSurface(
    colors: SidebarColors,
    material: SidebarMaterial,
    mode: SidebarGlassMode,
    backdrop: Backdrop?,
    edgeOnRight: Boolean,
): Modifier {
    val gradient = Brush.verticalGradient(listOf(colors.background, colors.backgroundEnd))
    val base = if (material != SidebarMaterial.Flat && backdrop != null && mode != SidebarGlassMode.Fallback) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { SidebarGlassLensShape },
            effects = {
                val radius = (if (material == SidebarMaterial.NeumorphicGlass) 18.dp else 12.dp).toPx()
                blur(radius, radius)
                if (mode == SidebarGlassMode.Refractive) {
                    lens(14.dp.toPx(), 4.dp.toPx(), depthEffect = true, chromaticAberration = 0f)
                }
            },
            highlight = {
                Highlight(
                    width = 0.8.dp,
                    alpha = (0.5f * colors.highlightStrength).coerceIn(0f, 1f),
                    style = BloomStroke(
                        color = colors.highlight.copy(alpha = (0.24f * colors.highlightStrength).coerceIn(0f, 1f)),
                        innerBlurRadius = 1.dp,
                        primaryLight = LightSource(
                            position = LightPosition(if (edgeOnRight) -0.2f else 1.2f, -0.4f, -0.1f),
                            color = colors.highlight, intensity = 0.8f,
                        ),
                        secondaryLight = LightSource(
                            position = LightPosition(if (edgeOnRight) 1.1f else -0.1f, 1.2f, -0.4f),
                            color = colors.highlight, intensity = 0.3f,
                        ),
                        dualPeak = true,
                    ),
                )
            },
            onDrawSurface = { drawRect(gradient, alpha = colors.tintAlpha) },
        )
    } else {
        Modifier.drawWithCache { onDrawBehind { drawRect(gradient) } }
    }
    return then(base).drawWithCache {
        val edgeWidth = 1.dp.toPx().coerceAtMost(size.width)
        onDrawWithContent {
            if (material == SidebarMaterial.NeumorphicGlass) drawSidebarRelief(colors, 1f, inset = false)
            if (material == SidebarMaterial.LiquidGlass && mode == SidebarGlassMode.Fallback) {
                drawRect(colors.highlight.copy(alpha = (0.35f * colors.highlightStrength).coerceIn(0f, 1f)), size = Size(size.width, edgeWidth))
            }
            drawContent()
            drawRect(
                colors.divider,
                Offset(if (edgeOnRight) size.width - edgeWidth else 0f, 0f),
                Size(edgeWidth, size.height),
            )
        }
    }
}

internal fun Modifier.sidebarSelectionSurface(
    colors: SidebarColors,
    material: SidebarMaterial,
    selection: Float,
): Modifier = drawWithCache {
    onDrawBehind {
        drawRect(colors.selection, alpha = selection)
        if (material == SidebarMaterial.NeumorphicGlass && selection > 0f) {
            drawSidebarRelief(colors, selection, inset = true)
        }
    }
}

/** Small static inner light/shade edges: no continuous animation, offscreen shadow or rounded corners. */
private fun DrawScope.drawSidebarRelief(colors: SidebarColors, alpha: Float, inset: Boolean) {
    val width = 5.dp.toPx().coerceAtMost(size.minDimension / 2f)
    if (width <= 0f) return
    val light = colors.highlight.copy(alpha = (0.28f * colors.reliefStrength * alpha).coerceIn(0f, 1f))
    val shade = colors.shade.copy(alpha = (0.12f * colors.reliefStrength * alpha).coerceIn(0f, 1f))
    val leading = if (inset) shade else light
    val trailing = if (inset) light else shade
    drawRect(Brush.horizontalGradient(listOf(leading, Color.Transparent), 0f, width), size = Size(width, size.height))
    drawRect(Brush.verticalGradient(listOf(leading, Color.Transparent), 0f, width), size = Size(size.width, width))
    drawRect(
        Brush.horizontalGradient(listOf(Color.Transparent, trailing), size.width - width, size.width),
        Offset(size.width - width, 0f), Size(width, size.height),
    )
    drawRect(
        Brush.verticalGradient(listOf(Color.Transparent, trailing), size.height - width, size.height),
        Offset(0f, size.height - width), Size(size.width, width),
    )
}
