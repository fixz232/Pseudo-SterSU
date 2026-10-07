package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.component.decoration.uiDecoratedCard
import me.weishu.kernelsu.ui.theme.immersiveScrolledTopBarColor

internal val overviewPrimaryCategories = listOf(
    SettingsCategory.Appearance,
    SettingsCategory.HomeAndManager,
    SettingsCategory.RootAndPermissions,
)

internal val overviewQuickCategories = listOf(
    SettingsCategory.MountAndHide,
    SettingsCategory.Toolbox,
    SettingsCategory.WebAndPrivacy,
    SettingsCategory.AppAndMaintenance,
)

@Composable
fun SettingsOverviewScreen(
    uiState: SettingsUiState,
    bottomInnerPadding: Dp,
    onOpenCategory: (SettingsCategory) -> Unit,
    onPageModeChange: (SettingsPageMode) -> Unit,
) {
    val sidebar = uiState.uiMode == InterfaceStyle.SidebarWidget.value
    val scrollBehavior = if (sidebar) {
        TopAppBarDefaults.pinnedScrollBehavior()
    } else {
        TopAppBarDefaults.enterAlwaysScrollBehavior()
    }
    val cardShape = when (uiState.uiMode) {
        InterfaceStyle.Pixel.value -> RoundedCornerShape(6.dp)
        InterfaceStyle.Skrootpro.value -> RoundedCornerShape(12.dp)
        else -> RoundedCornerShape(16.dp)
    }
    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = if (sidebar) MaterialTheme.colorScheme.surface else Color.Transparent,
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Top),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    SettingsPageModeButton(
                        currentMode = SettingsPageMode.Overview,
                        onModeChange = onPageModeChange,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = immersiveScrolledTopBarColor(
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 700.dp
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    end = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + bottomInnerPadding + 20.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item(key = "overview_header") {
                    if (wide) {
                        Row(
                            modifier = Modifier.widthIn(max = 980.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            OverviewIntroCard(cardShape, Modifier.weight(1f))
                            OverviewStyleCard(uiState, cardShape, Modifier.weight(1f))
                        }
                    } else {
                        Column(
                            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            OverviewIntroCard(cardShape, Modifier.fillMaxWidth())
                            OverviewStyleCard(uiState, cardShape, Modifier.fillMaxWidth())
                        }
                    }
                }
                item(key = "overview_categories") {
                    if (wide) {
                        Column(
                            modifier = Modifier.widthIn(max = 980.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            OverviewPrimaryCard(
                                shape = cardShape,
                                onOpenCategory = onOpenCategory,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OverviewQuickSection(
                                uiState = uiState,
                                shape = cardShape,
                                onOpenCategory = onOpenCategory,
                                columns = 4,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            OverviewPrimaryCard(cardShape, onOpenCategory, Modifier.fillMaxWidth())
                            OverviewQuickSection(
                                uiState = uiState,
                                shape = cardShape,
                                onOpenCategory = onOpenCategory,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewIntroCard(shape: RoundedCornerShape, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.uiDecoratedCard(shape),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = shape,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 184.dp).padding(22.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.settings_overview_eyebrow),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.settings_overview_intro),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.settings_overview_intro_summary),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = stringResource(
                    R.string.settings_overview_section_count,
                    SettingsCategory.entries.size,
                ),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun OverviewStyleCard(
    uiState: SettingsUiState,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
) {
    val installedName = uiState.installedInterfaceStyles
        .firstOrNull { uiState.isInterfaceStyleActive(it.style) }
        ?.style?.name
    val styleRes = InterfaceStyle.entries.firstOrNull { it.value == uiState.uiMode }?.labelRes
        ?: R.string.settings_ui_mode
    val activeStyleName = installedName ?: stringResource(styleRes)
    Surface(
        modifier = modifier.uiDecoratedCard(shape),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = shape,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 184.dp).padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_overview_current_style),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = activeStyleName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.settings_overview_style_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OverviewStylePreview()
        }
    }
}

@Composable
private fun OverviewStylePreview() {
    Surface(
        modifier = Modifier.width(78.dp).height(116.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier.padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Surface(
                modifier = Modifier.size(22.dp),
                shape = RoundedCornerShape(7.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Palette, null, Modifier.size(14.dp))
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth().height(22.dp),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) { }
            repeat(2) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(17.dp),
                    shape = RoundedCornerShape(5.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) { }
            }
        }
    }
}

@Composable
private fun OverviewPrimaryCard(
    shape: RoundedCornerShape,
    onOpenCategory: (SettingsCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.uiDecoratedCard(shape),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = shape,
    ) {
        Column {
            overviewPrimaryCategories.forEachIndexed { index, category ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 76.dp, end = 18.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                OverviewPrimaryRow(category, onClick = { onOpenCategory(category) })
            }
        }
    }
}

@Composable
private fun OverviewPrimaryRow(category: SettingsCategory, onClick: () -> Unit) {
    val accent = categoryAccent(category)
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .heightIn(min = 88.dp).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            color = accent.copy(alpha = 0.13f),
            contentColor = accent,
            shape = RoundedCornerShape(14.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(category.icon(), null, Modifier.size(23.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(category.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(category.summaryRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun OverviewQuickSection(
    uiState: SettingsUiState,
    shape: RoundedCornerShape,
    onOpenCategory: (SettingsCategory) -> Unit,
    columns: Int = 2,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.settings_overview_quick_access),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 3.dp),
        )
        overviewQuickCategories.chunked(columns).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pair.forEach { category ->
                    OverviewQuickTile(
                        category = category,
                        itemCount = SettingsCatalog.visibleEntryCount(category, uiState),
                        shape = shape,
                        onClick = { onOpenCategory(category) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewQuickTile(
    category: SettingsCategory,
    itemCount: Int,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = categoryAccent(category)
    Surface(
        modifier = modifier.uiDecoratedCard(shape).clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = shape,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 128.dp).padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = category.icon(),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(27.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(category.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.settings_section_item_count, itemCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
