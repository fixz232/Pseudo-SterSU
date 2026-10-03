package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SidebarGlassTest {
    @Test
    fun olderAndroidUsesStaticMaterial() {
        for (sdk in listOf(26, 29, 30)) {
            assertEquals(SidebarGlassMode.Fallback, sidebarGlassMode(true, sdk, true, true))
        }
    }

    @Test
    fun android12And13UseFrostedMaterialWithoutLens() {
        for (sdk in 31..33) {
            assertEquals(SidebarGlassMode.Frosted, sidebarGlassMode(true, sdk, true, true))
        }
    }

    @Test
    fun supportedAndroidUsesRefraction() {
        for (sdk in listOf(34, 35, 36)) {
            assertEquals(SidebarGlassMode.Refractive, sidebarGlassMode(true, sdk, true, true))
        }
    }

    @Test
    fun disabledMissingSourceOrSoftwareRenderingAlwaysFallsBack() {
        for (sdk in listOf(30, 31, 33, 34, 36)) {
            assertEquals(SidebarGlassMode.Fallback, sidebarGlassMode(false, sdk, true, true))
            assertEquals(SidebarGlassMode.Fallback, sidebarGlassMode(true, sdk, false, true))
            assertEquals(SidebarGlassMode.Fallback, sidebarGlassMode(true, sdk, true, false))
        }
    }

    @Test
    fun wallpaperCaptureRequiresSidebarAndNoVideoAnywhereInPager() {
        assertTrue(canCaptureSidebarWallpaper(true, null, false))
        assertTrue(canCaptureSidebarWallpaper(true, "", false))
        assertFalse(canCaptureSidebarWallpaper(false, null, false))
        assertFalse(canCaptureSidebarWallpaper(true, "content://wallpaper/video", false))
        assertFalse(canCaptureSidebarWallpaper(true, null, true))
        assertFalse(canCaptureSidebarWallpaper(true, "content://wallpaper/video", true))
    }

    @Test
    fun inkAndMaterialsRemainMonochrome() {
        listOf(
            SidebarGlassColors.LightInk, SidebarGlassColors.DarkInk,
            SidebarGlassColors.LightTint, SidebarGlassColors.DarkTint,
            SidebarGlassColors.LightFallback, SidebarGlassColors.DarkFallback,
        ).forEach { color ->
            assertEquals(color.red, color.green, 0.001f)
            assertEquals(color.green, color.blue, 0.001f)
        }
    }

    @Test
    fun tintPreservesTextContrastOverLightAndDarkWallpapers() {
        for (background in listOf(Color.Black, Color.White)) {
            assertContrast(SidebarGlassColors.LightInk, SidebarGlassColors.LightTint.compositeOver(background))
            assertContrast(SidebarGlassColors.DarkInk, SidebarGlassColors.DarkTint.compositeOver(background))
        }
    }

    @Test
    fun fallbackIsOpaqueAndReadable() {
        assertEquals(1f, SidebarGlassColors.LightFallback.alpha, 0f)
        assertEquals(1f, SidebarGlassColors.DarkFallback.alpha, 0f)
        assertContrast(SidebarGlassColors.LightInk, SidebarGlassColors.LightFallback)
        assertContrast(SidebarGlassColors.DarkInk, SidebarGlassColors.DarkFallback)
    }

    private fun assertContrast(ink: Color, surface: Color) {
        val brighter = maxOf(ink.luminance(), surface.luminance())
        val darker = minOf(ink.luminance(), surface.luminance())
        val contrast = (brighter + 0.05f) / (darker + 0.05f)
        assertTrue("Text contrast is $contrast", contrast >= 4.5f)
    }
}
