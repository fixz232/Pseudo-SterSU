package me.weishu.kernelsu.ui.component.rain

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.component.custom.CustomCardTarget
import me.weishu.kernelsu.ui.component.decoration.uiDecoratedCard
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.util.InterfaceStyleTheme
import me.weishu.kernelsu.ui.util.LocalInterfaceStyleTheme
import me.weishu.kernelsu.ui.util.drawInterfaceStyleMotifs
import top.yukonga.miuix.kmp.basic.CardDefaults
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Composable
@ReadOnlyComposable
fun isRainInterfaceStyle(): Boolean = LocalInterfaceStyle.current == InterfaceStyle.Rain.value

val LocalRainCardMotionEnabled = staticCompositionLocalOf { DEFAULT_RAIN_CARD_MOTION_ENABLED }

val LocalRainCardMotionProgress = staticCompositionLocalOf<State<Float>> {
    mutableFloatStateOf(STATIC_CARD_MOTION_PROGRESS)
}

val LocalRainSceneProgress = staticCompositionLocalOf<State<Float>> {
    mutableFloatStateOf(STATIC_PROGRESS)
}

@Composable
fun rememberRainCardMotionProgress(enabled: Boolean): State<Float> {
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    if (!enabled || !animationsEnabled) {
        return remember { mutableFloatStateOf(STATIC_CARD_MOTION_PROGRESS) }
    }
    val transition = rememberInfiniteTransition(label = "rainCardMotion")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(CARD_MOTION_CYCLE_MILLIS, easing = LinearEasing)),
        label = "rainCardMotionProgress",
    )
}

@Composable
fun rememberRainSceneProgress(
    enabled: Boolean,
    style: RainStyle,
    theme: InterfaceStyleTheme? = null,
): State<Float> {
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val clearOnCycle = theme?.scene?.clearOnCycle == true
    if (!enabled || !animationsEnabled) {
        return remember(style, clearOnCycle) {
            mutableFloatStateOf(if (clearOnCycle) 1f else STATIC_PROGRESS)
        }
    }
    if (clearOnCycle) {
        val clearing = remember(style, theme) { Animatable(0f) }
        val clearingState = remember(clearing) { derivedStateOf { clearing.value } }
        LaunchedEffect(clearing) {
            clearing.snapTo(0f)
            clearing.animateTo(
                1f,
                tween(theme.scene.cycleMillis, easing = FastOutSlowInEasing),
            )
        }
        return clearingState
    }
    val cycleMillis = RainSceneSpec.fromTheme(theme).cycleMillis
    val transition = rememberInfiniteTransition(label = "rainScene")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(cycleMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "rainSceneProgress",
    )
}

@Composable
fun RainBackdrop(modifier: Modifier = Modifier) {
    if (!isRainInterfaceStyle()) return
    val style = LocalRainStyle.current
    val theme = LocalInterfaceStyleTheme.current
    val dark = isInDarkTheme()
    val palette = rainPalette(theme, style, dark)
    val spec = remember(theme) { RainSceneSpec.fromTheme(theme) }
    val drops = remember(theme, style) { createRainDrops(spec, style.value.hashCode()) }
    val progress = LocalRainSceneProgress.current
    Canvas(modifier.fillMaxSize()) {
        if (size.width <= 1f || size.height <= 1f) return@Canvas
        drawRect(Brush.verticalGradient(listOf(palette.backgroundTop, palette.backgroundBottom)))
        drawClouds(palette, spec.rippleCount, style.value.hashCode())
        theme?.let {
            drawInterfaceStyleMotifs(
                motifs = it.scene.motifs,
                palette = if (dark || it.forceDark) it.darkPalette else it.lightPalette,
                progress = progress.value,
                alphaMultiplier = 0.52f,
            )
        }
        drawRainField(drops, spec, palette, progress.value, 0f, 0.58f, 0.40f)
    }
}

@Composable
fun RainForegroundOverlay(modifier: Modifier = Modifier) {
    if (!isRainInterfaceStyle()) return
    val style = LocalRainStyle.current
    val theme = LocalInterfaceStyleTheme.current
    val dark = isInDarkTheme()
    val palette = rainPalette(theme, style, dark)
    val spec = remember(theme) { RainSceneSpec.fromTheme(theme) }
    val drops = remember(theme, style) { createRainDrops(spec, style.value.hashCode()) }
    val ripples = remember(theme, style) { createRainRipples(spec, style.value.hashCode() xor RIPPLE_SEED) }
    val progress = LocalRainSceneProgress.current
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    var lightningAlpha by remember(theme, dark) { mutableFloatStateOf(0f) }

    LaunchedEffect(theme, dark, animationsEnabled) {
        lightningAlpha = 0f
        if (!isRainLightningEnabled(theme, dark, animationsEnabled)) return@LaunchedEffect
        val random = Random(LIGHTNING_SEED)
        while (true) {
            delay(nextLightningDelayMillis(random))
            lightningAlpha = if (random.nextBoolean()) 0.30f else 0.22f
            delay(nextLightningFlashDurationMillis(random))
            lightningAlpha = 0f
        }
    }

    Canvas(modifier.fillMaxSize()) {
        val rainAlpha = rainCycleAlpha(theme?.scene?.clearOnCycle == true, progress.value)
        drawRainField(drops, spec, palette, progress.value, 0.52f, 1f, 0.76f * rainAlpha)
        drawRipples(ripples, palette, progress.value, rainAlpha)
        if (lightningAlpha > 0f) drawLightning(palette, lightningAlpha)
    }
}

@Composable
fun RainChromeOverlay(modifier: Modifier = Modifier) {
    if (!isRainInterfaceStyle()) return
    val style = LocalRainStyle.current
    val palette = rainPalette(LocalInterfaceStyleTheme.current, style, isInDarkTheme())
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val progress = LocalRainCardMotionProgress.current
    Canvas(modifier.fillMaxSize()) {
        val inset = 16.dp.toPx()
        val y = topPadding.toPx() + 7.dp.toPx()
        drawLine(palette.highlight.copy(alpha = 0.32f), Offset(inset, y), Offset(size.width - inset, y), 0.75.dp.toPx())
        val x = inset + (size.width - inset * 2f) * progress.value
        drawCircle(palette.rainAccent.copy(alpha = 0.72f), 1.25.dp.toPx(), Offset(x, y))
    }
}

@Composable
fun RainMotto(modifier: Modifier = Modifier) {
    if (!isRainInterfaceStyle()) return
    val style = LocalRainStyle.current
    val theme = LocalInterfaceStyleTheme.current
    val dark = isInDarkTheme()
    val palette = rainPalette(theme, style, dark)
    val shape = RoundedCornerShape(theme?.chrome?.cornerDp?.dp ?: 8.dp)
    Box(
        modifier = modifier
            .height(22.dp)
            .shadow(1.dp, shape, ambientColor = palette.shadow.copy(alpha = 0.14f))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(palette.surfaceTop, palette.surfaceBottom)), shape)
            .border(0.75.dp, palette.outline.copy(alpha = 0.48f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(style.mottoRes),
            modifier = Modifier.padding(horizontal = 28.dp),
            color = palette.content,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun Modifier.rainMiuixCardSurface(
    enabled: Boolean = true,
    capHeight: Dp = 11.dp,
    customTarget: CustomCardTarget = CustomCardTarget.Default,
): Modifier {
    if (!enabled || !isRainInterfaceStyle()) return this
    val style = LocalRainStyle.current
    val theme = LocalInterfaceStyleTheme.current
    val dark = isInDarkTheme()
    val palette = rainPalette(theme, style, dark)
    val shape = RoundedCornerShape(theme?.chrome?.cornerDp?.dp ?: 14.dp)
    val motionProgress = LocalRainCardMotionProgress.current
    val cardAlpha = theme?.chrome?.cardAlpha ?: 0.76f
    val borderAlpha = theme?.chrome?.borderAlpha ?: 0.60f
    return shadow(2.dp, shape, ambientColor = palette.shadow.copy(alpha = 0.20f))
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(palette.surfaceTop.copy(alpha = cardAlpha), palette.surfaceBottom.copy(alpha = cardAlpha)),
            ),
            shape,
        )
        .border(0.85.dp, palette.outline.copy(alpha = borderAlpha), shape)
        .drawWithContent {
            theme?.let {
                drawInterfaceStyleMotifs(
                    it.scene.motifs,
                    if (dark || it.forceDark) it.darkPalette else it.lightPalette,
                    motionProgress.value,
                    0.12f,
                )
            }
            drawContent()
            val cap = rainCardDecorationHeight(capHeight.toPx(), size.width, size.height)
            if (cap > 0f) {
                drawLine(
                    palette.rainAccent.copy(alpha = 0.45f),
                    Offset(cap, cap * 0.45f),
                    Offset(size.width - cap, cap * 0.45f),
                    0.7.dp.toPx(),
                )
            }
        }
        .uiDecoratedCard(shape = shape, enabled = true, customTarget = customTarget)
}

@Composable
fun rainMiuixCardColors(color: Color) = if (isRainInterfaceStyle()) {
    CardDefaults.defaultColors(color = rainCardContentLayerColor(color))
} else {
    CardDefaults.defaultColors(color = color)
}

internal fun rainCardContentLayerColor(baseColor: Color): Color = baseColor.copy(alpha = baseColor.alpha * 0.14f)

internal fun rainCardDecorationHeight(requestedHeight: Float, width: Float, height: Float): Float {
    if (requestedHeight <= 0f || width < 72f || height < 48f) return 0f
    return minOf(requestedHeight, height * 0.19f)
}

@Composable
fun rainNavigationContainerColor(): Color {
    val theme = LocalInterfaceStyleTheme.current
    val palette = rainPalette(theme, LocalRainStyle.current, isInDarkTheme())
    return palette.surfaceBottom.copy(alpha = theme?.chrome?.navigationAlpha ?: 0.62f)
}

@Composable
fun Modifier.rainNavigationSurface(shape: Shape, paintBackground: Boolean = true): Modifier {
    if (!isRainInterfaceStyle()) return this
    val theme = LocalInterfaceStyleTheme.current
    val palette = rainPalette(theme, LocalRainStyle.current, isInDarkTheme())
    return clip(shape)
        .then(if (paintBackground) Modifier.background(rainNavigationContainerColor(), shape) else Modifier)
        .border(0.8.dp, palette.outline.copy(alpha = theme?.chrome?.borderAlpha ?: 0.58f), shape)
}

@Composable
fun Modifier.rainNavigationIndicator(
    shape: Shape,
    paintBackground: Boolean = true,
    interactionKey: Any? = Unit,
): Modifier {
    if (!isRainInterfaceStyle()) return this
    val palette = rainPalette(LocalInterfaceStyleTheme.current, LocalRainStyle.current, isInDarkTheme())
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val rippleProgress = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    LaunchedEffect(interactionKey, animationsEnabled) {
        if (animationsEnabled) {
            rippleProgress.snapTo(0f)
            rippleProgress.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        } else {
            rippleProgress.snapTo(1f)
        }
    }
    return clip(shape)
        .then(if (paintBackground) Modifier.background(palette.ripple.copy(alpha = 0.14f), shape) else Modifier)
        .border(0.8.dp, palette.rainAccent.copy(alpha = 0.52f), shape)
        .drawWithContent {
            drawContent()
            if (rippleProgress.value < 1f) {
                val radius = size.maxDimension * (0.22f + rippleProgress.value * 0.84f)
                drawOval(
                    palette.rainAccent.copy(alpha = (1f - rippleProgress.value) * 0.34f),
                    Offset(size.width / 2f - radius, size.height / 2f - radius * 0.42f),
                    Size(radius * 2f, radius * 0.84f),
                    style = Stroke(0.9.dp.toPx()),
                )
            }
        }
}

@Composable
fun rainTopBarContainerColor(): Color {
    val theme = LocalInterfaceStyleTheme.current
    val palette = rainPalette(theme, LocalRainStyle.current, isInDarkTheme())
    return palette.surfaceBottom.copy(alpha = theme?.chrome?.topBarAlpha ?: 0.58f)
}

@Composable
fun rainTopBarContentColor(): Color =
    rainPalette(LocalInterfaceStyleTheme.current, LocalRainStyle.current, isInDarkTheme()).content

private data class RainDrop(
    val x: Float,
    val y: Float,
    val phase: Float,
    val speed: Float,
    val lengthDp: Float,
    val strokeDp: Float,
    val alpha: Float,
    val depth: Float,
)

private data class RainRipple(val x: Float, val y: Float, val phase: Float, val radiusDp: Float, val alpha: Float)

private fun createRainDrops(spec: RainSceneSpec, seed: Int): List<RainDrop> {
    val random = Random(seed)
    return List(spec.dropCount) {
        RainDrop(
            x = random.nextFloat(),
            y = random.nextFloat(),
            phase = random.nextFloat(),
            speed = random.nextFloatRange(0.72f, 1.38f),
            lengthDp = random.nextFloatRange(spec.minLengthDp, spec.maxLengthDp),
            strokeDp = random.nextFloatRange(spec.minStrokeDp, spec.maxStrokeDp),
            alpha = random.nextFloatRange(spec.minAlpha, spec.maxAlpha),
            depth = random.nextFloat(),
        )
    }
}

private fun createRainRipples(spec: RainSceneSpec, seed: Int): List<RainRipple> {
    val random = Random(seed)
    return List(spec.rippleCount) {
        RainRipple(
            x = random.nextFloatRange(0.08f, 0.92f),
            y = random.nextFloatRange(0.62f, 0.98f),
            phase = random.nextFloat(),
            radiusDp = random.nextFloatRange(9f, 24f),
            alpha = random.nextFloatRange(0.18f, 0.46f),
        )
    }
}

private fun DrawScope.drawClouds(palette: RainPalette, count: Int, seed: Int) {
    val random = Random(seed xor CLOUD_SEED)
    repeat(count.coerceIn(2, 20)) {
        val width = size.width * random.nextFloatRange(0.16f, 0.38f)
        val height = width * random.nextFloatRange(0.16f, 0.28f)
        drawOval(
            palette.cloud.copy(alpha = random.nextFloatRange(0.06f, 0.16f)),
            Offset(size.width * random.nextFloatRange(-0.10f, 0.92f), size.height * random.nextFloatRange(0.05f, 0.46f)),
            Size(width, height),
        )
    }
}

private fun DrawScope.drawRainField(
    drops: List<RainDrop>,
    spec: RainSceneSpec,
    palette: RainPalette,
    progress: Float,
    minimumDepth: Float,
    maximumDepth: Float,
    alphaMultiplier: Float,
) {
    drops.asSequence().filter { it.depth in minimumDepth..maximumDepth }.forEach { drop ->
        val local = (drop.y + progress * drop.speed + drop.phase) % 1.18f
        val start = Offset(drop.x * size.width, local * size.height - size.height * 0.09f)
        val length = drop.lengthDp.dp.toPx() * (0.72f + drop.depth * 0.46f)
        drawLine(
            palette.rain.copy(alpha = (drop.alpha * alphaMultiplier).coerceIn(0f, 1f)),
            start,
            start + Offset(spec.windRatio * length, length),
            drop.strokeDp.dp.toPx(),
            StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawRipples(
    ripples: List<RainRipple>,
    palette: RainPalette,
    progress: Float,
    alphaMultiplier: Float,
) {
    ripples.forEach { ripple ->
        val local = (progress + ripple.phase) % 1f
        val radius = ripple.radiusDp.dp.toPx() * (0.35f + local)
        drawOval(
            palette.ripple.copy(alpha = ripple.alpha * (1f - local) * alphaMultiplier),
            Offset(ripple.x * size.width - radius, ripple.y * size.height - radius * 0.30f),
            Size(radius * 2f, radius * 0.60f),
            style = Stroke(0.75.dp.toPx()),
        )
    }
}

private fun DrawScope.drawLightning(palette: RainPalette, alpha: Float) {
    drawRect(palette.highlight.copy(alpha = alpha * 0.40f))
    val path = Path().apply {
        moveTo(size.width * 0.62f, 0f)
        lineTo(size.width * 0.54f, size.height * 0.18f)
        lineTo(size.width * 0.60f, size.height * 0.18f)
        lineTo(size.width * 0.48f, size.height * 0.39f)
    }
    drawPath(path, palette.highlight.copy(alpha = alpha), style = Stroke(1.2.dp.toPx()))
}

private fun Random.nextFloatRange(start: Float, end: Float): Float = start + nextFloat() * (end - start)

private const val STATIC_PROGRESS = 0.37f
private const val STATIC_CARD_MOTION_PROGRESS = 0.34f
private const val CARD_MOTION_CYCLE_MILLIS = 7_800
private const val RIPPLE_SEED = 0x21A7
private const val CLOUD_SEED = 0x17C3
private const val LIGHTNING_SEED = 0x7A11
