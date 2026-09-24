package me.weishu.kernelsu.ui.component.pixel

import android.animation.ValueAnimator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.util.InterfaceStyleTheme
import me.weishu.kernelsu.ui.util.drawInterfaceStyleMotifs
import kotlin.math.sin

val LocalPixelCardMotionEnabled = staticCompositionLocalOf { DEFAULT_PIXEL_CARD_MOTION_ENABLED }

val LocalPixelCardMotionProgress = staticCompositionLocalOf<State<Float>> {
    mutableFloatStateOf(STATIC_PIXEL_CARD_MOTION_PROGRESS)
}

@Composable
fun rememberPixelCardMotionProgress(
    enabled: Boolean,
    theme: InterfaceStyleTheme? = null,
): State<Float> {
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    if (!enabled || !animationsEnabled) {
        return remember { mutableFloatStateOf(STATIC_PIXEL_CARD_MOTION_PROGRESS) }
    }
    val transition = rememberInfiniteTransition(label = "pixelCardMotion")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(theme?.scene?.cycleMillis ?: PIXEL_CARD_MOTION_CYCLE_MILLIS, easing = LinearEasing),
        ),
        label = "pixelCardMotionProgress",
    )
}

internal fun DrawScope.drawPixelCardMotionUnderlay(
    theme: InterfaceStyleTheme?,
    dark: Boolean,
    progress: Float,
) {
    if (theme == null || size.width < 24f || size.height < 18f) return
    drawInterfaceStyleMotifs(
        motifs = theme.scene.motifs,
        palette = if (dark || theme.forceDark) theme.darkPalette else theme.lightPalette,
        progress = progress,
        alphaMultiplier = 0.10f,
    )
}

internal fun DrawScope.drawPixelCardMotionOverlay(
    theme: InterfaceStyleTheme?,
    palette: PixelPalette,
    progress: Float,
) {
    if (theme == null || size.width < 24f || size.height < 18f) return
    val unit = minOf(theme.chrome.unitDp.dp.toPx(), size.minDimension / 12f)
    val phase = ((progress % 1f) + 1f) % 1f
    val pulse = (0.5f + sin(phase * Math.PI.toFloat() * 2f) * 0.5f).coerceIn(0f, 1f)
    val travel = (size.width - unit * 8f).coerceAtLeast(1f)
    drawRect(
        color = palette.secondary.copy(alpha = 0.24f + pulse * 0.28f),
        topLeft = Offset(unit * 4f + travel * phase, unit),
        size = Size(unit * 2f, unit * 0.55f),
    )
    drawRect(
        color = palette.primary.copy(alpha = 0.20f + (1f - pulse) * 0.22f),
        topLeft = Offset(unit, size.height - unit * 1.5f),
        size = Size((size.width - unit * 2f) * phase, unit * 0.45f),
    )
}

private const val STATIC_PIXEL_CARD_MOTION_PROGRESS = 0.38f
private const val PIXEL_CARD_MOTION_CYCLE_MILLIS = 8_000
