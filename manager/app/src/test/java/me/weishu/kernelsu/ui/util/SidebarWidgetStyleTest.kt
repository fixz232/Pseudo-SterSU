package me.weishu.kernelsu.ui.util

import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SidebarWidgetStyleTest {
    @Test
    fun legacyImageWidgetBecomesClockWhileItsArtworkRemainsAvailableAsAvatar() {
        val config = SidebarWidgetConfig(
            widgetType = SidebarWidgetType.fromValue("image"),
            imageUriString = "content://test/existing-avatar.png",
            imageShape = SidebarImageShape.fromValue("star"),
        ).normalized()
        assertEquals(SidebarWidgetType.Clock, config.widgetType)
        assertEquals("content://test/existing-avatar.png", config.imageUriString)
        assertEquals(SidebarImageShape.Circle, config.imageShape)
    }

    @Test
    fun homeLayoutDefaultsToMaterialAndFallsBackOnUnknownValue() {
        assertEquals(SidebarHomeLayout.Material, SidebarWidgetConfig().homeLayout)
        assertEquals(SidebarHomeLayout.Material, SidebarHomeLayout.fromValue(null))
        assertEquals(SidebarHomeLayout.Material, SidebarHomeLayout.fromValue("unknown"))
        assertEquals(SidebarHomeLayout.StatusCards, SidebarHomeLayout.fromValue("status_cards"))
        assertTrue(isSidebarWidgetPreference("sidebar_widget_home_layout"))
    }

    @Test
    fun changingHomeLayoutPreservesOtherSidebarOptions() {
        val original = SidebarWidgetConfig(
            widgetType = SidebarWidgetType.Clock,
            side = SidebarSide.Right,
            navigationOrder = SIDEBAR_NAVIGATION_IDS.reversed(),
            glassEnabled = false,
        ).normalized()
        val updated = original.copy(homeLayout = SidebarHomeLayout.StatusCards).normalized()
        assertEquals(original, updated.copy(homeLayout = SidebarHomeLayout.Material))
    }

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
        assertEquals(SidebarNavigationPosition.Center, SidebarNavigationPosition.fromValue("broken"))
        assertEquals(SidebarSide.Left, SidebarSide.fromValue(null))
        assertEquals(SidebarSide.Left, SidebarSide.fromValue("broken"))
    }

    @Test
    fun clockStylesRoundTripAndWeatherToggleKeepsOtherOptions() {
        SidebarClockStyle.entries.forEach { style ->
            assertEquals(style, SidebarClockStyle.fromValue(style.value))
        }
        assertEquals(3, SidebarClockStyle.entries.count { it.gradient })
        assertEquals(false, SidebarClockStyle.Stacked.gradient)
        assertEquals(true, SidebarClockStyle.AuroraDigits.gradient)
        assertTrue(SidebarWidgetConfig().clockWeatherEnabled)
        assertTrue(isSidebarWidgetPreference("sidebar_widget_clock_weather_enabled"))
        val original = SidebarWidgetConfig(
            clockStyle = SidebarClockStyle.Dial,
            weatherLabel = "Cloudy",
            weatherTemperature = "21°",
            side = SidebarSide.Right,
        )
        val hidden = original.copy(clockWeatherEnabled = false).normalized()
        assertEquals(false, hidden.clockWeatherEnabled)
        assertEquals(original, hidden.copy(clockWeatherEnabled = true))
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
            widgetType = SidebarWidgetType.Clock,
            imageUriString = "content://test/sidebar.png",
            imageShape = SidebarImageShape.Diamond,
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
