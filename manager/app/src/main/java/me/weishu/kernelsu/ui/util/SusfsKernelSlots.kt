package me.weishu.kernelsu.ui.util

import com.topjohnwu.superuser.Shell
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import me.weishu.kernelsu.core.tasks.KernelIdentity
import me.weishu.kernelsu.core.tasks.KernelImageIdentity

internal enum class KernelBootSlot(val suffix: String) { A("a"), B("b"), Single("") }

internal data class KernelSlotInfo(
    val slot: KernelBootSlot,
    val partition: String = "",
    val identity: KernelIdentity? = null,
    val error: String = "",
)

internal data class KernelSlotsResult(
    val currentSlot: KernelBootSlot? = null,
    val slots: List<KernelSlotInfo> = emptyList(),
    val error: String = "",
)

internal fun parseKernelSlotProbe(lines: List<String>): KernelSlotsResult {
    val fields = lines.mapNotNull { line ->
        val separator = line.indexOf('=')
        if (separator < 1) null else line.substring(0, separator) to line.substring(separator + 1)
    }.toMap()
    val current = when (fields["current"]?.removePrefix("_")) {
        "a" -> KernelBootSlot.A
        "b" -> KernelBootSlot.B
        else -> null
    }
    fun partition(slot: KernelBootSlot): String = fields["boot${slot.suffix}"]
        ?.takeIf { it.startsWith("/dev/block/") && it.matches(Regex("/[A-Za-z0-9_./:+-]+")) && "/../" !in it }
        .orEmpty()
    val ab = current != null || fields["ab"] == "true" ||
        partition(KernelBootSlot.A).isNotEmpty() || partition(KernelBootSlot.B).isNotEmpty()
    val slots = if (ab) listOf(KernelBootSlot.A, KernelBootSlot.B) else listOf(KernelBootSlot.Single)
    val results = slots.map { slot ->
        val path = partition(slot)
        KernelSlotInfo(slot, path, error = if (path.isEmpty()) "slot_missing" else "")
    }
    if (ab && results[0].partition.isNotEmpty() && results[0].partition == results[1].partition) {
        return KernelSlotsResult(current, results.map { it.copy(error = "slot_alias_conflict") })
    }
    return KernelSlotsResult(current, results)
}

/** Only named boot partitions are queried; init_boot contains no GKI kernel. */
internal fun buildKernelSlotProbe(): String = buildString {
    appendLine("printf 'current=%s\\n' \"\$(getprop ro.boot.slot_suffix)\"")
    appendLine("case \"\$(getprop ro.boot.slot_suffix)\" in _a|_b|a|b) ;; *) printf 'current=%s\\n' \"\$(getprop ro.boot.slot)\" ;; esac")
    appendLine("printf 'ab=%s\\n' \"\$(getprop ro.build.ab_update)\"")
    for (slot in KernelBootSlot.entries) {
        val name = if (slot == KernelBootSlot.Single) "boot" else "boot_${slot.suffix}"
        appendLine("for path in /dev/block/by-name/$name /dev/block/bootdevice/by-name/$name /dev/block/$name \\")
        appendLine("  /dev/block/platform/*/by-name/$name /dev/block/platform/*/*/by-name/$name \\")
        appendLine("  /dev/block/platform/*/*/*/by-name/$name /dev/block/platform/*/*/*/*/by-name/$name; do")
        appendLine("  [ -b \"\$path\" ] || continue")
        appendLine("  resolved=\$(readlink -f \"\$path\") || continue")
        appendLine("  printf 'boot${slot.suffix}=%s\\n' \"\$resolved\"")
        appendLine("  break")
        appendLine("done")
    }
    appendLine("true")
}

internal fun buildKernelReadCommand(partition: String, destination: String, blockSize: Int, skip: Int, count: Int): String {
    require(partition.startsWith("/dev/block/") && partition.matches(Regex("/[A-Za-z0-9_./:+-]+")) && "/../" !in partition)
    require(blockSize in 1..65536 && skip in 0..1 && count > 0)
    require(blockSize.toLong() * count <= KernelImageIdentity.MAX_KERNEL_BYTES.toLong() + 65536)
    // Destination is a pre-created private cache file, never a block device.
    require(destination.startsWith("/") && !destination.startsWith("/dev/") && !destination.contains('\n'))
    return "/system/bin/toybox timeout -s KILL 25 /system/bin/toybox dd " +
        "if=${shellQuote(partition)} of=${shellQuote(destination)} bs=$blockSize skip=$skip count=$count"
}

internal suspend fun readSusfsKernelSlots(cacheDir: File): KernelSlotsResult = withContext(Dispatchers.IO) {
    try {
        createRootShell().use { shell ->
            if (!shell.isRoot) return@withContext KernelSlotsResult(error = "root_unavailable")
            val probe = shell.queryKernelInfo(buildKernelSlotProbe())
            if (!probe.isSuccess) throw IOException("slot_probe_failed")
            val result = parseKernelSlotProbe(probe.out)
            result.copy(slots = result.slots.map { slot ->
                currentCoroutineContext().ensureActive()
                if (slot.error.isNotEmpty()) slot else {
                    try {
                        slot.copy(identity = readSlotIdentity(shell, cacheDir, slot.partition))
                    } catch (_: TimeoutCancellationException) {
                        slot.copy(error = "read_timeout")
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        slot.copy(error = error.message.orEmpty().ifBlank { "image_read_failed" }.take(240))
                    }
                }
            })
        }
    } catch (_: TimeoutCancellationException) {
        KernelSlotsResult(error = "read_timeout")
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        KernelSlotsResult(error = error.message.orEmpty().ifBlank { "image_read_failed" }.take(240))
    }
}

private suspend fun readSlotIdentity(shell: Shell, cacheDir: File, partition: String): KernelIdentity {
    val file = File.createTempFile("susfs-slot-", ".bin", cacheDir)
    try {
        suspend fun copyRange(blockSize: Int, skip: Int, count: Int) {
            val result = shell.queryKernelInfo(buildKernelReadCommand(partition, file.absolutePath, blockSize, skip, count))
            if (!result.isSuccess) {
                throw IOException(if (result.code in listOf(124, 137, 143)) "read_timeout" else "image_read_failed: ${result.err.joinToString(" ").take(180)}")
            }
        }
        copyRange(64, 0, 1)
        val range = KernelImageIdentity.kernelRange(file.readBytes())
        if (cacheDir.usableSpace < range.size.toLong() + 8 * 1024 * 1024) throw IOException("cache_space_low")
        copyRange(range.offset, 1, (range.size + range.offset - 1) / range.offset)
        if (file.length() < range.size) throw IOException("truncated_kernel")
        return withTimeout(30_000) {
            val context = currentCoroutineContext()
            file.inputStream().use { KernelImageIdentity.readKernel(it, range.size) { context.ensureActive() } }
        }
    } finally {
        file.delete()
    }
}

/** A private shell keeps cancellation from interrupting unrelated manager jobs. */
private suspend fun Shell.queryKernelInfo(command: String): Shell.Result = withTimeout(30_000) {
    suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { runCatching { close() } }
        newJob().add(command).to(ArrayList(), ArrayList()).submit(null) { result ->
            if (continuation.isActive) continuation.resume(result)
        }
    }
}

internal fun SusfsPathConfigState.withKernelIdentity(identity: KernelIdentity): SusfsPathConfigState =
    copy(unameRelease = identity.release, unameVersion = identity.version)

private fun shellQuote(value: String): String = "'${value.replace("'", "'\"'\"'")}'"
