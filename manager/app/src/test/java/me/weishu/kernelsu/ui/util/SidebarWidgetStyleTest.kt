package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SidebarWidgetStyleTest {
    @Test
    fun unknownOptionValuesFallBackToSafeDefaults() {
        assertEquals(SidebarWidgetType.Clock, SidebarWidgetType.fromValue("broken"))
        assertEquals(SidebarClockStyle.Stacked, SidebarClockStyle.fromValue("broken"))
        assertEquals(SidebarImageShape.Circle, SidebarImageShape.fromValue("broken"))
        assertEquals(SidebarNavigationPosition.Bottom, SidebarNavigationPosition.fromValue("broken"))
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
