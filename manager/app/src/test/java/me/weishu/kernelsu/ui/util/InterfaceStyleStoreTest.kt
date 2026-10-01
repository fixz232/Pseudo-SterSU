package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class InterfaceStyleStoreTest {
    @Test
    fun defaultCatalogUsesCurrentThemeStoreRepository() {
        assertEquals(
            "https://raw.githubusercontent.com/fixz232/SterSU-ThemeStore/main/interface-styles/catalog-v1.json",
            interfaceStyleCatalogUrl(),
        )
    }

    @Test
    fun catalogParsesKnownDeclarativeVariants() {
        val catalog = parseInterfaceStyleCatalog(validCatalog())

        assertEquals(2, catalog.styles.size)
        assertEquals("snow", catalog.styles[0].engine)
        assertEquals("spring", catalog.styles[0].variant)
        assertEquals("pixel", catalog.styles[1].engine)
    }

    @Test(expected = IllegalArgumentException::class)
    fun catalogRejectsExecutableOrUnknownEngine() {
        parseInterfaceStyleCatalog(validCatalog().replace("\"snow\"", "\"dex\""))
    }

    @Test(expected = IllegalArgumentException::class)
    fun catalogRejectsUnknownVariant() {
        parseInterfaceStyleCatalog(validCatalog().replace("\"spring\"", "\"monsoon\""))
    }

    @Test
    fun lenientCatalogSkipsUnknownVariantAndKeepsOtherStyles() {
        val catalog = parseInterfaceStyleCatalogLenient(validCatalog().replace("\"spring\"", "\"monsoon\""))

        assertEquals(1, catalog.styles.size)
        assertEquals("pixel-cloud-town", catalog.styles.single().id)
    }

    @Test
    fun lenientCatalogReturnsEmptyWhenEveryOptionalStyleIsInvalid() {
        val catalog = parseInterfaceStyleCatalogLenient(
            validCatalog()
                .replace("\"spring\"", "\"monsoon\"")
                .replace("\"cloud_town\"", "\"unknown_pixel\""),
        )

        assertTrue(catalog.styles.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun catalogRejectsNonGithubPackageUrl() {
        parseInterfaceStyleCatalog(
            validCatalog().replace("https://raw.githubusercontent.com/fixz232/store/main", "https://example.com")
        )
    }

    @Test
    fun autoProxyFallsBackToDirectGithub() {
        val original = "https://raw.githubusercontent.com/fixz232/store/main/spring.ksstyle"
        val urls = resolveInterfaceStyleUrls(
            original,
            InterfaceStyleDownloadPreferences(mode = InterfaceStyleProxyMode.Auto),
        )

        assertEquals(2, urls.size)
        assertTrue(urls.first().startsWith("https://ghproxy.net/"))
        assertEquals(original, urls.last())
    }

    @Test
    fun customProxyNeverReceivesAnythingExceptOriginalUrl() {
        val original = "https://raw.githubusercontent.com/fixz232/store/main/spring.ksstyle"
        val urls = resolveInterfaceStyleUrls(
            original,
            InterfaceStyleDownloadPreferences(
                mode = InterfaceStyleProxyMode.Custom,
                customProxy = "https://proxy.example/download",
            ),
        )

        assertEquals(1, urls.size)
        assertTrue(urls.single().startsWith("https://proxy.example/download/https://raw.githubusercontent.com/"))
    }

    @Test
    fun declarativeBundleParsesAndVerifiesResourceHash() {
        val expected = packageFor("rain-light", "rain", "light_rain", 4284380326)
        val bundle = themeBundle(expected)

        val parsed = parseInterfaceStyleBundle(bundle, expected)

        assertTrue(parsed.resources.containsKey("theme.json"))
    }

    @Test
    fun v3ThemeCarriesExternalPaletteSceneChromeAndGlassData() {
        val expected = packageFor("rain-light", "rain", "light_rain", 4284380326)
        val theme = parseInterfaceStyleTheme(themeJson(expected), expected)

        assertEquals("rain", theme.engine)
        assertEquals("light_rain", theme.variant)
        assertEquals(0xFFF0F4F7L, theme.lightPalette.background)
        assertEquals(68, theme.scene.primaryCount)
        assertEquals(1, theme.scene.motifs.size)
        assertEquals(14f, theme.chrome.cornerDp)
        assertEquals(12f, theme.glass.blurDp)
    }

    @Test
    fun alphaThemeSupportsKernelEliteDayAndNightPalettes() {
        val expected = packageFor("kernel-elite", "alpha", null, 4278258918)
        val theme = parseInterfaceStyleTheme(
            themeJson(expected).toString(Charsets.UTF_8)
                .replace("\"primary\":\"#ff5e84a6\"", "\"primary\":\"#ff006a70\"")
                .replace("\"primary\":\"#ffd5e9f7\"", "\"primary\":\"#ff00dce6\"")
                .toByteArray(),
            expected,
        )

        assertEquals("alpha", theme.engine)
        assertEquals(null, theme.variant)
        assertEquals(0xFF006A70L, theme.lightPalette.primary)
        assertEquals(0xFF00DCE6L, theme.darkPalette.primary)
        assertEquals(8f, theme.chrome.cornerDp)
    }

    @Test(expected = IllegalArgumentException::class)
    fun v3ThemeRejectsUnsupportedFields() {
        val expected = packageFor("rain-light", "rain", "light_rain", 4284380326)
        val tampered = themeJson(expected).toString(Charsets.UTF_8)
            .replace("\"forceDark\":false,", "\"forceDark\":false,\"script\":\"payload\",")
            .toByteArray()

        parseInterfaceStyleTheme(tampered, expected)
    }

    @Test(expected = IllegalArgumentException::class)
    fun v3ThemeRejectsOutOfRangeRenderWork() {
        val expected = packageFor("rain-light", "rain", "light_rain", 4284380326)
        val tampered = themeJson(expected).toString(Charsets.UTF_8)
            .replace("\"primaryCount\":68", "\"primaryCount\":10000")
            .toByteArray()

        parseInterfaceStyleTheme(tampered, expected)
    }

    @Test(expected = IllegalArgumentException::class)
    fun bundleRejectsExecutableOrTraversalEntry() {
        val expected = packageFor("rain-light", "rain", "light_rain", 4284380326)
        val resource = themeJson(expected)
        val manifest = manifestJson(expected, resource)
        zipOf("manifest.json" to manifest, "../payload.dex" to byteArrayOf(1, 2, 3))
            .let { parseInterfaceStyleBundle(it, expected) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun bundleRejectsTamperedResource() {
        val expected = packageFor("rain-light", "rain", "light_rain", 4284380326)
        val resource = themeJson(expected)
        val manifest = manifestJson(expected, resource)
        zipOf("manifest.json" to manifest, "theme.json" to resource + 0)
            .let { parseInterfaceStyleBundle(it, expected) }
    }

    @Test
    fun catalogSignatureAcceptsOnlyMatchingBytes() {
        val keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        val catalog = validCatalog().toByteArray()
        val signer = Signature.getInstance("Ed25519").apply {
            initSign(keyPair.private)
            update(catalog)
        }
        val signature = Base64.getEncoder().encode(signer.sign())

        verifyInterfaceStyleCatalogSignature(
            catalog,
            signature,
            Base64.getEncoder().encodeToString(keyPair.public.encoded),
        )
    }

    private fun validCatalog(): String = """
        {
          "schema": "io.github.fixz.apkesu.interface-style-catalog",
          "version": 3,
          "generatedAt": 1,
          "styles": [
            {
              "id": "season-spring",
              "name": "Spring",
              "summary": "Spring season",
              "engine": "snow",
              "variant": "spring",
              "version": 3,
              "downloadUrl": "https://raw.githubusercontent.com/fixz232/store/main/spring.ksstyle",
              "sha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
              "sizeBytes": 120,
              "accent": 4283404098
            },
            {
              "id": "pixel-cloud-town",
              "name": "Cloud town",
              "summary": "Pixel style",
              "engine": "pixel",
              "variant": "cloud_town",
              "version": 3,
              "downloadUrl": "https://raw.githubusercontent.com/fixz232/store/main/cloud.ksstyle",
              "sha256": "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
              "sizeBytes": 120,
              "accent": 4285308569
            }
          ]
        }
    """.trimIndent()

    private fun packageFor(id: String, engine: String, variant: String?, accent: Long) = InterfaceStylePackage(
        id = id,
        name = id,
        summary = id,
        engine = engine,
        variant = variant,
        version = 3,
        downloadUrl = "https://raw.githubusercontent.com/fixz232/store/main/$id.ksstyle",
        sha256 = "a".repeat(64),
        sizeBytes = 512,
        accent = accent,
    )

    private fun themeBundle(style: InterfaceStylePackage): ByteArray {
        val resource = themeJson(style)
        return zipOf(
            "manifest.json" to manifestJson(style, resource),
            "theme.json" to resource,
        )
    }

    private fun themeJson(style: InterfaceStylePackage): ByteArray = """
        {
          "schema":"io.github.fixz.apkesu.interface-style-theme",
          "version":3,
          "engine":"${style.engine}",
          "variant":${style.variant?.let { "\"$it\"" } ?: "null"},
          "accent":${style.accent},
          "forceDark":false,
          "palette":{
            "light":{"background":"#fff0f4f7","backgroundAlt":"#ffe0e8ed","surface":"#fff8fbfd","surfaceAlt":"#ffeaf0f4","primary":"#ff5e84a6","secondary":"#ffc47b65","outline":"#ff708895","highlight":"#ffffffff","shadow":"#ff263640","muted":"#ffb8c6d4","content":"#ff1b3540"},
            "dark":{"background":"#ff121a20","backgroundAlt":"#ff19242d","surface":"#ff26343e","surfaceAlt":"#ff1b2931","primary":"#ffd5e9f7","secondary":"#ffa4e5df","outline":"#ff82abb2","highlight":"#fff3faff","shadow":"#ff080d13","muted":"#ff789094","content":"#ffe4edf2"}
          },
          "scene":{"cycleMillis":15000,"primaryCount":68,"secondaryCount":6,"speed":1.0,"angle":0.08,"minLengthDp":8.0,"maxLengthDp":17.0,"minStrokeDp":0.38,"maxStrokeDp":0.72,"minAlpha":0.18,"maxAlpha":0.42,"gridDp":18.0,"clearOnCycle":false,"lightning":false,"motifs":[{"type":"line","color":"muted","x":0.1,"y":0.2,"width":0.2,"height":0.02,"alpha":0.2,"strokeDp":0.7,"repeatX":2,"repeatY":2,"driftX":0.02,"driftY":0.0}]},
          "chrome":{"cardAlpha":${if (style.engine == "alpha") "0.96" else "0.76"},"borderAlpha":${if (style.engine == "alpha") "0.72" else "0.6"},"cornerDp":${if (style.engine == "alpha") "8.0" else "14.0"},"unitDp":1.5,"topBarAlpha":${if (style.engine == "alpha") "0.92" else "0.62"},"navigationAlpha":${if (style.engine == "alpha") "0.96" else "0.62"}},
          "glass":{"surfaceAlpha":0.7,"blurDp":12.0,"strokeAlpha":0.55,"refraction":false,"refractionHeightDp":0.0,"refractionAmountDp":0.0,"chromaticAberration":0.0}
        }
    """.trimIndent().toByteArray()

    private fun manifestJson(style: InterfaceStylePackage, resource: ByteArray): ByteArray {
        val hash = MessageDigest.getInstance("SHA-256").digest(resource).joinToString("") { "%02x".format(it) }
        return """
            {"schema":"io.github.fixz.apkesu.interface-style-bundle","version":${style.version},"id":"${style.id}","engine":"${style.engine}","variant":${style.variant?.let { "\"$it\"" } ?: "null"},"resources":[{"name":"theme","path":"theme.json","mimeType":"application/json","sha256":"$hash","sizeBytes":${resource.size}}]}
        """.trimIndent().toByteArray()
    }

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }
}
