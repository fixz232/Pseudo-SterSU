package me.weishu.kernelsu.ui.component.overflow

import androidx.compose.ui.graphics.Color
import me.weishu.kernelsu.ui.util.CustomWallpaperCrop
import me.weishu.kernelsu.ui.util.FULL_CUSTOM_WALLPAPER_CROP
import me.weishu.kernelsu.ui.util.sanitizeCustomWallpaperCrop

internal fun overflowMenuWidth(availableWidthDp: Float, fontScale: Float): Float {
    val width = availableWidthDp.takeIf(Float::isFinite) ?: 0f
    val scale = fontScale.takeIf(Float::isFinite)?.coerceIn(1f, 1.25f) ?: 1f
    return (232f * scale).coerceAtMost((width - 24f).coerceAtLeast(0f))
}

internal fun overflowMenuMaxHeight(availableHeightDp: Float): Float =
    ((availableHeightDp.takeIf(Float::isFinite) ?: 0f) - 24f).coerceAtLeast(0f)

internal fun overflowMenuTint(darkMode: Boolean, hasBlurredBackground: Boolean): Color =
    (if (darkMode) Color(0xFF19191B) else Color(0xFFF7F7F7))
        .copy(alpha = if (hasBlurredBackground) (if (darkMode) 0.68f else 0.64f) else 1f)

internal fun overflowMenuContentColor(darkMode: Boolean): Color =
    if (darkMode) Color(0xFFF7F7F7) else Color(0xFF202022)

internal fun overflowMenuHighlightAlpha(darkMode: Boolean): Float =
    if (darkMode) 0.015f else 0.05f

internal fun sanitizeOverflowMenuOpacity(value: Float): Float =
    if (value.isFinite()) value.coerceIn(0f, 1f) else 1f

internal enum class OverflowMenuTarget(val wallpaperKey: String) {
    Superuser("superuser_overflow_menu_wallpaper_uri"),
    Module("module_overflow_menu_wallpaper_uri");

    val cropKey: String get() = "${wallpaperKey}_crop"
    val opacityKey: String get() = "${wallpaperKey}_opacity"
}

internal fun decodeOverflowMenuCrop(value: String?): CustomWallpaperCrop {
    val parts = value?.split(',')?.map { it.toFloatOrNull() } ?: return FULL_CUSTOM_WALLPAPER_CROP
    if (parts.size != 4 || parts.any { it == null || !it.isFinite() }) return FULL_CUSTOM_WALLPAPER_CROP
    return sanitizeCustomWallpaperCrop(CustomWallpaperCrop(parts[0]!!, parts[1]!!, parts[2]!!, parts[3]!!))
}

internal fun encodeOverflowMenuCrop(crop: CustomWallpaperCrop): String =
    sanitizeCustomWallpaperCrop(crop).let { "${it.left},${it.top},${it.right},${it.bottom}" }
