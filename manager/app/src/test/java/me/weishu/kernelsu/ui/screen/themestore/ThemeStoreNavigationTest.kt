package me.weishu.kernelsu.ui.screen.themestore

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeStoreNavigationTest {
    @Test
    fun primaryDestinationsKeepSavedOrdinalsAfterMovingTheStyleStore() {
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
                ThemeStorePage.Customize,
                ThemeStorePage.My,
            ),
            themeStoreNavigationPages,
        )
    }

    // Modifier-order checks supplement compilation, not device rendering and gesture tests.
    @Test
    fun bottomNavigationPaintsBehindTheGestureInsetButKeepsButtonsAboveIt() {
        val screen = File("src/main/java/me/weishu/kernelsu/ui/screen/themestore/ThemeStoreScreen.kt").readText()
        val navigation = screen.substringAfter("internal fun ThemeStoreNavigationBar(")
            .substringBefore("internal fun ThemeStoreNavigationRail(")
        val background = navigation.indexOf(".background(palette.navigationSurface)")
        val safePadding = navigation.indexOf(".navigationBarsPadding()")
        val buttonArea = navigation.indexOf(".heightIn(min = 72.dp)")
        assertTrue(background >= 0 && safePadding > background && buttonArea > safePadding)
    }

    @Test
    fun storeDoesNotApplyBottomInsetOutsideTheNavigationSurfaceOrTwiceToContent() {
        val screen = File("src/main/java/me/weishu/kernelsu/ui/screen/themestore/ThemeStoreScreen.kt").readText()
        val scaffold = File("src/main/java/me/weishu/kernelsu/ui/component/store/StoreDesign.kt").readText()
        assertTrue(scaffold.contains("contentWindowInsets: WindowInsets = WindowInsets.safeDrawing"))
        assertTrue(scaffold.contains("contentWindowInsets = contentWindowInsets"))
        assertTrue(screen.contains("contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)"))
        assertTrue(screen.contains("Modifier.weight(1f).consumeWindowInsets(WindowInsets.navigationBars)"))
        assertTrue(screen.contains("Modifier.fillMaxSize().navigationBarsPadding()"))
        assertTrue(screen.contains("Modifier.weight(1f).navigationBarsPadding()"))
    }
}
