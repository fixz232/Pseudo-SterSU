package me.weishu.kernelsu.ui.screen.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.component.pixel.PixelStyle
import me.weishu.kernelsu.ui.component.snow.SeasonStyle
import me.weishu.kernelsu.ui.component.snow.snowMiuixCardSurface
import me.weishu.kernelsu.ui.theme.LocalUiPassthroughSettings
import me.weishu.kernelsu.ui.theme.SidebarUiTokens
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor

@StringRes
internal fun settingsHomeStyleLabelRes(state: SettingsUiState): Int = when (state.uiMode) {
    InterfaceStyle.Snow.value -> SeasonStyle.fromValue(state.seasonStyle).labelRes
    InterfaceStyle.Pixel.value -> PixelStyle.fromValue(state.pixelStyle).labelRes
    else -> InterfaceStyle.entries.first { it.value == InterfaceStyle.normalizeValue(state.uiMode) }.labelRes
}

/** Share the beautified settings geometry while retaining each style's surface artwork. */
@Composable
internal fun SettingsHomeCard(
    modifier: Modifier = Modifier,
    shape: Shape = SidebarUiTokens.Shapes.large,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val themed = LocalInterfaceStyle.current == InterfaceStyle.Snow.value ||
        LocalInterfaceStyle.current == InterfaceStyle.Pixel.value
    val decorate = themed && !LocalUiPassthroughSettings.current.enabled
    Surface(
        modifier = modifier.fillMaxWidth().snowMiuixCardSurface(shape = shape, enabled = decorate),
        shape = shape,
        color = if (decorate) Color.Transparent else immersiveSurfaceColor(color),
        contentColor = if (decorate) MaterialTheme.colorScheme.onSurface else contentColor,
    ) {
        Column(content = content)
    }
}
