package me.weishu.kernelsu.ui.component.liquid

// Keep the no-blur fallback legible instead of revealing unfiltered content.
internal fun frostedSurfaceAlpha(requested: Float, hasBlurredBackdrop: Boolean): Float =
    if (hasBlurredBackdrop) requested.coerceIn(0f, 1f) else 1f

internal fun frostedStrokeAlpha(requested: Float, darkMode: Boolean): Float =
    requested.coerceIn(0f, 1f) * if (darkMode) 0.38f else 0.48f

internal fun frostedBlurDp(requestedDp: Float, intensity: Float): Float =
    (requestedDp.coerceIn(0f, 48f) * intensity.coerceAtLeast(0f)).coerceIn(0f, 48f)
