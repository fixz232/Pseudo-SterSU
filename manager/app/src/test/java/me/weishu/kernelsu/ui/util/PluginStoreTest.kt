package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking
import java.io.File
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.util.Base64

class PluginStoreTest {
    @Test
    fun defaultCatalogUsesActiveRepository() {
        assertEquals(
            "https://raw.githubusercontent.com/fixz232/SterSU-ThemeStore/main/plugin-store/catalog-v2.json",
            managerPluginCatalogUrl(),
        )
    }

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
        assertEquals(
            setOf(PluginSlot.MountHidePathmaskLkm),
            catalog.plugins.first { it.id == ManagerPlugin.PathmaskLkm.id }.slots,
        )
    }

    @Test
    fun olderSignedCatalogCanOmitNewPlugin() {
        val catalog = parseManagerPluginCatalog(validCatalog(ManagerPlugin.entries.dropLast(1)))
        assertEquals(ManagerPlugin.entries.size - 1, catalog.plugins.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun catalogRejectsArbitraryMissingPlugin() {
        parseManagerPluginCatalog(validCatalog(ManagerPlugin.entries.drop(1)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun catalogRejectsDuplicatePlugin() {
        parseManagerPluginCatalog(
            validCatalog(ManagerPlugin.entries.dropLast(1) + ManagerPlugin.RescueProtection)
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

        assertEquals(4, urls.size)
        assertEquals(
            "https://ghproxy.net/https://raw.githubusercontent.com/fixz232/SterSU-ThemeStore/main/plugin-store/packages/remote-management-suite.ksplugin",
            urls[0],
        )
        assertEquals(
            "https://raw.githubusercontent.com/fixz232/SterSU-ThemeStore/main/plugin-store/packages/remote-management-suite.ksplugin",
            urls[1],
        )
        assertEquals(url, urls.last())
    }

    @Test
    fun catalogRouteKeepsCatalogAndSignatureOnTheSameRoute() {
        val url = managerPluginCatalogUrl()
        val pairs = resolvePluginCatalogUrls(url, PluginDownloadRoute.Accelerator)

        assertEquals(2, pairs.size)
        assertTrue(pairs.first().first.startsWith("https://ghproxy.net/"))
        assertTrue(pairs.first().second.endsWith("/catalog-v2.sig"))
        assertEquals(url, pairs.last().first)
        assertEquals(
            "https://raw.githubusercontent.com/fixz232/SterSU-ThemeStore/main/plugin-store/catalog-v2.sig",
            pairs.last().second,
        )
    }

    @Test
    fun catalogSignatureAcceptsGithubLineEndingNormalization() {
        val keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        val signedCatalog = "{\r\n  \"version\": 1\r\n}\r\n".toByteArray()
        val downloadedCatalog = "{\n  \"version\": 1\n}\n".toByteArray()
        val signer = Signature.getInstance("Ed25519").apply {
            initSign(keyPair.private)
            update(signedCatalog)
        }

        verifyManagerPluginCatalogSignature(
            downloadedCatalog,
            Base64.getEncoder().encode(signer.sign()),
            Base64.getEncoder().encodeToString(keyPair.public.encoded),
        )
    }

    @Test
    fun legacyBundledCatalogRemainsTrusted() {
        val catalog = bundledCatalog(1)
        verifyManagerPluginCatalogSignature(catalog, bundledSignature(1))

        val parsed = parseManagerPluginCatalog(catalog.toString(Charsets.UTF_8))
        assertEquals(ManagerPlugin.entries.size - 1, parsed.plugins.size)
        assertFalse(parsed.plugins.any { it.id == ManagerPlugin.PathmaskLkm.id })
    }

    @Test
    fun replacementSignedBundledCatalogIncludesEveryPlugin() {
        val catalog = bundledCatalog(2)
        verifyManagerPluginCatalogSignature(catalog, bundledSignature(2))

        val parsed = parseManagerPluginCatalog(catalog.toString(Charsets.UTF_8))
        assertEquals(ManagerPlugin.entries.map { it.id }.toSet(), parsed.plugins.map { it.id }.toSet())
        assertTrue(parsed.plugins.any { it.id == ManagerPlugin.PathmaskLkm.id })
    }

    @Test
    fun publishedAndBundledCatalogCopiesMatch() {
        for (version in 1..2) {
            assertArrayEquals(File("../../plugin-store/catalog-v$version.json").readBytes(), bundledCatalog(version))
            assertArrayEquals(File("../../plugin-store/catalog-v$version.sig").readBytes(), bundledSignature(version))
        }
    }

    @Test
    fun replacementCatalogPackagesMatchSignedHashes() {
        val bytes = bundledCatalog(2)
        verifyManagerPluginCatalogSignature(bytes, bundledSignature(2))
        val catalog = parseManagerPluginCatalog(bytes.toString(Charsets.UTF_8))
        for (plugin in catalog.plugins) {
            val downloaded = File("../../plugin-store/packages/${plugin.id}.ksplugin").readBytes()
            val verified = pluginPackageBytesMatchingCatalog(downloaded, plugin)
            assertEquals(plugin.sizeBytes, verified.size.toLong())
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun defaultTrustRejectsUnknownSigningKey() {
        val keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        val catalog = bundledCatalog(2)
        val signer = Signature.getInstance("Ed25519").apply {
            initSign(keyPair.private)
            update(catalog)
        }
        verifyManagerPluginCatalogSignature(catalog, Base64.getEncoder().encode(signer.sign()))
    }

    @Test
    fun bothTrustedCatalogsRejectTampering() {
        for (version in 1..2) {
            assertThrows(IllegalArgumentException::class.java) {
                verifyManagerPluginCatalogSignature(bundledCatalog(version) + " ".toByteArray(), bundledSignature(version))
            }
        }
    }

    @Test
    fun trustedCatalogsRejectSwappedSignatures() {
        for (version in 1..2) {
            assertThrows(IllegalArgumentException::class.java) {
                verifyManagerPluginCatalogSignature(bundledCatalog(version), bundledSignature(3 - version))
            }
        }
    }

    @Test
    fun catalogRejectsInvalidSignatureBytes() {
        val tampered = Base64.getMimeDecoder().decode(bundledSignature(2)).apply {
            this[0] = (this[0].toInt() xor 1).toByte()
        }
        val invalidSignatures = listOf(
            byteArrayOf(),
            "not-a-signature".toByteArray(),
            Base64.getEncoder().encode(ByteArray(63)),
            Base64.getEncoder().encode(tampered),
        )
        for (signature in invalidSignatures) {
            assertThrows(IllegalArgumentException::class.java) {
                verifyManagerPluginCatalogSignature(bundledCatalog(2), signature)
            }
        }
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
    fun packageHashAcceptsGithubLineEndingNormalization() {
        val signedPackage = "{\r\n  \"version\": 1\r\n}\r\n".toByteArray()
        val downloadedPackage = "{\n  \"version\": 1\n}\n".toByteArray()
        val entry = pluginPackage(version = 1).copy(
            sizeBytes = signedPackage.size.toLong(),
            sha256 = MessageDigest.getInstance("SHA-256")
                .digest(signedPackage)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) },
        )

        assertTrue(
            pluginPackageBytesMatchingCatalog(downloadedPackage, entry)
                .contentEquals(signedPackage)
        )
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
    fun failedRemoteStopKeepsPluginRecord() = runBlocking {
        var removed = false
        var stopAttempted = false
        val result = removeManagerPlugin(
            pluginId = ManagerPlugin.RemoteManagementSuite.id,
            stealthEnabled = false,
            stopRemoteManagement = { stopAttempted = true; false },
            stopPathmask = { true },
            removeRecord = { removed = true; true },
        )

        assertEquals(PluginRemovalResult.RemoteManagementStopFailed, result)
        assertTrue(stopAttempted)
        assertFalse(removed)
    }

    @Test
    fun stealthModeMustBeDisabledBeforeRemotePluginRemoval() = runBlocking {
        var stopAttempted = false
        val result = removeManagerPlugin(
            pluginId = ManagerPlugin.RemoteManagementSuite.id,
            stealthEnabled = true,
            stopRemoteManagement = { stopAttempted = true; true },
            stopPathmask = { true },
            removeRecord = { true },
        )

        assertEquals(PluginRemovalResult.RequiresStealthDisabled, result)
        assertFalse(stopAttempted)
    }

    @Test
    fun successfulRemoteStopAllowsRemoval() = runBlocking {
        val result = removeManagerPlugin(
            pluginId = ManagerPlugin.RemoteManagementSuite.id,
            stealthEnabled = false,
            stopRemoteManagement = { true },
            stopPathmask = { true },
            removeRecord = { true },
        )

        assertEquals(PluginRemovalResult.Removed, result)
    }

    @Test
    fun failedPathmaskShutdownKeepsPluginRecord() = runBlocking {
        var removed = false
        val result = removeManagerPlugin(
            pluginId = ManagerPlugin.PathmaskLkm.id,
            stealthEnabled = false,
            stopRemoteManagement = { true },
            stopPathmask = { false },
            removeRecord = { removed = true; true },
        )

        assertEquals(PluginRemovalResult.PathmaskStopFailed, result)
        assertFalse(removed)
    }

    @Test
    fun pathmaskRemovalDisablesAutoLoadAndConfirmsUnload() = runBlocking {
        var reads = 0
        var disabled = false
        var unloaded = false
        val stopped = stopPathmaskPluginForRemoval(
            readStatus = {
                reads++
                HiddenPathConfigReadResult(HiddenPathConfigState(
                    targetPaths = listOf("/system/bin/su"),
                    autoLoadEnabled = !disabled,
                    loaded = !unloaded,
                ))
            },
            disableAutoLoad = { disabled = true; ToolCommandResult(success = true) },
            unload = { unloaded = true; ToolCommandResult(success = true) },
        )

        assertTrue(stopped)
        assertTrue(disabled)
        assertTrue(unloaded)
        assertEquals(2, reads)
    }

    @Test
    fun pathmaskRemovalRejectsUnverifiedShutdown() = runBlocking {
        val stopped = stopPathmaskPluginForRemoval(
            readStatus = { HiddenPathConfigReadResult(HiddenPathConfigState(
                targetPaths = listOf("/system/bin/su"),
                autoLoadEnabled = true,
                loaded = true,
            )) },
            disableAutoLoad = { ToolCommandResult(success = true) },
            unload = { ToolCommandResult(success = true) },
        )

        assertFalse(stopped)
    }

    @Test
    fun pathmaskRemovalRejectsFailedStatusOrUnload() = runBlocking {
        assertFalse(stopPathmaskPluginForRemoval(
            readStatus = { HiddenPathConfigReadResult(error = "unavailable") },
            disableAutoLoad = { ToolCommandResult(success = true) },
            unload = { ToolCommandResult(success = true) },
        ))
        assertFalse(stopPathmaskPluginForRemoval(
            readStatus = { HiddenPathConfigReadResult(HiddenPathConfigState(
                targetPaths = listOf("/system/bin/su"),
                autoLoadEnabled = true,
                loaded = true,
            )) },
            disableAutoLoad = { ToolCommandResult(success = true) },
            unload = { ToolCommandResult(errorCode = "pathmask.module_busy") },
        ))
    }

    private fun bundledCatalog(version: Int): ByteArray =
        File("src/main/assets/plugin-store/catalog-v$version.json").readBytes()

    private fun bundledSignature(version: Int): ByteArray =
        File("src/main/assets/plugin-store/catalog-v$version.sig").readBytes()

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

    private fun validCatalog(pluginsToInclude: List<ManagerPlugin> = ManagerPlugin.entries): String {
        val plugins = pluginsToInclude.joinToString(",") { plugin ->
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
