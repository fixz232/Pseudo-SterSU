package me.weishu.kernelsu.ui.screen.superuser

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.overflow.CompactOverflowMenuItem
import me.weishu.kernelsu.ui.component.overflow.FrostedOverflowMenu
import me.weishu.kernelsu.ui.component.overflow.OverflowMenuWallpaperState
import me.weishu.kernelsu.ui.viewmodel.DEFAULT_SUPERUSER_SORT_OPTION

@Composable
internal fun SuperUserOverflowMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    uiState: SuperUserUiState,
    actions: SuperUserActions,
    showTools: Boolean,
    wallpaper: OverflowMenuWallpaperState,
) {
    var showSortMenu by remember { mutableStateOf(false) }
    val menuScrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(expanded, showSortMenu) {
        if (expanded) menuScrollState.scrollTo(0) else showSortMenu = false
    }
    FrostedOverflowMenu(expanded, onDismissRequest, wallpaper, menuScrollState) {
        val itemCount = if (showSortMenu) 6 else 2 +
            (if (uiState.userIds.size > 1) 1 else 0) + (if (showTools) 2 else 0)
        var itemIndex = 0
        if (showSortMenu) {
            CompactOverflowMenuItem(
                text = { Text(stringResource(R.string.menu_sort), fontWeight = FontWeight.SemiBold) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(20.dp)) },
                onClick = { showSortMenu = false },
                index = itemIndex++,
                count = itemCount,
            )
            val sortType = uiState.sortOption / 2
            val reversed = uiState.sortOption % 2 != 0
            listOf(
                2 to R.string.sort_by_install_time,
                0 to R.string.sort_by_name,
                1 to R.string.sort_by_package_name,
                3 to R.string.sort_by_update_time,
            ).forEach { (type, label) ->
                CompactOverflowMenuItem(
                    text = { Text(stringResource(label), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.semantics { selected = sortType == type },
                    selected = sortType == type,
                    index = itemIndex++,
                    count = itemCount,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        actions.onUpdateSortOption(if (type == 2) DEFAULT_SUPERUSER_SORT_OPTION else type * 2 + if (reversed) 1 else 0)
                        onDismissRequest()
                    },
                )
            }
            SuperUserCheckableMenuItem(R.string.sort_reverse, reversed, itemIndex, itemCount) {
                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                actions.onUpdateSortOption(sortType * 2 + if (reversed) 0 else 1)
                onDismissRequest()
            }
        } else {
            CompactOverflowMenuItem(
                text = { Text(stringResource(R.string.superuser_sort_menu, stringResource(superUserSortLabel(uiState.sortOption))), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                onClick = { showSortMenu = true },
                index = itemIndex++,
                count = itemCount,
            )
            SuperUserCheckableMenuItem(R.string.show_system_apps, uiState.showSystemApps, itemIndex++, itemCount) {
                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                actions.onToggleShowSystemApps()
                onDismissRequest()
            }
            if (uiState.userIds.size > 1) {
                SuperUserCheckableMenuItem(R.string.show_only_primary_user_apps, uiState.showOnlyPrimaryUserApps, itemIndex++, itemCount) {
                    haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    actions.onToggleShowOnlyPrimaryUserApps()
                    onDismissRequest()
                }
            }
            if (showTools) {
                CompactOverflowMenuItem(
                    text = { Text(stringResource(R.string.settings_sulog), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    onClick = { onDismissRequest(); actions.onOpenSulog() },
                    index = itemIndex++,
                    count = itemCount,
                )
                CompactOverflowMenuItem(
                    text = { Text(stringResource(R.string.superuser_app_tools_title), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    onClick = { onDismissRequest(); actions.onOpenAppTools() },
                    index = itemIndex,
                    count = itemCount,
                )
            }
        }
    }
}

@Composable
private fun SuperUserCheckableMenuItem(label: Int, checked: Boolean, index: Int, count: Int, onClick: () -> Unit) {
    CompactOverflowMenuItem(
        text = { Text(stringResource(label), maxLines = 2, overflow = TextOverflow.Ellipsis) },
        modifier = Modifier.semantics { role = Role.Checkbox; toggleableState = ToggleableState(checked) },
        selected = checked,
        index = index,
        count = count,
        onClick = onClick,
    )
}
