package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.component.passthrough.MaterialText as Text
import me.weishu.kernelsu.ui.util.adaptiveDialogContentMaxHeight

/** Every settings renderer shares the same family list and package selection mapping. */
@Composable
internal fun WithGroupedInterfaceStylePicker(
    uiState: SettingsUiState,
    onSelected: (Int) -> Unit,
    content: @Composable (labels: List<String>, selectedIndex: Int, select: (Int) -> Unit) -> Unit,
) {
    val groups = uiState.interfaceStylePickerGroups()
    val labels = groups.map { group ->
        group.labelRes?.let { stringResource(it) } ?: group.packageName.orEmpty()
    }
    var pendingFamily by rememberSaveable { mutableStateOf<String?>(null) }
    content(labels, uiState.interfaceStylePickerSelectedIndex(groups)) { index ->
        groups.getOrNull(index)?.let { group ->
            if (group.family != null) pendingFamily = group.key
            else group.selectionIndices.firstOrNull()?.let(onSelected)
        }
    }
    val groupIndex = groups.indexOfFirst { it.key == pendingFamily }
    val group = groups.getOrNull(groupIndex) ?: return
    val selectedIndex = uiState.interfaceStyleSelectedIndex()
    AlertDialog(
        onDismissRequest = { pendingFamily = null },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 1f),
        title = { Text(labels[groupIndex]) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .heightIn(max = adaptiveDialogContentMaxHeight())
                    .verticalScroll(rememberScrollState())
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                group.selectionIndices.forEach { index ->
                    val style = uiState.installedInterfaceStyles
                        .getOrNull(index - InterfaceStyle.selectableEntries.size)
                        ?.style ?: return@forEach
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .selectable(
                                selected = index == selectedIndex,
                                role = Role.RadioButton,
                                onClick = {
                                    pendingFamily = null
                                    onSelected(index)
                                },
                            ).padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = index == selectedIndex, onClick = null)
                        Spacer(Modifier.width(10.dp))
                        Text(style.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { pendingFamily = null }) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
