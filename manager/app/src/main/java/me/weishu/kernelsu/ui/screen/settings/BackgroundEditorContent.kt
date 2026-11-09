package me.weishu.kernelsu.ui.screen.settings

import me.weishu.kernelsu.ui.component.passthrough.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import me.weishu.kernelsu.ui.component.passthrough.MaterialText as Text
import androidx.compose.material3.TextButton
import me.weishu.kernelsu.ui.component.passthrough.MaterialIcon as Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.MediaVisualLayer
import me.weishu.kernelsu.ui.component.rememberCustomVideoFrameBitmap
import me.weishu.kernelsu.ui.component.rememberCustomWallpaperPreviewBitmap
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.util.CustomBackgroundState
import me.weishu.kernelsu.ui.util.CustomPageBackgroundTarget
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_WALLPAPER_OPACITY
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_WALLPAPER_OPACITY
import me.weishu.kernelsu.ui.util.rememberUiPassthroughScope
import me.weishu.kernelsu.ui.util.sanitizeCustomWallpaperOpacity
import kotlin.math.roundToInt

@Composable
internal fun BackgroundSettingsContent(
    uiState: SettingsUiState,
    actions: BackgroundSettingsActions,
    modifier: Modifier = Modifier,
) {
    var scopeKey by rememberSaveable { mutableStateOf(GLOBAL_BACKGROUND_SCOPE_KEY) }
    var clearScopeKey by rememberSaveable { mutableStateOf<String?>(null) }
    var section by rememberSaveable { mutableIntStateOf(0) }
    val state = uiState.backgroundStateForScope(scopeKey)
    val globalState = uiState.backgroundStateForScope(GLOBAL_BACKGROUND_SCOPE_KEY)
    val inherited = scopeKey != GLOBAL_BACKGROUND_SCOPE_KEY && !state.hasMedia
    val effectiveState = if (inherited) globalState else state

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        BoxWithConstraints(
            modifier = Modifier.widthIn(max = 1120.dp).fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            val twoPane = useBackgroundEditorTwoPane(
                maxWidth.value, maxHeight.value, LocalDensity.current.fontScale,
            )
            val header: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.background_editor_intro),
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    BackgroundScopeSelector(scopeKey = scopeKey, onSelect = { scopeKey = it })
                }
            }
            // Only the selected scope creates media decoders and editing controls.
            val source: @Composable () -> Unit = {
                key(scopeKey) {
                    BackgroundSourcePanel(
                        scopeKey = scopeKey,
                        state = state,
                        effectiveState = effectiveState,
                        inherited = inherited,
                        actions = actions,
                        onClear = { clearScopeKey = scopeKey },
                    )
                }
            }
            val controls: @Composable () -> Unit = {
                BackgroundControlPanel(scopeKey, state, uiState, actions, section, { section = it })
            }
            if (twoPane) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    header()
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        Column(
                            modifier = Modifier.weight(0.42f)
                                .verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
                        ) { source() }
                        Column(
                            modifier = Modifier.weight(0.58f)
                                .verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
                        ) { controls() }
                    }
                }
            } else {
                // Include the scope picker in the scroll area so short windows never lose the editor.
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    header()
                    source()
                    controls()
                }
            }
        }
    }

    clearScopeKey?.let { pendingScope ->
        AlertDialog(
            onDismissRequest = { clearScopeKey = null },
            title = { Text(stringResource(R.string.background_editor_clear_title, backgroundScopeTitle(pendingScope))) },
            text = {
                Text(stringResource(
                    if (pendingScope == GLOBAL_BACKGROUND_SCOPE_KEY) R.string.background_editor_clear_global
                    else R.string.background_editor_clear_page,
                ))
            },
            confirmButton = {
                TextButton(onClick = {
                    actions.onClear(pendingScope)
                    clearScopeKey = null
                }) { Text(stringResource(R.string.settings_page_background_clear_action)) }
            },
            dismissButton = {
                TextButton(onClick = { clearScopeKey = null }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun BackgroundScopeSelector(scopeKey: String, onSelect: (String) -> Unit) {
    val scopes = listOf(GLOBAL_BACKGROUND_SCOPE_KEY) + CustomPageBackgroundTarget.entries.map(::pageScopeKey)
    val selectedIndex = scopes.indexOf(scopeKey).coerceAtLeast(0)
    if (me.weishu.kernelsu.ui.util.currentAppWindowSize().compactNavigation) {
        AppearanceEditorTargetPicker(R.string.background_editor_scope, scopes.map { backgroundScopeTitle(it) },
            selectedIndex, { onSelect(scopes[it]) })
        return
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    LaunchedEffect(scopeKey) {
        val visibleIndices = listState.layoutInfo.visibleItemsInfo.map { it.index }
        if (selectedIndex !in visibleIndices) listState.scrollToItem(selectedIndex)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.background_editor_scope),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(scopes, key = { it }) { candidate ->
                val selected = candidate == scopeKey
                FilterChip(
                    modifier = Modifier.heightIn(min = 48.dp),
                    selected = selected,
                    onClick = { onSelect(candidate) },
                    label = { Text(backgroundScopeTitle(candidate)) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null,
                )
            }
        }
    }
}

@Composable
private fun BackgroundSourcePanel(
    scopeKey: String,
    state: CustomBackgroundState,
    effectiveState: CustomBackgroundState,
    inherited: Boolean,
    actions: BackgroundSettingsActions,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = backgroundScopeTitle(scopeKey),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(when {
                    inherited -> R.string.background_editor_inherited
                    scopeKey == GLOBAL_BACKGROUND_SCOPE_KEY -> R.string.settings_global_background_summary
                    else -> R.string.settings_page_background_override_hint
                }),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        key(effectiveState.wallpaperUriString, effectiveState.videoUriString, effectiveState.crop) {
            BackgroundEditorPreview(
                state = effectiveState,
                scopeKey = scopeKey,
                onPreview = { actions.onPreview(scopeKey) },
            )
        }
        if (effectiveState.hasMedia) {
            Text(
                text = if (inherited) stringResource(R.string.settings_page_background_empty)
                    else backgroundSelectionSummary(state),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            MediaFileInfoSummary(rememberMediaFileInfo(effectiveState.wallpaperUriString ?: effectiveState.videoUriString))
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BackgroundMediaButton(
                text = stringResource(R.string.background_editor_choose_image),
                icon = Icons.Rounded.PhotoLibrary,
                onClick = { actions.onPickImage(scopeKey) },
            )
            BackgroundMediaButton(
                text = stringResource(R.string.background_editor_choose_video),
                icon = Icons.Rounded.Videocam,
                onClick = { actions.onPickVideo(scopeKey) },
            )
        }
        if (effectiveState.hasMedia) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (state.hasMedia) {
                    BackgroundUtilityButton(stringResource(R.string.settings_page_background_crop_action),
                        Icons.Rounded.ImageSearch, { actions.onCrop(scopeKey) })
                }
                BackgroundUtilityButton(stringResource(R.string.settings_page_background_preview_action),
                    Icons.Rounded.Visibility, { actions.onPreview(scopeKey) })
                if (state.hasMedia) {
                    BackgroundUtilityButton(stringResource(R.string.settings_page_background_clear_action),
                        Icons.Rounded.Close, onClear)
                }
            }
        }
    }
}

@Composable
private fun BackgroundEditorPreview(state: CustomBackgroundState, scopeKey: String, onPreview: () -> Unit) {
    val image = rememberCustomWallpaperPreviewBitmap(state.wallpaperUriString, state.crop)
    val video = rememberCustomVideoFrameBitmap(state.videoUriString)
    val bitmap = image ?: video
    val passthrough = rememberUiPassthroughScope(pageTargetFromScopeKey(scopeKey)).settings
    val overlayColor = if (isInDarkTheme()) Color.Black else Color.White
    val aspectRatio = 1.6f / LocalDensity.current.fontScale.coerceAtLeast(1f)
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(enabled = state.hasMedia, onClickLabel = stringResource(R.string.settings_page_background_preview_action),
                onClick = onPreview),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            MediaVisualLayer(settings = state.visualSettings, modifier = Modifier.fillMaxSize()) { colorFilter ->
                Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop, colorFilter = colorFilter)
            }
            // Fade only the background, never the sample text or editor controls.
            Box(Modifier.fillMaxSize().background(overlayColor.copy(alpha = 1f - sanitizeCustomWallpaperOpacity(state.opacity))))
            BackgroundPreviewUi(passthrough)
        } else {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.Wallpaper, contentDescription = null, modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = stringResource(if (state.hasMedia) R.string.background_editor_preview_unavailable
                        else R.string.background_editor_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BackgroundControlPanel(
    scopeKey: String,
    state: CustomBackgroundState,
    uiState: SettingsUiState,
    actions: BackgroundSettingsActions,
    section: Int,
    onSectionChange: (Int) -> Unit,
) {
    val sections = listOf(R.string.background_editor_basic, R.string.background_editor_effects, R.string.ui_passthrough_title)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column {
                PrimaryScrollableTabRow(
                    selectedTabIndex = section,
                    edgePadding = 8.dp,
                    minTabWidth = 104.dp * LocalDensity.current.fontScale.coerceAtLeast(1f),
                    containerColor = Color.Transparent,
                    divider = {},
                ) {
                    sections.forEachIndexed { index, label ->
                        Tab(selected = section == index, onClick = { onSectionChange(index) },
                            text = { Text(stringResource(label)) })
                    }
                }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (section) {
                        0 -> BackgroundBasicControls(scopeKey, state, actions)
                        1 -> if (state.hasMedia) {
                            Text(stringResource(R.string.background_editor_effects_help),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            MediaVisualControls(
                                value = state.visualSettings,
                                onValueChange = { actions.onVisualSettingsChange(scopeKey, it) },
                                showContrast = true,
                                showTemperature = true,
                                showNoise = true,
                                showMotion = true,
                            )
                        } else BackgroundChooseMediaHint(scopeKey)
                        2 -> UiPassthroughOptions(target = pageTargetFromScopeKey(scopeKey), showHeader = false)
                    }
                }
            }
        }
        if (scopeKey == GLOBAL_BACKGROUND_SCOPE_KEY) SharedBackgroundOptions(uiState, actions)
        Text(
            text = stringResource(R.string.background_editor_auto_save),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BackgroundBasicControls(scopeKey: String, state: CustomBackgroundState, actions: BackgroundSettingsActions) {
    if (!state.hasMedia) {
        BackgroundChooseMediaHint(scopeKey)
        return
    }
    Text(stringResource(R.string.background_editor_opacity_help),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    BackgroundSlider(
        title = stringResource(R.string.settings_background_opacity),
        value = state.opacity,
        valueRange = MIN_CUSTOM_WALLPAPER_OPACITY..MAX_CUSTOM_WALLPAPER_OPACITY,
        valueLabel = { "${(it * 100).roundToInt()}%" },
        onValueChange = { actions.onOpacityChange(scopeKey, it) },
    )
    if (state.hasVideo) {
        BackgroundSlider(
            title = stringResource(R.string.settings_video_background_duration),
            value = state.videoDurationSeconds.toFloat(),
            valueRange = MIN_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS.toFloat()..MAX_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS.toFloat(),
            valueLabel = { stringResource(R.string.settings_video_background_duration_value, it.roundToInt()) },
            onValueChange = { actions.onDurationChange(scopeKey, it.roundToInt()) },
        )
        if (scopeKey == GLOBAL_BACKGROUND_SCOPE_KEY) {
            VideoFrameRateSelector(state.videoFrameRate, actions.onFrameRateChange)
        } else {
            Text(stringResource(R.string.background_editor_frame_rate_inherit, state.videoFrameRate),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BackgroundChooseMediaHint(scopeKey: String) {
    Text(
        text = stringResource(if (scopeKey == GLOBAL_BACKGROUND_SCOPE_KEY) R.string.background_editor_choose_hint
            else R.string.background_editor_override_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun BackgroundMediaButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
private fun BackgroundUtilityButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text)
    }
}

@Composable
private fun backgroundScopeTitle(scopeKey: String): String = stringResource(
    pageTargetFromScopeKey(scopeKey)?.titleRes ?: R.string.settings_global_background,
)
