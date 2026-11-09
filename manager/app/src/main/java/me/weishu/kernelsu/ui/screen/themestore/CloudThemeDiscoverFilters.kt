package me.weishu.kernelsu.ui.screen.themestore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.passthrough.MaterialText as Text
import me.weishu.kernelsu.ui.component.store.StoreExpandableSection
import me.weishu.kernelsu.ui.util.CloudThemeBrowseFilter
import me.weishu.kernelsu.ui.util.CloudThemeCategory
import me.weishu.kernelsu.ui.util.CloudThemeCompatibility
import me.weishu.kernelsu.ui.util.CloudThemeDevice
import me.weishu.kernelsu.ui.util.CloudThemeDiscoverySelection
import me.weishu.kernelsu.ui.util.CloudThemeOrder
import me.weishu.kernelsu.ui.util.CloudThemeStyle
import me.weishu.kernelsu.ui.util.CloudThemeTone

@Composable
internal fun CloudThemeDiscoverFilters(
    categories: List<CloudThemeCategory>,
    selection: CloudThemeDiscoverySelection,
    onSelectionChange: (CloudThemeDiscoverySelection) -> Unit,
) {
    val all = stringResource(R.string.cloud_theme_filter_all)
    val universal = stringResource(R.string.cloud_theme_discovery_universal)
    val browseLabels = listOf(all, stringResource(R.string.cloud_theme_filter_featured),
        stringResource(R.string.cloud_theme_filter_updates), stringResource(R.string.cloud_theme_filter_favorites))
    val orderLabels = listOf(stringResource(R.string.cloud_theme_discovery_recommended),
        stringResource(R.string.cloud_theme_filter_latest), stringResource(R.string.cloud_theme_discovery_oldest),
        stringResource(R.string.cloud_theme_discovery_most_used), stringResource(R.string.cloud_theme_discovery_least_used))
    val toneLabels = listOf(all, stringResource(R.string.cloud_theme_discovery_dark),
        stringResource(R.string.cloud_theme_discovery_light), universal)
    val deviceLabels = listOf(all, stringResource(R.string.cloud_theme_discovery_phone),
        stringResource(R.string.cloud_theme_discovery_tablet), universal)
    val compatibilityLabels = listOf(all, stringResource(R.string.cloud_theme_discovery_compatible),
        stringResource(R.string.cloud_theme_discovery_incompatible))
    val styleLabels = CloudThemeStyle.entries.map {
        it.interfaceStyle?.let { style -> stringResource(style.labelRes) }
            ?: if (it == CloudThemeStyle.Universal) universal else all
    }
    val selectedLabels = buildList {
        if (selection.browse != CloudThemeBrowseFilter.All) add(browseLabels[selection.browse.ordinal])
        if (selection.order != CloudThemeOrder.Recommended) add(orderLabels[selection.order.ordinal])
        if (selection.tone != CloudThemeTone.All) add(toneLabels[selection.tone.ordinal])
        if (selection.device != CloudThemeDevice.All) add(deviceLabels[selection.device.ordinal])
        if (selection.compatibility != CloudThemeCompatibility.All) add(compatibilityLabels[selection.compatibility.ordinal])
        if (selection.style != CloudThemeStyle.All) add(styleLabels[selection.style.ordinal])
        categories.firstOrNull { it.id == selection.categoryId }?.name?.let { add(it) }
    }
    StoreExpandableSection(
        title = stringResource(R.string.store_redesign_filter_category),
        summary = selectedLabels.takeIf { it.isNotEmpty() }?.joinToString(" / ")
            ?: stringResource(R.string.cloud_theme_category_all),
    ) {
        DiscoveryFilterGroup(stringResource(R.string.cloud_theme_discovery_browse), browseLabels, selection.browse.ordinal) {
            onSelectionChange(selection.copy(browse = CloudThemeBrowseFilter.entries[it]))
        }
        DiscoveryFilterGroup(stringResource(R.string.cloud_theme_discovery_order), orderLabels, selection.order.ordinal) {
            onSelectionChange(selection.copy(order = CloudThemeOrder.entries[it]))
        }
        DiscoveryFilterGroup(stringResource(R.string.cloud_theme_discovery_tone), toneLabels, selection.tone.ordinal) {
            onSelectionChange(selection.copy(tone = CloudThemeTone.entries[it]))
        }
        DiscoveryFilterGroup(stringResource(R.string.cloud_theme_discovery_device), deviceLabels, selection.device.ordinal) {
            onSelectionChange(selection.copy(device = CloudThemeDevice.entries[it]))
        }
        DiscoveryFilterGroup(stringResource(R.string.cloud_theme_discovery_compatibility), compatibilityLabels, selection.compatibility.ordinal) {
            onSelectionChange(selection.copy(compatibility = CloudThemeCompatibility.entries[it]))
        }
        DiscoveryFilterGroup(stringResource(R.string.cloud_theme_discovery_style), styleLabels, selection.style.ordinal) {
            onSelectionChange(selection.copy(style = CloudThemeStyle.entries[it]))
        }
        if (categories.isNotEmpty()) {
            DiscoveryFilterGroup(
                stringResource(R.string.cloud_theme_discovery_category),
                listOf(stringResource(R.string.cloud_theme_category_all)) + categories.map { it.name },
                categories.indexOfFirst { it.id == selection.categoryId } + 1,
            ) { index -> onSelectionChange(selection.copy(categoryId = categories.getOrNull(index - 1)?.id)) }
        }
        Text(stringResource(R.string.cloud_theme_discovery_notice), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = { onSelectionChange(CloudThemeDiscoverySelection()) }) {
            Text(stringResource(R.string.store_redesign_clear_filters))
        }
    }
}

@Composable
private fun DiscoveryFilterGroup(title: String, labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            labels.forEachIndexed { index, label ->
                FilterChip(selected = index == selectedIndex, onClick = { onSelected(index) },
                    label = { Text(label) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
    }
}
