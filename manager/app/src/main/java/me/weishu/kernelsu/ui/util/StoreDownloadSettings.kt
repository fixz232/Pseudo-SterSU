package me.weishu.kernelsu.ui.util

import android.content.Context
import androidx.core.content.edit
import java.net.URI
import java.util.Locale

data class StoreDownloadSettings(
    val accelerated: Boolean = true,
    /** Empty means the built-in accelerator. */
    val acceleratorAddress: String = "",
)

private const val PREFS_NAME = "store-download"
private const val ACCELERATED_KEY = "accelerated"
private const val ADDRESS_KEY = "accelerator_address"
private const val LEGACY_PREFS_NAME = "interface-style-download"
private const val DEFAULT_ACCELERATOR = "https://ghproxy.net"
private const val MAX_ADDRESS_LENGTH = 240
private val GITHUB_DOWNLOAD_HOSTS = setOf(
    "github.com",
    "raw.githubusercontent.com",
    "objects.githubusercontent.com",
    "release-assets.githubusercontent.com",
    "githubusercontent.com",
)

private fun isGitHubDownloadHost(host: String): Boolean {
    val normalized = host.lowercase(Locale.ROOT)
    return normalized in GITHUB_DOWNLOAD_HOSTS || normalized.endsWith(".githubusercontent.com")
}

fun readStoreDownloadSettings(context: Context): StoreDownloadSettings {
    val appContext = context.applicationContext
    val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (prefs.contains(ACCELERATED_KEY)) {
        val address = prefs.getString(ADDRESS_KEY, "").orEmpty()
        return StoreDownloadSettings(
            accelerated = prefs.getBoolean(ACCELERATED_KEY, true),
            acceleratorAddress = address.takeIf(::isValidStoreAcceleratorAddress).orEmpty(),
        )
    }

    // Keep an existing interface-style choice when consolidating the store controls.
    val legacy = appContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
    val mode = legacy.getString("proxy_mode", "Auto")
    val address = if (mode == "Custom") legacy.getString("custom_proxy", "").orEmpty() else ""
    return StoreDownloadSettings(
        accelerated = mode != "Direct",
        acceleratorAddress = address.takeIf(::isValidStoreAcceleratorAddress).orEmpty(),
    ).also { saveStoreDownloadSettings(appContext, it) }
}

fun saveStoreDownloadSettings(context: Context, settings: StoreDownloadSettings) {
    require(isValidStoreAcceleratorAddress(settings.acceleratorAddress)) {
        "Accelerator address must be an HTTPS URL without credentials or a query"
    }
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
        putBoolean(ACCELERATED_KEY, settings.accelerated)
        putString(ADDRESS_KEY, settings.acceleratorAddress.trim().removeSuffix("/"))
    }
}

fun isValidStoreAcceleratorAddress(raw: String): Boolean {
    val address = raw.trim()
    if (address.isEmpty()) return true
    if (address.length > MAX_ADDRESS_LENGTH || address.any { it.isWhitespace() }) return false
    val uri = runCatching { URI(address) }.getOrNull() ?: return false
    val host = uri.host?.lowercase(Locale.ROOT) ?: return false
    return uri.scheme.equals("https", ignoreCase = true) &&
        uri.userInfo == null && uri.query == null && uri.fragment == null &&
        uri.port == -1 && !isGitHubDownloadHost(host) && host != "localhost"
}

/** Only validated GitHub resource URLs may be sent to a user-selected accelerator. */
internal fun resolveStoreDownloadUrls(raw: String, settings: StoreDownloadSettings): List<String> {
    val original = URI(raw)
    require(original.scheme.equals("https", ignoreCase = true) &&
        original.host?.let(::isGitHubDownloadHost) == true &&
        original.userInfo == null && original.fragment == null) {
        "Store download URL must be a GitHub HTTPS URL"
    }
    if (!settings.accelerated) return listOf(raw)
    val configured = settings.acceleratorAddress.trim()
    if (!isValidStoreAcceleratorAddress(configured)) return listOf(raw)
    val base = (configured.ifEmpty { DEFAULT_ACCELERATOR }).removeSuffix("/")
    return listOf("$base/$raw", raw)
}
