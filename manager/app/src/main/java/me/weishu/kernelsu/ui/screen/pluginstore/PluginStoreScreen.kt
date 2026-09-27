package me.weishu.kernelsu.ui.screen.pluginstore

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.R
import me.weishu.kernelsu.stealth.StealthModeStore
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.screen.themestore.ThemeStoreNavigationBar
import me.weishu.kernelsu.ui.screen.themestore.ThemeStoreNavigationRail
import me.weishu.kernelsu.ui.screen.themestore.ThemeStorePage
import me.weishu.kernelsu.ui.util.InstalledManagerPlugin
import me.weishu.kernelsu.ui.util.InstalledKsudStatus
import me.weishu.kernelsu.ui.util.ManagerPluginCatalogRepository
import me.weishu.kernelsu.ui.util.ManagerPluginCompatibility
import me.weishu.kernelsu.ui.util.ManagerPluginInstaller
import me.weishu.kernelsu.ui.util.ManagerPluginPackage
import me.weishu.kernelsu.ui.util.ManagerPluginRegistry
import me.weishu.kernelsu.ui.util.PluginRemovalResult
import me.weishu.kernelsu.ui.util.PluginCatalogSource
import me.weishu.kernelsu.ui.util.PluginDownloadProgress
import me.weishu.kernelsu.ui.util.PluginDownloadRoute
import me.weishu.kernelsu.ui.util.PluginSlot
import me.weishu.kernelsu.ui.util.checkManagerPluginCompatibility
import me.weishu.kernelsu.ui.util.disableNativeWebManagerAndVerify
import me.weishu.kernelsu.ui.util.getInstalledKsudStatus
import me.weishu.kernelsu.ui.util.hasPluginUpdate
import me.weishu.kernelsu.ui.util.removeManagerPlugin
import me.weishu.kernelsu.ui.webmanager.WebManagerPreferences
import me.weishu.kernelsu.ui.webmanager.WebManagerServer
import java.text.DateFormat
import java.util.Date

@Composable
fun PluginStoreScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val repository = remember { ManagerPluginCatalogRepository(context) }
    val registry = remember { ManagerPluginRegistry(context) }
    val installer = remember { ManagerPluginInstaller(context, registry) }
    var catalog by remember { mutableStateOf<ManagerPluginCatalogSnapshotState>(ManagerPluginCatalogSnapshotState.Loading) }
    var installed by remember { mutableStateOf(emptyList<InstalledManagerPlugin>()) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf<PluginDownloadProgress?>(null) }
    var route by remember { mutableStateOf(PluginDownloadRoute.Accelerator) }
    var details by remember { mutableStateOf<ManagerPluginPackage?>(null) }
    var confirmRemoval by remember { mutableStateOf<ManagerPluginPackage?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var ksudStatus by remember { mutableStateOf<InstalledKsudStatus?>(null) }
    var pluginCompatibilityResolved by remember { mutableStateOf(false) }

    suspend fun readInstalledState(): Pair<List<InstalledManagerPlugin>, InstalledKsudStatus> =
        withContext(Dispatchers.IO) {
            registry.list() to getInstalledKsudStatus()
        }

    LaunchedEffect(registry) {
        val snapshot = readInstalledState()
        installed = snapshot.first
        ksudStatus = snapshot.second
        pluginCompatibilityResolved = true
    }

    fun refresh(force: Boolean) {
        if (refreshing) return
        refreshing = true
        scope.launch {
            if (catalog !is ManagerPluginCatalogSnapshotState.Ready) {
                catalog = ManagerPluginCatalogSnapshotState.Loading
            }
            try {
                val snapshot = try {
                    repository.fetch(forceNetwork = force, route = route)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    catalog = ManagerPluginCatalogSnapshotState.Error(error.safePluginMessageForUi(context))
                    null
                }
                snapshot?.let { catalog = ManagerPluginCatalogSnapshotState.Ready(it) }
                if (!force && snapshot?.source == PluginCatalogSource.Cache && snapshot.stale) {
                    try {
                        catalog = ManagerPluginCatalogSnapshotState.Ready(
                            repository.fetch(forceNetwork = true, route = route),
                        )
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Throwable) {
                        val current = catalog as? ManagerPluginCatalogSnapshotState.Ready
                        if (current != null) {
                            catalog = ManagerPluginCatalogSnapshotState.Ready(
                                current.snapshot.copy(
                                    errorMessage = error.safePluginMessageForUi(context),
                                    stale = true,
                                ),
                            )
                        }
                    }
                }
                val installedState = readInstalledState()
                installed = installedState.first
                ksudStatus = installedState.second
                pluginCompatibilityResolved = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } finally {
                refreshing = false
            }
        }
    }

    fun install(plugin: ManagerPluginPackage) {
        if (busyId != null) return
        busyId = plugin.id
        progress = null
        scope.launch {
            try {
                installer.install(plugin, route) {
                    scope.launch(Dispatchers.Main.immediate) {
                        if (busyId == plugin.id) progress = it
                    }
                }
                val installedState = readInstalledState()
                installed = installedState.first
                ksudStatus = installedState.second
                pluginCompatibilityResolved = true
                Toast.makeText(context, R.string.plugin_store_enabled, Toast.LENGTH_LONG).show()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                Toast.makeText(context, error.safePluginMessageForUi(context), Toast.LENGTH_LONG).show()
            } finally {
                busyId = null
                progress = null
            }
        }
    }

    fun remove(plugin: ManagerPluginPackage) {
        if (busyId != null) return
        busyId = plugin.id
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    removeManagerPlugin(
                        pluginId = plugin.id,
                        stealthEnabled = StealthModeStore.isEnabled(context),
                        stopRemoteManagement = {
                            if (!disableNativeWebManagerAndVerify()) return@removeManagerPlugin false
                            WebManagerPreferences.setAutoStartEnabled(context, false)
                            context.stopService(
                                android.content.Intent(
                                    context,
                                    me.weishu.kernelsu.ui.webmanager.WebManagerService::class.java,
                                ),
                            )
                            WebManagerServer.stop()
                            true
                        },
                        removeRecord = { registry.remove(plugin.id) },
                    )
                }
                when (result) {
                    PluginRemovalResult.Removed -> {
                        val installedState = readInstalledState()
                        installed = installedState.first
                        ksudStatus = installedState.second
                        pluginCompatibilityResolved = true
                        Toast.makeText(context, R.string.plugin_store_removed, Toast.LENGTH_LONG).show()
                    }
                    PluginRemovalResult.NotInstalled -> {
                        val installedState = readInstalledState()
                        installed = installedState.first
                        ksudStatus = installedState.second
                        pluginCompatibilityResolved = true
                    }
                    PluginRemovalResult.RequiresStealthDisabled ->
                        Toast.makeText(context, R.string.plugin_store_disable_stealth_first, Toast.LENGTH_LONG).show()
                    PluginRemovalResult.RemoteManagementStopFailed ->
                        Toast.makeText(context, R.string.plugin_store_stop_failed, Toast.LENGTH_LONG).show()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                Toast.makeText(context, error.safePluginMessageForUi(context), Toast.LENGTH_LONG).show()
            } finally {
                busyId = null
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        refresh(force = false)
        onPauseOrDispose { }
    }

    val onSelectedPage: (ThemeStorePage) -> Unit = { destination ->
        when (destination) {
            ThemeStorePage.Overview -> navigator.replace(Route.ThemeStore)
            ThemeStorePage.Customize -> navigator.replace(Route.ThemeStoreCustomize)
            ThemeStorePage.Plugins -> Unit
            ThemeStorePage.My -> navigator.replace(Route.ThemeStoreTransfer)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 720.dp
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.store_title)) },
                    navigationIcon = {
                        IconButton(onClick = dropUnlessResumed { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = dropUnlessResumed { refresh(force = true) },
                            enabled = busyId == null && !refreshing,
                        ) {
                            Icon(Icons.Rounded.Refresh, stringResource(R.string.plugin_store_refresh))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    ThemeStoreNavigationBar(
                        selectedPage = ThemeStorePage.Plugins,
                        onSelected = onSelectedPage,
                        modifier = Modifier.navigationBarsPadding(),
                    )
                }
            },
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                if (useNavigationRail) {
                    ThemeStoreNavigationRail(
                        selectedPage = ThemeStorePage.Plugins,
                        onSelected = onSelectedPage,
                        modifier = Modifier.navigationBarsPadding(),
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(if (useNavigationRail) Modifier.navigationBarsPadding() else Modifier),
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxSize()
                            .widthIn(max = 960.dp)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = stringResource(R.string.store_tab_plugins),
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        text = stringResource(R.string.plugin_store_security_notice),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = route == PluginDownloadRoute.Accelerator,
                                onClick = { route = PluginDownloadRoute.Accelerator },
                                label = { Text(stringResource(R.string.plugin_store_accelerated)) },
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                            FilterChip(
                                selected = route == PluginDownloadRoute.Direct,
                                onClick = { route = PluginDownloadRoute.Direct },
                                label = { Text(stringResource(R.string.plugin_store_direct)) },
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                        }
                        when (val state = catalog) {
                            ManagerPluginCatalogSnapshotState.Loading -> PluginStoreLoadingState()
                            is ManagerPluginCatalogSnapshotState.Error -> PluginStoreErrorState(
                                message = state.message,
                                onRetry = { refresh(force = true) },
                            )
                            is ManagerPluginCatalogSnapshotState.Ready -> {
                                state.snapshot.errorMessage?.let {
                                    PluginStoreStatusBand(message = it, isError = true)
                                }
                                Text(
                                    text = when (state.snapshot.source) {
                                        PluginCatalogSource.Network -> stringResource(R.string.plugin_store_source_network)
                                        PluginCatalogSource.Cache -> stringResource(R.string.plugin_store_source_cache)
                                        PluginCatalogSource.Bundled -> stringResource(R.string.plugin_store_source_bundled)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (refreshing) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
                                Text(
                                    text = stringResource(
                                        if (state.snapshot.stale) R.string.plugin_store_catalog_stale else R.string.plugin_store_catalog_generated,
                                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                            .format(Date(state.snapshot.catalog.generatedAt)),
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (state.snapshot.stale) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (state.snapshot.catalog.plugins.isEmpty()) {
                                    PluginStoreEmptyState()
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        items(state.snapshot.catalog.plugins, key = { it.id }) { plugin ->
                                            val compatibility = if (pluginCompatibilityResolved) {
                                                checkManagerPluginCompatibility(
                                                    plugin = plugin,
                                                    managerVersionCode = BuildConfig.VERSION_CODE,
                                                    ksudStatus = ksudStatus ?: InstalledKsudStatus(),
                                                )
                                            } else {
                                                null
                                            }
                                            PluginCard(
                                                plugin = plugin,
                                                installed = installed.firstOrNull { it.plugin.id == plugin.id },
                                                updateAvailable = hasPluginUpdate(
                                                    installed.firstOrNull { it.plugin.id == plugin.id },
                                                    plugin,
                                                ),
                                                busy = busyId == plugin.id,
                                                progress = if (busyId == plugin.id) progress else null,
                                                compatibility = compatibility,
                                                compatibilityResolved = pluginCompatibilityResolved,
                                                onDetails = { details = plugin },
                                                onInstall = { install(plugin) },
                                                onRemove = { confirmRemoval = plugin },
                                            )
                                        }
                                        item { Spacer(Modifier.height(24.dp)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    details?.let { plugin ->
        AlertDialog(
            onDismissRequest = { details = null },
            title = { Text(plugin.name) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(plugin.description)
                    Text(stringResource(R.string.plugin_store_builtin_notice))
                    Text(
                        stringResource(
                            R.string.plugin_store_compatibility,
                            plugin.minManagerVersionCode,
                            plugin.minKsudVersionCode,
                        ),
                    )
                    Text(stringResource(R.string.plugin_store_version, plugin.version))
                    Text(stringResource(R.string.plugin_store_instructions), style = MaterialTheme.typography.titleSmall)
                    plugin.instructions.forEachIndexed { index, instruction ->
                        Text(stringResource(R.string.plugin_store_instruction_item, index + 1, instruction))
                    }
                    if (plugin.slots.isNotEmpty()) {
                        Text(stringResource(R.string.plugin_store_capabilities), style = MaterialTheme.typography.titleSmall)
                        plugin.slots
                            .sortedBy { it.id }
                            .forEach { slot ->
                                Text(stringResource(R.string.plugin_store_slot_item, pluginSlotLabel(slot)))
                            }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { details = null }) { Text(stringResource(R.string.close)) } },
        )
    }

    confirmRemoval?.let { plugin ->
        val controlsRemoteManagement = plugin.slots.any {
            it == PluginSlot.MaintenanceWebManager || it == PluginSlot.MaintenanceStealthMode
        }
        AlertDialog(
            onDismissRequest = { if (busyId == null) confirmRemoval = null },
            title = { Text(stringResource(R.string.plugin_store_remove_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.plugin_store_remove_confirm_message, plugin.name))
                    if (controlsRemoteManagement) {
                        Text(
                            stringResource(R.string.plugin_store_remove_remote_warning),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemoval = null }, enabled = busyId == null) {
                    Text(stringResource(R.string.close))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRemoval = null
                        remove(plugin)
                    },
                    enabled = busyId == null,
                ) {
                    Text(stringResource(R.string.plugin_store_remove_action))
                }
            },
        )
    }
}

@Composable
private fun pluginSlotLabel(slot: PluginSlot): String = stringResource(
    when (slot) {
        PluginSlot.ToolboxRescue -> R.string.rescue_protection
        PluginSlot.ToolboxImageTools -> R.string.image_tool_title
        PluginSlot.ToolboxCpuSpoof -> R.string.settings_cpu_spoof
        PluginSlot.ToolboxDeviceIdentity -> R.string.settings_device_identity
        PluginSlot.ToolboxGraphicsRenderer -> R.string.settings_graphics_renderer_tool
        PluginSlot.ToolboxAiChat -> R.string.ai_chat_title
        PluginSlot.MaintenanceWebManager -> R.string.settings_group_web_manager
        PluginSlot.MaintenanceStealthMode -> R.string.stealth_mode_title
        PluginSlot.SuperuserAppIdManager -> R.string.app_id_manager_title
        PluginSlot.SuperuserAppFreeze -> R.string.app_freeze_title
    },
)

private sealed interface ManagerPluginCatalogSnapshotState {
    data object Loading : ManagerPluginCatalogSnapshotState
    data class Ready(val snapshot: me.weishu.kernelsu.ui.util.ManagerPluginCatalogSnapshot) : ManagerPluginCatalogSnapshotState
    data class Error(val message: String) : ManagerPluginCatalogSnapshotState
}

@Composable
private fun PluginStoreLoadingState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(3) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.52f)
                            .height(18.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), RoundedCornerShape(4.dp)),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .height(12.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f), RoundedCornerShape(4.dp)),
                    )
                }
            }
        }
    }
}

@Composable
private fun PluginStoreStatusBand(message: String, isError: Boolean) {
    val color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(
        color = color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Security, contentDescription = null, tint = color)
            Text(message, style = MaterialTheme.typography.bodySmall, color = color)
        }
    }
}

@Composable
private fun PluginStoreErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.Security, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.plugin_store_retry))
        }
    }
}

@Composable
private fun PluginStoreEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Rounded.Download, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.plugin_store_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.plugin_store_empty_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PluginCard(
    plugin: ManagerPluginPackage,
    installed: InstalledManagerPlugin?,
    updateAvailable: Boolean,
    busy: Boolean,
    progress: PluginDownloadProgress?,
    compatibility: ManagerPluginCompatibility?,
    compatibilityResolved: Boolean,
    onDetails: () -> Unit,
    onInstall: () -> Unit,
    onRemove: () -> Unit,
) {
    val incompatible = compatibilityResolved && compatibility?.isCompatible == false
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(plugin.name, style = MaterialTheme.typography.titleMedium)
            Text(plugin.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (installed != null) Icon(Icons.Rounded.CheckCircle, contentDescription = stringResource(R.string.plugin_store_installed), tint = MaterialTheme.colorScheme.primary)
            }
            if (installed != null) {
                Text(
                    text = stringResource(
                        if (updateAvailable) R.string.plugin_store_version_update else R.string.plugin_store_version_installed,
                        installed.plugin.version,
                        plugin.version,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (incompatible) {
                Text(
                    text = stringResource(R.string.plugin_store_incompatible),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (progress != null) LinearProgressIndicator(progress = { progress.fraction ?: 0f }, modifier = Modifier.fillMaxWidth())
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onDetails,
                    enabled = !busy,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.plugin_store_details)) }
                if (installed == null || updateAvailable) {
                    Button(
                        onClick = onInstall,
                        enabled = !busy && compatibilityResolved && !incompatible,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icon(
                            if (updateAvailable) Icons.Rounded.Update else Icons.Rounded.Download,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(if (updateAvailable) R.string.plugin_store_update else R.string.plugin_store_enable))
                    }
                }
                if (installed != null) {
                    OutlinedButton(
                        onClick = onRemove,
                        enabled = !busy,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.plugin_store_remove))
                    }
                }
            }
        }
    }
}

private fun Throwable.safePluginMessageForUi(context: android.content.Context): String {
    val detail = message
    ?.lineSequence()
    ?.firstOrNull()
    ?.trim()
    ?.take(160)
    ?.ifBlank { null }
    ?: context.getString(R.string.plugin_store_error_generic_detail)
    val normalized = detail.lowercase()
    val resource = when {
        listOf("requires", "incompatible", "newer manager", "newer ksud").any(normalized::contains) ->
            R.string.plugin_store_error_compatibility
        listOf("sha-256", "signature", "metadata", "schema", "redirect", "incomplete", "too large").any(normalized::contains) ->
            R.string.plugin_store_error_integrity
        listOf("http", "timeout", "connect", "network", "host", "dns").any(normalized::contains) ->
            R.string.plugin_store_error_network
        else -> R.string.plugin_store_error_generic
    }
    return context.getString(resource, detail)
}
