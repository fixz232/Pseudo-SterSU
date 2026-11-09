package me.weishu.kernelsu.ui.screen.settings

import androidx.annotation.StringRes
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.util.InterfaceStylePackage

internal data class InterfaceStylePickerGroup(
    val key: String,
    @StringRes val labelRes: Int? = null,
    val packageName: String? = null,
    val family: InterfaceStyle? = null,
    val selectionIndices: List<Int>,
)

/** Group the presentation only; package identities and saved variants remain independent. */
internal fun SettingsUiState.interfaceStylePickerGroups(): List<InterfaceStylePickerGroup> {
    val builtIns = InterfaceStyle.selectableEntries
    val groups = builtIns.mapIndexed { index, style ->
        InterfaceStylePickerGroup(
            key = "builtin:${style.value}",
            labelRes = style.labelRes,
            selectionIndices = listOf(index),
        )
    }.toMutableList()
    installedInterfaceStyles.forEachIndexed { index, installed ->
        val family = when (installed.style.engine) {
            InterfaceStyle.Snow.value -> InterfaceStyle.Snow
            InterfaceStyle.Pixel.value -> InterfaceStyle.Pixel
            else -> null
        }
        val selectionIndex = builtIns.size + index
        val key = if (family != null) "family:${family.value}" else "package:${installed.style.id}"
        val existing = groups.indexOfFirst { it.key == key }
        if (existing >= 0) {
            groups[existing] = groups[existing].copy(
                selectionIndices = groups[existing].selectionIndices + selectionIndex,
            )
        } else {
            groups += InterfaceStylePickerGroup(
                key = key,
                labelRes = family?.labelRes,
                packageName = if (family == null) installed.style.name else null,
                family = family,
                selectionIndices = listOf(selectionIndex),
            )
        }
    }
    return groups
}

internal fun SettingsUiState.interfaceStylePickerSelectedIndex(
    groups: List<InterfaceStylePickerGroup> = interfaceStylePickerGroups(),
): Int {
    val familyIndex = groups.indexOfFirst { it.family?.value == uiMode }
    if (familyIndex >= 0) return familyIndex
    val selectionIndex = interfaceStyleSelectedIndex()
    return groups.indexOfFirst { selectionIndex in it.selectionIndices }.coerceAtLeast(0)
}

internal fun SettingsUiState.applyInterfaceStyleSelection(
    index: Int,
    onBuiltInSelected: (Int) -> Unit,
    onPackageSelected: (InterfaceStylePackage) -> Unit,
) {
    val builtInCount = InterfaceStyle.selectableEntries.size
    if (index in 0 until builtInCount) {
        onBuiltInSelected(index)
    } else {
        installedInterfaceStyles.getOrNull(index - builtInCount)?.style?.let(onPackageSelected)
    }
}
