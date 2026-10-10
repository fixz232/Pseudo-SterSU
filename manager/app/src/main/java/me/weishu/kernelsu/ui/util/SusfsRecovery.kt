package me.weishu.kernelsu.ui.util

import android.os.Process
import android.util.AtomicFile
import android.util.Log
import com.topjohnwu.superuser.Shell
import java.io.File
import java.io.FileInputStream
import java.io.DataInputStream
import java.nio.file.Files
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.ksuApp
import org.json.JSONObject

private const val TAG = "SusfsRecovery"
private const val MAX_TOOL_BYTES = 8 * 1024 * 1024L
private const val MAX_SNAPSHOT_BYTES = 512 * 1024L
private val TOOL_HASH_PATTERN = Regex("[0-9a-f]{64}")
private val BACKUPABLE_TOOLS = setOf(
    "/data/adb/ksu/bin/ksu_susfs",
    "/data/adb/ap/bin/ksu_susfs",
    "/system/bin/ksu_susfs",
)
private const val RESTORED_TOOL = "/data/adb/ksu/bin/ksu_susfs"
private const val BUNDLED_TOOL = "ksud_susfs"
private val captureMutex = Mutex()
private val restoreMutex = Mutex()

data class SusfsRecoveryResult(val success: Boolean = false, val error: String = "")

private data class SusfsSnapshot(
    val config: SusfsPathConfigState,
    val tool: File?,
    val sha256: String,
    val version: String,
)

private fun recoveryDir(): File = File(ksuApp.noBackupFilesDir, "susfs-recovery")

private fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    FileInputStream(file).use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
}

private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
    .digest(bytes).joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

internal fun isSupportedSusfsToolElf(header: ByteArray): Boolean =
    header.size >= 20 && header[0] == 0x7f.toByte() &&
        header[1] == 'E'.code.toByte() && header[2] == 'L'.code.toByte() &&
        header[3] == 'F'.code.toByte() && header[4] == 2.toByte() &&
        header[5] == 1.toByte() && header[19] == 0.toByte() &&
        (header[18] == 183.toByte() || header[18] == 62.toByte())

internal fun validSusfsRecoveryTool(file: File, expectedHash: String, expectedSize: Long): Boolean {
    if (!TOOL_HASH_PATTERN.matches(expectedHash) || expectedSize !in 1024..MAX_TOOL_BYTES) return false
    if (Files.isSymbolicLink(file.toPath()) || !file.isFile || file.length() != expectedSize) return false
    val header = ByteArray(20)
    DataInputStream(FileInputStream(file)).use { it.readFully(header) }
    return isSupportedSusfsToolElf(header) && sha256(file) == expectedHash
}

private fun readSnapshot(): SusfsSnapshot? = runCatching {
    val dir = recoveryDir()
    val metadata = File(dir, "snapshot.json")
    val atomic = AtomicFile(metadata)
    if (Files.isSymbolicLink(metadata.toPath()) ||
        (!metadata.isFile && !File("${metadata.absolutePath}.bak").isFile)
    ) {
        return@runCatching null
    }
    val contents = atomic.openRead().use { it.readBytes() }
    if (contents.size.toLong() !in 1..MAX_SNAPSHOT_BYTES) return@runCatching null
    val json = JSONObject(contents.toString(Charsets.UTF_8))
    val format = json.optInt("format")
    if (format != 1 && format != 2) return@runCatching null
    val version = json.optString("tool_version")
    if (parseSusfsVersion(version) == null) return@runCatching null
    val configJson = if (format == 1) {
        json.optJSONObject("config")?.toString()
    } else {
        json.optString("config_json").takeIf(String::isNotBlank)
    } ?: return@runCatching null
    val hash = json.optString("sha256")
    val tool = if (format == 1) {
        if (!TOOL_HASH_PATTERN.matches(hash)) return@runCatching null
        File(dir, "tool-$hash").takeIf { validSusfsRecoveryTool(it, hash, json.optLong("tool_size")) }
            ?: return@runCatching null
    } else {
        if (!TOOL_HASH_PATTERN.matches(hash) || sha256(configJson.toByteArray(Charsets.UTF_8)) != hash) {
            return@runCatching null
        }
        null
    }
    val config = parseSusfsBackupJson(configJson).config
        ?: return@runCatching null
    if (validateSusfsConfig(config).isNotEmpty()) return@runCatching null
    SusfsSnapshot(config, tool, hash, version)
}.onFailure { Log.w(TAG, "read recovery snapshot failed", it) }.getOrNull()

suspend fun hasSusfsRecoverySnapshot(): Boolean = withContext(Dispatchers.IO) {
    captureMutex.lock()
    try {
        readSnapshot() != null
    } finally {
        captureMutex.unlock()
    }
}

private suspend fun rootCommand(command: String): Pair<Boolean, List<String>> {
    val stdout = ArrayList<String>()
    val stderr = ArrayList<String>()
    val result: Shell.Result? = runCatching {
        withTimeoutOrNull(20_000L) {
            getRootShell().newJob().add(command).to(stdout, stderr).exec()
        }
    }.onFailure { Log.w(TAG, "recovery root command failed", it) }.getOrNull()
    if (result == null) KsuCli.reset()
    return (result?.isSuccess == true) to stdout
}

private fun quote(value: String): String = "'${value.replace("'", "'\"'\"'")}'"

internal fun configForSusfsReinitialization(current: SusfsPathConfigState): SusfsPathConfigState {
    val hasRules = current.paths.isNotEmpty() || current.loopPaths.isNotEmpty() ||
        current.susMaps.isNotEmpty() || current.openRedirects.isNotEmpty() ||
        current.kstatEntries.isNotEmpty() || current.logging || current.avcLogSpoofing ||
        current.hideSusMntsForNonSuProcs || current.unameRelease.isNotBlank() ||
        current.unameVersion.isNotBlank() || current.cmdlineOrBootconfig.isNotBlank()
    return current.copy(enabled = hasRules && current.enabled)
}

/** Snapshot a verified GKI configuration; copy an external tool only when one is used. */
suspend fun captureSusfsRecoverySnapshot(): SusfsRecoveryResult =
    withContext(Dispatchers.IO) {
        captureMutex.lock()
        try {
        if (Natives.isLateLoadMode || Natives.isLkmMode) return@withContext SusfsRecoveryResult(error = "gki_mode_required")
        val current = getSusfsPathConfig()
        if (!current.available || current.runtimeStatus.generation.isBlank()) {
            return@withContext SusfsRecoveryResult(error = "configuration_unavailable")
        }
        val bundled = current.toolPath == BUNDLED_TOOL
        if ((!bundled && current.toolPath !in BACKUPABLE_TOOLS) ||
            parseSusfsVersion(current.capabilities.version) == null
        ) {
            return@withContext SusfsRecoveryResult(error = "tool_source_untrusted")
        }
        if (validateSusfsConfig(current).isNotEmpty()) return@withContext SusfsRecoveryResult(error = "invalid_config")
        val (configReady, generations) = rootCommand(
            "for settings in /data/adb/ksu/susfs/current/settings.conf /data/adb/ksu/susfs/settings.conf; do " +
                "if [ -f \"${'$'}settings\" ]; then sed -n 's/^generation=//p' \"${'$'}settings\" | sed -n '1p'; exit 0; fi; " +
                "done; exit 1",
        )
        if (!configReady || generations.firstOrNull() != current.runtimeStatus.generation) {
            return@withContext SusfsRecoveryResult(error = "configuration_unavailable")
        }
        if (bundled) {
            val dir = recoveryDir()
            val configJson = buildSusfsBackupJson(current)
            val metadata = JSONObject()
                .put("format", 2)
                .put("sha256", sha256(configJson.toByteArray(Charsets.UTF_8)))
                .put("tool_version", current.capabilities.version)
                .put("config_json", configJson)
            return@withContext runCatching {
                if (!dir.isDirectory && !dir.mkdirs()) error("create recovery directory failed")
                val atomic = AtomicFile(File(dir, "snapshot.json"))
                val stream = atomic.startWrite()
                try {
                    stream.write(metadata.toString().toByteArray(Charsets.UTF_8))
                    atomic.finishWrite(stream)
                } catch (error: Exception) {
                    atomic.failWrite(stream)
                    throw error
                }
                if (readSnapshot() != null) SusfsRecoveryResult(success = true)
                else SusfsRecoveryResult(error = "backup_verification_failed")
            }.onFailure { Log.w(TAG, "capture bundled SUSFS snapshot failed", it) }
                .getOrElse { SusfsRecoveryResult(error = "backup_storage_failed") }
        }
        val dir = recoveryDir()
        val pending = runCatching {
            if (!dir.isDirectory && !dir.mkdirs()) error("create recovery directory failed")
            File.createTempFile("tool-", ".pending", dir)
        }.getOrElse { error ->
            Log.w(TAG, "prepare recovery snapshot failed", error)
            return@withContext SusfsRecoveryResult(error = "backup_storage_failed")
        }
        try {
            val uid = Process.myUid()
            val source = quote(current.toolPath)
            val target = quote(pending.absolutePath)
            val expectedVersion = quote(current.capabilities.version)
            val command = """
                set -eu
                source=$source
                target=$target
                [ -f "${'$'}source" ] && [ -x "${'$'}source" ] && [ ! -L "${'$'}source" ]
                [ "${'$'}(stat -c %u "${'$'}source")" = 0 ]
                mode=${'$'}(stat -c %a "${'$'}source")
                case "${'$'}mode" in [0-7][0-7][0-7]|[0-7][0-7][0-7][0-7]) ;; *) exit 1 ;; esac
                [ ${'$'}((0${'$'}mode & 022)) -eq 0 ]
                size=${'$'}(stat -c %s "${'$'}source")
                [ "${'$'}size" -ge 1024 ] && [ "${'$'}size" -le $MAX_TOOL_BYTES ]
                [ "${'$'}("${'$'}source" show version 2>/dev/null | sed -n '1p')" = $expectedVersion ]
                cp "${'$'}source" "${'$'}target"
                chown $uid:$uid "${'$'}target"
                chmod 0600 "${'$'}target"
                source_hash=${'$'}(sha256sum "${'$'}source" | cut -d ' ' -f 1)
                target_hash=${'$'}(sha256sum "${'$'}target" | cut -d ' ' -f 1)
                [ "${'$'}source_hash" = "${'$'}target_hash" ]
                printf '__HASH__=%s\n' "${'$'}target_hash"
            """.trimIndent()
            val (copied, output) = rootCommand(command)
            val rootHash = output.firstOrNull { it.startsWith("__HASH__=") }?.substringAfter('=')
            if (!copied || !TOOL_HASH_PATTERN.matches(rootHash.orEmpty())) {
                return@withContext SusfsRecoveryResult(error = "tool_backup_failed")
            }
            val hash = rootHash.orEmpty()
            if (!validSusfsRecoveryTool(pending, hash, pending.length())) return@withContext SusfsRecoveryResult(error = "tool_integrity_failed")
            val tool = File(dir, "tool-$hash")
            if (!tool.isFile || !validSusfsRecoveryTool(tool, hash, pending.length())) {
                if (!pending.renameTo(tool)) return@withContext SusfsRecoveryResult(error = "backup_storage_failed")
            }
            val metadata = JSONObject()
                .put("format", 1)
                .put("sha256", hash)
                .put("tool_size", tool.length())
                .put("tool_version", current.capabilities.version)
                .put("config", JSONObject(buildSusfsBackupJson(current)))
            val atomic = AtomicFile(File(dir, "snapshot.json"))
            val stream = atomic.startWrite()
            try {
                stream.write(metadata.toString().toByteArray(Charsets.UTF_8))
                atomic.finishWrite(stream)
            } catch (error: Exception) {
                atomic.failWrite(stream)
                throw error
            }
            if (readSnapshot() != null) SusfsRecoveryResult(success = true)
            else SusfsRecoveryResult(error = "backup_verification_failed")
        } catch (error: Exception) {
            Log.w(TAG, "capture recovery snapshot failed", error)
            SusfsRecoveryResult(error = "backup_storage_failed")
        } finally {
            pending.delete()
        }
        } finally {
            captureMutex.unlock()
        }
    }

/** Never overwrite an existing tool; restore only a byte-for-byte verified private copy. */
suspend fun restoreSusfsFromSnapshot(): SusfsRecoveryResult {
    restoreMutex.lock()
    try {
        return restoreSusfsFromSnapshotLocked()
    } finally {
        restoreMutex.unlock()
    }
}

private suspend fun restoreSusfsFromSnapshotLocked(): SusfsRecoveryResult = withContext(Dispatchers.IO) {
    if (Natives.isLateLoadMode || Natives.isLkmMode) return@withContext SusfsRecoveryResult(error = "gki_mode_required")
    captureMutex.lock()
    val snapshot = try {
        readSnapshot()
    } finally {
        captureMutex.unlock()
    }
    var current = getSusfsPathConfig()
    var toolRestored = false
    if (current.error == "root_unavailable") return@withContext SusfsRecoveryResult(error = "root_unavailable")
    if (snapshot == null) {
        if (!current.available) return@withContext SusfsRecoveryResult(error = "kernel_or_tool_unsupported")
        if (current.runtimeStatus.generation.isNotBlank()) {
            return@withContext SusfsRecoveryResult(success = true, error = "already_present")
        }
        // There is no way to reconstruct deleted rules. Recreate only the
        // replay framework, with hiding disabled if no old rules remain.
        val applied = saveAndApplySusfsConfig(configForSusfsReinitialization(current))
        if (!applied.saved) {
            return@withContext SusfsRecoveryResult(error = applied.error.ifBlank { "config_restore_failed" })
        }
        if (!applied.success) return@withContext SusfsRecoveryResult(error = susfsApplyErrorDetail(applied))
        captureSusfsRecoverySnapshot()
        return@withContext SusfsRecoveryResult(success = true, error = "initialized_without_backup")
    }
    if (!current.available) {
        val sourceTool = snapshot.tool ?: return@withContext SusfsRecoveryResult(error = "kernel_or_tool_unsupported")
        val source = quote(sourceTool.absolutePath)
        val hash = quote(snapshot.sha256)
        val version = quote(snapshot.version)
        val command = """
            set -eu
            for dir in /data/adb /data/adb/ksu /data/adb/ksu/bin; do
                [ ! -L "${'$'}dir" ] || exit 1
                mkdir -p "${'$'}dir"
            done
            [ ! -e $RESTORED_TOOL ] && [ ! -L $RESTORED_TOOL ]
            umask 077
            pending=$RESTORED_TOOL.pending.${'$'}${'$'}
            trap 'rm -f "${'$'}pending"' EXIT
            cp $source "${'$'}pending"
            chown 0:0 "${'$'}pending"
            chmod 0700 "${'$'}pending"
            [ "${'$'}(sha256sum "${'$'}pending" | cut -d ' ' -f 1)" = $hash ]
            [ "${'$'}("${'$'}pending" show version 2>/dev/null | sed -n '1p')" = $version ]
            mv "${'$'}pending" $RESTORED_TOOL
        """.trimIndent()
        if (!rootCommand(command).first) return@withContext SusfsRecoveryResult(error = "tool_restore_failed")
        toolRestored = true
        current = getSusfsPathConfig()
        if (!current.available) return@withContext SusfsRecoveryResult(error = "kernel_or_tool_unsupported")
    }
    val (checked, output) = rootCommand(
        "if [ -f /data/adb/ksu/susfs/current/settings.conf ] || " +
            "[ -f /data/adb/ksu/susfs/settings.conf ]; then echo present; else echo missing; fi; " +
            "if [ -f /data/adb/service.d/98-apkesu-susfs-paths.sh ]; then echo service; else echo no_service; fi",
    )
    if (!checked) return@withContext SusfsRecoveryResult(error = "config_probe_failed")
    val hasConfig = "present" in output
    val hasService = "service" in output
    if (hasConfig && hasService) {
        if (!toolRestored && current.runtimeStatus.generation.isNotBlank()) {
            return@withContext SusfsRecoveryResult(success = true, error = "already_present")
        }
        if (!rootCommand("/system/bin/sh /data/adb/service.d/98-apkesu-susfs-paths.sh --immediate").first) {
            return@withContext SusfsRecoveryResult(error = "config_replay_failed")
        }
        val status = getSusfsPathConfig().runtimeStatus
        val success = isSusfsApplyStateSuccessful(status.state) && status.failedCount == 0
        return@withContext SusfsRecoveryResult(
            success = success,
            error = if (success) "" else susfsApplyErrorDetail(
                SusfsPathApplyResult(issues = status.issues, error = "apply_${status.state}"),
            ),
        )
    }
    val restoredConfig = if (hasConfig) current else snapshot.config.copy(
        available = current.available,
        toolPath = current.toolPath,
        capabilities = current.capabilities,
    )
    val applied = saveAndApplySusfsConfig(restoredConfig)
    if (!applied.saved) return@withContext SusfsRecoveryResult(error = applied.error.ifBlank { "config_restore_failed" })
    captureSusfsRecoverySnapshot()
    SusfsRecoveryResult(success = applied.success, error = if (applied.success) "" else susfsApplyErrorDetail(applied))
}
