package me.weishu.kernelsu.ui.component.custom

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.LinkedHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

val LocalCustomSwitchStyle = staticCompositionLocalOf<CustomSwitchStyle?> { null }
val LocalComponentMotionProgressOverride = staticCompositionLocalOf<Float?> { null }

fun SwitchTransitionEasing.composeEasing() = when (this) {
    SwitchTransitionEasing.Standard -> FastOutSlowInEasing
    SwitchTransitionEasing.Linear -> LinearEasing
    SwitchTransitionEasing.Accelerate -> FastOutLinearInEasing
    SwitchTransitionEasing.Decelerate -> LinearOutSlowInEasing
}

@Composable
fun rememberComponentMotionProgress(
    rule: PixelMotionRule,
    enabled: Boolean,
    label: String,
): Float {
    LocalComponentMotionProgressOverride.current?.let { return it.coerceIn(0f, 1f) }
    val normalized = rule.normalized()
    val systemAnimationsEnabled = ValueAnimator.areAnimatorsEnabled()
    if (!enabled || !normalized.enabled || !systemAnimationsEnabled) return INACTIVE_MOTION_PROGRESS
    val transition = rememberInfiniteTransition(label = label)
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(normalized.durationMillis, easing = LinearEasing),
            repeatMode = when (normalized.repeat) {
                PixelMotionRepeat.Restart -> RepeatMode.Restart
                PixelMotionRepeat.Reverse -> RepeatMode.Reverse
            },
        ),
        label = "${label}Progress",
    )
    return progress
}

data class CustomSwitchImages(
    val off: ImageBitmap? = null,
    val on: ImageBitmap? = null,
)

@Composable
fun rememberCustomSwitchImages(style: CustomSwitchStyle?): CustomSwitchImages {
    val context = LocalContext.current
    val imageStyle = style?.takeIf { it.source == CustomSwitchSource.Image }
    val offUri = imageStyle?.imageUri
    val onUri = imageStyle?.imageOnUri
    val images by produceState(initialValue = CustomSwitchImages(), offUri, onUri) {
        value = withContext(Dispatchers.IO) {
            val store = ComponentStyleStore(context)
            fun decode(uri: String?): ImageBitmap? {
                val file = store.resolveImageFile(uri) ?: return null
                return runCatching { decodeCustomSwitchImage(file) }.getOrNull()
            }
            val off = decode(offUri)
            CustomSwitchImages(
                off = off,
                on = if (onUri == offUri) off else decode(onUri),
            )
        }
    }
    return images
}

fun DrawScope.drawCustomSwitchStyle(
    style: CustomSwitchStyle,
    checkedProgress: Float,
    enabledAlpha: Float,
    motionProgress: Float,
    images: CustomSwitchImages,
) {
    val normalized = style.normalized()
    val trackSize = Size(
        width = size.width * normalized.trackScaleX,
        height = size.height * normalized.trackScaleY,
    )
    val trackTopLeft = Offset(
        x = (size.width - trackSize.width) / 2f,
        y = (size.height - trackSize.height) / 2f,
    )
    val corner = trackSize.height * normalized.cornerRadiusFraction
    val path = Path().apply {
        addRoundRect(RoundRect(Rect(trackTopLeft, trackSize), CornerRadius(corner, corner)))
    }
    val dynamicAlpha = enabledAlpha * motionAlpha(normalized.motion, motionProgress)
    val stateProgress = checkedProgress.coerceIn(0f, 1f)
    val trackColor = stateColor(
        off = normalized.trackOffColorOverride,
        on = normalized.trackOnColorOverride,
        fallback = normalized.trackBaseColor,
        progress = stateProgress,
    )
    val borderColor = stateColor(
        off = normalized.borderOffColorOverride,
        on = normalized.borderOnColorOverride,
        fallback = normalized.borderColor,
        progress = stateProgress,
    )
    val thumbColor = stateColor(
        off = normalized.thumbOffColorOverride,
        on = normalized.thumbOnColorOverride,
        fallback = normalized.thumbBaseColor,
        progress = stateProgress,
    )
    val offImageAppearance = normalized.imageAppearanceFor(on = false)
    val onImageAppearance = normalized.imageAppearanceFor(on = true)
    if (normalized.shadowRadiusDp > 0f) {
        drawRoundRect(
            color = Color(normalized.shadowColor.toInt()).copy(
                alpha = Color(normalized.shadowColor.toInt()).alpha * enabledAlpha * 0.55f
            ),
            topLeft = trackTopLeft + Offset(0f, normalized.shadowRadiusDp.dp.toPx() * 0.45f),
            size = trackSize,
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = (normalized.shadowRadiusDp * 1.8f).dp.toPx()),
        )
    }
    if (normalized.glowRadiusDp > 0f) {
        drawRoundRect(
            color = Color(normalized.glowColor.toInt()).copy(
                alpha = Color(normalized.glowColor.toInt()).alpha * enabledAlpha * 0.60f
            ),
            topLeft = trackTopLeft,
            size = trackSize,
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = (normalized.glowRadiusDp * 1.6f).dp.toPx()),
        )
    }
    clipPath(path) {
        drawRoundRect(
            color = trackColor.copy(alpha = trackColor.alpha * enabledAlpha),
            topLeft = trackTopLeft,
            size = trackSize,
            cornerRadius = CornerRadius(corner, corner),
        )
        if (normalized.source == CustomSwitchSource.Image) {
            // A missing state image may intentionally fall back to the other state;
            // a configured-but-undecodable image must remain visible as an error.
            val offImage = images.off ?: images.on.takeIf { normalized.imageUri == null }
            val onImage = images.on ?: images.off.takeIf { normalized.imageOnUri == null }
            if (offImage == null) {
                drawMissingSwitchImage(trackTopLeft, trackSize, dynamicAlpha * (1f - checkedProgress))
            } else {
                offImage.let { image ->
                    drawSwitchImage(
                        image = image,
                        appearance = offImageAppearance,
                        topLeft = trackTopLeft,
                        destinationSize = trackSize,
                        alpha = dynamicAlpha * offImageAppearance.opacity * (1f - checkedProgress),
                    )
                }
            }
            if (onImage == null) {
                drawMissingSwitchImage(trackTopLeft, trackSize, dynamicAlpha * checkedProgress)
            } else {
                onImage.let { image ->
                    drawSwitchImage(
                        image = image,
                        appearance = onImageAppearance,
                        topLeft = trackTopLeft,
                        destinationSize = trackSize,
                        alpha = dynamicAlpha * onImageAppearance.opacity * checkedProgress,
                    )
                }
            }
        } else {
            val offsetX = motionOffset(
                normalized.motion,
                motionProgress,
                trackSize.width / normalized.trackOff.width,
            )
            drawPixelLayer(
                grid = normalized.trackOff,
                topLeft = trackTopLeft,
                destinationSize = trackSize,
                alpha = dynamicAlpha * (1f - checkedProgress),
                offsetX = offsetX,
            )
            drawPixelLayer(
                grid = normalized.trackOn,
                topLeft = trackTopLeft,
                destinationSize = trackSize,
                alpha = dynamicAlpha * checkedProgress,
                offsetX = offsetX,
            )
        }
        if (
            normalized.motion.enabled &&
            normalized.motion.mode == PixelMotionMode.Scan &&
            motionProgress.isActiveMotionProgress()
        ) {
            val scanX = trackTopLeft.x + trackSize.width * motionProgress.coerceIn(0f, 1f)
            drawRect(
                color = Color.White.copy(alpha = enabledAlpha * 0.24f),
                topLeft = Offset(scanX, trackTopLeft.y),
                size = Size(1.dp.toPx(), trackSize.height),
            )
        }
    }

    if (normalized.borderWidthDp > 0f) {
        drawRoundRect(
            color = borderColor.copy(alpha = borderColor.alpha * enabledAlpha),
            topLeft = trackTopLeft,
            size = trackSize,
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = normalized.borderWidthDp.dp.toPx()),
        )
    }

    val inset = normalized.thumbPaddingDp.dp.toPx().coerceAtMost(trackSize.height * 0.45f)
    val radius = ((trackSize.height / 2f - inset) * normalized.thumbScale)
        .coerceIn(1.dp.toPx(), trackSize.height / 2f)
    val fullStartX = trackTopLeft.x + inset + radius
    val fullEndX = trackTopLeft.x + trackSize.width - inset - radius
    val travelCenter = (fullStartX + fullEndX) / 2f
    val travelRadius = ((fullEndX - fullStartX) / 2f * normalized.thumbTravel).coerceAtLeast(0f)
    val startX = travelCenter - travelRadius
    val endX = travelCenter + travelRadius
    val center = Offset(startX + (endX - startX) * checkedProgress, size.height / 2f)
    val thumbSize = Size(radius * 2f, radius * 2f)
    val thumbTopLeft = center - Offset(radius, radius)
    val offBlank = normalized.thumbOff.pixels.all { it == TRANSPARENT_PIXEL }
    val onBlank = normalized.thumbOn.pixels.all { it == TRANSPARENT_PIXEL }
    drawCircle(
        color = thumbColor.copy(alpha = thumbColor.alpha * enabledAlpha),
        radius = radius,
        center = center,
    )
    if (offBlank && onBlank) {
        return
    } else {
        drawPixelLayer(
            grid = normalized.thumbOff,
            topLeft = thumbTopLeft,
            destinationSize = thumbSize,
            alpha = dynamicAlpha * (1f - checkedProgress),
        )
        drawPixelLayer(
            grid = normalized.thumbOn,
            topLeft = thumbTopLeft,
            destinationSize = thumbSize,
            alpha = dynamicAlpha * checkedProgress,
        )
    }
}

private fun stateColor(
    off: Long?,
    on: Long?,
    fallback: Long,
    progress: Float,
): Color {
    val offColor = Color((off ?: fallback).toInt())
    val onColor = Color((on ?: fallback).toInt())
    return lerp(offColor, onColor, progress)
}

fun DrawScope.drawPixelLayer(
    grid: PixelGrid,
    topLeft: Offset,
    destinationSize: Size,
    alpha: Float = 1f,
    offsetX: Float = 0f,
    filter: (x: Int, y: Int, width: Int, height: Int) -> Boolean = { _, _, _, _ -> true },
) {
    if (alpha <= 0f || destinationSize.width <= 0f || destinationSize.height <= 0f) return
    val cellWidth = destinationSize.width / grid.width
    val cellHeight = destinationSize.height / grid.height
    clipRect(
        left = topLeft.x,
        top = topLeft.y,
        right = topLeft.x + destinationSize.width,
        bottom = topLeft.y + destinationSize.height,
    ) {
        translate(left = offsetX) {
            for (y in 0 until grid.height) {
                for (x in 0 until grid.width) {
                    if (!filter(x, y, grid.width, grid.height)) continue
                    val argb = grid.colorAt(x, y)
                    if (argb == TRANSPARENT_PIXEL) continue
                    val color = Color(argb.toInt())
                    drawRect(
                        color = color.copy(alpha = color.alpha * alpha.coerceIn(0f, 1f)),
                        topLeft = Offset(
                            x = topLeft.x + x * cellWidth,
                            y = topLeft.y + y * cellHeight,
                        ),
                        size = Size(cellWidth + PIXEL_OVERDRAW, cellHeight + PIXEL_OVERDRAW),
                    )
                }
            }
        }
    }
}

private fun decodeCustomSwitchImage(file: File): ImageBitmap? {
    val cacheKey = runCatching {
        "${file.canonicalPath}:${file.length()}:${file.lastModified()}"
    }.getOrElse {
        "${file.absolutePath}:${file.length()}:${file.lastModified()}"
    }
    synchronized(customSwitchImageCacheLock) {
        customSwitchImageCache[cacheKey]?.let { return it }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (
            bounds.outWidth / sampleSize > MAX_RENDERED_SWITCH_IMAGE_SIDE ||
            bounds.outHeight / sampleSize > MAX_RENDERED_SWITCH_IMAGE_SIDE
        ) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
            ?: return null
        customSwitchImageCache[cacheKey] = decoded
        return decoded
    }
}

private fun DrawScope.drawMotionScan(rule: PixelMotionRule, progress: Float, alpha: Float) {
    if (
        !rule.enabled ||
        rule.mode != PixelMotionMode.Scan ||
        !progress.isActiveMotionProgress() ||
        alpha <= 0f
    ) return
    val y = size.height * progress.coerceIn(0f, 1f)
    drawRect(
        color = Color.White.copy(alpha = alpha),
        topLeft = Offset(0f, y),
        size = Size(size.width, 1.dp.toPx()),
    )
}

private fun DrawScope.drawSwitchImage(
    image: ImageBitmap,
    appearance: SwitchImageAppearance,
    topLeft: Offset,
    destinationSize: Size,
    alpha: Float,
) {
    val sourceWidth = image.width
    val sourceHeight = image.height
    if (sourceWidth <= 0 || sourceHeight <= 0 || alpha <= 0f) return
    val sourceAspect = sourceWidth.toFloat() / sourceHeight
    val destinationAspect = destinationSize.width / destinationSize.height
    val sourceSize = when (appearance.scale) {
        SwitchImageScale.Fit -> androidx.compose.ui.unit.IntSize(sourceWidth, sourceHeight)
        SwitchImageScale.Crop -> if (sourceAspect > destinationAspect) {
            androidx.compose.ui.unit.IntSize(
                (sourceHeight * destinationAspect).toInt().coerceIn(1, sourceWidth),
                sourceHeight,
            )
        } else {
            androidx.compose.ui.unit.IntSize(
                sourceWidth,
                (sourceWidth / destinationAspect).toInt().coerceIn(1, sourceHeight),
            )
        }
    }
    val sourceOffset = androidx.compose.ui.unit.IntOffset(
        x = ((sourceWidth - sourceSize.width) / 2).coerceAtLeast(0),
        y = ((sourceHeight - sourceSize.height) / 2).coerceAtLeast(0),
    )
    val baseDestination = if (appearance.scale == SwitchImageScale.Fit) {
        val fitted = if (sourceAspect > destinationAspect) {
            androidx.compose.ui.unit.IntSize(
                destinationSize.width.toInt().coerceAtLeast(1),
                (destinationSize.width / sourceAspect).toInt().coerceAtLeast(1),
            )
        } else {
            androidx.compose.ui.unit.IntSize(
                (destinationSize.height * sourceAspect).toInt().coerceAtLeast(1),
                destinationSize.height.toInt().coerceAtLeast(1),
            )
        }
        fitted
    } else {
        androidx.compose.ui.unit.IntSize(destinationSize.width.toInt(), destinationSize.height.toInt())
    }
    val destination = androidx.compose.ui.unit.IntSize(
        width = (baseDestination.width * appearance.zoom).toInt().coerceAtLeast(1),
        height = (baseDestination.height * appearance.zoom).toInt().coerceAtLeast(1),
    )
    val center = Offset(
        x = topLeft.x + destinationSize.width / 2f + appearance.offsetX * destinationSize.width / 2f,
        y = topLeft.y + destinationSize.height / 2f + appearance.offsetY * destinationSize.height / 2f,
    )
    val destinationOffset = androidx.compose.ui.unit.IntOffset(
        x = (center.x - destination.width / 2f).toInt(),
        y = (center.y - destination.height / 2f).toInt(),
    )
    val colorMatrix = ColorMatrix().apply {
        setToSaturation(appearance.saturation)
        val brightnessShift = appearance.brightness * 255f
        this[0, 4] = brightnessShift
        this[1, 4] = brightnessShift
        this[2, 4] = brightnessShift
        appearance.tint?.let { argb ->
            val tint = Color(argb.toInt())
            for (column in 0..4) {
                this[0, column] *= tint.red
                this[1, column] *= tint.green
                this[2, column] *= tint.blue
                this[3, column] *= tint.alpha
            }
        }
    }
    withTransform({
        rotate(appearance.rotationDegrees, pivot = center)
        scale(
            scaleX = if (appearance.flipHorizontal) -1f else 1f,
            scaleY = if (appearance.flipVertical) -1f else 1f,
            pivot = center,
        )
    }) {
        drawImage(
            image = image,
            srcOffset = sourceOffset,
            srcSize = sourceSize,
            dstOffset = destinationOffset,
            dstSize = destination,
            alpha = alpha.coerceIn(0f, 1f),
            colorFilter = ColorFilter.colorMatrix(colorMatrix),
            blendMode = when (appearance.blend) {
                SwitchImageBlend.Normal -> BlendMode.SrcOver
                SwitchImageBlend.Multiply -> BlendMode.Multiply
                SwitchImageBlend.Screen -> BlendMode.Screen
                SwitchImageBlend.Add -> BlendMode.Plus
            },
        )
    }
}

private fun DrawScope.drawMissingSwitchImage(topLeft: Offset, destinationSize: Size, alpha: Float) {
    val color = Color(0xFFFF5B6E).copy(alpha = alpha.coerceIn(0f, 1f) * 0.88f)
    drawLine(
        color = color,
        start = topLeft + Offset(destinationSize.width * 0.24f, destinationSize.height * 0.26f),
        end = topLeft + Offset(destinationSize.width * 0.76f, destinationSize.height * 0.74f),
        strokeWidth = 2.dp.toPx(),
    )
    drawLine(
        color = color,
        start = topLeft + Offset(destinationSize.width * 0.76f, destinationSize.height * 0.26f),
        end = topLeft + Offset(destinationSize.width * 0.24f, destinationSize.height * 0.74f),
        strokeWidth = 2.dp.toPx(),
    )
}

private fun motionAlpha(rule: PixelMotionRule, progress: Float): Float {
    if (!rule.enabled || rule.mode != PixelMotionMode.Pulse || !progress.isActiveMotionProgress()) return 1f
    return 0.66f + 0.34f * abs(sin(progress.coerceIn(0f, 1f) * PI.toFloat()))
}

private fun motionOffset(rule: PixelMotionRule, progress: Float, cellWidth: Float): Float {
    if (
        !rule.enabled ||
        rule.mode != PixelMotionMode.Drift ||
        rule.amplitudeCells == 0 ||
        !progress.isActiveMotionProgress()
    ) return 0f
    return sin(progress.coerceIn(0f, 1f) * PI.toFloat() * 2f) * cellWidth * rule.amplitudeCells
}

private fun Float.isActiveMotionProgress(): Boolean = isFinite() && this >= 0f

private const val PIXEL_OVERDRAW = 0.35f
private const val MAX_RENDERED_SWITCH_IMAGE_SIDE = 512
private const val MAX_CACHED_SWITCH_IMAGES = 4
private const val INACTIVE_MOTION_PROGRESS = -1f
private val customSwitchImageCacheLock = Any()
private val customSwitchImageCache = object : LinkedHashMap<String, ImageBitmap>(
    MAX_CACHED_SWITCH_IMAGES,
    0.75f,
    true,
) {
    override fun removeEldestEntry(
        eldest: MutableMap.MutableEntry<String, ImageBitmap>?,
    ): Boolean = size > MAX_CACHED_SWITCH_IMAGES
}
