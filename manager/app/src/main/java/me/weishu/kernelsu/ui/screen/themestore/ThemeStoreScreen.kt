package me.weishu.kernelsu.ui.screen.themestore

import android.content.Intent
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.store.StoreScaffold
import me.weishu.kernelsu.ui.component.store.StoreSectionHeading
import me.weishu.kernelsu.ui.component.store.StoreExpandableSection
import me.weishu.kernelsu.ui.component.store.StoreSearchField
import me.weishu.kernelsu.ui.component.store.StoreFilters
import me.weishu.kernelsu.ui.component.store.StoreTag
import me.weishu.kernelsu.ui.component.store.StoreEmptyState
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.theme.ThemePreset
import me.weishu.kernelsu.ui.util.CustomNavigationIconSlot
import me.weishu.kernelsu.ui.util.CustomPageBackgroundTarget
import me.weishu.kernelsu.ui.util.THEME_STORE_FILE_EXTENSION
import me.weishu.kernelsu.ui.util.THEME_STORE_FILE_MIME_TYPE
import me.weishu.kernelsu.ui.util.ThemeStoreImageSlot
import me.weishu.kernelsu.ui.util.ThemeStoreImageGroup
import me.weishu.kernelsu.ui.util.ThemeStorePackageResult
import me.weishu.kernelsu.ui.util.ThemeStorePackageWarning
import me.weishu.kernelsu.ui.util.ThemeStoreSummary
import me.weishu.kernelsu.ui.util.exportThemeStorePackage
import me.weishu.kernelsu.ui.util.importThemeStorePackage
import me.weishu.kernelsu.ui.util.InterfaceStyleCatalogRepository
import me.weishu.kernelsu.ui.util.InterfaceStyleCatalogSource
import me.weishu.kernelsu.ui.util.InterfaceStyleInstaller
import me.weishu.kernelsu.ui.util.InterfaceStylePackage
import me.weishu.kernelsu.ui.util.InterfaceStyleProxyMode
import me.weishu.kernelsu.ui.util.InterfaceStyleRegistry
import me.weishu.kernelsu.ui.util.InstalledInterfaceStyle
import me.weishu.kernelsu.ui.util.InterfaceStyleDownloadProgress
import me.weishu.kernelsu.ui.util.INTERFACE_STYLE_PACKAGE_MIME_TYPE
import me.weishu.kernelsu.ui.util.INTERFACE_STYLE_RESULT_KEY
import me.weishu.kernelsu.ui.util.interfaceStylePackageFileName
import me.weishu.kernelsu.ui.util.readInterfaceStyleDownloadPreferences
import me.weishu.kernelsu.ui.util.saveInterfaceStyleDownloadPreferences
import me.weishu.kernelsu.ui.util.safeInterfaceStyleMessage
import me.weishu.kernelsu.data.repository.SettingsRepositoryImpl
import me.weishu.kernelsu.ui.util.previewThemeStorePackage
import me.weishu.kernelsu.ui.util.readThemeStoreSummary
import java.io.File
import java.io.FileOutputStream

enum class ThemeStorePage(@StringRes val titleRes: Int) {
    Overview(R.string.store_title),
    Customize(R.string.theme_store_customize_title),
    My(R.string.theme_store_my_library_title),
    Plugins(R.string.store_tab_plugins),
    Styles(R.string.store_redesign_styles),
}

internal val themeStoreNavigationPages = listOf(
    ThemeStorePage.Overview,
    ThemeStorePage.Styles,
    ThemeStorePage.Plugins,
    ThemeStorePage.Customize,
    ThemeStorePage.My,
)

enum class ThemeStoreCustomizeSection(@StringRes val titleRes: Int) {
    Style(R.string.theme_store_customize_style),
    Assets(R.string.theme_store_customize_assets),
    Atmosphere(R.string.theme_store_customize_atmosphere),
}

private enum class InterfaceStyleCategory(@StringRes val labelRes: Int) {
    All(R.string.interface_style_category_all),
    FrostedGlass(R.string.interface_style_category_frosted_glass),
    Seasons(R.string.interface_style_category_seasons),
    Rain(R.string.interface_style_category_rain),
    Pixel(R.string.interface_style_category_pixel),
    System(R.string.interface_style_category_system),
}

private fun InterfaceStylePackage.category(): InterfaceStyleCategory = when (engine) {
    "liquid_glass" -> InterfaceStyleCategory.FrostedGlass
    "snow" -> InterfaceStyleCategory.Seasons
    "rain" -> InterfaceStyleCategory.Rain
    "pixel" -> InterfaceStyleCategory.Pixel
    else -> InterfaceStyleCategory.System
}

@Composable
fun ThemeStoreScreen(
    page: ThemeStorePage = ThemeStorePage.Overview,
    customizeSection: ThemeStoreCustomizeSection = ThemeStoreCustomizeSection.Style,
    returnToAppearance: Boolean = false,
    interfaceStyleStore: Boolean = false,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    var summary by remember { mutableStateOf(readThemeStoreSummary(context)) }
    var busy by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<PendingThemeStoreImport?>(null) }
    var transferReport by remember { mutableStateOf<ThemeStoreTransferReport?>(null) }
    var selectedPageIndex by rememberSaveable(page, interfaceStyleStore) {
        mutableIntStateOf(if (interfaceStyleStore) ThemeStorePage.Styles.ordinal else page.ordinal)
    }
    val pageStateHolder = rememberSaveableStateHolder()
    var selectedStyleId by rememberSaveable { mutableStateOf<String?>(null) }
    val styleListScroll = rememberScrollState()
    val styleDetailScroll = rememberScrollState()
    LaunchedEffect(selectedStyleId) {
        if (selectedStyleId != null) styleDetailScroll.scrollTo(0)
    }
    val selectedPage = ThemeStorePage.entries.getOrElse(selectedPageIndex) {
        ThemeStorePage.Overview
    }

    fun refresh() {
        summary = readThemeStoreSummary(context)
    }

    fun showTransferFailure(error: Throwable, messageRes: Int) {
        transferReport = ThemeStoreTransferReport.failure(error)
        Toast.makeText(context, messageRes, Toast.LENGTH_LONG).show()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(THEME_STORE_FILE_MIME_TYPE),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    exportThemeStorePackage(context, uri)
                }
                transferReport = ThemeStoreTransferReport.from(result)
                Toast.makeText(
                    context,
                    when {
                        !result.success -> R.string.theme_store_export_failed
                        result.warnings.isNotEmpty() -> R.string.theme_store_export_partial
                        else -> R.string.theme_store_export_success
                    },
                    Toast.LENGTH_LONG,
                ).show()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                showTransferFailure(error, R.string.theme_store_export_failed)
            } finally {
                busy = false
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        if (busy) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    previewThemeStorePackage(context, uri)
                }
                if (result.success && result.preview != null) {
                    pendingImport = PendingThemeStoreImport(
                        uri = uri,
                        preview = result.preview,
                        warnings = result.warnings,
                    )
                } else {
                    transferReport = ThemeStoreTransferReport(
                        success = false,
                        warnings = result.warnings,
                        errorMessage = result.error?.localizedMessage
                            ?.lineSequence()
                            ?.firstOrNull()
                            ?.take(240),
                    )
                    Toast.makeText(
                        context,
                        R.string.theme_store_import_failed,
                        Toast.LENGTH_LONG,
                    ).show()
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                showTransferFailure(error, R.string.theme_store_import_failed)
            } finally {
                busy = false
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        refresh()
        onPauseOrDispose { }
    }

    val actions = ThemeStoreActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onOpenCloudTheme = { themeId ->
            navigator.push(Route.CloudThemeDetail(themeId))
        },
        onOpenCloudThemeRanking = dropUnlessResumed {
            navigator.push(Route.CloudThemeRanking)
        },
        onOpenHomeCardWallpapers = dropUnlessResumed { navigator.push(Route.HomeCardWallpapers) },
        onOpenInstallCardWallpapers = dropUnlessResumed { navigator.push(Route.InstallCardWallpapers) },
        onOpenNavigationIcons = dropUnlessResumed { navigator.push(Route.NavigationIcons) },
        onOpenFonts = dropUnlessResumed { navigator.push(Route.ThemeStoreFonts) },
        onOpenColorPalette = dropUnlessResumed { navigator.push(Route.ColorPalette) },
        onOpenUiDecorationLibrary = dropUnlessResumed { navigator.push(Route.UiDecorationLibrary) },
        onOpenVisualEffects = dropUnlessResumed { navigator.push(Route.VisualEffects) },
        onOpenProfile = dropUnlessResumed { navigator.push(Route.ThemeStoreMy) },
        onOpenInterfaceStyles = dropUnlessResumed { navigator.push(Route.StoreInterfaceStyles) },
        onOpenBackgroundSettings = dropUnlessResumed { navigator.push(Route.Backgrounds) },
        onOpenSoundEffects = dropUnlessResumed { navigator.push(Route.SoundEffects) },
        onOpenStartupAnimation = dropUnlessResumed { navigator.push(Route.StartupAnimation) },
        onExport = {
            transferReport = null
            exportLauncher.launch("SterSU-theme.$THEME_STORE_FILE_EXTENSION")
        },
        onImport = {
            transferReport = null
            importLauncher.launch(
                arrayOf(THEME_STORE_FILE_MIME_TYPE, "application/octet-stream", "*/*")
            )
        },
    )

    val onInterfaceStyleInstalled: (String) -> Unit = { id ->
        if (returnToAppearance) {
            navigator.setResult(INTERFACE_STYLE_RESULT_KEY, id)
        }
    }
    val onSelectedPage: (ThemeStorePage) -> Unit = { selected ->
        if (selected == ThemeStorePage.Plugins) {
            navigator.replace(Route.PluginStore)
        } else {
            if (selected == ThemeStorePage.My) refresh()
            selectedPageIndex = selected.ordinal
        }
    }
    val content: @Composable () -> Unit = {
        pageStateHolder.SaveableStateProvider(selectedPage.name) {
            if (selectedPage == ThemeStorePage.Styles) {
                ThemeStorePageColumn(
                    scrollState = if (selectedStyleId == null) styleListScroll else styleDetailScroll,
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 840.dp)
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                ) {
                    InterfaceStyleStoreContent(
                        selectedStyleId = selectedStyleId,
                        onSelectStyle = { selectedStyleId = it },
                        onInstalled = onInterfaceStyleInstalled,
                        onConfigureSidebar = dropUnlessResumed {
                            navigator.push(Route.SidebarWidgetSettings)
                        },
                    )
                }
            } else {
                when (selectedPage) {
                    ThemeStorePage.Styles -> Unit
                    ThemeStorePage.Overview -> ThemeStoreOverviewContent(
                        actions = actions,
                        modifier = Modifier.fillMaxSize(),
                    )

                    ThemeStorePage.Customize -> ThemeStoreCustomizeContent(
                        summary = summary,
                        actions = actions,
                        initialSection = customizeSection,
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 840.dp)
                            .fillMaxWidth(),
                    )

                    ThemeStorePage.Plugins -> ThemeStorePageColumn(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 840.dp)
                            .fillMaxWidth(),
                    ) {
                        ThemeStoreDestinationItem(
                            title = stringResource(R.string.store_tab_plugins),
                            summary = stringResource(R.string.store_summary),
                            status = stringResource(R.string.store_tab_plugins),
                            icon = Icons.Rounded.Extension,
                            onClick = { navigator.replace(Route.PluginStore) },
                        )
                    }

                    ThemeStorePage.My -> ThemeStoreLibraryScreen(
                        embedded = true,
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 840.dp)
                            .fillMaxWidth(),
                        headerContent = { libraryBusy ->
                            StoreSectionHeading(stringResource(R.string.store_tab_library),
                                stringResource(R.string.store_redesign_library_intro))
                            ThemeStoreCurrentThemeStatus(summary)
                            StoreExpandableSection(stringResource(R.string.store_redesign_backup)) {
                                Text(stringResource(R.string.theme_store_transfer_panel_summary), style = MaterialTheme.typography.bodyMedium)
                                FilledTonalButton(onClick = actions.onExport, enabled = !busy && !libraryBusy,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                    Text(stringResource(R.string.theme_store_export))
                                }
                                OutlinedButton(onClick = actions.onImport, enabled = !busy && !libraryBusy,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                    Text(stringResource(R.string.theme_store_import))
                                }
                                ThemeStoreNotice(stringResource(R.string.theme_store_import_replaces_notice))
                            }
                            transferReport?.let { ThemeStoreTransferReportCard(it) }
                        },
                    )
                }
            }
        }
    }

    StoreScaffold(
        title = stringResource(if (selectedPage == ThemeStorePage.Styles && selectedStyleId != null)
            R.string.interface_style_details else R.string.store_title),
        onBack = {
            if (selectedPage == ThemeStorePage.Styles && selectedStyleId != null) selectedStyleId = null
            else actions.onBack()
        },
        actions = {
            if (selectedPage == ThemeStorePage.My) {
                IconButton(onClick = actions.onOpenProfile) {
                    Icon(Icons.Rounded.Person, stringResource(R.string.theme_store_my_title))
                }
            }
        },
    ) { padding ->
        ThemeStoreResponsiveLayout(
            selectedPage = selectedPage,
            onSelected = onSelectedPage,
            showNavigation = selectedPage != ThemeStorePage.Styles || selectedStyleId == null,
            showBackButton = false,
            modifier = Modifier.fillMaxSize().padding(padding),
            onBack = actions.onBack,
            content = content,
        )
    }

    pendingImport?.let { pending ->
        ThemeStoreImportPreviewDialog(
            pending = pending,
            confirmLabel = stringResource(R.string.theme_store_import_preview_apply_action),
            onDismiss = { pendingImport = null },
            onConfirm = {
                pendingImport = null
                busy = true
                scope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) {
                            importThemeStorePackage(context, pending.uri)
                        }
                        transferReport = ThemeStoreTransferReport.from(result)
                        refresh()
                        Toast.makeText(
                            context,
                            when {
                                !result.success -> R.string.theme_store_import_failed
                                result.warnings.isNotEmpty() -> R.string.theme_store_import_partial
                                else -> R.string.theme_store_import_success
                            },
                            Toast.LENGTH_LONG,
                        ).show()
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (error: Throwable) {
                        showTransferFailure(error, R.string.theme_store_import_failed)
                    } finally {
                        busy = false
                    }
                }
            },
        )
    }
}

@Composable
private fun ThemeStoreResponsiveLayout(
    selectedPage: ThemeStorePage,
    onSelected: (ThemeStorePage) -> Unit,
    showNavigation: Boolean = true,
    showBackButton: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useNavigationRail = showNavigation && maxWidth >= 720.dp
        if (!showNavigation) {
            ThemeStoreContentFrame(
                showBackButton = showBackButton,
                onBack = onBack,
                modifier = Modifier.fillMaxSize(),
                content = content,
            )
        } else if (useNavigationRail) {
            Row(modifier = Modifier.fillMaxSize()) {
                ThemeStoreNavigationRail(
                    selectedPage = selectedPage,
                    onSelected = onSelected,
                    modifier = Modifier.navigationBarsPadding(),
                )
                ThemeStoreContentFrame(
                    showBackButton = showBackButton,
                    onBack = onBack,
                    modifier = Modifier.weight(1f),
                    content = content,
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                ThemeStoreContentFrame(
                    showBackButton = showBackButton,
                    onBack = onBack,
                    modifier = Modifier.weight(1f),
                    content = content,
                )
                ThemeStoreNavigationBar(
                    selectedPage = selectedPage,
                    onSelected = onSelected,
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
        }
    }
}

@Composable
private fun ThemeStoreContentFrame(
    showBackButton: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 1120.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            content()
            if (showBackButton) {
                ThemeStoreBackButton(onClick = onBack)
            }
        }
    }
}

@Composable
internal fun ThemeStoreNavigationBar(
    selectedPage: ThemeStorePage,
    onSelected: (ThemeStorePage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = themeStorePalette()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .background(palette.navigationSurface)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        themeStoreNavigationPages.forEach { destination ->
            val selected = selectedPage == destination
            val icon = themeStorePageIcon(destination)
            val label = themeStorePageLabel(destination)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = { onSelected(destination) },
                    )
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selected) palette.accentContainer else Color.Transparent,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(21.dp),
                        tint = if (selected) palette.accent else palette.mutedText,
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) palette.text else palette.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun ThemeStoreNavigationRail(
    selectedPage: ThemeStorePage,
    onSelected: (ThemeStorePage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = themeStorePalette()
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(96.dp)
            .background(palette.navigationSurface)
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .selectableGroup(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        themeStoreNavigationPages.forEach { destination ->
            val selected = selectedPage == destination
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = { onSelected(destination) },
                    )
                    .background(if (selected) palette.accentContainer else Color.Transparent)
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
            ) {
                Icon(
                    imageVector = themeStorePageIcon(destination),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = if (selected) palette.accent else palette.mutedText,
                )
                Text(
                    text = themeStorePageLabel(destination),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) palette.text else palette.mutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun themeStorePageIcon(page: ThemeStorePage): ImageVector = when (page) {
    ThemeStorePage.Styles -> Icons.Rounded.Palette
    ThemeStorePage.Overview -> Icons.Rounded.Explore
    ThemeStorePage.Customize -> Icons.Rounded.AutoFixHigh
    ThemeStorePage.Plugins -> Icons.Rounded.Extension
    ThemeStorePage.My -> Icons.Rounded.SaveAlt
}

@Composable
private fun themeStorePageLabel(page: ThemeStorePage): String = stringResource(
    when (page) {
        ThemeStorePage.Styles -> R.string.store_redesign_styles
        ThemeStorePage.Overview -> R.string.store_redesign_themes
        ThemeStorePage.Customize -> R.string.store_tab_customize
        ThemeStorePage.Plugins -> R.string.store_tab_plugins
        ThemeStorePage.My -> R.string.store_tab_library
    }
)

@Composable
private fun ThemeStoreOverviewContent(
    actions: ThemeStoreActions,
    modifier: Modifier = Modifier,
) {
    CloudThemeDiscoverContent(
        onOpenTheme = actions.onOpenCloudTheme,
        onOpenRanking = actions.onOpenCloudThemeRanking,
        modifier = modifier,
    )
}

@Composable
private fun ThemeStoreCustomizeContent(
    summary: ThemeStoreSummary,
    actions: ThemeStoreActions,
    initialSection: ThemeStoreCustomizeSection,
    modifier: Modifier = Modifier,
) {
    var selectedSection by rememberSaveable(initialSection) {
        mutableStateOf(initialSection)
    }
    ThemeStorePageColumn(modifier) {
        StoreSectionHeading(stringResource(R.string.theme_store_customize_title), stringResource(R.string.store_redesign_customize_intro))
        ThemeStoreCurrentThemeStatus(summary)
        ThemeStoreCustomizeSelector(
            selected = selectedSection,
            onSelected = { selectedSection = it },
        )
        when (selectedSection) {
            ThemeStoreCustomizeSection.Style -> ThemeStoreStyleItems(
                summary = summary,
                actions = actions,
            )
            ThemeStoreCustomizeSection.Assets -> ThemeStoreAssetItems(summary, actions)
            ThemeStoreCustomizeSection.Atmosphere -> ThemeStoreAtmosphereItems(summary, actions)
        }
    }
}

@Composable
private fun ThemeStoreCustomizeSelector(
    selected: ThemeStoreCustomizeSection,
    onSelected: (ThemeStoreCustomizeSection) -> Unit,
) {
    StoreFilters(
        labels = ThemeStoreCustomizeSection.entries.map { stringResource(it.titleRes) },
        selectedIndex = selected.ordinal,
        onSelect = { onSelected(ThemeStoreCustomizeSection.entries[it]) },
    )
}

@Composable
private fun ThemeStoreCurrentThemeStatus(summary: ThemeStoreSummary) {
    val palette = themeStorePalette()
    val previewColor = if (summary.appearance.keyColor != 0) {
        Color(summary.appearance.keyColor)
    } else {
        palette.accent
    }
    ThemeStoreSurface {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(previewColor),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.theme_store_current_theme),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = themeStoreTextColor(),
                )
                Text(
                    text = summary.appearance.colorStyle.replace('_', ' '),
                    style = MaterialTheme.typography.bodySmall,
                    color = themeStoreMutedColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = stringResource(R.string.theme_store_applied),
                style = MaterialTheme.typography.labelMedium,
                color = palette.accent,
            )
        }
    }
}

@Composable
private fun ThemeStoreSectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = themeStoreTextColor(),
    )
}

@Composable
private fun ThemeStoreSettingsGroup(
    content: @Composable ColumnScope.() -> Unit,
) {
    ThemeStoreSurface {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            content = content,
        )
    }
}

@Composable
private fun ThemeStoreSettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = themeStorePalette().mutedText.copy(alpha = 0.18f),
    )
}

@Composable
private fun ColumnScope.ThemeStoreStyleItems(
    summary: ThemeStoreSummary,
    actions: ThemeStoreActions,
) {
    ThemeStoreSectionHeader(stringResource(R.string.interface_style_store_title))
    ThemeStoreDestinationItem(
        title = stringResource(R.string.interface_style_store_title),
        summary = stringResource(R.string.interface_style_store_summary),
        status = stringResource(R.string.theme_store_open_editor),
        icon = Icons.Rounded.Palette,
        onClick = actions.onOpenInterfaceStyles,
    )
    ThemeStoreSettingsGroup {
        ThemeStoreDestinationRow(
            title = stringResource(R.string.theme_store_monet_title),
            summary = stringResource(R.string.theme_store_monet_summary),
            status = if (summary.appearance.miuixMonet) {
                stringResource(
                    R.string.theme_store_monet_status,
                    (summary.appearance.monetSurfaceOpacity * 100).toInt(),
                )
            } else {
                stringResource(R.string.theme_store_monet_disabled)
            },
            icon = Icons.Rounded.Palette,
            onClick = actions.onOpenColorPalette,
        )
        ThemeStoreSettingsDivider()
        ThemeStoreDestinationRow(
            title = stringResource(R.string.settings_ui_decoration_library),
            summary = stringResource(R.string.settings_ui_decoration_library_summary),
            status = stringResource(R.string.theme_store_open_editor),
            icon = Icons.Rounded.AutoFixHigh,
            onClick = actions.onOpenUiDecorationLibrary,
        )
        ThemeStoreSettingsDivider()
        ThemeStoreDestinationRow(
            title = stringResource(R.string.settings_section_visual_effects),
            summary = stringResource(R.string.settings_visual_effects_summary),
            status = stringResource(R.string.theme_store_open_editor),
            icon = Icons.Rounded.Visibility,
            onClick = actions.onOpenVisualEffects,
        )
    }
}

@Composable
private fun ColumnScope.ThemeStoreAssetItems(
    summary: ThemeStoreSummary,
    actions: ThemeStoreActions,
) {
    ThemeStoreSettingsGroup {
        ThemeStoreDestinationRow(
            title = stringResource(R.string.home_card_wallpapers),
            summary = stringResource(R.string.home_card_wallpapers_summary),
            status = stringResource(
                R.string.theme_store_configured_count,
                summary.cardConfiguredCount,
                ThemeStoreImageSlot.entries.count { it.group == ThemeStoreImageGroup.Home },
            ),
            icon = Icons.Rounded.ImageSearch,
            onClick = actions.onOpenHomeCardWallpapers,
        )
        ThemeStoreSettingsDivider()
        ThemeStoreDestinationRow(
            title = stringResource(R.string.install_card_wallpapers),
            summary = stringResource(R.string.install_card_wallpapers_summary),
            status = stringResource(
                R.string.theme_store_configured_count,
                summary.installCardConfiguredCount,
                ThemeStoreImageSlot.entries.count { it.group == ThemeStoreImageGroup.Install },
            ),
            icon = Icons.Rounded.AutoFixHigh,
            onClick = actions.onOpenInstallCardWallpapers,
        )
        ThemeStoreSettingsDivider()
        ThemeStoreDestinationRow(
            title = stringResource(R.string.theme_store_navigation_icons),
            summary = stringResource(R.string.settings_navigation_icons_summary),
            status = stringResource(
                R.string.theme_store_configured_count,
                summary.navigationIcons.selectedCount,
                CustomNavigationIconSlot.entries.size,
            ),
            icon = Icons.Rounded.Settings,
            onClick = actions.onOpenNavigationIcons,
        )
        ThemeStoreSettingsDivider()
        ThemeStoreDestinationRow(
            title = stringResource(R.string.app_font_title),
            summary = stringResource(R.string.theme_store_fonts_summary),
            status = appFontStateName(summary.appFont),
            icon = Icons.Rounded.FontDownload,
            onClick = actions.onOpenFonts,
        )
    }
    ThemeStoreNotice(stringResource(R.string.theme_store_assets_full_editor_notice))
}

@Composable
private fun ColumnScope.ThemeStoreAtmosphereItems(
    summary: ThemeStoreSummary,
    actions: ThemeStoreActions,
) {
    ThemeStoreSettingsGroup {
        ThemeStoreDestinationRow(
            title = stringResource(R.string.theme_store_global_background),
            summary = stringResource(R.string.theme_store_global_background_summary),
            status = stringResource(
                when {
                    summary.wallpaper.hasVideoSelected -> R.string.settings_video_background_selected_summary
                    summary.wallpaper.hasImageSelected -> R.string.settings_wallpaper_selected_summary
                    else -> R.string.settings_background_summary
                }
            ),
            icon = Icons.Rounded.Wallpaper,
            onClick = actions.onOpenBackgroundSettings,
        )
        ThemeStoreSettingsDivider()
        ThemeStoreDestinationRow(
            title = stringResource(R.string.settings_sound_effects),
            summary = stringResource(R.string.settings_sound_effects_summary),
            status = stringResource(
                R.string.theme_store_configured_count,
                summary.audio.configuredCount,
                3,
            ),
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
            onClick = actions.onOpenSoundEffects,
        )
        ThemeStoreSettingsDivider()
        ThemeStoreDestinationRow(
            title = stringResource(R.string.settings_startup_animation),
            summary = stringResource(R.string.settings_startup_animation_summary),
            status = stringResource(
                if (summary.startupAnimationUri.isNullOrBlank()) {
                    R.string.theme_store_item_empty
                } else {
                    R.string.theme_store_item_selected
                }
            ),
            icon = Icons.Rounded.PlayCircle,
            onClick = actions.onOpenStartupAnimation,
        )
    }
    ThemeStoreNotice(stringResource(R.string.theme_store_page_backgrounds_notice))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InterfaceStyleStoreContent(
    selectedStyleId: String?,
    onSelectStyle: (String?) -> Unit,
    onInstalled: (String) -> Unit,
    onConfigureSidebar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { InterfaceStyleCatalogRepository(context) }
    val registry = remember { InterfaceStyleRegistry(context) }
    val installer = remember { InterfaceStyleInstaller(context, registry) }
    val settingsRepository = remember { SettingsRepositoryImpl() }
    var snapshot by remember { mutableStateOf<me.weishu.kernelsu.ui.util.InterfaceStyleCatalogSnapshot?>(null) }
    var installed by remember { mutableStateOf(registry.list()) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf<InterfaceStyleDownloadProgress?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var preferences by remember { mutableStateOf(readInterfaceStyleDownloadPreferences(context)) }
    var customProxy by rememberSaveable { mutableStateOf(preferences.customProxy) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategoryKey by rememberSaveable { mutableStateOf(InterfaceStyleCategory.All.name) }
    var catalogLoading by remember { mutableStateOf(false) }
    var pendingSaveStyle by remember { mutableStateOf<InterfaceStylePackage?>(null) }

    fun loadCatalog(force: Boolean) {
        if (catalogLoading) return
        catalogLoading = true
        error = null
        scope.launch {
            try {
                snapshot = withContext(Dispatchers.IO) { repository.fetch(forceNetwork = force) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                error = failure.safeInterfaceStyleMessage()
            } finally {
                catalogLoading = false
            }
        }
    }
    LaunchedEffect(Unit) { loadCatalog(force = true) }

    fun runInstall(style: InterfaceStylePackage, applyAfter: Boolean) {
        if (busyId != null) return
        busyId = style.id
        error = null
        progress = null
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    installer.install(style, preferences) { next ->
                        scope.launch(Dispatchers.Main.immediate) { progress = next }
                    }
                }
                if (applyAfter) {
                    withContext(Dispatchers.IO) { settingsRepository.applyInterfaceStylePackage(result.style) }
                }
                installed = registry.list()
                Toast.makeText(
                    context,
                    if (applyAfter) R.string.interface_style_download_applied else R.string.interface_style_downloaded,
                    Toast.LENGTH_SHORT,
                ).show()
                onInstalled(style.id)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                error = failure.safeInterfaceStyleMessage()
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            } finally {
                busyId = null
                progress = null
            }
        }
    }

    fun applyInstalled(style: InterfaceStylePackage) {
        if (busyId != null) return
        busyId = style.id
        error = null
        scope.launch {
            try {
                withContext(Dispatchers.IO) { settingsRepository.applyInterfaceStylePackage(style) }
                Toast.makeText(context, R.string.interface_style_applied, Toast.LENGTH_SHORT).show()
                onInstalled(style.id)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                error = failure.safeInterfaceStyleMessage()
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            } finally {
                busyId = null
            }
        }
    }

    fun isActive(style: InterfaceStylePackage): Boolean =
        settingsRepository.activeInterfaceStyleId == style.id &&
            (settingsRepository.uiMode == style.engine ||
                style.id == "alpha-delta" && style.engine == InterfaceStyle.Alpha.value &&
                settingsRepository.uiMode == InterfaceStyle.Delta.value) &&
            when (style.engine) {
                InterfaceStyle.Snow.value -> settingsRepository.seasonStyle == style.variant
                InterfaceStyle.Rain.value -> settingsRepository.rainStyle == style.variant
                InterfaceStyle.Pixel.value -> settingsRepository.pixelStyle == style.variant
                else -> true
            }

    fun exportPackage(
        style: InterfaceStylePackage,
        successMessage: Int,
        consume: suspend (ByteArray) -> Unit,
    ) {
        if (busyId != null) return
        busyId = style.id
        error = null
        progress = null
        scope.launch {
            try {
                val bytes = installer.downloadPackage(style, preferences) { next ->
                    scope.launch(Dispatchers.Main.immediate) { progress = next }
                }
                consume(bytes)
                Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                error = failure.safeInterfaceStyleMessage()
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            } finally {
                busyId = null
                progress = null
            }
        }
    }

    val savePackageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(INTERFACE_STYLE_PACKAGE_MIME_TYPE),
    ) { uri ->
        val style = pendingSaveStyle
        pendingSaveStyle = null
        if (uri == null || style == null) return@rememberLauncherForActivityResult
        exportPackage(style, R.string.interface_style_saved_to_device) { bytes ->
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri, "w")?.use { output ->
                    output.write(bytes)
                    output.flush()
                } ?: throw IllegalStateException("Cannot open the selected destination")
            }
        }
    }

    fun sharePackage(style: InterfaceStylePackage) {
        exportPackage(style, R.string.interface_style_share_ready) { bytes ->
            val packageFile = withContext(Dispatchers.IO) {
                val directory = File(context.cacheDir, "shared-interface-styles")
                require(directory.exists() || directory.mkdirs()) { "Cannot prepare the share cache" }
                File(directory, interfaceStylePackageFileName(style)).also { target ->
                    FileOutputStream(target).use { output ->
                        output.write(bytes)
                        output.fd.sync()
                    }
                }
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                packageFile,
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                setDataAndType(uri, INTERFACE_STYLE_PACKAGE_MIME_TYPE)
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TITLE, style.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(
                    shareIntent,
                    context.getString(R.string.interface_style_share_title, style.name),
                )
            )
        }
    }

    val selectedStyle = snapshot?.catalog?.styles?.firstOrNull { it.id == selectedStyleId }
    BackHandler(enabled = selectedStyleId != null) { onSelectStyle(null) }
    if (selectedStyleId != null && selectedStyle == null) {
        if (catalogLoading) {
            me.weishu.kernelsu.ui.component.store.StoreLoadingItems()
        } else {
            StoreEmptyState(Icons.Rounded.Palette,
                stringResource(R.string.store_redesign_unavailable),
                stringResource(R.string.store_redesign_unavailable_summary),
                stringResource(R.string.back), { onSelectStyle(null) })
        }
        return
    }
    if (selectedStyle != null) {
        val installedStyle = installed.firstOrNull { it.style.id == selectedStyle.id }
        InterfaceStyleDetailContent(
            style = selectedStyle,
            installed = installedStyle,
            active = isActive(selectedStyle),
            busy = busyId != null,
            progress = if (busyId == selectedStyle.id) progress else null,
            onDownload = { runInstall(selectedStyle, applyAfter = false) },
            onApply = {
                if (installedStyle == null || hasInterfaceStyleUpdate(installedStyle, selectedStyle))
                    runInstall(selectedStyle, applyAfter = true)
                else applyInstalled(installedStyle.style)
            },
            onSave = {
                pendingSaveStyle = selectedStyle
                savePackageLauncher.launch(interfaceStylePackageFileName(selectedStyle))
            },
            onShare = { sharePackage(selectedStyle) },
            onConfigure = onConfigureSidebar.takeIf {
                installedStyle != null && selectedStyle.engine == InterfaceStyle.SidebarWidget.value
            },
            onRemove = {
                if (isActive(selectedStyle)) {
                    settingsRepository.applyInterfaceStyle(
                        UiMode.DEFAULT_VALUE,
                        ThemePreset.CLEAN_TOOL,
                        settingsRepository.themeMode,
                    )
                }
                registry.remove(selectedStyle.id)
                installed = registry.list()
            },
        )
        error?.let { ThemeStoreNotice(stringResource(R.string.interface_style_operation_failed, it)) }
        return
    }

    val selectedCategory = InterfaceStyleCategory.entries.firstOrNull {
        it.name == selectedCategoryKey
    } ?: InterfaceStyleCategory.All
    val visibleStyles = remember(snapshot, query, selectedCategory) {
        val normalized = query.trim().lowercase()
        snapshot?.catalog?.styles.orEmpty().filter { style ->
            (selectedCategory == InterfaceStyleCategory.All || style.category() == selectedCategory) &&
                (normalized.isBlank() || listOf(style.name, style.summary, style.engine, style.variant.orEmpty())
                    .any { normalized in it.lowercase() })
        }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StoreSectionHeading(
            stringResource(R.string.interface_style_store_title),
            stringResource(R.string.store_redesign_style_intro),
            action = {
                IconButton(onClick = { loadCatalog(true) }, enabled = !catalogLoading && busyId == null) {
                    Icon(Icons.Rounded.Refresh, stringResource(R.string.interface_style_catalog_refresh))
                }
            },
        )
        StoreSearchField(query, { query = it }, stringResource(R.string.interface_style_search_hint))
        StoreFilters(
            InterfaceStyleCategory.entries.map { stringResource(it.labelRes) },
            selectedCategory.ordinal,
            { selectedCategoryKey = InterfaceStyleCategory.entries[it].name },
        )
        StoreExpandableSection(
            title = stringResource(R.string.store_redesign_network),
            summary = stringResource(when (preferences.mode) {
                InterfaceStyleProxyMode.Direct -> R.string.interface_style_proxy_direct
                InterfaceStyleProxyMode.Auto -> R.string.interface_style_proxy_auto
                InterfaceStyleProxyMode.Custom -> R.string.interface_style_proxy_custom
            }),
        ) {
            StoreFilters(
                InterfaceStyleProxyMode.entries.map { mode -> stringResource(when (mode) {
                    InterfaceStyleProxyMode.Direct -> R.string.interface_style_proxy_direct
                    InterfaceStyleProxyMode.Auto -> R.string.interface_style_proxy_auto
                    InterfaceStyleProxyMode.Custom -> R.string.interface_style_proxy_custom
                }) },
                preferences.mode.ordinal,
                { index ->
                    preferences = preferences.copy(mode = InterfaceStyleProxyMode.entries[index])
                    saveInterfaceStyleDownloadPreferences(context, preferences)
                },
            )
            if (preferences.mode == InterfaceStyleProxyMode.Custom) {
                OutlinedTextField(
                    value = customProxy, onValueChange = { customProxy = it.take(240) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(stringResource(R.string.interface_style_proxy_custom_hint)) },
                    supportingText = { Text(stringResource(R.string.interface_style_proxy_custom_summary)) },
                )
                TextButton(onClick = {
                    preferences = preferences.copy(customProxy = customProxy.trim())
                    saveInterfaceStyleDownloadPreferences(context, preferences)
                }) { Text(stringResource(R.string.interface_style_proxy_save)) }
            }
            Text(stringResource(R.string.interface_style_store_notice),
                style = MaterialTheme.typography.bodySmall, color = themeStoreMutedColor())
            snapshot?.source?.let { source ->
                Text(stringResource(when (source) {
                    InterfaceStyleCatalogSource.Network -> R.string.interface_style_source_network
                    InterfaceStyleCatalogSource.Cache -> R.string.interface_style_source_cache
                    InterfaceStyleCatalogSource.Bundled -> R.string.interface_style_source_bundled
                }), style = MaterialTheme.typography.labelMedium, color = themeStoreMutedColor())
            }
        }
        snapshot?.errorMessage?.let { ThemeStoreNotice(stringResource(R.string.interface_style_catalog_offline, it)) }
        error?.let { ThemeStoreNotice(stringResource(R.string.interface_style_operation_failed, it)) }
        if (catalogLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (snapshot == null && catalogLoading) {
            me.weishu.kernelsu.ui.component.store.StoreLoadingItems()
        } else if (visibleStyles.isEmpty()) {
            StoreEmptyState(Icons.Rounded.Search,
                stringResource(R.string.store_redesign_no_results),
                stringResource(R.string.store_redesign_no_results_summary),
                stringResource(R.string.store_redesign_clear_filters),
                { query = ""; selectedCategoryKey = InterfaceStyleCategory.All.name; if (snapshot == null) loadCatalog(true) })
        } else {
            StoreSectionHeading(stringResource(R.string.store_redesign_results, visibleStyles.size))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = if (maxWidth >= 620.dp) 2 else 1
                val itemWidth = (maxWidth - 16.dp * (columns - 1)) / columns
                FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp), maxItemsInEachRow = columns) {
                    visibleStyles.forEach { style ->
                        val installedStyle = installed.firstOrNull { it.style.id == style.id }
                        InterfaceStylePackageRow(
                            style, installedStyle, isActive(style),
                            progress = if (busyId == style.id) progress else null,
                            busy = busyId != null,
                            modifier = Modifier.width(itemWidth),
                            onDetails = { onSelectStyle(style.id) },
                            onApply = {
                                if (installedStyle == null || hasInterfaceStyleUpdate(installedStyle, style)) runInstall(style, true)
                                else applyInstalled(installedStyle.style)
                            },
                        )
                    }
                }
            }
        }
    }
}

internal fun hasInterfaceStyleUpdate(installed: InstalledInterfaceStyle?, remote: InterfaceStylePackage): Boolean =
    installed != null && (remote.version > installed.style.version ||
        (remote.version == installed.style.version && !remote.sha256.equals(installed.style.sha256, ignoreCase = true)))

@Composable
private fun InterfaceStylePackageRow(
    style: InterfaceStylePackage,
    installed: InstalledInterfaceStyle?,
    active: Boolean,
    progress: InterfaceStyleDownloadProgress?,
    busy: Boolean,
    modifier: Modifier = Modifier,
    onDetails: () -> Unit,
    onApply: () -> Unit,
) {
    val context = LocalContext.current
    val updateAvailable = hasInterfaceStyleUpdate(installed, style)
    ThemeStoreSurface(modifier, onClick = onDetails) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(MaterialTheme.shapes.medium).background(Color(style.accent.toInt())),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Palette, null, tint = if (Color(style.accent.toInt()).luminance() > 0.4f) Color.Black else Color.White)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(style.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(style.category().labelRes), style = MaterialTheme.typography.labelMedium,
                        color = themeStoreMutedColor())
                }
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, stringResource(R.string.interface_style_details),
                    Modifier.size(20.dp), tint = themeStoreMutedColor())
            }
            Text(style.summary.ifBlank { style.engine }, style = MaterialTheme.typography.bodyMedium,
                color = themeStoreMutedColor(), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("v" + style.version + " · " + Formatter.formatShortFileSize(context, style.sizeBytes),
                style = MaterialTheme.typography.labelMedium, color = themeStoreMutedColor())
            progress?.let {
                if (it.fraction == null) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                else LinearProgressIndicator(progress = { it.fraction ?: 0f }, modifier = Modifier.fillMaxWidth())
            }
            FilledTonalButton(onClick = onApply, enabled = !busy && (!active || updateAvailable || installed == null),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(when {
                    updateAvailable -> R.string.cloud_theme_update_and_apply
                active && installed != null -> R.string.theme_store_applied
                    installed == null -> R.string.interface_style_download_apply
                    else -> R.string.interface_style_apply
                }))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InterfaceStyleDetailContent(
    style: InterfaceStylePackage,
    installed: InstalledInterfaceStyle?,
    active: Boolean,
    busy: Boolean,
    progress: InterfaceStyleDownloadProgress?,
    onDownload: () -> Unit,
    onApply: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onConfigure: (() -> Unit)?,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val updateAvailable = hasInterfaceStyleUpdate(installed, style)
    var confirmRemove by rememberSaveable(style.id) { mutableStateOf(false) }
    StoreSectionHeading(style.name, stringResource(style.category().labelRes))
    me.weishu.kernelsu.ui.component.store.StorePanel {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(MaterialTheme.shapes.medium).background(Color(style.accent.toInt())),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Palette, null, Modifier.size(28.dp),
                    tint = if (Color(style.accent.toInt()).luminance() > 0.4f) Color.Black else Color.White)
            }
            Text(style.summary.ifBlank { style.engine }, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        }
        StoreTag(stringResource(when {
            updateAvailable -> R.string.store_redesign_updates
            active && installed != null -> R.string.theme_store_applied
            installed != null -> R.string.interface_style_installed
            else -> R.string.interface_style_not_downloaded
        }), installed != null)
        progress?.let {
            if (it.fraction == null) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            else LinearProgressIndicator(progress = { it.fraction ?: 0f }, modifier = Modifier.fillMaxWidth())
        }
        FilledTonalButton(onClick = onApply, enabled = !busy && (!active || updateAvailable || installed == null),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(stringResource(when {
                updateAvailable -> R.string.cloud_theme_update_and_apply
                    active && installed != null -> R.string.theme_store_applied
                installed == null -> R.string.interface_style_download_apply
                else -> R.string.interface_style_apply
            }))
        }
        OutlinedButton(onClick = onDownload, enabled = !busy && (installed == null || updateAvailable), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(when {
                installed == null -> R.string.interface_style_download
                updateAvailable -> R.string.interface_style_update
                else -> R.string.interface_style_downloaded
            }))
        }
        onConfigure?.let { configure ->
            TextButton(onClick = configure, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.sidebar_widget_configure))
            }
        }
    }
    StoreSectionHeading(stringResource(R.string.store_redesign_information))
    me.weishu.kernelsu.ui.component.store.StorePanel {
        InterfaceStyleMetadataRow(stringResource(R.string.interface_style_version_label), style.version.toString())
        InterfaceStyleMetadataRow(stringResource(R.string.interface_style_size_label), Formatter.formatShortFileSize(context, style.sizeBytes))
        InterfaceStyleMetadataRow(stringResource(R.string.store_redesign_filter_category), stringResource(style.category().labelRes))
    }
    StoreExpandableSection(stringResource(R.string.interface_style_verified_title)) {
        Text(stringResource(R.string.interface_style_verified_summary), style = MaterialTheme.typography.bodyMedium)
        InterfaceStyleMetadataRow(stringResource(R.string.interface_style_engine), style.engine)
        InterfaceStyleMetadataRow(stringResource(R.string.interface_style_variant), style.variant ?: stringResource(R.string.interface_style_variant_default))
    }
    StoreExpandableSection(stringResource(R.string.store_redesign_manage)) {
        OutlinedButton(onClick = onSave, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(Icons.Rounded.SaveAlt, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.interface_style_save_to_device))
        }
        OutlinedButton(onClick = onShare, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(Icons.Rounded.Share, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.interface_style_share))
        }
        if (installed != null) TextButton(onClick = { confirmRemove = true }, enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.interface_style_remove), color = MaterialTheme.colorScheme.error)
        }
    }
    if (confirmRemove) me.weishu.kernelsu.ui.component.store.StoreAlertDialog(
        onDismissRequest = { confirmRemove = false },
        title = { Text(stringResource(R.string.interface_style_remove)) },
        text = { Text(style.name) },
        confirmButton = { TextButton(onClick = { confirmRemove = false; onRemove() }, enabled = !busy) {
            Text(stringResource(R.string.interface_style_remove))
        } },
        dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text(stringResource(android.R.string.cancel)) } },
    )
}

@Composable
private fun InterfaceStyleMetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = themeStoreMutedColor(),
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = themeStoreTextColor(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ThemeStorePageColumn(
    modifier: Modifier,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        content()
        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun ThemeStoreDestinationItem(
    title: String,
    summary: String,
    status: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    ThemeStoreSurface {
        ThemeStoreDestinationRow(
            title = title,
            summary = summary,
            status = status,
            icon = icon,
            onClick = onClick,
        )
    }
}

@Composable
private fun ThemeStoreDestinationRow(
    title: String,
    summary: String,
    status: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val palette = themeStorePalette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(palette.accentContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(21.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = themeStoreTextColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = themeStoreMutedColor(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = status,
                style = MaterialTheme.typography.labelMedium,
                color = palette.accent,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = themeStoreMutedColor(),
        )
    }
}


@Composable
private fun ThemeStoreTransferReportCard(report: ThemeStoreTransferReport) {
    val dark = isInDarkTheme()
    val palette = themeStorePalette()
    val icon = when {
        !report.success -> Icons.Rounded.Error
        report.warnings.isNotEmpty() -> Icons.Rounded.Warning
        else -> Icons.Rounded.CheckCircle
    }
    val tint = when {
        !report.success -> if (dark) Color(0xFFFFB4AB) else MaterialTheme.colorScheme.error
        report.warnings.isNotEmpty() -> if (dark) Color(0xFFFFD18A) else MaterialTheme.colorScheme.tertiary
        else -> palette.accent
    }
    ThemeStoreSurface {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint)
                Text(
                    text = stringResource(
                        when {
                            !report.success -> R.string.theme_store_transfer_failed
                            report.warnings.isNotEmpty() -> {
                                R.string.theme_store_transfer_completed_with_warnings
                            }
                            else -> R.string.theme_store_transfer_completed
                        }
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = themeStoreTextColor(),
                )
            }
            report.errorMessage?.let { error ->
                Text(
                    text = stringResource(R.string.theme_store_error_detail, error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (report.warnings.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.theme_store_warning_count, report.warnings.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = themeStoreMutedColor(),
                )
                report.warnings.take(8).forEach { warning ->
                    Text(
                        text = themeStoreWarningText(warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = themeStoreMutedColor(),
                    )
                }
            }
        }
    }
}

@Composable
private fun themeStoreWarningText(warning: ThemeStorePackageWarning): String {
    if (warning.assetId == "previous_theme_backup") {
        return stringResource(R.string.theme_store_warning_backup_cleanup)
    }
    if (warning.assetId == "app_font") {
        val base = stringResource(R.string.theme_store_warning_font)
        return warning.reason?.takeIf(String::isNotBlank)?.let { reason ->
            stringResource(R.string.theme_store_warning_with_reason, base, reason)
        } ?: base
    }
    val base = stringResource(R.string.theme_store_warning_media, warning.assetId)
    return warning.reason?.takeIf(String::isNotBlank)?.let { reason ->
        stringResource(R.string.theme_store_warning_with_reason, base, reason)
    } ?: base
}

@Composable
private fun ThemeStoreNotice(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 4.dp),
        style = MaterialTheme.typography.bodySmall,
        color = themeStoreMutedColor(),
    )
}

@Composable
private fun ThemeStoreBackButton(onClick: () -> Unit) {
    val palette = themeStorePalette()
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .padding(start = 16.dp, top = 14.dp)
            .size(48.dp)
            .clip(CircleShape)
            .background(palette.navigationSurface),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.back),
            tint = palette.text,
        )
    }
}

@Composable
private fun ThemeStoreSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val palette = themeStorePalette()
    val shape = if (LocalInterfaceStyle.current == InterfaceStyle.SidebarWidget.value) {
        MaterialTheme.shapes.large
    } else RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .clip(shape)
            .background(palette.surface)
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            ),
    ) {
        content()
    }
}

@Composable
private fun themeStoreTextColor(): Color {
    return themeStorePalette().text
}

@Composable
private fun themeStoreMutedColor(): Color {
    return themeStorePalette().mutedText
}

private data class ThemeStorePalette(
    val surface: Color,
    val navigationSurface: Color,
    val text: Color,
    val mutedText: Color,
    val accent: Color,
    val accentContainer: Color,
)

@Composable
private fun themeStorePalette(): ThemeStorePalette {
    val colors = MaterialTheme.colorScheme
    return ThemeStorePalette(
        surface = colors.surface,
        navigationSurface = colors.background,
        text = colors.onSurface,
        mutedText = colors.onSurfaceVariant,
        accent = colors.primary,
        accentContainer = colors.primaryContainer,
    )
}

private data class ThemeStoreActions(
    val onBack: () -> Unit,
    val onOpenCloudTheme: (String) -> Unit,
    val onOpenCloudThemeRanking: () -> Unit,
    val onOpenHomeCardWallpapers: () -> Unit,
    val onOpenInstallCardWallpapers: () -> Unit,
    val onOpenNavigationIcons: () -> Unit,
    val onOpenFonts: () -> Unit,
    val onOpenColorPalette: () -> Unit,
    val onOpenUiDecorationLibrary: () -> Unit,
    val onOpenVisualEffects: () -> Unit,
    val onOpenProfile: () -> Unit,
    val onOpenInterfaceStyles: () -> Unit,
    val onOpenBackgroundSettings: () -> Unit,
    val onOpenSoundEffects: () -> Unit,
    val onOpenStartupAnimation: () -> Unit,
    val onExport: () -> Unit,
    val onImport: () -> Unit,
)

private data class ThemeStoreTransferReport(
    val success: Boolean,
    val warnings: List<ThemeStorePackageWarning>,
    val errorMessage: String?,
) {
    companion object {
        fun failure(error: Throwable): ThemeStoreTransferReport {
            return ThemeStoreTransferReport(
                success = false,
                warnings = emptyList(),
                errorMessage = error.themeStoreDisplayMessage(),
            )
        }

        fun from(
            result: ThemeStorePackageResult,
        ): ThemeStoreTransferReport {
            val errorMessage = result.error?.themeStoreDisplayMessage()
            return ThemeStoreTransferReport(
                success = result.success,
                warnings = result.warnings.distinctBy { it.assetId to it.reason },
                errorMessage = errorMessage,
            )
        }
    }
}

private fun Throwable.themeStoreDisplayMessage(): String {
    return localizedMessage
        ?.trim()
        ?.lineSequence()
        ?.firstOrNull()
        ?.take(240)
        ?.takeIf { it.isNotBlank() }
        ?: javaClass.simpleName
}

private val ThemeStoreSummary.cardConfiguredCount: Int
    get() = listOf(
        lkmCard,
        classicMiuixLkmCard,
        materialLkmCard,
        superuserCard,
        moduleCard,
        statusMonitorCard,
        systemInfoCard,
        rebootMenuCard,
    ).count { it.hasSelected }

private val ThemeStoreSummary.installCardConfiguredCount: Int
    get() = listOf(
        installImageCard,
        installMethodsCard,
        installOptionsCard,
    ).count { it.hasSelected }

private val ThemeStoreSummary.mediaConfiguredCount: Int
    get() = CustomPageBackgroundTarget.entries.count { pageBackgrounds[it].hasMedia } +
        listOf(wallpaper.hasSelected, !startupAnimationUri.isNullOrBlank()).count { it } +
        audio.configuredCount

private val ThemeStoreSummary.mediaItemCount: Int
    get() = CustomPageBackgroundTarget.entries.size + 5
