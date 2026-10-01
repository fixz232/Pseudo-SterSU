package me.weishu.kernelsu.ui.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope

fun InterfaceStylePalette.color(token: InterfaceStyleColorToken): Color = Color(
    when (token) {
        InterfaceStyleColorToken.Primary -> primary
        InterfaceStyleColorToken.Secondary -> secondary
        InterfaceStyleColorToken.Outline -> outline
        InterfaceStyleColorToken.Highlight -> highlight
        InterfaceStyleColorToken.Shadow -> shadow
        InterfaceStyleColorToken.Muted -> muted
        InterfaceStyleColorToken.Content -> content
    }
)

fun DrawScope.drawInterfaceStyleMotifs(
    motifs: List<InterfaceStyleMotif>,
    palette: InterfaceStylePalette,
    progress: Float,
    alphaMultiplier: Float = 1f,
) {
    if (size.width <= 1f || size.height <= 1f) return
    val phase = progress.normalizedPhase()
    motifs.forEach { motif ->
        val color = palette.color(motif.color).copy(
            alpha = (motif.alpha * alphaMultiplier).coerceIn(0f, 1f),
        )
        repeat(motif.repeatY) { row ->
            repeat(motif.repeatX) { column ->
                val normalizedX = (
                    motif.x + column.toFloat() / motif.repeatX + motif.driftX * phase
                    ).normalizedPhase()
                val normalizedY = (
                    motif.y + row.toFloat() / motif.repeatY + motif.driftY * phase
                    ).normalizedPhase()
                val origin = Offset(normalizedX * size.width, normalizedY * size.height)
                val motifSize = Size(motif.width * size.width, motif.height * size.height)
                when (motif.type) {
                    InterfaceStyleMotifType.Rectangle -> drawRect(color, origin, motifSize)
                    InterfaceStyleMotifType.Circle -> drawCircle(
                        color = color,
                        radius = minOf(motifSize.width, motifSize.height) / 2f,
                        center = origin,
                    )
                    InterfaceStyleMotifType.Line -> drawLine(
                        color = color,
                        start = origin,
                        end = origin + Offset(motifSize.width, motifSize.height),
                        strokeWidth = motif.strokeDp * density,
                        cap = StrokeCap.Square,
                    )
                }
            }
        }
    }
}

private fun Float.normalizedPhase(): Float = ((this % 1f) + 1f) % 1f
