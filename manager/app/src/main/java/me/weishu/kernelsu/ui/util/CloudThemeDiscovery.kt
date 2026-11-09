package me.weishu.kernelsu.ui.util

import java.util.Locale
import me.weishu.kernelsu.ui.InterfaceStyle

internal enum class CloudThemeBrowseFilter { All, Featured, Updates, Favorites }
internal enum class CloudThemeOrder { Recommended, Newest, Oldest, MostUsed, LeastUsed }
internal enum class CloudThemeTone { All, Dark, Light, Universal }
internal enum class CloudThemeDevice { All, Phone, Tablet, Universal }
internal enum class CloudThemeCompatibility { All, Compatible, Incompatible }
internal enum class CloudThemeStyle(val interfaceStyle: InterfaceStyle? = null) {
    All, Universal,
    Miuix(InterfaceStyle.Miuix), Material(InterfaceStyle.Material),
    Sidebar(InterfaceStyle.SidebarWidget), Seasons(InterfaceStyle.Snow),
    Rain(InterfaceStyle.Rain), Pixel(InterfaceStyle.Pixel), Glass(InterfaceStyle.LiquidGlass),
    Alpha(InterfaceStyle.Alpha), Delta(InterfaceStyle.Delta), Skrootpro(InterfaceStyle.Skrootpro),
}

internal data class CloudThemeDiscoverySelection(
    val browse: CloudThemeBrowseFilter = CloudThemeBrowseFilter.All,
    val order: CloudThemeOrder = CloudThemeOrder.Recommended,
    val tone: CloudThemeTone = CloudThemeTone.All,
    val device: CloudThemeDevice = CloudThemeDevice.All,
    val compatibility: CloudThemeCompatibility = CloudThemeCompatibility.All,
    val style: CloudThemeStyle = CloudThemeStyle.All,
    val categoryId: String? = null,
)

private val universalTags = setOf("universal", "general", "all", "\u901a\u7528")
private val darkTags = setOf("dark", "darkmode", "night", "\u6697\u8272", "\u6df1\u8272", "\u591c\u95f4")
private val lightTags = setOf("light", "lightmode", "day", "\u4eae\u8272", "\u6d45\u8272", "\u767d\u65e5")
private val phoneTags = setOf("phone", "mobile", "handset", "\u624b\u673a")
private val tabletTags = setOf("tablet", "pad", "\u5e73\u677f")
private val styleTags = mapOf(
    CloudThemeStyle.Miuix to setOf("miuix"),
    CloudThemeStyle.Material to setOf("material", "material3", "md3"),
    CloudThemeStyle.Sidebar to setOf("sidebarwidget", "miuixenhanced", "miuixbeautified", "miuix\u7f8e\u5316\u7248", "\u4fa7\u680f\u7ec4\u4ef6", "\u4fa7\u680f\u5c0f\u7ec4\u4ef6"),
    CloudThemeStyle.Seasons to setOf("snow", "seasons", "fourseasons", "\u56db\u5b63", "\u56db\u5b63\u4e3b\u9898"),
    CloudThemeStyle.Rain to setOf("rain", "\u96e8\u5883"),
    CloudThemeStyle.Pixel to setOf("pixel", "\u50cf\u7d20", "\u50cf\u7d20\u98ce"),
    CloudThemeStyle.Glass to setOf("liquidglass", "frostedglass", "\u6bdb\u73bb\u7483"),
    CloudThemeStyle.Alpha to setOf("alpha"),
    CloudThemeStyle.Delta to setOf("delta"),
    CloudThemeStyle.Skrootpro to setOf("skrootpro"),
)

private fun String.discoveryToken(): String = lowercase(Locale.ROOT)
    .replace(Regex("[\\s_()\uFF08\uFF09-]"), "")

private fun Set<String>.hasTag(aliases: Set<String>, vararg prefixes: String): Boolean =
    any { token -> token in aliases || prefixes.any { prefix -> token.removePrefix("$prefix:") in aliases } }

private class DiscoveryTags(theme: CloudTheme) {
    private val tags = theme.tags.mapTo(mutableSetOf()) { it.discoveryToken() }
    private val universal = tags.any { it in universalTags }
    val dark = tags.hasTag(darkTags, "tone", "mode", "color")
    val light = tags.hasTag(lightTags, "tone", "mode", "color")
    val universalTone = universal || tags.hasTag(universalTags + setOf("daynight", "both"), "tone", "mode", "color") || dark && light
    val phone = tags.hasTag(phoneTags, "device")
    val tablet = tags.hasTag(tabletTags, "device")
    val universalDevice = universal || tags.hasTag(universalTags, "device") || phone && tablet
    val universalStyle = universal || tags.hasTag(universalTags, "style", "ui")
    val styles = styleTags.filterValues { tags.hasTag(it, "style", "ui") }.keys

    fun matchesTone(tone: CloudThemeTone): Boolean = when (tone) {
        CloudThemeTone.All -> true
        CloudThemeTone.Dark -> dark || universalTone
        CloudThemeTone.Light -> light || universalTone
        CloudThemeTone.Universal -> universalTone
    }

    fun matchesDevice(device: CloudThemeDevice): Boolean = when (device) {
        CloudThemeDevice.All -> true
        CloudThemeDevice.Phone -> phone || universalDevice
        CloudThemeDevice.Tablet -> tablet || universalDevice
        CloudThemeDevice.Universal -> universalDevice
    }

    fun matchesStyle(style: CloudThemeStyle): Boolean = when (style) {
        CloudThemeStyle.All -> true
        CloudThemeStyle.Universal -> universalStyle
        else -> universalStyle || style in styles
    }

    fun supportsEnvironment(interfaceStyle: String, tabletDevice: Boolean): Boolean {
        val styleSupported = universalStyle || styles.isEmpty() || styles.any { it.interfaceStyle?.value == interfaceStyle }
        val deviceSupported = universalDevice || !phone && !tablet || if (tabletDevice) tablet else phone
        return styleSupported && deviceSupported
    }
}

/** Declaration-based discovery only; never adds a restriction to downloading or importing. */
internal fun discoverCloudThemes(
    themes: List<CloudTheme>,
    localState: CloudThemeLocalState,
    query: String,
    selection: CloudThemeDiscoverySelection,
    managerVersionCode: Long,
    interfaceStyle: String,
    tabletDevice: Boolean,
): List<CloudTheme> {
    val normalizedQuery = query.trim().lowercase(Locale.ROOT)
    val comparator = when (selection.order) {
        CloudThemeOrder.Recommended -> compareByDescending<CloudTheme> { it.featured }.thenByDescending { it.publishedAt }
        CloudThemeOrder.Newest -> compareByDescending<CloudTheme> { it.publishedAt }
        CloudThemeOrder.Oldest -> compareBy<CloudTheme> { it.publishedAt }
        CloudThemeOrder.MostUsed -> compareByDescending<CloudTheme> { it.downloadCount.coerceAtLeast(0L) }.thenByDescending { it.publishedAt }
        CloudThemeOrder.LeastUsed -> compareBy<CloudTheme> { it.downloadCount.coerceAtLeast(0L) }.thenByDescending { it.publishedAt }
    }.thenBy { it.name.lowercase(Locale.ROOT) }.thenBy { it.id }
    return themes.asSequence()
        .filter { it.status == CloudThemePublicationStatus.Published }
        .filter { selection.categoryId == null || it.categoryId == selection.categoryId }
        .filter { theme ->
            normalizedQuery.isBlank() || (listOf(theme.name, theme.author.name, theme.description) + theme.tags)
                .any { normalizedQuery in it.lowercase(Locale.ROOT) }
        }
        .filter { theme ->
            when (selection.browse) {
                CloudThemeBrowseFilter.All -> true
                CloudThemeBrowseFilter.Featured -> theme.featured
                CloudThemeBrowseFilter.Favorites -> localState.isFavorite(theme.id)
                CloudThemeBrowseFilter.Updates -> localState.record(theme.id)?.let {
                    it.versionCode < theme.versionCode || it.versionCode == theme.versionCode && it.sha256 != theme.sha256
                } == true
            }
        }
        .filter { theme ->
            val tags = DiscoveryTags(theme)
            val compatible = managerVersionCode >= theme.minManagerVersionCode &&
                (theme.maxManagerVersionCode == null || managerVersionCode <= theme.maxManagerVersionCode) &&
                tags.supportsEnvironment(interfaceStyle, tabletDevice)
            tags.matchesTone(selection.tone) && tags.matchesDevice(selection.device) && tags.matchesStyle(selection.style) &&
                when (selection.compatibility) {
                    CloudThemeCompatibility.All -> true
                    CloudThemeCompatibility.Compatible -> compatible
                    CloudThemeCompatibility.Incompatible -> !compatible
                }
        }
        .sortedWith(comparator)
        .toList()
}
