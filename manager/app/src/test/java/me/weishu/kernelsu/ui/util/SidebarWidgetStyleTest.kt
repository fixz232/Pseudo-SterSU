package me.weishu.kernelsu.ui.util

import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SidebarWidgetStyleTest {
    @Test
    fun sidebarGlassDefaultsToEnabledAndHasItsOwnPreference() {
        assertTrue(SidebarWidgetConfig().normalized().glassEnabled)
        assertTrue(isSidebarWidgetPreference("sidebar_widget_glass_enabled"))
    }

    @Test
    fun changingGlassPreservesWidgetWeatherAndNavigation() {
        val original = SidebarWidgetConfig(
            widgetType = SidebarWidgetType.Weather,
            side = SidebarSide.Right,
            imageUriString = "content://test/sidebar.png",
            navigationPosition = SidebarNavigationPosition.Center,
            navigationOrder = SIDEBAR_NAVIGATION_IDS.reversed(),
            weatherApi = SidebarWeatherConfig(enabled = true, url = "https://weather.test/current"),
        ).normalized()
        val updated = original.copy(glassEnabled = false).normalized()

        assertEquals(false, updated.glassEnabled)
        assertEquals(original, updated.copy(glassEnabled = true))
    }

    @Test
    fun unknownOptionValuesFallBackToSafeDefaults() {
        assertEquals(SidebarWidgetType.Clock, SidebarWidgetType.fromValue("broken"))
        assertEquals(SidebarClockStyle.Stacked, SidebarClockStyle.fromValue("broken"))
        assertEquals(SidebarImageShape.Circle, SidebarImageShape.fromValue("broken"))
        assertEquals(SidebarNavigationPosition.Bottom, SidebarNavigationPosition.fromValue("broken"))
        assertEquals(SidebarSide.Left, SidebarSide.fromValue(null))
        assertEquals(SidebarSide.Left, SidebarSide.fromValue("broken"))
    }

    @Test
    fun sidebarStaysOnTheChosenPhysicalSideInBothLayoutDirections() {
        assertEquals(true, SidebarSide.Left.isAtStart(LayoutDirection.Ltr))
        assertEquals(false, SidebarSide.Left.isAtStart(LayoutDirection.Rtl))
        assertEquals(false, SidebarSide.Right.isAtStart(LayoutDirection.Ltr))
        assertEquals(true, SidebarSide.Right.isAtStart(LayoutDirection.Rtl))
    }

    @Test
    fun changingSidesPreservesWidgetAndNavigationCustomization() {
        val original = SidebarWidgetConfig(
            widgetType = SidebarWidgetType.Image,
            imageUriString = "content://test/sidebar.png",
            imageShape = SidebarImageShape.Star,
            navigationPosition = SidebarNavigationPosition.Center,
            navigationOrder = SIDEBAR_NAVIGATION_IDS.reversed(),
            weatherApi = SidebarWeatherConfig(enabled = true, url = "https://weather.test/current"),
        ).normalized()
        val updated = original.copy(side = SidebarSide.Right).normalized()

        assertEquals(original, updated.copy(side = SidebarSide.Left))
        assertEquals(updated.side, SidebarSide.fromValue(updated.side.value))
        assertTrue(isSidebarWidgetPreference("sidebar_widget_side"))
    }

    @Test
    fun navigationOrderDropsDuplicatesAndRestoresMissingItems() {
        val normalized = normalizeSidebarNavigationOrder(
            requested = listOf(
                SIDEBAR_NAV_SETTINGS,
                SIDEBAR_NAV_HOME,
                SIDEBAR_NAV_HOME,
                "unknown",
            )
        )

        assertEquals(
            listOf(
                SIDEBAR_NAV_SETTINGS,
                SIDEBAR_NAV_HOME,
                SIDEBAR_NAV_KPM,
                SIDEBAR_NAV_SUPERUSER,
                SIDEBAR_NAV_MODULE,
            ),
            normalized,
        )
    }

    @Test
    fun navigationOrderFiltersUnavailableKpmWithoutLosingOtherDestinations() {
        val normalized = normalizeSidebarNavigationOrder(
            requested = listOf(SIDEBAR_NAV_KPM, SIDEBAR_NAV_MODULE, SIDEBAR_NAV_HOME),
            available = listOf(
                SIDEBAR_NAV_HOME,
                SIDEBAR_NAV_SUPERUSER,
                SIDEBAR_NAV_MODULE,
                SIDEBAR_NAV_SETTINGS,
            ),
        )

        assertEquals(
            listOf(
                SIDEBAR_NAV_MODULE,
                SIDEBAR_NAV_HOME,
                SIDEBAR_NAV_SUPERUSER,
                SIDEBAR_NAV_SETTINGS,
            ),
            normalized,
        )
    }
}
