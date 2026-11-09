package me.weishu.kernelsu.ui.component.overflow

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.theme.UiPassthroughTheme
import me.weishu.kernelsu.ui.util.UiPassthroughSettings

/** Tap opens actions; long press edits only this page's menu appearance. */
@Composable
internal fun OverflowMenuAnchor(
    target: OverflowMenuTarget,
    icon: @Composable () -> Unit,
    menu: @Composable (Boolean, () -> Unit, OverflowMenuWallpaperState) -> Unit,
) {
    var expanded by remember(target) { mutableStateOf(false) }
    var showWallpaperEditor by remember(target) { mutableStateOf(false) }
    val wallpaper = rememberOverflowMenuWallpaper(target)
    val label = stringResource(R.string.overflow_menu_actions)
    val wallpaperLabel = stringResource(R.string.overflow_menu_appearance)

    Box {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current,
                    role = Role.Button,
                    onClick = { expanded = true },
                    onLongClickLabel = wallpaperLabel,
                    onLongClick = {
                        expanded = false
                        showWallpaperEditor = true
                    },
                )
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) { icon() }
        menu(expanded, { expanded = false }, wallpaper)
    }
    if (showWallpaperEditor) {
        // Wallpaper settings and crop controls must not inherit translucent foregrounds.
        UiPassthroughTheme(UiPassthroughSettings()) {
            OverflowMenuWallpaperDialog(wallpaper, onDismissRequest = { showWallpaperEditor = false })
        }
    }
}
