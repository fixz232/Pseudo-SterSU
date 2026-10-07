package me.weishu.kernelsu.ui.screen.settings

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor

private const val SETTINGS_PAGE_MODE_KEY = "settings_page_mode"

enum class SettingsPageMode(val value: String) {
    Collapsed("collapsed"),
    Categories("categories"),
    Overview("overview");

    companion object {
        fun fromValue(value: String?): SettingsPageMode {
            return entries.firstOrNull { it.value == value } ?: Categories
        }
    }
}

fun readSettingsPageMode(context: Context): SettingsPageMode {
    val value = context.applicationContext
        .getSharedPreferences("settings", Context.MODE_PRIVATE)
        .getString(SETTINGS_PAGE_MODE_KEY, null)
    return SettingsPageMode.fromValue(value)
}

fun setSettingsPageMode(context: Context, mode: SettingsPageMode) {
    context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE).edit {
        putString(SETTINGS_PAGE_MODE_KEY, mode.value)
    }
}

@Composable
fun SettingsPageModeButton(
    currentMode: SettingsPageMode,
    onModeChange: (SettingsPageMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val contentDescription = stringResource(R.string.settings_page_mode_select)
    Box(modifier = modifier) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                positioning = TooltipAnchorPosition.Below,
            ),
            tooltip = {
                PlainTooltip { Text(contentDescription) }
            },
            state = rememberTooltipState(),
        ) {
            Surface(
                color = immersiveSurfaceColor(
                    defaultColor = MaterialTheme.colorScheme.surfaceContainer,
                    darkAlpha = 0.62f,
                    lightAlpha = 0.72f,
                ),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 2.dp,
            ) {
                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (currentMode) {
                                SettingsPageMode.Collapsed -> Icons.Rounded.ViewAgenda
                                SettingsPageMode.Categories -> Icons.Rounded.AccountTree
                                SettingsPageMode.Overview -> Icons.Rounded.Dashboard
                            },
                            contentDescription = contentDescription,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            SettingsPageMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                when (mode) {
                                    SettingsPageMode.Collapsed -> R.string.settings_page_mode_collapsed
                                    SettingsPageMode.Categories -> R.string.settings_page_mode_categories
                                    SettingsPageMode.Overview -> R.string.settings_page_mode_overview
                                }
                            )
                        )
                    },
                    onClick = {
                        expanded = false
                        if (mode != currentMode) onModeChange(mode)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = when (mode) {
                                SettingsPageMode.Collapsed -> Icons.Rounded.ViewAgenda
                                SettingsPageMode.Categories -> Icons.Rounded.AccountTree
                                SettingsPageMode.Overview -> Icons.Rounded.Dashboard
                            },
                            contentDescription = null,
                        )
                    },
                    trailingIcon = {
                        if (mode == currentMode) Icon(Icons.Rounded.Check, contentDescription = null)
                    },
                )
            }
        }
    }
}
