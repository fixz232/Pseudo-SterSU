package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginStoreTest {
    @Test
    fun catalogContainsEveryAllowlistedPlugin() {
        val catalog = parseManagerPluginCatalog(validCatalog())

        assertEquals(ManagerPlugin.entries.size, catalog.plugins.size)
        assertEquals(
            ManagerPlugin.entries.map { it.id }.toSet(),
            catalog.plugins.map { it.id }.toSet(),
        )
        assertEquals(
            setOf(PluginSlot.MaintenanceWebManager, PluginSlot.MaintenanceStealthMode),
            catalog.plugins.first { it.id == ManagerPlugin.RemoteManagementSuite.id }.slots,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun catalogRejectsUnknownPlugin() {
        parseManagerPluginCatalog(validCatalog().replace("\"rescue-protection\"", "\"unknown-plugin\""))
    }

    @Test(expected = IllegalArgumentException::class)
    fun catalogRejectsWrongSlot() {
        parseManagerPluginCatalog(
            validCatalog().replace("\"toolbox.rescue\"", "\"superuser.app-freeze\"")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun catalogRejectsNonGithubUrl() {
        parseManagerPluginCatalog(
            validCatalog().replace(
                "https://raw.githubusercontent.com/fixz232/ApkeSU-PluginStore",
                "https://example.com",
            )
        )
    }

    @Test
    fun remoteManagementDownloadUsesAcceleratorAndDirectFallback() {
        val url = "https://raw.githubusercontent.com/fixz232/ApkeSU-PluginStore/main/packages/remote-management-suite.ksplugin"
        val urls = resolvePluginDownloadUrls(url, PluginDownloadRoute.Accelerator)

        assertEquals(2, urls.size)
        assertTrue(urls.first().startsWith("https://ghproxy.net/"))
        assertEquals(url, urls.last())
    }

    @Test
    fun catalogRouteKeepsCatalogAndSignatureOnTheSameRoute() {
        val url = "https://raw.githubusercontent.com/fixz232/ApkeSU-PluginStore/main/catalog-v1.json"
        val pairs = resolvePluginCatalogUrls(url, PluginDownloadRoute.Accelerator)

        assertEquals(2, pairs.size)
        assertTrue(pairs.first().first.startsWith("https://ghproxy.net/"))
        assertTrue(pairs.first().second.endsWith("/catalog-v1.sig"))
        assertEquals(url, pairs.last().first)
        assertEquals(
            "https://raw.githubusercontent.com/fixz232/ApkeSU-PluginStore/main/catalog-v1.sig",
            pairs.last().second,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun downloadRejectsNonHttps() {
        resolvePluginDownloadUrls("http://raw.githubusercontent.com/fixz232/plugin", PluginDownloadRoute.Direct)
    }

    @Test
    fun packageMustMatchAllCatalogMetadata() {
        val entry = pluginPackage(version = 2)
        val downloaded = entry.copy(sha256 = "", sizeBytes = 0L)

        assertTrue(pluginPackageMatchesCatalog(downloaded, entry))
        assertFalse(pluginPackageMatchesCatalog(downloaded.copy(description = "different"), entry))
        assertFalse(pluginPackageMatchesCatalog(downloaded.copy(downloadUrl = "https://raw.githubusercontent.com/other/file"), entry))
        assertFalse(pluginPackageMatchesCatalog(downloaded.copy(version = 1), entry))
    }

    @Test
    fun staleCatalogUsesGenerationTimeAndAllowsSmallClockSkew() {
        val now = 1_000_000L

        assertFalse(isPluginCatalogStale(now, now))
        assertFalse(isPluginCatalogStale(now + 60_000L, now))
        assertTrue(isPluginCatalogStale(now - 24L * 60L * 60L * 1000L, now))
        assertTrue(isPluginCatalogStale(now + 6L * 60L * 1000L, now))
    }

    @Test
    fun catalogOffersForwardOrSameVersionHashUpdates() {
        val installed = InstalledManagerPlugin(pluginPackage(version = 1), installedAt = 1L)

        assertTrue(hasPluginUpdate(installed, pluginPackage(version = 2)))
        assertFalse(hasPluginUpdate(installed, pluginPackage(version = 1)))
        assertTrue(hasPluginUpdate(installed, pluginPackage(version = 1).copy(sha256 = "b".repeat(64))))
        assertFalse(hasPluginUpdate(installed, pluginPackage(version = 0)))
        assertFalse(hasPluginUpdate(null, pluginPackage(version = 2)))
    }

    @Test
    fun failedRemoteStopKeepsPluginRecord() {
        var removed = false
        var stopAttempted = false
        val result = removeManagerPlugin(
            pluginId = ManagerPlugin.RemoteManagementSuite.id,
            stealthEnabled = false,
            stopRemoteManagement = { stopAttempted = true; false },
            removeRecord = { removed = true; true },
        )

        assertEquals(PluginRemovalResult.RemoteManagementStopFailed, result)
        assertTrue(stopAttempted)
        assertFalse(removed)
    }

    @Test
    fun stealthModeMustBeDisabledBeforeRemotePluginRemoval() {
        var stopAttempted = false
        val result = removeManagerPlugin(
            pluginId = ManagerPlugin.RemoteManagementSuite.id,
            stealthEnabled = true,
            stopRemoteManagement = { stopAttempted = true; true },
            removeRecord = { true },
        )

        assertEquals(PluginRemovalResult.RequiresStealthDisabled, result)
        assertFalse(stopAttempted)
    }

    @Test
    fun successfulRemoteStopAllowsRemoval() {
        val result = removeManagerPlugin(
            pluginId = ManagerPlugin.RemoteManagementSuite.id,
            stealthEnabled = false,
            stopRemoteManagement = { true },
            removeRecord = { true },
        )

        assertEquals(PluginRemovalResult.Removed, result)
    }

    private fun pluginPackage(version: Int) = ManagerPluginPackage(
        id = ManagerPlugin.RescueProtection.id,
        version = version,
        name = "rescue",
        summary = "summary",
        description = "description",
        instructions = listOf("instruction"),
        slots = ManagerPlugin.RescueProtection.slots,
        minManagerVersionCode = 32800,
        minKsudVersionCode = 32800,
        downloadUrl = "https://raw.githubusercontent.com/fixz232/ApkeSU-PluginStore/main/packages/rescue-protection.ksplugin",
        sha256 = "a".repeat(64),
        sizeBytes = 128,
    )

    private fun validCatalog(): String {
        val plugins = ManagerPlugin.entries.joinToString(",") { plugin ->
            val slots = plugin.slots.joinToString(",") { "\"${it.id}\"" }
            """
                {
                  "id": "${plugin.id}",
                  "version": 1,
                  "name": "${plugin.id}",
                  "summary": "summary",
                  "description": "description",
                  "instructions": ["read the feature instructions"],
                  "slots": [$slots],
                  "minManagerVersionCode": 32800,
                  "minKsudVersionCode": 32800,
                  "downloadUrl": "https://raw.githubusercontent.com/fixz232/ApkeSU-PluginStore/main/packages/${plugin.id}.ksplugin",
                  "sha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                  "sizeBytes": 128
                }
            """.trimIndent()
        }
        return """
            {
              "schema": "io.github.fixz.apkesu.plugin-catalog",
              "version": 1,
              "generatedAt": 1,
              "plugins": [$plugins]
            }
        """.trimIndent()
    }
}
