package me.weishu.kernelsu.ui.component.bottombar

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.component.liquid.lens
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.colorControls
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/** Wallpaper only. Never provide a layer containing a rail or an AndroidView. */
internal val LocalSidebarGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

internal enum class SidebarGlassMode { Fallback, Frosted, Refractive }

internal fun sidebarGlassMode(
    enabled: Boolean,
    sdk: Int,
    hardwareAccelerated: Boolean,
    hasBackdrop: Boolean,
): SidebarGlassMode = when {
    !enabled || !hardwareAccelerated || !hasBackdrop || sdk < 31 -> SidebarGlassMode.Fallback
    sdk >= 34 -> SidebarGlassMode.Refractive
    else -> SidebarGlassMode.Frosted
}

internal fun canCaptureSidebarWallpaper(
    sidebarActive: Boolean,
    videoUri: String?,
    pagerHasVideo: Boolean,
): Boolean = sidebarActive && videoUri.isNullOrBlank() && !pagerHasVideo

internal object SidebarGlassColors {
    val LightInk = Color(0xFF161616)
    val DarkInk = Color(0xFFF5F5F5)
    val LightTint = Color(0xFFBFBFBF).copy(alpha = 0.82f)
    val DarkTint = Color.Black.copy(alpha = 0.76f)
    val LightFallback = Color(0xFFBFBFBF)
    val DarkFallback = Color(0xFF202020)
}

@Composable
internal fun rememberSidebarGlassBackdrop(enabled: Boolean): LayerBackdrop? {
    val inspection = LocalInspectionMode.current
    val hardwareAccelerated = LocalView.current.isHardwareAccelerated
    if (sidebarGlassMode(enabled && !inspection, Build.VERSION.SDK_INT, hardwareAccelerated, true) == SidebarGlassMode.Fallback) {
        return null
    }
    return rememberLayerBackdrop { drawContent() }
}

/** A restrained neutral underlay gives the default (no wallpaper) glass something to refract. */
internal fun Modifier.sidebarGlassUnderlay(dark: Boolean): Modifier = drawWithCache {
    val base = Brush.verticalGradient(
        if (dark) listOf(Color(0xFF353535), Color(0xFF151515), Color(0xFF252525))
        else listOf(Color(0xFFBDBDBD), Color(0xFFE7E7E7), Color(0xFFFAFAFA))
    )
    val light = Brush.radialGradient(
        listOf(Color.White.copy(alpha = if (dark) 0.16f else 0.68f), Color.Transparent),
        center = Offset(size.width * 0.18f, size.height * 0.3f),
        radius = size.maxDimension.coerceAtLeast(1f) * 0.72f,
    )
    onDrawBehind {
        drawRect(base)
        drawRect(light)
    }
}

/** Blur/refraction affects only the backdrop; icons, badges and weather remain sharp. */
internal fun Modifier.sidebarGlassMaterial(
    backdrop: Backdrop?,
    mode: SidebarGlassMode,
    shape: Shape,
    dark: Boolean,
    atStart: Boolean,
    indicator: Boolean = false,
    pressed: Float = 0f,
): Modifier {
    val tint = if (dark) SidebarGlassColors.DarkTint else SidebarGlassColors.LightTint
    val fallback = if (dark) SidebarGlassColors.DarkFallback else SidebarGlassColors.LightFallback
    val sheen = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (dark) 0.11f else 0.30f),
            Color.Transparent,
            Color.Black.copy(alpha = if (dark) 0.08f else 0.025f),
        )
    )
    val material = if (backdrop != null && mode != SidebarGlassMode.Fallback) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                // Deliberately remove wallpaper colour: this style stays black and white.
                colorControls(brightness = 0f, contrast = 1f, saturation = 0f)
                val radius = (if (indicator) 4.dp else 12.dp).toPx()
                blur(radius, radius)
                if (mode == SidebarGlassMode.Refractive) {
                    lens(
                        refractionHeight = (if (indicator) 8.dp else 14.dp).toPx(),
                        refractionAmount = (if (indicator) 5.dp else 4.dp).toPx() * (1f + pressed * 0.2f),
                        depthEffect = true,
                        chromaticAberration = 0f,
                    )
                }
            },
            highlight = {
                Highlight(
                    width = 0.8.dp,
                    alpha = if (indicator) 0.9f else 0.5f,
                    style = BloomStroke(
                        color = Color.White.copy(alpha = if (dark) 0.14f else 0.24f),
                        innerBlurRadius = 1.dp,
                        primaryLight = LightSource(
                            position = LightPosition(if (atStart) -0.2f else 1.2f, -0.4f, -0.1f),
                            color = Color.White, intensity = 0.8f + pressed * 0.2f,
                        ),
                        secondaryLight = LightSource(
                            position = LightPosition(if (atStart) 1.1f else -0.1f, 1.2f, -0.4f),
                            color = Color.White, intensity = 0.3f,
                        ),
                        dualPeak = true,
                    ),
                )
            },
            onDrawSurface = {
                drawRect(tint)
                drawRect(sheen)
                if (indicator) drawRect(if (dark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.045f))
            },
        )
    } else {
        Modifier.drawWithCache {
            onDrawBehind {
                drawRect(fallback)
                drawRect(sheen)
                if (indicator) drawRect(if (dark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.045f))
            }
        }
    }
    return this.then(material)
}

/** A narrow, soft edge separates the rail from the Material page without changing its layout. */
internal fun Modifier.sidebarGlassEdge(dark: Boolean, edgeOnRight: Boolean): Modifier = drawWithCache {
    val width = 8.dp.toPx().coerceAtMost(size.width)
    val startX = if (edgeOnRight) size.width - width else 0f
    val shades = listOf(Color.Transparent, Color.Black.copy(alpha = if (dark) 0.2f else 0.065f))
    val edge = Brush.horizontalGradient(if (edgeOnRight) shades else shades.reversed(), startX, startX + width)
    onDrawWithContent {
        drawContent()
        drawRect(edge, Offset(startX, 0f), Size(width, size.height))
    }
}
