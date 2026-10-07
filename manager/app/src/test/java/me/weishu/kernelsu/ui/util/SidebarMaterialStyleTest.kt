package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SidebarMaterialStyleTest {
    @Test
    fun defaultIsLiquidAndLegacyDisabledSwitchMigratesToFlat() {
        assertEquals(SidebarMaterial.LiquidGlass, SidebarWidgetConfig().material)
        assertEquals(SidebarMaterial.Flat, SidebarWidgetConfig(glassEnabled = false).material)
        assertEquals(SidebarMaterial.Flat, SidebarMaterial.fromValue(null, false))
        assertEquals(SidebarMaterial.LiquidGlass, SidebarMaterial.fromValue(null, true))
        assertEquals(SidebarMaterial.Flat, SidebarMaterial.fromValue("invalid", false))
        assertEquals(SidebarMaterial.LiquidGlass, SidebarMaterial.fromValue("invalid", true))
        SidebarMaterial.entries.forEach { material ->
            assertEquals(material, SidebarMaterial.fromValue(material.value, false))
            assertEquals(material, SidebarMaterial.fromValue(material.value, true))
        }
    }

    @Test
    fun allSixPaletteSlotsSurvivePersistenceIndependently() {
        var palettes = SidebarPalettes()
        val expected = mutableListOf<Triple<SidebarMaterial, Boolean, SidebarPalette>>()
        SidebarMaterial.entries.forEachIndexed { index, material ->
            for (dark in listOf(false, true)) {
                val color = 0xFF123456L + index * 0x1100 + if (dark) 0x33 else 0
                val palette = defaultSidebarPalette(material, dark).copy(
                    background = color,
                    content = 0xFF765432,
                    shade = 0xFF254769,
                    tintAlpha = 0.82f,
                    highlightStrength = 1.25f,
                    reliefStrength = 0.65f,
                )
                palettes = palettes.withPalette(material, dark, palette)
                expected += Triple(material, dark, palette)
            }
        }
        val restored = decodeSidebarPalettes(encodeSidebarPalettes(palettes))
        assertEquals(palettes, restored)
        expected.forEach { (material, dark, palette) -> assertEquals(palette, restored.palette(material, dark)) }
    }

    @Test
    fun resettingOnePaletteDoesNotChangeOtherThemesOrMaterials() {
        var palettes = SidebarPalettes()
        SidebarMaterial.entries.forEach { material ->
            for (dark in listOf(false, true)) {
                palettes = palettes.withPalette(material, dark, defaultSidebarPalette(material, dark).copy(background = 0xFF225599))
            }
        }
        val reset = palettes.reset(SidebarMaterial.NeumorphicGlass, false)
        SidebarMaterial.entries.forEach { material ->
            for (dark in listOf(false, true)) {
                assertEquals(
                    if (material == SidebarMaterial.NeumorphicGlass && !dark) defaultSidebarPalette(material, dark) else palettes.palette(material, dark),
                    reset.palette(material, dark),
                )
            }
        }
    }

    @Test
    fun changingMaterialOrPalettePreservesWidgetNavigationAvatarAndWeather() {
        val config = SidebarWidgetConfig(
            widgetType = SidebarWidgetType.Weather,
            side = SidebarSide.Right,
            navigationOrder = SIDEBAR_NAVIGATION_IDS.reversed(),
            imageUriString = "content://avatar/saved",
            imageShape = SidebarImageShape.Diamond,
            weatherApi = SidebarWeatherConfig(enabled = true, url = "https://weather.test/current"),
        ).normalized()
        val updated = config.copy(
            material = SidebarMaterial.NeumorphicGlass,
            palettes = config.palettes.withPalette(SidebarMaterial.NeumorphicGlass, false, defaultSidebarPalette(SidebarMaterial.NeumorphicGlass, false).copy(selection = 0xFFABCDEF)),
        ).normalized()
        assertEquals(config, updated.copy(material = config.material, palettes = config.palettes))
    }

    @Test
    fun missingOrDamagedPaletteDataUsesDefaults() {
        for (value in listOf(null, "", "broken", "[]", "null", "{\"overrides\":null}", " ".repeat(9000))) {
            assertEquals(SidebarPalettes(), decodeSidebarPalettes(value))
        }
        assertEquals(SidebarPalettes(), decodeSidebarPalettes("{\"futureVersion\":2}"))
    }

    @Test
    fun existingFiveColorPalettesKeepTheirOriginalEffectDefaults() {
        val fallback = defaultSidebarPalette(SidebarMaterial.LiquidGlass, false)
        val legacy = """{"overrides":{"liquid_glass.light":{"background":4280431428,"backgroundEnd":4294440951,"selection":4291677645,"content":4279637526,"highlight":4294967295}}}"""
        val restored = decodeSidebarPalettes(legacy).palette(SidebarMaterial.LiquidGlass, false)
        assertEquals(0.90f, restored.tintAlpha, 0f)
        assertEquals(1f, restored.highlightStrength, 0f)
        assertEquals(1f, restored.reliefStrength, 0f)
        assertEquals(fallback.shade, restored.shade)
        assertEquals(4280431428L, restored.background)
    }

    @Test
    fun malformedEffectValuesCannotMakeTheRailInvisible() {
        val fallback = defaultSidebarPalette(SidebarMaterial.NeumorphicGlass, true)
        val normalized = fallback.copy(
            shade = 0x00112233,
            tintAlpha = Float.NaN,
            highlightStrength = -10f,
            reliefStrength = 20f,
        ).normalized(fallback)
        assertEquals(fallback.shade, normalized.shade)
        assertEquals(fallback.tintAlpha, normalized.tintAlpha, 0f)
        assertEquals(0f, normalized.highlightStrength, 0f)
        assertEquals(1.5f, normalized.reliefStrength, 0f)
        val restored = decodeSidebarPalettes(
            """{"overrides":{"neumorphic_glass.dark":{"tintAlpha":-1,"highlightStrength":99,"reliefStrength":"invalid"}}}"""
        ).palette(SidebarMaterial.NeumorphicGlass, true)
        assertEquals(0.75f, restored.tintAlpha, 0f)
        assertEquals(1.5f, restored.highlightStrength, 0f)
        assertEquals(fallback.reliefStrength, restored.reliefStrength, 0f)
    }

    @Test
    fun invalidChannelsFallBackWithoutDiscardingValidColors() {
        val fallback = defaultSidebarPalette(SidebarMaterial.LiquidGlass, false)
        val malformed = fallback.copy(background = -1L, backgroundEnd = 0xFFFFFFFFFL, selection = 0x00FFFFFF, highlight = 0xFF123456)
        val value = decodeSidebarPalettes("""{"overrides":{"liquid_glass.light":{"background":-1,"backgroundEnd":68719476735,"selection":16777215,"content":${fallback.content},"highlight":4279383126}}}""")
        assertEquals(fallback.copy(highlight = 0xFF123456), malformed.normalized(fallback))
        assertEquals(malformed.normalized(fallback), value.palette(SidebarMaterial.LiquidGlass, false))
    }

    @Test
    fun unknownPaletteSlotsAreNotWrittenBack() {
        val value = SidebarPalettes(mapOf("future.unknown" to defaultSidebarPalette(SidebarMaterial.Flat, false)))
        assertTrue(decodeSidebarPalettes(encodeSidebarPalettes(value)).overrides.isEmpty())
    }

    @Test
    fun rgbInputSupportsAllOpaqueColorsAndRejectsMalformedOrTransparentColors() {
        for (color in listOf(0xFF000000L, 0xFFFFFFFFL, 0xFF012345L, 0xFFABCDEF, 0xFF336699)) {
            assertEquals(color, parseSidebarRgb(formatSidebarRgb(color)))
        }
        assertEquals(0xFFABCDEF, parseSidebarRgb("  #abcdef  "))
        assertEquals(0xFF123456, parseSidebarRgb("123456"))
        for (text in listOf("#FFF", "red", "#GGGGGG", "#00FFFFFF", "", "#-12345", "##123456", "#12 456")) {
            assertNull(parseSidebarRgb(text))
        }
    }

    @Test
    fun newPreferenceKeysInvalidateLiveRail() {
        assertTrue(isSidebarWidgetPreference(SIDEBAR_MATERIAL_KEY))
        assertTrue(isSidebarWidgetPreference(SIDEBAR_PALETTES_KEY))
        assertFalse(isSidebarWidgetPreference("unrelated"))
    }
}
