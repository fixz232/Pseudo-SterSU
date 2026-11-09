package me.weishu.kernelsu.ui.screen.settings

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Dialog
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalMainPagerState
import me.weishu.kernelsu.ui.instantNavTransitionContentTransform
import me.weishu.kernelsu.ui.sidebarStackForwardTransitionContentTransform
import me.weishu.kernelsu.ui.sidebarStackPopTransitionContentTransform
import me.weishu.kernelsu.ui.component.rememberSystemAnimationsEnabled
import me.weishu.kernelsu.ui.component.bottombar.LocalSidebarGlassBackdrop
import me.weishu.kernelsu.ui.component.bottombar.LocalSidebarGlassBackground
import me.weishu.kernelsu.ui.component.bottombar.rememberSidebarGlassBackdrop
import me.weishu.kernelsu.ui.component.bottombar.sidebarGlassUnderlay
import me.weishu.kernelsu.ui.component.bottombar.NavigationDestinationIcon
import me.weishu.kernelsu.ui.component.bottombar.SidebarRailItem
import me.weishu.kernelsu.ui.component.bottombar.SidebarRailLayout
import me.weishu.kernelsu.ui.component.bottombar.mainDestinations
import me.weishu.kernelsu.ui.component.bottombar.labelFor
import me.weishu.kernelsu.ui.component.bottombar.orderSidebarDestinations
import me.weishu.kernelsu.ui.component.bottombar.stateFor
import me.weishu.kernelsu.ui.component.passthrough.MaterialIcon as Icon
import me.weishu.kernelsu.ui.component.passthrough.MaterialText as Text
import me.weishu.kernelsu.ui.component.CustomBackgroundMedia
import me.weishu.kernelsu.ui.util.LocalCustomNavigationIcons
import me.weishu.kernelsu.ui.util.SidebarWidgetConfig
import me.weishu.kernelsu.ui.util.SidebarMaterial
import me.weishu.kernelsu.ui.theme.immersivePageColor
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor
import top.yukonga.miuix.kmp.blur.layerBackdrop

internal enum class SidebarDesignPage(@StringRes val title: Int, @StringRes val summary: Int) {
    Appearance(R.string.sidebar_material_title, R.string.sidebar_editor_appearance_summary),
    Avatar(R.string.sidebar_avatar_title, R.string.sidebar_avatar_summary),
    Widget(R.string.sidebar_widget_component_title, R.string.sidebar_editor_widget_summary),
    Weather(R.string.sidebar_editor_weather_title, R.string.sidebar_editor_weather_summary),
    Navigation(R.string.sidebar_navigation_layout_title, R.string.sidebar_editor_navigation_summary),
    Home(R.string.sidebar_home_layout_title, R.string.sidebar_editor_home_summary),
}

internal fun useSidebarDesignTwoPane(widthDp: Float, heightDp: Float, fontScale: Float): Boolean =
    widthDp / fontScale.coerceAtLeast(1f) >= 840f && heightDp >= 480f

internal fun sidebarDesignPageTransition(
    initialPage: SidebarDesignPage?,
    targetPage: SidebarDesignPage?,
    animationsEnabled: Boolean,
    layoutDirection: LayoutDirection,
): ContentTransform {
    if (!animationsEnabled || initialPage == targetPage) return instantNavTransitionContentTransform()
    val direction = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
    return if (targetPage == null) {
        sidebarStackPopTransitionContentTransform(direction)
    } else {
        sidebarStackForwardTransitionContentTransform(direction)
    }
}

private fun SidebarDesignPage.icon(): ImageVector = when (this) {
    SidebarDesignPage.Appearance -> Icons.Rounded.Palette
    SidebarDesignPage.Avatar -> Icons.Rounded.Image
    SidebarDesignPage.Widget -> Icons.Rounded.Schedule
    SidebarDesignPage.Weather -> Icons.Rounded.Cloud
    SidebarDesignPage.Navigation -> Icons.Rounded.Tune
    SidebarDesignPage.Home -> Icons.Rounded.Home
}

@Composable
internal fun SidebarDesignWorkspace(
    config: SidebarWidgetConfig,
    selectedPage: SidebarDesignPage?,
    onSelectPage: (SidebarDesignPage?) -> Unit,
    editingDark: Boolean,
    onEditingDarkChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    content: @Composable (SidebarDesignPage) -> Unit,
) {
    var preview by rememberSaveable { mutableStateOf(false) }
    val stateHolder = rememberSaveableStateHolder()
    val animationsEnabled = rememberSystemAnimationsEnabled()
    val layoutDirection = LocalLayoutDirection.current
    // Keep the shared day/night or immersive background stationary behind both
    // animated pages; only the page content moves, never the external rail.
    BoxWithConstraints(Modifier.fillMaxSize()
        .background(immersivePageColor(MaterialTheme.colorScheme.background)).clipToBounds()) {
        val twoPane = useSidebarDesignTwoPane(maxWidth.value, maxHeight.value, LocalDensity.current.fontScale)
        val active = selectedPage ?: SidebarDesignPage.Appearance
        val back = { if (!twoPane && selectedPage != null) onSelectPage(null) else onBack() }
        BackHandler(enabled = !twoPane && selectedPage != null) { onSelectPage(null) }

        @Composable
        fun Editor(page: SidebarDesignPage, modifier: Modifier) {
            stateHolder.SaveableStateProvider(page.name) {
                Column(
                    modifier.fillMaxHeight().verticalScroll(rememberScrollState()).imePadding().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (twoPane) Text(stringResource(page.title), style = MaterialTheme.typography.headlineSmall)
                        Text(stringResource(page.summary), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        SidebarDesignPreviewButton(onClick = { preview = true })
                    }
                    content(page)
                }
            }
        }

        if (twoPane) {
            AppearanceEditorPage(
                title = stringResource(R.string.sidebar_widget_settings_title),
                onBack = back,
            ) { modifier ->
                Box(modifier, contentAlignment = Alignment.TopCenter) {
                    Row(Modifier.widthIn(max = 1160.dp).fillMaxSize()) {
                        Column(Modifier.width(264.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            SidebarDesignSavedHint()
                            SidebarDesignPageList(active, onSelectPage)
                            SidebarDesignPreviewButton(onClick = { preview = true })
                        }
                        AnimatedContent(
                            targetState = active,
                            modifier = Modifier.weight(1f).fillMaxHeight().clipToBounds(),
                            transitionSpec = {
                                sidebarDesignPageTransition(initialState, targetState, animationsEnabled, layoutDirection)
                            },
                            contentKey = { it.name },
                            label = "sidebarDesignEditor",
                        ) { page ->
                            Editor(page, Modifier.fillMaxSize())
                        }
                    }
                }
            }
        } else {
            AnimatedContent(
                targetState = selectedPage,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    sidebarDesignPageTransition(initialState, targetState, animationsEnabled, layoutDirection)
                },
                contentKey = { it?.name ?: "overview" },
                label = "sidebarDesignPage",
            ) { page ->
                // Render the transition snapshot, not selectedPage: the outgoing
                // page must retain its own title, editor and saveable-state key.
                AppearanceEditorPage(
                    title = stringResource(page?.title ?: R.string.sidebar_widget_settings_title),
                    onBack = { if (page == selectedPage) back() },
                ) { modifier ->
                    Box(modifier, contentAlignment = Alignment.TopCenter) {
                        if (page != null) {
                            Editor(page, Modifier.widthIn(max = 720.dp).fillMaxWidth())
                        } else stateHolder.SaveableStateProvider("overview") {
                            Column(Modifier.widthIn(max = 720.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                SidebarDesignSavedHint()
                                SidebarDesignPreviewButton(onClick = { preview = true })
                                SidebarDesignPageList(null, onSelectPage)
                            }
                        }
                    }
                }
            }
        }
    }
    if (preview) SidebarDesignPreview(config, editingDark, onEditingDarkChange) { preview = false }
}

@Composable
private fun SidebarDesignPreviewButton(onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Icon(Icons.Rounded.Visibility, null, Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.sidebar_editor_preview))
    }
}

@Composable
private fun SidebarDesignSavedHint() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.CheckCircle, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.sidebar_editor_save_help), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SidebarDesignPageList(selected: SidebarDesignPage?, onSelect: (SidebarDesignPage) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SidebarDesignPage.entries.forEach { page ->
            val isSelected = selected == page
            Row(
                Modifier.fillMaxWidth().heightIn(min = 72.dp).clip(shape)
                    .background(immersiveSurfaceColor(if (isSelected) colors.secondaryContainer else colors.surfaceContainer))
                    .clickable(role = Role.Button) { onSelect(page) }.padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(page.icon(), null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(page.title), style = MaterialTheme.typography.titleSmall)
                    if (selected == null) Text(stringResource(page.summary), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(20.dp))
            }
        }
    }
}

@Composable
internal fun SidebarDesignChoice(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSelected: (Int) -> Unit,
) {
    var open by rememberSaveable(title) { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
        OutlinedButton(onClick = { open = true }, enabled = enabled && options.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp)) {
            Text(options.getOrNull(selectedIndex).orEmpty(), modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(20.dp))
        }
    }
    if (open && enabled) AlertDialog(
        onDismissRequest = { open = false }, title = { Text(title) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()).selectableGroup()) {
                options.forEachIndexed { index, option ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        .selectable(selected = index == selectedIndex, role = Role.RadioButton,
                            onClick = { onSelected(index); open = false }).padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RadioButton(selected = index == selectedIndex, onClick = null)
                        Text(option, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(android.R.string.cancel)) } },
    )
}

@Composable
private fun SidebarDesignPreview(config: SidebarWidgetConfig, dark: Boolean, onDarkChange: (Boolean) -> Unit, onDismiss: () -> Unit) {
    val main = LocalMainPagerState.current
    val customIcons = LocalCustomNavigationIcons.current
    val background = LocalSidebarGlassBackground.current
    val destinations = orderSidebarDestinations(config.navigationOrder, mainDestinations(main.kpmActive, main.stealthModeEnabled))
    val configuration = LocalConfiguration.current
    val window = me.weishu.kernelsu.ui.util.currentAppWindowSize()
    val layoutDirection = LocalLayoutDirection.current
    // Do not compress the real rail into a thumbnail: that hides its widgets in compact mode.
    // The dialog scrolls around a device-height rail instead.
    val previewHeight = (configuration.screenHeightDp - 112).coerceAtLeast(360).dp
    val tablet = configuration.smallestScreenWidthDp >= 600
    Dialog(onDismissRequest = onDismiss) {
        val previewBackdrop = rememberSidebarGlassBackdrop(
            enabled = config.material != SidebarMaterial.Flat && !background.hasVideo,
        )
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.widthIn(max = 480.dp).fillMaxWidth()
                .heightIn(max = (window.heightDp - 48f).coerceAtLeast(0f).dp)
                .verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.sidebar_editor_preview), style = MaterialTheme.typography.titleLarge)
                SidebarDesignChoice(stringResource(R.string.sidebar_palette_mode),
                    listOf(stringResource(R.string.sidebar_palette_light), stringResource(R.string.sidebar_palette_dark)),
                    if (dark) 1 else 0, onSelected = { onDarkChange(it == 1) })
                Box(Modifier.fillMaxWidth().height(previewHeight).clip(RoundedCornerShape(12.dp))) {
                    // Capture a separate wallpaper sibling inside this dialog window.
                    // Never sample the rail itself or reuse coordinates from the activity window.
                    Box(Modifier.fillMaxSize()
                        .then(if (previewBackdrop != null) Modifier.layerBackdrop(previewBackdrop) else Modifier)
                        .sidebarGlassUnderlay(dark)) {
                        CustomBackgroundMedia(
                            imageUriString = background.wallpaperUriString,
                            videoUriString = null,
                            opacity = background.opacity,
                            crop = background.crop,
                            visualSettings = background.visualSettings,
                        )
                    }
                    CompositionLocalProvider(LocalSidebarGlassBackdrop provides previewBackdrop) {
                        Row(Modifier.fillMaxSize()) {
                            @Composable
                            fun Rail() {
                                SidebarRailLayout(config, destinations.size, dark = dark, tablet = tablet) {
                                    destinations.forEachIndexed { index, destination ->
                                        SidebarRailItem(selected = index == 0,
                                            label = customIcons.labelFor(destination, stringResource(destination.label)),
                                            iconOnly = config.iconOnlyNavigation) {
                                            NavigationDestinationIcon(destination, customIcons.stateFor(destination), null,
                                                tint = androidx.compose.material3.LocalContentColor.current)
                                        }
                                    }
                                }
                            }
                            @Composable
                            fun Page() {
                                Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(stringResource(R.string.home), color = if (dark) Color.White else Color.Black,
                                        style = MaterialTheme.typography.titleMedium)
                                    Text(stringResource(R.string.sidebar_editor_preview_note),
                                        color = if (dark) Color.White else Color.Black, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (config.side.isAtStart(layoutDirection)) { Rail(); Page() } else { Page(); Rail() }
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}
