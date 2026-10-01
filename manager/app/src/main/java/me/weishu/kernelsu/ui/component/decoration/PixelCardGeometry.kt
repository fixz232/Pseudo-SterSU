package me.weishu.kernelsu.ui.component.decoration

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset

internal enum class PixelCardPattern {
    Generic,
    Handheld,
    Arcade,
    Pastoral,
    StarVoyage,
    InkJade,
    Wasteland,
    Ocean,
    Cyber,
    ThreeKingdoms,
    Bianliang,
    FishingHarbor,
    TribalJungle,
    LavaValley,
    DunhuangDesert,
    VikingSnowfield,
    JiangnanWatertown,
    CloudTown,
}

internal fun DrawScope.drawPixelCardPattern(
    pattern: PixelCardPattern,
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
    alpha: Float = 1f,
) {
    drawPixelCardPatternUnderlay(pattern, unit, primary, secondary, highlight, shadow, alpha)
    drawPixelCardPatternOverlay(pattern, unit, primary, secondary, highlight, shadow, alpha)
}

internal fun DrawScope.drawPixelCardPatternUnderlay(
    pattern: PixelCardPattern,
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
    alpha: Float = 1f,
) {
    if (unit <= 0f || size.width <= unit * 4f || size.height <= unit * 4f) return
    val seed = pattern.ordinal.coerceAtLeast(1)
    val primaryColor = primary.scaledAlpha(alpha * 0.48f)
    val secondaryColor = secondary.scaledAlpha(alpha * 0.52f)
    val highlightColor = highlight.scaledAlpha(alpha * 0.40f)
    val shadowColor = shadow.scaledAlpha(alpha * 0.28f)

    val railY = size.height * (0.22f + (seed % 5) * 0.09f)
    val railHeight = unit * (0.38f + (seed % 3) * 0.16f)
    drawRect(
        color = shadowColor,
        topLeft = Offset(unit * 2f, railY + unit * 0.55f),
        size = Size((size.width - unit * 4f).coerceAtLeast(0f), railHeight),
    )
    drawRect(
        color = if (seed % 2 == 0) primaryColor else secondaryColor,
        topLeft = Offset(unit * (2f + seed % 4), railY),
        size = Size((size.width * (0.22f + (seed % 6) * 0.07f)).coerceAtMost(size.width - unit * 4f), railHeight),
    )

    val columns = 3 + seed % 6
    val rows = 1 + seed % 3
    repeat(rows) { row ->
        repeat(columns) { column ->
            val xFraction = ((column + 1f) / (columns + 1f) + (seed % 4) * 0.017f) % 0.94f
            val yFraction = 0.56f + row * 0.10f + (seed % 3) * 0.018f
            val side = unit * (0.52f + ((column + seed) % 3) * 0.18f)
            val color = when ((column + row + seed) % 3) {
                0 -> primaryColor
                1 -> secondaryColor
                else -> highlightColor
            }
            drawRect(color, Offset(size.width * xFraction, size.height * yFraction), Size(side, side))
        }
    }

    when (seed % 4) {
        0 -> drawPixelCross(
            Offset(size.width * 0.82f, size.height * 0.30f),
            unit * 0.72f,
            secondaryColor,
        )
        1 -> drawRect(
            primaryColor,
            Offset(size.width * 0.74f, size.height * 0.25f),
            Size(unit * (2f + seed % 3), unit * 0.65f),
        )
        2 -> drawRect(
            highlightColor,
            Offset(size.width * 0.18f, size.height * 0.28f),
            Size(unit, unit * (2f + seed % 4)),
        )
        else -> drawRect(
            secondaryColor,
            Offset(size.width * 0.76f, size.height * 0.26f),
            Size(unit * 2.2f, unit * 2.2f),
            style = Stroke(unit * 0.42f),
        )
    }
}

internal fun DrawScope.drawPixelCardPatternOverlay(
    pattern: PixelCardPattern,
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
    alpha: Float = 1f,
) {
    val safeInset = pixelCardOverlayInset(unit, size.width, size.height)
    inset(safeInset, safeInset, safeInset, safeInset) {
        drawPixelDepthFrame(unit, primary, secondary, highlight, shadow, alpha)
        drawPixelComponentFrame(unit, primary.scaledAlpha(alpha * 0.64f), secondary.scaledAlpha(alpha * 0.70f))
        drawPixelPatternFramePolish(pattern, unit, primary, secondary, highlight, shadow, alpha)
        drawPixelCardTopDecoration(pattern, unit, primary, secondary, highlight, shadow, alpha)
    }
}

internal fun pixelCardOverlayInset(unit: Float, width: Float, height: Float): Float {
    if (unit <= 0f || width <= 0f || height <= 0f) return 0f
    return minOf(unit * 0.72f, width * 0.02f, height * 0.055f)
}

internal fun pixelCardTopDecorationHeight(unit: Float, width: Float, height: Float): Float {
    if (unit <= 0f || width < unit * 18f || height < unit * 10f) return 0f
    return minOf(unit * 4f, height * 0.16f)
}

internal fun pixelCardTopDecorationScale(unit: Float, availableHeight: Float): Float {
    if (unit <= 0f || availableHeight <= 0f) return 0f
    return (availableHeight / (unit * 4f)).coerceIn(0f, 1f)
}

internal fun pixelPatternFramePolishEnabled(unit: Float, width: Float, height: Float): Boolean =
    unit > 0f && width >= unit * 18f && height >= unit * 10f

private fun DrawScope.drawPixelPatternFramePolish(
    pattern: PixelCardPattern,
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
    alpha: Float,
) {
    if (!pixelPatternFramePolishEnabled(unit, size.width, size.height)) return
    val seed = pattern.ordinal.coerceAtLeast(1)
    val inset = unit * (1.2f + (seed % 3) * 0.18f)
    drawRect(
        color = primary.scaledAlpha(alpha * (0.28f + (seed % 4) * 0.06f)),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        style = Stroke(unit * (0.24f + (seed % 3) * 0.08f)),
    )
    repeat(2 + seed % 5) { index ->
        val x = size.width * (0.10f + index * (0.78f / (2 + seed % 5)))
        val color = when ((index + seed) % 3) {
            0 -> primary
            1 -> secondary
            else -> highlight
        }.scaledAlpha(alpha * 0.46f)
        drawRect(color, Offset(x, inset), Size(unit * 0.65f, unit * 0.65f))
    }
    drawRect(
        shadow.scaledAlpha(alpha * 0.36f),
        Offset(size.width * 0.28f, size.height - inset - unit * 0.36f),
        Size(size.width * 0.44f, unit * 0.36f),
    )
}

private fun DrawScope.drawPixelCardTopDecoration(
    pattern: PixelCardPattern,
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
    alpha: Float,
) {
    if (pattern == PixelCardPattern.Generic) return
    val height = pixelCardTopDecorationHeight(unit, size.width, size.height)
    if (height <= 0f) return
    val seed = pattern.ordinal
    val baseline = height - unit * 0.5f
    drawRect(
        shadow.scaledAlpha(alpha * 0.50f),
        Offset(size.width * 0.06f, baseline),
        Size(size.width * 0.88f, unit * 0.5f),
    )
    val segments = 4 + seed % 5
    val segmentWidth = size.width * 0.82f / segments
    repeat(segments) { index ->
        val color = when ((index + seed) % 3) {
            0 -> primary
            1 -> secondary
            else -> highlight
        }.scaledAlpha(alpha * 0.72f)
        val lift = unit * (0.45f + ((index + seed) % 3) * 0.55f)
        drawRect(
            color,
            Offset(size.width * 0.09f + segmentWidth * index, baseline - lift),
            Size((segmentWidth - unit * 0.35f).coerceAtLeast(unit * 0.35f), lift),
        )
    }
}

private fun DrawScope.drawPixelDepthFrame(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
    alpha: Float,
) {
    if (!pixelPatternFramePolishEnabled(unit, size.width, size.height)) return
    val inset = unit * 1.45f
    drawRect(
        primary.scaledAlpha(alpha * 0.24f),
        Offset(inset, inset),
        Size(size.width - inset * 2f, size.height - inset * 2f),
        style = Stroke(unit * 0.28f),
    )
    drawRect(highlight.scaledAlpha(alpha * 0.42f), Offset(size.width * 0.19f, inset), Size(size.width * 0.28f, unit * 0.32f))
    drawRect(secondary.scaledAlpha(alpha * 0.46f), Offset(size.width * 0.53f, inset), Size(size.width * 0.18f, unit * 0.32f))
    drawRect(shadow.scaledAlpha(alpha * 0.38f), Offset(size.width * 0.28f, size.height - inset - unit * 0.32f), Size(size.width * 0.44f, unit * 0.32f))
}

private fun DrawScope.drawPixelComponentFrame(unit: Float, primary: Color, secondary: Color) {
    val length = unit * 4.5f
    drawRect(primary, Offset.Zero, Size(length, unit))
    drawRect(primary, Offset.Zero, Size(unit, length))
    drawRect(primary, Offset(size.width - length, 0f), Size(length, unit))
    drawRect(primary, Offset(size.width - unit, 0f), Size(unit, length))
    drawRect(primary, Offset(0f, size.height - unit), Size(length, unit))
    drawRect(primary, Offset(0f, size.height - length), Size(unit, length))
    drawRect(primary, Offset(size.width - length, size.height - unit), Size(length, unit))
    drawRect(primary, Offset(size.width - unit, size.height - length), Size(unit, length))
    drawRect(secondary, Offset(unit * 2f, 0f), Size(unit, unit))
    drawRect(secondary, Offset(size.width - unit * 3f, size.height - unit), Size(unit, unit))
}

private fun DrawScope.drawPixelCross(center: Offset, unit: Float, color: Color) {
    drawRect(color, center - Offset(unit * 0.5f, unit * 1.5f), Size(unit, unit * 3f))
    drawRect(color, center - Offset(unit * 1.5f, unit * 0.5f), Size(unit * 3f, unit))
}

private fun Color.scaledAlpha(multiplier: Float): Color = copy(
    alpha = (alpha * multiplier).coerceIn(0f, 1f),
)
