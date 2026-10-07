package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.navigation3.Navigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.util.INTERFACE_STYLE_RESULT_KEY
import me.weishu.kernelsu.ui.viewmodel.SettingsViewModel

@Composable
fun SettingPager(
    navigator: Navigator,
    bottomInnerPadding: Dp,
) {
    val context = LocalContext.current
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pageMode = remember { mutableStateOf(readSettingsPageMode(context)) }
    val onPageModeChange: (SettingsPageMode) -> Unit = { mode ->
        pageMode.value = mode
        setSettingsPageMode(context, mode)
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    LaunchedEffect(navigator) {
        navigator.observeResult<String>(INTERFACE_STYLE_RESULT_KEY).collect {
            viewModel.refresh()
            navigator.clearResult(INTERFACE_STYLE_RESULT_KEY)
        }
    }

    val onOpenCategory: (SettingsCategory) -> Unit = { category ->
        navigator.push(Route.SettingsCategory(category.routeValue))
    }
    if (uiState.uiMode == InterfaceStyle.SidebarWidget.value && pageMode.value == SettingsPageMode.Categories) {
        SettingsSidebarScreen(
            uiState = uiState,
            navigator = navigator,
            bottomInnerPadding = bottomInnerPadding,
            onOpenCategory = onOpenCategory,
            onPageModeChange = onPageModeChange,
            onSetColorMode = viewModel::setColorMode,
        )
        return
    }
    when (pageMode.value) {
        SettingsPageMode.Categories -> {
            SettingsHubScreen(
                uiState = uiState,
                bottomInnerPadding = bottomInnerPadding,
                onOpenCategory = onOpenCategory,
                onPageModeChange = onPageModeChange,
                onOpenSidebarDesign = { navigator.push(Route.SidebarWidgetSettings) },
            )
            return
        }
        SettingsPageMode.Overview -> {
            SettingsOverviewScreen(
                uiState = uiState,
                bottomInnerPadding = bottomInnerPadding,
                onOpenCategory = onOpenCategory,
                onPageModeChange = onPageModeChange,
            )
            return
        }
    }
}
