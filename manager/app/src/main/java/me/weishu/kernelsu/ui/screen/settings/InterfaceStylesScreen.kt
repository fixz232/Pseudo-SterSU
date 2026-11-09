package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.component.material.ConnectedTabRow
import me.weishu.kernelsu.ui.component.passthrough.MaterialIcon as Icon
import me.weishu.kernelsu.ui.component.passthrough.MaterialText as Text
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.screen.themestore.InterfaceStyleStoreContent
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor
import me.weishu.kernelsu.ui.viewmodel.SettingsViewModel

/** Installed styles and the catalog share one destination and keep independent scroll positions. */
@Composable
fun InterfaceStylesScreen(initialStore: Boolean = false) {
    val navigator = LocalNavigator.current
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable(initialStore) { mutableIntStateOf(if (initialStore) 1 else 0) }
    var selectedStyleId by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedFamily by rememberSaveable { mutableStateOf<String?>(null) }
    val installedScroll = rememberScrollState()
    val catalogScroll = rememberScrollState()
    val detailScroll = rememberScrollState()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    LaunchedEffect(selectedStyleId) {
        if (selectedStyleId != null) detailScroll.scrollTo(0)
    }
    val onBack = dropUnlessResumed {
        if (selectedStyleId != null) selectedStyleId = null else navigator.pop()
    }
    SettingsCategoryScaffold(
        category = SettingsCategory.Appearance,
        title = stringResource(if (selectedStyleId == null) R.string.settings_ui_mode else R.string.interface_style_details),
        onBack = onBack,
        miuix = uiState.uiMode != InterfaceStyle.Material.value,
        scrollState = if (tab == 0) installedScroll else if (selectedStyleId == null) catalogScroll else detailScroll,
    ) {
        if (selectedStyleId == null) {
            Text(stringResource(R.string.interface_styles_workspace_summary),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            InterfaceStyleTabs(tab) { tab = it }
        }
        if (tab == 1) {
            InterfaceStyleStoreContent(
                selectedStyleId = selectedStyleId,
                onSelectStyle = { selectedStyleId = it },
                onInstalled = { viewModel.refresh() },
                onConfigureSidebar = dropUnlessResumed { navigator.push(Route.SidebarWidgetSettings) },
                stacked = true,
            )
        } else if (!uiState.appearanceResolved) {
            androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth())
        } else {
            val groups = uiState.interfaceStylePickerGroups()
            val selectedIndex = uiState.interfaceStyleSelectedIndex()
            val selectedGroupIndex = uiState.interfaceStylePickerSelectedIndex(groups)
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                groups.forEachIndexed { groupIndex, group ->
                    val title = group.labelRes?.let { stringResource(it) } ?: group.packageName.orEmpty()
                    val active = groupIndex == selectedGroupIndex
                    InterfaceStyleStackCard(
                        title = title,
                        summary = stringResource(when {
                            group.family != null -> R.string.interface_styles_variants
                            group.key.startsWith("builtin:") -> R.string.interface_styles_builtin
                            else -> R.string.interface_styles_installed
                        }, group.selectionIndices.size),
                        active = active,
                        family = group.family != null,
                        expanded = expandedFamily == group.key,
                        sidebar = group.key == "builtin:${InterfaceStyle.SidebarWidget.value}",
                        onClick = {
                            if (group.family != null) {
                                expandedFamily = if (expandedFamily == group.key) null else group.key
                            } else group.selectionIndices.firstOrNull()?.let { index ->
                                uiState.applyInterfaceStyleSelection(
                                    index,
                                    { viewModel.setUiMode(InterfaceStyle.fromIndex(it).value) },
                                    viewModel::applyInterfaceStylePackage,
                                )
                            }
                        },
                    ) {
                        group.selectionIndices.forEach variant@ { index ->
                            val installed = uiState.installedInterfaceStyles
                                .getOrNull(index - InterfaceStyle.selectableEntries.size) ?: return@variant
                            Row(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                                    .selectable(
                                        selected = selectedIndex == index,
                                        role = Role.RadioButton,
                                        onClick = { viewModel.applyInterfaceStylePackage(installed.style) },
                                    ).padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = selectedIndex == index, onClick = null)
                                Spacer(Modifier.width(10.dp))
                                Text(installed.style.name, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InterfaceStyleTabs(selectedTab: Int, onSelected: (Int) -> Unit) {
    ConnectedTabRow(
        tabs = listOf(stringResource(R.string.interface_styles_mine), stringResource(R.string.interface_style_store_title)),
        selectedTabIndex = selectedTab,
        onTabSelected = onSelected,
    )
}

@Composable
private fun InterfaceStyleStackCard(
    title: String,
    summary: String,
    active: Boolean,
    family: Boolean,
    expanded: Boolean,
    sidebar: Boolean,
    onClick: () -> Unit,
    variants: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val colors = MaterialTheme.colorScheme
    val sidebarStyle = LocalInterfaceStyle.current == InterfaceStyle.SidebarWidget.value
    val surface = immersiveSurfaceColor(if (sidebarStyle) colors.surfaceContainerHigh else colors.surfaceContainer)
    Box(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Surface(
            shape = shape,
            color = surface,
            border = BorderStroke(1.dp, if (active) colors.primary.copy(alpha = 0.5f) else colors.outlineVariant.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val showPreview = maxWidth >= 240.dp
                    val compactPreview = maxWidth < 300.dp
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)
                            .clickable(role = if (family) Role.Button else Role.RadioButton, onClick = onClick)
                            .semantics { selected = active }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        if (showPreview) StackedStylePreview(sidebar, compact = compactPreview)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(if (active) stringResource(R.string.theme_store_applied) else summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        if (family) {
                            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, Modifier.size(22.dp))
                        } else if (active) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
                AnimatedVisibility(visible = family && expanded, enter = expandVertically(), exit = shrinkVertically()) {
                    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp).selectableGroup()) { variants() }
                }
            }
        }
    }
}

/** A compact stack previews the page, cards, and navigation without manager-logo decoration. */
@Composable
private fun StackedStylePreview(sidebar: Boolean, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    Canvas(Modifier.size(width = if (compact) 60.dp else 80.dp, height = if (compact) 52.dp else 68.dp)) {
        val w = size.width
        val h = size.height
        val radius = CornerRadius(7.dp.toPx())
        drawRoundRect(colors.primary.copy(alpha = 0.12f), Offset(w * 0.18f, 0f), Size(w * 0.82f, h * 0.80f), radius)
        drawRoundRect(colors.primary.copy(alpha = 0.20f), Offset(w * 0.09f, h * 0.08f), Size(w * 0.82f, h * 0.80f), radius)
        drawRoundRect(colors.surfaceContainerHighest, Offset(0f, h * 0.18f), Size(w * 0.82f, h * 0.80f), radius)
        drawRoundRect(colors.primary, Offset(w * 0.10f, h * 0.28f), Size(w * 0.36f, h * 0.04f), radius)
        val start = if (sidebar) 0.22f else 0.10f
        if (sidebar) drawRoundRect(colors.primary.copy(alpha = 0.4f), Offset(w * 0.08f, h * 0.40f), Size(w * 0.08f, h * 0.46f), radius)
        drawRoundRect(colors.primary.copy(alpha = 0.22f), Offset(w * start, h * 0.40f), Size(w * (0.72f - start), h * 0.20f), radius)
        drawRoundRect(colors.onSurface.copy(alpha = 0.12f), Offset(w * start, h * 0.65f), Size(w * (0.72f - start), h * 0.15f), radius)
        if (!sidebar) drawRoundRect(colors.primary.copy(alpha = 0.45f), Offset(w * 0.20f, h * 0.88f), Size(w * 0.40f, h * 0.04f), radius)
    }
}
