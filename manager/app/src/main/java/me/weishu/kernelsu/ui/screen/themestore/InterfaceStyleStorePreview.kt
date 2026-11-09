package me.weishu.kernelsu.ui.screen.themestore

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.util.InstalledInterfaceStyle
import me.weishu.kernelsu.ui.util.InterfaceStylePackage
import me.weishu.kernelsu.ui.util.InterfaceStyleRegistry
import me.weishu.kernelsu.ui.util.InterfaceStyleTheme

private data class StyleCover(
    val id: String,
    val theme: InterfaceStyleTheme?,
    val wallpaper: ImageBitmap?,
)

/** Static artwork never starts a scene, applies a style, or downloads a package. */
@Composable
internal fun InterfaceStyleStorePreview(style: InterfaceStylePackage, installed: InstalledInterfaceStyle?) {
    val context = LocalContext.current
    val cover by produceState<StyleCover?>(null, context, style.id, installed?.installedAt, installed?.style?.sha256) {
        value = null
        if (installed != null) value = withContext(Dispatchers.IO) {
            val registry = InterfaceStyleRegistry(context)
            val theme = registry.theme(style.engine, style.variant, style.id)
            val wallpaper = registry.resourceFile(style.id, "wallpaper.jpg")?.let { file -> runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
                val options = BitmapFactory.Options().apply { inSampleSize = 1 }
                while (maxOf(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 640) {
                    options.inSampleSize *= 2
                }
                BitmapFactory.decodeFile(file.path, options)?.asImageBitmap()
            }.getOrNull() }
            StyleCover(style.id, theme, wallpaper)
        }
    }
    val artwork = cover?.takeIf { it.id == style.id }
    val dark = isInDarkTheme() || artwork?.theme?.forceDark == true
    val palette = artwork?.theme?.let { if (dark) it.darkPalette else it.lightPalette }
    val accent = Color(style.accent.toInt()).copy(alpha = 1f)
    val base = if (dark) Color(0xFF111820) else Color(0xFFEDF2F6)
    val background = palette?.let { Color(it.background.toInt()) } ?: lerp(base, accent, 0.12f)
    val backgroundAlt = palette?.let { Color(it.backgroundAlt.toInt()) } ?: lerp(base, accent, 0.26f)
    val panel = palette?.let { Color(it.surface.toInt()) } ?: if (dark) Color(0xFF25323E) else Color.White
    val ink = palette?.let { Color(it.content.toInt()) } ?: if (dark) Color.White else Color(0xFF1D2935)
    val label = stringResource(R.string.interface_style_preview_artwork)
    Box(Modifier.fillMaxWidth().aspectRatio(2.1f).background(background)
        .semantics { contentDescription = label }) {
        artwork?.wallpaper?.let {
            Image(it, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        }
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            if (artwork?.wallpaper == null) drawRect(Brush.linearGradient(listOf(background, backgroundAlt)))
            when (style.engine) {
                "rain" -> {
                    repeat(32) { i ->
                        val x = ((i * 37) % 101) / 100f * w
                        val y = ((i * 23) % 97) / 100f * h
                        drawLine(accent.copy(alpha = 0.48f), Offset(x, y), Offset(x - w * 0.018f, y + h * 0.12f),
                            strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round)
                    }
                    if (style.variant == "thunderstorm" || style.id == "rain-environment") {
                        val bolt = Path().apply {
                            moveTo(w * 0.86f, h * 0.08f); lineTo(w * 0.81f, h * 0.30f)
                            lineTo(w * 0.86f, h * 0.29f); lineTo(w * 0.78f, h * 0.60f)
                            lineTo(w * 0.81f, h * 0.37f); lineTo(w * 0.76f, h * 0.39f); close()
                        }
                        drawPath(bolt, Color(0xFFE1EEFF))
                    }
                }
                "pixel" -> repeat(42) { i ->
                    val cell = 4.dp.toPx()
                    val x = ((i * 17) % 61) / 60f * w
                    val y = ((i * 31) % 53) / 52f * h
                    drawRect(accent.copy(alpha = if (i % 3 == 0) 0.60f else 0.18f),
                        Offset((x / cell).toInt() * cell, (y / cell).toInt() * cell), Size(cell, cell))
                }
                "snow" -> repeat(20) { i ->
                    val color = if (style.variant == "winter") Color.White else accent
                    drawCircle(color.copy(alpha = 0.45f), (2 + i % 4).dp.toPx(),
                        Offset(((i * 29) % 97) / 96f * w, ((i * 43) % 89) / 88f * h))
                }
                else -> {
                    drawCircle(accent.copy(alpha = 0.16f), h * 0.65f, Offset(w * 0.88f, h * 0.18f))
                    drawCircle(accent.copy(alpha = 0.10f), h * 0.48f, Offset(w * 0.10f, h * 0.90f))
                }
            }
            // A small page shows the card/navigation language without pretending to be a screenshot.
            val pixel = style.engine == "pixel"
            val radius = CornerRadius(if (pixel) 2.dp.toPx() else 9.dp.toPx())
            fun block(x: Float, y: Float, width: Float, height: Float, color: Color) {
                drawRoundRect(color, Offset(w * x, h * y), Size(w * width, h * height), radius)
            }
            block(0.14f, 0.14f, 0.62f, 0.73f, panel.copy(alpha = if (style.engine == "liquid_glass") 0.60f else 0.94f))
            block(0.19f, 0.22f, 0.20f, 0.035f, ink.copy(alpha = 0.82f))
            block(0.19f, 0.32f, 0.52f, 0.16f, accent.copy(alpha = 0.26f))
            block(0.22f, 0.365f, 0.20f, 0.025f, ink.copy(alpha = 0.72f))
            block(0.19f, 0.53f, 0.24f, 0.18f, ink.copy(alpha = 0.08f))
            block(0.47f, 0.53f, 0.24f, 0.18f, ink.copy(alpha = 0.08f))
            if (style.engine == "sidebar_widget") {
                block(0.79f, 0.14f, 0.08f, 0.73f, panel.copy(alpha = 0.90f))
                repeat(4) { i -> drawCircle(accent, 2.5.dp.toPx(), Offset(w * 0.83f, h * (0.29f + i * 0.14f))) }
            } else {
                block(0.19f, 0.77f, 0.52f, 0.035f, ink.copy(alpha = 0.10f))
                block(0.19f, 0.77f, 0.13f, 0.035f, accent)
            }
        }
        Text(label, Modifier.align(Alignment.TopEnd).padding(8.dp)
            .clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.72f))
            .padding(horizontal = 7.dp, vertical = 4.dp), color = Color.White,
            style = MaterialTheme.typography.labelSmall)
    }
}
