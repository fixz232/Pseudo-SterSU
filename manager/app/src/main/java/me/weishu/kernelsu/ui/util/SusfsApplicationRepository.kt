package me.weishu.kernelsu.ui.util

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.Natives
import java.io.File

internal class SusfsApplicationRepository(private val context: Context) {
    private companion object {
        // A leaving screen can still be checkpointing one native write while a new screen opens.
        val operationMutex = Mutex()
    }

    private suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        operationMutex.withLock { block() }
    }
    private val controller = SusfsPolicyController(object : SusfsPolicyAccess {
        override fun profile(packageName: String, uid: Int) = Natives.getAppProfileStrict(packageName, uid)
        override fun effectiveHidden(uid: Int) = Natives.uidShouldUmountStrict(uid)
        override fun packages(uid: Int) = context.packageManager.getPackagesForUid(uid)?.toList().orEmpty()
        override fun write(profile: Natives.Profile) = Natives.setAppProfileStrict(profile)
    }, context.packageName)
    private val directory get() = File(context.noBackupFilesDir, "susfs-app-policies")
    private fun file(name: String) = AtomicFile(File(directory, name))

    suspend fun load() = io { loadSusfsApplications(context, controller) }

    suspend fun refreshPolicies(apps: List<SusfsApplication>): List<SusfsApplication> = io {
        val states = apps.distinctBy { it.uid }.associate { app ->
            currentCoroutineContext().ensureActive()
            app.uid to controller.read(app.uid, app.packageNames.first())
        }
        apps.map { it.copy(policy = states.getValue(it.uid)) }
    }

    suspend fun readUid(target: SusfsPolicyTarget): SusfsPolicyState = io {
        controller.read(target.uid, target.packages.first())
    }

    suspend fun prepare(uri: Uri): List<SusfsApplicationHidingConfigEntry> = io {
        val json = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= SUSFS_APP_CONFIG_MAX_BYTES) { "config_too_large" }
                output.write(buffer, 0, count)
            }
            output.toString(Charsets.UTF_8.name())
        } ?: error("open_input_failed")
        parseSusfsApplicationHidingConfig(json)
    }

    suspend fun recoveryEntries(): List<SusfsApplicationHidingConfigEntry> = io {
        parseSusfsApplicationHidingConfig(file("before-change.json").readFully().toString(Charsets.UTF_8))
    }

    suspend fun hasBackup(): Boolean = io {
        File(directory, "before-change.json").exists() || File(directory, "before-change.json.bak").exists()
    }

    suspend fun lastReport(): SusfsApplyReport? = io {
        val report = file("last-operation.json")
        if (!report.baseFile.exists() && !File(directory, "last-operation.json.bak").exists()) null
        else SusfsApplyReport.parse(report.readFully().toString(Charsets.UTF_8))
    }

    private fun save(name: String, content: String) {
        check(directory.isDirectory || directory.mkdirs()) { "backup_directory_failed" }
        val atomic = file(name)
        val bytes = content.toByteArray(Charsets.UTF_8)
        val stream = atomic.startWrite()
        try {
            stream.write(bytes)
            stream.fd.sync()
            atomic.finishWrite(stream)
        } catch (failure: Exception) {
            atomic.failWrite(stream)
            throw failure
        }
        check(atomic.readFully().contentEquals(bytes)) { "backup_verification_failed" }
    }

    suspend fun apply(plan: SusfsImportPlan): SusfsApplyReport = io {
        // Revalidate the whole preview before replacing the recovery snapshot or writing any UID.
        plan.changes.forEach { item ->
            currentCoroutineContext().ensureActive()
            val target = requireNotNull(item.target)
            check(context.packageManager.getPackagesForUid(target.uid)?.sorted() == target.packages.sorted()) {
                "package_uid_changed: ${target.packages.joinToString()}"
            }
            check(controller.read(target.uid, target.packages.first()) == target.state) {
                "preview_expired: ${target.packages.joinToString()}"
            }
        }
        applySusfsPlan(
            plan,
            saveBackup = { json, restoring -> save(if (restoring) "before-restore.json" else "before-change.json", json) },
            saveReport = { save("last-operation.json", it.encode()) },
            change = { target, mode -> controller.change(target, mode) },
        )
    }

    suspend fun export(uri: Uri, apps: List<SusfsApplication>): Int = io {
        val manageable = apps.filter { it.canManage }
        check(manageable.all { it.policy.known }) { "export_unknown_policies" }
        val entries = manageable.filterNot { it.policy.allowSu }.map {
            SusfsApplicationHidingConfigEntry(it.packageName, it.policy.mode)
        }
        val json = encodeSusfsApplicationHidingConfig(entries)
        val output = context.contentResolver.openOutputStream(uri, "wt") ?: error("open_output_failed")
        output.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
        entries.size
    }
}
