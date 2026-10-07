package me.weishu.kernelsu.ui.screen.settings

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import me.weishu.kernelsu.ui.util.getSystemProperty
import me.weishu.kernelsu.ui.util.resolveDeviceName
import java.io.File

internal data class SettingsDeviceInfo(
    val model: String? = null,
    val processor: String? = null,
    val androidVersion: String? = null,
    val totalMemoryBytes: Long? = null,
)

internal fun firstDeviceValue(vararg candidates: String?): String? = candidates
    .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
    .firstOrNull { it.lowercase() !in setOf("unknown", "null", "n/a", "none") }

internal fun processorDisplayName(manufacturer: String?, model: String?): String? {
    val name = firstDeviceValue(model) ?: return null
    val vendor = firstDeviceValue(manufacturer) ?: return name
    return if (name.startsWith(vendor, ignoreCase = true)) name else "$vendor $name"
}

internal fun cpuHardwareValue(cpuInfo: String): String? = cpuInfo.lineSequence()
    .map { it.split(':', limit = 2) }
    .filter { it.size == 2 && it[0].trim().equals("Hardware", ignoreCase = true) }
    .mapNotNull { firstDeviceValue(it[1]) }
    .firstOrNull()

/** Local, read-only information: no root shell, network lookup, or polling. */
internal fun readSettingsDeviceInfo(context: Context): SettingsDeviceInfo {
    val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else null
    val socVendor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MANUFACTURER else null
    val processor = processorDisplayName(socVendor, socModel)
        ?: firstDeviceValue(getSystemProperty("ro.soc.model"), getSystemProperty("ro.vendor.soc.model"))
        ?: runCatching { cpuHardwareValue(File("/proc/cpuinfo").readText()) }.getOrNull()
        ?: firstDeviceValue(getSystemProperty("ro.board.platform"), Build.HARDWARE)
    val memory = runCatching {
        val info = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java)?.getMemoryInfo(info)
        info.totalMem.takeIf { it > 0L }
    }.getOrNull()
    return SettingsDeviceInfo(
        model = firstDeviceValue(runCatching { resolveDeviceName() }.getOrNull(), Build.MODEL),
        processor = processor,
        androidVersion = firstDeviceValue(Build.VERSION.RELEASE),
        totalMemoryBytes = memory,
    )
}

/** Keep labels readable in the narrower pane beside the navigation rail. */
internal fun settingsShortcutColumns(widthDp: Float, fontScale: Float): Int =
    when {
        widthDp / fontScale.coerceAtLeast(1f) >= 240f -> 4
        widthDp / fontScale.coerceAtLeast(1f) >= 192f -> 3
        else -> 2
    }
