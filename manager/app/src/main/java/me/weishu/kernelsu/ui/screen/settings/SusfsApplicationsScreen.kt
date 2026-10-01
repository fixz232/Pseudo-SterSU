package me.weishu.kernelsu.ui.screen.settings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.AppIconImage
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.theme.immersivePageColor
import me.weishu.kernelsu.ui.theme.immersiveScrolledTopBarColor
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor
import me.weishu.kernelsu.ui.theme.immersiveTopBarColor
import me.weishu.kernelsu.ui.util.SusfsApplication
import me.weishu.kernelsu.ui.util.SusfsApplicationHidingConfigEntry
import me.weishu.kernelsu.ui.util.SusfsRiskSignal
import me.weishu.kernelsu.ui.util.encodeSusfsApplicationHidingConfig
import me.weishu.kernelsu.ui.util.importSusfsApplicationHidingConfig
import me.weishu.kernelsu.ui.util.loadSusfsApplications
import me.weishu.kernelsu.ui.util.setSusfsApplicationHidden
import java.io.ByteArrayOutputStream

private enum class SusfsApplicationCategory {
    Risk,
    Normal,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SusfsApplicationsScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    var applications by remember { mutableStateOf(emptyList<SusfsApplication>()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableIntStateOf(0) }
    var updatingUid by remember { mutableStateOf<Int?>(null) }
    var showSystemApps by rememberSaveable { mutableStateOf(false) }
    var configBusy by remember { mutableStateOf(false) }

    fun refresh(showLoading: Boolean = true) {
        if (refreshing || configBusy) return
        scope.launch {
            refreshing = true
            if (showLoading) loading = true
            error = ""
            val result = runCatching { loadSusfsApplications(context) }
            result.onSuccess { applications = it }
                .onFailure { error = it.message.orEmpty().ifBlank { "load_failed" } }
            loading = false
            refreshing = false
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                configBusy = true
                try {
                    val result = runCatching {
                        val json = readSusfsApplicationHidingConfig(context, uri)
                        val imported = importSusfsApplicationHidingConfig(context, json)
                        imported to loadSusfsApplications(context)
                    }
                    result.onSuccess { (imported, refreshed) ->
                        applications = refreshed
                        error = ""
                        Toast.makeText(
                            context,
                            context.getString(
                                R.string.susfs_applications_import_success,
                                imported.updated,
                                imported.unchanged,
                                imported.skipped,
                                imported.failed,
                            ),
                            Toast.LENGTH_LONG,
                        ).show()
                    }.onFailure { failure ->
                        Toast.makeText(
                            context,
                            context.getString(
                                R.string.susfs_applications_import_failed,
                                failure.message.orEmpty().ifBlank { "invalid_config" },
                            ),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                } finally {
                    configBusy = false
                }
            }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                configBusy = true
                try {
                    val result = runCatching {
                        val entries = applications
                            .asSequence()
                            .filter { it.canManage }
                            .map { SusfsApplicationHidingConfigEntry(it.packageName, it.hidden) }
                            .toList()
                        writeSusfsApplicationHidingConfig(
                            context,
                            uri,
                            encodeSusfsApplicationHidingConfig(entries),
                        )
                    }
                    Toast.makeText(
                        context,
                        if (result.isSuccess) {
                            R.string.susfs_applications_export_success
                        } else {
                            R.string.susfs_applications_export_failed
                        },
                        Toast.LENGTH_LONG,
                    ).show()
                } finally {
                    configBusy = false
                }
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    val category = if (selectedCategory == 0) {
        SusfsApplicationCategory.Risk
    } else {
        SusfsApplicationCategory.Normal
    }
    val normalizedQuery = query.trim()
    val displayedApplications = remember(applications, showSystemApps) {
        if (showSystemApps) applications else applications.filterNot { it.isSystem }
    }
    val filtered = remember(displayedApplications, category, normalizedQuery) {
        displayedApplications.filter { app ->
            val categoryMatches = app.isRisk == (category == SusfsApplicationCategory.Risk)
            val queryMatches = normalizedQuery.isBlank() ||
                app.label.contains(normalizedQuery, ignoreCase = true) ||
                app.packageName.contains(normalizedQuery, ignoreCase = true) ||
                app.uid.toString().contains(normalizedQuery)
            categoryMatches && queryMatches
        }
    }
    val hiddenCount = displayedApplications.count { it.hidden }
    val systemAppCount = applications.count { it.isSystem }

    Scaffold(
        containerColor = immersivePageColor(MaterialTheme.colorScheme.background),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.susfs_applications_title)) },
                navigationIcon = {
                    IconButton(onClick = dropUnlessResumed { navigator.pop() }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = dropUnlessResumed { refresh(showLoading = false) },
                        enabled = !refreshing && updatingUid == null && !configBusy,
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.susfs_applications_refresh),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = immersiveTopBarColor(MaterialTheme.colorScheme.background),
                    scrolledContainerColor = immersiveScrolledTopBarColor(MaterialTheme.colorScheme.surface),
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null)
                        Text(
                            text = stringResource(R.string.susfs_applications_summary),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SusfsAppMetric(
                        value = displayedApplications.count { it.isRisk }.toString(),
                        label = stringResource(R.string.susfs_applications_risk_count),
                        modifier = Modifier.weight(1f),
                    )
                    SusfsAppMetric(
                        value = displayedApplications.count { !it.isRisk }.toString(),
                        label = stringResource(R.string.susfs_applications_normal_count),
                        modifier = Modifier.weight(1f),
                    )
                    SusfsAppMetric(
                        value = hiddenCount.toString(),
                        label = stringResource(R.string.susfs_applications_hidden_count),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(R.string.susfs_applications_show_system),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = stringResource(
                                    R.string.susfs_applications_show_system_summary,
                                    systemAppCount,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = showSystemApps,
                            onCheckedChange = { showSystemApps = it },
                            enabled = !loading && !configBusy,
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                        enabled = !configBusy && updatingUid == null,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.FileUpload, contentDescription = null)
                        Text(
                            text = stringResource(R.string.susfs_applications_import),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    OutlinedButton(
                        onClick = { exportLauncher.launch("stersu-susfs-app-hiding.json") },
                        enabled = !loading && !configBusy && updatingUid == null,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = null)
                        Text(
                            text = stringResource(R.string.susfs_applications_export),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.susfs_applications_search)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )
            }

            item {
                TabRow(selectedTabIndex = selectedCategory) {
                    Tab(
                        selected = selectedCategory == 0,
                        onClick = { selectedCategory = 0 },
                        text = {
                            Text(
                                stringResource(
                                    R.string.susfs_applications_risk_tab,
                                    categoryCountFor(displayedApplications, true),
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        icon = { Icon(Icons.Rounded.Security, contentDescription = null) },
                    )
                    Tab(
                        selected = selectedCategory == 1,
                        onClick = { selectedCategory = 1 },
                        text = {
                            Text(
                                stringResource(
                                    R.string.susfs_applications_normal_tab,
                                    categoryCountFor(displayedApplications, false),
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        icon = { Icon(Icons.Rounded.Apps, contentDescription = null) },
                    )
                }
            }

            if (error.isNotBlank()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.ErrorOutline, contentDescription = null)
                            Text(
                                text = stringResource(R.string.susfs_applications_load_failed, error),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            if (loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (filtered.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(Icons.Rounded.Apps, contentDescription = null, modifier = Modifier.size(32.dp))
                            Text(
                                text = stringResource(R.string.susfs_applications_empty),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            } else {
                items(filtered, key = { it.packageName }) { app ->
                    SusfsApplicationRow(
                        app = app,
                        busy = configBusy || updatingUid == app.uid,
                        onToggle = { hidden ->
                            if (updatingUid == null) {
                                scope.launch {
                                    updatingUid = app.uid
                                    try {
                                        val result = setSusfsApplicationHidden(app, hidden)
                                        val cause = result.exceptionOrNull()
                                        if (cause == null) {
                                            error = ""
                                            Toast.makeText(
                                                context,
                                                if (hidden) {
                                                    R.string.susfs_applications_hidden_success
                                                } else {
                                                    R.string.susfs_applications_visible_success
                                                },
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                            runCatching { loadSusfsApplications(context) }
                                                .onSuccess { applications = it }
                                                .onFailure {
                                                    error = it.message.orEmpty().ifBlank { "load_failed" }
                                                }
                                        } else {
                                            error = cause.message.orEmpty().ifBlank { "profile_update_failed" }
                                        }
                                    } finally {
                                        updatingUid = null
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun categoryCountFor(applications: List<SusfsApplication>, risk: Boolean): Int =
    applications.count { it.isRisk == risk }

@Composable
private fun SusfsAppMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SusfsApplicationRow(
    app: SusfsApplication,
    busy: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val riskDetails = when {
        SusfsRiskSignal.RootOrIntegrityName in app.riskSignals &&
            SusfsRiskSignal.SensitivePermission in app.riskSignals -> {
            stringResource(R.string.susfs_applications_risk_name) + " · " +
                stringResource(R.string.susfs_applications_risk_permission)
        }
        SusfsRiskSignal.RootOrIntegrityName in app.riskSignals ->
            stringResource(R.string.susfs_applications_risk_name)
        SusfsRiskSignal.SensitivePermission in app.riskSignals ->
            stringResource(R.string.susfs_applications_risk_permission)
        else -> ""
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIconImage(
                    packageInfo = app.packageInfo,
                    label = app.label,
                    modifier = Modifier.size(44.dp),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        app.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (app.isSystem) {
                            stringResource(R.string.susfs_applications_uid_system, app.uid)
                        } else {
                            stringResource(R.string.susfs_applications_uid, app.uid)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (app.packageNames.size > 1) {
                        Text(
                            stringResource(
                                R.string.susfs_applications_shared_uid,
                                app.packageNames.size - 1,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Switch(
                        checked = app.hidden,
                        onCheckedChange = onToggle,
                        enabled = app.canManage && !app.allowSu && !busy,
                    )
                    Text(
                        text = if (app.hidden) {
                            stringResource(R.string.susfs_applications_hidden)
                        } else {
                            stringResource(R.string.susfs_applications_visible)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (app.hidden) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            if (app.isRisk || app.allowSu || !app.canManage) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = when {
                        app.allowSu -> stringResource(R.string.susfs_applications_root_allowed)
                        !app.canManage -> stringResource(R.string.susfs_applications_system_disabled)
                        else -> riskDetails
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (app.allowSu || !app.canManage) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

private suspend fun readSusfsApplicationHidingConfig(context: Context, uri: Uri): String =
    withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: error("open_input_failed")
        input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                total += count
                require(total <= SUSFS_APP_CONFIG_FILE_MAX_BYTES) { "config_too_large" }
                output.write(buffer, 0, count)
            }
            output.toString(Charsets.UTF_8.name())
        }
    }

private suspend fun writeSusfsApplicationHidingConfig(
    context: Context,
    uri: Uri,
    json: String,
) = withContext(Dispatchers.IO) {
    val output = context.contentResolver.openOutputStream(uri, "wt") ?: error("open_output_failed")
    output.bufferedWriter(Charsets.UTF_8).use { writer ->
        writer.write(json)
        writer.newLine()
    }
}

private const val SUSFS_APP_CONFIG_FILE_MAX_BYTES = 512 * 1024
