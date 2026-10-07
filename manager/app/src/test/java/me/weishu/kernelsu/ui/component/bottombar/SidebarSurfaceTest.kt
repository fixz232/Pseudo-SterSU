package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SidebarSurfaceTest {
    @Test
    fun lightAndDarkTokensAreOpaqueAndMonochrome() {
        for (dark in listOf(false, true)) {
            val colors = sidebarColors(dark)
            for (color in listOf(colors.background, colors.selection, colors.content, colors.divider)) {
                assertEquals(1f, color.alpha, 0f)
                assertEquals(color.red, color.green, 0f)
                assertEquals(color.green, color.blue, 0f)
            }
        }
    }

    @Test
    fun everyInteractionStateKeepsTextReadableInBothThemes() {
        for (dark in listOf(false, true)) {
            val colors = sidebarColors(dark)
            for (alpha in listOf(0f, 0.45f, 0.7f, 0.85f, 1f)) {
                val surface = colors.selection.copy(alpha = alpha).compositeOver(colors.background)
                assertContrast(colors.content, surface)
            }
        }
    }

    @Test
    fun selectionAndThemeChangesRemainVisible() {
        assertNotEquals(sidebarColors(false).background, sidebarColors(true).background)
        for (dark in listOf(false, true)) {
            val colors = sidebarColors(dark)
            assertNotEquals(colors.background, colors.selection)
            assertNotEquals(colors.background, colors.divider)
        }
    }

    private fun assertContrast(ink: Color, surface: Color) {
        val brighter = maxOf(ink.luminance(), surface.luminance())
        val darker = minOf(ink.luminance(), surface.luminance())
        val contrast = (brighter + 0.05f) / (darker + 0.05f)
        assertTrue("Text contrast is $contrast", contrast >= 4.5f)
    }
}
