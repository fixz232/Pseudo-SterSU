package me.weishu.kernelsu.ui.screen.colorpalette

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CallToAction
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.IconButton
import me.weishu.kernelsu.ui.component.passthrough.MaterialIcon as Icon
import me.weishu.kernelsu.ui.component.material.ExpressiveTopAppBar as LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import me.weishu.kernelsu.ui.component.passthrough.MaterialText as Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.bottombar.useNavigationRail
import me.weishu.kernelsu.ui.component.material.ExpressiveScaffold
import me.weishu.kernelsu.ui.component.material.ExpressiveToggleButton
import me.weishu.kernelsu.ui.component.material.SegmentedColumn
import me.weishu.kernelsu.ui.component.material.SegmentedDropdownItem
import me.weishu.kernelsu.ui.component.material.SegmentedListItem
import me.weishu.kernelsu.ui.component.material.SegmentedSwitchItem
import me.weishu.kernelsu.ui.component.material.TonalCard
import me.weishu.kernelsu.ui.component.material.TopBarBackButton
import me.weishu.kernelsu.ui.component.material.expressiveTopAppBarColors
import me.weishu.kernelsu.ui.theme.ColorMode
import me.weishu.kernelsu.ui.theme.ThemeAppearanceDefaults
import me.weishu.kernelsu.ui.theme.ThemePreset
import me.weishu.kernelsu.ui.theme.ThemeSyncStrategy
import me.weishu.kernelsu.ui.theme.keyColorOptions
import me.weishu.kernelsu.ui.theme.rememberKernelSUColorScheme
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import kotlin.math.roundToInt

@Composable
fun ColorPaletteScreenMaterial(
    state: ColorPaletteUiState,
    actions: ColorPaletteScreenActions,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val uiState = state.uiState
    val currentColorMode = ColorMode.fromValue(state.currentColorMode.toNonMonetMode())
    val currentKeyColor = uiState.keyColor
    val colorStyle = state.currentPaletteStyle
    val colorSpec = state.currentColorSpec
    val haptic = LocalHapticFeedback.current
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    var showSavePresetDialog by rememberSaveable { mutableStateOf(false) }
    var renamePresetId by rememberSaveable { mutableStateOf<String?>(null) }

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                navigationIcon = {
                    TopBarBackButton(onClick = actions.onBack)
                },
                title = { Text(stringResource(R.string.settings_theme)) },
                colors = expressiveTopAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { paddingValues ->
        val navBars = WindowInsets.navigationBars.asPaddingValues()
        val captionBar = WindowInsets.captionBar.asPaddingValues()

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isDark = currentColorMode.isDark || currentColorMode.isSystem && isSystemInDarkTheme()
            val isAmoled = currentColorMode.isAmoled
            ThemePreviewCard(
                keyColor = currentKeyColor,
                isDark = isDark,
                isAmoled = isAmoled,
                paletteStyle = colorStyle,
                colorSpec = colorSpec,
            )

            Spacer(modifier = Modifier.height(8.dp))

            SegmentedColumn(modifier = Modifier.padding(horizontal = 16.dp), connected = true) {
                item {
                    val currentPreset = ThemePreset.fromValue(uiState.themePreset)
                    val compatiblePresets = ThemePreset.workshopPresets.filter { it.isCompatibleWith(uiState.uiMode) }
                    val presets = if (currentPreset == ThemePreset.CUSTOM) {
                        listOf(ThemePreset.CUSTOM) + compatiblePresets
                    } else compatiblePresets
                    SegmentedDropdownItem(
                        icon = Icons.Rounded.Style,
                        title = stringResource(R.string.theme_workshop),
                        summary = stringResource(R.string.theme_workshop_summary),
                        items = presets.map { stringResource(it.titleRes) },
                        selectedIndex = presets.indexOf(currentPreset).coerceAtLeast(0),
                        onItemSelected = { index ->
                            if (presets[index] != ThemePreset.CUSTOM) actions.onApplyThemePreset(presets[index])
                        },
                    )
                }
                item {
                    val currentAccent = MaterialTheme.colorScheme.primary.toArgb()
                    SegmentedSwitchItem(
                        icon = Icons.Rounded.Wallpaper,
                        title = stringResource(R.string.settings_material_wallpaper_color),
                        summary = stringResource(R.string.settings_material_wallpaper_color_summary),
                        checked = currentKeyColor == 0,
                        onCheckedChange = { enabled -> actions.onSetKeyColor(if (enabled) 0 else currentAccent) },
                    )
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    ColorButtonMaterial(
                        color = Color.Unspecified,
                        isSelected = currentKeyColor == 0,
                        isDark = isDark,
                        isAmoled = isAmoled,
                        paletteStyle = colorStyle,
                        colorSpec = colorSpec,
                        onClick = {
                            actions.onSetKeyColor(0)
                        }
                    )
                }

                items(keyColorOptions) { color ->
                    ColorButtonMaterial(
                        color = Color(color),
                        isSelected = currentKeyColor == color,
                        isDark = isDark,
                        isAmoled = isAmoled,
                        paletteStyle = colorStyle,
                        colorSpec = colorSpec,
                        onClick = {
                            actions.onSetKeyColor(color)
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val options = listOf(
                    listOf(ColorMode.SYSTEM) to stringResource(R.string.settings_theme_mode_system),
                    listOf(ColorMode.LIGHT) to stringResource(R.string.settings_theme_mode_light),
                    listOf(ColorMode.DARK) to stringResource(R.string.settings_theme_mode_dark),
                    listOf(ColorMode.DARK_AMOLED) to stringResource(R.string.settings_theme_mode_amoled)
                )

                options.chunked(if (me.weishu.kernelsu.ui.util.currentAppWindowSize().narrow) 2 else 4).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                    ) {
                        rowOptions.forEachIndexed { index, (modes, label) ->
                            ExpressiveToggleButton(
                                checked = currentColorMode in modes,
                                onCheckedChange = {
                                    if (it) {
                                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                        actions.onSetColorMode(modes.first())
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { role = Role.RadioButton },
                                shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    rowOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                },
                            ) {
                                Icon(
                                    imageVector = when (modes.first()) {
                                        ColorMode.SYSTEM -> Icons.Filled.Brightness4
                                        ColorMode.LIGHT -> Icons.Filled.Brightness7
                                        ColorMode.DARK -> Icons.Filled.Brightness3
                                        ColorMode.DARK_AMOLED -> Icons.Filled.Brightness1
                                        else -> Icons.Filled.Brightness4
                                    },
                                    contentDescription = label
                                )
                            }
                        }
                    }
                }

                SegmentedColumn(
                    modifier = Modifier.padding(top = 4.dp),
                    connected = true,
                    content = listOf(
                        {
                            val styles = PaletteStyle.entries
                            SegmentedDropdownItem(
                                icon = Icons.Rounded.Style,
                                title = stringResource(R.string.settings_color_style),
                                items = styles.map { it.name },
                                selectedIndex = styles.indexOf(colorStyle),
                                onItemSelected = { index ->
                                    actions.onSetColorStyle(styles[index].name)
                                }
                            )
                        },
                        {
                            val specs = ColorSpec.SpecVersion.entries
                            SegmentedDropdownItem(
                                icon = Icons.Rounded.DesignServices,
                                title = stringResource(R.string.settings_color_spec),
                                items = specs.map { it.name },
                                selectedIndex = specs.indexOf(colorSpec).coerceAtLeast(0),
                                onItemSelected = { index ->
                                    actions.onSetColorSpec(specs[index].name)
                                }
                            )
                        }
                    )
                )

                ThemeSliderMaterial(
                    title = stringResource(R.string.settings_monet_surface_opacity),
                    summary = stringResource(R.string.settings_monet_surface_opacity_summary),
                    icon = Icons.Rounded.WaterDrop,
                    value = uiState.monetSurfaceOpacity,
                    valueRange = ThemeAppearanceDefaults.MIN_MONET_SURFACE_OPACITY..
                        ThemeAppearanceDefaults.MAX_MONET_SURFACE_OPACITY,
                    keyPoints = listOf(0.45f, 0.6f, 0.75f, 0.9f, 1f),
                    onValueChangeFinished = actions.onSetMonetSurfaceOpacity,
                )

                SegmentedColumn(modifier = Modifier.padding(top = 4.dp), connected = true) {
                    item(visible = supportsBlur) {
                        SegmentedSwitchItem(
                            icon = Icons.Rounded.BlurOn,
                            title = stringResource(R.string.settings_enable_blur),
                            summary = stringResource(R.string.settings_enable_blur_summary),
                            checked = uiState.enableBlur,
                            onCheckedChange = actions.onSetEnableBlur,
                        )
                    }
                    item {
                        SegmentedSwitchItem(
                            icon = Icons.Rounded.CallToAction,
                            title = stringResource(R.string.settings_floating_bottom_bar),
                            summary = stringResource(R.string.settings_floating_bottom_bar_summary),
                            checked = uiState.enableFloatingBottomBar,
                            onCheckedChange = actions.onSetEnableFloatingBottomBar,
                        )
                    }
                    item(visible = supportsBlur && uiState.enableFloatingBottomBar) {
                        SegmentedSwitchItem(
                            icon = Icons.Rounded.WaterDrop,
                            title = stringResource(R.string.settings_enable_glass),
                            summary = stringResource(R.string.settings_enable_glass_summary),
                            checked = uiState.enableFloatingBottomBarBlur,
                            onCheckedChange = actions.onSetEnableFloatingBottomBarBlur,
                        )
                    }
                }

                SegmentedColumn(
                    modifier = Modifier.padding(top = 4.dp),
                    connected = true,
                    content = listOf(
                        {
                            SegmentedSwitchItem(
                                icon = Icons.Rounded.Timer,
                                title = stringResource(R.string.settings_auto_hide_navigation_bar),
                                summary = stringResource(R.string.settings_auto_hide_navigation_bar_summary),
                                checked = uiState.autoHideNavigationBar,
                                onCheckedChange = actions.onSetAutoHideNavigationBar,
                            )
                        },
                        {
                            SegmentedSwitchItem(
                                icon = Icons.Rounded.SwapVert,
                                title = stringResource(R.string.settings_scroll_hide_navigation_bar),
                                summary = stringResource(R.string.settings_scroll_hide_navigation_bar_summary),
                                checked = uiState.scrollHideNavigationBar,
                                onCheckedChange = actions.onSetScrollHideNavigationBar,
                            )
                        },
                        {
                            SegmentedSwitchItem(
                                icon = Icons.Rounded.Timer,
                                title = stringResource(id = R.string.settings_module_top_bar_auto_hide),
                                summary = stringResource(id = R.string.settings_module_top_bar_auto_hide_summary),
                                checked = uiState.moduleTopBarAutoHideEnabled,
                                onCheckedChange = actions.onSetModuleTopBarAutoHideEnabled,
                            )
                        }
                    )
                )

                TonalCard(modifier = Modifier.padding(top = 4.dp)) {
                    var sliderValue by remember(uiState.pageScale) { mutableFloatStateOf(uiState.pageScale) }
                    val scaleTitle = stringResource(R.string.settings_page_scale)

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.AspectRatio,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = scaleTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(id = R.string.settings_page_scale_summary),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${(sliderValue * 100).roundToInt()}%",
                                modifier = Modifier.padding(start = 12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            onValueChangeFinished = { actions.onSetPageScale(sliderValue) },
                            valueRange = 0.8f..1.1f,
                            showKeyPoints = true,
                            keyPoints = listOf(0.8f, 0.9f, 1f, 1.1f),
                            magnetThreshold = 0.01f,
                            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                            colors = SliderDefaults.sliderColors(
                                foregroundColor = MaterialTheme.colorScheme.primary,
                                backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                thumbColor = MaterialTheme.colorScheme.onPrimary,
                                keyPointColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                keyPointForegroundColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .semantics { contentDescription = scaleTitle },
                        )
                    }
                }

                ThemeSliderMaterial(
                    title = stringResource(R.string.settings_font_scale),
                    summary = stringResource(R.string.settings_font_scale_summary),
                    icon = Icons.Rounded.FontDownload,
                    value = uiState.fontScale,
                    valueRange = 0.85f..1.2f,
                    keyPoints = listOf(0.85f, 1f, 1.1f, 1.2f),
                    onValueChangeFinished = actions.onSetFontScale,
                )

                if (supportsBlur) {
                    ThemeSliderMaterial(
                        title = stringResource(R.string.settings_blur_intensity),
                        summary = stringResource(R.string.settings_blur_intensity_summary),
                        icon = Icons.Rounded.BlurOn,
                        value = uiState.blurIntensity,
                        valueRange = 0.5f..1.5f,
                        keyPoints = listOf(0.5f, 1f, 1.25f, 1.5f),
                        onValueChangeFinished = actions.onSetBlurIntensity,
                    )
                }

                SegmentedColumn(
                    modifier = Modifier.padding(top = 4.dp),
                    title = stringResource(R.string.theme_custom_presets),
                    connected = true,
                ) {
                    item {
                        SegmentedListItem(
                            onClick = { showSavePresetDialog = true },
                            leadingContent = { Icon(Icons.Rounded.Save, contentDescription = null) },
                            headlineContent = { Text(stringResource(R.string.theme_custom_preset_save)) },
                            supportingContent = { Text(stringResource(R.string.theme_custom_preset_save_summary)) },
                        )
                    }
                    uiState.customThemePresets.forEach { preset ->
                        item(key = preset.id) {
                            SegmentedListItem(
                                onClick = { actions.onApplyCustomThemePreset(preset.id) },
                                leadingContent = { Icon(Icons.Rounded.Tune, contentDescription = null) },
                                headlineContent = { Text(preset.name) },
                                supportingContent = { Text(stringResource(R.string.theme_custom_preset_item_summary)) },
                                trailingContent = {
                                    Row {
                                        IconButton(onClick = { renamePresetId = preset.id }) {
                                            Icon(Icons.Rounded.Edit, stringResource(R.string.theme_custom_preset_rename))
                                        }
                                        IconButton(onClick = { actions.onDeleteCustomThemePreset(preset.id) }) {
                                            Icon(Icons.Rounded.Delete, stringResource(R.string.theme_custom_preset_delete))
                                        }
                                    }
                                },
                            )
                        }
                    }
                    item {
                        SegmentedDropdownItem(
                            icon = Icons.Rounded.SyncAlt,
                            title = stringResource(R.string.theme_sync_strategy),
                            summary = stringResource(R.string.theme_sync_strategy_summary),
                            items = listOf(
                                stringResource(R.string.theme_sync_shared),
                                stringResource(R.string.theme_sync_per_style),
                            ),
                            selectedIndex = if (uiState.themeSyncStrategy == ThemeSyncStrategy.SHARED) 0 else 1,
                            onItemSelected = { index ->
                                actions.onSetThemeSyncStrategy(
                                    if (index == 0) ThemeSyncStrategy.SHARED else ThemeSyncStrategy.PER_STYLE,
                                )
                            },
                        )
                    }
                    item {
                        SegmentedListItem(
                            onClick = actions.onResetThemeToDefault,
                            leadingContent = { Icon(Icons.Rounded.RestartAlt, contentDescription = null) },
                            headlineContent = { Text(stringResource(R.string.theme_reset_default)) },
                            supportingContent = { Text(stringResource(R.string.theme_reset_default_summary)) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp + navBars.calculateBottomPadding() + captionBar.calculateBottomPadding()))
        }
    }

    ThemePresetNameDialog(
        show = showSavePresetDialog,
        title = stringResource(R.string.theme_custom_preset_save),
        onDismissRequest = { showSavePresetDialog = false },
        onConfirm = actions.onSaveCustomThemePreset,
    )
    uiState.customThemePresets.firstOrNull { it.id == renamePresetId }?.let { preset ->
        ThemePresetNameDialog(
            show = true,
            title = stringResource(R.string.theme_custom_preset_rename),
            initialName = preset.name,
            onDismissRequest = { renamePresetId = null },
            onConfirm = { name -> actions.onRenameCustomThemePreset(preset.id, name) },
        )
    }
}

@Composable
private fun ThemeSliderMaterial(
    title: String,
    summary: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    keyPoints: List<Float>,
    onValueChangeFinished: (Float) -> Unit,
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value) }
    TonalCard(modifier = Modifier.padding(top = 4.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "${(sliderValue * 100).roundToInt()}%",
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onValueChangeFinished(sliderValue) },
                valueRange = valueRange,
                showKeyPoints = true,
                keyPoints = keyPoints,
                magnetThreshold = 0.01f,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                colors = SliderDefaults.sliderColors(
                    foregroundColor = MaterialTheme.colorScheme.primary,
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    thumbColor = MaterialTheme.colorScheme.onPrimary,
                    keyPointColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    keyPointForegroundColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = title },
            )
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun ThemePreviewCard(
    keyColor: Int,
    isDark: Boolean,
    isAmoled: Boolean = false,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2025,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val screenHeight = configuration.screenHeightDp.toFloat()
    val screenRatio = screenWidth / screenHeight
    val useRail = useNavigationRail()

    val colorScheme = rememberKernelSUColorScheme(
        seedColor = if (keyColor == 0) Color.Unspecified else Color(keyColor),
        isDark = isDark,
        isAmoled = isAmoled,
        paletteStyle = paletteStyle,
        colorSpec = colorSpec,
    )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .aspectRatio(screenRatio),
            color = colorScheme.surfaceContainer,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, color = colorScheme.outlineVariant)
        ) {
            val content: @Composable ColumnScope.() -> Unit = {
                    // top bar
                    Box(
                        modifier = Modifier
                            .height(if (useRail) 36.dp else 48.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 12.dp, top = if (useRail) 8.dp else 16.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.app_name),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurface
                            )
                        }
                    }

                    BoxWithConstraints(modifier = Modifier.weight(1f)) {
                        val showInfoCard = maxHeight >= 72.dp
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TonalCard(
                                containerColor = colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                content = { }
                            )
                            if (showInfoCard) {
                                TonalCard(
                                    containerColor = colorScheme.surfaceBright,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    content = { }
                                )
                            }
                        }
                    }
            }

            if (useRail) {
                Row {
                    Surface(
                        color = colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(36.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Home, null, tint = colorScheme.primary)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) { content() }
                }
            } else {
                Column {
                    content()

                    // bottom bar
                    Surface(
                        color = colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .height(40.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Home, null, tint = colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorButtonMaterial(
    color: Color,
    isSelected: Boolean,
    isDark: Boolean,
    isAmoled: Boolean = false,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2025,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val colorScheme = rememberKernelSUColorScheme(
        seedColor = color,
        isDark = isDark,
        isAmoled = isAmoled,
        paletteStyle = paletteStyle,
        colorSpec = colorSpec,
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        },
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surfaceContainer,
        modifier = Modifier.size(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(48.dp)) {
                drawArc(
                    color = colorScheme.primaryContainer,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true
                )
                drawArc(
                    color = colorScheme.tertiaryContainer,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true
                )
            }

            val scale by animateFloatAsState(targetValue = if (isSelected) 1.1f else 1.0f)
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(2.dp, colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = colorScheme.onPrimary,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(16.dp)
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = !isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(colorScheme.primary, CircleShape)
                    )
                }
            }
        }
    }
}
