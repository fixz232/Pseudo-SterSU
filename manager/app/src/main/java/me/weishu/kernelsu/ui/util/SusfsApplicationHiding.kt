package me.weishu.kernelsu.ui.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.Natives
import org.json.JSONArray
import org.json.JSONObject

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
    val isSystem: Boolean,
) {
    val packageName: String
        get() = packageInfo.packageName

    val isRisk: Boolean
        get() = riskSignals.isNotEmpty()
}

internal suspend fun loadSusfsApplications(context: Context): List<SusfsApplication> = withContext(Dispatchers.IO) {
    val packageManager = context.packageManager
    val queryFlags = PackageManager.GET_PERMISSIONS or
        PackageManager.MATCH_DISABLED_COMPONENTS or
        PackageManager.MATCH_UNINSTALLED_PACKAGES
    val packageInfos = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(
                PackageManager.PackageInfoFlags.of(queryFlags.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(queryFlags)
        }
    }.getOrDefault(emptyList())

    packageInfos
        .asSequence()
        .mapNotNull { info ->
            val appInfo = info.applicationInfo ?: return@mapNotNull null
            if (appInfo.flags and ApplicationInfo.FLAG_INSTALLED == 0) return@mapNotNull null
            if (appInfo.isResourceOverlay) return@mapNotNull null
            if (info.packageName == context.packageName) return@mapNotNull null
            val label = runCatching { appInfo.loadLabel(packageManager).toString() }
                .getOrDefault(info.packageName)
            val assessment = assessSusfsAppRisk(
                packageName = info.packageName,
                requestedPermissions = info.requestedPermissions.orEmpty().asList(),
            )
            SusfsPackageCandidate(
                packageInfo = info,
                label = label,
                assessment = assessment,
                isSystem = appInfo.flags and
                    (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
            )
        }
        .groupBy { it.packageInfo.applicationInfo?.uid ?: -1 }
        .values
        .flatMap { candidates ->
            val primary = candidates.minWithOrNull(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.label },
            ) ?: return@flatMap emptyList()
            val uid = primary.packageInfo.applicationInfo?.uid ?: return@flatMap emptyList()
            val profile = runCatching {
                Natives.getAppProfile(primary.packageInfo.packageName, uid)
            }.getOrNull()
            val defaultHidden = runCatching { Natives.uidShouldUmount(uid) }.getOrDefault(true)
            val allowSu = profile?.allowSu == true
            val sharedPackageNames = candidates.map { it.packageInfo.packageName }.sorted()
            candidates.map { candidate ->
                SusfsApplication(
                    label = candidate.label,
                    packageInfo = candidate.packageInfo,
                    packageNames = sharedPackageNames,
                    uid = uid,
                    riskSignals = candidate.assessment.signals,
                    hidden = !allowSu && (profile?.umountModules ?: defaultHidden),
                    allowSu = allowSu,
                    canManage = !isProtectedSusfsPackage(
                        candidate.packageInfo.packageName,
                        uid,
                        context.packageName,
                    ),
                    isSystem = candidate.isSystem,
                )
            }
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

internal data class SusfsApplicationHidingConfigEntry(
    val packageName: String,
    val hidden: Boolean,
)

internal data class SusfsApplicationHidingImportResult(
    val updated: Int,
    val unchanged: Int,
    val skipped: Int,
    val failed: Int,
)

internal fun encodeSusfsApplicationHidingConfig(
    entries: Iterable<SusfsApplicationHidingConfigEntry>,
    exportedAt: Long = System.currentTimeMillis(),
): String {
    val normalized = entries
        .distinctBy { it.packageName }
        .sortedBy { it.packageName }
        .toList()
    require(normalized.size <= SUSFS_APP_CONFIG_MAX_ENTRIES) { "too_many_entries" }
    val applications = JSONArray()
    normalized.forEach { entry ->
        require(isValidSusfsPackageName(entry.packageName)) { "invalid_package_name" }
        applications.put(
            JSONObject()
                .put("packageName", entry.packageName)
                .put("hidden", entry.hidden),
        )
    }
    return JSONObject()
        .put("schema", SUSFS_APP_CONFIG_SCHEMA)
        .put("version", SUSFS_APP_CONFIG_VERSION)
        .put("exportedAt", exportedAt.coerceAtLeast(0L))
        .put("applications", applications)
        .toString(2)
}

internal fun parseSusfsApplicationHidingConfig(json: String): List<SusfsApplicationHidingConfigEntry> {
    require(json.toByteArray(Charsets.UTF_8).size <= SUSFS_APP_CONFIG_MAX_BYTES) { "config_too_large" }
    val root = JSONObject(json)
    require(root.optString("schema") == SUSFS_APP_CONFIG_SCHEMA) { "invalid_schema" }
    require(root.optInt("version") == SUSFS_APP_CONFIG_VERSION) { "unsupported_version" }
    val applications = root.optJSONArray("applications") ?: error("missing_applications")
    require(applications.length() <= SUSFS_APP_CONFIG_MAX_ENTRIES) { "too_many_entries" }
    val seen = hashSetOf<String>()
    return buildList {
        for (index in 0 until applications.length()) {
            val item = applications.optJSONObject(index) ?: error("invalid_entry")
            val packageName = item.optString("packageName").trim()
            require(isValidSusfsPackageName(packageName)) { "invalid_package_name" }
            require(seen.add(packageName)) { "duplicate_package" }
            require(item.has("hidden")) { "missing_hidden_state" }
            add(SusfsApplicationHidingConfigEntry(packageName, item.getBoolean("hidden")))
        }
    }
}

internal suspend fun importSusfsApplicationHidingConfig(
    context: Context,
    json: String,
): SusfsApplicationHidingImportResult = withContext(Dispatchers.IO) {
    val entries = parseSusfsApplicationHidingConfig(json)
    val applications = loadSusfsApplications(context)
    val byPackage = applications.associateBy { it.packageName }
    val desiredByUid = linkedMapOf<Int, Pair<SusfsApplication, Boolean>>()
    var skipped = 0

    entries.forEach { entry ->
        val application = byPackage[entry.packageName]
        if (application == null || !application.canManage || application.allowSu) {
            skipped++
            return@forEach
        }
        val existing = desiredByUid[application.uid]
        require(existing == null || existing.second == entry.hidden) { "shared_uid_conflict" }
        desiredByUid[application.uid] = application to entry.hidden
    }

    var updated = 0
    var unchanged = 0
    var failed = 0
    desiredByUid.values.forEach { (application, hidden) ->
        if (application.hidden == hidden) {
            unchanged++
        } else if (setSusfsApplicationHidden(application, hidden).isSuccess) {
            updated++
        } else {
            failed++
        }
    }
    SusfsApplicationHidingImportResult(updated, unchanged, skipped, failed)
}

private data class SusfsPackageCandidate(
    val packageInfo: PackageInfo,
    val label: String,
    val assessment: SusfsRiskAssessment,
    val isSystem: Boolean,
)

private const val FIRST_APPLICATION_UID = 10_000
private const val SUSFS_APP_CONFIG_SCHEMA = "io.github.fixz.stersu.susfs-app-hiding"
private const val SUSFS_APP_CONFIG_VERSION = 1
private const val SUSFS_APP_CONFIG_MAX_BYTES = 512 * 1024
private const val SUSFS_APP_CONFIG_MAX_ENTRIES = 5_000

private val SUSFS_PACKAGE_NAME_PATTERN = Regex("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+")

private fun isValidSusfsPackageName(packageName: String): Boolean =
    packageName.length in 3..255 && SUSFS_PACKAGE_NAME_PATTERN.matches(packageName)

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
