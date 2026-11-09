package me.weishu.kernelsu.ui.screen.module

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import me.weishu.kernelsu.ui.component.passthrough.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import me.weishu.kernelsu.ui.component.passthrough.MaterialIcon as Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import me.weishu.kernelsu.ui.component.passthrough.MaterialText as Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.model.Module
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.screen.settings.SettingsWallpaperCropDialog
import me.weishu.kernelsu.ui.screen.settings.AppearanceEditorWorkspace
import me.weishu.kernelsu.ui.component.miuix.SunMoonSwitchPreference
import me.weishu.kernelsu.ui.theme.LocalUiPassthroughSettings
import me.weishu.kernelsu.ui.theme.UiPassthroughTheme
import me.weishu.kernelsu.ui.util.UiPassthroughSettings
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor
import me.weishu.kernelsu.ui.theme.immersivePageColor
import me.weishu.kernelsu.ui.theme.withUiPassthrough
import me.weishu.kernelsu.ui.util.MediaVisualSettings
import me.weishu.kernelsu.ui.viewmodel.ModuleViewModel
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.math.roundToInt

private enum class WallpaperImportMode { Add, Replace }
private enum class WallpaperBatchMode { Selected, SameType, All }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleWallpaperEditorScreen(
    moduleId: String,
    displayName: String? = null,
    displayAuthor: String? = null,
    displayVersion: String? = null,
    displayDescription: String? = null,
    allowBatch: Boolean = true,
) {
    val navigator = LocalNavigator.current
    ModuleWallpaperEditorTheme {
        ModuleWallpaperEditorContent(
            moduleId = moduleId,
            displayName = displayName,
            displayAuthor = displayAuthor,
            displayVersion = displayVersion,
            displayDescription = displayDescription,
            allowBatch = allowBatch,
            onClose = navigator::pop,
            inDialog = false,
        )
    }
}

@Composable
fun ModuleWallpaperEditorDialog(
    request: Route.ModuleWallpaperEditor,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            val dialogWidth = maxWidth.coerceAtMost(960.dp)
            val dialogHeight = maxHeight.coerceAtMost(720.dp)
            Surface(
                modifier = Modifier.width(dialogWidth).height(dialogHeight),
                shape = RoundedCornerShape(24.dp),
                // A modal needs its own readable surface even over a fully transparent page.
                color = MaterialTheme.colorScheme.surface.copy(alpha = 1f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp,
            ) {
                ModuleWallpaperEditorTheme {
                    ModuleWallpaperEditorContent(
                        moduleId = request.moduleId,
                        displayName = request.displayName,
                        displayAuthor = request.displayAuthor,
                        displayVersion = request.displayVersion,
                        displayDescription = request.displayDescription,
                        allowBatch = request.allowBatch,
                        onClose = onDismissRequest,
                        inDialog = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleWallpaperEditorTheme(content: @Composable () -> Unit) {
    if (LocalUiMode.current == UiMode.Material) {
        val dark = isInDarkTheme()
        val accent = MaterialTheme.colorScheme.primary
        val controller = remember(dark, accent) {
            ThemeController(
                if (dark) ColorSchemeMode.Dark else ColorSchemeMode.Light,
                keyColor = accent,
                isDark = dark,
            )
        }
        MiuixTheme(controller = controller) {
            // Material/sidebar styles still need a matching palette for native Miuix controls.
            MiuixTheme(colors = MiuixTheme.colorScheme.withUiPassthrough(LocalUiPassthroughSettings.current)) {
                content()
            }
        }
    } else {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModuleWallpaperEditorContent(
    moduleId: String,
    displayName: String?,
    displayAuthor: String?,
    displayVersion: String?,
    displayDescription: String?,
    allowBatch: Boolean,
    onClose: () -> Unit,
    inDialog: Boolean,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val moduleViewModel = viewModel<ModuleViewModel>()
    val moduleUiState by moduleViewModel.uiState.collectAsStateWithLifecycle()
    val isKpmWallpaper = moduleId.startsWith(KPM_CARD_WALLPAPER_ID_PREFIX)
    val module = moduleUiState.moduleList.firstOrNull { it.id == moduleId }
    var snapshot by remember(moduleId) { mutableStateOf(readModuleCardWallpaperSnapshot(context, moduleId)) }
    val darkTheme = isInDarkTheme()
    var variant by rememberSaveable(moduleId) { mutableStateOf(activeModuleWallpaperVariant(darkTheme, snapshot)) }
    var selectedIndex by rememberSaveable(moduleId) { mutableIntStateOf(snapshot.collection(variant).selectedIndex) }
    var previewAspectRatio by rememberSaveable(moduleId) { mutableFloatStateOf(MODULE_CARD_WALLPAPER_ASPECT_RATIO) }
    var importMode by rememberSaveable(moduleId) { mutableStateOf(WallpaperImportMode.Add) }
    var busy by remember { mutableStateOf(false) }
    var showCrop by remember { mutableStateOf(false) }
    var showBatch by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val collection = snapshot.collection(variant)
    val selectedEntry = collection.entries.getOrNull(
        selectedIndex.coerceIn(0, collection.entries.lastIndex.coerceAtLeast(0))
    )
    val previewState = rememberModuleCardWallpaperLoadState(selectedEntry)

    val prefs = remember(context) { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    DisposableEffect(prefs, moduleId, variant) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == moduleWallpaperEntriesKey(moduleId)) {
                scope.launch {
                    snapshot = readModuleCardWallpaperSnapshot(prefs, moduleId)
                    selectedIndex = snapshot.collection(variant).selectedIndex
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun commit(transform: (ModuleCardWallpaperSnapshot) -> ModuleCardWallpaperSnapshot): Boolean {
        val next = updateModuleCardWallpaperSnapshot(context, moduleId, transform)
        if (next == null) {
            errorText = resources.getString(R.string.module_wallpaper_save_failed)
            return false
        }
        snapshot = next
        selectedIndex = next.collection(variant).selectedIndex
        return true
    }

    fun updateCollection(transform: (ModuleWallpaperCollection) -> ModuleWallpaperCollection) {
        commit { latest -> latest.withCollection(variant, transform(latest.collection(variant))) }
    }

    fun updateSelectedEntry(
        persist: Boolean = true,
        transform: (ModuleCardWallpaperEntry) -> ModuleCardWallpaperEntry,
    ) {
        val uri = selectedEntry?.uriString ?: return
        if (persist) {
            if (!commit { latest -> updateModuleWallpaperEntry(latest, variant, uri, transform) }) {
                snapshot = readModuleCardWallpaperSnapshot(context, moduleId)
            }
        } else {
            snapshot = updateModuleWallpaperEntry(snapshot, variant, uri, transform)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val importVariant = variant
        val append = importMode == WallpaperImportMode.Add
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                importAndSaveModuleWallpapers(context, moduleId, importVariant, uris, append)
            }
            result.onSuccess { next ->
                snapshot = next
                selectedIndex = next.collection(variant).selectedIndex
            }.onFailure { error ->
                errorText = if (error is ModuleWallpaperLimitException) {
                    resources.getString(R.string.module_wallpaper_too_many, MODULE_CARD_WALLPAPER_MAX_COUNT)
                } else {
                    resources.getString(R.string.module_wallpaper_import_failed)
                }
            }
            busy = false
        }
    }

    LaunchedEffect(Unit) {
        if (!isKpmWallpaper) {
            moduleViewModel.initializePreferences()
            moduleViewModel.fetchModuleList(checkUpdate = false)
        }
    }
    LaunchedEffect(variant, collection.entries.size) {
        selectedIndex = collection.selectedIndex.coerceIn(0, collection.entries.lastIndex.coerceAtLeast(0))
    }

    MiuixScaffold(
        containerColor = if (inDialog) Color.Transparent else immersivePageColor(MiuixTheme.colorScheme.background),
        contentWindowInsets = if (inDialog) WindowInsets(0, 0, 0, 0) else WindowInsets.safeDrawing,
        popupHost = {},
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.module_wallpaper_editor_title),
                color = Color.Transparent,
                titleColor = MaterialTheme.colorScheme.onSurface,
                defaultWindowInsetsPadding = false,
                navigationIcon = {
                    MiuixIconButton(onClick = onClose) {
                        Icon(
                            if (inDialog) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                            stringResource(if (inDialog) R.string.close else R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            val previewHeight = (maxHeight * 0.42f).coerceIn(128.dp, 240.dp)
            AppearanceEditorWorkspace(
                modifier = Modifier.fillMaxSize(),
                selector = {
                    VariantSelector(
                        variant = variant,
                        onVariantChange = { variant = it },
                    )
                },
                preview = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ModuleWallpaperRealPreview(
                            module = module,
                            displayName = displayName,
                            displayAuthor = displayAuthor,
                            displayVersion = displayVersion,
                            displayDescription = displayDescription,
                            entry = selectedEntry,
                            loadState = previewState,
                            aspectRatio = previewAspectRatio,
                            variant = variant,
                            maxHeight = previewHeight,
                        )
                        PreviewRatioSelector(
                            selected = previewAspectRatio,
                            onSelected = { previewAspectRatio = it },
                        )
                    }
                },
                controls = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (variant == ModuleWallpaperVariant.Night && collection.entries.isEmpty() && snapshot.entries.isNotEmpty()) {
                            MiuixTextButton(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(R.string.module_wallpaper_copy_day_to_night),
                                enabled = !busy,
                                onClick = {
                                    scope.launch {
                                        busy = true
                                        val copied = withContext(Dispatchers.IO) {
                                            duplicateModuleWallpaperSnapshotForTarget(
                                                context,
                                                "${moduleId}_night_seed",
                                                ModuleCardWallpaperSnapshot(snapshot.entries, snapshot.carouselEnabled),
                                            )
                                        }
                                        copied.onSuccess { duplicate ->
                                            if (!commit { latest ->
                                                latest.withCollection(
                                                    ModuleWallpaperVariant.Night,
                                                    duplicate.collection(ModuleWallpaperVariant.Day),
                                                )
                                            }) {
                                                releaseModuleWallpaperSnapshot(context, duplicate)
                                            }
                                        }.onFailure { errorText = resources.getString(R.string.module_wallpaper_save_failed) }
                                        busy = false
                                    }
                                },
                            )
                        }
                        WallpaperEditorCard {
                            WallpaperQueue(
                                entries = collection.entries,
                                selectedIndex = selectedIndex,
                                enabled = !busy,
                                onSelect = { index ->
                                    val uri = collection.entries.getOrNull(index)?.uriString ?: return@WallpaperQueue
                                    updateCollection { current ->
                                        val selected = current.entries.indexOfFirst { it.uriString == uri }
                                        if (selected < 0) current else current.copy(selectedIndex = selected)
                                    }
                                },
                                onMove = { from, to ->
                                    val movedUri = collection.entries.getOrNull(from)?.uriString
                                    val targetUri = collection.entries.getOrNull(to)?.uriString
                                    if (from == to || movedUri == null || targetUri == null) return@WallpaperQueue
                                    updateCollection { current ->
                                        val entries = current.entries.toMutableList()
                                        val source = entries.indexOfFirst { it.uriString == movedUri }
                                        val target = entries.indexOfFirst { it.uriString == targetUri }
                                        if (source < 0 || target < 0) current else {
                                            val selectedUri = current.entries.getOrNull(current.selectedIndex)?.uriString
                                            entries.add(target, entries.removeAt(source))
                                            current.copy(
                                                entries = entries,
                                                selectedIndex = entries.indexOfFirst { it.uriString == selectedUri }.coerceAtLeast(0),
                                            )
                                        }
                                    }
                                },
                                onDelete = { index ->
                                    val uri = collection.entries.getOrNull(index)?.uriString
                                    updateCollection {
                                        val nextEntries = it.entries.filterNot { entry -> entry.uriString == uri }
                                        it.copy(
                                            entries = nextEntries,
                                            selectedIndex = selectedIndex.coerceAtMost(nextEntries.lastIndex.coerceAtLeast(0)),
                                            carouselEnabled = it.carouselEnabled && nextEntries.size > 1,
                                        )
                                    }
                                },
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                MiuixTextButton(
                                    modifier = Modifier.widthIn(min = 100.dp).weight(1f),
                                    text = stringResource(R.string.module_wallpaper_add),
                                    colors = ButtonDefaults.textButtonColorsPrimary(),
                                    enabled = !busy && collection.entries.size < MODULE_CARD_WALLPAPER_MAX_COUNT,
                                    onClick = {
                                        importMode = WallpaperImportMode.Add
                                        imagePicker.launch(arrayOf("image/*"))
                                    },
                                )
                                MiuixTextButton(
                                    modifier = Modifier.widthIn(min = 100.dp).weight(1f),
                                    text = stringResource(R.string.module_wallpaper_replace),
                                    enabled = !busy,
                                    onClick = {
                                        importMode = WallpaperImportMode.Replace
                                        imagePicker.launch(arrayOf("image/*"))
                                    },
                                )
                                MiuixTextButton(
                                    modifier = Modifier.widthIn(min = 100.dp).weight(1f),
                                    text = stringResource(R.string.module_wallpaper_crop),
                                    enabled = selectedEntry != null && !busy,
                                    onClick = { showCrop = true },
                                )
                            }
                        }
                        selectedEntry?.let { entry ->
                            VisualSettingsEditor(
                                settings = entry.visualSettings,
                                autoContrast = entry.autoContrast,
                                onSettingsPreview = { next -> updateSelectedEntry(persist = false) { it.copy(visualSettings = next) } },
                                onSettingsCommit = { next -> updateSelectedEntry { it.copy(visualSettings = next) } },
                                onAutoContrastChange = { enabled -> updateSelectedEntry { it.copy(autoContrast = enabled) } },
                            )
                        }
                        if (collection.entries.size > 1) {
                            CarouselSettings(
                                collection = collection,
                                onChange = { next ->
                                    updateCollection { current ->
                                        current.copy(
                                            carouselEnabled = next.carouselEnabled,
                                            carouselOrder = next.carouselOrder,
                                            intervalMillis = next.intervalMillis,
                                        )
                                    }
                                },
                            )
                        }
                        if (allowBatch) {
                            MiuixTextButton(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(R.string.module_wallpaper_batch_apply),
                                enabled = snapshot.allEntries().isNotEmpty() && moduleUiState.moduleList.size > 1 && !busy,
                                onClick = { showBatch = true },
                            )
                        }
                        if (busy) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(stringResource(R.string.module_wallpaper_processing))
                            }
                        }
                    }
                },
            )
        }
    }

    SettingsWallpaperCropDialog(
        show = showCrop && selectedEntry != null,
        uriString = selectedEntry?.uriString,
        crop = selectedEntry?.crop ?: me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_WALLPAPER_CROP,
        onCropChange = { crop -> updateSelectedEntry { it.copy(crop = crop) } },
        onDismissRequest = { showCrop = false },
        title = stringResource(R.string.module_wallpaper_crop),
        editorAspectRatio = previewAspectRatio,
        cropAspectRatio = previewAspectRatio,
    )
    if (showBatch) {
        ModuleWallpaperBatchDialog(
            sourceModule = module,
            modules = moduleUiState.moduleList,
            busy = busy,
            onDismiss = { showBatch = false },
            onApply = { targets ->
                showBatch = false
                busy = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        runCatching {
                            val replacements = linkedMapOf<String, ModuleCardWallpaperSnapshot>()
                            try {
                                targets.forEach { target ->
                                    replacements[target.id] = duplicateModuleWallpaperSnapshotForTarget(
                                        context,
                                        target.id,
                                        snapshot,
                                    ).getOrThrow()
                                }
                                if (!replaceModuleCardWallpaperSnapshots(context, replacements)) {
                                    error("Unable to save batch wallpaper settings")
                                }
                            } catch (error: Throwable) {
                                replacements.values.forEach { releaseModuleWallpaperSnapshot(context, it) }
                                throw error
                            }
                        }
                    }
                    result.onSuccess {
                        Toast.makeText(context, R.string.module_wallpaper_batch_success, Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        errorText = resources.getString(R.string.module_wallpaper_batch_failed)
                    }
                    busy = false
                }
            },
        )
    }
    UiPassthroughTheme(UiPassthroughSettings()) {
        errorText?.let { message ->
            AlertDialog(
                onDismissRequest = { errorText = null },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 1f),
                title = { Text(stringResource(R.string.module_wallpaper_error_title)) },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { errorText = null }) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
            )
        }
    }
}

@Composable
private fun WallpaperEditorCard(content: @Composable ColumnScope.() -> Unit) {
    MiuixCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        colors = MiuixCardDefaults.defaultColors(
            color = immersiveSurfaceColor(MiuixTheme.colorScheme.surfaceContainer),
        ),
        insideMargin = PaddingValues(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun WallpaperChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    MiuixTextButton(
        text = label,
        onClick = onClick,
        modifier = modifier
            .widthIn(min = 84.dp * fontScale)
            .heightIn(min = 48.dp)
            .semantics { this.selected = selected },
        colors = if (selected) ButtonDefaults.textButtonColorsPrimary() else ButtonDefaults.textButtonColors(),
    )
}

@Composable
private fun VariantSelector(
    variant: ModuleWallpaperVariant,
    onVariantChange: (ModuleWallpaperVariant) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WallpaperChoice(
            modifier = Modifier.widthIn(min = 100.dp).weight(1f),
            selected = variant == ModuleWallpaperVariant.Day,
            onClick = { onVariantChange(ModuleWallpaperVariant.Day) },
            label = stringResource(R.string.module_wallpaper_day),
        )
        WallpaperChoice(
            modifier = Modifier.widthIn(min = 100.dp).weight(1f),
            selected = variant == ModuleWallpaperVariant.Night,
            onClick = { onVariantChange(ModuleWallpaperVariant.Night) },
            label = stringResource(R.string.module_wallpaper_night),
        )
    }
}

@Composable
private fun PreviewRatioSelector(selected: Float, onSelected: (Float) -> Unit) {
    val options = listOf(
        2.4f to R.string.module_wallpaper_preview_compact,
        MODULE_CARD_WALLPAPER_ASPECT_RATIO to R.string.module_wallpaper_preview_standard,
        1.25f to R.string.module_wallpaper_preview_expanded,
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (ratio, label) ->
            WallpaperChoice(
                modifier = Modifier.widthIn(min = 84.dp).weight(1f),
                selected = selected == ratio,
                onClick = { onSelected(ratio) },
                label = stringResource(label),
            )
        }
    }
}

@Composable
private fun ModuleWallpaperRealPreview(
    module: Module?,
    displayName: String?,
    displayAuthor: String?,
    displayVersion: String?,
    displayDescription: String?,
    entry: ModuleCardWallpaperEntry?,
    loadState: ModuleWallpaperBitmapLoadState,
    aspectRatio: Float,
    variant: ModuleWallpaperVariant,
    maxHeight: Dp,
) {
    val night = variant == ModuleWallpaperVariant.Night
    val surface = if (night) Color(0xFF191A1D) else Color(0xFFF5F6F8)
    val foreground = if (night) Color(0xFFF3F4F6) else Color(0xFF191A1D)
    val secondary = if (night) Color(0xFFCACDD4) else Color(0xFF555B65)
    val fontScale = LocalDensity.current.fontScale
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = maxHeight * aspectRatio)
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(18.dp))
                .background(surface),
        ) {
            ModuleCardWallpaperBackground(
                bitmap = loadState.bitmap,
                entry = entry,
                contentIsLight = night,
            )
            if (loadState.loading) CircularProgressIndicator(Modifier.align(Alignment.Center).size(26.dp))
            if (loadState.failed) {
                Text(
                    text = stringResource(R.string.module_wallpaper_load_failed),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = module?.name ?: displayName ?: stringResource(R.string.module_wallpaper_preview_module),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = foreground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (this@BoxWithConstraints.maxHeight >= 80.dp * fontScale) Text(
                        text = module?.let { "${it.version} · ${it.author}" }
                            ?: listOfNotNull(displayVersion, displayAuthor).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (this@BoxWithConstraints.maxHeight >= 128.dp * fontScale) Text(
                    text = module?.description ?: displayDescription.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = foreground,
                    maxLines = if (aspectRatio < 1.5f) 3 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun WallpaperQueue(
    entries: List<ModuleCardWallpaperEntry>,
    selectedIndex: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDelete: (Int) -> Unit,
) {
    if (entries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.module_wallpaper_empty_queue),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }
    val dragThreshold = with(LocalDensity.current) { 56.dp.toPx() }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(entries, key = { _, entry -> entry.uriString }) { index, entry ->
            val state = rememberModuleCardWallpaperLoadState(entry, maxSide = 260)
            var dragDistance by remember(entry.uriString) { mutableFloatStateOf(0f) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(104.dp)
                        .aspectRatio(MODULE_CARD_WALLPAPER_ASPECT_RATIO)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(
                            width = if (index == selectedIndex) 2.dp else 1.dp,
                            color = if (index == selectedIndex) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = RoundedCornerShape(8.dp),
                        )
                        .clickable(enabled = enabled) { onSelect(index) }
                        .pointerInput(enabled, index, entries.size) {
                            if (!enabled) return@pointerInput
                            detectDragGesturesAfterLongPress(
                                onDragEnd = { dragDistance = 0f },
                                onDragCancel = { dragDistance = 0f },
                            ) { change, amount ->
                                change.consume()
                                dragDistance += amount.x
                                when {
                                    dragDistance > dragThreshold && index < entries.lastIndex -> {
                                        onMove(index, index + 1)
                                        dragDistance = 0f
                                    }
                                    dragDistance < -dragThreshold && index > 0 -> {
                                        onMove(index, index - 1)
                                        dragDistance = 0f
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    state.bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = stringResource(R.string.module_wallpaper_thumbnail, index + 1),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } ?: Icon(Icons.Rounded.Image, null)
                    Icon(
                        Icons.Rounded.DragIndicator,
                        contentDescription = stringResource(R.string.module_wallpaper_drag_reorder),
                        modifier = Modifier.align(Alignment.BottomStart).padding(4.dp).size(24.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                                RoundedCornerShape(6.dp),
                            ).padding(2.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(
                        enabled = enabled,
                        onClick = { onDelete(index) },
                        modifier = Modifier.align(Alignment.TopEnd).size(48.dp),
                    ) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            stringResource(R.string.module_wallpaper_delete_one),
                            modifier = Modifier.size(32.dp).background(
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                                CircleShape,
                            ).padding(6.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Text(
                    text = "${index + 1}/${entries.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun VisualSettingsEditor(
    settings: MediaVisualSettings,
    autoContrast: Boolean,
    onSettingsPreview: (MediaVisualSettings) -> Unit,
    onSettingsCommit: (MediaVisualSettings) -> Unit,
    onAutoContrastChange: (Boolean) -> Unit,
) {
    WallpaperEditorCard {
        Text(stringResource(R.string.module_wallpaper_visual_title), style = MaterialTheme.typography.titleMedium)
        SettingsSlider(
            label = stringResource(R.string.module_wallpaper_brightness),
            value = settings.brightness,
            range = -0.6f..0.6f,
            onPreview = { onSettingsPreview(settings.copy(brightness = it)) },
            onCommit = { onSettingsCommit(settings.copy(brightness = it)) },
        )
        SettingsSlider(
            label = stringResource(R.string.module_wallpaper_dark_overlay),
            value = settings.overlayAlpha,
            range = 0f..0.82f,
            onPreview = { onSettingsPreview(settings.copy(overlayAlpha = it)) },
            onCommit = { onSettingsCommit(settings.copy(overlayAlpha = it)) },
        )
        SettingsSlider(
            label = stringResource(R.string.module_wallpaper_blur),
            value = settings.blurRadius,
            range = 0f..28f,
            onPreview = { onSettingsPreview(settings.copy(blurRadius = it)) },
            onCommit = { onSettingsCommit(settings.copy(blurRadius = it)) },
        )
        SettingsSlider(
            label = stringResource(R.string.module_wallpaper_saturation),
            value = settings.saturation,
            range = 0f..2f,
            onPreview = { onSettingsPreview(settings.copy(saturation = it)) },
            onCommit = { onSettingsCommit(settings.copy(saturation = it)) },
        )
        SettingsSlider(
            label = stringResource(R.string.module_wallpaper_contrast),
            value = settings.contrast,
            range = 0.5f..1.8f,
            onPreview = { onSettingsPreview(settings.copy(contrast = it)) },
            onCommit = { onSettingsCommit(settings.copy(contrast = it)) },
        )
        SettingsSlider(
            label = stringResource(R.string.module_wallpaper_opacity),
            value = settings.opacity,
            range = 0.1f..1f,
            onPreview = { onSettingsPreview(settings.copy(opacity = it)) },
            onCommit = { onSettingsCommit(settings.copy(opacity = it)) },
        )
        SunMoonSwitchPreference(
            title = stringResource(R.string.module_wallpaper_auto_contrast),
            checked = autoContrast,
            onCheckedChange = onAutoContrastChange,
        )
    }
}

@Composable
private fun SettingsSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onPreview: (Float) -> Unit,
    onCommit: (Float) -> Unit,
) {
    var localValue by remember(value) { mutableFloatStateOf(value) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (range.endInclusive > 3f) "${localValue.toInt()}" else "%.2f".format(localValue),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = localValue,
            valueRange = range,
            onValueChange = {
                localValue = it
                onPreview(it)
            },
            onValueChangeFinished = { onCommit(localValue) },
        )
    }
}

@Composable
private fun CarouselSettings(
    collection: ModuleWallpaperCollection,
    onChange: (ModuleWallpaperCollection) -> Unit,
) {
    var intervalSeconds by remember(collection.intervalMillis) {
        mutableFloatStateOf(collection.intervalMillis / 1000f)
    }
    WallpaperEditorCard {
        Text(stringResource(R.string.module_wallpaper_carousel_settings), style = MaterialTheme.typography.titleMedium)
        SunMoonSwitchPreference(
            title = stringResource(R.string.module_wallpaper_carousel_enable),
            checked = collection.carouselEnabled,
            onCheckedChange = { onChange(collection.copy(carouselEnabled = it)) },
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WallpaperChoice(
                selected = collection.carouselEnabled && collection.carouselOrder == ModuleWallpaperCarouselOrder.Sequential,
                onClick = { onChange(collection.copy(carouselEnabled = true, carouselOrder = ModuleWallpaperCarouselOrder.Sequential)) },
                label = stringResource(R.string.module_wallpaper_order_sequential),
            )
            WallpaperChoice(
                selected = collection.carouselEnabled && collection.carouselOrder == ModuleWallpaperCarouselOrder.Random,
                onClick = { onChange(collection.copy(carouselEnabled = true, carouselOrder = ModuleWallpaperCarouselOrder.Random)) },
                label = stringResource(R.string.module_wallpaper_order_random),
            )
            WallpaperChoice(
                selected = !collection.carouselEnabled,
                onClick = { onChange(collection.copy(carouselEnabled = false)) },
                label = stringResource(R.string.module_wallpaper_order_fixed),
            )
        }
        Text(stringResource(R.string.module_wallpaper_interval_seconds, intervalSeconds.toInt()))
        Slider(
            value = intervalSeconds,
            valueRange = 3f..60f,
            onValueChange = { intervalSeconds = it.roundToInt().toFloat() },
            onValueChangeFinished = {
                onChange(collection.copy(intervalMillis = intervalSeconds.toLong() * 1000L))
            },
        )
    }
}

@Composable
private fun ModuleWallpaperBatchDialog(
    sourceModule: Module?,
    modules: List<Module>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onApply: (List<Module>) -> Unit,
) {
    var mode by remember { mutableStateOf(WallpaperBatchMode.Selected) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    val candidates = modules.filterNot { it.id == sourceModule?.id }
    val targets = when (mode) {
        WallpaperBatchMode.Selected -> candidates.filter { it.id in selectedIds }
        WallpaperBatchMode.SameType -> candidates.filter { it.metamodule == sourceModule?.metamodule }
        WallpaperBatchMode.All -> candidates
    }
    // A second modal must not reveal the editor controls through its own surface.
    UiPassthroughTheme(UiPassthroughSettings()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 1f),
            title = { Text(stringResource(R.string.module_wallpaper_batch_apply)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        WallpaperChoice(
                            selected = mode == WallpaperBatchMode.Selected,
                            onClick = { mode = WallpaperBatchMode.Selected },
                            label = stringResource(R.string.module_wallpaper_batch_selected),
                        )
                        WallpaperChoice(
                            selected = mode == WallpaperBatchMode.SameType,
                            onClick = { mode = WallpaperBatchMode.SameType },
                            label = stringResource(R.string.module_wallpaper_batch_same_type),
                        )
                        WallpaperChoice(
                            selected = mode == WallpaperBatchMode.All,
                            onClick = { mode = WallpaperBatchMode.All },
                            label = stringResource(R.string.module_wallpaper_batch_all),
                        )
                    }
                    if (mode == WallpaperBatchMode.Selected) {
                        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                            items(candidates, key = Module::id) { module ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .toggleable(
                                            value = module.id in selectedIds,
                                            enabled = !busy,
                                            role = Role.Checkbox,
                                            onValueChange = { checked ->
                                                selectedIds = if (checked) selectedIds + module.id else selectedIds - module.id
                                            },
                                        )
                                        .padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = module.id in selectedIds,
                                        onCheckedChange = null,
                                        enabled = !busy,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(module.name, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.module_wallpaper_batch_count, targets.size),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = targets.isNotEmpty() && !busy, onClick = { onApply(targets) }) {
                    Text(stringResource(R.string.module_wallpaper_batch_confirm, targets.size))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}
