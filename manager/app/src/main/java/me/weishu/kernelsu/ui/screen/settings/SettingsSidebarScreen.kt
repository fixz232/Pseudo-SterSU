package me.weishu.kernelsu.ui.screen.settings

import android.content.Intent
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ViewSidebar
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.DarkMode
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.dialog.rememberLoadingDialog
import me.weishu.kernelsu.ui.component.material.ExpressiveScaffold
import me.weishu.kernelsu.ui.component.miuix.SendLogDialog
import me.weishu.kernelsu.ui.component.uninstalldialog.UninstallDialog
import me.weishu.kernelsu.ui.navigation3.Navigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.theme.ColorMode
import me.weishu.kernelsu.ui.theme.SidebarUiTokens
import me.weishu.kernelsu.ui.util.FULL_CUSTOM_WALLPAPER_CROP

/** Compact, device-oriented settings for the sidebar style; other styles keep their layouts. */
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
    val deviceViewModel = viewModel<SettingsDeviceViewModel>()
    val device by deviceViewModel.state.collectAsStateWithLifecycle()
    var showAllSettings by rememberSaveable { mutableStateOf(false) }
    var showPhotoActions by rememberSaveable { mutableStateOf(false) }
    var showDeviceTextEditor by rememberSaveable { mutableStateOf(false) }
    var modelDraft by rememberSaveable { mutableStateOf("") }
    var processorDraft by rememberSaveable { mutableStateOf("") }
    var showColorMode by rememberSaveable { mutableStateOf(false) }
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
        showPhotoActions = false
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
    if (showAllSettings) {
        SettingsHubScreen(
            uiState = uiState,
            bottomInnerPadding = bottomInnerPadding,
            onOpenCategory = onOpenCategory,
            onPageModeChange = onPageModeChange,
            onOpenSidebarDesign = openSidebar,
            onNavigateBack = { showAllSettings = false },
        )
    } else {
        ExpressiveScaffold(
            modifier = Modifier.fillMaxSize(),
            contentMaxWidth = SidebarUiTokens.DetailMaxWidth,
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Top),
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.SemiBold) },
                    actions = {
                        SettingsPageModeButton(SettingsPageMode.Categories, onPageModeChange)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 4.dp,
                    bottom = padding.calculateBottomPadding() + bottomInnerPadding + 20.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(SidebarUiTokens.SectionSpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item(key = "sidebar_settings_intro") {
                    SidebarSettingsCard {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("SterSU", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f))
                                Icon(Icons.AutoMirrored.Rounded.ViewSidebar, null, Modifier.size(30.dp))
                            }
                            Text(stringResource(R.string.settings_sidebar_intro), style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.settings_sidebar_version, BuildConfig.VERSION_NAME),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = openSidebar, contentPadding = PaddingValues(horizontal = 0.dp)) {
                                Text(stringResource(R.string.sidebar_widget_settings_title))
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.padding(start = 8.dp).size(18.dp))
                            }
                        }
                    }
                }
                item(key = "sidebar_settings_device") {
                    SettingsDeviceCard(
                        state = device,
                        onEditPhoto = {
                            if (device.photo == null) choosePhoto() else showPhotoActions = true
                        },
                        onEditText = {
                            modelDraft = device.customModel.orEmpty()
                            processorDraft = device.customProcessor.orEmpty()
                            showDeviceTextEditor = true
                        },
                    )
                }
                item(key = "sidebar_settings_navigation") {
                    SidebarSettingsCard {
                        SidebarSettingsEntry(
                            stringResource(R.string.settings_sidebar_appearance),
                            stringResource(R.string.settings_sidebar_appearance_summary),
                            Icons.Rounded.Palette,
                        ) { onOpenCategory(SettingsCategory.Appearance) }
                        HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        SidebarSettingsEntry(
                            stringResource(R.string.settings_sidebar_more),
                            stringResource(R.string.settings_sidebar_more_summary),
                            Icons.Rounded.Tune,
                        ) { showAllSettings = true }
                    }
                }
                item(key = "sidebar_settings_shortcuts") {
                    val repository = stringResource(R.string.home_learn_kernelsu_url).trimEnd('/')
                    SidebarSettingsCard {
                        BoxWithConstraints(Modifier.fillMaxWidth().padding(8.dp)) {
                            val columns = settingsShortcutColumns(maxWidth.value, LocalDensity.current.fontScale)
                            val shortcuts = listOf(
                                SettingsShortcut(R.string.settings_shortcut_color_mode, Icons.Rounded.DarkMode, { showColorMode = true }),
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
                                                Modifier.weight(1f).clickable(role = Role.Button, onClick = shortcut.onClick)
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

    if (showPhotoActions) {
        AlertDialog(
            onDismissRequest = { showPhotoActions = false },
            title = { Text(stringResource(R.string.settings_device_photo)) },
            text = {
                Column {
                    TextButton(onClick = ::choosePhoto, enabled = !device.busy) {
                        Text(stringResource(R.string.settings_device_photo_choose))
                    }
                    TextButton(onClick = {
                        showPhotoActions = false
                        deviceViewModel.editPhoto()
                    }, enabled = !device.busy) { Text(stringResource(R.string.settings_device_photo_crop)) }
                    TextButton(onClick = {
                        showPhotoActions = false
                        deviceViewModel.clearPhoto()
                    }, enabled = !device.busy) { Text(stringResource(R.string.settings_device_photo_reset)) }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoActions = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
    if (showDeviceTextEditor) {
        AlertDialog(
            onDismissRequest = { showDeviceTextEditor = false },
            title = { Text(stringResource(R.string.settings_device_text_edit)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = modelDraft,
                        onValueChange = { modelDraft = it.take(MAX_DEVICE_TEXT_LENGTH) },
                        label = { Text(stringResource(R.string.settings_device_model)) },
                        placeholder = { Text(device.info.model ?: stringResource(R.string.settings_device_unknown)) },
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
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    deviceViewModel.saveCustomText(modelDraft, processorDraft)
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

private data class SettingsShortcut(val label: Int, val icon: ImageVector, val onClick: () -> Unit)

@Composable
private fun SidebarSettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
    ) { Column(content = content) }
}

@Composable
private fun SidebarSettingsEntry(title: String, summary: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
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
private fun SettingsDeviceCard(state: SettingsDeviceState, onEditPhoto: () -> Unit, onEditText: () -> Unit) {
    val context = LocalContext.current
    val unknown = stringResource(if (state.loading) R.string.loading else R.string.settings_device_unknown)
    SidebarSettingsCard {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    state.customModel ?: state.info.model ?: unknown,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = onEditText, enabled = !state.loading) {
                    Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.settings_device_text_edit))
                }
            }
            Text(stringResource(R.string.settings_device_android, state.info.androidVersion ?: unknown),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stack = maxWidth < 220.dp || LocalDensity.current.fontScale > 1.4f
                val photo: @Composable (Modifier) -> Unit = { modifier ->
                    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.fillMaxWidth().heightIn(min = 164.dp, max = 240.dp)
                                .clickable(enabled = !state.loading && !state.busy, role = Role.Button,
                                    onClickLabel = stringResource(R.string.settings_device_photo_choose), onClick = onEditPhoto),
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
                        TextButton(onClick = onEditPhoto, enabled = !state.loading && !state.busy) {
                            Text(stringResource(if (state.photo == null) R.string.settings_device_photo_choose else R.string.settings_device_photo_edit))
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
