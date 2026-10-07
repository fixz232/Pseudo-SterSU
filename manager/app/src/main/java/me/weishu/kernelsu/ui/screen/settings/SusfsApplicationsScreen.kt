@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package me.weishu.kernelsu.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.AppIconImage
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.theme.immersivePageColor
import me.weishu.kernelsu.ui.theme.immersiveScrolledTopBarColor
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor
import me.weishu.kernelsu.ui.theme.immersiveTopBarColor
import me.weishu.kernelsu.ui.util.SusfsApplication
import me.weishu.kernelsu.ui.util.SusfsApplyReport
import me.weishu.kernelsu.ui.util.SusfsHideMode
import me.weishu.kernelsu.ui.util.SusfsImportPlan
import me.weishu.kernelsu.ui.util.SusfsPlanAction
import me.weishu.kernelsu.ui.util.SusfsPolicyState
import me.weishu.kernelsu.ui.util.SusfsRiskSignal
import me.weishu.kernelsu.ui.viewmodel.SusfsAppCategory
import me.weishu.kernelsu.ui.viewmodel.SusfsAppFilter
import me.weishu.kernelsu.ui.viewmodel.SusfsApplicationsViewModel
import me.weishu.kernelsu.ui.viewmodel.filterSusfsApplications
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SusfsApplicationsScreen() {
    val model = viewModel<SusfsApplicationsViewModel>()
    val state by model.state.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(SusfsAppCategory.All) }
    var filter by rememberSaveable { mutableStateOf(SusfsAppFilter.All) }
    var showSystem by rememberSaveable { mutableStateOf(false) }
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var showReport by rememberSaveable { mutableStateOf(false) }
    var pendingChange by remember { mutableStateOf<Pair<SusfsApplication, SusfsHideMode>?>(null) }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        model.choosingDocument = false
        if (uri != null) model.prepareImport(uri)
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        model.choosingDocument = false
        if (uri != null) model.export(uri)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.onResume() }
    LaunchedEffect(state.notice) {
        if (state.notice == "applied" && state.report?.items?.any { it.status == "failed" } == true) showReport = true
    }
    val filtered = remember(state.apps, query, category, filter, showSystem) {
        filterSusfsApplications(state.apps, query, category, filter, showSystem)
    }

    fun requestChange(app: SusfsApplication, mode: SusfsHideMode) {
        if (state.busy) return
        if (app.packageNames.size > 1) pendingChange = app to mode else model.change(app, mode)
    }

    Scaffold(
        containerColor = immersivePageColor(MaterialTheme.colorScheme.background),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.susfs_applications_title), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = dropUnlessResumed { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = model::refresh, enabled = !state.busy) {
                        Icon(Icons.Rounded.Refresh, stringResource(R.string.susfs_applications_refresh))
                    }
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Rounded.MoreVert, stringResource(R.string.susfs_more_actions))
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.susfs_applications_import)) },
                                enabled = !state.busy,
                                onClick = {
                                    menu = false
                                    model.choosingDocument = true
                                    importer.launch(arrayOf("application/json", "text/*", "application/octet-stream"))
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.susfs_applications_export)) },
                                enabled = state.loaded && !state.busy,
                                onClick = {
                                    menu = false
                                    model.choosingDocument = true
                                    exporter.launch("SterSU-application-policies.json")
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.susfs_apps_restore)) },
                                enabled = state.hasBackup && !state.busy,
                                onClick = { menu = false; model.prepareRestore() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.susfs_apps_results)) },
                                enabled = state.report != null,
                                onClick = { menu = false; showReport = true },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.susfs_apps_help)) },
                                onClick = { menu = false; showHelp = true },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = immersiveTopBarColor(MaterialTheme.colorScheme.background),
                    scrolledContainerColor = immersiveScrolledTopBarColor(MaterialTheme.colorScheme.surface),
                ),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "search") {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.susfs_applications_search)) },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    )
                }
                item(key = "categories") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        SusfsAppCategory.entries.forEach { item ->
                            FilterChip(
                                selected = category == item,
                                onClick = { category = item },
                                label = { Text(stringResource(when (item) {
                                    SusfsAppCategory.All -> R.string.susfs_apps_all
                                    SusfsAppCategory.Risk -> R.string.susfs_applications_risk_count
                                    SusfsAppCategory.Normal -> R.string.susfs_applications_normal_count
                                })) },
                            )
                        }
                        FilterChip(
                            selected = filtersOpen || showSystem || filter != SusfsAppFilter.All,
                            onClick = { filtersOpen = !filtersOpen },
                            label = { Text(stringResource(R.string.susfs_apps_filters)) },
                            leadingIcon = { Icon(Icons.Rounded.Tune, null, Modifier.size(18.dp)) },
                        )
                    }
                    if (filtersOpen) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SusfsAppFilter.entries.forEach { item ->
                                FilterChip(
                                    selected = filter == item,
                                    onClick = { filter = item },
                                    label = { Text(stringResource(when (item) {
                                        SusfsAppFilter.All -> R.string.susfs_apps_all_policies
                                        SusfsAppFilter.Enabled -> R.string.susfs_apps_enabled
                                        SusfsAppFilter.Disabled -> R.string.susfs_apps_disabled
                                        SusfsAppFilter.Default -> R.string.susfs_apps_default
                                        SusfsAppFilter.Unknown -> R.string.susfs_apps_unknown
                                    })) },
                                )
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                                value = showSystem, role = Role.Switch, onValueChange = { showSystem = it },
                            ).padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(stringResource(R.string.susfs_applications_show_system), Modifier.weight(1f))
                            Switch(checked = showSystem, onCheckedChange = null)
                        }
                    }
                    Text(
                        stringResource(
                            R.string.susfs_apps_count,
                            filtered.size,
                            filtered.count { it.policy.effectiveHidden == true },
                            filtered.count { !it.policy.known },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.busy) item(key = "progress") {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.susfs_apps_working), style = MaterialTheme.typography.bodySmall)
                }
                if (state.error != null) item(key = "error") {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer, shape = RoundedCornerShape(8.dp)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            SelectionContainer { Text(policyFailureText(state.error.orEmpty()), style = MaterialTheme.typography.bodyMedium) }
                            TextButton(onClick = model::refresh, enabled = !state.busy) { Text(stringResource(R.string.retry)) }
                        }
                    }
                }
                if (state.notice != null) item(key = "notice") {
                    Column {
                        Text(
                            stringResource(if (state.notice == "exported") R.string.susfs_applications_export_success else R.string.susfs_apps_saved),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (state.notice != "exported") TextButton(onClick = { showReport = true }) { Text(stringResource(R.string.susfs_apps_results)) }
                    }
                }
                if (state.loaded && filtered.isEmpty() && !state.busy) item(key = "empty") {
                    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.susfs_applications_empty))
                        TextButton(onClick = { query = ""; category = SusfsAppCategory.All; filter = SusfsAppFilter.All; showSystem = false }) {
                            Text(stringResource(R.string.susfs_apps_reset_filters))
                        }
                    }
                }
                items(filtered, key = { "${it.uid}:${it.packageName}" }, contentType = { "application" }) { app ->
                    SusfsApplicationRow(app, state.busy, onChange = { requestChange(app, it) })
                }
            }
        }
    }

    state.preview?.let { plan ->
        SusfsImportPreview(plan, state.busy, onDismiss = model::dismissPreview, onApply = model::applyPreview)
    }
    pendingChange?.let { (app, mode) ->
        AlertDialog(
            onDismissRequest = { pendingChange = null },
            title = { Text(stringResource(R.string.susfs_apps_shared_title)) },
            text = {
                LazyColumn(Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Text(stringResource(R.string.susfs_apps_shared_confirm, modeLabel(mode), app.uid)) }
                    items(app.packageNames) { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            },
            confirmButton = {
                TextButton(enabled = !state.busy, onClick = { pendingChange = null; model.change(app, mode) }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = { TextButton(onClick = { pendingChange = null }) { Text(stringResource(R.string.close)) } },
        )
    }
    if (showHelp) AlertDialog(
        onDismissRequest = { showHelp = false },
        title = { Text(stringResource(R.string.susfs_apps_help)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text(stringResource(R.string.susfs_applications_summary)) }
                item { Text(stringResource(R.string.susfs_apps_help_steps)) }
                item { Text(stringResource(R.string.susfs_apps_backup_help)) }
            }
        },
        confirmButton = { TextButton(onClick = { showHelp = false }) { Text(stringResource(R.string.close)) } },
    )
    if (showReport && state.preview == null && state.report != null) {
        SusfsResultDialog(requireNotNull(state.report)) { showReport = false }
    }
}

@Composable
private fun SusfsApplicationRow(app: SusfsApplication, busy: Boolean, onChange: (SusfsHideMode) -> Unit) {
    var expanded by rememberSaveable(app.uid, app.packageName) { mutableStateOf(false) }
    val description = policyLabel(app.policy)
    val switchDescription = stringResource(R.string.susfs_apps_switch, app.label)
    val enabled = app.canManage && app.policy.known && !app.policy.allowSu && !busy
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainer),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIconImage(packageInfo = app.packageInfo, label = app.label, modifier = Modifier.size(44.dp).clearAndSetSemantics { })
                Column(
                    Modifier.weight(1f).heightIn(min = 48.dp).clickable(role = Role.Button) { expanded = !expanded },
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(app.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = if (app.policy.known) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
                }
                Switch(
                    checked = app.policy.effectiveHidden == true,
                    onCheckedChange = { onChange(if (it) SusfsHideMode.Enabled else SusfsHideMode.Disabled) },
                    enabled = enabled,
                    modifier = Modifier.heightIn(min = 48.dp).semantics {
                        contentDescription = switchDescription
                        stateDescription = description
                    },
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        !app.canManage -> stringResource(R.string.susfs_apps_protected)
                        app.policy.allowSu -> stringResource(R.string.susfs_apps_root)
                        app.packageNames.size > 1 -> stringResource(R.string.susfs_apps_shared_count, app.packageNames.size)
                        app.isRisk -> stringResource(R.string.susfs_apps_risk_hint)
                        else -> app.packageName
                    },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, stringResource(if (expanded) R.string.susfs_apps_collapse else R.string.susfs_apps_details))
                }
            }
            if (expanded) {
                HorizontalDivider()
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectionContainer {
                        Text(app.packageNames.joinToString("\n"), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(stringResource(if (app.isSystem) R.string.susfs_applications_uid_system else R.string.susfs_applications_uid, app.uid), style = MaterialTheme.typography.bodySmall)
                    when {
                        !app.canManage -> Text(stringResource(R.string.susfs_applications_system_disabled), style = MaterialTheme.typography.bodySmall)
                        app.policy.allowSu -> Text(stringResource(R.string.susfs_applications_root_allowed), style = MaterialTheme.typography.bodySmall)
                    }
                    app.policy.error?.let { SelectionContainer { Text(policyFailureText(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) } }
                    if (app.isRisk) {
                        val reasons = app.riskSignals.map {
                            stringResource(if (it == SusfsRiskSignal.RootOrIntegrityName) R.string.susfs_applications_risk_name else R.string.susfs_applications_risk_permission)
                        }
                        Text(reasons.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.susfs_apps_risk_disclaimer), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(stringResource(R.string.susfs_apps_effect_note), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { onChange(SusfsHideMode.Default) }, enabled = enabled && app.policy.mode != SusfsHideMode.Default) {
                        Text(stringResource(R.string.susfs_apps_use_default))
                    }
                }
            }
        }
    }
}

@Composable
private fun modeLabel(mode: SusfsHideMode): String = stringResource(when (mode) {
    SusfsHideMode.Default -> R.string.susfs_apps_default
    SusfsHideMode.Enabled -> R.string.susfs_apps_explicit_on
    SusfsHideMode.Disabled -> R.string.susfs_apps_explicit_off
})

@Composable
private fun policyLabel(policy: SusfsPolicyState): String = when {
    !policy.known -> stringResource(R.string.susfs_apps_unknown)
    policy.allowSu -> stringResource(R.string.susfs_apps_root)
    policy.mode == SusfsHideMode.Default -> stringResource(
        R.string.susfs_apps_default_state,
        stringResource(if (policy.effectiveHidden == true) R.string.susfs_apps_enabled else R.string.susfs_apps_disabled),
    )
    else -> modeLabel(policy.mode)
}

@Composable
private fun policyFailureText(detail: String): String {
    val reason = detail.substringBefore(":")
    val res = when (reason) {
        "not_installed" -> R.string.susfs_apps_not_installed
        "protected_app" -> R.string.susfs_applications_system_disabled
        "root_allowed_app" -> R.string.susfs_applications_root_allowed
        "package_uid_changed" -> R.string.susfs_apps_identity_changed
        "shared_uid_conflict" -> R.string.susfs_apps_conflict
        "policy_changed", "preview_expired" -> R.string.susfs_apps_stale
        "unknown_policy", "export_unknown_policies", "refresh_failed", "inconsistent_policy_read" -> R.string.susfs_apps_read_failed
        "profile_update_failed", "readback_failed", "readback_mismatch" -> R.string.susfs_apps_write_failed
        else -> R.string.susfs_apps_operation_failed
    }
    return stringResource(res) + "\n" + detail
}

@Composable
private fun SusfsImportPreview(plan: SusfsImportPlan, busy: Boolean, onDismiss: () -> Unit, onApply: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(if (plan.restoring) R.string.susfs_apps_restore else R.string.susfs_apps_preview)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text(stringResource(
                        R.string.susfs_apps_preview_counts,
                        plan.changes.size,
                        plan.items.count { it.action == SusfsPlanAction.Unchanged },
                        plan.items.count { it.action == SusfsPlanAction.Skipped },
                        plan.items.count { it.action == SusfsPlanAction.Conflict },
                    ))
                    Text(stringResource(R.string.susfs_apps_preview_note), style = MaterialTheme.typography.bodySmall)
                }
                items(plan.items) { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.packages.joinToString("\n"), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (item.action == SusfsPlanAction.Change) stringResource(
                                R.string.susfs_apps_mode_change, modeLabel(requireNotNull(item.target).state.mode), modeLabel(item.desired),
                            ) else stringResource(when (item.action) {
                                SusfsPlanAction.Unchanged -> R.string.susfs_apps_unchanged
                                SusfsPlanAction.Conflict -> R.string.susfs_apps_conflict
                                else -> R.string.susfs_apps_skipped
                            }),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        if (item.reason.isNotEmpty()) Text(policyFailureText(item.reason), style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onApply, enabled = plan.canApply && !busy) { Text(stringResource(R.string.susfs_apps_apply)) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
private fun SusfsResultDialog(report: SusfsApplyReport, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.susfs_apps_results)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text(DateFormat.getDateTimeInstance().format(Date(report.at)), style = MaterialTheme.typography.labelMedium)
                    Text(stringResource(
                        R.string.susfs_apps_result_counts,
                        report.items.count { it.status == "applied" },
                        report.items.count { it.status == "unchanged" },
                        report.items.count { it.status == "skipped" },
                        report.items.count { it.status == "failed" || it.status == "pending" },
                    ))
                }
                items(report.items) { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SelectionContainer { Text(item.packages.joinToString("\n"), style = MaterialTheme.typography.bodyMedium) }
                        Text(stringResource(when (item.status) {
                            "applied" -> R.string.susfs_apps_verified
                            "unchanged" -> R.string.susfs_apps_unchanged
                            "skipped" -> R.string.susfs_apps_skipped
                            "pending" -> R.string.susfs_apps_pending
                            else -> R.string.susfs_apps_failed
                        }), style = MaterialTheme.typography.labelLarge)
                        if (item.detail.isNotEmpty()) SelectionContainer { Text(policyFailureText(item.detail), style = MaterialTheme.typography.bodySmall) }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
