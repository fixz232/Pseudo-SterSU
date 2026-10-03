package me.weishu.kernelsu.ui.component.material

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle

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
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = if (LocalInterfaceStyle.current == InterfaceStyle.SidebarWidget.value) {
            MaterialTheme.colorScheme.surface
        } else containerColor,
        contentColor = contentColor,
        contentWindowInsets = contentWindowInsets,
        content = content,
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
