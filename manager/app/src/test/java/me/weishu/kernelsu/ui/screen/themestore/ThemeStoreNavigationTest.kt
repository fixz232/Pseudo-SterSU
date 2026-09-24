package me.weishu.kernelsu.ui.screen.themestore

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeStoreNavigationTest {
    @Test
    fun primaryDestinationsStayFocusedOnThreeTasks() {
        assertEquals(
            listOf(
                ThemeStorePage.Overview,
                ThemeStorePage.Customize,
                ThemeStorePage.My,
            ),
            ThemeStorePage.entries,
        )
    }
}
