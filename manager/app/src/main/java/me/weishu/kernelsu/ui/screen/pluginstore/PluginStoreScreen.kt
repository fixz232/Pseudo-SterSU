package me.weishu.kernelsu.ui.screen.pluginstore

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import me.weishu.kernelsu.ui.component.store.StoreAlertDialog as AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.store.StoreScaffold
import me.weishu.kernelsu.ui.component.store.StoreSectionHeading
import me.weishu.kernelsu.ui.component.store.StoreSearchField
import me.weishu.kernelsu.ui.component.store.StoreFilters
import me.weishu.kernelsu.ui.component.store.StoreExpandableSection
import me.weishu.kernelsu.ui.component.store.StorePanel
import me.weishu.kernelsu.ui.component.store.StoreTag
import me.weishu.kernelsu.ui.component.store.StoreEmptyState
import androidx.compose.material.icons.rounded.Search
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
import me.weishu.kernelsu.ui.util.ManagerPluginCompatibilityIssue
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
import me.weishu.kernelsu.ui.util.readHiddenPathConfig
import me.weishu.kernelsu.ui.util.setHiddenPathAutoLoad
import me.weishu.kernelsu.ui.util.stopPathmaskPluginForRemoval
import me.weishu.kernelsu.ui.util.unloadHiddenPathKernelPaths
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
    var detailsId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmRemoval by remember { mutableStateOf<ManagerPluginPackage?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var ksudStatus by remember { mutableStateOf<InstalledKsudStatus?>(null) }
    var pluginCompatibilityResolved by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(0) }

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
                        stopPathmask = {
                            stopPathmaskPluginForRemoval(
                                readStatus = ::readHiddenPathConfig,
                                disableAutoLoad = { setHiddenPathAutoLoad(false) },
                                unload = ::unloadHiddenPathKernelPaths,
                            )
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
                    PluginRemovalResult.PathmaskStopFailed ->
                        Toast.makeText(context, R.string.plugin_store_pathmask_stop_failed, Toast.LENGTH_LONG).show()
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
            ThemeStorePage.Styles -> navigator.replace(Route.StoreInterfaceStyles)
            ThemeStorePage.Overview -> navigator.replace(Route.ThemeStore)
            ThemeStorePage.Customize -> navigator.replace(Route.ThemeStoreCustomize)
            ThemeStorePage.Plugins -> Unit
            ThemeStorePage.My -> navigator.replace(Route.ThemeStoreTransfer)
        }
    }

    val details = (catalog as? ManagerPluginCatalogSnapshotState.Ready)
        ?.snapshot
        ?.catalog
        ?.plugins
        ?.firstOrNull { it.id == detailsId }
    BackHandler(enabled = detailsId != null) { detailsId = null }

    val visiblePlugins = remember(catalog, installed, query, selectedFilter) {
        val normalized = query.trim().lowercase()
        (catalog as? ManagerPluginCatalogSnapshotState.Ready)?.snapshot?.catalog?.plugins.orEmpty()
            .filter { plugin ->
                val local = installed.firstOrNull { it.plugin.id == plugin.id }
                (normalized.isBlank() || listOf(plugin.name, plugin.summary, plugin.description)
                    .any { normalized in it.lowercase() }) && when (selectedFilter) {
                    1 -> local != null
                    2 -> hasPluginUpdate(local, plugin)
                    else -> true
                }
            }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 720.dp
        StoreScaffold(
            title = details?.name ?: stringResource(R.string.store_title),
            onBack = { if (detailsId != null) detailsId = null else navigator.pop() },
            actions = {
                if (detailsId == null) IconButton(
                    onClick = { refresh(true) }, enabled = busyId == null && !refreshing,
                ) { Icon(Icons.Rounded.Refresh, stringResource(R.string.plugin_store_refresh)) }
            },
            bottomBar = {
                if (!useNavigationRail && detailsId == null) ThemeStoreNavigationBar(
                    ThemeStorePage.Plugins, onSelectedPage, Modifier.navigationBarsPadding())
            },
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                if (useNavigationRail && detailsId == null) ThemeStoreNavigationRail(
                    ThemeStorePage.Plugins, onSelectedPage, Modifier.navigationBarsPadding())
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    val selectedDetails = details
                    if (selectedDetails != null) {
                        val local = installed.firstOrNull { it.plugin.id == selectedDetails.id }
                        PluginDetailContent(
                            plugin = selectedDetails, installed = local,
                            updateAvailable = hasPluginUpdate(local, selectedDetails),
                            compatibility = if (pluginCompatibilityResolved) checkManagerPluginCompatibility(
                                selectedDetails, BuildConfig.VERSION_CODE, ksudStatus ?: InstalledKsudStatus()) else null,
                            compatibilityResolved = pluginCompatibilityResolved,
                            busy = busyId != null,
                            progress = if (busyId == selectedDetails.id) progress else null,
                            modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
                            onInstall = { install(selectedDetails) },
                            onRemove = { confirmRemoval = selectedDetails },
                        )
                    } else if (detailsId != null) {
                        if (catalog is ManagerPluginCatalogSnapshotState.Loading) {
                            me.weishu.kernelsu.ui.component.store.StoreLoadingItems()
                        } else {
                            StoreEmptyState(Icons.Rounded.Extension,
                                stringResource(R.string.store_redesign_unavailable),
                                stringResource(R.string.store_redesign_unavailable_summary),
                                stringResource(R.string.back), { detailsId = null })
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            item(key = "heading") {
                                StoreSectionHeading(stringResource(R.string.store_tab_plugins),
                                    stringResource(R.string.store_redesign_plugin_intro))
                            }
                            item(key = "search") {
                                StoreSearchField(query, { query = it }, stringResource(R.string.store_redesign_plugin_search))
                            }
                            item(key = "filters") {
                                StoreFilters(listOf(stringResource(R.string.cloud_theme_category_all),
                                    stringResource(R.string.store_redesign_installed), stringResource(R.string.store_redesign_updates)),
                                    selectedFilter, { selectedFilter = it })
                            }
                            item(key = "network") {
                                StoreExpandableSection(stringResource(R.string.store_redesign_network),
                                    stringResource(if (route == PluginDownloadRoute.Accelerator)
                                        R.string.plugin_store_accelerated else R.string.plugin_store_direct)) {
                                    StoreFilters(listOf(stringResource(R.string.plugin_store_accelerated), stringResource(R.string.plugin_store_direct)),
                                        if (route == PluginDownloadRoute.Accelerator) 0 else 1,
                                        { route = if (it == 0) PluginDownloadRoute.Accelerator else PluginDownloadRoute.Direct })
                                    Text(stringResource(R.string.plugin_store_security_notice),
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    (catalog as? ManagerPluginCatalogSnapshotState.Ready)?.snapshot?.let { snapshot ->
                                        Text(stringResource(when (snapshot.source) {
                                            PluginCatalogSource.Network -> R.string.plugin_store_source_network
                                            PluginCatalogSource.Cache -> R.string.plugin_store_source_cache
                                            PluginCatalogSource.Bundled -> R.string.plugin_store_source_bundled
                                        }), style = MaterialTheme.typography.labelMedium)
                                        Text(stringResource(if (snapshot.stale) R.string.plugin_store_catalog_stale else R.string.plugin_store_catalog_generated,
                                            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(snapshot.catalog.generatedAt))),
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            if (refreshing) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                            when (val state = catalog) {
                                ManagerPluginCatalogSnapshotState.Loading -> item { PluginStoreLoadingState() }
                                is ManagerPluginCatalogSnapshotState.Error -> item {
                                    PluginStoreErrorState(state.message) { refresh(true) }
                                }
                                is ManagerPluginCatalogSnapshotState.Ready -> {
                                    state.snapshot.errorMessage?.let { message -> item {
                                        PluginStoreStatusBand(message, isError = true)
                                    } }
                                    item { StoreSectionHeading(stringResource(R.string.store_redesign_results, visiblePlugins.size)) }
                                    if (state.snapshot.catalog.plugins.isEmpty()) item { PluginStoreEmptyState() }
                                    else if (visiblePlugins.isEmpty()) item {
                                        StoreEmptyState(Icons.Rounded.Search, stringResource(R.string.store_redesign_no_results),
                                            stringResource(R.string.store_redesign_no_results_summary),
                                            stringResource(R.string.store_redesign_clear_filters), { query = ""; selectedFilter = 0 })
                                    }
                                    items(visiblePlugins, key = { it.id }) { plugin ->
                                        val local = installed.firstOrNull { it.plugin.id == plugin.id }
                                        PluginCard(plugin, local, hasPluginUpdate(local, plugin), busyId != null,
                                            if (busyId == plugin.id) progress else null,
                                            if (pluginCompatibilityResolved) checkManagerPluginCompatibility(
                                                plugin, BuildConfig.VERSION_CODE, ksudStatus ?: InstalledKsudStatus()) else null,
                                            pluginCompatibilityResolved,
                                            onDetails = { detailsId = plugin.id }, onInstall = { install(plugin) })
                                    }
                                }
                            }
                            item { Spacer(Modifier.height(8.dp)) }
                        }
                    }
                }
            }
        }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PluginDetailContent(
    plugin: ManagerPluginPackage,
    installed: InstalledManagerPlugin?,
    updateAvailable: Boolean,
    compatibility: ManagerPluginCompatibility?,
    compatibilityResolved: Boolean,
    busy: Boolean,
    progress: PluginDownloadProgress?,
    modifier: Modifier = Modifier,
    onInstall: () -> Unit,
    onRemove: () -> Unit,
) {
    val incompatible = compatibilityResolved && compatibility?.isCompatible == false
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            StorePanel {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Extension, null, Modifier.width(44.dp).height(44.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(plugin.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(plugin.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(stringResource(R.string.plugin_store_version, plugin.version),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (installed != null) StoreTag(stringResource(if (updateAvailable) R.string.store_redesign_updates else R.string.plugin_store_installed), true)
                if (incompatible) Text(pluginCompatibilityMessage(compatibility), color = MaterialTheme.colorScheme.error)
                if (installed == null || updateAvailable) {
                    Button(onClick = onInstall, enabled = !busy && compatibilityResolved && !incompatible,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Text(stringResource(if (updateAvailable) R.string.plugin_store_update else R.string.plugin_store_enable))
                    }
                }
                progress?.let {
                    if (it.fraction == null) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    else LinearProgressIndicator(progress = { it.fraction ?: 0f }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StoreSectionHeading(stringResource(R.string.store_redesign_about))
                Text(plugin.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            StoreSectionHeading(stringResource(R.string.plugin_store_instructions))
        }
        items(plugin.instructions.size) { index ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Text((index + 1).toString().padStart(2, '0'), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(28.dp))
                Text(plugin.instructions[index], style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
        }
        item {
            StoreExpandableSection(stringResource(R.string.plugin_store_capabilities)) {
                Text(stringResource(R.string.plugin_store_builtin_notice), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.plugin_store_compatibility, plugin.minManagerVersionCode, plugin.minKsudVersionCode),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    plugin.slots.sortedBy { it.id }.forEach { StoreTag(pluginSlotLabel(it)) }
                }
            }
        }
        if (installed != null) item {
            StoreExpandableSection(stringResource(R.string.store_redesign_manage),
                stringResource(R.string.plugin_store_version, installed.plugin.version)) {
                OutlinedButton(onClick = onRemove, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.Rounded.Delete, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.plugin_store_remove))
                }
            }
        }
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
        PluginSlot.MountHidePathmaskLkm -> R.string.hidden_path_config
    },
)

@Composable
private fun pluginCompatibilityMessage(compatibility: ManagerPluginCompatibility?): String = stringResource(
    if (compatibility?.issue == ManagerPluginCompatibilityIssue.LkmModeRequired) {
        R.string.plugin_store_lkm_required
    } else {
        R.string.plugin_store_incompatible
    }
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
) {
    val incompatible = compatibilityResolved && compatibility?.isCompatible == false
    Surface(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable(onClick = onDetails),
        shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Box(Modifier.width(48.dp).height(48.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Extension, null)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(plugin.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(plugin.summary, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, stringResource(R.string.plugin_store_details))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.plugin_store_version, plugin.version), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (incompatible) Text(pluginCompatibilityMessage(compatibility),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    else if (installed != null) StoreTag(stringResource(if (updateAvailable) R.string.store_redesign_updates
                        else R.string.plugin_store_installed), emphasized = updateAvailable)
                }
                if (installed == null || updateAvailable) {
                    Button(onClick = onInstall, enabled = !busy && compatibilityResolved && !incompatible,
                        modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(if (updateAvailable) R.string.plugin_store_update else R.string.plugin_store_enable))
                    }
                } else TextButton(onClick = onDetails, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.store_redesign_manage))
                }
            }
            progress?.let {
                if (it.fraction == null) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                else LinearProgressIndicator(progress = { it.fraction ?: 0f }, modifier = Modifier.fillMaxWidth())
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
    if ("lkm mode" in normalized) return context.getString(R.string.plugin_store_lkm_required)
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
