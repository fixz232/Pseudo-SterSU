package me.weishu.kernelsu.ui.component.pixel

import android.animation.ValueAnimator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.util.InterfaceStyleTheme
import me.weishu.kernelsu.ui.util.LocalInterfaceStyleTheme
import me.weishu.kernelsu.ui.util.drawInterfaceStyleMotifs
import top.yukonga.miuix.kmp.basic.CardDefaults

val pixelMottoShape: Shape = RectangleShape

@Composable
@ReadOnlyComposable
fun isPixelInterfaceStyle(): Boolean = LocalInterfaceStyle.current == InterfaceStyle.Pixel.value

@Composable
fun PixelBackdrop(modifier: Modifier = Modifier) {
    if (!isPixelInterfaceStyle()) return
    val style = LocalPixelStyle.current
    val theme = LocalInterfaceStyleTheme.current
    val dark = isInDarkTheme()
    val palette = pixelPalette(style, dark, theme)
    val progress = rememberPixelSceneProgress(theme)
    Canvas(modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(palette.background, palette.backgroundAlt)))
        drawPixelGrid(palette, theme?.scene?.gridDp ?: 18f)
        theme?.let {
            drawInterfaceStyleMotifs(
                motifs = it.scene.motifs,
                palette = if (dark || it.forceDark) it.darkPalette else it.lightPalette,
                progress = progress,
            )
        }
    }
}

@Composable
private fun rememberPixelSceneProgress(theme: InterfaceStyleTheme?): Float {
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    if (!animationsEnabled || theme == null || theme.scene.speed <= 0f) return STATIC_PIXEL_SCENE_PROGRESS
    val transition = rememberInfiniteTransition(label = "externalPixelScene")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(theme.scene.cycleMillis, easing = LinearEasing)),
        label = "externalPixelSceneProgress",
    )
    return progress
}

@Composable
fun PixelChromeOverlay(modifier: Modifier = Modifier) {
    if (!isPixelInterfaceStyle()) return
    val theme = LocalInterfaceStyleTheme.current
    val palette = pixelPalette(LocalPixelStyle.current, isInDarkTheme(), theme)
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Canvas(modifier.fillMaxSize()) {
        val unit = (theme?.chrome?.unitDp ?: 2f).dp.toPx()
        val top = topPadding.toPx() + unit
        drawRect(palette.primary.copy(alpha = 0.48f), Offset(unit * 5f, top), Size(unit * 8f, unit))
        drawRect(
            palette.secondary.copy(alpha = 0.42f),
            Offset(size.width - unit * 13f, top),
            Size(unit * 8f, unit),
        )
    }
}

@Composable
fun PixelMotto(modifier: Modifier = Modifier) {
    if (!isPixelInterfaceStyle()) return
    val style = LocalPixelStyle.current
    val theme = LocalInterfaceStyleTheme.current
    val dark = isInDarkTheme()
    val palette = pixelPalette(style, dark, theme)
    val motion = LocalPixelCardMotionProgress.current
    Box(
        modifier = modifier
            .height(22.dp)
            .clip(pixelMottoShape)
            .background(palette.surface.copy(alpha = theme?.chrome?.cardAlpha ?: 0.92f), pixelMottoShape)
            .border(1.dp, palette.primary.copy(alpha = theme?.chrome?.borderAlpha ?: 0.72f), pixelMottoShape)
            .drawWithContent {
                val unit = (theme?.chrome?.unitDp ?: 2f).dp.toPx().coerceAtMost(size.minDimension / 4f)
                drawRect(palette.secondary.copy(alpha = 0.62f), Offset(unit, unit), Size(unit * 2f, unit))
                drawRect(
                    palette.primary.copy(alpha = 0.54f),
                    Offset(size.width - unit * (3f + motion.value), size.height - unit * 2f),
                    Size(unit * 2f, unit),
                )
                drawContent()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(style.summaryRes),
            modifier = Modifier.padding(horizontal = 28.dp),
            color = palette.primary,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun Modifier.pixelMiuixCardSurface(
    shape: Shape = RectangleShape,
    enabled: Boolean = true,
    paintBackground: Boolean = true,
): Modifier {
    if (!enabled || !isPixelInterfaceStyle()) return this
    val style = LocalPixelStyle.current
    val theme = LocalInterfaceStyleTheme.current
    val dark = isInDarkTheme()
    val palette = pixelCardPaintPalette(style, dark, theme)
    val motionEnabled = LocalPixelCardMotionEnabled.current
    val motionProgress = LocalPixelCardMotionProgress.current
    val cardAlpha = theme?.chrome?.cardAlpha ?: 0.94f
    val borderAlpha = theme?.chrome?.borderAlpha ?: 0.72f
    return clip(shape)
        .then(
            if (paintBackground) {
                Modifier.background(palette.surface.copy(alpha = cardAlpha), shape)
            } else {
                Modifier
            },
        )
        .border(1.dp, palette.primary.copy(alpha = borderAlpha), shape)
        .drawWithContent {
            drawPixelCardFoundation(palette, theme)
            if (motionEnabled) drawPixelCardMotionUnderlay(theme, dark, motionProgress.value)
            drawContent()
            if (motionEnabled) drawPixelCardMotionOverlay(theme, palette, motionProgress.value)
            drawPixelCardFrame(palette, theme)
        }
}

@Composable
fun pixelAwareMiuixCardCornerRadius(defaultRadius: Dp): Dp =
    resolvePixelMiuixCardCornerRadius(isPixelInterfaceStyle(), defaultRadius)

internal fun resolvePixelMiuixCardCornerRadius(pixelStyle: Boolean, defaultRadius: Dp): Dp =
    if (pixelStyle) 0.dp else defaultRadius

@Composable
fun pixelAwareMiuixCardShape(defaultShape: Shape): Shape =
    if (isPixelInterfaceStyle()) RectangleShape else defaultShape

internal fun pixelCardPaintPalette(
    style: PixelStyle,
    dark: Boolean,
    theme: InterfaceStyleTheme? = null,
): PixelPalette {
    val palette = pixelPalette(style, dark, theme)
    if (dark || theme?.forceDark == true) return palette
    return palette.copy(highlight = lerp(palette.primary, palette.highlight, 0.42f))
}

@Composable
fun pixelMiuixCardColors(color: Color, enabled: Boolean = true) = if (enabled && isPixelInterfaceStyle()) {
    CardDefaults.defaultColors(color = pixelCardContentLayerColor(color))
} else {
    CardDefaults.defaultColors(color = color)
}

internal fun pixelCardContentLayerColor(baseColor: Color): Color = baseColor.copy(alpha = 0f)

@Composable
fun pixelNavigationContainerColor(): Color {
    val theme = LocalInterfaceStyleTheme.current
    val palette = pixelPalette(LocalPixelStyle.current, isInDarkTheme(), theme)
    return palette.surface.copy(alpha = theme?.chrome?.navigationAlpha ?: 0.94f)
}

@Composable
fun Modifier.pixelNavigationSurface(shape: Shape, paintBackground: Boolean = true): Modifier {
    if (!isPixelInterfaceStyle()) return this
    val theme = LocalInterfaceStyleTheme.current
    val palette = pixelPalette(LocalPixelStyle.current, isInDarkTheme(), theme)
    return clip(shape)
        .then(if (paintBackground) Modifier.background(pixelNavigationContainerColor(), shape) else Modifier)
        .border(1.dp, palette.outline.copy(alpha = theme?.chrome?.borderAlpha ?: 0.64f), shape)
        .drawWithContent {
            drawContent()
            val unit = (theme?.chrome?.unitDp ?: 2f).dp.toPx()
            drawRect(palette.primary.copy(alpha = 0.46f), Offset(unit * 2f, unit), Size(unit * 5f, unit * 0.5f))
            drawRect(
                palette.secondary.copy(alpha = 0.40f),
                Offset(size.width - unit * 7f, size.height - unit * 1.5f),
                Size(unit * 5f, unit * 0.5f),
            )
        }
}

@Composable
fun Modifier.pixelNavigationIndicator(shape: Shape, paintBackground: Boolean = true): Modifier {
    if (!isPixelInterfaceStyle()) return this
    val theme = LocalInterfaceStyleTheme.current
    val palette = pixelPalette(LocalPixelStyle.current, isInDarkTheme(), theme)
    return clip(shape)
        .then(if (paintBackground) Modifier.background(palette.primary.copy(alpha = 0.18f), shape) else Modifier)
        .border(1.dp, palette.primary.copy(alpha = 0.72f), shape)
        .drawWithContent {
            drawContent()
            val unit = (theme?.chrome?.unitDp ?: 2f).dp.toPx()
            drawRect(palette.secondary.copy(alpha = 0.64f), Offset(unit, unit), Size(unit, unit))
            drawRect(
                palette.highlight.copy(alpha = 0.52f),
                Offset(size.width - unit * 2f, size.height - unit * 2f),
                Size(unit, unit),
            )
        }
}

private fun DrawScope.drawPixelGrid(palette: PixelPalette, gridDp: Float) {
    val step = gridDp.dp.toPx().coerceAtLeast(4f)
    var x = 0f
    while (x <= size.width) {
        drawLine(palette.outline.copy(alpha = 0.08f), Offset(x, 0f), Offset(x, size.height), 1f)
        x += step
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(palette.outline.copy(alpha = 0.08f), Offset(0f, y), Offset(size.width, y), 1f)
        y += step
    }
}

private fun DrawScope.drawPixelCardFoundation(palette: PixelPalette, theme: InterfaceStyleTheme?) {
    val unit = (theme?.chrome?.unitDp ?: 2f).dp.toPx().coerceAtMost(size.minDimension / 8f)
    if (unit <= 0f) return
    drawRect(palette.surfaceAlt.copy(alpha = 0.22f), Offset(unit, unit), Size(size.width - unit * 2f, unit))
    drawRect(
        palette.shadow.copy(alpha = 0.10f),
        Offset(unit, size.height - unit * 2f),
        Size(size.width - unit * 2f, unit),
    )
}

private fun DrawScope.drawPixelCardFrame(palette: PixelPalette, theme: InterfaceStyleTheme?) {
    val unit = (theme?.chrome?.unitDp ?: 2f).dp.toPx().coerceAtMost(size.minDimension / 8f)
    if (unit <= 0f) return
    val length = unit * 4f
    val color = palette.primary.copy(alpha = 0.72f)
    drawRect(color, Offset.Zero, Size(length, unit))
    drawRect(color, Offset.Zero, Size(unit, length))
    drawRect(color, Offset(size.width - length, size.height - unit), Size(length, unit))
    drawRect(color, Offset(size.width - unit, size.height - length), Size(unit, length))
}

private const val STATIC_PIXEL_SCENE_PROGRESS = 0.37f
