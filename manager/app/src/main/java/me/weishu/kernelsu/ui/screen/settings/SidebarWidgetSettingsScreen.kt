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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.bottombar.SidebarHeaderWidget
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
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
import me.weishu.kernelsu.ui.util.SidebarWidgetConfig
import me.weishu.kernelsu.ui.util.SidebarWidgetType
import me.weishu.kernelsu.ui.util.persistCustomImageReference
import me.weishu.kernelsu.ui.util.readSidebarWidgetConfig
import me.weishu.kernelsu.ui.util.releaseCustomImageReference
import me.weishu.kernelsu.ui.util.takePersistableImageReadPermission
import me.weishu.kernelsu.ui.util.writeSidebarWidgetConfig

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SidebarWidgetSettingsScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    var config by remember { mutableStateOf(readSidebarWidgetConfig(context)) }
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
                        onPickImage = { imageLauncher.launch(arrayOf("image/*")) },
                        onClearImage = {
                            releaseCustomImageReference(context, config.imageUriString)
                            update(config.copy(imageUriString = null))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp),
                    )
                    SidebarWidgetPreview(
                        config = config,
                        modifier = Modifier
                            .widthIn(min = 300.dp, max = 420.dp)
                            .fillMaxHeight()
                            .padding(bottom = 24.dp),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SidebarWidgetPreview(
                        config = config,
                        modifier = Modifier.fillMaxWidth().height(360.dp),
                    )
                    SidebarWidgetControls(
                        config = config,
                        onUpdate = ::update,
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
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = stringResource(R.string.sidebar_widget_settings_summary),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
                SidebarWidgetType.Weather -> WeatherOptions(config, onUpdate)
                SidebarWidgetType.Alarm -> Text(
                    text = stringResource(R.string.sidebar_widget_alarm_system_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SidebarWidgetType.Image -> ImageOptions(config, onUpdate, onPickImage, onClearImage)
            }
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
private fun WeatherOptions(config: SidebarWidgetConfig, onUpdate: (SidebarWidgetConfig) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.sidebar_widget_weather_privacy_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = config.weatherTemperature,
            onValueChange = { onUpdate(config.copy(weatherTemperature = it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.sidebar_widget_weather_temperature)) },
        )
        OutlinedTextField(
            value = config.weatherLabel,
            onValueChange = { onUpdate(config.copy(weatherLabel = it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.sidebar_widget_weather_label)) },
        )
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
        shape = RoundedCornerShape(24.dp),
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(
                modifier = Modifier
                    .width(86.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF73A8F0), Color(0xFFBFD0E5), Color(0xFFE1E2E4))
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = Color(0xFF101820))
                CompositionLocalProvider(LocalContentColor provides Color(0xFF101820)) {
                    SidebarHeaderWidget(config, modifier = Modifier.height(142.dp), compact = true)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = when (config.navigationPosition) {
                        SidebarNavigationPosition.Top -> Arrangement.Top
                        SidebarNavigationPosition.Center -> Arrangement.Center
                        SidebarNavigationPosition.Bottom -> Arrangement.Bottom
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    config.navigationOrder.take(4).forEachIndexed { index, id ->
                        val spec = navigationSpec(id) ?: return@forEachIndexed
                        Box(
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .size(42.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (index == 0) Color.White.copy(alpha = 0.70f) else Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(spec.icon, contentDescription = null, tint = Color(0xFF101820), modifier = Modifier.size(21.dp))
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 12.dp, top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.sidebar_widget_preview_material_home),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Card(
                    modifier = Modifier.fillMaxWidth().height(90.dp),
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
                    Card(
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(28.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(stringResource(R.string.sidebar_widget_preview_card, index + 1), style = MaterialTheme.typography.labelLarge)
                                Text("Material", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
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
