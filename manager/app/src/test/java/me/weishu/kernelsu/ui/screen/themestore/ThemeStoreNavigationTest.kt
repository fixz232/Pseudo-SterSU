package me.weishu.kernelsu.ui.screen.themestore

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeStoreNavigationTest {
    @Test
    fun primaryDestinationsKeepSavedOrdinalsAndGroupStoreCatalogs() {
        assertEquals(
            listOf(
                ThemeStorePage.Overview,
                ThemeStorePage.Customize,
                ThemeStorePage.My,
                ThemeStorePage.Plugins,
                ThemeStorePage.Styles,
            ),
            ThemeStorePage.entries,
        )
        assertEquals(
            listOf(
                ThemeStorePage.Overview,
                ThemeStorePage.Styles,
                ThemeStorePage.Plugins,
                ThemeStorePage.Customize,
                ThemeStorePage.My,
            ),
            themeStoreNavigationPages,
        )
    }
}
