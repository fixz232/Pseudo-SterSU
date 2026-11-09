package me.weishu.kernelsu.ui.screen.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Adb
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.ElectricalServices
import androidx.compose.material.icons.rounded.Fence
import androidx.compose.material.icons.rounded.FolderDelete
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.RemoveModerator
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import me.weishu.kernelsu.ui.component.passthrough.MaterialIcon as Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.component.StyledSwitch
import me.weishu.kernelsu.ui.component.ApkeSecondaryScaffold
import me.weishu.kernelsu.ui.component.ApkeUiTokens
import me.weishu.kernelsu.ui.component.dialog.rememberLoadingDialog
import me.weishu.kernelsu.ui.component.miuix.SendLogDialog
import me.weishu.kernelsu.ui.component.uninstalldialog.UninstallDialog
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.theme.LocalImmersiveBackgroundActive
import me.weishu.kernelsu.ui.theme.immersiveScrolledTopBarColor
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor
import me.weishu.kernelsu.ui.theme.immersivePageColor
import me.weishu.kernelsu.ui.theme.immersiveTopBarColor
import me.weishu.kernelsu.ui.util.INTERFACE_STYLE_RESULT_KEY
import me.weishu.kernelsu.ui.util.ManagerPlugin
import me.weishu.kernelsu.ui.viewmodel.SettingsViewModel
import me.weishu.kernelsu.ui.component.miuix.SunMoonSwitchPreference
import me.weishu.kernelsu.ui.component.passthrough.MiuixText
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsCategoryScreen(routeValue: String) {
    val category = SettingsCategory.fromRouteValue(routeValue)
    val navigator = LocalNavigator.current
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val onBack = dropUnlessResumed { navigator.pop() }
    var showSendLogDialog by rememberSaveable { mutableStateOf(false) }
    var showUninstallDialog by rememberSaveable { mutableStateOf(false) }
    var showStealthModeDialog by rememberSaveable { mutableStateOf(false) }
    var stealthModeDialogEnablesMode by rememberSaveable { mutableStateOf(false) }
    var highlightedInterfaceStyleId by rememberSaveable { mutableStateOf<String?>(null) }
    val loadingDialog = rememberLoadingDialog()
    LaunchedEffect(category, navigator) {
        if (category == SettingsCategory.Appearance) {
            navigator.observeResult<String>(INTERFACE_STYLE_RESULT_KEY).collect { styleId ->
                highlightedInterfaceStyleId = styleId
                viewModel.refresh()
                navigator.clearResult(INTERFACE_STYLE_RESULT_KEY)
            }
        }
    }
    LifecycleResumeEffect(category.routeValue) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    SettingsCategoryScaffold(
        category = category,
        onBack = onBack,
        miuix = category == SettingsCategory.Appearance && uiState.uiMode != InterfaceStyle.Material.value,
    ) {
        when (category) {
            SettingsCategory.Appearance -> {
                if (!uiState.appearanceResolved) {
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else AppearanceSettingsContent(
                uiState = uiState,
                onSetAlphaDeltaMode = { useDelta ->
                    viewModel.setUiMode(if (useDelta) InterfaceStyle.Delta.value else InterfaceStyle.Alpha.value)
                },
                onSetMiuixClassicHomeLayout = viewModel::setMiuixClassicHomeLayoutEnabled,
                onSetSeasonCardMotion = viewModel::setSeasonCardMotionEnabled,
                onSetDayNightMode = viewModel::setDayNightMode,
                onSetPixelCardMotion = viewModel::setPixelCardMotionEnabled,
                highlightedInterfaceStyleId = highlightedInterfaceStyleId,
                onOpen = { route ->
                    if (route == Route.InterfaceStyleStore) {
                        navigator.navigateForResult(route, INTERFACE_STYLE_RESULT_KEY)
                    } else {
                        navigator.push(route)
                    }
                },
                )
            }
            SettingsCategory.HomeAndManager -> HomeManagerSettingsContent(
                uiState = uiState,
                onSetShowSupportCard = viewModel::setShowHomeSupportCard,
                onSetShowLearnCard = viewModel::setShowHomeLearnCard,
                onOpen = navigator::push,
            )
            SettingsCategory.RootAndPermissions -> RootPermissionSettingsContent(
                uiState = uiState,
                onSetSuCompatMode = viewModel::setSuCompatMode,
                onSetSulog = viewModel::setSulogEnabled,
                onSetAdbRoot = viewModel::setAdbRootEnabled,
                onSetShellSuAdbOnly = viewModel::setShellSuAdbOnlyEnabled,
                onSetUseSoftReboot = viewModel::setUseSoftReboot,
                onSetAutoJailbreak = viewModel::setAutoJailbreak,
                onOpen = navigator::push,
            )
            SettingsCategory.MountAndHide -> MountHideSettingsContent(
                uiState = uiState,
                onSetKernelUmount = viewModel::setKernelUmountEnabled,
                onSetWebViewZygoteUmount = viewModel::setWebViewZygoteUmountEnabled,
                onSetDefaultUmountModules = viewModel::setDefaultUmountModules,
                onSetSelinuxHide = viewModel::setSelinuxHideEnabled,
                onSetAvcSpoof = viewModel::setAvcSpoofEnabled,
                onOpenPathConfig = {
                    when (uiState.pathConfigBackend) {
                        PathConfigBackend.SusfsGki -> navigator.push(Route.SusfsPathConfig)
                        PathConfigBackend.Disabled,
                        PathConfigBackend.Unknown,
                        -> Unit
                    }
                },
                onSetEpkesuHide = viewModel::setEpkesuHideEnabled,
                onOpenKpm = { navigator.push(Route.Kpm) },
                onOpenLkmSusfs = { navigator.push(Route.LkmSusfs) },
            )
            SettingsCategory.Plugins -> PluginSettingsContent(
                uiState = uiState,
                onOpen = navigator::push,
                onSetGraphicsRendererEnabled = viewModel::setGraphicsRendererFeatureEnabled,
            )
            SettingsCategory.AppAndMaintenance -> AppMaintenanceSettingsContent(
                uiState = uiState,
                onSetWebDebugging = viewModel::setEnableWebDebugging,
                onSetCheckModuleUpdate = viewModel::setCheckModuleUpdate,
                onSetVersionMismatchWarning = viewModel::setShowVersionMismatchWarning,
                onSetGkiWarning = viewModel::setShowGkiWarning,
                onSendLog = { showSendLogDialog = true },
                onUninstall = { showUninstallDialog = true },
                onOpen = navigator::push,
                onOpenStealthModeDialog = { enableAfterSave ->
                    if (enableAfterSave) {
                        navigator.push(Route.StealthRecoveryCheck)
                    } else {
                        stealthModeDialogEnablesMode = false
                        showStealthModeDialog = true
                    }
                },
            )
        }
    }

    SendLogDialog(
        show = showSendLogDialog,
        onDismissRequest = { showSendLogDialog = false },
        loadingDialog = loadingDialog,
    )
    UninstallDialog(
        show = showUninstallDialog,
        onDismissRequest = { showUninstallDialog = false },
    )
    StealthModeCodeDialog(
        show = showStealthModeDialog,
        currentCode = uiState.stealthModeCode,
        enableAfterSave = stealthModeDialogEnablesMode,
        busy = uiState.stealthModeBusy,
        onDismissRequest = { showStealthModeDialog = false },
        onConfirm = { code ->
            viewModel.setStealthMode(stealthModeDialogEnablesMode, code) {
                showStealthModeDialog = false
            }
        },
    )
}

@Composable
internal fun SettingsCategoryScaffold(
    category: SettingsCategory,
    onBack: () -> Unit,
    miuix: Boolean = false,
    title: String = stringResource(category.titleRes),
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val pageContent: @Composable (PaddingValues) -> Unit = { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(
                        start = ApkeUiTokens.PageHorizontalPadding,
                        top = innerPadding.calculateTopPadding() + 4.dp,
                        end = ApkeUiTokens.PageHorizontalPadding,
                        bottom = bottomPadding + 18.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content()
            }
        }
    }
    if (miuix) {
        MiuixScaffold(
            containerColor = immersivePageColor(MiuixTheme.colorScheme.background),
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            popupHost = { },
            topBar = {
                SmallTopAppBar(
                    title = title,
                    color = immersiveTopBarColor(MiuixTheme.colorScheme.surface),
                    navigationIcon = {
                        val layoutDirection = LocalLayoutDirection.current
                        MiuixIconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                modifier = Modifier.graphicsLayer {
                                    if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                                },
                                contentDescription = stringResource(R.string.back),
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    },
                )
            },
            content = pageContent,
        )
    } else {
        ApkeSecondaryScaffold(
            title = title,
            onBack = onBack,
            maxContentWidth = 680.dp,
        ) { innerPadding, _ -> pageContent(innerPadding) }
    }
}

@Composable
private fun AppearanceSettingsContent(
    uiState: SettingsUiState,
    onSetAlphaDeltaMode: (Boolean) -> Unit,
    onSetMiuixClassicHomeLayout: (Boolean) -> Unit,
    onSetSeasonCardMotion: (Boolean) -> Unit,
    onSetDayNightMode: (Boolean) -> Unit,
    onSetPixelCardMotion: (Boolean) -> Unit,
    highlightedInterfaceStyleId: String?,
    onOpen: (Route) -> Unit,
) {
    val miuix = uiState.uiMode != InterfaceStyle.Material.value
    val stylePickerRequester = remember { BringIntoViewRequester() }
    val highlightStylePicker = uiState.installedInterfaceStyles.any {
        it.style.id == highlightedInterfaceStyleId
    }
    LaunchedEffect(highlightedInterfaceStyleId, highlightStylePicker) {
        if (highlightStylePicker) stylePickerRequester.bringIntoView()
    }
    SettingsGroup(stringResource(R.string.settings_group_interface), miuix = miuix) {
        SettingsActionRow(
            title = stringResource(R.string.store_title),
            summary = stringResource(R.string.store_summary),
            icon = Icons.Rounded.Storefront,
            miuix = miuix,
            onClick = { onOpen(Route.ThemeStore) },
        )
        if (uiState.isSidebarDesignEntryVisible) {
            SettingsActionRow(
                title = stringResource(R.string.sidebar_widget_settings_title),
                summary = stringResource(R.string.sidebar_design_entry_summary),
                icon = Icons.Rounded.Tune,
                miuix = miuix,
                onClick = { onOpen(Route.SidebarWidgetSettings) },
            )
        }
        SettingsActionRow(
            title = stringResource(R.string.settings_ui_mode),
            summary = stringResource(R.string.settings_ui_mode_summary),
            icon = Icons.Rounded.Dashboard,
            modifier = Modifier.bringIntoViewRequester(stylePickerRequester),
            highlighted = highlightStylePicker,
            miuix = miuix,
            onClick = { onOpen(Route.InterfaceStyles) },
        )
        if (uiState.uiMode == InterfaceStyle.Miuix.value) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_miuix_classic_home_layout),
                summary = stringResource(R.string.settings_miuix_classic_home_layout_summary),
                icon = Icons.Rounded.Dashboard,
                checked = uiState.miuixClassicHomeLayoutEnabled,
                miuix = miuix,
                onCheckedChange = onSetMiuixClassicHomeLayout,
            )
        }
        if (uiState.activeInterfaceStyleId == "alpha-delta" &&
            (uiState.uiMode == InterfaceStyle.Alpha.value || uiState.uiMode == InterfaceStyle.Delta.value)) {
            SettingsChoiceRow(
                title = stringResource(R.string.settings_alpha_delta_mode),
                summary = stringResource(R.string.settings_alpha_delta_mode_summary),
                icon = Icons.Rounded.Tune,
                options = listOf(
                    stringResource(R.string.settings_alpha_mode),
                    stringResource(R.string.settings_delta_mode),
                ),
                selectedIndex = if (uiState.uiMode == InterfaceStyle.Delta.value) 1 else 0,
                miuix = miuix,
                onSelected = { onSetAlphaDeltaMode(it == 1) },
            )
        }
        if (uiState.uiMode == InterfaceStyle.Snow.value) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_season_card_motion),
                summary = stringResource(R.string.settings_season_card_motion_summary),
                icon = Icons.Rounded.AutoFixHigh,
                checked = uiState.seasonCardMotionEnabled,
                miuix = miuix,
                onCheckedChange = onSetSeasonCardMotion,
            )
        }
        if (uiState.uiMode == InterfaceStyle.Rain.value) {
            SettingsActionRow(
                title = stringResource(R.string.rain_design_title),
                summary = stringResource(R.string.rain_design_summary),
                icon = Icons.Rounded.Tune,
                miuix = miuix,
                onClick = { onOpen(Route.RainDesign) },
            )
        }
        if (uiState.uiMode == InterfaceStyle.Pixel.value) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_pixel_card_motion),
                summary = stringResource(R.string.settings_pixel_card_motion_summary),
                icon = Icons.Rounded.AutoFixHigh,
                checked = uiState.pixelCardMotionEnabled,
                miuix = miuix,
                onCheckedChange = onSetPixelCardMotion,
            )
        }
        SettingsSwitchRow(
            title = stringResource(R.string.settings_day_night_switch),
            summary = stringResource(R.string.settings_day_night_switch_summary),
            icon = Icons.Rounded.Palette,
            checked = isDayNightSwitchChecked(uiState.themeMode),
            miuix = miuix,
            onCheckedChange = onSetDayNightMode,
        )
    }
}

@Composable
private fun HomeManagerSettingsContent(
    uiState: SettingsUiState,
    onSetShowSupportCard: (Boolean) -> Unit,
    onSetShowLearnCard: (Boolean) -> Unit,
    onOpen: (Route) -> Unit,
) {
    SettingsGroup(stringResource(R.string.settings_group_manager)) {
        SettingsActionRow(
            title = stringResource(R.string.settings_manager_identity),
            summary = stringResource(R.string.settings_manager_identity_summary),
            icon = Icons.Rounded.Apps,
            onClick = { onOpen(Route.LauncherIcon) },
        )
        SettingsActionRow(
            title = stringResource(R.string.dynamic_manager_title),
            summary = stringResource(R.string.dynamic_manager_settings_summary),
            icon = Icons.Rounded.AdminPanelSettings,
            onClick = { onOpen(Route.DynamicManager) },
        )
    }
    SettingsGroup(stringResource(R.string.settings_group_home_cards)) {
        SettingsSwitchRow(
            title = stringResource(R.string.settings_show_home_support_card),
            summary = stringResource(R.string.settings_show_home_support_card_summary),
            icon = Icons.Rounded.Visibility,
            checked = uiState.showHomeSupportCard,
            onCheckedChange = onSetShowSupportCard,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.settings_show_home_learn_card),
            summary = stringResource(R.string.settings_show_home_learn_card_summary),
            icon = Icons.Rounded.Visibility,
            checked = uiState.showHomeLearnCard,
            onCheckedChange = onSetShowLearnCard,
        )
    }
}

@Composable
private fun RootPermissionSettingsContent(
    uiState: SettingsUiState,
    onSetSuCompatMode: (Int) -> Unit,
    onSetSulog: (Boolean) -> Unit,
    onSetAdbRoot: (Boolean) -> Unit,
    onSetShellSuAdbOnly: (Boolean) -> Unit,
    onSetUseSoftReboot: (Boolean) -> Unit,
    onSetAutoJailbreak: (Boolean) -> Unit,
    onOpen: (Route) -> Unit,
) {
    SettingsGroup(stringResource(R.string.settings_group_app_profiles)) {
        SettingsActionRow(
            title = stringResource(R.string.settings_profile_template),
            summary = stringResource(R.string.settings_profile_template_summary),
            icon = Icons.Rounded.Fence,
            onClick = { onOpen(Route.AppProfileTemplate) },
        )
    }
    SettingsGroup(stringResource(R.string.settings_group_root_core)) {
        val suSummary = when (uiState.suCompatStatus) {
            "unsupported" -> stringResource(R.string.feature_status_unsupported_summary)
            "managed" -> stringResource(R.string.feature_status_managed_summary)
            else -> stringResource(R.string.settings_sucompat_summary)
        }
        SettingsChoiceRow(
            title = stringResource(R.string.settings_sucompat),
            summary = suSummary,
            icon = Icons.Rounded.RemoveModerator,
            options = listOf(
                stringResource(R.string.settings_mode_enable_by_default),
                stringResource(R.string.settings_mode_disable_until_reboot),
                stringResource(R.string.settings_mode_disable_always),
            ),
            selectedIndex = uiState.suCompatMode,
            enabled = uiState.suCompatStatus == "supported",
            onSelected = onSetSuCompatMode,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.settings_sulog),
            summary = featureSummary(uiState.sulogStatus, R.string.settings_sulog_summary),
            icon = Icons.AutoMirrored.Rounded.Article,
            enabled = uiState.sulogStatus == "supported",
            checked = uiState.isSulogEnabled,
            onCheckedChange = onSetSulog,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.settings_adb_root),
            summary = featureSummary(uiState.adbRootStatus, R.string.settings_adb_root_summary),
            icon = Icons.Rounded.Adb,
            enabled = uiState.adbRootStatus == "supported",
            checked = uiState.isAdbRootEnabled,
            onCheckedChange = onSetAdbRoot,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.settings_shell_su_adb_only),
            summary = featureSummary(uiState.shellSuAdbOnlyStatus, R.string.settings_shell_su_adb_only_summary),
            icon = Icons.Rounded.VerifiedUser,
            enabled = uiState.shellSuAdbOnlyStatus == "supported",
            checked = uiState.isShellSuAdbOnlyEnabled,
            onCheckedChange = onSetShellSuAdbOnly,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.settings_soft_reboot),
            summary = stringResource(R.string.settings_soft_reboot_summary),
            icon = Icons.Rounded.ElectricalServices,
            enabled = !uiState.isLateLoadMode,
            checked = uiState.isLateLoadMode || uiState.useSoftReboot,
            onCheckedChange = onSetUseSoftReboot,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.settings_auto_jailbreak),
            summary = stringResource(R.string.settings_auto_jailbreak_summary),
            icon = Icons.Rounded.ElectricalServices,
            enabled = uiState.isLateLoadMode,
            checked = uiState.autoJailbreak,
            onCheckedChange = onSetAutoJailbreak,
        )
    }
}

@Composable
private fun MountHideSettingsContent(
    uiState: SettingsUiState,
    onSetKernelUmount: (Boolean) -> Unit,
    onSetWebViewZygoteUmount: (Boolean) -> Unit,
    onSetDefaultUmountModules: (Boolean) -> Unit,
    onSetSelinuxHide: (Boolean) -> Unit,
    onSetAvcSpoof: (Boolean) -> Unit,
    onOpenPathConfig: () -> Unit,
    onSetEpkesuHide: (Boolean) -> Unit,
    onOpenKpm: () -> Unit,
    onOpenLkmSusfs: () -> Unit,
) {
    if (uiState.isKpmSettingsEntryVisible ||
        (uiState.runtimeModeResolved && uiState.isLkmMode && !uiState.isLateLoadMode)) {
        SettingsGroup(stringResource(R.string.settings_group_kernel_modules)) {
            if (uiState.isKpmSettingsEntryVisible) {
                SettingsActionRow(
                    title = stringResource(R.string.kpm_title),
                    summary = stringResource(R.string.kpm_settings_summary),
                    icon = Icons.Rounded.Bolt,
                    onClick = onOpenKpm,
                )
            }
            if (uiState.runtimeModeResolved && uiState.isLkmMode && !uiState.isLateLoadMode) {
                SettingsActionRow(
                    title = stringResource(R.string.lkm_susfs_title),
                    summary = stringResource(R.string.lkm_susfs_summary),
                    icon = Icons.Rounded.Shield,
                    onClick = onOpenLkmSusfs,
                )
            }
        }
    }
    SettingsGroup(stringResource(R.string.settings_group_mounting)) {
        SettingsSwitchRow(
            title = stringResource(R.string.settings_kernel_umount),
            summary = featureSummary(uiState.kernelUmountStatus, R.string.settings_kernel_umount_summary),
            icon = Icons.Rounded.FolderDelete,
            enabled = uiState.kernelUmountStatus == "supported",
            checked = uiState.isKernelUmountEnabled,
            onCheckedChange = onSetKernelUmount,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.settings_webview_zygote_umount),
            summary = featureSummary(
                uiState.webViewZygoteUmountStatus,
                R.string.settings_webview_zygote_umount_summary,
            ),
            icon = Icons.Rounded.Language,
            enabled = uiState.webViewZygoteUmountStatus == "supported",
            checked = uiState.isWebViewZygoteUmountEnabled,
            onCheckedChange = onSetWebViewZygoteUmount,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.settings_umount_modules_default),
            summary = stringResource(R.string.settings_umount_modules_default_summary),
            icon = Icons.Rounded.FolderDelete,
            checked = uiState.isDefaultUmountModules,
            onCheckedChange = onSetDefaultUmountModules,
        )
    }
    SettingsGroup(stringResource(R.string.settings_group_hiding)) {
        SettingsSwitchRow(
            title = stringResource(R.string.settings_selinux_hide),
            summary = featureSummary(uiState.selinuxHideStatus, R.string.settings_selinux_hide_summary),
            icon = Icons.Rounded.Shield,
            enabled = uiState.selinuxHideStatus == "supported",
            checked = uiState.isSelinuxHideEnabled,
            onCheckedChange = onSetSelinuxHide,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.settings_avc_spoof),
            summary = featureSummary(uiState.avcSpoofStatus, R.string.settings_avc_spoof_summary),
            icon = Icons.Rounded.Policy,
            enabled = uiState.avcSpoofStatus == "supported",
            checked = uiState.isAvcSpoofEnabled,
            onCheckedChange = onSetAvcSpoof,
        )
        if (uiState.isBuiltinPathConfigEntryVisible) {
            SettingsActionRow(
                title = pathConfigTitle(uiState),
                summary = pathConfigSummary(uiState),
                icon = Icons.Rounded.Visibility,
                enabled = uiState.canOpenPathConfig,
                onClick = onOpenPathConfig,
            )
        }
        SettingsSwitchRow(
            title = stringResource(R.string.settings_epkesu_hide),
            summary = stringResource(R.string.settings_epkesu_hide_summary),
            icon = Icons.Rounded.Security,
            checked = uiState.isEpkesuHideEnabled,
            onCheckedChange = onSetEpkesuHide,
        )
    }
}

@Composable
private fun PluginSettingsContent(
    uiState: SettingsUiState,
    onOpen: (Route) -> Unit,
    onSetGraphicsRendererEnabled: (Boolean) -> Unit,
) {
    val entries = SettingsCatalog.entriesFor(SettingsCategory.Plugins, uiState)
        .filter { it.plugin != null }
    SettingsGroup(stringResource(R.string.settings_hub_plugins)) {
        SettingsActionRow(
            title = stringResource(R.string.plugin_store_title),
            summary = stringResource(R.string.settings_plugins_store_summary),
            icon = Icons.Rounded.Storefront,
            onClick = { onOpen(Route.PluginStore) },
        )
    }
    if (entries.isEmpty()) {
        Text(
            text = stringResource(R.string.settings_plugins_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        )
    } else {
        SettingsGroup(stringResource(R.string.settings_plugins_installed)) {
            entries.forEach { entry ->
                val title = stringResource(entry.titleRes)
                val summary = entry.summaryRes?.let { stringResource(it) }.orEmpty()
                when (entry.key) {
                    "graphics_toggle" -> SettingsSwitchRow(
                        title = title,
                        summary = summary,
                        icon = Icons.Rounded.Tune,
                        checked = uiState.graphicsRendererFeatureEnabled,
                        onCheckedChange = onSetGraphicsRendererEnabled,
                    )
                    else -> {
                        val destination = when (entry.plugin) {
                            ManagerPlugin.RescueProtection -> Route.RescueProtection to Icons.Rounded.Security
                            ManagerPlugin.ImageTools -> Route.ImageTool to Icons.Rounded.ImageSearch
                            ManagerPlugin.CpuSpoof -> Route.CpuSpoof to Icons.Rounded.DeveloperMode
                            ManagerPlugin.DeviceIdentity -> Route.DeviceIdentity to Icons.Rounded.Badge
                            ManagerPlugin.AiChat -> Route.AiChat to Icons.Rounded.AutoFixHigh
                            ManagerPlugin.GraphicsRenderer -> Route.GraphicsRenderer to Icons.Rounded.Tune
                            ManagerPlugin.AppIdManager -> Route.AppIdManager to Icons.Rounded.Badge
                            ManagerPlugin.AppFreeze -> Route.AppFreeze to Icons.Rounded.Apps
                            ManagerPlugin.RemoteManagementSuite, null -> null
                        }
                        destination?.let { (route, icon) ->
                            SettingsActionRow(
                                title = title,
                                summary = summary,
                                icon = icon,
                                onClick = { onOpen(route) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppMaintenanceSettingsContent(
    uiState: SettingsUiState,
    onSetWebDebugging: (Boolean) -> Unit,
    onSetCheckModuleUpdate: (Boolean) -> Unit,
    onSetVersionMismatchWarning: (Boolean) -> Unit,
    onSetGkiWarning: (Boolean) -> Unit,
    onSendLog: () -> Unit,
    onUninstall: () -> Unit,
    onOpen: (Route) -> Unit,
    onOpenStealthModeDialog: (Boolean) -> Unit,
) {
    SettingsGroup(stringResource(R.string.settings_group_general)) {
        SettingsActionRow(
            title = stringResource(R.string.settings_language),
            summary = stringResource(R.string.settings_language_summary),
            icon = Icons.Rounded.Language,
            onClick = { onOpen(Route.LanguageSettings) },
        )
    }
    SettingsGroup(stringResource(R.string.settings_group_stealth_access)) {
        SettingsSwitchRow(
            title = stringResource(R.string.stealth_mode_title),
            summary = stringResource(R.string.stealth_mode_summary),
            icon = Icons.Rounded.Security,
            enabled = !uiState.stealthModeBusy && !uiState.stealthModeEnabled,
            checked = uiState.stealthModeEnabled,
            onCheckedChange = { if (it) onOpenStealthModeDialog(true) },
        )
        SettingsActionRow(
            title = stringResource(R.string.stealth_mode_code_title),
            summary = uiState.stealthModeCode,
            icon = Icons.Rounded.Visibility,
            enabled = !uiState.stealthModeBusy && !uiState.stealthModeEnabled,
            onClick = { onOpenStealthModeDialog(false) },
        )
    }
    SettingsGroup(stringResource(R.string.settings_group_updates_warnings)) {
        SettingsSwitchRow(
            title = stringResource(R.string.settings_module_check_update),
            summary = stringResource(R.string.settings_module_check_update_summary),
            icon = Icons.Rounded.UploadFile,
            checked = uiState.checkModuleUpdate,
            onCheckedChange = onSetCheckModuleUpdate,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.settings_version_mismatch_warning),
            summary = stringResource(R.string.settings_version_mismatch_warning_summary),
            icon = Icons.Rounded.BugReport,
            checked = uiState.showVersionMismatchWarning,
            onCheckedChange = onSetVersionMismatchWarning,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.settings_gki_warning),
            summary = stringResource(R.string.settings_gki_warning_summary),
            icon = Icons.Rounded.BugReport,
            checked = uiState.showGkiWarning,
            onCheckedChange = onSetGkiWarning,
        )
    }
    SettingsGroup(stringResource(R.string.settings_group_diagnostics)) {
        SettingsSwitchRow(
            title = stringResource(R.string.enable_web_debugging),
            summary = stringResource(R.string.enable_web_debugging_summary),
            icon = Icons.Rounded.DeveloperMode,
            checked = uiState.enableWebDebugging,
            onCheckedChange = onSetWebDebugging,
        )
        SettingsActionRow(
            title = stringResource(R.string.send_log),
            summary = stringResource(R.string.settings_log_export_summary),
            icon = Icons.Rounded.BugReport,
            onClick = onSendLog,
        )
    }
    SettingsGroup(stringResource(R.string.settings_group_maintenance_about)) {
        if (uiState.isLkmMode) {
            SettingsActionRow(
                title = stringResource(R.string.settings_uninstall),
                summary = stringResource(
                    if (!uiState.isLateLoadMode) {
                        R.string.settings_uninstall_summary
                    } else {
                        R.string.feature_status_unsupported_summary
                    }
                ),
                icon = Icons.Rounded.Delete,
                enabled = !uiState.isLateLoadMode,
                destructive = true,
                onClick = onUninstall,
            )
        }
        SettingsActionRow(
            title = stringResource(R.string.about),
            summary = stringResource(R.string.settings_about_summary),
            icon = Icons.Rounded.ContactPage,
            onClick = { onOpen(Route.About) },
        )
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    miuix: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (miuix) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MiuixText(
                text = title,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            MiuixCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                colors = MiuixCardDefaults.defaultColors(
                    color = immersiveSurfaceColor(MiuixTheme.colorScheme.surfaceContainer),
                ),
                insideMargin = PaddingValues(vertical = 4.dp),
            ) {
                Column(content = content)
            }
        }
        return
    }
    val shape = RoundedCornerShape(8.dp)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Surface(
            color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainerLow),
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = shape,
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    summary: String,
    icon: ImageVector,
    enabled: Boolean = true,
    destructive: Boolean = false,
    trailingText: String? = null,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier,
    miuix: Boolean = false,
    onClick: () -> Unit,
) {
    if (miuix) {
        ArrowPreference(
            title = title,
            summary = summary,
            modifier = modifier.background(
                if (highlighted) MiuixTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent,
            ),
            startAction = { MiuixSettingsRowIcon(icon, enabled) },
            enabled = enabled,
            onClick = onClick,
        )
        return
    }
    val accent = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        destructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (highlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                else Color.Transparent,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SettingsRowIcon(icon = icon, color = accent, enabled = enabled)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = if (enabled) accent.takeIf { destructive } ?: MaterialTheme.colorScheme.onSurface else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = summary,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailingText?.let {
            Text(
                text = it,
                color = accent,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 96.dp),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.8f else 0.35f),
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    summary: String,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean = true,
    miuix: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
) {
    if (miuix) {
        SunMoonSwitchPreference(
            title = title,
            summary = summary,
            startAction = { MiuixSettingsRowIcon(icon, enabled) },
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
        )
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .heightIn(min = 60.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SettingsRowIcon(icon = icon, color = MaterialTheme.colorScheme.primary, enabled = enabled)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.45f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = summary,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        StyledSwitch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SettingsChoiceRow(
    title: String,
    summary: String,
    icon: ImageVector,
    options: List<String>,
    selectedIndex: Int,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    miuix: Boolean = false,
    onSelected: (Int) -> Unit,
) {
    val safeIndex = selectedIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))
    if (miuix) {
        OverlayDropdownPreference(
            title = title,
            summary = summary,
            modifier = modifier.background(
                if (highlighted) MiuixTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent,
            ),
            items = options,
            startAction = { MiuixSettingsRowIcon(icon, enabled) },
            selectedIndex = safeIndex,
            enabled = enabled && options.isNotEmpty(),
            onSelectedIndexChange = onSelected,
        )
        return
    }
    var showDialog by rememberSaveable(title) { mutableStateOf(false) }
    SettingsActionRow(
        title = title,
        summary = summary,
        icon = icon,
        enabled = enabled && options.isNotEmpty(),
        trailingText = options.getOrNull(safeIndex),
        modifier = modifier,
        highlighted = highlighted,
        onClick = { showDialog = true },
    )
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState())
                        .selectableGroup(),
                ) {
                    options.forEachIndexed { index, option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = index == safeIndex,
                                    role = Role.RadioButton,
                                    onClick = {
                                        onSelected(index)
                                        showDialog = false
                                    },
                                )
                                .padding(vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = index == safeIndex,
                                onClick = null,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(option, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun MiuixSettingsRowIcon(icon: ImageVector, enabled: Boolean) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(22.dp),
        tint = if (enabled) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.disabledOnSecondaryVariant,
    )
}

@Composable
private fun SettingsRowIcon(icon: ImageVector, color: Color, enabled: Boolean) {
    Surface(
        color = color.copy(alpha = if (enabled) 0.13f else 0.06f),
        contentColor = color.copy(alpha = if (enabled) 1f else 0.45f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun featureSummary(status: String, defaultSummary: Int): String = when (status) {
    "unsupported" -> stringResource(R.string.feature_status_unsupported_summary)
    "managed" -> stringResource(R.string.feature_status_managed_summary)
    else -> stringResource(defaultSummary)
}
