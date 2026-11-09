package me.weishu.kernelsu.ui.screen.settings

import java.io.File
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.component.pixel.PixelStyle
import me.weishu.kernelsu.ui.component.snow.SeasonStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsHomeStyleTest {
    @Test
    fun everySeasonUsesTheDeviceHomeAndItsOwnLabel() {
        SeasonStyle.entries.forEach { season ->
            val state = SettingsUiState(uiMode = InterfaceStyle.Snow.value, seasonStyle = season.value)
            assertTrue(season.value, usesDeviceSettingsHome(state.uiMode))
            assertEquals(season.value, season.labelRes, settingsHomeStyleLabelRes(state))
        }
    }

    @Test
    fun everyPixelVariantUsesTheDeviceHomeAndItsOwnLabel() {
        PixelStyle.entries.forEach { pixel ->
            val state = SettingsUiState(uiMode = InterfaceStyle.Pixel.value, pixelStyle = pixel.value)
            assertTrue(pixel.value, usesDeviceSettingsHome(state.uiMode))
            assertEquals(pixel.value, pixel.labelRes, settingsHomeStyleLabelRes(state))
        }
    }

    @Test
    fun everyInterfaceStyleUsesTheSharedDeviceHome() {
        InterfaceStyle.entries.forEach { style ->
            assertTrue(style.name, usesDeviceSettingsHome(style.value))
        }
        assertFalse(usesDeviceSettingsHome("unknown"))
    }

    @Test
    fun unknownAndLegacyVariantLabelsHaveSafeFallbacks() {
        assertEquals(SeasonStyle.Winter.labelRes, settingsHomeStyleLabelRes(
            SettingsUiState(uiMode = InterfaceStyle.Snow.value, seasonStyle = "unknown"),
        ))
        assertEquals(PixelStyle.ClassicHandheld.labelRes, settingsHomeStyleLabelRes(
            SettingsUiState(uiMode = InterfaceStyle.Pixel.value, pixelStyle = "unknown"),
        ))
        assertEquals(PixelStyle.PastoralFields.labelRes, settingsHomeStyleLabelRes(
            SettingsUiState(uiMode = InterfaceStyle.Pixel.value, pixelStyle = "forest_quest"),
        ))
    }

    @Test
    fun newHomeKeepsStyleSettingsWithoutSidebarOnlyControls() {
        listOf(
            InterfaceStyle.Snow to "season_motion",
            InterfaceStyle.Pixel to "pixel_motion",
        ).forEach { (style, motionKey) ->
            val entries = SettingsCatalog.entriesFor(SettingsCategory.Appearance, SettingsUiState(uiMode = style.value))
            assertTrue(entries.any { it.key == motionKey })
            assertTrue(entries.any { it.key == "day_night" })
            assertTrue(entries.any { it.key == "store" })
            assertFalse(entries.any { it.key == "sidebar_design" })
        }
    }

    // Source contracts check composition wiring, not rendered appearance or touch behavior.
    @Test
    fun deviceHomeRetainsDialogsAndDoesNotForceSidebarOrCategoryMode() {
        val source = settingsSource("SettingsSidebarScreen.kt")
        val pager = settingsSource("SettingsScreen.kt")
        assertTrue(pager.contains("usesDeviceSettingsHome(LocalInterfaceStyle.current) && pageMode.value == SettingsPageMode.Categories"))
        assertTrue(pager.contains("SettingsOverviewScreen("))
        assertTrue(source.contains("if (sidebarStyle) {\n                                    TextButton(onClick = openSidebar"))
        assertTrue(source.contains("showDeviceTextEditor && device.editing == null"))
        assertTrue(source.contains("SettingsWallpaperCropDialog("))
        assertTrue(source.contains(".consumeWindowInsets(contentInsets)"))
        assertTrue(source.contains("settingsShortcutColumns(maxWidth.value, LocalDensity.current.fontScale)"))
        assertFalse(source.contains(".alpha("))
    }

    @Test
    fun homeMoreSettingsAndOverviewUseTheSameThemedSurface() {
        val home = settingsSource("SettingsSidebarScreen.kt")
        val hub = settingsSource("SettingsHubScreen.kt")
        val overview = settingsSource("SettingsOverviewScreen.kt")
        val card = settingsSource("SettingsHomeStyle.kt")
        assertEquals(4, Regex("SettingsHomeCard \\{").findAll(home).count())
        assertTrue(hub.contains("SettingsHomeCard("))
        assertEquals(4, Regex("SettingsHomeCard\\(").findAll(overview).count())
        assertTrue(card.contains("shape: Shape = SidebarUiTokens.Shapes.large"))
        assertFalse(card.contains("pixelAwareMiuixCardShape"))
        assertTrue(card.contains("snowMiuixCardSurface(shape = shape, enabled = decorate)"))
        assertTrue(card.contains("!LocalUiPassthroughSettings.current.enabled"))
        assertTrue(card.contains("else immersiveSurfaceColor(color)"))
        assertFalse(card.contains(".alpha("))
    }

    private fun settingsSource(name: String): String =
        File("src/main/java/me/weishu/kernelsu/ui/screen/settings/$name").readText().replace("\r\n", "\n")
}
