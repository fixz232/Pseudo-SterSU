package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.util.SidebarNavigationPosition
import me.weishu.kernelsu.ui.util.SidebarWidgetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SidebarLayoutTest {
    @Test
    fun bothTopCornersAreSquareOnEitherSideAndInEitherDirection() {
        for (atStart in listOf(true, false)) {
            for (direction in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
                // Inspect corner sizes without invoking Android's Path/RectF stubs.
                val shape = sidebarPaneShape(atStart)
                val size = Size(80f, 600f)
                val density = Density(1f)
                assertEquals(0f, shape.topStart.toPx(size, density), 0f)
                assertEquals(0f, shape.topEnd.toPx(size, density), 0f)
                val leftIsInner = atStart == (direction == LayoutDirection.Ltr)
                val left = if (direction == LayoutDirection.Ltr) shape.bottomStart else shape.bottomEnd
                val right = if (direction == LayoutDirection.Ltr) shape.bottomEnd else shape.bottomStart
                assertEquals(if (leftIsInner) 16f else 0f, left.toPx(size, density), 0f)
                assertEquals(if (leftIsInner) 0f else 16f, right.toPx(size, density), 0f)
            }
        }
    }

    @Test
    fun avatarAndTextWidgetsHaveRoomAtLargeFontSizes() {
        assertEquals(420.dp, sidebarMinimumWidgetHeight(SidebarWidgetType.Clock, false, 2f))
        assertEquals(404.dp, sidebarMinimumWidgetHeight(SidebarWidgetType.Alarm, true, 2f))
        assertEquals(484.dp, sidebarMinimumWidgetHeight(SidebarWidgetType.Weather, false, 2f))
        assertEquals(260.dp, sidebarMinimumWidgetHeight(SidebarWidgetType.Clock, false, 0.8f))
    }

    @Test
    fun shortLandscapeRailScrollsWithoutClippingWidgetsOrDestinations() {
        for (position in SidebarNavigationPosition.entries) {
            val sizing = sidebarRailSizing(240.dp, 176.dp, 5, position)
            assertEquals(496.5.dp, sizing.contentHeight)
            assertEquals(176.dp, sizing.widgetHeight)
            assertTrue(sizing.contentHeight - sizing.widgetHeight >= 320.dp)
        }
    }

    @Test
    fun bodyRemainsScrollableAfterTheFixedPowerFooterIsReserved() {
        val availableBody = 600.dp - 76.dp
        val minimumHeader = sidebarMinimumWidgetHeight(SidebarWidgetType.Clock, false, 2f)
        val sizing = sidebarRailSizing(availableBody, minimumHeader, 5, SidebarNavigationPosition.Center, fontScale = 2f)
        assertTrue(sizing.contentHeight > availableBody)
        assertTrue(sizing.widgetHeight >= minimumHeader)
        assertTrue(sizing.contentHeight - sizing.widgetHeight >= 400.dp)
    }

    @Test
    fun topNavigationKeepsWidgetCompact() {
        val sizing = sidebarRailSizing(800.dp, 144.dp, 5, SidebarNavigationPosition.Top)
        assertEquals(144.dp, sizing.widgetHeight)
        assertEquals(800.dp, sizing.contentHeight)
    }

    @Test
    fun largeTextWeatherAndChangingNavigationCountsKeepAllTargetsReachable() {
        for (count in 1..5) {
            for (position in SidebarNavigationPosition.entries) {
                val sizing = sidebarRailSizing(400.dp, 400.dp, count, position)
                assertTrue(sizing.widgetHeight >= 400.dp)
                assertTrue(sizing.contentHeight - sizing.widgetHeight >= 64.dp * count)
            }
        }
    }

    @Test
    fun navigationLabelsScaleWithoutShrinkingTouchTargets() {
        assertEquals(64.dp, sidebarNavigationItemHeight(0.85f))
        assertEquals(64.dp, sidebarNavigationItemHeight(1f))
        assertEquals(80.dp, sidebarNavigationItemHeight(2f))
        for (fontScale in listOf(1f, 1.3f, 2f, 3f)) {
            for (position in SidebarNavigationPosition.entries) {
                val sizing = sidebarRailSizing(320.dp, 260.dp, 5, position, fontScale)
                assertTrue(sizing.widgetHeight >= 260.dp)
                assertTrue(sizing.contentHeight - sizing.widgetHeight >= sidebarNavigationItemHeight(fontScale) * 5)
            }
        }
    }
}
