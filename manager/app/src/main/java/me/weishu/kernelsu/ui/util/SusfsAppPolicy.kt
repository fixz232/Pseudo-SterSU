package me.weishu.kernelsu.ui.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import me.weishu.kernelsu.Natives
import org.json.JSONArray
import org.json.JSONObject

internal enum class SusfsHideMode(val wire: String) {
    Default("default"), Enabled("enabled"), Disabled("disabled");

    companion object {
        fun fromProfile(profile: Natives.Profile?): SusfsHideMode = when {
            profile == null || profile.nonRootUseDefault -> Default
            profile.umountModules -> Enabled
            else -> Disabled
        }
    }
}

/** This describes the configured kernel policy, NOT a probe of an application's process. */
internal data class SusfsPolicyState(
    val mode: SusfsHideMode = SusfsHideMode.Default,
    val effectiveHidden: Boolean? = null,
    val allowSu: Boolean = false,
    val error: String? = null,
) {
    val known: Boolean get() = error == null && effectiveHidden != null
}

internal data class SusfsPolicyTarget(
    val uid: Int,
    val packages: List<String>,
    val protected: Boolean,
    val state: SusfsPolicyState,
)

internal interface SusfsPolicyAccess {
    fun profile(packageName: String, uid: Int): Natives.Profile?
    fun effectiveHidden(uid: Int): Boolean
    fun packages(uid: Int): List<String>
    fun write(profile: Natives.Profile): Boolean
}

internal class SusfsPolicyController(
    private val access: SusfsPolicyAccess,
    private val managerPackage: String,
) {
    fun read(uid: Int, packageName: String): SusfsPolicyState = try {
        val profile = access.profile(packageName, uid)
        val mode = SusfsHideMode.fromProfile(profile)
        val effective = access.effectiveHidden(uid)
        val allowSu = profile?.allowSu == true
        // A grant or policy can change between the two IOCTLs. Do not display a mixed snapshot.
        check(if (allowSu) !effective else mode == SusfsHideMode.Default || effective == (mode == SusfsHideMode.Enabled)) {
            "inconsistent_policy_read"
        }
        // Even explicit policies use the checked kernel query (root and default rules included).
        SusfsPolicyState(
            mode = mode,
            effectiveHidden = effective,
            allowSu = allowSu,
        )
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        SusfsPolicyState(error = failure.message ?: failure.javaClass.simpleName)
    }

    fun change(target: SusfsPolicyTarget, mode: SusfsHideMode): SusfsPolicyState {
        check(!target.protected && target.packages.isNotEmpty()) { "protected_app" }
        check(target.state.known) { "unknown_policy" }
        val packages = access.packages(target.uid).sorted()
        check(packages == target.packages.sorted()) { "package_uid_changed" }
        check(packages.none { isProtectedSusfsPackage(it, target.uid, managerPackage) }) { "protected_app" }
        val current = access.profile(packages.first(), target.uid)
        check(current?.allowSu != true) { "root_allowed_app" }
        check(SusfsHideMode.fromProfile(current) == target.state.mode) { "policy_changed" }
        check(access.effectiveHidden(target.uid) == target.state.effectiveHidden) { "policy_changed" }
        val desired = (current ?: Natives.Profile(packages.first(), target.uid)).copy(
            name = packages.first(),
            currentUid = target.uid,
            nonRootUseDefault = mode == SusfsHideMode.Default,
            umountModules = when (mode) {
                SusfsHideMode.Default -> current?.umountModules ?: true
                SusfsHideMode.Enabled -> true
                SusfsHideMode.Disabled -> false
            },
        )
        check(access.write(desired)) { "profile_update_failed" }
        val verified = read(target.uid, packages.first())
        check(verified.known) { "readback_failed: ${verified.error}" }
        check(!verified.allowSu && verified.mode == mode) { "readback_mismatch" }
        check(mode == SusfsHideMode.Default || verified.effectiveHidden == (mode == SusfsHideMode.Enabled)) {
            "readback_mismatch"
        }
        return verified
    }
}

internal data class SusfsApplicationHidingConfigEntry(
    val packageName: String,
    val mode: SusfsHideMode,
    // Only recovery snapshots bind to a UID. Portable user exports intentionally omit it.
    val expectedUid: Int? = null,
) {
    constructor(packageName: String, hidden: Boolean) : this(
        packageName, if (hidden) SusfsHideMode.Enabled else SusfsHideMode.Disabled,
    )
}

internal const val SUSFS_APP_CONFIG_MAX_BYTES = 512 * 1024
private const val SUSFS_APP_CONFIG_SCHEMA = "io.github.fixz.stersu.susfs-app-hiding"
private const val SUSFS_APP_CONFIG_MAX_ENTRIES = 5_000
private val SUSFS_PACKAGE_NAME_PATTERN = Regex("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+")

private fun validPackage(name: String) = name.length in 3..255 && SUSFS_PACKAGE_NAME_PATTERN.matches(name)

internal fun encodeSusfsApplicationHidingConfig(
    entries: Iterable<SusfsApplicationHidingConfigEntry>,
    exportedAt: Long = System.currentTimeMillis(),
): String {
    val normalized = entries.toList().sortedBy { it.packageName }
    require(normalized.size <= SUSFS_APP_CONFIG_MAX_ENTRIES) { "too_many_entries" }
    require(normalized.distinctBy { it.packageName }.size == normalized.size) { "duplicate_package" }
    val applications = JSONArray()
    normalized.forEach { entry ->
        require(validPackage(entry.packageName)) { "invalid_package_name" }
        applications.put(JSONObject().put("packageName", entry.packageName).put("mode", entry.mode.wire).apply {
            entry.expectedUid?.let { require(it >= 0); put("uid", it) }
        })
    }
    return JSONObject().put("schema", SUSFS_APP_CONFIG_SCHEMA).put("version", 2)
        .put("exportedAt", exportedAt.coerceAtLeast(0L)).put("applications", applications)
        .toString(2).also {
            require(it.toByteArray(Charsets.UTF_8).size <= SUSFS_APP_CONFIG_MAX_BYTES) { "config_too_large" }
        }
}

internal fun parseSusfsApplicationHidingConfig(json: String): List<SusfsApplicationHidingConfigEntry> {
    require(json.toByteArray(Charsets.UTF_8).size <= SUSFS_APP_CONFIG_MAX_BYTES) { "config_too_large" }
    val root = JSONObject(json)
    require(root.optString("schema") == SUSFS_APP_CONFIG_SCHEMA) { "invalid_schema" }
    val version = root.opt("version")
    require(version == 1 || version == 2) { "unsupported_version" }
    val applications = root.optJSONArray("applications") ?: error("missing_applications")
    require(applications.length() <= SUSFS_APP_CONFIG_MAX_ENTRIES) { "too_many_entries" }
    val seen = hashSetOf<String>()
    return List(applications.length()) { index ->
        val item = applications.optJSONObject(index) ?: error("invalid_entry")
        val packageName = item.opt("packageName") as? String ?: error("invalid_package_name")
        require(validPackage(packageName)) { "invalid_package_name" }
        require(seen.add(packageName)) { "duplicate_package" }
        val mode = if (version == 1) {
            val hidden = item.opt("hidden") as? Boolean ?: error("invalid_hidden_state")
            if (hidden) SusfsHideMode.Enabled else SusfsHideMode.Disabled
        } else {
            SusfsHideMode.entries.firstOrNull { it.wire == item.opt("mode") } ?: error("invalid_mode")
        }
        val uid = if (item.has("uid")) {
            (item.opt("uid") as? Int)?.also { require(it >= 0) { "invalid_uid" } } ?: error("invalid_uid")
        } else null
        SusfsApplicationHidingConfigEntry(packageName, mode, uid)
    }
}

internal enum class SusfsPlanAction { Change, Unchanged, Skipped, Conflict }
internal data class SusfsPlanItem(
    val packages: List<String>,
    val target: SusfsPolicyTarget?,
    val desired: SusfsHideMode,
    val action: SusfsPlanAction,
    val reason: String = "",
)
internal data class SusfsImportPlan(val items: List<SusfsPlanItem>, val restoring: Boolean = false) {
    val changes get() = items.filter { it.action == SusfsPlanAction.Change }
    val canApply get() = changes.isNotEmpty() && items.none { it.action == SusfsPlanAction.Conflict }
}

/** Build a dry-run plan; no profile writes occur here. Counts are per UID, not per alias. */
internal fun planSusfsImport(
    entries: List<SusfsApplicationHidingConfigEntry>,
    targets: List<SusfsPolicyTarget>,
    restoring: Boolean = false,
): SusfsImportPlan {
    val byPackage = targets.flatMap { target -> target.packages.map { it to target } }.toMap()
    val missing = entries.filter { it.packageName !in byPackage }.map {
        SusfsPlanItem(listOf(it.packageName), null, it.mode, SusfsPlanAction.Skipped, "not_installed")
    }
    val grouped = entries.filter { it.packageName in byPackage }.groupBy { byPackage.getValue(it.packageName).uid }
    return SusfsImportPlan(missing + grouped.values.map { group ->
        val target = byPackage.getValue(group.first().packageName)
        val mode = group.first().mode
        val reason = when {
            group.any { it.mode != mode } -> "shared_uid_conflict"
            group.any { it.expectedUid != null && it.expectedUid != target.uid } -> "package_uid_changed"
            target.protected -> "protected_app"
            !target.state.known -> "unknown_policy: ${target.state.error.orEmpty()}"
            target.state.allowSu -> "root_allowed_app"
            else -> ""
        }
        val action = when {
            reason == "shared_uid_conflict" -> SusfsPlanAction.Conflict
            reason.isNotEmpty() -> SusfsPlanAction.Skipped
            target.state.mode == mode -> SusfsPlanAction.Unchanged
            else -> SusfsPlanAction.Change
        }
        SusfsPlanItem(target.packages, target, mode, action, reason)
    }, restoring)
}

internal data class SusfsApplyItem(val packages: List<String>, val status: String, val detail: String = "")
internal data class SusfsApplyReport(val at: Long, val items: List<SusfsApplyItem>) {
    fun encode(): String = JSONObject().put("at", at).put("items", JSONArray().apply {
        items.forEach { put(JSONObject().put("packages", JSONArray(it.packages)).put("status", it.status).put("detail", it.detail)) }
    }).toString(2)

    companion object {
        fun parse(json: String): SusfsApplyReport {
            val root = JSONObject(json)
            val items = root.getJSONArray("items")
            return SusfsApplyReport(root.getLong("at"), List(items.length()) { index ->
                val item = items.getJSONObject(index)
                val packages = item.getJSONArray("packages")
                SusfsApplyItem(List(packages.length()) { packages.getString(it) }, item.getString("status"), item.optString("detail"))
            })
        }
    }
}

/** Synchronous checkpoints live inside the IO operation; pending entries survive process death. */
internal suspend fun applySusfsPlan(
    plan: SusfsImportPlan,
    saveBackup: (String, Boolean) -> Unit,
    saveReport: (SusfsApplyReport) -> Unit,
    change: (SusfsPolicyTarget, SusfsHideMode) -> Unit,
): SusfsApplyReport {
    check(plan.canApply) { "no_changes_or_conflicts" }
    val backup = plan.changes.flatMap { item ->
        val target = requireNotNull(item.target)
        target.packages.map { SusfsApplicationHidingConfigEntry(it, target.state.mode, target.uid) }
    }
    currentCoroutineContext().ensureActive()
    saveBackup(encodeSusfsApplicationHidingConfig(backup), plan.restoring)
    var report = SusfsApplyReport(System.currentTimeMillis(), plan.items.map {
        SusfsApplyItem(it.packages, if (it.action == SusfsPlanAction.Change) "pending" else it.action.name.lowercase(), it.reason)
    })
    saveReport(report) // A checkpoint failure must stop the batch before any additional writes.
    plan.items.forEachIndexed { index, item ->
        currentCoroutineContext().ensureActive()
        if (item.action != SusfsPlanAction.Change) return@forEachIndexed
        val result = try {
            change(requireNotNull(item.target), item.desired)
            SusfsApplyItem(item.packages, "applied")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            SusfsApplyItem(item.packages, "failed", failure.message ?: failure.javaClass.simpleName)
        }
        report = report.copy(items = report.items.toMutableList().also { it[index] = result })
        saveReport(report)
    }
    return report
}
