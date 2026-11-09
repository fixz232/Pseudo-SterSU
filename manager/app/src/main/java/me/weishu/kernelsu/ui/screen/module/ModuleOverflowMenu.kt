package me.weishu.kernelsu.ui.screen.module

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.overflow.CompactOverflowMenuItem
import me.weishu.kernelsu.ui.component.overflow.FrostedOverflowMenu
import me.weishu.kernelsu.ui.component.overflow.OverflowMenuWallpaperState

@Composable
internal fun ModuleOverflowMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    uiState: ModuleUiState,
    actions: ModuleActions,
    wallpaper: OverflowMenuWallpaperState,
    onToggleFeedback: () -> Unit = {},
) {
    FrostedOverflowMenu(expanded, onDismissRequest, wallpaper) {
        listOf(
            Triple(R.string.module_sort_action_first, uiState.sortActionFirst, actions.onToggleSortActionFirst),
            Triple(R.string.module_sort_enabled_first, uiState.sortEnabledFirst, actions.onToggleSortEnabledFirst),
        ).forEachIndexed { index, (label, checked, toggle) ->
            CompactOverflowMenuItem(
                text = { Text(stringResource(label), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.semantics {
                    role = Role.Checkbox
                    toggleableState = ToggleableState(checked)
                },
                selected = checked,
                index = index,
                count = 2,
                onClick = {
                    onToggleFeedback()
                    toggle()
                    onDismissRequest()
                },
            )
        }
    }
}
