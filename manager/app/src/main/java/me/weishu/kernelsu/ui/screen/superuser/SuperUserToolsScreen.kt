package me.weishu.kernelsu.ui.screen.superuser

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.theme.immersivePageColor
import me.weishu.kernelsu.ui.theme.immersiveScrolledTopBarColor
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor
import me.weishu.kernelsu.ui.theme.immersiveTopBarColor
import me.weishu.kernelsu.ui.util.ManagerPlugin
import me.weishu.kernelsu.ui.util.ManagerPluginRegistry
import me.weishu.kernelsu.ui.util.getInstalledKsudStatus
import me.weishu.kernelsu.ui.util.resolveCompatiblePluginIds

@Composable
fun SuperUserToolsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val pluginRegistry = remember { ManagerPluginRegistry(context) }
    val scope = rememberCoroutineScope()
    var compatiblePluginIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var compatibilityResolved by remember { mutableStateOf(false) }
    var refreshJob by remember { mutableStateOf<Job?>(null) }

    fun refreshPluginCompatibility() {
        refreshJob?.cancel()
        compatibilityResolved = false
        refreshJob = scope.launch {
            val compatible = runCatching {
                withContext(Dispatchers.IO) {
                    resolveCompatiblePluginIds(
                        installed = pluginRegistry.list(),
                        managerVersionCode = BuildConfig.VERSION_CODE,
                        ksudStatus = getInstalledKsudStatus(),
                    )
                }
            }.getOrElse { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "unable to resolve superuser tool plugins", error)
                emptySet()
            }
            compatiblePluginIds = compatible
            compatibilityResolved = true
        }
    }

    LifecycleResumeEffect(Unit) {
        refreshPluginCompatibility()
        onPauseOrDispose {
            refreshJob?.cancel()
            refreshJob = null
        }
    }

    val appIdInstalled = compatibilityResolved &&
        ManagerPlugin.AppIdManager.id in compatiblePluginIds
    val appFreezeInstalled = compatibilityResolved &&
        ManagerPlugin.AppFreeze.id in compatiblePluginIds
    val navigator = LocalNavigator.current
    val onBack = dropUnlessResumed { navigator.pop() }

    Scaffold(
        containerColor = immersivePageColor(MaterialTheme.colorScheme.background),
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.superuser_app_tools_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = immersiveTopBarColor(MaterialTheme.colorScheme.background),
                    scrolledContainerColor = immersiveScrolledTopBarColor(
                        MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ),
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (appIdInstalled) {
                    SuperUserToolRow(
                        title = stringResource(R.string.app_id_manager_title),
                        summary = stringResource(R.string.superuser_app_tools_id_summary),
                        icon = Icons.Rounded.Fingerprint,
                        onClick = dropUnlessResumed { navigator.push(Route.AppIdManager) },
                    )
                }
                if (appFreezeInstalled) {
                    SuperUserToolRow(
                        title = stringResource(R.string.app_freeze_title),
                        summary = stringResource(R.string.superuser_app_tools_freeze_summary),
                        icon = Icons.Rounded.AcUnit,
                        onClick = dropUnlessResumed { navigator.push(Route.AppFreeze) },
                    )
                }
            }
        }
    }
}

private const val TAG = "SterSU-SuperUserTools"

@Composable
private fun SuperUserToolRow(
    title: String,
    summary: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 76.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(21.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
