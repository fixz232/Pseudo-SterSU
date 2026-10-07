package me.weishu.kernelsu.ui.component.store

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreColorsTest {
    @Test
    fun customAccentsRemainReadableInBothModes() {
        val accents = listOf(
            Color.Black, Color.White, Color.Red, Color.Green, Color.Blue,
            Color.Yellow, Color.Gray, Color(0xFF46569B), Color(0xFFC2CAFF),
            Color(0x880000FF),
        )
        for (dark in listOf(false, true)) {
            for (accent in accents) {
                val colors = storeColorScheme(dark, accent)
                val pairs = listOf(
                    colors.onBackground to colors.background,
                    colors.onSurface to colors.surface,
                    colors.onSurfaceVariant to colors.surface,
                    colors.onSurfaceVariant to colors.surfaceContainerHigh,
                    colors.primary to colors.background,
                    colors.primary to colors.surface,
                    colors.onPrimary to colors.primary,
                    colors.onPrimaryContainer to colors.primaryContainer,
                )
                pairs.forEach { (foreground, background) ->
                    assertTrue(
                        "Insufficient contrast for dark=$dark accent=$accent: $foreground / $background",
                        storeContrast(foreground, background) >= 4.5f,
                    )
                }
            }
        }
    }

    @Test
    fun readableUserAccentIsPreserved() {
        val lightAccent = Color(0xFF46569B)
        val darkAccent = Color(0xFFC2CAFF)
        assertEquals(lightAccent, storeColorScheme(false, lightAccent).primary)
        assertEquals(darkAccent, storeColorScheme(true, darkAccent).primary)
    }
}
