package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Outline
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
    fun allCornersStaySquareAtPhoneTabletAndPreviewSizesInEitherDirection() {
        for (size in listOf(Size(80f, 600f), Size(96f, 900f), Size(116f, 240f))) {
            for (direction in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
                for (scale in listOf(1f, 2f, 3f)) {
                    assertEquals(
                        Outline.Rectangle(Rect(0f, 0f, size.width, size.height)),
                        SidebarPaneShape.createOutline(size, direction, Density(scale)),
                    )
                }
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
