package me.weishu.kernelsu.ui.screen.settings

import android.content.Intent
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import me.weishu.kernelsu.ui.component.passthrough.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ViewSidebar
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import me.weishu.kernelsu.ui.component.passthrough.MaterialIcon as Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import me.weishu.kernelsu.ui.component.passthrough.MaterialText as Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.component.dialog.rememberLoadingDialog
import me.weishu.kernelsu.ui.component.material.ExpressiveScaffold
import me.weishu.kernelsu.ui.component.miuix.SendLogDialog
import me.weishu.kernelsu.ui.component.uninstalldialog.UninstallDialog
import me.weishu.kernelsu.ui.component.rememberSystemAnimationsEnabled
import me.weishu.kernelsu.ui.navigation3.Navigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.theme.ColorMode
import me.weishu.kernelsu.ui.theme.SidebarUiTokens
import me.weishu.kernelsu.ui.theme.immersivePageColor
import me.weishu.kernelsu.ui.theme.immersiveTopBarColor
import me.weishu.kernelsu.ui.util.FULL_CUSTOM_WALLPAPER_CROP
import me.weishu.kernelsu.ui.util.adaptiveDialogContentMaxHeight
import me.weishu.kernelsu.ui.util.managerDisplayName

/** The beautified device-oriented settings home shared by all interface styles. */
@Composable
internal fun SettingsSidebarScreen(
    uiState: SettingsUiState,
    navigator: Navigator,
    bottomInnerPadding: Dp,
    onOpenCategory: (SettingsCategory) -> Unit,
    onPageModeChange: (SettingsPageMode) -> Unit,
    onSetColorMode: (ColorMode) -> Unit,
) {
    val context = LocalContext.current
    val sidebarStyle = me.weishu.kernelsu.ui.LocalInterfaceStyle.current == InterfaceStyle.SidebarWidget.value
    val styleName = uiState.installedInterfaceStyles
        .firstOrNull { it.style.id == uiState.activeInterfaceStyleId && uiState.isInterfaceStyleActive(it.style) }
        ?.style?.name
        ?: uiState.installedInterfaceStyles.firstOrNull { uiState.isInterfaceStyleActive(it.style) }?.style?.name
        ?: stringResource(settingsHomeStyleLabelRes(uiState))
    val pageColor = immersivePageColor(if (sidebarStyle) MaterialTheme.colorScheme.surface else Color.Transparent)
    val deviceViewModel = viewModel<SettingsDeviceViewModel>()
    val device by deviceViewModel.state.collectAsStateWithLifecycle()
    var showAllSettings by rememberSaveable { mutableStateOf(false) }
    var showDeviceTextEditor by rememberSaveable { mutableStateOf(false) }
    val deviceEditorScrollState = rememberScrollState()
    var modelDraft by rememberSaveable { mutableStateOf("") }
    var processorDraft by rememberSaveable { mutableStateOf("") }
    var systemDraft by rememberSaveable { mutableStateOf("") }
    var showColorMode by rememberSaveable { mutableStateOf(false) }
    var showPageMode by rememberSaveable { mutableStateOf(false) }
    var showLogs by rememberSaveable { mutableStateOf(false) }
    var showUninstall by rememberSaveable { mutableStateOf(false) }
    val loadingDialog = rememberLoadingDialog()
    val openSidebar = dropUnlessResumed { navigator.push(Route.SidebarWidgetSettings) }
    val openAbout = dropUnlessResumed { navigator.push(Route.About) }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(deviceViewModel::pickPhoto)
    }
    fun launchExternal(intent: Intent) {
        runCatching { context.startActivity(intent) }.onFailure {
            Toast.makeText(context, R.string.settings_shortcut_unavailable, Toast.LENGTH_LONG).show()
        }
    }
    fun choosePhoto() {
        runCatching { photoLauncher.launch(arrayOf("image/*")) }.onFailure {
            Toast.makeText(context, R.string.settings_shortcut_unavailable, Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(device.error) {
        device.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            deviceViewModel.dismissError()
        }
    }
    BackHandler(showAllSettings) { showAllSettings = false }
    val animationsEnabled = rememberSystemAnimationsEnabled()
    val contentLayoutDirection = LocalLayoutDirection.current
    val layoutDirection = if (contentLayoutDirection == LayoutDirection.Rtl) -1 else 1
    AnimatedContent(
        targetState = showAllSettings,
        modifier = Modifier.fillMaxSize().background(pageColor).clipToBounds(),
        transitionSpec = {
            if (!animationsEnabled) {
                ContentTransform(EnterTransition.None, ExitTransition.None, sizeTransform = null)
            } else {
                val direction = if (targetState) layoutDirection else -layoutDirection
                ContentTransform(
                    targetContentEnter = fadeIn(tween(110)) + slideInHorizontally(
                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                        initialOffsetX = { width -> width * direction / 4 },
                    ),
                    initialContentExit = fadeOut(tween(70)) + slideOutHorizontally(
                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                        targetOffsetX = { width -> -width * direction / 10 },
                    ),
                    targetContentZIndex = 1f,
                    sizeTransform = null,
                )
            }
        },
        label = "sidebarSettingsPage",
    ) { isMoreSettings ->
        if (isMoreSettings) {
            SettingsHubScreen(
                uiState = uiState,
                bottomInnerPadding = bottomInnerPadding,
                onOpenCategory = onOpenCategory,
                onPageModeChange = onPageModeChange,
                isSidebarMoreSettings = true,
                onNavigateBack = { showAllSettings = false },
            )
        } else {
            ExpressiveScaffold(
                modifier = Modifier.fillMaxSize(),
                contentMaxWidth = SidebarUiTokens.DetailMaxWidth,
                containerColor = pageColor,
                contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.SemiBold) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = immersiveTopBarColor(if (sidebarStyle) MaterialTheme.colorScheme.surface else Color.Transparent),
                        ),
                    )
                },
            ) { padding ->
                val contentInsets = PaddingValues(
                    start = padding.calculateStartPadding(contentLayoutDirection),
                    top = padding.calculateTopPadding(),
                    end = padding.calculateEndPadding(contentLayoutDirection),
                )
                LazyColumn(
                    // Keep scrolling cards below the transparent app bar, not behind its actions.
                    modifier = Modifier.fillMaxSize()
                        .padding(contentInsets)
                        .consumeWindowInsets(contentInsets)
                        .clipToBounds(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        top = 4.dp,
                        bottom = padding.calculateBottomPadding() + bottomInnerPadding + 20.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(SidebarUiTokens.SectionSpacing),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    item(key = "sidebar_settings_intro") {
                        SettingsHomeCard {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = managerDisplayName(
                                            customName = uiState.customManagerName,
                                            defaultName = stringResource(R.string.app_name),
                                            syncEnabled = uiState.syncManagerNameToSettingsHeader,
                                        ),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Icon(if (sidebarStyle) Icons.AutoMirrored.Rounded.ViewSidebar else Icons.Rounded.Palette,
                                        null, Modifier.size(30.dp))
                                }
                                Text(stringResource(R.string.settings_sidebar_intro), style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold)
                                Text(stringResource(R.string.settings_home_style_version, styleName, BuildConfig.VERSION_NAME),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (sidebarStyle) {
                                    TextButton(onClick = openSidebar, contentPadding = PaddingValues(horizontal = 0.dp)) {
                                        Text(stringResource(R.string.sidebar_widget_settings_title))
                                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.padding(start = 8.dp).size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                    item(key = "sidebar_settings_device") {
                        SettingsDeviceCard(state = device)
                    }
                    item(key = "sidebar_settings_navigation") {
                        SettingsHomeCard {
                            SidebarSettingsEntry(
                                stringResource(R.string.settings_sidebar_appearance),
                                stringResource(if (sidebarStyle) R.string.settings_sidebar_appearance_summary else R.string.settings_home_appearance_summary),
                                Icons.Rounded.Palette,
                            ) { onOpenCategory(SettingsCategory.Appearance) }
                            SidebarSettingsEntry(
                                stringResource(R.string.settings_sidebar_more),
                                stringResource(R.string.settings_sidebar_more_summary),
                                Icons.Rounded.Tune,
                            ) { showAllSettings = true }
                        }
                    }
                    item(key = "sidebar_settings_shortcuts") {
                        val repository = stringResource(R.string.home_learn_kernelsu_url).trimEnd('/')
                        SettingsHomeCard {
                            BoxWithConstraints(Modifier.fillMaxWidth().padding(8.dp)) {
                                val columns = settingsShortcutColumns(maxWidth.value, LocalDensity.current.fontScale)
                                val shortcuts = listOf(
                                    SettingsShortcut(R.string.settings_shortcut_color_mode, Icons.Rounded.DarkMode, { showColorMode = true }),
                                    SettingsShortcut(R.string.settings_device_text_edit, Icons.Rounded.Edit, {
                                        modelDraft = device.customModel.orEmpty()
                                        processorDraft = device.customProcessor.orEmpty()
                                        systemDraft = device.customSystem.orEmpty()
                                        showDeviceTextEditor = true
                                    }, enabled = !device.loading),
                                    SettingsShortcut(R.string.settings_page_mode_select, Icons.Rounded.Dashboard, { showPageMode = true }),
                                    SettingsShortcut(R.string.settings_shortcut_share, Icons.Rounded.Share, {
                                        launchExternal(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "SterSU ${BuildConfig.VERSION_NAME}\n$repository")
                                        }, context.getString(R.string.settings_shortcut_share)))
                                    }),
                                    SettingsShortcut(R.string.settings_shortcut_releases, Icons.Rounded.SystemUpdate, {
                                        launchExternal(Intent(Intent.ACTION_VIEW, "$repository/releases".toUri()))
                                    }),
                                    SettingsShortcut(R.string.settings_uninstall, Icons.Rounded.DeleteOutline, {
                                        if (uiState.runtimeModeResolved && uiState.isLkmMode) showUninstall = true
                                        else Toast.makeText(context, R.string.settings_shortcut_uninstall_unavailable, Toast.LENGTH_LONG).show()
                                    }),
                                    SettingsShortcut(R.string.settings_shortcut_logs, Icons.Rounded.BugReport, { showLogs = true }),
                                    SettingsShortcut(R.string.about, Icons.Rounded.Info, openAbout),
                                )
                                Column {
                                    shortcuts.chunked(columns).forEach { row ->
                                        Row(Modifier.fillMaxWidth()) {
                                            row.forEach { shortcut ->
                                                Column(
                                                    Modifier.weight(1f).clickable(enabled = shortcut.enabled, role = Role.Button, onClick = shortcut.onClick)
                                                        .heightIn(min = 88.dp).padding(horizontal = 3.dp, vertical = 14.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(7.dp),
                                                ) {
                                                    Icon(shortcut.icon, null, Modifier.size(27.dp))
                                                    Text(stringResource(shortcut.label), style = MaterialTheme.typography.labelMedium,
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                                }
                                            }
                                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Retain text drafts and scroll position while the crop dialog temporarily replaces this one.
    if (showDeviceTextEditor && device.editing == null) {
        AlertDialog(
            onDismissRequest = { showDeviceTextEditor = false },
            title = { Text(stringResource(R.string.settings_device_text_edit)) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = adaptiveDialogContentMaxHeight())
                        .verticalScroll(deviceEditorScrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = modelDraft,
                        onValueChange = { modelDraft = it.take(MAX_DEVICE_TEXT_LENGTH) },
                        label = { Text(stringResource(R.string.settings_device_model)) },
                        placeholder = { Text(device.info.model ?: stringResource(R.string.settings_device_unknown)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = systemDraft,
                        onValueChange = { systemDraft = it.take(MAX_DEVICE_TEXT_LENGTH) },
                        label = { Text(stringResource(R.string.settings_device_system)) },
                        placeholder = {
                            Text(stringResource(R.string.settings_device_android,
                                device.info.androidVersion ?: stringResource(R.string.settings_device_unknown)))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = processorDraft,
                        onValueChange = { processorDraft = it.take(MAX_DEVICE_TEXT_LENGTH) },
                        label = { Text(stringResource(R.string.settings_device_processor)) },
                        placeholder = { Text(device.info.processor ?: stringResource(R.string.settings_device_unknown)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        stringResource(R.string.settings_device_text_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = {
                        modelDraft = ""
                        processorDraft = ""
                        systemDraft = ""
                    }) {
                        Text(stringResource(R.string.settings_device_text_reset))
                    }
                    Text(
                        stringResource(R.string.settings_device_photo),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Column {
                        TextButton(
                            onClick = ::choosePhoto,
                            enabled = !device.loading && !device.busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text(stringResource(R.string.settings_device_photo_choose))
                        }
                        if (device.photo != null) {
                            TextButton(
                                onClick = deviceViewModel::editPhoto,
                                enabled = !device.loading && !device.busy,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) {
                                Text(stringResource(R.string.settings_device_photo_edit))
                            }
                            TextButton(
                                onClick = deviceViewModel::clearPhoto,
                                enabled = !device.loading && !device.busy,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) {
                                Text(stringResource(R.string.settings_device_photo_reset))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    deviceViewModel.saveCustomText(model = modelDraft, processor = processorDraft, system = systemDraft)
                    showDeviceTextEditor = false
                }, enabled = !device.loading) { Text(stringResource(R.string.settings_device_text_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeviceTextEditor = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
    device.editing?.let { editing ->
        var confirmedCrop by remember(editing) { mutableStateOf(editing.crop) }
        SettingsWallpaperCropDialog(
            show = true, uriString = editing.uri, crop = editing.crop,
            onCropChange = { confirmedCrop = it },
            onTransformChange = { deviceViewModel.savePhoto(confirmedCrop, it) },
            transform = editing.transform,
            onDismissRequest = deviceViewModel::cancelEdit,
            title = stringResource(R.string.settings_device_photo_crop),
            editorAspectRatio = 1f, defaultCrop = FULL_CUSTOM_WALLPAPER_CROP,
            allowTransforms = false,
        )
    }
    if (showPageMode) {
        SettingsPageModeDialog(
            currentMode = SettingsPageMode.Categories,
            onModeChange = onPageModeChange,
            onDismissRequest = { showPageMode = false },
        )
    }
    if (showColorMode) {
        val choices = listOf(
            ColorMode.SYSTEM to R.string.settings_theme_mode_system,
            ColorMode.LIGHT to R.string.settings_theme_mode_light,
            ColorMode.DARK to R.string.settings_theme_mode_dark,
        )
        AlertDialog(
            onDismissRequest = { showColorMode = false },
            title = { Text(stringResource(R.string.settings_shortcut_color_mode)) },
            text = {
                Column(Modifier.selectableGroup()) {
                    choices.forEach { (mode, label) ->
                        val selected = when (mode) {
                            ColorMode.SYSTEM -> ColorMode.fromValue(uiState.themeMode).isSystem
                            ColorMode.DARK -> ColorMode.fromValue(uiState.themeMode).isDark
                            else -> !ColorMode.fromValue(uiState.themeMode).isSystem && !ColorMode.fromValue(uiState.themeMode).isDark
                        }
                        Row(
                            Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton) {
                                onSetColorMode(mode)
                                showColorMode = false
                            }.heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected, onClick = null)
                            Text(stringResource(label), Modifier.padding(start = 12.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showColorMode = false }) { Text(stringResource(android.R.string.cancel)) } },
        )
    }
    SendLogDialog(showLogs, { showLogs = false }, loadingDialog)
    UninstallDialog(showUninstall, { showUninstall = false })
}

private data class SettingsShortcut(
    val label: Int,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)

@Composable
private fun SidebarSettingsEntry(
    title: String,
    summary: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 80.dp).padding(SidebarUiTokens.PagePadding),
        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(27.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsDeviceCard(state: SettingsDeviceState) {
    val context = LocalContext.current
    val unknown = stringResource(if (state.loading) R.string.loading else R.string.settings_device_unknown)
    SettingsHomeCard {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                state.customModel ?: state.info.model ?: unknown,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(state.customSystem ?: stringResource(R.string.settings_device_android, state.info.androidVersion ?: unknown),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stack = maxWidth < 220.dp || LocalDensity.current.fontScale > 1.4f
                val photo: @Composable (Modifier) -> Unit = { modifier ->
                    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.fillMaxWidth().heightIn(min = 164.dp, max = 240.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            val bitmap = state.bitmap
                            when {
                                state.busy || state.loading -> CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                                bitmap != null -> Image(
                                    bitmap = remember(bitmap) { bitmap.asImageBitmap() },
                                    contentDescription = stringResource(R.string.settings_device_photo),
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 224.dp),
                                )
                                else -> Column(horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Rounded.PhotoCamera, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(stringResource(R.string.settings_device_photo_empty),
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                        }
                    }
                }
                val details: @Composable (Modifier) -> Unit = { modifier ->
                    Column(modifier, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        DeviceSpec(stringResource(R.string.settings_device_processor), state.customProcessor ?: state.info.processor ?: unknown)
                        DeviceSpec(stringResource(R.string.settings_device_memory), state.info.totalMemoryBytes
                            ?.let { Formatter.formatShortFileSize(context, it) } ?: unknown)
                        DeviceSpec(stringResource(R.string.settings_device_manager), "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    }
                }
                if (stack) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        photo(Modifier.fillMaxWidth())
                        details(Modifier.fillMaxWidth())
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        photo(Modifier.weight(1f))
                        details(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceSpec(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
            maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}
