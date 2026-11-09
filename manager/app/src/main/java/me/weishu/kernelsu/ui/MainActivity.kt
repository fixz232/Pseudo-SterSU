package me.weishu.kernelsu.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplayTransitionEffects
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.ui.util.SidebarSide
import me.weishu.kernelsu.ui.util.rememberSidebarWidgetConfig
import me.weishu.kernelsu.ui.component.AutoHidingNavigationBar
import me.weishu.kernelsu.ui.component.LocalBackgroundScrollFollowState
import me.weishu.kernelsu.ui.component.backgroundScrollFollowController
import me.weishu.kernelsu.ui.component.CustomWallpaperRoot
import me.weishu.kernelsu.ui.component.preloadCustomPageBackgroundImages
import me.weishu.kernelsu.ui.component.GlobalScrollEffect
import me.weishu.kernelsu.ui.component.GlobalScrollEffectOverlay
import me.weishu.kernelsu.ui.component.GlobalSnowEffect
import me.weishu.kernelsu.ui.component.GlobalSnowEffectOverlay
import me.weishu.kernelsu.ui.component.ThemeModeTransitionOverlay
import me.weishu.kernelsu.ui.component.LocalSwitchStyle
import me.weishu.kernelsu.ui.component.LocalNightBackgroundEffectActive
import me.weishu.kernelsu.ui.component.NightBackgroundEffect
import me.weishu.kernelsu.ui.component.NightBackgroundEffectOverlay
import me.weishu.kernelsu.ui.component.LocalPageTransitionEffect
import me.weishu.kernelsu.ui.component.PageTransitionEffect
import me.weishu.kernelsu.ui.component.mainPageTransition
import me.weishu.kernelsu.ui.component.rememberSystemAnimationsEnabled
import me.weishu.kernelsu.ui.component.navigationBarVisibilityController
import me.weishu.kernelsu.ui.component.rememberNavigationBarVisibilityState
import me.weishu.kernelsu.ui.component.StartupAnimationOverlay
import me.weishu.kernelsu.ui.component.SwitchStyle
import me.weishu.kernelsu.ui.component.bottombar.BottomBar
import me.weishu.kernelsu.ui.component.bottombar.MainDestination
import me.weishu.kernelsu.ui.component.bottombar.MainPagerState
import me.weishu.kernelsu.ui.component.bottombar.mainDestinations
import me.weishu.kernelsu.ui.component.bottombar.NavigationBadgeState
import me.weishu.kernelsu.ui.component.bottombar.SideRail
import me.weishu.kernelsu.ui.component.bottombar.SidebarWidgetRail
import me.weishu.kernelsu.ui.component.bottombar.SidebarPaneShape
import me.weishu.kernelsu.ui.component.bottombar.LocalSidebarGlassBackdrop
import me.weishu.kernelsu.ui.component.bottombar.LocalSidebarGlassBackground
import me.weishu.kernelsu.ui.component.bottombar.canCaptureSidebarWallpaper
import me.weishu.kernelsu.ui.component.bottombar.rememberSidebarGlassBackdrop
import me.weishu.kernelsu.ui.component.bottombar.sidebarGlassUnderlay
import me.weishu.kernelsu.ui.util.SidebarMaterial
import me.weishu.kernelsu.ui.component.bottombar.rememberMainPagerState
import me.weishu.kernelsu.ui.component.bottombar.useNavigationRail
import me.weishu.kernelsu.ui.component.dialog.rememberConfirmDialog
import me.weishu.kernelsu.ui.component.custom.LocalCustomSwitchStyle
import me.weishu.kernelsu.ui.component.liquid.LocalLiquidGlassBackdrop
import me.weishu.kernelsu.ui.component.liquid.liquidGlassBackdropColor
import me.weishu.kernelsu.ui.theme.SidebarUiTokens
import me.weishu.kernelsu.ui.component.pixel.LocalPixelStyle
import me.weishu.kernelsu.ui.component.pixel.LocalPixelCardMotionEnabled
import me.weishu.kernelsu.ui.component.pixel.LocalPixelCardMotionProgress
import me.weishu.kernelsu.ui.component.pixel.PixelBackdrop
import me.weishu.kernelsu.ui.component.pixel.PixelChromeOverlay
import me.weishu.kernelsu.ui.component.pixel.PixelStyle
import me.weishu.kernelsu.ui.component.pixel.rememberPixelCardMotionProgress
import me.weishu.kernelsu.ui.component.rain.LocalRainStyle
import me.weishu.kernelsu.ui.component.rain.LocalRainCardMotionEnabled
import me.weishu.kernelsu.ui.component.rain.RainStyle
import me.weishu.kernelsu.ui.component.snow.LocalSeasonStyle
import me.weishu.kernelsu.ui.component.snow.LocalSeasonCardMotionEnabled
import me.weishu.kernelsu.ui.component.snow.LocalSeasonCardMotionProgress
import me.weishu.kernelsu.ui.component.snow.SeasonAmbientOverlay
import me.weishu.kernelsu.ui.component.snow.SeasonChromeOverlay
import me.weishu.kernelsu.ui.component.snow.SeasonStyle
import me.weishu.kernelsu.ui.component.snow.SeasonStyleWallpaper
import me.weishu.kernelsu.ui.component.snow.rememberSeasonCardMotionProgress
import me.weishu.kernelsu.ui.component.calculateContinuousPagerPagePosition
import me.weishu.kernelsu.ui.component.globalScrollEffectController
import me.weishu.kernelsu.ui.component.rememberBackgroundScrollFollowState
import me.weishu.kernelsu.ui.component.rememberGlobalScrollEffectState
import me.weishu.kernelsu.ui.navigation3.HandleDeepLink
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Navigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.navigation3.rememberNavigator
import me.weishu.kernelsu.ui.screen.about.AboutScreen
import me.weishu.kernelsu.ui.screen.appprofile.AppProfileScreen
import me.weishu.kernelsu.ui.screen.colorpalette.ColorPaletteScreen
import me.weishu.kernelsu.ui.screen.executemoduleaction.ExecuteModuleActionScreen
import me.weishu.kernelsu.ui.screen.flash.FlashIt
import me.weishu.kernelsu.ui.screen.flash.FlashScreen
import me.weishu.kernelsu.ui.screen.home.HomePager
import me.weishu.kernelsu.ui.screen.home.HomeMetricCardWallpaperTarget
import me.weishu.kernelsu.ui.screen.home.hasHomeMetricCardWallpaperImage
import me.weishu.kernelsu.ui.screen.home.preloadHomeMetricCardWallpaperImages
import me.weishu.kernelsu.ui.screen.install.InstallScreen
import me.weishu.kernelsu.ui.screen.launchericon.LauncherIconScreen
import me.weishu.kernelsu.ui.screen.module.ModulePager
import me.weishu.kernelsu.ui.screen.module.ModuleToolsScreen
import me.weishu.kernelsu.ui.screen.module.ModuleWallpaperBackupScreen
import me.weishu.kernelsu.ui.screen.module.ModuleWallpaperEditorScreen
import me.weishu.kernelsu.ui.screen.modulerepo.ModuleRepoDetailScreen
import me.weishu.kernelsu.ui.screen.modulerepo.ModuleRepoScreen
import me.weishu.kernelsu.ui.screen.navigationicon.NavigationIconScreen
import me.weishu.kernelsu.ui.screen.settings.BackgroundSettingsScreen
import me.weishu.kernelsu.ui.screen.settings.AiChatScreen
import me.weishu.kernelsu.ui.screen.settings.AiModuleStudioScreen
import me.weishu.kernelsu.ui.screen.settings.CpuSpoofScreen
import me.weishu.kernelsu.ui.screen.settings.DeviceIdentityScreen
import me.weishu.kernelsu.ui.screen.settings.DynamicManagerScreen
import me.weishu.kernelsu.ui.screen.settings.GraphicsRendererScreen
import me.weishu.kernelsu.ui.screen.settings.ForegroundToolProtectionScreen
import me.weishu.kernelsu.ui.screen.settings.ImageToolScreen
import me.weishu.kernelsu.ui.screen.settings.KpmScreen
import me.weishu.kernelsu.ui.screen.settings.LkmSusfsApplicationsScreen
import me.weishu.kernelsu.ui.screen.settings.LkmSusfsScreen
import me.weishu.kernelsu.ui.screen.settings.SusfsPathConfigScreen
import me.weishu.kernelsu.ui.util.captureSusfsRecoverySnapshot
import me.weishu.kernelsu.ui.util.getSusfsPathConfig
import me.weishu.kernelsu.ui.util.hasSusfsRecoverySnapshot
import me.weishu.kernelsu.ui.util.restoreSusfsFromSnapshot
import me.weishu.kernelsu.ui.screen.settings.SusfsApplicationsScreen
import me.weishu.kernelsu.ui.screen.settings.SusfsGuideScreen
import me.weishu.kernelsu.ui.screen.settings.RescueProtectionScreen
import me.weishu.kernelsu.ui.screen.settings.HomeCardWallpaperScreen
import me.weishu.kernelsu.ui.screen.settings.InstallCardWallpaperScreen
import me.weishu.kernelsu.ui.screen.settings.LanguageSettingsScreen
import me.weishu.kernelsu.ui.screen.settings.StealthRecoveryCheckScreen
import me.weishu.kernelsu.ui.screen.settings.PreInstallStyleSettingsScreen
import me.weishu.kernelsu.ui.screen.scriptrun.ScriptRunScreen
import me.weishu.kernelsu.ui.screen.settings.SettingPager
import me.weishu.kernelsu.ui.screen.settings.SettingsCategoryScreen
import me.weishu.kernelsu.ui.screen.settings.SoundEffectsScreen
import me.weishu.kernelsu.ui.screen.settings.SidebarWidgetSettingsScreen
import me.weishu.kernelsu.ui.screen.settings.StartupAnimationScreen
import me.weishu.kernelsu.ui.screen.settings.VisualEffectsScreen
import me.weishu.kernelsu.ui.screen.settings.SwitchStyleCreatorScreen
import me.weishu.kernelsu.ui.screen.sulog.SulogScreen
import me.weishu.kernelsu.ui.screen.superuser.AppIdManagerScreen
import me.weishu.kernelsu.ui.screen.superuser.AppFreezeScreen
import me.weishu.kernelsu.ui.screen.superuser.SuperUserToolsScreen
import me.weishu.kernelsu.ui.screen.superuser.SuperUserPager
import me.weishu.kernelsu.ui.screen.template.AppProfileTemplateScreen
import me.weishu.kernelsu.ui.screen.templateeditor.TemplateEditorScreen
import me.weishu.kernelsu.ui.screen.themestore.ThemeStorePage
import me.weishu.kernelsu.ui.screen.themestore.ThemeStoreCustomizeSection
import me.weishu.kernelsu.ui.screen.themestore.CloudThemeCreatorScreen
import me.weishu.kernelsu.ui.screen.themestore.CloudThemeCreatorGuideScreen
import me.weishu.kernelsu.ui.screen.themestore.CloudThemeDetailScreen
import me.weishu.kernelsu.ui.screen.themestore.ThemeCommentsScreen
import me.weishu.kernelsu.ui.screen.themestore.CloudThemeRankingScreen
import me.weishu.kernelsu.ui.screen.themestore.ThemeStoreLibraryScreen
import me.weishu.kernelsu.ui.screen.themestore.ThemeStoreMyScreen
import me.weishu.kernelsu.ui.screen.themestore.AppFontScreen
import me.weishu.kernelsu.ui.screen.themestore.ThemeStoreScreen
import me.weishu.kernelsu.ui.screen.pluginstore.PluginStoreScreen
import me.weishu.kernelsu.ui.screen.home.hasBlockingRootVersionMismatch
import me.weishu.kernelsu.ui.theme.KernelSUTheme
import me.weishu.kernelsu.ui.theme.resolveEffectiveDarkMode
import me.weishu.kernelsu.ui.theme.LocalAutoHideNavigationBar
import me.weishu.kernelsu.ui.theme.LocalBlurIntensity
import me.weishu.kernelsu.ui.theme.LocalModuleDescriptionMaxLines
import me.weishu.kernelsu.ui.theme.LocalColorMode
import me.weishu.kernelsu.ui.theme.LocalDeltaColorVariant
import me.weishu.kernelsu.ui.theme.LocalEnableBlur
import me.weishu.kernelsu.ui.theme.LocalEnableFloatingBottomBar
import me.weishu.kernelsu.ui.theme.LocalEnableFloatingBottomBarBlur
import me.weishu.kernelsu.ui.theme.LocalImmersiveBackgroundActive
import me.weishu.kernelsu.ui.theme.LocalScrollHideNavigationBar
import me.weishu.kernelsu.ui.theme.LocalModuleTopBarAutoHide
import me.weishu.kernelsu.ui.util.BackgroundMusicPlayer
import me.weishu.kernelsu.ui.util.ClickSoundPlayer
import me.weishu.kernelsu.ui.util.readAppAudioSettings
import me.weishu.kernelsu.ui.util.isAudioPlaybackAllowed
import me.weishu.kernelsu.ui.util.KernelStatusEvents
import me.weishu.kernelsu.ui.util.LocalCustomNavigationIcons
import me.weishu.kernelsu.ui.util.LocalInterfaceStyleTheme
import me.weishu.kernelsu.ui.util.LocalScrollAnimation
import me.weishu.kernelsu.ui.util.LocalScrollAnimationEffect
import me.weishu.kernelsu.ui.util.InterfaceStyleRegistry
import me.weishu.kernelsu.ui.util.interfaceStyleTheme
import me.weishu.kernelsu.ui.util.ManagerUpdateChecker
import me.weishu.kernelsu.ui.util.ManagerUpdateInfo
import me.weishu.kernelsu.ui.util.ManagerPlugin
import me.weishu.kernelsu.ui.util.ManagerPluginRegistry
import me.weishu.kernelsu.ui.util.checkManagerPluginCompatibility
import me.weishu.kernelsu.ui.util.ensureManagerRegistered
import me.weishu.kernelsu.ui.util.getFileName
import me.weishu.kernelsu.ui.util.getInstalledKsudStatus
import me.weishu.kernelsu.ui.util.getSuperuserCount
import me.weishu.kernelsu.ui.util.KpmCaps
import me.weishu.kernelsu.ui.util.getKpmCaps
import me.weishu.kernelsu.ui.util.KPatchNextStatus
import me.weishu.kernelsu.ui.util.install
import me.weishu.kernelsu.ui.util.ksuRootAvailable
import me.weishu.kernelsu.ui.util.rememberBlurBackdrop
import me.weishu.kernelsu.ui.util.rememberContentReady
import me.weishu.kernelsu.ui.util.rootAvailable
import me.weishu.kernelsu.ui.util.StartupSoundPlayer
import me.weishu.kernelsu.ui.viewmodel.MainActivityUiState
import me.weishu.kernelsu.ui.viewmodel.MainActivityViewModel
import me.weishu.kernelsu.ui.viewmodel.ModuleViewModel
import me.weishu.kernelsu.ui.webui.WebUIActivity
import me.weishu.kernelsu.ui.util.CustomBackgroundState
import me.weishu.kernelsu.ui.util.CustomPageBackgroundTarget
import me.weishu.kernelsu.ui.util.AppLanguageManager
import me.weishu.kernelsu.stealth.StealthModeStore
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MainActivity : ComponentActivity() {
    private var launcherVerificationJob: Job? = null

    private val intentState = MutableStateFlow(0)
    private val managerReadyState = MutableStateFlow(false)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrapContext(newBase))
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        val startupSplash = installSplashScreen()
        super.onCreate(savedInstanceState)

        // The splash post-theme is NoActionBar, but some OEM/theme combinations
        // can restore a platform action bar when the activity is recreated. The
        // Compose home screens already own their title bar, so keep the platform
        // bar hidden to avoid rendering a duplicate app title.
        actionBar?.hide()

        // Keep immersive pages, including transparent Material settings, drawn behind side cutouts.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }

        val lkmWallpaperTargets = listOf(
            HomeMetricCardWallpaperTarget.Lkm,
            HomeMetricCardWallpaperTarget.ClassicMiuixLkm,
            HomeMetricCardWallpaperTarget.MaterialLkm,
        ).filter { target ->
            hasHomeMetricCardWallpaperImage(
                context = applicationContext,
                target = target,
            )
        }
        var startupStateResolved = false
        var waitingForLkmWallpaper = lkmWallpaperTargets.isNotEmpty()
        startupSplash.setKeepOnScreenCondition {
            !startupStateResolved || waitingForLkmWallpaper
        }
        if (lkmWallpaperTargets.isNotEmpty()) {
            lifecycleScope.launch {
                lkmWallpaperTargets.map { target ->
                    async {
                        preloadHomeMetricCardWallpaperImages(
                            context = applicationContext,
                            target = target,
                        )
                    }
                }.awaitAll()
                waitingForLkmWallpaper = false
            }
            lifecycleScope.launch {
                delay(LKM_WALLPAPER_SPLASH_TIMEOUT_MS)
                waitingForLkmWallpaper = false
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val managerReady = runCatching {
                Natives.refreshInfo()
                Natives.isManager || ensureManagerRegistered()
            }.onFailure {
                Log.e(TAG, "refresh manager identity failed", it)
            }.getOrDefault(false)
            managerReadyState.value = managerReady
            if (managerReady) {
                KernelStatusEvents.requestRefresh()
            }
            val kernelCompatible = runCatching { !Natives.requireNewKernel() }.getOrDefault(false)
            if (managerReady && kernelCompatible) {
                runCatching { check(install()) { "ksud install command failed" } }
                    .onSuccess { KernelStatusEvents.requestRefresh() }
                    .onFailure { Log.e(TAG, "install ksud failed", it) }
                if (!Natives.isLateLoadMode && !Natives.isLkmMode) {
                    runCatching {
                        val current = getSusfsPathConfig()
                        if (current.runtimeStatus.generation.isBlank() && hasSusfsRecoverySnapshot()) {
                            val restored = restoreSusfsFromSnapshot()
                            if (!restored.success) Log.w(TAG, "SUSFS recovery failed: ${restored.error}")
                        } else if (current.available) {
                            captureSusfsRecoverySnapshot()
                        }
                    }
                        .onFailure { Log.w(TAG, "SUSFS recovery snapshot failed", it) }
                }
            }
        }

        setContent {
            val viewModel = viewModel<MainActivityViewModel>()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val selectedMainDestination by viewModel.selectedMainDestination.collectAsStateWithLifecycle()
            val managerReady by managerReadyState.collectAsStateWithLifecycle()
            val interfaceStyleGeneration by InterfaceStyleRegistry.changes.collectAsStateWithLifecycle()
            SideEffect {
                startupStateResolved = uiState.stealthModeResolved
            }
            val appSettings = uiState.appSettings
            val rainSettings = me.weishu.kernelsu.ui.component.rain.rememberRainSettings(
                active = uiState.interfaceStyle == InterfaceStyle.Rain.value,
            )
            val layoutInterfaceStyle = me.weishu.kernelsu.ui.component.rain.resolveRainLayoutStyle(
                uiState.interfaceStyle, rainSettings,
            )
            val uiMode = UiMode.fromValue(layoutInterfaceStyle)
            val startupAnimationUri = if (uiState.stealthModeEnabled) null else uiState.customStartupAnimationUri
            val clickSoundUri = if (uiState.stealthModeEnabled) null else uiState.customClickSoundUri
            val clickSoundVolume = uiState.customClickSoundVolume
            val backgroundMusicUri = if (uiState.stealthModeEnabled) null else uiState.customBackgroundMusicUri
            val backgroundMusicVolume = uiState.customBackgroundMusicVolume
            val appAudioSettings = uiState.appAudioSettings
            var showStartupAnimation by rememberSaveable { mutableStateOf(!startupAnimationUri.isNullOrBlank()) }
            LaunchedEffect(uiState.stealthModeEnabled) {
                if (uiState.stealthModeEnabled) showStartupAnimation = false
            }
            val effectiveEnableBlur = resolveRealtimeBlurEnabled(
                interfaceStyle = uiState.interfaceStyle,
                requested = uiState.enableBlur,
            )
            val effectiveEnableFloatingBottomBarBlur = resolveRealtimeBlurEnabled(
                interfaceStyle = uiState.interfaceStyle,
                requested = uiState.enableFloatingBottomBarBlur,
            )
            val rainInterfaceActive = uiState.interfaceStyle == InterfaceStyle.Rain.value
            val rainWeather = me.weishu.kernelsu.ui.component.rain.rememberRainWeather(rainSettings)
            // Atmosphere only applies while rain is the active style; otherwise the stored scene stands.
            val rainTimeOfDay = me.weishu.kernelsu.ui.component.rain.rememberRainTimeOfDay(
                rainInterfaceActive && rainSettings.atmosphere,
            )
            val selectedRainStyle = if (!rainInterfaceActive || !rainSettings.atmosphere) rainSettings.scene else {
                me.weishu.kernelsu.ui.component.rain.resolveRainScene(rainSettings, rainTimeOfDay, rainWeather.condition)
            }
            val selectedPixelStyle = PixelStyle.fromValue(uiState.pixelStyle)
            val selectedExternalTheme = remember(
                uiState.interfaceStyle,
                uiState.activeInterfaceStyleId,
                uiState.rainStyle,
                uiState.pixelStyle,
                interfaceStyleGeneration,
            ) {
                val variant = when (uiState.interfaceStyle) {
                    InterfaceStyle.Rain.value -> uiState.rainStyle
                    InterfaceStyle.Pixel.value -> uiState.pixelStyle
                    else -> null
                }
                if (uiState.interfaceStyle == InterfaceStyle.Rain.value) null else interfaceStyleTheme(
                    applicationContext,
                    if (uiState.interfaceStyle == InterfaceStyle.Delta.value) InterfaceStyle.Alpha.value
                    else uiState.interfaceStyle,
                    variant,
                    uiState.activeInterfaceStyleId,
                )
            }
            val seasonInterfaceActive = uiState.interfaceStyle == InterfaceStyle.Snow.value
            val pixelInterfaceActive = uiState.interfaceStyle == InterfaceStyle.Pixel.value
            val seasonCardMotionProgress = rememberSeasonCardMotionProgress(
                enabled = seasonInterfaceActive && uiState.seasonCardMotionEnabled,
            )
            val rainCardMotionEnabled = rainInterfaceActive && rainSettings.decorationMotionEnabled()
            val pixelCardMotionProgress = rememberPixelCardMotionProgress(
                enabled = pixelInterfaceActive && uiState.pixelCardMotionEnabled,
                theme = selectedExternalTheme,
            )
            val darkMode = resolveEffectiveDarkMode(
                colorMode = appSettings.colorMode,
                systemDark = isSystemInDarkTheme(),
                interfaceStyle = uiState.interfaceStyle,
                rainStyle = selectedRainStyle,
                pixelStyle = selectedPixelStyle,
                interfaceTheme = selectedExternalTheme,
            )
            val selectedNightBackgroundEffect =
                NightBackgroundEffect.fromValue(uiState.nightBackgroundEffect)

            DisposableEffect(darkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkMode },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkMode },
                )
                window.isNavigationBarContrastEnforced = false
                onDispose { }
            }

            LaunchedEffect(clickSoundUri) {
                if (clickSoundUri.isNullOrBlank()) {
                    ClickSoundPlayer.release()
                }
            }

            LaunchedEffect(backgroundMusicUri, backgroundMusicVolume, appAudioSettings) {
                if (backgroundMusicUri.isNullOrBlank() ||
                    !appAudioSettings.masterEnabled ||
                    !appAudioSettings.background.enabled
                ) {
                    BackgroundMusicPlayer.stop()
                } else {
                    BackgroundMusicPlayer.play(this@MainActivity, backgroundMusicUri, backgroundMusicVolume)
                }
            }

            val navigator = rememberNavigator(Route.Main)
            val stealthNavigator = remember { Navigator(Route.Main) }
            val swipeDismiss = if (uiState.enableSwipeDismiss) {
                if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
                    NavSwipeDirection.RightToLeft
                } else {
                    NavSwipeDirection.LeftToRight
                }
            } else {
                NavSwipeDirection.None
            }
            val currentRoute = if (uiState.stealthModeEnabled) {
                Route.Main
            } else {
                navigator.current() as? Route
            }
            val rainSoundVolume = me.weishu.kernelsu.ui.component.rain.rainSoundVolume(
                rainSettings.sound, rainSettings.soundVolume, selectedRainStyle,
            )
            val systemDensity = LocalDensity.current
            val density = remember(systemDensity, uiState.pageScale, uiState.fontScale) {
                Density(
                    density = systemDensity.density * uiState.pageScale,
                    fontScale = systemDensity.fontScale * uiState.fontScale,
                )
            }
            CompositionLocalProvider(
                LocalNavigator provides navigator,
                LocalCustomSwitchStyle provides uiState.customSwitchStyle,
                LocalDensity provides density,
                LocalColorMode provides appSettings.colorMode.value,
                LocalEnableBlur provides effectiveEnableBlur,
                LocalBlurIntensity provides uiState.blurIntensity,
                LocalEnableFloatingBottomBar provides uiState.enableFloatingBottomBar,
                LocalEnableFloatingBottomBarBlur provides effectiveEnableFloatingBottomBarBlur,
                LocalAutoHideNavigationBar provides uiState.autoHideNavigationBar,
                LocalScrollHideNavigationBar provides uiState.scrollHideNavigationBar,
                LocalModuleTopBarAutoHide provides uiState.moduleTopBarAutoHideEnabled,
                LocalModuleDescriptionMaxLines provides uiState.moduleDescriptionMaxLines,
                LocalUiMode provides uiMode,
                LocalInterfaceStyle provides layoutInterfaceStyle,
                me.weishu.kernelsu.ui.component.rain.LocalRainSettings provides
                    rainSettings.copy(scene = selectedRainStyle),
                LocalInterfaceStyleTheme provides selectedExternalTheme,
                LocalSeasonStyle provides SeasonStyle.fromValue(uiState.seasonStyle),
                LocalSeasonCardMotionEnabled provides (
                    seasonInterfaceActive && uiState.seasonCardMotionEnabled
                    ),
                LocalSeasonCardMotionProgress provides seasonCardMotionProgress,
                LocalRainStyle provides selectedRainStyle,
                LocalRainCardMotionEnabled provides rainCardMotionEnabled,
                LocalPixelStyle provides selectedPixelStyle,
                LocalPixelCardMotionEnabled provides (
                    pixelInterfaceActive && uiState.pixelCardMotionEnabled
                    ),
                LocalPixelCardMotionProgress provides pixelCardMotionProgress,
                LocalDeltaColorVariant provides uiState.deltaColorVariant,
                LocalCustomNavigationIcons provides uiState.customNavigationIcons,
                LocalSwitchStyle provides SwitchStyle.fromValue(uiState.switchStyle),
                LocalNightBackgroundEffectActive provides (
                        darkMode &&
                        !uiState.nightBackgroundPassthrough &&
                        selectedNightBackgroundEffect != NightBackgroundEffect.Off
                    ),
                LocalScrollAnimation provides uiState.globalScrollEffectEnabled,
                LocalScrollAnimationEffect provides GlobalScrollEffect.fromValue(uiState.globalScrollEffect),
                LocalPageTransitionEffect provides PageTransitionEffect.fromValue(uiState.pageTransitionEffect),
            ) {
                KernelSUTheme(
                    appSettings = appSettings,
                    appFontState = uiState.appFont,
                    uiMode = uiMode,
                ) {
                    if (!uiState.stealthModeResolved) {
                        Box(modifier = Modifier.fillMaxSize())
                        return@KernelSUTheme
                    }
                    if (!uiState.stealthModeEnabled) {
                        HandleDeepLink(intentState = intentState.collectAsStateWithLifecycle())
                        ManagerUpdatePrompt()
                        ZipFileIntentHandler(intentState = intentState, isManager = managerReady)
                        ShortcutIntentHandler(intentState = intentState)
                    }
                    val mainPagerPageCount = remember {
                        mutableIntStateOf(
                            mainDestinations(
                                kpmActive = false,
                                stealthModeEnabled = uiState.stealthModeEnabled,
                            ).size
                        )
                    }
                    val mainPagerState = rememberMainPagerState(
                        pagerState = rememberPagerState(
                            initialPage = mainDestinations(
                                kpmActive = false,
                                stealthModeEnabled = uiState.stealthModeEnabled,
                            )
                                .indexOf(selectedMainDestination)
                                .coerceAtLeast(0),
                            pageCount = { mainPagerPageCount.intValue },
                        ),
                        pageCountState = mainPagerPageCount,
                        restoredDestination = selectedMainDestination,
                        initialStealthModeEnabled = uiState.stealthModeEnabled,
                    )
                    val flashRoute = navigator.current() as? Route.Flash
                    var flashBackEnabled by remember(flashRoute) { mutableStateOf(false) }
                    var sidebarNavigationBadge by remember { mutableStateOf(NavigationBadgeState()) }
                    val scriptRunRoute = navigator.current() as? Route.ScriptRun
                    var scriptRunBackEnabled by remember(scriptRunRoute) { mutableStateOf(true) }
                    // Read here, not inside the NavDisplay builder: NavDisplay only rebuilds its entries
                    // (and their swipeDismiss) when the builder lambda changes, so it must capture the value.
                    val scriptRunSwipeDismiss = if (scriptRunBackEnabled) swipeDismiss else NavSwipeDirection.None
                    val mainScreenEntry = @Composable {
                        MainScreen(
                            onDestinationChanged = viewModel::setSelectedMainDestination,
                            onNavigationBadgeChanged = { sidebarNavigationBadge = it },
                            mainPagerState = mainPagerState,
                            stealthModeEnabled = uiState.stealthModeEnabled,
                        )
                    }

                    LaunchedEffect(uiState.stealthModeEnabled) {
                        if (uiState.stealthModeEnabled) {
                            viewModel.setSelectedMainDestination(MainDestination.Home)
                            navigator.replaceAll(listOf(Route.Main))
                        }
                    }

                    val kpmPageActiveForNavigation = mainPagerState.kpmActive || currentRoute == Route.Kpm
                    val sidebarStackTransitions = shouldUseSidebarStackNavigationTransitions(
                        kpmPageActive = kpmPageActiveForNavigation,
                        sidebarStyleActive = layoutInterfaceStyle == InterfaceStyle.SidebarWidget.value,
                        animationsEnabled = rememberSystemAnimationsEnabled(),
                        stealthModeEnabled = uiState.stealthModeEnabled,
                    )
                    val stackDirection = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
                    val navDisplay = @Composable {
                        NavDisplay(
                            modifier = Modifier.fillMaxSize().clipToBounds(),
                            backStack = if (uiState.stealthModeEnabled) {
                                stealthNavigator.backStack
                            } else {
                                navigator.backStack
                            },
                            entryDecorators = listOf(
                                rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator()
                            ),
                            onBack = {
                                if (flashRoute == null || flashBackEnabled) {
                                    when (val top = navigator.current()) {
                                        is Route.TemplateEditor -> {
                                            if (!top.readOnly) {
                                                navigator.setResult("template_edit", true)
                                            } else {
                                                navigator.pop()
                                            }
                                        }

                                        else -> navigator.pop()
                                    }
                                }
                            },
                            entryProvider = entryProvider {
                                entry<Route.Main> { mainScreenEntry() }
                                entry<Route.SettingsCategory> { key -> SettingsCategoryScreen(key.category) }
                                entry<Route.InterfaceStyles> {
                                    me.weishu.kernelsu.ui.screen.settings.InterfaceStylesScreen()
                                }
                                entry<Route.About> { AboutScreen() }
                                entry<Route.Sulog> { SulogScreen() }
                                entry<Route.SuperUserTools> { SuperUserToolsScreen() }
                                entry<Route.AppIdManager> { PluginRouteGate(ManagerPlugin.AppIdManager) { AppIdManagerScreen() } }
                                entry<Route.DynamicManager> { DynamicManagerScreen() }
                                entry<Route.AppFreeze> { PluginRouteGate(ManagerPlugin.AppFreeze) { AppFreezeScreen() } }
                                entry<Route.ColorPalette> { ColorPaletteScreen() }
                                entry<Route.LauncherIcon> { LauncherIconScreen() }
                                entry<Route.NavigationIcons> { NavigationIconScreen() }
                                entry<Route.SidebarWidgetSettings> {
                                    SidebarWidgetSettingsScreen()
                                }
                                entry<Route.RainDesign> {
                                    me.weishu.kernelsu.ui.component.rain.RainDesignScreen()
                                }
                                entry<Route.Backgrounds> { BackgroundSettingsScreen() }
                                entry<Route.SoundEffects> { SoundEffectsScreen() }
                                entry<Route.StartupAnimation> { StartupAnimationScreen() }
                                entry<Route.HomeCardWallpapers> { HomeCardWallpaperScreen() }
                                entry<Route.InstallCardWallpapers> { InstallCardWallpaperScreen() }
                                entry<Route.LanguageSettings> { LanguageSettingsScreen() }
                                entry<Route.StealthRecoveryCheck> { StealthRecoveryCheckScreen() }
                                entry<Route.PreInstallStyleSettings> { PreInstallStyleSettingsScreen() }
                                entry<Route.VisualEffects> { VisualEffectsScreen() }
                                entry<Route.SwitchStyleCreator> { SwitchStyleCreatorScreen() }
                                entry<Route.SusfsPathConfig> { SusfsPathConfigScreen() }
                                entry<Route.LkmSusfs> { LkmSusfsScreen() }
                                entry<Route.LkmSusfsApplications> { LkmSusfsApplicationsScreen() }
                                entry<Route.SusfsApplications> { SusfsApplicationsScreen() }
                                entry<Route.SusfsGuide> { SusfsGuideScreen() }
                                entry<Route.ForegroundToolProtection> { ForegroundToolProtectionScreen() }
                                entry<Route.AiChat> { PluginRouteGate(ManagerPlugin.AiChat) { AiChatScreen() } }
                                entry<Route.AiModuleStudio> { AiModuleStudioScreen() }
                                entry<Route.RescueProtection> { PluginRouteGate(ManagerPlugin.RescueProtection) { RescueProtectionScreen() } }
                                entry<Route.CpuSpoof> { PluginRouteGate(ManagerPlugin.CpuSpoof) { CpuSpoofScreen() } }
                                entry<Route.DeviceIdentity> { PluginRouteGate(ManagerPlugin.DeviceIdentity) { DeviceIdentityScreen() } }
                                entry<Route.GraphicsRenderer> { PluginRouteGate(ManagerPlugin.GraphicsRenderer) { GraphicsRendererScreen() } }
                                entry<Route.Kpm> { KpmScreen() }
                                entry<Route.ImageTool> { PluginRouteGate(ManagerPlugin.ImageTools) { ImageToolScreen() } }
                                entry<Route.ThemeStore> { ThemeStoreScreen() }
                                entry<Route.ThemeStoreCustomize> {
                                    ThemeStoreScreen(page = ThemeStorePage.Customize)
                                }
                                entry<Route.PluginStore> { PluginStoreScreen() }
                                entry<Route.InterfaceStyleStore> {
                                    me.weishu.kernelsu.ui.screen.settings.InterfaceStylesScreen(initialStore = true)
                                }
                                entry<Route.StoreInterfaceStyles> {
                                    me.weishu.kernelsu.ui.screen.settings.InterfaceStylesScreen(initialStore = true)
                                }
                                entry<Route.ThemeStoreAssets> {
                                    ThemeStoreScreen(
                                        page = ThemeStorePage.Customize,
                                        customizeSection = ThemeStoreCustomizeSection.Assets,
                                    )
                                }
                                entry<Route.ThemeStoreFonts> { AppFontScreen() }
                                entry<Route.ThemeStoreBackgrounds> {
                                    ThemeStoreScreen(
                                        page = ThemeStorePage.Customize,
                                        customizeSection = ThemeStoreCustomizeSection.Atmosphere,
                                    )
                                }
                                entry<Route.ThemeStoreTransfer> { ThemeStoreScreen(ThemeStorePage.My) }
                                entry<Route.ThemeStoreMy> { ThemeStoreMyScreen() }
                                entry<Route.ThemeCommentAccount> { ThemeCommentsScreen("", "", false, accountMode = true) }
                                entry<Route.ThemeStoreLibrary> { ThemeStoreLibraryScreen() }
                                entry<Route.CloudThemeDetail> { key -> CloudThemeDetailScreen(key.themeId) }
                                entry<Route.ThemeComments> { key ->
                                    ThemeCommentsScreen(key.subjectId, key.subjectName, key.interfaceStyle)
                                }
                                entry<Route.CloudThemeRanking> { CloudThemeRankingScreen() }
                                entry<Route.CloudThemeCreator> { CloudThemeCreatorScreen() }
                                entry<Route.CloudThemeCreatorSubmission> {
                                    CloudThemeCreatorScreen(initialPageIndex = 1)
                                }
                                entry<Route.CloudThemeCreatorGuide> { CloudThemeCreatorGuideScreen() }
                                entry<Route.ModuleTools> { ModuleToolsScreen() }
                                entry<Route.ModuleWallpaperBackup> { ModuleWallpaperBackupScreen() }
                                entry<Route.ModuleWallpaperEditor> { key ->
                                    ModuleWallpaperEditorScreen(
                                        moduleId = key.moduleId,
                                        displayName = key.displayName,
                                        displayAuthor = key.displayAuthor,
                                        displayVersion = key.displayVersion,
                                        displayDescription = key.displayDescription,
                                        allowBatch = key.allowBatch,
                                    )
                                }
                                entry<Route.AppProfileTemplate> { AppProfileTemplateScreen() }
                                entry<Route.TemplateEditor> { key -> TemplateEditorScreen(key.template, key.readOnly) }
                                entry<Route.AppProfile> { key -> AppProfileScreen(key.uid) }
                                entry<Route.ModuleRepo> { ModuleRepoScreen() }
                                entry<Route.ModuleRepoDetail> { key -> ModuleRepoDetailScreen(key.module) }
                                entry<Route.Install> { InstallScreen() }
                                entry<Route.Flash> { key ->
                                    FlashScreen(
                                        key.flashIt,
                                        onBackEnabledChange = { enabled ->
                                            if (navigator.current() == key) flashBackEnabled = enabled
                                        },
                                    )
                                }
                                entry<Route.ExecuteModuleAction> { key -> ExecuteModuleActionScreen(key.moduleId, key.fromShortcut) }
                                entry<Route.Home> { mainScreenEntry() }
                                entry<Route.SuperUser> { mainScreenEntry() }
                                entry<Route.Module> { mainScreenEntry() }
                                entry<Route.Settings> { mainScreenEntry() }
                            },
                            transitionSpec = when {
                                sidebarStackTransitions -> sidebarStackForwardTransition(stackDirection)
                                shouldUseLayeredNavigationTransitions(
                                    kpmPageActiveForNavigation,
                                    layoutInterfaceStyle == InterfaceStyle.SidebarWidget.value,
                                ) -> stableNavForwardTransition()
                                else -> instantNavTransition()
                            },
                            popTransitionSpec = when {
                                sidebarStackTransitions -> sidebarStackPopTransition(stackDirection)
                                shouldUseLayeredNavigationTransitions(
                                    kpmPageActiveForNavigation,
                                    layoutInterfaceStyle == InterfaceStyle.SidebarWidget.value,
                                ) -> stableNavPopTransition()
                                else -> instantNavTransition()
                            },
                            transitionEffects = NavDisplayTransitionEffects(
                                enableCornerClip = false,
                                dimAmount = 0f,
                                blockInputDuringTransition = true,
                                popDirectionFollowsSwipeEdge = false,
                            ),
                        )
                    }
                    val globalGlassBackdrop = rememberBlurBackdrop(effectiveEnableBlur)
                    // KPM owns an Android WebView. During a route transition the old
                    // MainScreen can remain composed briefly, so keep it out of the
                    // global backdrop even after SettingsCategory becomes current.
                    val kpmWebViewSurfaceActive = currentRoute == Route.Kpm ||
                        (mainPagerState.kpmActive && !uiState.stealthModeEnabled)
                    var routeInitialized by remember { mutableStateOf(false) }
                    var navigationTransitionActive by remember { mutableStateOf(false) }
                    LaunchedEffect(currentRoute) {
                        if (!routeInitialized) {
                            routeInitialized = true
                        } else {
                            navigationTransitionActive = true
                            delay(
                                (if (sidebarStackTransitions) SIDEBAR_STACK_DURATION_MS else NAV_TRANSITION_DURATION_MS).toLong()
                            )
                            navigationTransitionActive = false
                        }
                    }
                    val pageBackgroundTarget = customPageBackgroundTarget(
                        currentRoute,
                        storeAncestor = navigator.backStack.any { (it as? Route).isStoreRoute() },
                    )
                    val effectiveBackground = uiState.effectiveCustomBackground(
                        if (uiState.stealthModeEnabled) MainDestination.Home else mainPagerState.destinationForPage(),
                        pageBackgroundTarget,
                    )
                    val passthroughTarget = pageBackgroundTarget ?: when (
                        if (uiState.stealthModeEnabled) MainDestination.Home else mainPagerState.destinationForPage()
                    ) {
                        MainDestination.Home -> CustomPageBackgroundTarget.Home
                        MainDestination.SuperUser -> CustomPageBackgroundTarget.Superuser
                        MainDestination.Module -> CustomPageBackgroundTarget.Module
                        MainDestination.Settings -> CustomPageBackgroundTarget.Settings
                        MainDestination.Kpm -> CustomPageBackgroundTarget.Kpm
                    }
                    val uiPassthrough = me.weishu.kernelsu.ui.util.rememberUiPassthroughScope(passthroughTarget).settings
                    val pagerBackgrounds = if (
                        uiState.backgroundScrollFollowEnabled && currentRoute.hostsMainPager()
                    ) {
                        uiState.mainPagerBackgrounds(
                            mainPagerState.kpmActive,
                            uiState.stealthModeEnabled,
                        )
                    } else {
                        emptyList()
                    }
                    LaunchedEffect(uiState.customPageBackgrounds) {
                        preloadCustomPageBackgroundImages(
                            context = applicationContext,
                            backgrounds = uiState.customPageBackgrounds,
                        )
                    }
                    val seasonalStyleActive = seasonInterfaceActive
                    val rainStyleActive = rainInterfaceActive
                    val pixelStyleActive = uiState.interfaceStyle == InterfaceStyle.Pixel.value
                    val sidebarStyleActive = layoutInterfaceStyle == InterfaceStyle.SidebarWidget.value
                    val sidebarConfig = if (sidebarStyleActive) rememberSidebarWidgetConfig() else null
                    val sidebarMaterial = if (rainSettings.active) SidebarMaterial.Flat else sidebarConfig?.material ?: SidebarMaterial.Flat
                    val sidebarGlassBackdrop = rememberSidebarGlassBackdrop(
                        enabled = canCaptureSidebarWallpaper(
                            sidebarStyleActive, sidebarMaterial,
                            effectiveBackground.videoUriString, pagerBackgrounds.any { it.hasVideo },
                            homeCardsNeedGlass = sidebarConfig?.homeCards?.needsBackdrop(darkMode) == true,
                        ),
                    )
                    val sidebarWallpaperModifier = if (sidebarGlassBackdrop != null) {
                        Modifier.layerBackdrop(sidebarGlassBackdrop).sidebarGlassUnderlay(darkMode)
                    } else Modifier
                    val hasCustomBackground =
                        !effectiveBackground.wallpaperUriString.isNullOrBlank() ||
                            !effectiveBackground.videoUriString.isNullOrBlank()
                    // Use the same resolved main-page/route wallpaper as the renderer.
                    me.weishu.kernelsu.ui.component.rain.RainAmbience(
                        enabled = rainInterfaceActive && !uiState.stealthModeEnabled && rainSoundVolume > 0f &&
                            (!hasCustomBackground || rainSettings.soundWithWallpaper),
                        sound = rainSettings.sound, volume = rainSoundVolume,
                    )
                    val immersiveBackgroundActive =
                        seasonalStyleActive ||
                            rainStyleActive ||
                            pixelStyleActive ||
                            hasCustomBackground ||
                            (
                                darkMode &&
                                    selectedNightBackgroundEffect != NightBackgroundEffect.Off
                                )
                    val globalScrollEffectState = rememberGlobalScrollEffectState(
                        enabled = uiState.globalScrollEffectEnabled && !navigationTransitionActive,
                        effectValue = uiState.globalScrollEffect,
                    )
                    val backgroundScrollFollowState = rememberBackgroundScrollFollowState(
                        enabled = uiState.backgroundScrollFollowEnabled && !navigationTransitionActive,
                        resetKey = currentRoute,
                        horizontalPagerDriven = true,
                    )

                    me.weishu.kernelsu.ui.component.overflow.OverflowMenuBackdrop(
                        modifier = Modifier
                            .fillMaxSize()
                            .backgroundScrollFollowController(backgroundScrollFollowState)
                            .globalScrollEffectController(globalScrollEffectState)
                    ) {
                        CustomWallpaperRoot(
                            uriString = effectiveBackground.wallpaperUriString,
                            videoUriString = effectiveBackground.videoUriString,
                            videoDurationSeconds = effectiveBackground.videoDurationSeconds,
                            videoFrameRate = effectiveBackground.videoFrameRate,
                            opacity = effectiveBackground.opacity,
                            crop = effectiveBackground.crop,
                            visualSettings = effectiveBackground.visualSettings,
                            passthroughEnabled = uiState.customWallpaperPassthroughEnabled,
                            passthroughOpacity = uiState.customWallpaperPassthroughOpacity,
                            uiPassthroughSettings = uiPassthrough,
                            backgroundScrollFollowState = backgroundScrollFollowState,
                            pagerBackgrounds = pagerBackgrounds,
                            horizontalPagerPosition = {
                                if (uiState.stealthModeEnabled) 0f else
                                    calculateContinuousPagerPagePosition(
                                        currentPage = mainPagerState.pagerState.currentPage,
                                        currentPageOffsetFraction =
                                            mainPagerState.pagerState.currentPageOffsetFraction,
                                    )
                            },
                            backgroundLayerModifier = sidebarWallpaperModifier,
                        ) {
                            CompositionLocalProvider(
                                LocalImmersiveBackgroundActive provides immersiveBackgroundActive,
                                LocalBackgroundScrollFollowState provides backgroundScrollFollowState,
                                LocalSidebarGlassBackdrop provides sidebarGlassBackdrop,
                                LocalSidebarGlassBackground provides effectiveBackground,
                                me.weishu.kernelsu.ui.component.rain.LocalRainCoveredByWallpaper provides
                                    hasCustomBackground,
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (seasonalStyleActive && !hasCustomBackground) {
                                        SeasonStyleWallpaper(modifier = Modifier.fillMaxSize())
                                        SeasonAmbientOverlay(modifier = Modifier.fillMaxSize())
                                    }
                                    if (pixelStyleActive && !hasCustomBackground) {
                                        PixelBackdrop(modifier = Modifier.fillMaxSize())
                                    }
                                    if (!uiState.nightBackgroundPassthrough) {
                                        NightBackgroundEffectOverlay(
                                            enabled = darkMode,
                                            effectValue = selectedNightBackgroundEffect.value,
                                            passthrough = false,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                    if (immersiveBackgroundActive) {
                                        StatusBarContrastScrim(darkMode = darkMode)
                                    }
                                    CompositionLocalProvider(LocalLiquidGlassBackdrop provides globalGlassBackdrop) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .windowInsetsPadding(WindowInsets.captionBar)
                                                .imePadding()
                                                .customClickSound(clickSoundUri, clickSoundVolume)
                                                .then(
                                                    if (globalGlassBackdrop != null && !kpmWebViewSurfaceActive) {
                                                        Modifier.layerBackdrop(globalGlassBackdrop)
                                                    } else {
                                                        Modifier
                                                    }
                                                )
                                        ) {
                                            val settingsSidebarActive = sidebarStyleActive &&
                                                mainPagerState.fullFeatured &&
                                                shouldShowSettingsSidebar(
                                                    currentRoute,
                                                    navigator.backStack.any {
                                                        it is Route.SettingsCategory || it == Route.InterfaceStyles || it == Route.SidebarWidgetSettings || it == Route.RainDesign
                                                    },
                                                    navigator.backStack.any { (it as? Route).isStoreRoute() },
                                                )
                                            // Keep the rail outside NavDisplay across main, module repository,
                                            // and settings scenes so the animated content keeps one width.
                                            val hostedSidebarRail = me.weishu.kernelsu.ui.util.currentAppWindowSize().canShowSidebar && shouldHostSidebarRail(
                                                route = currentRoute,
                                                settingsSidebarActive = settingsSidebarActive,
                                                fullFeatured = mainPagerState.fullFeatured,
                                                stealthModeEnabled = uiState.stealthModeEnabled,
                                            )
                                            Scaffold(
                                                containerColor = Color.Transparent,
                                                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                                            ) {
                                                if (sidebarStyleActive && sidebarConfig != null) {
                                                    val railAtStart = sidebarConfig.side.isAtStart(LocalLayoutDirection.current)
                                                    val railInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
                                                        if (sidebarConfig.side == SidebarSide.Left) {
                                                            WindowInsetsSides.Left
                                                        } else {
                                                            WindowInsetsSides.Right
                                                        }
                                                    )
                                                    val rail: @Composable () -> Unit = {
                                                        SidebarWidgetRail(
                                                            navigationBadge = sidebarNavigationBadge,
                                                            destinations = mainDestinations(mainPagerState.kpmActive),
                                                            selectedDestination = when {
                                                                currentRoute.hostsMainPager() -> mainPagerState.destinationForPage()
                                                                currentRoute == Route.Sulog -> MainDestination.SuperUser
                                                                currentRoute == Route.ModuleRepo || currentRoute is Route.ModuleRepoDetail -> MainDestination.Module
                                                                else -> MainDestination.Settings
                                                            },
                                                            onDestinationSelected = { destination ->
                                                                mainPagerState.animateTo(destination)
                                                                navigator.popUntil { (it as? Route)?.hostsMainPager() == true }
                                                            },
                                                        )
                                                    }
                                                    CompositionLocalProvider(LocalMainPagerState provides mainPagerState) {
                                                        Row(modifier = Modifier.fillMaxSize()) {
                                                            if (hostedSidebarRail && railAtStart) rail()
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .then(
                                                                        if (hostedSidebarRail) {
                                                                            Modifier.consumeWindowInsets(railInsets).clip(SidebarPaneShape)
                                                                        } else Modifier
                                                                    ),
                                                            ) {
                                                                CompositionLocalProvider(
                                                                    LocalSidebarRailHostedExternally provides hostedSidebarRail,
                                                                ) {
                                                                    key(uiState.stealthModeEnabled) { navDisplay() }
                                                                }
                                                            }
                                                            if (hostedSidebarRail && !railAtStart) rail()
                                                        }
                                                    }
                                                } else {
                                                    key(uiState.stealthModeEnabled) { navDisplay() }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        SeasonChromeOverlay(modifier = Modifier.fillMaxSize())
                        PixelChromeOverlay(modifier = Modifier.fillMaxSize())

                        if (uiState.nightBackgroundPassthrough) {
                            NightBackgroundEffectOverlay(
                                enabled = darkMode,
                                effectValue = selectedNightBackgroundEffect.value,
                                passthrough = true,
                                passthroughOpacity = uiState.nightBackgroundPassthroughOpacity,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        GlobalSnowEffectOverlay(
                            enabled = darkMode &&
                                uiState.globalSnowEnabled &&
                                !(
                                    rainStyleActive &&
                                        GlobalSnowEffect.fromValue(uiState.globalSnowEffect) ==
                                        GlobalSnowEffect.SeasonalRain
                                    ),
                            effectValue = uiState.globalSnowEffect,
                            modifier = Modifier.fillMaxSize(),
                        )

                        GlobalScrollEffectOverlay(
                            state = globalScrollEffectState,
                            modifier = Modifier.fillMaxSize(),
                        )

                        ThemeModeTransitionOverlay(darkMode = darkMode)

                        if (showStartupAnimation && !startupAnimationUri.isNullOrBlank()) {
                            StartupAnimationOverlay(
                                uriString = startupAnimationUri,
                                settings = uiState.startupAnimationSettings,
                                onFinished = { showStartupAnimation = false },
                                onError = { showStartupAnimation = false },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        val challenge = intent.getStringExtra("launcher_challenge")
        if (challenge != null) {
            intent.removeExtra("launcher_challenge")
            // Hide aliases only after the signed entry has reached the real, resumed Manager.
            if (!me.weishu.kernelsu.ui.util.InstalledLauncher.acceptChallenge(this, challenge)) {
                android.widget.Toast.makeText(this, R.string.launcher_install_verify_failed, android.widget.Toast.LENGTH_LONG).show()
            }
        }
        launcherVerificationJob?.cancel()
        launcherVerificationJob = lifecycleScope.launch {
            try {
                me.weishu.kernelsu.ui.util.InstalledLauncher.completePendingInstall(this@MainActivity)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("InstalledLauncher", "Unable to verify the newly installed entry", error)
                android.widget.Toast.makeText(this@MainActivity, R.string.launcher_install_verify_failed,
                    android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        KernelStatusEvents.requestRefresh()
        StealthModeStore.reconcileFromRootAsync(this)
        if (!StealthModeStore.isEnabled(this)) {
            StartupSoundPlayer.playConfigured(this)
            BackgroundMusicPlayer.playConfigured(this)
        }
    }

    override fun onStop() {
        launcherVerificationJob?.cancel()
        StartupSoundPlayer.stop()
        ClickSoundPlayer.release()
        BackgroundMusicPlayer.stop()
        super.onStop()
    }

    override fun onDestroy() {
        StartupSoundPlayer.stop()
        ClickSoundPlayer.release()
        BackgroundMusicPlayer.stop()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Increment intentState to trigger LaunchedEffect re-execution
        intentState.value += 1
    }
}

private fun MainActivityUiState.effectiveCustomBackground(
    mainDestination: MainDestination,
    pageTarget: CustomPageBackgroundTarget?,
): CustomBackgroundState {
    val routeBackground = pageTarget
        ?.let(customPageBackgrounds::get)
        ?.takeIf { it.hasMedia }
    if (routeBackground != null) {
        return routeBackground
    }
    if (pageTarget != null) return globalCustomBackground()

    return customBackgroundForMainDestination(mainDestination)
}

internal fun customPageBackgroundTarget(
    route: Route?,
    storeAncestor: Boolean = false,
): CustomPageBackgroundTarget? {
    return when (route) {
        // Editors follow their store/settings entry point, not the pager hidden underneath.
        Route.Backgrounds, Route.HomeCardWallpapers, Route.InstallCardWallpapers, Route.NavigationIcons ->
            if (storeAncestor) CustomPageBackgroundTarget.Store else CustomPageBackgroundTarget.Settings
        Route.InterfaceStyles -> CustomPageBackgroundTarget.Settings
        Route.Install -> CustomPageBackgroundTarget.Install
        Route.Kpm -> CustomPageBackgroundTarget.Kpm
        is Route.AppProfile -> CustomPageBackgroundTarget.Superuser
        Route.AppProfileTemplate, is Route.TemplateEditor -> CustomPageBackgroundTarget.Templates
        Route.About -> CustomPageBackgroundTarget.About
        else -> if (route.isStoreRoute()) CustomPageBackgroundTarget.Store else null
    }
}

private fun MainActivityUiState.mainPagerBackgrounds(
    kpmActive: Boolean,
    stealthModeEnabled: Boolean,
): List<CustomBackgroundState> {
    return mainDestinations(kpmActive, stealthModeEnabled).map(::customBackgroundForMainDestination)
}

private fun MainActivityUiState.customBackgroundForMainDestination(
    destination: MainDestination,
): CustomBackgroundState {
    val target = when (destination) {
        MainDestination.Home -> CustomPageBackgroundTarget.Home
        MainDestination.SuperUser -> CustomPageBackgroundTarget.Superuser
        MainDestination.Module -> CustomPageBackgroundTarget.Module
        MainDestination.Settings -> CustomPageBackgroundTarget.Settings
        MainDestination.Kpm -> CustomPageBackgroundTarget.Kpm
    }
    return target
        .let { customPageBackgrounds[it] }
        .takeIf { it.hasMedia }
        ?: globalCustomBackground()
}

private fun MainActivityUiState.globalCustomBackground(): CustomBackgroundState {
    return CustomBackgroundState(
        wallpaperUriString = customWallpaperUri,
        videoUriString = customVideoBackgroundUri,
        opacity = customWallpaperOpacity,
        crop = customWallpaperCrop,
        videoDurationSeconds = customVideoBackgroundDurationSeconds,
        videoFrameRate = customVideoBackgroundFrameRate,
        visualSettings = customWallpaperVisualSettings,
    )
}

private fun Route?.hostsMainPager(): Boolean {
    return this == Route.Main ||
        this == Route.Home ||
        this == Route.SuperUser ||
        this == Route.Module ||
        this == Route.Settings
}

private fun Route?.isStoreRoute(): Boolean = when (this) {
    Route.ThemeStore, Route.ThemeStoreCustomize, Route.InterfaceStyleStore,
    Route.StoreInterfaceStyles, Route.ThemeStoreAssets, Route.ThemeStoreFonts,
    Route.ThemeStoreBackgrounds, Route.ThemeStoreTransfer, Route.ThemeStoreMy,
    Route.ThemeStoreLibrary, Route.ThemeCommentAccount, is Route.CloudThemeDetail, is Route.ThemeComments, Route.CloudThemeRanking,
    Route.CloudThemeCreator, Route.CloudThemeCreatorSubmission, Route.CloudThemeCreatorGuide,
    -> true
    else -> false
}

internal fun shouldShowSettingsSidebar(
    route: Route?,
    settingsAncestor: Boolean,
    storeAncestor: Boolean = false,
): Boolean {
    if (storeAncestor || route.isStoreRoute()) return false
    return when (route) {
        is Route.SettingsCategory, Route.InterfaceStyles, Route.SidebarWidgetSettings, Route.RainDesign, Route.About -> true
        null, Route.Main, Route.Home, Route.SuperUser, Route.Module, Route.Settings,
        Route.Install, is Route.Flash -> false
        else -> settingsAncestor
    }
}

@Composable
private fun StatusBarContrastScrim(darkMode: Boolean) {
    val statusBarHeight = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()
    val contrastColor = if (darkMode) Color.Black else Color.White
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(statusBarHeight + 28.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        contrastColor.copy(alpha = if (darkMode) 0.42f else 0.30f),
                        contrastColor.copy(alpha = if (darkMode) 0.18f else 0.12f),
                        Color.Transparent,
                    ),
                ),
            ),
    )
}

@Composable
private fun ManagerUpdatePrompt() {
    val context = LocalContext.current
    var updateInfo by remember { mutableStateOf<ManagerUpdateInfo?>(null) }
    val forceUpdate = updateInfo?.force == true
    val updateTitle = stringResource(
        if (forceUpdate) R.string.manager_force_update_title else R.string.manager_update_title
    )
    val downloadText = stringResource(R.string.download)
    val updateContent = updateInfo?.let { latest ->
        val version = stringResource(
            if (latest.force) R.string.manager_force_update_message else R.string.manager_update_message,
            latest.versionName,
            latest.versionCode,
        )
        val changelog = latest.changelog.trim().take(MAX_MANAGER_UPDATE_CHANGELOG_LENGTH)
        if (changelog.isBlank()) {
            version
        } else {
            stringResource(R.string.manager_update_changelog, version, changelog)
        }
    }
    val updateDialog = rememberConfirmDialog(
        onConfirm = {
            updateInfo?.let { ManagerUpdateChecker.download(context, it) }
        },
        onDismiss = {
            if (updateInfo?.force != true) {
                updateInfo = null
            }
        },
    )

    LaunchedEffect(updateInfo, updateContent, updateTitle, downloadText) {
        val latest = updateInfo ?: return@LaunchedEffect
        val content = updateContent ?: return@LaunchedEffect
        updateDialog.showConfirm(
            title = updateTitle,
            content = content,
            markdown = latest.changelog.isNotBlank(),
            confirm = downloadText,
            dismissible = !latest.force,
        )
    }

    LaunchedEffect(Unit) {
        val latest = ManagerUpdateChecker.checkLatest(context) ?: return@LaunchedEffect
        updateInfo = latest
    }
}

@Composable
private fun Modifier.customClickSound(uriString: String?, volume: Float): Modifier {
    if (uriString.isNullOrBlank()) return this
    val context = LocalContext.current.applicationContext
    val haptic = LocalHapticFeedback.current
    return pointerInput(uriString, volume) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val startPosition = down.position
            var wasConsumed = down.isConsumed
            var moved = false
            var completed = false

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                wasConsumed = wasConsumed || change.isConsumed
                if ((change.position - startPosition).getDistance() > viewConfiguration.touchSlop) {
                    moved = true
                }
                if (!change.pressed) {
                    completed = true
                    break
                }
            }

            if (wasConsumed && completed && !moved) {
                val audioSettings = readAppAudioSettings(context)
                ClickSoundPlayer.play(context, uriString, volume)
                if (audioSettings.hapticWithClick &&
                    isAudioPlaybackAllowed(context, audioSettings.click)
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                }
            }
        }
    }
}

private const val NAV_TRANSITION_DURATION_MS = 240
private const val SIDEBAR_STACK_DURATION_MS = 280
private const val SIDEBAR_STACK_ENTER_FADE_MS = 110
private const val SIDEBAR_STACK_EXIT_FADE_MS = 70
private const val NAV_TRANSITION_FADE_IN_MS = 220
private const val NAV_TRANSITION_FADE_OUT_MS = 180
private const val NAV_TRANSITION_ENTER_SCALE = 0.965f
private const val NAV_TRANSITION_EXIT_SCALE = 0.985f
private const val LKM_WALLPAPER_SPLASH_TIMEOUT_MS = 1_500L
private const val MAX_MANAGER_UPDATE_CHANGELOG_LENGTH = 4000
private const val TAG = "MainActivity"

private fun <T : Any> stableNavForwardTransition(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    stableNavForwardTransitionContentTransform()
}

private fun <T : Any> stableNavPopTransition(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    stableNavPopTransitionContentTransform()
}

private fun <T : Any> instantNavTransition(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    instantNavTransitionContentTransform()
}

private fun <T : Any> sidebarStackForwardTransition(direction: Int): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    sidebarStackForwardTransitionContentTransform(direction)
}

internal fun sidebarStackForwardTransitionContentTransform(direction: Int): ContentTransform =
    ContentTransform(
        targetContentEnter = fadeIn(
            animationSpec = tween(SIDEBAR_STACK_ENTER_FADE_MS),
        ) + slideInHorizontally(
            animationSpec = tween(SIDEBAR_STACK_DURATION_MS, easing = FastOutSlowInEasing),
            initialOffsetX = { width -> width * direction / 4 },
        ),
        initialContentExit = fadeOut(
            animationSpec = tween(SIDEBAR_STACK_EXIT_FADE_MS),
        ) + slideOutHorizontally(
            animationSpec = tween(SIDEBAR_STACK_DURATION_MS, easing = FastOutSlowInEasing),
            targetOffsetX = { width -> -width * direction / 10 },
        ),
        targetContentZIndex = 1f,
        sizeTransform = null,
    )

private fun <T : Any> sidebarStackPopTransition(direction: Int): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    sidebarStackPopTransitionContentTransform(direction)
}

internal fun sidebarStackPopTransitionContentTransform(direction: Int): ContentTransform = ContentTransform(
    targetContentEnter = fadeIn(
        animationSpec = tween(SIDEBAR_STACK_ENTER_FADE_MS),
    ) + slideInHorizontally(
        animationSpec = tween(SIDEBAR_STACK_DURATION_MS, easing = FastOutSlowInEasing),
        initialOffsetX = { width -> -width * direction / 10 },
    ),
    initialContentExit = fadeOut(
        animationSpec = tween(SIDEBAR_STACK_EXIT_FADE_MS),
    ) + slideOutHorizontally(
        animationSpec = tween(SIDEBAR_STACK_DURATION_MS, easing = FastOutSlowInEasing),
        targetOffsetX = { width -> width * direction / 4 },
    ),
    targetContentZIndex = 0f,
    sizeTransform = null,
)

internal fun instantNavTransitionContentTransform(): ContentTransform {
    return ContentTransform(
        targetContentEnter = EnterTransition.None,
        initialContentExit = ExitTransition.None,
        sizeTransform = null,
    )
}

private fun stableNavForwardTransitionContentTransform(): ContentTransform {
    return ContentTransform(
        targetContentEnter = fadeIn(
            animationSpec = tween(NAV_TRANSITION_FADE_IN_MS, easing = LinearOutSlowInEasing),
        ) + scaleIn(
            animationSpec = tween(NAV_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing),
            initialScale = NAV_TRANSITION_ENTER_SCALE,
        ) + slideInHorizontally(
            animationSpec = tween(NAV_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing),
            initialOffsetX = { width -> width / 8 },
        ),
        initialContentExit = fadeOut(
            animationSpec = tween(NAV_TRANSITION_FADE_OUT_MS, easing = FastOutLinearInEasing),
        ) + scaleOut(
            animationSpec = tween(NAV_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing),
            targetScale = NAV_TRANSITION_EXIT_SCALE,
        ) + slideOutHorizontally(
            animationSpec = tween(NAV_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing),
            targetOffsetX = { width -> -width / 18 },
        ),
        targetContentZIndex = 1f,
        sizeTransform = null,
    )
}

private fun stableNavPopTransitionContentTransform(): ContentTransform {
    return ContentTransform(
        targetContentEnter = fadeIn(
            animationSpec = tween(NAV_TRANSITION_FADE_IN_MS, easing = LinearOutSlowInEasing),
        ) + scaleIn(
            animationSpec = tween(NAV_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing),
            initialScale = NAV_TRANSITION_ENTER_SCALE,
        ) + slideInHorizontally(
            animationSpec = tween(NAV_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing),
            initialOffsetX = { width -> -width / 8 },
        ),
        initialContentExit = fadeOut(
            animationSpec = tween(NAV_TRANSITION_FADE_OUT_MS, easing = FastOutLinearInEasing),
        ) + scaleOut(
            animationSpec = tween(NAV_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing),
            targetScale = NAV_TRANSITION_EXIT_SCALE,
        ) + slideOutHorizontally(
            animationSpec = tween(NAV_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing),
            targetOffsetX = { width -> width / 18 },
        ),
        targetContentZIndex = 0f,
        sizeTransform = null,
    )
}

val LocalMainPagerState = staticCompositionLocalOf<MainPagerState> { error("LocalMainPagerState not provided") }
private val LocalSidebarRailHostedExternally = staticCompositionLocalOf { false }

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    onDestinationChanged: (MainDestination) -> Unit = {},
    onNavigationBadgeChanged: (NavigationBadgeState) -> Unit = {},
    mainPagerState: MainPagerState,
    stealthModeEnabled: Boolean = false,
) {
    val navController = LocalNavigator.current
    val enableBlur = LocalEnableBlur.current
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    val enableFloatingBottomBarBlur = LocalEnableFloatingBottomBarBlur.current
    val autoHideNavigationBar = LocalAutoHideNavigationBar.current
    val scrollHideNavigationBar = LocalScrollHideNavigationBar.current
    val pageTransitionEffect = LocalPageTransitionEffect.current
    val interfaceStyle = LocalInterfaceStyle.current
    val seasonStyle = LocalSeasonStyle.current
    val rainStyle = LocalRainStyle.current
    val pixelStyle = LocalPixelStyle.current
    val systemAnimationsEnabled = rememberSystemAnimationsEnabled()
    val refreshTick by KernelStatusEvents.refreshTick.collectAsStateWithLifecycle()
    val kpmDisableTick by KernelStatusEvents.kpmDisableTick.collectAsStateWithLifecycle()
    val kpmEnableTick by KernelStatusEvents.kpmEnableTick.collectAsStateWithLifecycle()
    val pagerState = mainPagerState.pagerState
    val fullFeaturedResult by produceState<Boolean?>(initialValue = null, refreshTick, stealthModeEnabled) {
        if (stealthModeEnabled) {
            value = false
            return@produceState
        }
        // Keep the last confirmed value while the refresh probe is running. Writing
        // null here creates a transient false state that can remove the KPM page.
        val fullFeatured = kotlinx.coroutines.withContext(Dispatchers.IO) {
            runCatching { Natives.refreshInfo() }
            val managerRegistered = runCatching {
                Natives.isManager || ensureManagerRegistered()
            }.getOrDefault(false)
            runCatching {
                val driverVersion = Natives.version.takeIf { it > 0 }
                val requiresNewKernel = Natives.requireNewKernel()
                val uapiMismatch = Natives.checkUAPIMismatch()
                managerRegistered &&
                    driverVersion != null &&
                    ksuRootAvailable() &&
                    !hasBlockingRootVersionMismatch(
                        managerVersionCode = BuildConfig.VERSION_CODE.toLong(),
                        driverVersion = driverVersion,
                        requiresNewKernel = requiresNewKernel,
                        uapiMismatch = uapiMismatch,
                    )
            }.getOrDefault(false)
        }
        value = fullFeatured
    }
    val effectiveFullFeaturedResult = if (stealthModeEnabled) false else fullFeaturedResult
    val isFullFeatured = effectiveFullFeaturedResult == true
    val userScrollEnabled = mainPagerState.fullFeatured && !stealthModeEnabled
    val kpmPageActiveResult by produceState<KpmPageAvailability>(
        initialValue = KpmPageAvailability.Unknown,
        effectiveFullFeaturedResult,
        refreshTick,
    ) {
        value = when (effectiveFullFeaturedResult) {
            // A full-feature probe can briefly fail while manager registration or
            // the root shell is being refreshed. Keep the committed pager topology
            // until the backend capability probe reports a confirmed inactive state.
            false -> KpmPageAvailability.Unknown
            true -> kotlinx.coroutines.withContext(Dispatchers.IO) {
                runCatching { getKpmCaps() }
                    .fold(
                        onSuccess = KpmPageAvailability::fromCaps,
                        onFailure = { KpmPageAvailability.Unknown },
                    )
            }
            null -> KpmPageAvailability.Unknown
        }
    }
    // The pager owns the committed page topology. The async probe only feeds it;
    // using the probe result directly here can render a page with the old index
    // while the pager is still reconfiguring its page count.
    val kpmPageActive = mainPagerState.kpmActive && !stealthModeEnabled
    val moduleViewModel = viewModel<ModuleViewModel>()
    val moduleUiState by moduleViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(isFullFeatured) {
        if (isFullFeatured && !stealthModeEnabled && moduleViewModel.uiState.value.moduleList.isEmpty()) {
            moduleViewModel.initializePreferences()
            moduleViewModel.fetchModuleList(checkUpdate = true, resort = false)
        }
    }
    val superuserCount by produceState(initialValue = 0, isFullFeatured, refreshTick) {
        value = if (isFullFeatured && !stealthModeEnabled) {
            kotlinx.coroutines.withContext(Dispatchers.IO) {
                runCatching { getSuperuserCount() }.getOrDefault(0)
            }
        } else {
            0
        }
    }
    val navigationBadge = if (isFullFeatured) {
        NavigationBadgeState(
            superuserCount = superuserCount,
            moduleCount = moduleUiState.modules.size,
        )
    } else {
        NavigationBadgeState()
    }
    SideEffect { onNavigationBadgeChanged(navigationBadge) }
    val surfaceColor = liquidGlassBackdropColor()
    val blurBackdrop = rememberBlurBackdrop(enableBlur)
    val floatingBarBackdrop = if (enableFloatingBottomBar && enableFloatingBottomBarBlur) {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    } else {
        null
    }

    val settledPage = mainPagerState.pagerState.settledPage
    LaunchedEffect(settledPage) {
        onDestinationChanged(mainPagerState.syncSettledPage(settledPage))
    }

    LaunchedEffect(effectiveFullFeaturedResult) {
        mainPagerState.updateFeatureAvailability(effectiveFullFeaturedResult)
    }
    LaunchedEffect(kpmPageActiveResult) {
        mainPagerState.updateKpmAvailability(kpmPageActiveResult.asBooleanOrNull())
    }
    LaunchedEffect(stealthModeEnabled) {
        mainPagerState.updateStealthMode(stealthModeEnabled)
    }
    LaunchedEffect(kpmDisableTick) {
        if (kpmDisableTick > 0) {
            // An explicit user disable is authoritative. Do not wait for the
            // background probe's two-sample debounce to remove the page.
            mainPagerState.markKpmExplicitlyDisabled()
        }
    }
    LaunchedEffect(kpmEnableTick) {
        if (kpmEnableTick > 0) {
            // Only a successful enable operation may reopen the probe gate.
            // The refresh requested with this event will then confirm the
            // backend before adding the destination again.
            mainPagerState.clearKpmExplicitDisable()
        }
    }

    MainScreenBackHandler(mainPagerState, navController)

    val useNavigationRail = useNavigationRail()
    val sidebarConfig = if (interfaceStyle == InterfaceStyle.SidebarWidget.value) {
        rememberSidebarWidgetConfig()
    } else {
        null
    }
    val layoutDirection = LocalLayoutDirection.current
    val navigationBarVisibilityState = rememberNavigationBarVisibilityState(
        enabled = !useNavigationRail && (autoHideNavigationBar || scrollHideNavigationBar),
        autoHideAfterInactivity = autoHideNavigationBar,
        hideOnScroll = scrollHideNavigationBar,
    )

    LaunchedEffect(isFullFeatured) {
        if (isFullFeatured) {
            navigationBarVisibilityState.reveal(resetIdleTimer = true)
        }
    }

    CompositionLocalProvider(
        LocalMainPagerState provides mainPagerState,
        LocalLiquidGlassBackdrop provides blurBackdrop,
    ) {
        val contentReady = rememberContentReady()
        val currentPagerContent = androidx.compose.runtime.rememberUpdatedState<@Composable (Dp) -> Unit> { bottomInnerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        // A pager can retain nearby pages. Do not let the global
                        // backdrop sample a retained KPM WebView on any page.
                        if (blurBackdrop != null && !kpmPageActive) {
                            Modifier.layerBackdrop(blurBackdrop)
                        } else {
                            Modifier
                        }
                    ),
            ) {
                if (stealthModeEnabled) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = mainPagerContentMaxWidth(MainDestination.Home, sidebarConfig != null))
                                .fillMaxSize(),
                        ) {
                            HomePager(navController, bottomInnerPadding, stealthModeEnabled = true)
                        }
                    }
                } else HorizontalPager(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .then(
                            if (floatingBarBackdrop != null && !kpmPageActive) {
                                Modifier.layerBackdrop(floatingBarBackdrop)
                            } else {
                                Modifier
                            }
                        ),
                    state = mainPagerState.pagerState,
                    beyondViewportPageCount = if (contentReady) 3 else 0,
                    overscrollEffect = null,
                    userScrollEnabled = userScrollEnabled,
                ) { page ->
                    val isCurrentPage = page == settledPage
                    val destination = mainDestinations(
                        kpmActive = mainPagerState.kpmActive,
                        stealthModeEnabled = stealthModeEnabled,
                    ).getOrNull(page)
                    val containsKpmWebView = destination == MainDestination.Kpm
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .mainPageTransition(
                                effect = pageTransitionEffect,
                                interfaceStyle = interfaceStyle,
                                seasonStyle = seasonStyle,
                                rainStyle = rainStyle,
                                pixelStyle = pixelStyle,
                                animationsEnabled = systemAnimationsEnabled,
                                containsEmbeddedAndroidView = containsKpmWebView,
                                pageOffset = {
                                    page - (
                                        mainPagerState.pagerState.currentPage +
                                            mainPagerState.pagerState.currentPageOffsetFraction
                                        )
                                },
                            ),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        if (isCurrentPage || contentReady) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = mainPagerContentMaxWidth(destination, sidebarConfig != null))
                                    .fillMaxSize(),
                            ) {
                                when (destination) {
                                    MainDestination.Home -> HomePager(
                                        navController,
                                        bottomInnerPadding,
                                        isCurrentPage,
                                        stealthModeEnabled,
                                    )

                                    MainDestination.Kpm -> KpmScreen(
                                        inPager = true,
                                        bottomInnerPadding = bottomInnerPadding,
                                    )

                                    MainDestination.SuperUser -> SuperUserPager(
                                        navigator = navController,
                                        bottomInnerPadding = bottomInnerPadding,
                                        isCurrentPage = isCurrentPage,
                                        onOpenSecondary = {
                                            onDestinationChanged(MainDestination.SuperUser)
                                        },
                                    )

                                    MainDestination.Module -> ModulePager(
                                        bottomInnerPadding,
                                        isCurrentPage,
                                    )

                                    MainDestination.Settings -> SettingPager(
                                        navController,
                                        bottomInnerPadding,
                                    )

                                    null -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }

        // Reparent the same pages when resizing switches between a rail and a bottom bar.
        // A plain composable lambda at different call sites would discard local page state.
        val pagerContent = remember {
            androidx.compose.runtime.movableContentOf<Dp> { bottomInnerPadding ->
                currentPagerContent.value(bottomInnerPadding)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarVisibilityController(navigationBarVisibilityState),
        ) {
        if (stealthModeEnabled || (sidebarConfig != null && LocalSidebarRailHostedExternally.current)) {
            pagerContent(WindowInsets.systemBars.asPaddingValues().calculateBottomPadding())
        } else if (useNavigationRail) {
            val railAtStart = sidebarConfig?.let {
                it.side.isAtStart(layoutDirection)
            } ?: true
            val railInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
                when (sidebarConfig?.side) {
                    SidebarSide.Left -> WindowInsetsSides.Left
                    SidebarSide.Right -> WindowInsetsSides.Right
                    null -> WindowInsetsSides.Start
                }
            )
            val navBarBottomPadding = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
            ) { _ ->
                Row(modifier = Modifier.fillMaxSize()) {
                    // Keep pager state and page effects when the rail changes sides.
                    val panes = if (railAtStart) listOf(true, false) else listOf(false, true)
                    panes.forEach { isRail ->
                        key(isRail) {
                            if (isRail) {
                                SideRail(
                                    blurBackdrop = blurBackdrop,
                                    navigationBadge = navigationBadge,
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .consumeWindowInsets(railInsets)
                                        .then(
                                            if (sidebarConfig != null) {
                                                Modifier.clip(
                                                    SidebarPaneShape
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                ) {
                                    pagerContent(navBarBottomPadding)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val bottomBar = @Composable {
                AutoHidingNavigationBar(
                    visible = navigationBarVisibilityState.visible,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        BottomBar(
                            blurBackdrop = blurBackdrop.takeUnless { kpmPageActive },
                            backdrop = floatingBarBackdrop.takeUnless { kpmPageActive },
                            navigationBadge = navigationBadge,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }

            Scaffold(
                bottomBar = bottomBar,
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
            ) { innerPadding ->
                val systemNavigationPadding = WindowInsets.systemBars
                    .asPaddingValues()
                    .calculateBottomPadding()
                pagerContent(
                    resolveMainContentBottomPadding(
                        scaffoldPadding = innerPadding.calculateBottomPadding(),
                        systemNavigationPadding = systemNavigationPadding,
                        floatingBarEnabled = enableFloatingBottomBar && !me.weishu.kernelsu.ui.util.currentAppWindowSize().compactNavigation,
                        navigationBarVisible = navigationBarVisibilityState.visible,
                    ),
                )
            }
        }
        }
    }
}

internal fun mainPagerContentMaxWidth(destination: MainDestination?, sidebarStyle: Boolean): Dp = when {
    sidebarStyle || destination == MainDestination.Settings -> SidebarUiTokens.ContentMaxWidth
    destination == MainDestination.Kpm -> 960.dp
    else -> SidebarUiTokens.DetailMaxWidth
}

internal fun resolveMainContentBottomPadding(
    scaffoldPadding: Dp,
    systemNavigationPadding: Dp,
    floatingBarEnabled: Boolean,
    navigationBarVisible: Boolean,
): Dp {
    if (!floatingBarEnabled || !navigationBarVisible) return maxOf(scaffoldPadding, systemNavigationPadding)
    val floatingBarClearance = 88.dp + systemNavigationPadding
    return maxOf(scaffoldPadding, floatingBarClearance)
}

internal enum class KpmPageAvailability {
    Active,
    Inactive,
    Unknown;

    fun asBooleanOrNull(): Boolean? = when (this) {
        Active -> true
        Inactive -> false
        Unknown -> null
    }

    companion object {
        fun fromCaps(caps: KpmCaps): KpmPageAvailability {
            if (caps.error.isNotBlank()) return Unknown
            if (caps.lateLoad || caps.backend == "none") return Inactive
            return when (caps.backend) {
                "native-gki" -> if (caps.managementAvailable) Active else Inactive
                "kpatch-next" -> if (caps.managementAvailable) Active else Inactive
                else -> Unknown
            }
        }

        fun fromStatus(status: KPatchNextStatus): KpmPageAvailability {
            if (status.error.isNotBlank()) return Unknown
            return if (shouldShowKpmPage(status)) Active else Inactive
        }
    }
}

internal fun shouldShowKpmPage(status: KPatchNextStatus?): Boolean {
    return status?.let {
        it.error.isBlank() && it.installed && it.enabled && !it.pendingRemove
    } == true
}

internal fun shouldUseLayeredNavigationTransitions(
    kpmPageActive: Boolean,
    sidebarStyleActive: Boolean = false,
): Boolean {
    // NavDisplay animates its outgoing scene through a render layer. If the main
    // pager retains KPM's WebView, HWUI can crash in GLFunctorDrawable while that
    // layer is rendered, even when KPM is not the currently selected main page.
    // Sidebar's opaque secondary pages also expose both scenes during fades,
    // producing a white flash and overlapping cards on return.
    return !kpmPageActive && !sidebarStyleActive
}

internal fun shouldUseSidebarStackNavigationTransitions(
    kpmPageActive: Boolean,
    sidebarStyleActive: Boolean,
    animationsEnabled: Boolean,
    stealthModeEnabled: Boolean = false,
): Boolean = sidebarStyleActive && !kpmPageActive && animationsEnabled && !stealthModeEnabled

internal fun shouldHostSidebarRail(
    route: Route?,
    settingsSidebarActive: Boolean,
    fullFeatured: Boolean,
    stealthModeEnabled: Boolean,
): Boolean = fullFeatured && !stealthModeEnabled && (
    route.hostsMainPager() || route == Route.Sulog || route == Route.ModuleRepo ||
        route is Route.ModuleRepoDetail || settingsSidebarActive
)

internal fun shouldReturnMainPagerBackToHome(
    selectedPage: Int,
    kpmActive: Boolean = false,
    stealthModeEnabled: Boolean = false,
): Boolean {
    return when (mainDestinations(kpmActive, stealthModeEnabled).getOrNull(selectedPage)) {
        MainDestination.Kpm,
        MainDestination.Module,
        MainDestination.Settings,
        -> true
        else -> false
    }
}


@Composable
private fun MainScreenBackHandler(
    mainState: MainPagerState,
    navController: Navigator,
) {
    val isPagerBackHandlerEnabled by remember {
        derivedStateOf {
            navController.current() is Route.Main &&
                navController.backStackSize() == 1 &&
                shouldReturnMainPagerBackToHome(
                    selectedPage = mainState.selectedPage,
                    kpmActive = mainState.kpmActive,
                    stealthModeEnabled = mainState.stealthModeEnabled,
                )
        }
    }

    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)

    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = isPagerBackHandlerEnabled,
        onBackCompleted = {
            mainState.animateToPage(0)
        }
    )
}

/**
 * Handles ZIP file installation from external apps (e.g., file managers).
 * - In normal mode: Shows a confirmation dialog before installation
 * - In safe mode: Shows a Toast notification and prevents installation
 */
@SuppressLint("StringFormatInvalid", "LocalContextGetResourceValueCall")
@Composable
private fun ZipFileIntentHandler(
    intentState: MutableStateFlow<Int>,
    isManager: Boolean,
) {
    val activity = LocalActivity.current ?: return
    val context = LocalContext.current
    var zipUri by remember { mutableStateOf<Uri?>(null) }
    var isAnyKernel by remember { mutableStateOf(false) }
    val isSafeMode = runCatching { Natives.isSafeMode }.getOrDefault(false)
    val clearZipUri = {
        zipUri = null
        isAnyKernel = false
    }
    val navigator = LocalNavigator.current

    val installDialog = rememberConfirmDialog(
        onConfirm = {
            zipUri?.let { uri ->
                val flashIt = if (isAnyKernel) {
                    FlashIt.FlashAnyKernel(uri)
                } else {
                    FlashIt.FlashModules(listOf(uri))
                }
                navigator.push(Route.Flash(flashIt))
            }
            clearZipUri()
        },
        onDismiss = clearZipUri
    )

    fun getDisplayName(uri: Uri): String {
        return uri.getFileName(context) ?: uri.lastPathSegment ?: "Unknown"
    }

    val intentStateValue by intentState.collectAsStateWithLifecycle()
    LaunchedEffect(intentStateValue, isManager) {
        val currentIntent = activity.intent
        val uri = currentIntent?.data ?: return@LaunchedEffect

        val supportedScheme = uri.scheme == "content" || uri.scheme == "file"
        val component = currentIntent.component?.className.orEmpty()
        val isAnyKernelIntent = component.endsWith("FlashAnyKernel")
        if (!isManager || !supportedScheme || currentIntent.type != "application/zip") {
            return@LaunchedEffect
        }

        activity.intent.data = null
        activity.intent.type = null

        if (isSafeMode) {
            Toast.makeText(context, context.getString(R.string.safe_mode_module_disabled), Toast.LENGTH_SHORT).show()
        } else {
            zipUri = uri
            isAnyKernel = isAnyKernelIntent
            installDialog.showConfirm(
                title = if (isAnyKernelIntent) {
                    context.getString(R.string.anykernel_install)
                } else {
                    context.getString(R.string.module)
                },
                content = context.getString(
                    R.string.module_install_prompt_with_name,
                    "\n${getDisplayName(uri)}"
                )
            )
        }
    }
}

@Composable
private fun ShortcutIntentHandler(
    intentState: MutableStateFlow<Int>,
) {
    val activity = LocalActivity.current ?: return
    val context = LocalContext.current
    val intentStateValue by intentState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    LaunchedEffect(intentStateValue) {
        val intent = activity.intent
        val type = intent?.getStringExtra("shortcut_type") ?: return@LaunchedEffect

        when (type) {
            "module_action" -> {
                val moduleId = intent.getStringExtra("module_id") ?: return@LaunchedEffect
                navigator.push(Route.ExecuteModuleAction(moduleId, fromShortcut = true))
                intent.removeExtra("shortcut_type")
                intent.removeExtra("module_id")
            }

            "module_webui" -> {
                val moduleId = intent.getStringExtra("module_id") ?: return@LaunchedEffect
                val webIntent = Intent(context, WebUIActivity::class.java)
                    .setData("kernelsu://webui/$moduleId".toUri())
                    .putExtra("id", moduleId)
                context.startActivity(webIntent)
                intent.removeExtra("shortcut_type")
                intent.removeExtra("module_id")
            }

            else -> return@LaunchedEffect
        }
    }
}

@Composable
private fun PluginRouteGate(
    plugin: ManagerPlugin,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val registry = remember { ManagerPluginRegistry(context) }
    val compatible by produceState<Boolean?>(initialValue = null, key1 = plugin.id) {
        value = kotlinx.coroutines.withContext(Dispatchers.IO) {
            val installed = registry.list().firstOrNull { it.plugin.id == plugin.id }
            if (installed == null) {
                false
            } else {
                checkManagerPluginCompatibility(
                    plugin = installed.plugin,
                    managerVersionCode = BuildConfig.VERSION_CODE,
                    ksudStatus = getInstalledKsudStatus(),
                ).isCompatible
            }
        }
    }
    if (compatible == true) {
        content()
    } else if (compatible == false) {
        LaunchedEffect(plugin.id) {
            navigator.replace(Route.PluginStore)
        }
    }
}
