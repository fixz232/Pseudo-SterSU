package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import me.weishu.kernelsu.ui.util.SidebarMaterial
import me.weishu.kernelsu.ui.util.defaultSidebarPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SidebarGlassTest {
    @Test
    fun effectSelectionRespectsEveryCapabilityAndMaterial() {
        for (sdk in listOf(26, 30, 31, 33, 34, 35, 36)) {
            for (material in SidebarMaterial.entries) {
                for (hardware in listOf(false, true)) {
                    for (backdrop in listOf(false, true)) {
                        val expected = when {
                            material == SidebarMaterial.Flat || sdk < 31 || !hardware || !backdrop -> SidebarGlassMode.Fallback
                            material == SidebarMaterial.LiquidGlass && sdk >= 34 -> SidebarGlassMode.Refractive
                            else -> SidebarGlassMode.Frosted
                        }
                        assertEquals(expected, sidebarGlassMode(material, sdk, hardware, backdrop))
                    }
                }
            }
        }
    }

    @Test
    fun flatAndInactiveStylesNeverCaptureWallpaperAndVideosAlwaysFallBack() {
        for (material in SidebarMaterial.entries) {
            assertFalse(canCaptureSidebarWallpaper(false, material, null, false))
            assertFalse(canCaptureSidebarWallpaper(true, material, "content://video", false))
            assertFalse(canCaptureSidebarWallpaper(true, material, null, true))
            assertEquals(material != SidebarMaterial.Flat, canCaptureSidebarWallpaper(true, material, null, false))
        }
    }

    @Test
    fun homeCardGlassCapturesWallpaperEvenWhenTheRailIsFlat() {
        assertTrue(canCaptureSidebarWallpaper(true, SidebarMaterial.Flat, null, false, homeCardsNeedGlass = true))
        assertFalse(canCaptureSidebarWallpaper(false, SidebarMaterial.Flat, null, false, homeCardsNeedGlass = true))
        assertFalse(canCaptureSidebarWallpaper(true, SidebarMaterial.Flat, "content://video", false, homeCardsNeedGlass = true))
        assertFalse(canCaptureSidebarWallpaper(true, SidebarMaterial.Flat, null, true, homeCardsNeedGlass = true))
    }

    @Test
    fun lensReceivesSupportedShapeButAllCornersRemainSquare() {
        for (size in listOf(Size(80f, 600f), Size(96f, 900f), Size(116f, 240f))) {
            for (direction in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
                for (scale in listOf(1f, 2f, 3f)) {
                    assertEquals(Outline.Rectangle(Rect(0f, 0f, size.width, size.height)), SidebarGlassLensShape.createOutline(size, direction, Density(scale)))
                }
            }
        }
    }

    @Test
    fun defaultDayNightPalettesRemainNeutralAndReadableAcrossWallpaperExtremes() {
        for (material in SidebarMaterial.entries) {
            for (dark in listOf(false, true)) {
                val colors = sidebarColors(defaultSidebarPalette(material, dark))
                for (color in listOf(colors.background, colors.backgroundEnd, colors.content, colors.selection, colors.highlight)) {
                    assertEquals(1f, color.alpha, 0f)
                    assertEquals(color.red, color.green, 0f)
                    assertEquals(color.red, color.blue, 0f)
                }
                val contrast = sidebarMinimumContrast(colors, material != SidebarMaterial.Flat)
                assertTrue("$material dark=$dark contrast=$contrast", contrast >= 4.5f)
            }
        }
    }

    @Test
    fun arbitraryCustomColorsReachTheRendererUnchanged() {
        val palette = defaultSidebarPalette(SidebarMaterial.LiquidGlass, false).copy(
            background = 0xFF214C72, backgroundEnd = 0xFF173651, selection = 0xFF367899,
            content = 0xFFFFFFFF, highlight = 0xFF77BADD, shade = 0xFF152637,
            tintAlpha = 0.82f, highlightStrength = 1.3f, reliefStrength = 0.4f,
        )
        val colors = sidebarColors(palette)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF214C72), colors.background)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF173651), colors.backgroundEnd)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF367899), colors.selection)
        assertEquals(androidx.compose.ui.graphics.Color.White, colors.content)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF77BADD), colors.highlight)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF152637), colors.shade)
        assertEquals(0.82f, colors.tintAlpha, 0f)
        assertEquals(1.3f, colors.highlightStrength, 0f)
        assertEquals(0.4f, colors.reliefStrength, 0f)
    }

    @Test
    fun contrastCheckUsesTheSelectedGlassOpacity() {
        val palette = defaultSidebarPalette(SidebarMaterial.LiquidGlass, true)
        val opaque = sidebarMinimumContrast(sidebarColors(palette.copy(tintAlpha = 1f)), true)
        val translucent = sidebarMinimumContrast(sidebarColors(palette.copy(tintAlpha = 0.75f)), true)
        assertTrue(translucent < opaque)
    }

    @Test
    fun lowContrastCustomTextIsDetected() {
        val palette = defaultSidebarPalette(SidebarMaterial.NeumorphicGlass, false).copy(content = 0xFFE5E5E5)
        assertTrue(sidebarMinimumContrast(sidebarColors(palette), true) < 4.5f)
    }
}
