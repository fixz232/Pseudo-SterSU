package me.weishu.kernelsu.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SidebarThemeTest {
    @Test
    fun textRemainsReadableOnEveryNeutralSurface() {
        for (dark in listOf(false, true)) {
            for (amoled in listOf(false, true)) {
                val scheme = sidebarColorScheme(if (dark) darkColorScheme() else lightColorScheme(), dark, amoled)
                val surfaces = listOf(
                    scheme.background, scheme.surface, scheme.surfaceDim, scheme.surfaceBright,
                    scheme.surfaceContainerLowest, scheme.surfaceContainerLow, scheme.surfaceContainer,
                    scheme.surfaceContainerHigh, scheme.surfaceContainerHighest, scheme.surfaceVariant,
                )
                for (surface in surfaces) {
                    assertContrast(scheme.onSurface, surface)
                    assertContrast(scheme.onSurfaceVariant, surface)
                    assertEquals(1f, surface.alpha, 0f)
                    assertEquals(surface.red, surface.green, 0f)
                    assertEquals(surface.green, surface.blue, 0f)
                }
                assertContrast(scheme.inverseOnSurface, scheme.inverseSurface)
            }
        }
    }

    @Test
    fun userAccentAndSemanticActionColorsArePreserved() {
        for (base in listOf(lightColorScheme(), darkColorScheme())) {
            for (dark in listOf(false, true)) {
                val scheme = sidebarColorScheme(base, dark, false)
                assertEquals(base.primary, scheme.primary)
                assertEquals(base.onPrimary, scheme.onPrimary)
                assertEquals(base.primaryContainer, scheme.primaryContainer)
                assertEquals(base.onPrimaryContainer, scheme.onPrimaryContainer)
                assertEquals(base.secondary, scheme.secondary)
                assertEquals(base.tertiary, scheme.tertiary)
                assertEquals(base.error, scheme.error)
                assertEquals(base.onError, scheme.onError)
                assertEquals(base.errorContainer, scheme.errorContainer)
                assertEquals(base.onErrorContainer, scheme.onErrorContainer)
            }
        }
    }

    @Test
    fun amoledKeepsPageBlackWithoutFlatteningCards() {
        val scheme = sidebarColorScheme(darkColorScheme(), dark = true, amoled = true)
        assertEquals(Color.Black, scheme.background)
        assertEquals(Color.Black, scheme.surface)
        assertTrue(scheme.surfaceContainer.luminance() > scheme.surface.luminance())
    }

    @Test
    fun amoledFlagDoesNotDarkenLightMode() {
        val base = lightColorScheme()
        assertEquals(
            sidebarColorScheme(base, dark = false, amoled = false).surface,
            sidebarColorScheme(base, dark = false, amoled = true).surface,
        )
    }

    private fun assertContrast(ink: Color, surface: Color) {
        val contrast = (maxOf(ink.luminance(), surface.luminance()) + 0.05f) /
            (minOf(ink.luminance(), surface.luminance()) + 0.05f)
        assertTrue("Text contrast is $contrast for $ink on $surface", contrast >= 4.5f)
    }
}
