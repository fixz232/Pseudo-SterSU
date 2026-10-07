package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.util.SidebarNavigationPosition
import me.weishu.kernelsu.ui.util.SidebarWidgetType

/** Live panes, controls and settings previews share square corners on either side. */
internal val SidebarPaneShape = RectangleShape

internal data class SidebarRailSizing(val contentHeight: Dp, val widgetHeight: Dp)

/** Icon, single-line label, and padding share the same budget in live and preview rails. */
internal fun sidebarNavigationItemHeight(fontScale: Float): Dp =
    64.dp + 16.dp * (fontScale.coerceAtLeast(1f) - 1f)

internal fun sidebarMinimumWidgetHeight(type: SidebarWidgetType, tablet: Boolean, fontScale: Float): Dp {
    val base = when (type) {
        SidebarWidgetType.Weather -> if (tablet) 208.dp else 192.dp
        SidebarWidgetType.Clock -> if (tablet) 192.dp else 160.dp
        SidebarWidgetType.Alarm -> if (tablet) 144.dp else 112.dp
    }
    // Avatar and padding stay fixed; text widgets grow with the system font size.
    return (if (tablet) 116.dp else 100.dp) + base * fontScale.coerceAtLeast(1f)
}

internal fun sidebarRailSizing(
    availableHeight: Dp,
    minimumWidgetHeight: Dp,
    navigationCount: Int,
    navigationPosition: SidebarNavigationPosition,
    fontScale: Float = 1f,
): SidebarRailSizing {
    val navigationHeight = sidebarNavigationItemHeight(fontScale) * navigationCount.coerceAtLeast(0)
    val contentHeight = maxOf(availableHeight, minimumWidgetHeight + navigationHeight + 0.5.dp)
    val widgetHeight = if (navigationPosition == SidebarNavigationPosition.Top) {
        minimumWidgetHeight
    } else {
        maxOf(minimumWidgetHeight, (contentHeight - navigationHeight - 0.5.dp) * 0.6f)
    }
    return SidebarRailSizing(contentHeight, widgetHeight)
}
