package me.weishu.kernelsu.ui.util

import java.io.File
import me.weishu.kernelsu.ui.InterfaceStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudThemeDiscoveryTest {
    private val base = CloudTheme(
        id = "sample", name = "Sample", author = CloudThemeAuthor("author", "Creator", null, null, ""),
        description = "Theme", categoryId = "anime", tags = emptyList(), versionCode = 2,
        versionName = "2", packageSchema = CLOUD_THEME_PACKAGE_SCHEMA, packageVersion = 1,
        minManagerVersionCode = 1, maxManagerVersionCode = null, coverUrl = "https://example.invalid/cover.png",
        screenshotUrls = emptyList(), downloadUrl = "https://example.invalid/theme.kstheme", sha256 = "a".repeat(64),
        sizeBytes = 1, license = "CC0-1.0", changelog = "", publishedAt = 1,
        status = CloudThemePublicationStatus.Published, featured = false, downloadCount = 0,
    )

    private fun discover(
        themes: List<CloudTheme> = listOf(base),
        selection: CloudThemeDiscoverySelection = CloudThemeDiscoverySelection(),
        local: CloudThemeLocalState = CloudThemeLocalState(),
        query: String = "",
        version: Long = 33300,
        style: String = InterfaceStyle.SidebarWidget.value,
        tablet: Boolean = false,
    ) = discoverCloudThemes(themes, local, query, selection, version, style, tablet).map { it.id }

    @Test
    fun appearanceDeviceStyleCategoryAndSearchCanBeCombined() {
        val match = base.copy(id = "match", tags = listOf("dark", "phone", "miuix (Enhanced)"))
        val themes = listOf(match, match.copy(id = "tablet", tags = listOf("dark", "tablet", "miuix")),
            match.copy(id = "light", tags = listOf("light", "phone", "miuix (Enhanced)")),
            match.copy(id = "category", categoryId = "nature"))
        val selected = CloudThemeDiscoverySelection(tone = CloudThemeTone.Dark, device = CloudThemeDevice.Phone,
            style = CloudThemeStyle.Sidebar, categoryId = "anime")
        assertEquals(listOf("match"), discover(themes, selected, query = " CREATOR "))
    }

    @Test
    fun unlabeledThemesRemainInAllWithoutBeingGuessedFromTheirName() {
        val unlabeled = base.copy(name = "Dark phone miuix universal")
        assertEquals(listOf(base.id), discover(listOf(unlabeled)))
        for (selection in listOf(
            CloudThemeDiscoverySelection(tone = CloudThemeTone.Dark),
            CloudThemeDiscoverySelection(tone = CloudThemeTone.Universal),
            CloudThemeDiscoverySelection(device = CloudThemeDevice.Phone),
            CloudThemeDiscoverySelection(device = CloudThemeDevice.Universal),
            CloudThemeDiscoverySelection(style = CloudThemeStyle.Miuix),
            CloudThemeDiscoverySelection(style = CloudThemeStyle.Universal),
        )) assertTrue(discover(listOf(unlabeled), selection).isEmpty())
    }

    @Test
    fun universalAndExplicitDualSupportMatchBothTargets() {
        val universal = base.copy(tags = listOf("universal"))
        for (tone in CloudThemeTone.entries) for (device in CloudThemeDevice.entries) for (style in CloudThemeStyle.entries) {
            assertEquals(listOf(base.id), discover(listOf(universal),
                CloudThemeDiscoverySelection(tone = tone, device = device, style = style)))
        }
        val dual = base.copy(tags = listOf("dark", "light", "phone", "tablet"))
        assertEquals(listOf(base.id), discover(listOf(dual), CloudThemeDiscoverySelection(
            tone = CloudThemeTone.Universal, device = CloudThemeDevice.Universal)))
    }

    @Test
    fun scopedUniversalTagsDoNotLeakIntoOtherDimensions() {
        val theme = base.copy(tags = listOf("device:universal", "mode:dark", "style:miuix"))
        assertEquals(listOf(base.id), discover(listOf(theme), CloudThemeDiscoverySelection(
            tone = CloudThemeTone.Dark, device = CloudThemeDevice.Tablet, style = CloudThemeStyle.Miuix)))
        assertTrue(discover(listOf(theme), CloudThemeDiscoverySelection(tone = CloudThemeTone.Universal)).isEmpty())
        assertTrue(discover(listOf(theme), CloudThemeDiscoverySelection(style = CloudThemeStyle.Universal)).isEmpty())
    }

    @Test
    fun enhancedAndClassicMiuixAreSeparateAndRecognizeExistingChineseTags() {
        val themes = listOf(base.copy(id = "classic", tags = listOf("MIUIX")),
            base.copy(id = "enhanced", tags = listOf("miuix\uFF08\u7f8e\u5316\u7248\uFF09")),
            base.copy(id = "legacy", tags = listOf("\u4fa7\u680f\u5c0f\u7ec4\u4ef6")))
        assertEquals(listOf("classic"), discover(themes, CloudThemeDiscoverySelection(style = CloudThemeStyle.Miuix)))
        assertEquals(listOf("enhanced", "legacy"), discover(themes, CloudThemeDiscoverySelection(style = CloudThemeStyle.Sidebar)))
    }

    @Test
    fun compatibilityUsesInclusiveVersionBoundsAndDoesNotChangeDownloadPolicy() {
        val theme = base.copy(minManagerVersionCode = 33300, maxManagerVersionCode = 33400)
        val compatible = CloudThemeDiscoverySelection(compatibility = CloudThemeCompatibility.Compatible)
        val incompatible = compatible.copy(compatibility = CloudThemeCompatibility.Incompatible)
        for (version in listOf(33300L, 33400L)) {
            assertEquals(listOf(base.id), discover(listOf(theme), compatible, version = version))
            assertTrue(discover(listOf(theme), incompatible, version = version).isEmpty())
        }
        for (version in listOf(33299L, 33401L)) {
            assertTrue(discover(listOf(theme), compatible, version = version).isEmpty())
            assertEquals(listOf(base.id), discover(listOf(theme), incompatible, version = version))
            assertEquals(listOf(base.id), discover(listOf(theme), version = version))
            assertTrue(theme.isCompatible(version))
        }
    }

    @Test
    fun compatibilityRespectsDeclaredStyleAndDeviceSupport() {
        val theme = base.copy(tags = listOf("style:miuix", "device:tablet"))
        val compatible = CloudThemeDiscoverySelection(compatibility = CloudThemeCompatibility.Compatible)
        assertTrue(discover(listOf(theme), compatible, style = "miuix", tablet = false).isEmpty())
        assertTrue(discover(listOf(theme), compatible, style = "sidebar_widget", tablet = true).isEmpty())
        assertEquals(listOf(base.id), discover(listOf(theme), compatible, style = "miuix", tablet = true))
    }

    @Test
    fun newestAndOldestOrdersKeepTheWholeCatalog() {
        val themes = (0 until 40).map { base.copy(id = "theme-$it", publishedAt = it.toLong(), featured = it == 5) }
        val selected = CloudThemeDiscoverySelection(order = CloudThemeOrder.Newest)
        assertEquals(themes.reversed().map { it.id }, discover(themes, selected))
        assertEquals(themes.map { it.id }, discover(themes, selected.copy(order = CloudThemeOrder.Oldest)))
        assertEquals("theme-5", discover(themes).first())
    }

    @Test
    fun usageOrderingHandlesTiesAndNeverTreatsNegativeCountsAsPopularity() {
        val themes = listOf(base.copy(id = "popular", downloadCount = 20), base.copy(id = "low", downloadCount = 1),
            base.copy(id = "zero", downloadCount = 0), base.copy(id = "negative", downloadCount = -1))
        assertEquals(listOf("popular", "low", "negative", "zero"), discover(themes,
            CloudThemeDiscoverySelection(order = CloudThemeOrder.MostUsed)))
        assertEquals(listOf("negative", "zero", "low", "popular"), discover(themes.reversed(),
            CloudThemeDiscoverySelection(order = CloudThemeOrder.LeastUsed)))
    }

    @Test
    fun updatesAndFavoritesKeepTheExistingLocalRecordBehavior() {
        val record = CloudThemeLocalRecord(base.id, 1, "1", "a".repeat(64), "entry", 1)
        val themes = listOf(base, base.copy(id = "hash"), base.copy(id = "current"), base.copy(id = "new"))
        val local = CloudThemeLocalState(favorites = setOf("new"), records = mapOf(
            base.id to record,
            "hash" to record.copy(themeId = "hash", versionCode = 2, sha256 = "b".repeat(64)),
            "current" to record.copy(themeId = "current", versionCode = 2),
        ))
        assertEquals(listOf("hash", base.id), discover(themes,
            CloudThemeDiscoverySelection(browse = CloudThemeBrowseFilter.Updates), local))
        assertEquals(listOf("new"), discover(themes,
            CloudThemeDiscoverySelection(browse = CloudThemeBrowseFilter.Favorites), local))
    }

    @Test
    fun unpublishedThemesStayHiddenForEveryOrder() {
        for (order in CloudThemeOrder.entries) {
            assertTrue(discover(listOf(base.copy(status = CloudThemePublicationStatus.Deprecated)),
                CloudThemeDiscoverySelection(order = order)).isEmpty())
        }
    }

    @Test
    fun currentBundledCatalogWorksWithoutChangingAuthorMetadata() {
        val themes = parseCloudThemeCatalog(File("src/main/assets/theme-store/catalog-v1.json").readText()).themes
        assertEquals(setOf("rikka-mix", "shiroko-memory-horizon-dark"), discover(themes,
            CloudThemeDiscoverySelection(style = CloudThemeStyle.Sidebar)).toSet())
        assertEquals(listOf("shiroko-memory-horizon-dark"), discover(themes,
            CloudThemeDiscoverySelection(tone = CloudThemeTone.Dark, style = CloudThemeStyle.Sidebar)))
    }
}
