package me.weishu.kernelsu.ui.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Risk is a recommendation only. It never enables hiding automatically.
 * The configured policy is stored in the kernel app profile for the UID.
 */
internal enum class SusfsRiskSignal {
    RootOrIntegrityName,
    SensitivePermission,
}

internal data class SusfsRiskAssessment(
    val signals: Set<SusfsRiskSignal> = emptySet(),
)

internal fun assessSusfsAppRisk(
    packageName: String,
    requestedPermissions: Iterable<String> = emptyList(),
): SusfsRiskAssessment {
    val normalizedPackage = packageName.lowercase()
    val packageSignal = RISK_PACKAGE_MARKERS.any(normalizedPackage::contains)
    val permissionSignal = requestedPermissions.any { permission ->
        permission in RISK_PERMISSIONS
    }
    return SusfsRiskAssessment(
        buildSet {
            if (packageSignal) add(SusfsRiskSignal.RootOrIntegrityName)
            if (permissionSignal) add(SusfsRiskSignal.SensitivePermission)
        },
    )
}

internal fun isProtectedSusfsPackage(
    packageName: String,
    uid: Int,
    managerPackage: String,
): Boolean = uid < 0 || uid % 100_000 < FIRST_APPLICATION_UID || packageName in setOf(
    managerPackage,
    "android",
    "com.android.systemui",
    "com.android.shell",
    "com.android.settings",
)

internal data class SusfsApplication(
    val label: String,
    val packageInfo: PackageInfo,
    val packageNames: List<String>,
    val uid: Int,
    val riskSignals: Set<SusfsRiskSignal>,
    val policy: SusfsPolicyState,
    val canManage: Boolean,
    val isSystem: Boolean,
) {
    val packageName: String get() = packageInfo.packageName
    val isRisk: Boolean get() = riskSignals.isNotEmpty()
    val target: SusfsPolicyTarget get() = SusfsPolicyTarget(uid, packageNames, !canManage, policy)
}

internal suspend fun loadSusfsApplications(
    context: Context,
    controller: SusfsPolicyController,
): List<SusfsApplication> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val flags = PackageManager.GET_PERMISSIONS or PackageManager.MATCH_DISABLED_COMPONENTS or
        PackageManager.MATCH_UNINSTALLED_PACKAGES
    // A failed query is not an empty device. Let the caller show a retryable load error.
    val infos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        pm.getInstalledPackages(flags)
    }
    infos.filter { info ->
        info.applicationInfo?.let { it.flags and ApplicationInfo.FLAG_INSTALLED != 0 } == true
    }
        .groupBy { requireNotNull(it.applicationInfo).uid }
        .flatMap { (uid, group) ->
            currentCoroutineContext().ensureActive()
            // Include manager and overlays in group protection, even if their rows are hidden.
            val packages = (pm.getPackagesForUid(uid)?.toList().orEmpty() +
                group.map { it.packageName }).distinct().sorted()
            val protected = packages.any { isProtectedSusfsPackage(it, uid, context.packageName) }
            val policy = controller.read(uid, packages.first())
            group.filter { it.packageName != context.packageName && it.applicationInfo?.isResourceOverlay != true }
                .map { info ->
                    val appInfo = requireNotNull(info.applicationInfo)
                    SusfsApplication(
                        label = runCatching { appInfo.loadLabel(pm).toString() }.getOrDefault(info.packageName),
                        packageInfo = info,
                        packageNames = packages,
                        uid = uid,
                        riskSignals = assessSusfsAppRisk(info.packageName, info.requestedPermissions.orEmpty().asList()).signals,
                        policy = policy,
                        canManage = !protected,
                        isSystem = appInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
                    )
                }
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
}

internal fun updateSusfsUid(
    applications: List<SusfsApplication>,
    uid: Int,
    policy: SusfsPolicyState,
): List<SusfsApplication> = applications.map { if (it.uid == uid) it.copy(policy = policy) else it }

private const val FIRST_APPLICATION_UID = 10_000

private val RISK_PACKAGE_MARKERS = setOf(
    "bank",
    "wallet",
    "payment",
    "finance",
    "safetynet",
    "integrity",
    "deviceadmin",
    "security",
    "root",
    "magisk",
    "kernelsu",
    "supersu",
    "xposed",
    "lsposed",
    "frida",
    "shizuku",
)

private val RISK_PERMISSIONS = setOf(
    "android.permission.REQUEST_INSTALL_PACKAGES",
    "android.permission.SYSTEM_ALERT_WINDOW",
    "android.permission.PACKAGE_USAGE_STATS",
    "android.permission.QUERY_ALL_PACKAGES",
    "android.permission.MANAGE_EXTERNAL_STORAGE",
    "android.permission.BIND_ACCESSIBILITY_SERVICE",
)
