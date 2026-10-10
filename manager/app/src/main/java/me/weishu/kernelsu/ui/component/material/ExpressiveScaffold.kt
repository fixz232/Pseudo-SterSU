package me.weishu.kernelsu.ui.component.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.theme.SidebarUiTokens

@Composable
fun ExpressiveScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    contentMaxWidth: Dp = SidebarUiTokens.ContentMaxWidth,
    content: @Composable (PaddingValues) -> Unit,
) {
    val sidebar = LocalInterfaceStyle.current == InterfaceStyle.SidebarWidget.value
    Scaffold(
        modifier = modifier,
        topBar = {
            if (sidebar) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.widthIn(max = contentMaxWidth).fillMaxWidth()) {
                        topBar()
                    }
                }
            } else topBar()
        },
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = if (sidebar) {
            MaterialTheme.colorScheme.surface
        } else containerColor,
        contentColor = contentColor,
        contentWindowInsets = contentWindowInsets,
        content = { padding ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = contentMaxWidth).fillMaxSize()) {
                    content(padding)
                }
            }
        },
    )
}

@Composable
fun expressiveTopAppBarColors(
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    scrolledContainerColor: Color = containerColor,
): TopAppBarColors {
    val sidebar = LocalInterfaceStyle.current == InterfaceStyle.SidebarWidget.value
    return TopAppBarDefaults.topAppBarColors(
        containerColor = if (sidebar) MaterialTheme.colorScheme.surface else containerColor,
        scrolledContainerColor = if (sidebar) MaterialTheme.colorScheme.surface else scrolledContainerColor,
    )
}

/** Compact, consistent page chrome beside a rail; preserve other styles' large headers. */
@Composable
fun ExpressiveTopAppBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = expressiveTopAppBarColors(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    if (LocalInterfaceStyle.current == InterfaceStyle.SidebarWidget.value) {
        TopAppBar(
            title = title, navigationIcon = navigationIcon, actions = actions,
            colors = colors, windowInsets = windowInsets, scrollBehavior = scrollBehavior,
        )
    } else {
        LargeFlexibleTopAppBar(
            title = title, navigationIcon = navigationIcon, actions = actions,
            colors = colors, windowInsets = windowInsets, scrollBehavior = scrollBehavior,
        )
    }
}

@Composable
fun rememberExpressivePageScrollBehavior(): TopAppBarScrollBehavior {
    val state = rememberTopAppBarState()
    return if (LocalInterfaceStyle.current == InterfaceStyle.SidebarWidget.value) {
        TopAppBarDefaults.pinnedScrollBehavior(state)
    } else {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(state)
    }
}
