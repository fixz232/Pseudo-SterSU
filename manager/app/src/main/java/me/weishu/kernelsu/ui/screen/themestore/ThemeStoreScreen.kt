package me.weishu.kernelsu.ui.screen.themestore

import android.text.format.Formatter
import android.widget.Toast
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.rounded.FileUpload
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
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproColors
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproScreen
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
import me.weishu.kernelsu.ui.util.InterfaceStyleInstaller
import me.weishu.kernelsu.ui.util.InterfaceStylePackage
import me.weishu.kernelsu.ui.util.InterfaceStyleProxyMode
import me.weishu.kernelsu.ui.util.InterfaceStyleRegistry
import me.weishu.kernelsu.ui.util.InstalledInterfaceStyle
import me.weishu.kernelsu.ui.util.InterfaceStyleDownloadProgress
import me.weishu.kernelsu.ui.util.INTERFACE_STYLE_RESULT_KEY
import me.weishu.kernelsu.ui.util.readInterfaceStyleDownloadPreferences
import me.weishu.kernelsu.ui.util.saveInterfaceStyleDownloadPreferences
import me.weishu.kernelsu.ui.util.safeInterfaceStyleMessage
import me.weishu.kernelsu.data.repository.SettingsRepositoryImpl
import me.weishu.kernelsu.ui.util.previewThemeStorePackage
import me.weishu.kernelsu.ui.util.readThemeStoreSummary
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

enum class ThemeStorePage(@StringRes val titleRes: Int) {
    Overview(R.string.theme_store),
    Customize(R.string.theme_store_customize_title),
    My(R.string.theme_store_my_library_title),
}

enum class ThemeStoreCustomizeSection(@StringRes val titleRes: Int) {
    Style(R.string.theme_store_customize_style),
    Assets(R.string.theme_store_customize_assets),
    Atmosphere(R.string.theme_store_customize_atmosphere),
}

@Composable
fun ThemeStoreScreen(
    page: ThemeStorePage = ThemeStorePage.Overview,
    customizeSection: ThemeStoreCustomizeSection = ThemeStoreCustomizeSection.Style,
    returnToAppearance: Boolean = false,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    var summary by remember { mutableStateOf(readThemeStoreSummary(context)) }
    var downloadedStyleCount by remember {
        mutableIntStateOf(InterfaceStyleRegistry(context).list().size)
    }
    var busy by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<PendingThemeStoreImport?>(null) }
    var transferReport by remember { mutableStateOf<ThemeStoreTransferReport?>(null) }
    var selectedPageIndex by rememberSaveable(page) { mutableIntStateOf(page.ordinal) }
    val pageStateHolder = rememberSaveableStateHolder()
    val selectedPage = ThemeStorePage.entries.getOrElse(selectedPageIndex) {
        ThemeStorePage.Overview
    }

    fun refresh() {
        summary = readThemeStoreSummary(context)
        downloadedStyleCount = InterfaceStyleRegistry(context).list().size
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
        onOpenInterfaceStyles = dropUnlessResumed { navigator.push(Route.InterfaceStyleStore) },
        onOpenBackgroundSettings = dropUnlessResumed { navigator.push(Route.Backgrounds) },
        onOpenSoundEffects = dropUnlessResumed { navigator.push(Route.SoundEffects) },
        onOpenStartupAnimation = dropUnlessResumed { navigator.push(Route.StartupAnimation) },
        onExport = {
            transferReport = null
            exportLauncher.launch("apkesu-theme.$THEME_STORE_FILE_EXTENSION")
        },
        onImport = {
            transferReport = null
            importLauncher.launch(
                arrayOf(THEME_STORE_FILE_MIME_TYPE, "application/octet-stream", "*/*")
            )
        },
    )

    val content: @Composable () -> Unit = {
        pageStateHolder.SaveableStateProvider(selectedPage.name) {
            when (selectedPage) {
                ThemeStorePage.Overview -> ThemeStoreOverviewContent(
                    actions = actions,
                    modifier = Modifier.fillMaxSize(),
                )

                ThemeStorePage.Customize -> ThemeStoreCustomizeContent(
                    summary = summary,
                    actions = actions,
                    initialSection = customizeSection,
                    onInterfaceStyleInstalled = { id ->
                        downloadedStyleCount = InterfaceStyleRegistry(context).list().size
                        if (returnToAppearance) {
                            navigator.setResult(INTERFACE_STYLE_RESULT_KEY, id)
                        }
                    },
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 840.dp)
                        .fillMaxWidth(),
                )

                ThemeStorePage.My -> ThemeStoreLibraryScreen(
                    embedded = true,
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 840.dp)
                        .fillMaxWidth(),
                    headerContent = { libraryBusy ->
                        ThemeStoreCurrentThemeStatus(summary)
                        ThemeStoreDestinationItem(
                            title = stringResource(R.string.interface_style_store_title),
                            summary = stringResource(R.string.interface_style_store_summary),
                            status = stringResource(
                                R.string.theme_store_downloaded_style_count,
                                downloadedStyleCount,
                            ),
                            icon = Icons.Rounded.Palette,
                            onClick = actions.onOpenInterfaceStyles,
                        )
                        ThemeStoreTransferPanel(summary, busy || libraryBusy, actions)
                        ThemeStoreNotice(stringResource(R.string.theme_store_import_replaces_notice))
                        transferReport?.let { ThemeStoreTransferReportCard(it) }
                    },
                )
            }
        }
    }

    if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) {
        SkrootproScreen(
            title = stringResource(selectedPage.titleRes),
            showAdd = selectedPage == ThemeStorePage.My,
            actionIcon = Icons.Rounded.Person,
            actionContentDescription = stringResource(R.string.theme_store_my_title),
            onAddClick = actions.onOpenProfile,
            bottomInnerPadding = 0.dp,
        ) { paddingValues ->
            ThemeStoreResponsiveLayout(
                selectedPage = selectedPage,
                onSelected = {
                    if (it == ThemeStorePage.My) refresh()
                    selectedPageIndex = it.ordinal
                },
                showBackButton = true,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                onBack = actions.onBack,
                content = content,
            )
        }
    } else {
        MiuixScaffold(
            containerColor = Color.Transparent,
            popupHost = { },
            contentWindowInsets = WindowInsets.systemBars
                .add(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Horizontal),
            topBar = {
                MiuixTopAppBar(
                    title = stringResource(selectedPage.titleRes),
                    color = Color.Transparent,
                    titleColor = themeStoreTextColor(),
                    navigationIcon = {
                        MiuixIconButton(onClick = actions.onBack) {
                            MiuixIcon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = themeStoreTextColor(),
                            )
                        }
                    },
                    actions = {
                        if (selectedPage == ThemeStorePage.My) {
                            MiuixIconButton(onClick = actions.onOpenProfile) {
                                MiuixIcon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = stringResource(R.string.theme_store_my_title),
                                    tint = themeStoreTextColor(),
                                )
                            }
                        }
                    },
                )
            },
            content = { paddingValues ->
                ThemeStoreResponsiveLayout(
                    selectedPage = selectedPage,
                    onSelected = {
                        if (it == ThemeStorePage.My) refresh()
                        selectedPageIndex = it.ordinal
                    },
                    showBackButton = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    onBack = actions.onBack,
                    content = content,
                )
            },
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
    showBackButton: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 720.dp
        if (useNavigationRail) {
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
private fun ThemeStoreNavigationBar(
    selectedPage: ThemeStorePage,
    onSelected: (ThemeStorePage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = themeStorePalette()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(palette.navigationSurface)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThemeStorePage.entries.forEach { destination ->
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
private fun ThemeStoreNavigationRail(
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
        ThemeStorePage.entries.forEach { destination ->
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
    ThemeStorePage.Overview -> Icons.Rounded.Explore
    ThemeStorePage.Customize -> Icons.Rounded.AutoFixHigh
    ThemeStorePage.My -> Icons.Rounded.SaveAlt
}

@Composable
private fun themeStorePageLabel(page: ThemeStorePage): String = stringResource(
    when (page) {
        ThemeStorePage.Overview -> R.string.cloud_theme_tab_discover
        ThemeStorePage.Customize -> R.string.theme_store_tab_customize
        ThemeStorePage.My -> R.string.theme_store_my_library_title
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
    onInterfaceStyleInstalled: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedSection by rememberSaveable(initialSection) {
        mutableStateOf(initialSection)
    }
    ThemeStorePageColumn(modifier) {
        ThemeStoreCurrentThemeStatus(summary)
        ThemeStoreCustomizeSelector(
            selected = selectedSection,
            onSelected = { selectedSection = it },
        )
        when (selectedSection) {
            ThemeStoreCustomizeSection.Style -> ThemeStoreStyleItems(
                summary = summary,
                actions = actions,
                onInterfaceStyleInstalled = onInterfaceStyleInstalled,
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
    val palette = themeStorePalette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.surface.copy(alpha = 0.68f))
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThemeStoreCustomizeSection.entries.forEach { section ->
            val isSelected = selected == section
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .selectable(
                        selected = isSelected,
                        role = Role.Tab,
                        onClick = { onSelected(section) },
                    )
                    .padding(4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) palette.accentContainer else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(section.titleRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) palette.text else palette.mutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
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
    onInterfaceStyleInstalled: (String) -> Unit,
) {
    ThemeStoreSectionHeader(stringResource(R.string.interface_style_store_title))
    InterfaceStyleStoreContent(onInstalled = onInterfaceStyleInstalled)
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
    onInstalled: (String) -> Unit,
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

    fun loadCatalog(force: Boolean) {
        scope.launch {
            snapshot = withContext(Dispatchers.IO) { repository.fetch(forceNetwork = force) }
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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ThemeStoreNotice(stringResource(R.string.interface_style_store_notice))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            InterfaceStyleProxyMode.entries.forEach { mode ->
                FilterChip(
                    selected = preferences.mode == mode,
                    onClick = {
                        preferences = preferences.copy(mode = mode)
                        saveInterfaceStyleDownloadPreferences(context, preferences)
                    },
                    label = {
                        Text(
                            when (mode) {
                                InterfaceStyleProxyMode.Direct -> stringResource(R.string.interface_style_proxy_direct)
                                InterfaceStyleProxyMode.Auto -> stringResource(R.string.interface_style_proxy_auto)
                                InterfaceStyleProxyMode.Custom -> stringResource(R.string.interface_style_proxy_custom)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        if (preferences.mode == InterfaceStyleProxyMode.Custom) {
            OutlinedTextField(
                value = customProxy,
                onValueChange = { customProxy = it.take(240) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.interface_style_proxy_custom_hint)) },
                supportingText = { Text(stringResource(R.string.interface_style_proxy_custom_summary)) },
                trailingIcon = {
                    TextButton(onClick = {
                        preferences = preferences.copy(customProxy = customProxy.trim())
                        saveInterfaceStyleDownloadPreferences(context, preferences)
                    }) { Text(stringResource(R.string.interface_style_proxy_save)) }
                },
            )
        }
        snapshot?.let { current ->
            if (current.errorMessage != null) {
                ThemeStoreNotice(stringResource(R.string.interface_style_catalog_offline, current.errorMessage))
            }
            if (current.catalog.styles.isEmpty()) {
                ThemeStoreNotice(stringResource(R.string.interface_style_catalog_empty))
            }
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val gap = 12.dp
                val columns = when {
                    maxWidth >= 1000.dp -> 3
                    maxWidth >= 620.dp -> 2
                    else -> 1
                }
                val itemWidth = (maxWidth - gap * (columns - 1)) / columns
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalArrangement = Arrangement.spacedBy(gap),
                    maxItemsInEachRow = columns,
                ) {
                    current.catalog.styles.forEach { style ->
                        val installedStyle = installed.firstOrNull { it.style.id == style.id }
                        val active = settingsRepository.uiMode == style.engine &&
                            when (style.engine) {
                                InterfaceStyle.Snow.value -> settingsRepository.seasonStyle == style.variant
                                InterfaceStyle.Rain.value -> settingsRepository.rainStyle == style.variant
                                InterfaceStyle.Pixel.value -> settingsRepository.pixelStyle == style.variant
                                else -> true
                            }
                        InterfaceStylePackageRow(
                            style = style,
                            installed = installedStyle,
                            active = active,
                            progress = if (busyId == style.id) progress else null,
                            busy = busyId != null,
                            modifier = Modifier.width(itemWidth),
                            onDownload = { runInstall(style, applyAfter = false) },
                            onApply = {
                                if (installedStyle != null) applyInstalled(style)
                                else runInstall(style, applyAfter = true)
                            },
                            onRemove = {
                                if (active) {
                                    settingsRepository.applyInterfaceStyle(
                                        UiMode.DEFAULT_VALUE,
                                        ThemePreset.CLEAN_TOOL,
                                        settingsRepository.themeMode,
                                    )
                                }
                                registry.remove(style.id)
                                installed = registry.list()
                            },
                        )
                    }
                }
            }
        } ?: run {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            ThemeStoreNotice(stringResource(R.string.interface_style_catalog_loading))
        }
        error?.let { ThemeStoreNotice(stringResource(R.string.interface_style_operation_failed, it)) }
        if (snapshot != null) {
            OutlinedButton(
                onClick = { loadCatalog(force = true) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                enabled = busyId == null,
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.interface_style_catalog_refresh))
            }
        }
    }
}

@Composable
private fun InterfaceStylePackageRow(
    style: InterfaceStylePackage,
    installed: InstalledInterfaceStyle?,
    active: Boolean,
    progress: InterfaceStyleDownloadProgress?,
    busy: Boolean,
    modifier: Modifier = Modifier,
    onDownload: () -> Unit,
    onApply: () -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    ThemeStoreSurface(modifier = modifier) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                        .background(Color(style.accent.toInt())),
                )
                Column(Modifier.weight(1f)) {
                    Text(style.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = themeStoreTextColor())
                    Text(style.summary.ifBlank { style.engine }, style = MaterialTheme.typography.bodySmall, color = themeStoreMutedColor(), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (installed != null) {
                    Text(
                        text = stringResource(
                            if (active) R.string.theme_store_applied else R.string.interface_style_installed,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = themeStorePalette().accent,
                    )
                }
            }
            Text(
                text = stringResource(
                    R.string.interface_style_package_meta,
                    style.engine,
                    style.version,
                    Formatter.formatShortFileSize(context, style.sizeBytes),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = themeStoreMutedColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            progress?.let {
                LinearProgressIndicator(progress = { it.fraction ?: 0f }, modifier = Modifier.fillMaxWidth())
                Text(
                    text = stringResource(
                        R.string.interface_style_download_progress,
                        ((it.fraction ?: 0f) * 100).toInt(),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = themeStoreMutedColor(),
                )
            }
            FilledTonalButton(
                onClick = onApply,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Icon(
                    imageVector = if (installed == null) Icons.Rounded.Download else Icons.Rounded.CheckCircle,
                    contentDescription = null,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(
                        if (installed == null) R.string.interface_style_download_apply
                        else R.string.interface_style_apply,
                    )
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDownload, enabled = !busy) {
                    Icon(Icons.Rounded.Download, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(
                            if (installed == null) R.string.interface_style_download
                            else R.string.interface_style_update,
                        )
                    )
                }
                if (installed != null) {
                    IconButton(onClick = onRemove, enabled = !busy, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.interface_style_remove),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeStorePageColumn(
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
            .clickable(onClick = onClick)
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
private fun ThemeStoreTransferPanel(
    summary: ThemeStoreSummary,
    busy: Boolean,
    actions: ThemeStoreActions,
) {
    val palette = themeStorePalette()
    ThemeStoreSurface {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(palette.accentContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = palette.accent,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.SaveAlt,
                            contentDescription = null,
                            tint = palette.accent,
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.theme_store_transfer_panel_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = themeStoreTextColor(),
                    )
                    Text(
                        text = stringResource(R.string.theme_store_selected_count, summary.selectedCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = themeStoreMutedColor(),
                    )
                }
            }
            Text(
                text = stringResource(R.string.theme_store_transfer_panel_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = themeStoreMutedColor(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    onClick = actions.onExport,
                ) {
                    Text(
                        text = stringResource(R.string.theme_store_export),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    onClick = actions.onImport,
                ) {
                    Text(
                        text = stringResource(R.string.theme_store_import),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
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
    Box(
        modifier = Modifier
            .padding(start = 16.dp, top = 14.dp)
            .size(48.dp)
            .clip(CircleShape)
            .background(palette.navigationSurface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
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
    content: @Composable () -> Unit,
) {
    val palette = themeStorePalette()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.surface),
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
    if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) {
        return ThemeStorePalette(
            surface = SkrootproColors.BarSurface,
            navigationSurface = SkrootproColors.BarSurface,
            text = SkrootproColors.Text,
            mutedText = SkrootproColors.Muted,
            accent = SkrootproColors.Purple,
            accentContainer = SkrootproColors.Purple.copy(alpha = 0.18f),
        )
    }

    val dark = isInDarkTheme()
    val materialColors = MaterialTheme.colorScheme
    val requestedAccent = materialColors.primary
    val accent = if (dark && requestedAccent.luminance() < 0.34f) {
        Color(0xFFC7CCFF)
    } else {
        requestedAccent
    }
    return if (dark) {
        ThemeStorePalette(
            surface = Color(0xDE191C23),
            navigationSurface = Color(0xF0181A20),
            text = Color(0xFFF4F5FA),
            mutedText = Color(0xFFC3C7D2),
            accent = accent,
            accentContainer = accent.copy(alpha = 0.20f),
        )
    } else {
        ThemeStorePalette(
            surface = colorScheme.surfaceContainer,
            navigationSurface = colorScheme.surface,
            text = materialColors.onSurface,
            mutedText = materialColors.onSurfaceVariant,
            accent = accent,
            accentContainer = accent.copy(alpha = 0.11f),
        )
    }
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
