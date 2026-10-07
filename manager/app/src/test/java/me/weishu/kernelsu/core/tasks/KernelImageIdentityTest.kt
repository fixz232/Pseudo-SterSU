package me.weishu.kernelsu.core.tasks

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.CancellationException
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorOutputStream
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import org.junit.Assert.*
import org.junit.Test

class KernelImageIdentityTest {
    private val release = "5.15.180-android13-8-gabc123"
    private val version = "#1 SMP PREEMPT Wed Apr 1 08:33:52 UTC 2026"
    private val banner = "Linux version $release (builder@host) (Android clang (build 123) 18.0.0, LLD 18) $version"
    private val bytes get() = (banner + "\n\u0000").toByteArray()

    private fun read(bytes: ByteArray) = KernelImageIdentity.readKernel(ByteArrayInputStream(bytes), bytes.size)
    private fun failure(code: String, action: () -> Unit) {
        val thrown = assertThrows(IOException::class.java) { action() }
        assertEquals(code, thrown.message)
    }

    @Test fun parsesNestedCompilerGroupsAndCompleteVersion() {
        val identity = KernelImageIdentity.parseBanner(banner)!!
        assertEquals(release, identity.release)
        assertEquals(version, identity.version)
        assertEquals("Wed Apr 1 08:33:52 UTC 2026", identity.buildTime)
    }

    @Test fun preservesNonstandardBuildTimeWithoutInventingDate() {
        val identity = KernelImageIdentity.parseBanner(banner.replace(version, "#2 SMP reproducible-build"))!!
        assertEquals("#2 SMP reproducible-build", identity.version)
        assertNull(identity.buildTime)
    }

    @Test fun acceptsDateWithoutTimezone() {
        assertEquals("Wed Apr 1 08:33:52 2026", KernelImageIdentity.parseBanner(banner.replace(" UTC", ""))!!.buildTime)
    }

    @Test fun rejectsPartialBannerAndControlCharacters() {
        assertNull(KernelImageIdentity.parseBanner("Linux version $release"))
        assertNull(KernelImageIdentity.parseBanner(banner.replace("(builder@host)", "(builder@host")))
        assertNull(KernelImageIdentity.parseBanner(banner + "\n"))
        assertNull(KernelImageIdentity.parseBanner(banner.replace(version, "#" + "x".repeat(64))))
    }

    @Test fun findsRawKernelWithBinaryPrefixAndChunkBoundary() {
        val data = ByteArray(32768 - 5) + bytes + ByteArray(79)
        assertEquals(release, read(data).release)
    }

    @Test fun acceptsDuplicateIdenticalBanner() {
        assertEquals(version, read(bytes + ByteArray(10) + bytes).version)
    }

    @Test fun rejectsConflictingBanners() {
        failure("ambiguous_kernel_banner") { read(bytes + String(bytes).replace(release, "6.1.100-other").toByteArray()) }
    }

    @Test fun stopsAtFirstConflictInsteadOfAccumulatingUntrustedVersions() {
        val data = bytes + String(bytes).replace(release, "6.1.100-other").toByteArray() + ByteArray(128 * 1024)
        var reads = 0
        failure("ambiguous_kernel_banner") {
            KernelImageIdentity.readKernel(ByteArrayInputStream(data), data.size) { reads++ }
        }
        assertEquals(1, reads)
    }

    @Test fun rejectsUnterminatedBanner() {
        failure("kernel_banner_unavailable") { read(banner.toByteArray()) }
    }

    @Test fun rejectsMissingBanner() {
        failure("kernel_banner_unavailable") { read(ByteArray(9000)) }
    }

    @Test fun readsGzip() {
        val encoded = ByteArrayOutputStream().also { output -> GZIPOutputStream(output).use { it.write(bytes) } }.toByteArray()
        assertEquals(version, read(encoded).version)
    }

    @Test fun readsXz() {
        val encoded = ByteArrayOutputStream().also { output -> XZCompressorOutputStream(output).use { it.write(bytes) } }.toByteArray()
        assertEquals(version, read(encoded).version)
    }

    @Test fun readsFramedLz4() {
        val encoded = ByteArrayOutputStream().also { output -> FramedLZ4CompressorOutputStream(output).use { it.write(bytes) } }.toByteArray()
        assertEquals(release, read(encoded).release)
    }

    private fun legacyLz4(data: ByteArray, trailer: Boolean = false): ByteArray {
        val block = ByteArrayOutputStream().also { output -> BlockLZ4CompressorOutputStream(output).use { it.write(data) } }.toByteArray()
        val header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(0x184c2102).putInt(block.size).array()
        val size = if (trailer) ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(data.size).array() else byteArrayOf()
        return header + block + size
    }

    @Test fun readsLegacyLz4() { assertEquals(release, read(legacyLz4(bytes)).release) }
    @Test fun readsLegacyLz4WithLinuxSizeTrailer() { assertEquals(version, read(legacyLz4(bytes, trailer = true)).version) }

    @Test fun readsLegacyLz4WithZeroTerminator() {
        assertEquals(version, read(legacyLz4(bytes) + ByteArray(4)).version)
    }

    @Test fun readsBannerAcrossLegacyLz4Blocks() {
        val first = legacyLz4(bytes.copyOfRange(0, 20))
        val second = legacyLz4(bytes.copyOfRange(20, bytes.size)).drop(4).toByteArray()
        assertEquals(version, read(first + second).version)
    }

    @Test fun rejectsLegacyLz4Truncation() {
        assertThrows(IOException::class.java) { read(legacyLz4(bytes).dropLast(5).toByteArray()) }
    }

    @Test fun rejectsGzipCorruptionEvenAfterBanner() {
        val encoded = ByteArrayOutputStream().also { output -> GZIPOutputStream(output).use { it.write(bytes) } }.toByteArray()
        encoded[encoded.lastIndex - 5] = (encoded[encoded.lastIndex - 5].toInt() xor 1).toByte()
        assertThrows(IOException::class.java) { read(encoded) }
    }

    @Test fun rejectsUnsupportedCompression() {
        failure("unsupported_kernel_compression") { read(byteArrayOf(0x28, 0xb5.toByte(), 0x2f, 0xfd.toByte(), 0, 0)) }
    }

    @Test fun boundsReadingToKernelNotRamdisk() {
        val raw = bytes + "Linux version 6.1.1-fake (a) (b) #1\n".toByteArray()
        assertEquals(release, KernelImageIdentity.readKernel(ByteArrayInputStream(raw), bytes.size).release)
    }

    @Test fun refusesShortInputEvenIfItContainsBanner() {
        failure("truncated_kernel") { KernelImageIdentity.readKernel(ByteArrayInputStream(bytes), bytes.size + 8) }
    }

    @Test fun checksCancellationWhileScanning() {
        assertThrows(CancellationException::class.java) {
            KernelImageIdentity.readKernel(ByteArrayInputStream(bytes), bytes.size) { throw CancellationException() }
        }
    }

    private fun header(version: Int = 4, size: Int = 12345, page: Int = 4096): ByteArray {
        val bytes = ByteArray(64)
        "ANDROID!".toByteArray().copyInto(bytes)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(8, size)
            putInt(36, page)
            putInt(40, version)
        }
        return bytes
    }

    @Test fun readsModernAndLegacyHeaders() {
        assertEquals(BootKernelRange(4096, 12345), KernelImageIdentity.kernelRange(header()))
        assertEquals(BootKernelRange(2048, 12345), KernelImageIdentity.kernelRange(header(version = 2, page = 2048)))
    }

    @Test fun rejectsInitBootUnknownHeadersAndOverflow() {
        failure("invalid_boot_header") { KernelImageIdentity.kernelRange(header(size = 0)) }
        failure("invalid_boot_header") { KernelImageIdentity.kernelRange(header(version = 99)) }
        failure("invalid_boot_header") { KernelImageIdentity.kernelRange(ByteArray(12)) }
        failure("kernel_size_limit") { KernelImageIdentity.kernelRange(header(size = -1)) }
        failure("kernel_size_limit") { KernelImageIdentity.kernelRange(header(size = 129 * 1024 * 1024)) }
        failure("kernel_size_limit") { KernelImageIdentity.kernelRange(header(version = 2, page = Int.MAX_VALUE)) }
    }
}
