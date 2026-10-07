package me.weishu.kernelsu.ui.screen.themestore

import me.weishu.kernelsu.ui.util.InstalledInterfaceStyle
import me.weishu.kernelsu.ui.util.InterfaceStylePackage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterfaceStyleUpdateStateTest {
    private val current = InterfaceStylePackage(
        id = "test-style", name = "Test", summary = "", engine = "material", variant = null,
        version = 2, downloadUrl = "https://example.invalid/test.ksstyle",
        sha256 = "a".repeat(64), sizeBytes = 1024, accent = 0xFF46569BL,
    )

    @Test
    fun newDownloadIsNotAnUpdate() {
        assertFalse(hasInterfaceStyleUpdate(null, current))
    }

    @Test
    fun newerVersionOrChangedContentOffersUpdate() {
        val installed = InstalledInterfaceStyle(current, installedAt = 0)
        assertTrue(hasInterfaceStyleUpdate(installed, current.copy(version = 3)))
        assertTrue(hasInterfaceStyleUpdate(installed, current.copy(sha256 = "b".repeat(64))))
    }

    @Test
    fun cachedOlderCatalogDoesNotDowngradeInstalledStyle() {
        val installed = InstalledInterfaceStyle(current, installedAt = 0)
        assertFalse(hasInterfaceStyleUpdate(installed, current.copy(version = 1, sha256 = "b".repeat(64))))
    }

    @Test
    fun sameContentDoesNotOfferAnUpdate() {
        val installed = InstalledInterfaceStyle(current, installedAt = 0)
        assertFalse(hasInterfaceStyleUpdate(installed, current))
        assertFalse(hasInterfaceStyleUpdate(installed, current.copy(sha256 = current.sha256.uppercase())))
    }
}
