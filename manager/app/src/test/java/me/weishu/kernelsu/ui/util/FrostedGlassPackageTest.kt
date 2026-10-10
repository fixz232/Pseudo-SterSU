package me.weishu.kernelsu.ui.util

import java.io.File
import java.security.MessageDigest
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrostedGlassPackageTest {
    private fun currentTheme(): InterfaceStyleTheme {
        val assets = File("src/main/assets/interface-style")
        val catalog = File(assets, "catalog-v2.json").readBytes()
        verifyInterfaceStyleCatalogSignature(catalog, File(assets, "catalog-v2.sig").readBytes())
        val style = parseInterfaceStyleCatalog(catalog.toString(Charsets.UTF_8))
            .styles.single { it.id == "liquid-glass" }
        // Released Managers require bundle and theme versions to both remain v3.
        assertEquals(3, style.version)
        assertTrue(style.downloadUrl.endsWith("/liquid-glass-clear-20261009.ksstyle"))
        val bytes = File("../../interface-styles/packages/liquid-glass-clear-20261009.ksstyle").readBytes()
        assertEquals(style.sizeBytes, bytes.size.toLong())
        assertEquals(style.sha256, sha256(bytes))
        val bundle = parseInterfaceStyleBundle(bytes, style)
        return parseInterfaceStyleTheme(bundle.resources.getValue("theme.json"), style)
    }

    @Test
    fun signedPackageUsesQuietGlassWithoutDistortion() {
        val theme = currentTheme()
        assertEquals("liquid_glass", theme.engine)
        assertFalse(theme.forceDark)
        assertFalse(theme.glass.refraction)
        assertEquals(0f, theme.glass.chromaticAberration, 0f)
        assertEquals(0.86f, theme.glass.surfaceAlpha, 0f)
        assertEquals(0.14f, theme.glass.strokeAlpha, 0f)
        assertTrue(theme.chrome.navigationAlpha > theme.glass.surfaceAlpha)
        assertEquals(0, theme.scene.primaryCount)
        assertEquals(0, theme.scene.secondaryCount)
    }

    @Test
    fun bothPalettesKeepTextReadableOnCardsAndBackground() {
        val theme = currentTheme()
        for (palette in listOf(theme.lightPalette, theme.darkPalette)) {
            for (surface in listOf(palette.surface, palette.background, palette.backgroundAlt)) {
                assertTrue("Body text contrast", contrast(palette.content, surface) >= 7.0)
                assertTrue("Accent text contrast", contrast(palette.primary, surface) >= 4.5)
                assertTrue("Secondary text contrast", contrast(palette.secondary, surface) >= 4.5)
            }
        }
    }

    @Test
    fun legacyPackageRemainsUsableByFrozenCatalog() {
        val assets = File("src/main/assets/interface-style")
        val catalog = parseInterfaceStyleCatalog(File(assets, "catalog-v1.json").readText())
        val legacy = catalog.styles.single { it.id == "liquid-glass" }
        val bytes = File("../../interface-styles/packages/liquid-glass.ksstyle").readBytes()
        assertEquals(legacy.sha256, sha256(bytes))
        assertEquals(legacy.sizeBytes, bytes.size.toLong())
        parseInterfaceStyleBundle(bytes, legacy)
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun contrast(foreground: Long, background: Long): Double {
        fun luminance(color: Long): Double {
            fun channel(shift: Int): Double {
                val value = ((color shr shift) and 0xff) / 255.0
                return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
            }
            return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
        }
        val a = luminance(foreground)
        val b = luminance(background)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }
}
