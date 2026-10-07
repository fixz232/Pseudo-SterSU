package me.weishu.kernelsu.ui.screen.themestore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.store.StoreScaffold
import me.weishu.kernelsu.ui.component.store.StoreSectionHeading
import me.weishu.kernelsu.ui.component.store.StoreExpandableSection
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.util.CloudTheme
import me.weishu.kernelsu.ui.util.CloudThemeCatalogSnapshot
import me.weishu.kernelsu.ui.util.CloudThemeCatalogSource
import me.weishu.kernelsu.ui.util.CloudThemeLocalState
import me.weishu.kernelsu.ui.util.CloudThemeRepository
import me.weishu.kernelsu.ui.util.CloudThemeUsageStatistics
import me.weishu.kernelsu.ui.util.calculateUsageStatistics
import java.text.NumberFormat

@Composable
fun CloudThemeRankingScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val repository = remember(context.applicationContext) {
        CloudThemeRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf<CloudThemeCatalogSnapshot?>(null) }
    var localState by remember { mutableStateOf(repository.readLocalState()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }

    fun refresh(force: Boolean) {
        if (refreshing) return
        refreshing = true
        scope.launch {
            try {
                snapshot = repository.loadCatalog(forceRefresh = force)
                localState = repository.readLocalState()
                loadFailed = false
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                loadFailed = snapshot == null
            } finally {
                loading = false
                refreshing = false
            }
        }
    }

    LaunchedEffect(repository) {
        try {
            snapshot = repository.loadCatalog(forceRefresh = false)
            localState = repository.readLocalState()
            loadFailed = false
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            loadFailed = true
        } finally {
            loading = false
        }
    }
    LifecycleResumeEffect(repository) {
        localState = repository.readLocalState()
        onPauseOrDispose { }
    }

    val statistics = remember(snapshot) {
        snapshot?.catalog?.calculateUsageStatistics()
    }
    val rankedThemes = remember(statistics) {
        statistics?.rankedThemes.orEmpty()
    }
    val onBack = dropUnlessResumed { navigator.pop() }
    val onOpenTheme: (String) -> Unit = { themeId ->
        navigator.push(Route.CloudThemeDetail(themeId))
    }
    val content: @Composable (PaddingValues) -> Unit = { paddingValues ->
        CloudThemeRankingContent(
            snapshot = snapshot,
            statistics = statistics,
            rankedThemes = rankedThemes,
            localState = localState,
            loading = loading,
            refreshing = refreshing,
            loadFailed = loadFailed,
            onRefresh = { refresh(true) },
            onOpenTheme = onOpenTheme,
            modifier = Modifier.padding(paddingValues),
        )
    }

    StoreScaffold(title = stringResource(R.string.cloud_theme_ranking_title),
        onBack = onBack, maxContentWidth = 840.dp, content = content)
}

@Composable
private fun CloudThemeRankingContent(
    snapshot: CloudThemeCatalogSnapshot?,
    statistics: CloudThemeUsageStatistics?,
    rankedThemes: List<CloudTheme>,
    localState: CloudThemeLocalState,
    loading: Boolean,
    refreshing: Boolean,
    loadFailed: Boolean,
    onRefresh: () -> Unit,
    onOpenTheme: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (refreshing) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
        statistics?.let {
            item {
                CloudThemeStatisticsPanel(
                    statistics = it,
                    onRefresh = onRefresh,
                    refreshing = refreshing,
                )
            }
        }
        snapshot?.let { current ->
            if (current.offline || current.source == CloudThemeCatalogSource.Bundled) {
                item { CloudThemeRankingCatalogStatus(current) }
            }
        }

        when {
            loading && statistics == null -> {
                items(4) { CloudThemeRankingLoadingItem() }
            }
            loadFailed && statistics == null -> {
                item { CloudThemeRankingEmpty(error = true, onRetry = onRefresh) }
            }
            statistics != null && rankedThemes.isEmpty() -> {
                item {
                    CloudThemeRankingEmpty(
                        error = false,
                        onRetry = if (snapshot?.offline == true) onRefresh else null,
                    )
                }
            }
            else -> {
                itemsIndexed(rankedThemes, key = { _, theme -> theme.id }) { index, theme ->
                    CloudThemeRankingItem(
                        rank = index + 1,
                        theme = theme,
                        categoryName = snapshot?.catalog?.categoryName(theme.categoryId).orEmpty(),
                        localState = localState,
                        onClick = { onOpenTheme(theme.id) },
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun CloudThemeStatisticsPanel(
    statistics: CloudThemeUsageStatistics,
    onRefresh: () -> Unit,
    refreshing: Boolean,
) {

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StoreSectionHeading(
            title = stringResource(R.string.cloud_theme_ranking_overview),
            subtitle = stringResource(R.string.cloud_theme_ranking_method),
            action = {
                IconButton(enabled = !refreshing, onClick = onRefresh) {
                    Icon(Icons.Rounded.Refresh, stringResource(R.string.cloud_theme_refresh))
                }
            },
        )
        StoreExpandableSection(title = stringResource(R.string.store_redesign_information)) {
            CloudThemeStatistic(
                label = stringResource(R.string.cloud_theme_ranking_total_usage),
                value = NumberFormat.getIntegerInstance().format(statistics.totalUsageCount),
            )
            CloudThemeStatistic(
                label = stringResource(R.string.cloud_theme_ranking_theme_count),
                value = NumberFormat.getIntegerInstance().format(statistics.publishedThemeCount),
            )
            CloudThemeStatistic(
                label = stringResource(R.string.cloud_theme_ranking_creator_count),
                value = NumberFormat.getIntegerInstance().format(statistics.creatorCount),
            )
            CloudThemeStatistic(
                label = stringResource(R.string.cloud_theme_ranking_category_count),
                value = NumberFormat.getIntegerInstance().format(statistics.categoryCount),
            )
        }
    }

}

@Composable
private fun CloudThemeStatistic(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {

    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            color = cloudThemeMutedColor())
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
            color = cloudThemeTextColor())
    }

}

@Composable
private fun CloudThemeRankingItem(
    rank: Int,
    theme: CloudTheme,
    categoryName: String,
    localState: CloudThemeLocalState,
    onClick: () -> Unit,
) {

    Surface(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large, color = cloudThemeSurfaceColor(),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top) {
            Box(Modifier.size(64.dp)) {
                CloudThemeRemoteImage(theme.coverUrl, theme.name,
                    Modifier.fillMaxSize().clip(MaterialTheme.shapes.small), maxSide = 320)
                Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.extraSmall) {
                    Text("#" + rank, Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(theme.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(theme.author.name, style = MaterialTheme.typography.bodySmall,
                    color = cloudThemeMutedColor(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOf(
                    stringResource(R.string.cloud_theme_ranking_usage_count,
                        NumberFormat.getIntegerInstance().format(theme.downloadCount.coerceAtLeast(0L))),
                    categoryName,
                ).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = cloudThemeMutedColor(),
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (localState.record(theme.id) != null || localState.isFavorite(theme.id)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (localState.isFavorite(theme.id)) Icon(Icons.Rounded.Favorite,
                            stringResource(R.string.cloud_theme_remove_favorite), Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        if (localState.record(theme.id) != null) Icon(Icons.Rounded.CheckCircle,
                            stringResource(R.string.cloud_theme_downloaded), Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }


}

@Composable
private fun CloudThemeRankingCatalogStatus(snapshot: CloudThemeCatalogSnapshot) {
    val tint = if (snapshot.errorMessage != null) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(
            text = stringResource(
                if (snapshot.source == CloudThemeCatalogSource.Bundled) {
                    R.string.cloud_theme_bundled_catalog
                } else {
                    R.string.cloud_theme_offline_catalog
                }
            ),
            style = MaterialTheme.typography.bodySmall,
            color = cloudThemeTextColor(),
        )
    }
}

@Composable
private fun CloudThemeRankingLoadingItem() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = cloudThemeSurfaceColor(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(cloudThemeMutedColor().copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            )
            Box(
                Modifier
                    .size(66.dp)
                    .background(cloudThemeMutedColor().copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth(0.72f)
                        .height(16.dp)
                        .background(cloudThemeMutedColor().copy(alpha = 0.13f), RoundedCornerShape(4.dp))
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.48f)
                        .height(11.dp)
                        .background(cloudThemeMutedColor().copy(alpha = 0.09f), RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
private fun CloudThemeRankingEmpty(
    error: Boolean,
    onRetry: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = if (error) Icons.Rounded.ErrorOutline else Icons.Rounded.Image,
            contentDescription = null,
            tint = cloudThemeMutedColor(),
            modifier = Modifier.size(38.dp),
        )
        Text(
            text = stringResource(
                if (error) R.string.cloud_theme_error_title else R.string.cloud_theme_ranking_empty_title
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = cloudThemeTextColor(),
        )
        Text(
            text = stringResource(
                if (error) R.string.cloud_theme_error_summary else R.string.cloud_theme_ranking_empty_summary
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = cloudThemeMutedColor(),
            textAlign = TextAlign.Center,
        )
        onRetry?.let {
            OutlinedButton(onClick = it) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.cloud_theme_retry))
            }
        }
    }
}
