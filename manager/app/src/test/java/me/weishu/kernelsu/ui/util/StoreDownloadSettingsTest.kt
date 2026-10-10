package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreDownloadSettingsTest {
    private val resource = "https://raw.githubusercontent.com/fixz232/SterSU-ThemeStore/main/theme.kstheme"

    @Test
    fun disabledAccelerationOnlyUsesGithub() {
        assertEquals(listOf(resource), resolveStoreDownloadUrls(resource, StoreDownloadSettings(false)))
    }

    @Test
    fun builtInAndCustomAccelerationAlwaysFallBackToGithub() {
        assertEquals(
            listOf("https://ghproxy.net/$resource", resource),
            resolveStoreDownloadUrls(resource, StoreDownloadSettings()),
        )
        assertEquals(
            listOf("https://proxy.example/download/$resource", resource),
            resolveStoreDownloadUrls(resource, StoreDownloadSettings(true, "https://proxy.example/download/")),
        )
        val asset = "https://assets.githubusercontent.com/theme.kstheme"
        assertEquals(listOf("https://ghproxy.net/$asset", asset),
            resolveStoreDownloadUrls(asset, StoreDownloadSettings()))
    }

    @Test
    fun invalidAcceleratorCannotReceiveResourceUrl() {
        for (address in listOf(
            "http://proxy.example",
            "https://user:secret@proxy.example",
            "https://proxy.example/?token=secret",
            "https://proxy.example/#fragment",
            "https://github.com/",
            "https://localhost/",
        )) {
            assertFalse(isValidStoreAcceleratorAddress(address))
            assertEquals(listOf(resource), resolveStoreDownloadUrls(resource, StoreDownloadSettings(true, address)))
        }
        assertTrue(isValidStoreAcceleratorAddress(""))
        assertTrue(isValidStoreAcceleratorAddress("https://proxy.example/download"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonGithubResourceCannotBeProxied() {
        resolveStoreDownloadUrls("https://example.com/theme.kstheme", StoreDownloadSettings())
    }
}
