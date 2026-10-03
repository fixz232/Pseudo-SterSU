package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.util.SidebarNavigationPosition
import me.weishu.kernelsu.ui.util.SidebarWidgetType

/** Both panes meet the top edge without a rounded notch, on either side. */
internal fun sidebarPaneShape(edgeAtStart: Boolean) = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = 0.dp,
    bottomStart = if (edgeAtStart) 16.dp else 0.dp,
    bottomEnd = if (edgeAtStart) 0.dp else 16.dp,
)

internal data class SidebarRailSizing(val contentHeight: Dp, val widgetHeight: Dp)

internal fun sidebarMinimumWidgetHeight(type: SidebarWidgetType, tablet: Boolean, fontScale: Float): Dp {
    val base = if (type == SidebarWidgetType.Weather) {
        if (tablet) 200.dp else 176.dp
    } else {
        if (tablet) 144.dp else 112.dp
    }
    return if (type == SidebarWidgetType.Image) base else base * fontScale.coerceAtLeast(1f)
}

internal fun sidebarRailSizing(
    availableHeight: Dp,
    minimumWidgetHeight: Dp,
    navigationCount: Int,
    navigationPosition: SidebarNavigationPosition,
): SidebarRailSizing {
    val navigationHeight = 52.dp * navigationCount.coerceAtLeast(0)
    val contentHeight = maxOf(availableHeight, minimumWidgetHeight + navigationHeight)
    val widgetHeight = if (navigationPosition == SidebarNavigationPosition.Top) {
        minimumWidgetHeight
    } else {
        maxOf(minimumWidgetHeight, (contentHeight - navigationHeight) * 0.85f)
    }
    return SidebarRailSizing(contentHeight, widgetHeight)
}
