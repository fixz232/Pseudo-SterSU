package me.weishu.kernelsu.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ViewSidebar
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.bottombar.SidebarRailItem
import me.weishu.kernelsu.ui.component.bottombar.SidebarRailLayout
import me.weishu.kernelsu.ui.component.bottombar.LocalSidebarGlassBackdrop
import me.weishu.kernelsu.ui.component.bottombar.rememberSidebarGlassBackdrop
import me.weishu.kernelsu.ui.component.bottombar.sidebarGlassUnderlay
import me.weishu.kernelsu.ui.component.bottombar.sidebarPaneShape
import me.weishu.kernelsu.ui.component.bottombar.MainDestination
import me.weishu.kernelsu.ui.component.bottombar.NavigationDestinationIcon
import me.weishu.kernelsu.ui.component.bottombar.mainDestinations
import me.weishu.kernelsu.ui.component.bottombar.orderSidebarDestinations
import me.weishu.kernelsu.ui.component.bottombar.stateFor
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.util.LocalCustomNavigationIcons
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.util.SIDEBAR_NAVIGATION_IDS
import me.weishu.kernelsu.ui.util.SIDEBAR_NAV_HOME
import me.weishu.kernelsu.ui.util.SIDEBAR_NAV_KPM
import me.weishu.kernelsu.ui.util.SIDEBAR_NAV_MODULE
import me.weishu.kernelsu.ui.util.SIDEBAR_NAV_SETTINGS
import me.weishu.kernelsu.ui.util.SIDEBAR_NAV_SUPERUSER
import me.weishu.kernelsu.ui.util.SIDEBAR_WIDGET_IMAGE_STORAGE_KEY
import me.weishu.kernelsu.ui.util.SidebarClockStyle
import me.weishu.kernelsu.ui.util.SidebarImageShape
import me.weishu.kernelsu.ui.util.SidebarNavigationPosition
import me.weishu.kernelsu.ui.util.SidebarSide
import me.weishu.kernelsu.ui.util.SidebarWidgetConfig
import me.weishu.kernelsu.ui.util.SidebarWidgetType
import me.weishu.kernelsu.ui.util.persistCustomImageReference
import me.weishu.kernelsu.ui.util.readSidebarWidgetConfig
import me.weishu.kernelsu.ui.util.releaseCustomImageReference
import me.weishu.kernelsu.ui.util.takePersistableImageReadPermission
import me.weishu.kernelsu.ui.util.writeSidebarWidgetConfig
import top.yukonga.miuix.kmp.blur.layerBackdrop

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SidebarWidgetSettingsScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    var config by remember { mutableStateOf(readSidebarWidgetConfig(context)) }
    var selectedSection by rememberSaveable { mutableStateOf(0) }
    var previewExpanded by rememberSaveable { mutableStateOf(false) }
    val controlsScrollState = rememberScrollState()
    LaunchedEffect(selectedSection) { controlsScrollState.scrollTo(0) }
    val onBack = dropUnlessResumed { navigator.pop() }

    fun update(next: SidebarWidgetConfig) {
        val normalized = next.normalized()
        config = normalized
        writeSidebarWidgetConfig(context, normalized)
    }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val previous = config.imageUriString
        val stored = persistCustomImageReference(
            context = context,
            sourceUri = uri,
            storageKey = SIDEBAR_WIDGET_IMAGE_STORAGE_KEY,
            maxBytes = 12L * 1024L * 1024L,
        ) ?: uri.toString().also { takePersistableImageReadPermission(context, uri) }
        update(config.copy(imageUriString = stored, widgetType = SidebarWidgetType.Image))
        if (previous != stored) releaseCustomImageReference(context, previous)
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sidebar_widget_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            val wide = maxWidth >= 760.dp
            if (wide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    SidebarWidgetControls(
                        config = config,
                        onUpdate = ::update,
                        selectedSection = selectedSection,
                        onSelectSection = { selectedSection = it },
                        onEditNavigationIcons = { navigator.push(Route.NavigationIcons) },
                        onPickImage = { imageLauncher.launch(arrayOf("image/*")) },
                        onClearImage = {
                            releaseCustomImageReference(context, config.imageUriString)
                            update(config.copy(imageUriString = null))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(controlsScrollState)
                            .padding(bottom = 24.dp),
                    )
                    SidebarWidgetPreview(
                        config = config,
                        modifier = Modifier
                            .width(320.dp)
                            .fillMaxHeight()
                            .padding(bottom = 24.dp),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(controlsScrollState)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    OutlinedButton(
                        onClick = { previewExpanded = !previewExpanded },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(if (previewExpanded) R.string.sidebar_design_preview_hide else R.string.sidebar_design_preview_show))
                    }
                    if (previewExpanded) {
                        SidebarWidgetPreview(
                            config = config,
                            modifier = Modifier.fillMaxWidth().height(360.dp),
                        )
                    }
                    SidebarWidgetControls(
                        config = config,
                        onUpdate = ::update,
                        selectedSection = selectedSection,
                        onSelectSection = { selectedSection = it },
                        onEditNavigationIcons = { navigator.push(Route.NavigationIcons) },
                        onPickImage = { imageLauncher.launch(arrayOf("image/*")) },
                        onClearImage = {
                            releaseCustomImageReference(context, config.imageUriString)
                            update(config.copy(imageUriString = null))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarWidgetControls(
    config: SidebarWidgetConfig,
    onUpdate: (SidebarWidgetConfig) -> Unit,
    selectedSection: Int,
    onSelectSection: (Int) -> Unit,
    onEditNavigationIcons: () -> Unit,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.sidebar_design_saved_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                R.string.sidebar_design_widgets_tab,
                R.string.sidebar_design_appearance_tab,
                R.string.sidebar_design_navigation_tab,
            ).forEachIndexed { index, label ->
                FilterChip(
                    selected = selectedSection == index,
                    onClick = { onSelectSection(index) },
                    label = { Text(stringResource(label)) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        if (selectedSection == 1) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .toggleable(value = config.glassEnabled, role = Role.Switch) {
                        onUpdate(config.copy(glassEnabled = it))
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.sidebar_widget_glass_title), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.sidebar_widget_glass_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = config.glassEnabled, onCheckedChange = null)
            }
            SettingsSection(
                title = stringResource(R.string.sidebar_widget_side_title),
                icon = Icons.AutoMirrored.Rounded.ViewSidebar,
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SidebarSide.entries.forEach { side ->
                        FilterChip(
                            selected = config.side == side,
                            onClick = { onUpdate(config.copy(side = side)) },
                            label = { Text(stringResource(side.labelRes())) },
                        )
                    }
                }
            }
        }
        if (selectedSection == 0) {
            SettingsSection(
                title = stringResource(R.string.sidebar_widget_component_title),
                icon = Icons.Rounded.Tune,
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SidebarWidgetType.entries.forEach { type ->
                        FilterChip(
                            selected = config.widgetType == type,
                            onClick = { onUpdate(config.copy(widgetType = type)) },
                            label = { Text(stringResource(type.labelRes())) },
                            leadingIcon = { Icon(type.icon(), contentDescription = null, modifier = Modifier.size(18.dp)) },
                        )
                    }
                }
                when (config.widgetType) {
                    SidebarWidgetType.Clock -> ClockOptions(config, onUpdate)
                    SidebarWidgetType.Weather -> SidebarWeatherOptions(config, onUpdate)
                    SidebarWidgetType.Alarm -> Text(
                        text = stringResource(R.string.sidebar_widget_alarm_system_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SidebarWidgetType.Image -> ImageOptions(config, onUpdate, onPickImage, onClearImage)
                }
            }
        }
        if (selectedSection == 2) {
            OutlinedButton(onClick = onEditNavigationIcons, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.sidebar_design_icons))
            }
            SettingsSection(
                title = stringResource(R.string.sidebar_widget_navigation_position_title),
                icon = Icons.Rounded.Schedule,
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SidebarNavigationPosition.entries.forEach { position ->
                        FilterChip(
                            selected = config.navigationPosition == position,
                            onClick = { onUpdate(config.copy(navigationPosition = position)) },
                            label = { Text(stringResource(position.labelRes())) },
                        )
                    }
                }
            }
            SettingsSection(
                title = stringResource(R.string.sidebar_widget_navigation_order_title),
                icon = Icons.Rounded.Home,
            ) {
                config.navigationOrder.forEachIndexed { index, id ->
                    val spec = navigationSpec(id) ?: return@forEachIndexed
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape,
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(spec.icon, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                        Text(
                            text = stringResource(spec.labelRes),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        IconButton(
                            onClick = { onUpdate(config.copy(navigationOrder = config.navigationOrder.swap(index, index - 1))) },
                            enabled = index > 0,
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = stringResource(R.string.sidebar_widget_move_up))
                        }
                        IconButton(
                            onClick = { onUpdate(config.copy(navigationOrder = config.navigationOrder.swap(index, index + 1))) },
                            enabled = index < config.navigationOrder.lastIndex,
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = stringResource(R.string.sidebar_widget_move_down))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClockOptions(config: SidebarWidgetConfig, onUpdate: (SidebarWidgetConfig) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.sidebar_widget_clock_style_title),
            style = MaterialTheme.typography.labelLarge,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SidebarClockStyle.entries.forEach { style ->
                FilterChip(
                    selected = config.clockStyle == style,
                    onClick = { onUpdate(config.copy(clockStyle = style)) },
                    label = { Text(stringResource(style.labelRes())) },
                )
            }
        }
    }
}

@Composable
private fun ImageOptions(
    config: SidebarWidgetConfig,
    onUpdate: (SidebarWidgetConfig) -> Unit,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onPickImage, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.ImageSearch, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.sidebar_widget_choose_image))
            }
            if (config.imageUriString != null) {
                OutlinedButton(onClick = onClearImage) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.sidebar_widget_clear_image))
                }
            }
        }
        Text(
            text = stringResource(R.string.sidebar_widget_image_shape_title),
            style = MaterialTheme.typography.labelLarge,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SidebarImageShape.entries.forEach { shape ->
                FilterChip(
                    selected = config.imageShape == shape,
                    onClick = { onUpdate(config.copy(imageShape = shape)) },
                    label = { Text(stringResource(shape.labelRes())) },
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun SidebarWidgetPreview(config: SidebarWidgetConfig, modifier: Modifier = Modifier) {
    val dark = isInDarkTheme()
    val previewBackdrop = rememberSidebarGlassBackdrop(config.glassEnabled)
    val railAtStart = config.side.isAtStart(LocalLayoutDirection.current)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (dark) Color(0xFF171717) else Color(0xFFF1F1F1),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Preview has its own non-recursive source, not the page behind the settings screen.
            Box(
                Modifier.matchParentSize()
                    .then(previewBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
                    .sidebarGlassUnderlay(dark)
            )
            CompositionLocalProvider(LocalSidebarGlassBackdrop provides previewBackdrop) {
                SidebarWidgetPreviewPanes(config, railAtStart, dark)
            }
        }
    }
}

@Composable
private fun SidebarWidgetPreviewPanes(config: SidebarWidgetConfig, railAtStart: Boolean, dark: Boolean) {
    val customIcons = LocalCustomNavigationIcons.current
    val destinations = orderSidebarDestinations(config.navigationOrder, mainDestinations(kpmActive = true))
    Row(modifier = Modifier.fillMaxSize()) {
        val panes = if (railAtStart) listOf(true, false) else listOf(false, true)
        panes.forEach { isRail ->
            if (isRail) {
                SidebarRailLayout(
                    config = config,
                    navigationCount = destinations.size,
                ) {
                    destinations.forEach { destination ->
                        SidebarRailItem(selected = destination == MainDestination.Home) {
                            NavigationDestinationIcon(
                                destination = destination,
                                state = customIcons.stateFor(destination),
                                contentDescription = stringResource(destination.label),
                                tint = if (dark) Color(0xFFF5F5F5) else Color(0xFF161616),
                            )
                        }
                    }
                }
            } else {
                SidebarMaterialPreviewContent(railAtStart, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SidebarMaterialPreviewContent(railAtStart: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(
                sidebarPaneShape(edgeAtStart = railAtStart)
            )
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.sidebar_widget_preview_material_home),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(stringResource(R.string.sidebar_widget_preview_status), fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.sidebar_widget_preview_status_summary),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        repeat(3) { index ->
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(28.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape))
                Column {
                    Text(stringResource(R.string.sidebar_widget_preview_card, index + 1), style = MaterialTheme.typography.labelLarge)
                    Text("Material", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private data class NavigationSpec(@StringRes val labelRes: Int, val icon: ImageVector)

private fun navigationSpec(id: String): NavigationSpec? = when (id) {
    SIDEBAR_NAV_HOME -> NavigationSpec(R.string.home, Icons.Rounded.Home)
    SIDEBAR_NAV_KPM -> NavigationSpec(R.string.kpm_short_title, Icons.Rounded.Memory)
    SIDEBAR_NAV_SUPERUSER -> NavigationSpec(R.string.superuser, Icons.Rounded.Security)
    SIDEBAR_NAV_MODULE -> NavigationSpec(R.string.module, Icons.Rounded.Extension)
    SIDEBAR_NAV_SETTINGS -> NavigationSpec(R.string.settings, Icons.Rounded.Settings)
    else -> null
}

private fun List<String>.swap(from: Int, to: Int): List<String> {
    if (from !in indices || to !in indices || from == to) return this
    return toMutableList().apply {
        val value = removeAt(from)
        add(to, value)
    }
}

@StringRes
private fun SidebarWidgetType.labelRes(): Int = when (this) {
    SidebarWidgetType.Clock -> R.string.sidebar_widget_type_clock
    SidebarWidgetType.Weather -> R.string.sidebar_widget_type_weather
    SidebarWidgetType.Alarm -> R.string.sidebar_widget_type_alarm
    SidebarWidgetType.Image -> R.string.sidebar_widget_type_image
}

private fun SidebarWidgetType.icon(): ImageVector = when (this) {
    SidebarWidgetType.Clock -> Icons.Rounded.Schedule
    SidebarWidgetType.Weather -> Icons.Rounded.Cloud
    SidebarWidgetType.Alarm -> Icons.Rounded.Alarm
    SidebarWidgetType.Image -> Icons.Rounded.Image
}

@StringRes
private fun SidebarClockStyle.labelRes(): Int = when (this) {
    SidebarClockStyle.Stacked -> R.string.sidebar_widget_clock_stacked
    SidebarClockStyle.Compact -> R.string.sidebar_widget_clock_compact
    SidebarClockStyle.DateFirst -> R.string.sidebar_widget_clock_date_first
}

@StringRes
private fun SidebarImageShape.labelRes(): Int = when (this) {
    SidebarImageShape.Circle -> R.string.sidebar_widget_shape_circle
    SidebarImageShape.Square -> R.string.sidebar_widget_shape_square
    SidebarImageShape.Diamond -> R.string.sidebar_widget_shape_diamond
    SidebarImageShape.Star -> R.string.sidebar_widget_shape_star
    SidebarImageShape.Triangle -> R.string.sidebar_widget_shape_triangle
}

@StringRes
private fun SidebarNavigationPosition.labelRes(): Int = when (this) {
    SidebarNavigationPosition.Top -> R.string.sidebar_widget_position_top
    SidebarNavigationPosition.Center -> R.string.sidebar_widget_position_center
    SidebarNavigationPosition.Bottom -> R.string.sidebar_widget_position_bottom
}

@StringRes
private fun SidebarSide.labelRes(): Int = when (this) {
    SidebarSide.Left -> R.string.sidebar_widget_side_left
    SidebarSide.Right -> R.string.sidebar_widget_side_right
}
