package me.weishu.kernelsu.ui.screen.themestore

import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import me.weishu.kernelsu.ui.component.store.StoreAlertDialog as AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
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
import me.weishu.kernelsu.ui.component.store.StoreSearchField
import me.weishu.kernelsu.ui.component.store.StoreEmptyState
import me.weishu.kernelsu.ui.component.store.StoreScaffold
import me.weishu.kernelsu.ui.component.store.StoreSectionHeading
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.util.THEME_STORE_FILE_EXTENSION
import me.weishu.kernelsu.ui.util.THEME_STORE_FILE_MIME_TYPE
import me.weishu.kernelsu.ui.util.ThemeLibraryEntry
import me.weishu.kernelsu.ui.util.ThemeLibraryOperationResult
import me.weishu.kernelsu.ui.util.applyThemeFromLibrary
import me.weishu.kernelsu.ui.util.deleteThemeLibraryEntry
import me.weishu.kernelsu.ui.util.exportThemeLibraryEntry
import me.weishu.kernelsu.ui.util.importThemeToLibrary
import me.weishu.kernelsu.ui.util.previewThemeStorePackage
import me.weishu.kernelsu.ui.util.readThemeLibrary
import me.weishu.kernelsu.ui.util.renameThemeLibraryEntry
import me.weishu.kernelsu.ui.util.saveCurrentThemeToLibrary
import java.text.DateFormat
import java.util.Date

@Composable
fun ThemeStoreLibraryScreen(
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
    headerContent: (@Composable ColumnScope.(busy: Boolean) -> Unit)? = null,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    var themes by remember { mutableStateOf(readThemeLibrary(context)) }
    var busy by remember { mutableStateOf(false) }
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ThemeLibraryEntry?>(null) }
    var applyTarget by remember { mutableStateOf<ThemeLibraryEntry?>(null) }
    var deleteTarget by remember { mutableStateOf<ThemeLibraryEntry?>(null) }
    var exportTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<PendingThemeStoreImport?>(null) }
    var detailMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var detailIsError by rememberSaveable { mutableStateOf(true) }

    fun refreshThemes() {
        themes = readThemeLibrary(context)
    }

    fun runOperation(
        fallbackMessage: String,
        operation: suspend () -> ThemeLibraryOperationResult,
    ) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) { operation() }
                refreshThemes()
                if (!result.success) {
                    detailIsError = true
                    detailMessage = result.failureMessage(fallbackMessage)
                } else if (result.packageResult?.warnings?.isNotEmpty() == true) {
                    detailIsError = false
                    detailMessage = resources.getString(
                        R.string.theme_store_my_completed_with_warnings,
                        result.packageResult.warnings.size,
                    )
                }
                Toast.makeText(
                    context,
                    if (result.success) R.string.theme_store_transfer_completed else R.string.theme_store_transfer_failed,
                    Toast.LENGTH_SHORT,
                ).show()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                detailIsError = true
                detailMessage = error.localizedMessage
                    ?.lineSequence()
                    ?.firstOrNull()
                    ?.take(240)
                    ?: fallbackMessage
                Toast.makeText(context, R.string.theme_store_transfer_failed, Toast.LENGTH_SHORT).show()
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
                    detailIsError = true
                    detailMessage = result.error?.localizedMessage
                        ?.lineSequence()
                        ?.firstOrNull()
                        ?.take(240)
                        ?: resources.getString(R.string.theme_store_my_import_failed)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                detailIsError = true
                detailMessage = error.localizedMessage
                    ?.lineSequence()
                    ?.firstOrNull()
                    ?.take(240)
                    ?: resources.getString(R.string.theme_store_my_import_failed)
            } finally {
                busy = false
            }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(THEME_STORE_FILE_MIME_TYPE),
    ) { uri ->
        val targetId = exportTargetId
        exportTargetId = null
        if (uri == null || targetId == null) return@rememberLauncherForActivityResult
        runOperation(resources.getString(R.string.theme_store_export_failed)) {
            exportThemeLibraryEntry(context, targetId, uri)
        }
    }

    LifecycleResumeEffect(Unit) {
        refreshThemes()
        onPauseOrDispose { }
    }

    val content: @Composable (PaddingValues) -> Unit = { paddingValues ->
        ThemeStoreMyContent(
            themes = themes,
            busy = busy,
            modifier = modifier.padding(paddingValues),
            headerContent = headerContent,
            onSaveCurrent = { showSaveDialog = true },
            onImport = {
                importLauncher.launch(
                    arrayOf(THEME_STORE_FILE_MIME_TYPE, "application/octet-stream", "*/*")
                )
            },
            onApply = { applyTarget = it },
            onRename = { renameTarget = it },
            onExport = { entry ->
                exportTargetId = entry.id
                exportLauncher.launch("${entry.safeFileName()}.$THEME_STORE_FILE_EXTENSION")
            },
            onDelete = { deleteTarget = it },
        )
    }
    val onBack = dropUnlessResumed { navigator.pop() }

    if (embedded) content(PaddingValues()) else StoreScaffold(
        title = stringResource(R.string.theme_store_my_library_title), onBack = onBack,
        maxContentWidth = 840.dp, content = content)

    if (showSaveDialog) {
        ThemeStoreNameDialog(
            title = stringResource(R.string.theme_store_my_save_title),
            initialName = stringResource(R.string.theme_store_my_default_name),
            confirmLabel = stringResource(R.string.theme_store_my_save_action),
            onDismiss = { showSaveDialog = false },
            onConfirm = { name ->
                showSaveDialog = false
                runOperation(resources.getString(R.string.theme_store_my_save_failed)) {
                    saveCurrentThemeToLibrary(context, name)
                }
            },
        )
    }

    renameTarget?.let { entry ->
        ThemeStoreNameDialog(
            title = stringResource(R.string.theme_store_my_rename_title),
            initialName = entry.name,
            confirmLabel = stringResource(R.string.theme_store_my_rename_action),
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                renameTarget = null
                runOperation(resources.getString(R.string.theme_store_my_rename_failed)) {
                    renameThemeLibraryEntry(context, entry.id, name)
                }
            },
        )
    }

    applyTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = { applyTarget = null },
            title = { Text(stringResource(R.string.theme_store_my_apply_title)) },
            text = { Text(stringResource(R.string.theme_store_my_apply_message, entry.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        applyTarget = null
                        runOperation(resources.getString(R.string.theme_store_my_apply_failed)) {
                            applyThemeFromLibrary(context, entry.id)
                        }
                    },
                ) {
                    Text(stringResource(R.string.theme_store_my_apply_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { applyTarget = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    deleteTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.theme_store_my_delete_title)) },
            text = { Text(stringResource(R.string.theme_store_my_delete_message, entry.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteTarget = null
                        runOperation(resources.getString(R.string.theme_store_my_delete_failed)) {
                            deleteThemeLibraryEntry(context, entry.id)
                        }
                    },
                ) {
                    Text(
                        text = stringResource(R.string.theme_store_my_delete_action),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    detailMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { detailMessage = null },
            title = {
                Text(
                    stringResource(
                        if (detailIsError) {
                            R.string.theme_store_transfer_failed
                        } else {
                            R.string.theme_store_transfer_completed_with_warnings
                        }
                    )
                )
            },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { detailMessage = null }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }

    pendingImport?.let { pending ->
        ThemeStoreImportPreviewDialog(
            pending = pending,
            confirmLabel = stringResource(R.string.theme_store_import_preview_add_action),
            onDismiss = { pendingImport = null },
            onConfirm = {
                pendingImport = null
                runOperation(resources.getString(R.string.theme_store_my_import_failed)) {
                    importThemeToLibrary(context, pending.uri)
                }
            },
        )
    }
}

@Composable
private fun ThemeStoreMyContent(
    themes: List<ThemeLibraryEntry>,
    busy: Boolean,
    modifier: Modifier = Modifier,
    headerContent: (@Composable ColumnScope.(busy: Boolean) -> Unit)?,
    onSaveCurrent: () -> Unit,
    onImport: () -> Unit,
    onApply: (ThemeLibraryEntry) -> Unit,
    onRename: (ThemeLibraryEntry) -> Unit,
    onExport: (ThemeLibraryEntry) -> Unit,
    onDelete: (ThemeLibraryEntry) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visibleThemes = remember(themes, query) { themes.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) } }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        headerContent?.let { header ->
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = { header(busy) },
                )
            }
        }
        item {
            ThemeLibraryActionPanel(
                themeCount = themes.size,
                busy = busy,
                onSaveCurrent = onSaveCurrent,
                onImport = onImport,
            )
        }
        item { StoreSearchField(query, { query = it }, stringResource(R.string.cloud_theme_search_hint)) }
        if (themes.isNotEmpty() && visibleThemes.isEmpty()) {
            item { StoreEmptyState(Icons.Rounded.Search, stringResource(R.string.store_redesign_no_results),
                stringResource(R.string.store_redesign_no_results_summary), stringResource(R.string.store_redesign_clear_filters), { query = "" }) }
        }
        if (themes.isEmpty()) {
            item { ThemeLibraryEmptyState() }
        } else {
            items(visibleThemes, key = ThemeLibraryEntry::id) { entry ->
                ThemeLibraryItem(
                    entry = entry,
                    enabled = !busy,
                    onApply = { onApply(entry) },
                    onRename = { onRename(entry) },
                    onExport = { onExport(entry) },
                    onDelete = { onDelete(entry) },
                )
            }
        }
        item { Spacer(modifier = Modifier.height(18.dp)) }
    }
}

@Composable
private fun ThemeLibraryActionPanel(
    themeCount: Int,
    busy: Boolean,
    onSaveCurrent: () -> Unit,
    onImport: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StoreSectionHeading(stringResource(R.string.theme_store_my_library_title),
            stringResource(R.string.theme_store_my_count, themeCount))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onSaveCurrent, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(stringResource(R.string.theme_store_my_save_action))
            }
            OutlinedButton(onClick = onImport, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(stringResource(R.string.theme_store_my_import_action))
            }
        }
    }
}

@Composable
private fun ThemeLibraryItem(
    entry: ThemeLibraryEntry,
    enabled: Boolean,
    onApply: () -> Unit,
    onRename: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var menuExpanded by rememberSaveable(entry.id) { mutableStateOf(false) }
    ThemeLibrarySurface {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 8.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = themeLibraryTextColor(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            R.string.theme_store_my_saved_meta,
                            formatThemeLibraryDate(entry.createdAt),
                            Formatter.formatShortFileSize(context, entry.sizeBytes),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = themeLibraryMutedColor(),
                    )
                }
                Box {
                    IconButton(
                        enabled = enabled,
                        onClick = { menuExpanded = true },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.theme_store_my_more_actions),
                            tint = themeLibraryMutedColor(),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.theme_store_my_rename_action)) },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onRename()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.theme_store_export)) },
                            leadingIcon = { Icon(Icons.Rounded.FileDownload, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onExport()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.theme_store_my_delete_action)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                        )
                    }
                }
            }
            entry.lastAppliedAt?.let { appliedAt ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(
                            R.string.theme_store_my_last_applied,
                            formatThemeLibraryDate(appliedAt),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                onClick = onApply,
            ) {
                Text(stringResource(R.string.theme_store_my_apply_action))
            }
        }
    }
}

@Composable
private fun ThemeLibraryEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 34.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Palette,
            contentDescription = null,
            tint = themeLibraryMutedColor(),
            modifier = Modifier.size(36.dp),
        )
        Text(
            text = stringResource(R.string.theme_store_my_empty_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = themeLibraryTextColor(),
        )
        Text(
            text = stringResource(R.string.theme_store_my_empty_summary),
            style = MaterialTheme.typography.bodySmall,
            color = themeLibraryMutedColor(),
        )
    }
}

@Composable
private fun ThemeStoreNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(48) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.theme_store_my_name_label)) },
            )
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name) },
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}


@Composable
private fun ThemeLibrarySurface(content: @Composable () -> Unit) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface, content = content,
    )
}

@Composable
private fun themeLibraryTextColor(): Color {
    return MaterialTheme.colorScheme.onSurface
}

@Composable
private fun themeLibraryMutedColor(): Color {
    return MaterialTheme.colorScheme.onSurfaceVariant
}

private fun formatThemeLibraryDate(timestamp: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
}

private fun ThemeLibraryEntry.safeFileName(): String {
    return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "SterSU-theme" }
}

private fun ThemeLibraryOperationResult.failureMessage(fallback: String): String {
    return error?.localizedMessage
        ?.trim()
        ?.lineSequence()
        ?.firstOrNull()
        ?.take(240)
        ?.takeIf { it.isNotBlank() }
        ?: packageResult?.error?.localizedMessage
            ?.trim()
            ?.lineSequence()
            ?.firstOrNull()
            ?.take(240)
            ?.takeIf { it.isNotBlank() }
        ?: fallback
}
