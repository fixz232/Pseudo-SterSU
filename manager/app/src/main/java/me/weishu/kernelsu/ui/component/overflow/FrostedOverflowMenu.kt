package me.weishu.kernelsu.ui.component.overflow

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.component.liquid.LocalLiquidGlassBackdrop
import me.weishu.kernelsu.ui.theme.UiPassthroughTheme
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.util.UiPassthroughSettings
import me.weishu.kernelsu.ui.util.currentAppWindowSize
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported

private val LocalOverflowMenuOpacity = staticCompositionLocalOf { 1f }

/** A single shared surface: blur the background, never the menu's text or icons. */
@Composable
internal fun FrostedOverflowMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    wallpaper: OverflowMenuWallpaperState,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val window = currentAppWindowSize()
    val darkMode = isInDarkTheme()
    val contentColor = overflowMenuContentColor(darkMode)
    val canBlur = isRenderEffectSupported()
    val pageBackdrop = LocalOverflowMenuBackdrop.current
    val backdrop = pageBackdrop?.backdrop ?: LocalLiquidGlassBackdrop.current
    val shape = RoundedCornerShape(16.dp)
    val hasBlurredBackground = canBlur && (backdrop != null || wallpaper.bitmap != null)
    val opacity = sanitizeOverflowMenuOpacity(wallpaper.opacity)
    // Restore original theme colors and opaque foregrounds even with global UI passthrough.
    UiPassthroughTheme(UiPassthroughSettings()) {
        DropdownMenuPopup(expanded = expanded, onDismissRequest = onDismissRequest) {
            // Keep the source recorded through the popup's exit animation too.
            DisposableEffect(pageBackdrop) {
                val release = pageBackdrop?.acquireCapture()
                onDispose { release?.invoke() }
            }
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShapes(),
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                contentPadding = PaddingValues(0.dp),
            ) {
                Box(
                    Modifier.widthIn(max = overflowMenuWidth(window.widthDp, window.fontScale).dp)
                        .width(IntrinsicSize.Max)
                        .heightIn(max = overflowMenuMaxHeight(window.heightDp).dp)
                        .clip(shape),
                ) {
                    // Only the background layer fades; actions and icons stay fully opaque.
                    Box(Modifier.matchParentSize().graphicsLayer { alpha = opacity }) {
                        if (backdrop != null && wallpaper.bitmap == null) {
                            Box(Modifier.matchParentSize().drawBackdrop(
                                backdrop = backdrop,
                                shape = { shape },
                                effects = { blur(20.dp.toPx(), 20.dp.toPx()) },
                            ))
                        }
                        wallpaper.bitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                                    .blur(12.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle),
                            )
                        }
                        Box(Modifier.matchParentSize().background(overflowMenuTint(darkMode, hasBlurredBackground)))
                        if (hasBlurredBackground) {
                            Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = overflowMenuHighlightAlpha(darkMode)),
                                    Color.Transparent,
                                ),
                            )))
                        }
                    }
                    CompositionLocalProvider(
                        LocalContentColor provides contentColor,
                        LocalOverflowMenuOpacity provides opacity,
                    ) {
                        Column(Modifier.fillMaxWidth().verticalScroll(scrollState)
                            .padding(MenuDefaults.DropdownMenuGroupContentPadding)) {
                            content()
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CompactOverflowMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    index: Int = 0,
    count: Int = 1,
) {
    val opacity = LocalOverflowMenuOpacity.current
    val colors = MenuDefaults.selectableItemColors()
    SelectableDropdownMenuItem(
        text = text,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        shapes = MenuDefaults.itemShape(index = index, count = count),
        colors = colors.copy(
            containerColor = colors.containerColor.copy(alpha = colors.containerColor.alpha * opacity),
            selectedContainerColor = colors.selectedContainerColor.copy(alpha = colors.selectedContainerColor.alpha * opacity),
            disabledContainerColor = colors.disabledContainerColor.copy(alpha = colors.disabledContainerColor.alpha * opacity),
        ),
        leadingIcon = leadingIcon,
        selectedLeadingIcon = {
            Icon(Icons.Filled.Check, contentDescription = null,
                modifier = Modifier.size(MenuDefaults.LeadingIconSize))
        },
    )
}
