package me.weishu.kernelsu.ui.screen.themestore

import android.graphics.Bitmap
import android.text.format.Formatter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.store.StoreSectionHeading
import me.weishu.kernelsu.ui.component.store.StoreSearchField
import me.weishu.kernelsu.ui.component.store.StoreFilters
import me.weishu.kernelsu.ui.component.store.StoreExpandableSection
import me.weishu.kernelsu.ui.component.store.StoreTag
import me.weishu.kernelsu.ui.util.CloudTheme
import me.weishu.kernelsu.ui.util.CloudThemeCatalogSnapshot
import me.weishu.kernelsu.ui.util.CloudThemeCatalogSource
import me.weishu.kernelsu.ui.util.CloudThemeLocalRecord
import me.weishu.kernelsu.ui.util.CloudThemePublicationStatus
import me.weishu.kernelsu.ui.util.CloudThemeRepository
import me.weishu.kernelsu.ui.util.loadCloudThemeImage
import java.text.DateFormat
import java.util.Date

private enum class CloudThemeDiscoverFilter {
    All,
    Featured,
    Latest,
    Updates,
    Favorites,
}

@Composable
internal fun CloudThemeDiscoverContent(
    onOpenTheme: (String) -> Unit,
    onOpenRanking: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repository = remember(context.applicationContext) {
        CloudThemeRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf<CloudThemeCatalogSnapshot?>(null) }
    var localState by remember { mutableStateOf(repository.readLocalState()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(CloudThemeDiscoverFilter.All) }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }

    fun refresh(force: Boolean) {
        if (refreshing) return
        refreshing = true
        scope.launch {
            try {
                snapshot = repository.loadCatalog(forceRefresh = force)
                localState = repository.readLocalState()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                // Keep the last usable catalog visible; a null snapshot exposes the retry state.
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
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            snapshot = null
        } finally {
            loading = false
        }
    }
    LifecycleResumeEffect(repository) {
        localState = repository.readLocalState()
        onPauseOrDispose { }
    }

    val visibleThemes = remember(snapshot, localState, query, filter, categoryId) {
        val normalizedQuery = query.trim().lowercase()
        val all = snapshot?.catalog?.themes
            .orEmpty()
            .asSequence()
            .filter { it.status == CloudThemePublicationStatus.Published }
            .filter { categoryId == null || it.categoryId == categoryId }
            .filter { theme ->
                normalizedQuery.isBlank() || listOf(
                    theme.name,
                    theme.author.name,
                    theme.description,
                    theme.tags.joinToString(" "),
                ).any { normalizedQuery in it.lowercase() }
            }
            .filter { theme ->
                when (filter) {
                    CloudThemeDiscoverFilter.All,
                    CloudThemeDiscoverFilter.Latest -> true
                    CloudThemeDiscoverFilter.Featured -> theme.featured
                    CloudThemeDiscoverFilter.Updates -> {
                        val record = localState.record(theme.id)
                        record != null && (
                            record.versionCode < theme.versionCode ||
                                (record.versionCode == theme.versionCode && record.sha256 != theme.sha256)
                            )
                    }
                    CloudThemeDiscoverFilter.Favorites -> localState.isFavorite(theme.id)
                }
            }
            .sortedWith(
                if (filter == CloudThemeDiscoverFilter.Latest) {
                    compareByDescending(CloudTheme::publishedAt)
                } else {
                    compareByDescending<CloudTheme> { it.featured }
                        .thenByDescending(CloudTheme::publishedAt)
                }
            )
            .toList()
        if (filter == CloudThemeDiscoverFilter.Latest) all.take(24) else all
    }
    val themeCard: @Composable (CloudTheme) -> Unit = { theme ->
        CloudThemeCard(
            theme = theme,
            categoryName = snapshot?.catalog?.categoryName(theme.categoryId).orEmpty(),
            record = localState.record(theme.id),
            isActive = localState.isActive(theme.id),
            favorite = localState.isFavorite(theme.id),
            onFavorite = { favorite ->
                scope.launch {
                    localState = withContext(Dispatchers.IO) {
                        repository.setFavorite(theme.id, favorite)
                    }
                }
            },
            onClick = { onOpenTheme(theme.id) },
        )
    }

    LazyVerticalGrid(
        modifier = modifier.fillMaxSize(),
        columns = GridCells.Adaptive(minSize = 280.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "heading", span = { GridItemSpan(maxLineSpan) }) {
            CloudThemeDiscoverHeader(snapshot, refreshing, onRefresh = { refresh(true) })
        }
        item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
            StoreSearchField(query, { query = it }, stringResource(R.string.cloud_theme_search_hint))
        }
        item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
            CloudThemeFilters(selected = filter, onSelected = { filter = it })
        }
        snapshot?.catalog?.categories?.takeIf { it.isNotEmpty() }?.let { categories ->
            item(key = "categories", span = { GridItemSpan(maxLineSpan) }) {
                StoreExpandableSection(
                    title = stringResource(R.string.store_redesign_filter_category),
                    summary = categories.firstOrNull { it.id == categoryId }?.name
                        ?: stringResource(R.string.cloud_theme_category_all),
                ) {
                    StoreFilters(
                        labels = listOf(stringResource(R.string.cloud_theme_category_all)) + categories.map { it.name },
                        selectedIndex = categories.indexOfFirst { it.id == categoryId } + 1,
                        onSelect = { categoryId = if (it == 0) null else categories[it - 1].id },
                    )
                }
            }
        }
        item(key = "results", span = { GridItemSpan(maxLineSpan) }) {
            StoreSectionHeading(
                title = stringResource(R.string.store_redesign_results, visibleThemes.size),
                action = {
                    androidx.compose.material3.TextButton(onClick = onOpenRanking) {
                        Text(stringResource(R.string.cloud_theme_ranking_entry_title))
                    }
                },
            )
        }
        if (loading && snapshot == null) {
            gridItems(listOf(0, 1, 2)) { CloudThemeLoadingCard() }
        } else if (snapshot == null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                CloudThemeEmptyState(error = true, onRetry = { refresh(true) })
            }
        } else if (visibleThemes.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                me.weishu.kernelsu.ui.component.store.StoreEmptyState(
                    icon = Icons.Rounded.Search,
                    title = stringResource(R.string.store_redesign_no_results),
                    message = stringResource(R.string.store_redesign_no_results_summary),
                    actionLabel = stringResource(R.string.store_redesign_clear_filters),
                    onAction = { query = ""; categoryId = null; filter = CloudThemeDiscoverFilter.All },
                )
            }
        } else {
            gridItems(visibleThemes, key = CloudTheme::id) { themeCard(it) }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun CloudThemeDiscoverHeader(
    snapshot: CloudThemeCatalogSnapshot?,
    refreshing: Boolean,
    onRefresh: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StoreSectionHeading(
            title = stringResource(R.string.store_redesign_themes),
            subtitle = stringResource(R.string.store_redesign_theme_intro),
            action = {
                IconButton(enabled = !refreshing, onClick = onRefresh) {
                    Icon(Icons.Rounded.Refresh, stringResource(R.string.cloud_theme_refresh))
                }
            },
        )
        if (refreshing) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth())
        snapshot?.takeIf { it.offline || it.source == CloudThemeCatalogSource.Bundled }?.let {
            CloudThemeStatusBand(
                icon = Icons.Rounded.CloudOff,
                text = stringResource(if (it.source == CloudThemeCatalogSource.Bundled)
                    R.string.cloud_theme_bundled_catalog else R.string.cloud_theme_offline_catalog),
                isError = it.errorMessage != null,
            )
        }
    }
}

@Composable
private fun CloudThemeFilters(
    selected: CloudThemeDiscoverFilter,
    onSelected: (CloudThemeDiscoverFilter) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(CloudThemeDiscoverFilter.entries, key = { it.name }) { filter ->
            val label = stringResource(
                when (filter) {
                    CloudThemeDiscoverFilter.All -> R.string.cloud_theme_filter_all
                    CloudThemeDiscoverFilter.Featured -> R.string.cloud_theme_filter_featured
                    CloudThemeDiscoverFilter.Latest -> R.string.cloud_theme_filter_latest
                    CloudThemeDiscoverFilter.Updates -> R.string.cloud_theme_filter_updates
                    CloudThemeDiscoverFilter.Favorites -> R.string.cloud_theme_filter_favorites
                }
            )
            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = { Text(label) },
                modifier = Modifier.heightIn(min = 48.dp),
                leadingIcon = when (filter) {
                    CloudThemeDiscoverFilter.Featured -> ({
                        Icon(Icons.Rounded.NewReleases, contentDescription = null, modifier = Modifier.size(16.dp))
                    })
                    CloudThemeDiscoverFilter.Updates -> ({
                        Icon(Icons.Rounded.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                    })
                    CloudThemeDiscoverFilter.Favorites -> ({
                        Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                    })
                    else -> null
                },
            )
        }
    }
}

@Composable
private fun CloudThemeCard(
    theme: CloudTheme,
    categoryName: String,
    record: CloudThemeLocalRecord?,
    isActive: Boolean,
    favorite: Boolean,
    onFavorite: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(1.65f)) {
                CloudThemeRemoteImage(theme.coverUrl, theme.name, Modifier.fillMaxSize(), maxSide = 1200)
                IconButton(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.56f), CircleShape),
                    onClick = { onFavorite(!favorite) },
                ) {
                    Icon(if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        stringResource(if (favorite) R.string.cloud_theme_remove_favorite else R.string.cloud_theme_add_favorite),
                        tint = Color.White)
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(theme.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(theme.author.name, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                CloudThemeRecordBadge(theme, record, isActive)
                Text(theme.description, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(listOf(categoryName, theme.versionName, Formatter.formatShortFileSize(context, theme.sizeBytes))
                    .filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (theme.featured) StoreTag(stringResource(R.string.cloud_theme_featured), emphasized = true)
            }
        }
    }
}

@Composable
private fun CloudThemeRecordBadge(
    theme: CloudTheme,
    record: CloudThemeLocalRecord?,
    isActive: Boolean,
) {
    val (icon, label, tint) = when {
        record?.let {
            it.versionCode < theme.versionCode ||
                (it.versionCode == theme.versionCode && it.sha256 != theme.sha256)
        } == true -> Triple(
            Icons.Rounded.Update,
            stringResource(R.string.cloud_theme_update_available),
            MaterialTheme.colorScheme.tertiary,
        )
        isActive &&
            record?.appliedVersionCode == theme.versionCode &&
            record.appliedSha256 == theme.sha256 -> Triple(
            Icons.Rounded.CheckCircle,
            stringResource(R.string.cloud_theme_applied),
            MaterialTheme.colorScheme.primary,
        )
        record?.versionCode == theme.versionCode && record.sha256 == theme.sha256 -> Triple(
            Icons.Rounded.CheckCircle,
            stringResource(R.string.cloud_theme_downloaded),
            MaterialTheme.colorScheme.primary,
        )
        else -> return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CloudThemeLoadingCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = cloudThemeSurfaceColor(),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(cloudThemeMutedColor().copy(alpha = 0.12f)),
            )
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.55f)
                        .height(18.dp)
                        .background(cloudThemeMutedColor().copy(alpha = 0.13f), RoundedCornerShape(4.dp))
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.86f)
                        .height(12.dp)
                        .background(cloudThemeMutedColor().copy(alpha = 0.09f), RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
private fun CloudThemeEmptyState(
    error: Boolean,
    onRetry: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 42.dp),
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
                if (error) R.string.cloud_theme_error_title else R.string.cloud_theme_empty_title
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = cloudThemeTextColor(),
        )
        Text(
            text = stringResource(
                if (error) R.string.cloud_theme_error_summary else R.string.cloud_theme_empty_summary
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = cloudThemeMutedColor(),
        )
        onRetry?.let {
            OutlinedButton(onClick = it) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text(stringResource(R.string.cloud_theme_retry))
            }
        }
    }
}

@Composable
private fun CloudThemeStatusBand(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isError: Boolean,
) {
    val tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = cloudThemeTextColor())
    }
}

@Composable
internal fun CloudThemeRemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    maxSide: Int = 1400,
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, url, maxSide, context.applicationContext) {
        value = try {
            loadCloudThemeImage(context.applicationContext, url, maxSide)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            null
        }
    }
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        val loadedBitmap = bitmap
        if (loadedBitmap != null) {
            Image(
                bitmap = loadedBitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
internal fun cloudThemeSurfaceColor(): Color = MaterialTheme.colorScheme.surface

@Composable
internal fun cloudThemeTextColor(): Color = MaterialTheme.colorScheme.onSurface

@Composable
internal fun cloudThemeMutedColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant

internal fun formatCloudThemeDate(timestamp: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
}
