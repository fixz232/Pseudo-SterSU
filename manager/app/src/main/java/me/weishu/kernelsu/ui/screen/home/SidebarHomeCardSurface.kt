package me.weishu.kernelsu.ui.screen.home

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.component.bottombar.LocalSidebarGlassBackdrop
import me.weishu.kernelsu.ui.component.bottombar.SidebarGlassMode
import me.weishu.kernelsu.ui.component.bottombar.sidebarGlassMode
import me.weishu.kernelsu.ui.component.liquid.lens
import me.weishu.kernelsu.ui.component.rememberCustomImageBitmap
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.util.SidebarCardGradient
import me.weishu.kernelsu.ui.util.SidebarCardMaterial
import me.weishu.kernelsu.ui.util.SidebarHomeCardId
import me.weishu.kernelsu.ui.util.SidebarHomeCardStyle
import me.weishu.kernelsu.ui.util.SidebarHomeCards
import me.weishu.kernelsu.ui.util.SidebarMaterial
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop

internal val LocalSidebarHomeCards = staticCompositionLocalOf { SidebarHomeCards() }

internal data class CardReadability(val ink: Color, val scrim: Color, val alpha: Float)

/** Protect text even with arbitrary photographs or opposite-luminance gradient stops. */
internal fun sidebarCardReadability(ink: Color, backgrounds: List<Color>): CardReadability {
    fun contrast(a: Color, b: Color) = (maxOf(a.luminance(), b.luminance()) + 0.05f) /
        (minOf(a.luminance(), b.luminance()) + 0.05f)
    val scrim = if (contrast(ink, Color.Black) >= contrast(ink, Color.White)) Color.Black else Color.White
    val inkLuminance = ink.luminance()
    for (step in 0..100) {
        val alpha = step / 100f
        val luminances = backgrounds.map { scrim.copy(alpha = alpha).compositeOver(it).luminance() }
        val low = luminances.minOrNull() ?: 0f
        val high = luminances.maxOrNull() ?: 1f
        // Endpoints alone are insufficient: mid-grey can contrast against both
        // black and white while disappearing into an intermediate photo pixel.
        val minimumContrast = when {
            inkLuminance > high -> (inkLuminance + 0.05f) / (high + 0.05f)
            inkLuminance < low -> (low + 0.05f) / (inkLuminance + 0.05f)
            else -> 1f
        }
        if (minimumContrast >= 4.5f) {
            return CardReadability(ink, scrim, alpha)
        }
    }
    return CardReadability(ink, scrim, 1f)
}

internal fun sidebarCardBackgroundSamples(style: SidebarHomeCardStyle, base: Color, hasImage: Boolean): List<Color> {
    if (style.material == SidebarCardMaterial.Default) return if (hasImage) listOf(Color.Black, Color.White) else listOf(base)
    val stops = listOf(Color(style.start.toInt()), Color(style.middle.toInt()), Color(style.end.toInt()))
    // Per-channel bounds contain every gradient interpolation, including the
    // sweep's closing segment and intermediate colors darker than both stops.
    val samples = if (style.gradient == SidebarCardGradient.Solid) listOf(stops[0]) else listOf(
        Color(stops.minOf { it.red }, stops.minOf { it.green }, stops.minOf { it.blue }),
        Color(stops.maxOf { it.red }, stops.maxOf { it.green }, stops.maxOf { it.blue }),
    )
    return samples.flatMap { color -> listOf(Color.Black, Color.White).map { color.copy(alpha = style.tint).compositeOver(it) } }
}

internal fun sidebarCardBrush(style: SidebarHomeCardStyle, size: Size): Brush {
    val colors = listOf(Color(style.start.toInt()), Color(style.middle.toInt()), Color(style.end.toInt()))
    return when (style.gradient) {
        SidebarCardGradient.Solid -> Brush.linearGradient(listOf(colors.first(), colors.first()))
        SidebarCardGradient.Vertical -> Brush.verticalGradient(colors)
        SidebarCardGradient.Horizontal -> Brush.horizontalGradient(colors)
        SidebarCardGradient.Diagonal -> Brush.linearGradient(colors, Offset.Zero, Offset(size.width, size.height))
        SidebarCardGradient.ReverseDiagonal -> Brush.linearGradient(colors, Offset(size.width, 0f), Offset(0f, size.height))
        SidebarCardGradient.Radial -> Brush.radialGradient(colors, Offset(size.width / 2f, size.height / 2f), size.maxDimension.coerceAtLeast(1f) * 0.7f)
        SidebarCardGradient.Sweep -> Brush.sweepGradient(colors + colors.first(), Offset(size.width / 2f, size.height / 2f))
    }
}

@Composable
internal fun SidebarHomeCardSurface(
    id: SidebarHomeCardId,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    fallbackContentColor: Color = MaterialTheme.colorScheme.onSurface,
    inheritedHasImage: Boolean = false,
    inheritedImage: @Composable BoxScope.() -> Unit = {},
    styleOverride: SidebarHomeCardStyle? = null,
    content: @Composable (Color, Color) -> Unit,
) {
    val style = styleOverride ?: LocalSidebarHomeCards.current.style(id, isInDarkTheme())
    val bitmap = rememberCustomImageBitmap(style.imageUri, maxSide = 1200, crop = style.crop)
    val inheritImage = style.imageUri == null && style.inheritImage
    val hasImage = bitmap != null || (inheritImage && inheritedHasImage)
    val glass = style.material != SidebarCardMaterial.Default
    val ink = if (!glass && style.imageUri == null && style.inheritImage) fallbackContentColor else Color(style.content.toInt())
    val readability = remember(style, containerColor, hasImage, ink) {
        sidebarCardReadability(ink, sidebarCardBackgroundSamples(style, containerColor, hasImage))
    }
    val backdrop = LocalSidebarGlassBackdrop.current
    val material = if (style.material == SidebarCardMaterial.LiquidGlass) SidebarMaterial.LiquidGlass else SidebarMaterial.NeumorphicGlass
    val mode = sidebarGlassMode(material, Build.VERSION.SDK_INT, LocalView.current.isHardwareAccelerated, backdrop != null)
    val shape = MaterialTheme.shapes.large
    val glassModifier = if (glass && backdrop != null && mode != SidebarGlassMode.Fallback) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                val radius = (if (style.material == SidebarCardMaterial.NeumorphicGlass) 18.dp else 10.dp).toPx()
                blur(radius, radius)
                if (mode == SidebarGlassMode.Refractive) lens(12.dp.toPx(), 3.dp.toPx(), depthEffect = true, chromaticAberration = 0f)
            },
        )
    } else Modifier
    val body: @Composable () -> Unit = {
        Box {
            Box(Modifier.matchParentSize().then(glassModifier))
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize().then(if (glass) Modifier.blur(4.dp) else Modifier),
                )
            } else if (inheritImage) inheritedImage()
            if (glass) Box(Modifier.matchParentSize().drawWithCache {
                val gradient = sidebarCardBrush(style, size)
                onDrawBehind { drawRect(gradient, alpha = style.tint) }
            })
            if (readability.alpha > 0f) Box(Modifier.matchParentSize().background(readability.scrim.copy(alpha = readability.alpha)))
            if (glass) Box(Modifier.matchParentSize().drawWithCache {
                val highlight = Color(style.highlight.toInt())
                val outline = shape.createOutline(size, layoutDirection, this)
                val edge = 6.dp.toPx().coerceAtMost(size.minDimension / 2)
                onDrawBehind {
                    if (style.material == SidebarCardMaterial.LiquidGlass) {
                        drawOutline(outline, Brush.linearGradient(listOf(highlight.copy(alpha = 0.65f), Color.Transparent, highlight.copy(alpha = 0.2f))), style = Stroke(1.dp.toPx()))
                    } else if (edge > 0f) {
                        drawRect(Brush.verticalGradient(listOf(highlight.copy(alpha = 0.3f), Color.Transparent), 0f, edge), size = Size(size.width, edge))
                        drawRect(Brush.horizontalGradient(listOf(highlight.copy(alpha = 0.25f), Color.Transparent), 0f, edge), size = Size(edge, size.height))
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.16f)), size.height - edge, size.height), Offset(0f, size.height - edge), Size(size.width, edge))
                        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.12f)), size.width - edge, size.width), Offset(size.width - edge, 0f), Size(edge, size.height))
                    }
                }
            })
            CompositionLocalProvider(LocalContentColor provides readability.ink) {
                // Keep both text levels opaque; hierarchy comes from size and weight.
                content(readability.ink, readability.ink)
            }
        }
    }
    if (onClick == null) {
        Surface(modifier = modifier, shape = shape, color = containerColor, contentColor = readability.ink, content = body)
    } else {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = containerColor, contentColor = readability.ink, content = body)
    }
}
