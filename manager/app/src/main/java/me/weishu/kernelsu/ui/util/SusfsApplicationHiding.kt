package me.weishu.kernelsu.ui.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.Natives

/**
 * Risk is a recommendation only. It never enables hiding automatically.
 * The actual hiding state is stored in the kernel app profile for the UID.
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
): Boolean = uid < FIRST_APPLICATION_UID || packageName in setOf(
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
    val hidden: Boolean,
    val allowSu: Boolean,
    val canManage: Boolean,
) {
    val packageName: String
        get() = packageInfo.packageName

    val isRisk: Boolean
        get() = riskSignals.isNotEmpty()
}

internal suspend fun loadSusfsApplications(context: Context): List<SusfsApplication> = withContext(Dispatchers.IO) {
    val packageManager = context.packageManager
    val packageInfos = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        }
    }.getOrDefault(emptyList())

    packageInfos
        .asSequence()
        .mapNotNull { info ->
            val appInfo = info.applicationInfo ?: return@mapNotNull null
            if (appInfo.isResourceOverlay) return@mapNotNull null
            if (isProtectedSusfsPackage(info.packageName, appInfo.uid, context.packageName)) {
                return@mapNotNull null
            }
            val label = runCatching { appInfo.loadLabel(packageManager).toString() }
                .getOrDefault(info.packageName)
            val assessment = assessSusfsAppRisk(
                packageName = info.packageName,
                requestedPermissions = info.requestedPermissions.orEmpty().asList(),
            )
            SusfsPackageCandidate(info, label, assessment)
        }
        .groupBy { it.packageInfo.applicationInfo?.uid ?: -1 }
        .values
        .mapNotNull { candidates ->
            val primary = candidates.minWithOrNull(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.label },
            ) ?: return@mapNotNull null
            val uid = primary.packageInfo.applicationInfo?.uid ?: return@mapNotNull null
            val profile = runCatching {
                Natives.getAppProfile(primary.packageInfo.packageName, uid)
            }.getOrNull()
            val defaultHidden = runCatching { Natives.uidShouldUmount(uid) }.getOrDefault(true)
            val allowSu = profile?.allowSu == true
            SusfsApplication(
                label = if (candidates.size == 1) {
                    primary.label
                } else {
                    "${primary.label} (+${candidates.size - 1})"
                },
                packageInfo = primary.packageInfo,
                packageNames = candidates.map { it.packageInfo.packageName }.sorted(),
                uid = uid,
                riskSignals = candidates.flatMap { it.assessment.signals }.toSet(),
                hidden = !allowSu && (profile?.umountModules ?: defaultHidden),
                allowSu = allowSu,
                canManage = uid >= FIRST_APPLICATION_UID,
            )
        }
        .sortedWith(
            compareByDescending<SusfsApplication> { it.isRisk }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label },
        )
}

internal suspend fun setSusfsApplicationHidden(
    app: SusfsApplication,
    hidden: Boolean,
): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        check(app.canManage) { "protected_app" }
        check(!app.allowSu) { "root_allowed_app" }
        val current = Natives.getAppProfile(app.packageName, app.uid)
            ?: Natives.Profile(app.packageName, app.uid)
        val desired = current.copy(
            name = app.packageName,
            currentUid = app.uid,
            nonRootUseDefault = false,
            umountModules = hidden,
        )
        val updated = Natives.setAppProfile(desired) ||
            (ensureManagerRegistered() && Natives.setAppProfile(desired))
        check(updated) { "profile_update_failed" }
    }
}

private data class SusfsPackageCandidate(
    val packageInfo: PackageInfo,
    val label: String,
    val assessment: SusfsRiskAssessment,
)

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
