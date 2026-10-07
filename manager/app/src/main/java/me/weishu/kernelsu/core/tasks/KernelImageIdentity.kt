package me.weishu.kernelsu.core.tasks

import java.io.BufferedInputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorInputStream
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream

internal data class KernelIdentity(
    val release: String,
    val version: String,
    val buildTime: String?,
)

internal data class BootKernelRange(val offset: Int, val size: Int)

/** Reads the image, never the (possibly spoofed) running kernel's uname. */
internal object KernelImageIdentity {
    const val MAX_KERNEL_BYTES = 128 * 1024 * 1024
    private const val MAX_EXPANDED_BYTES = 256 * 1024 * 1024
    private const val MAX_BANNER_BYTES = 4096
    private const val PREFIX = "Linux version "
    private val buildTimePattern = Regex(
        "(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun)\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)" +
            "\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2}\\s+(?:\\S+\\s+)?\\d{4}$",
    )

    fun kernelRange(header: ByteArray): BootKernelRange {
        val (offset, size) = BootKernelVersion.bootKernelBlock(header)
            ?: throw IOException("invalid_boot_header")
        if (offset !in 512..65536 || offset % 512 != 0 || size !in 1..MAX_KERNEL_BYTES) {
            throw IOException("kernel_size_limit")
        }
        return BootKernelRange(offset, size)
    }

    fun parseBanner(banner: String): KernelIdentity? {
        if (!banner.startsWith(PREFIX) || banner.length > MAX_BANNER_BYTES) return null
        if (banner.any { it.code !in 32..126 }) return null
        val releaseEnd = banner.indexOf(' ', PREFIX.length)
        if (releaseEnd < 0) return null
        val release = banner.substring(PREFIX.length, releaseEnd)
        if (!release.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+[A-Za-z0-9._+\\-]*")) || release.length > 64) return null

        // Builder and compiler are two balanced groups; compiler strings often
        // contain their own parentheses. Only the suffix is UTS_VERSION.
        var position = releaseEnd + 1
        repeat(2) {
            if (banner.getOrNull(position) != '(') return null
            var depth = 0
            do {
                when (banner.getOrNull(position++) ?: return null) {
                    '(' -> depth++
                    ')' -> depth--
                }
            } while (depth > 0)
            while (banner.getOrNull(position) == ' ') position++
        }
        val version = banner.substring(position).trim()
        if (!version.startsWith('#') || version.length !in 2..64) return null
        return KernelIdentity(release, version, buildTimePattern.find(version)?.value)
    }

    fun readKernel(input: InputStream, size: Int, checkActive: () -> Unit = {}): KernelIdentity {
        require(size in 1..MAX_KERNEL_BYTES)
        val bounded = KernelLimitedInputStream(input, size.toLong())
        val source = BufferedInputStream(bounded)
        source.mark(8)
        val magic = ByteArray(6)
        val count = source.read(magic)
        source.reset()
        fun starts(vararg bytes: Int) = count >= bytes.size && bytes.indices.all {
            magic[it].toInt() and 255 == bytes[it]
        }
        val decoded = when {
            starts(0x1f, 0x8b) -> GZIPInputStream(source)
            starts(0xfd, 0x37, 0x7a, 0x58, 0x5a, 0) -> XZCompressorInputStream.builder()
                .setInputStream(source)
                .setMemoryLimitKiB(65536)
                .get()
            starts(0x04, 0x22, 0x4d, 0x18) -> FramedLZ4CompressorInputStream(source)
            starts(0x02, 0x21, 0x4c, 0x18) -> LegacyKernelLz4InputStream(source)
            starts(0x28, 0xb5, 0x2f, 0xfd) || starts(0x42, 0x5a, 0x68) || starts(0x5d, 0, 0) ->
                throw IOException("unsupported_kernel_compression")
            else -> source
        }
        val identities = linkedSetOf<KernelIdentity>()
        val buffer = ByteArray(32768)
        var total = 0L
        var prefixLength = 0
        var banner: StringBuilder? = null
        decoded.use { stream ->
            while (true) {
                checkActive()
                val read = stream.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_EXPANDED_BYTES) throw IOException("kernel_size_limit")
                for (index in 0 until read) {
                    val ch = (buffer[index].toInt() and 255).toChar()
                    val candidate = banner
                    if (candidate != null) {
                        if (ch == '\u0000' || ch == '\n') {
                            parseBanner(candidate.toString())?.let(identities::add)
                            if (identities.size > 1) throw IOException("ambiguous_kernel_banner")
                            banner = null
                        } else if (ch.code !in 32..126 || candidate.length >= MAX_BANNER_BYTES) {
                            banner = null
                        } else {
                            candidate.append(ch)
                        }
                    } else if (ch == PREFIX[prefixLength]) {
                        prefixLength++
                        if (prefixLength == PREFIX.length) {
                            banner = StringBuilder(PREFIX)
                            prefixLength = 0
                        }
                    } else {
                        prefixLength = if (ch == PREFIX[0]) 1 else 0
                    }
                }
            }
        }
        // Never use a truncated banner or silently choose between conflicting versions.
        return identities.singleOrNull() ?: throw IOException("kernel_banner_unavailable")
    }
}

/** Does not close its parent, so legacy LZ4 blocks can share one stream. */
private class KernelLimitedInputStream(private val input: InputStream, private var remaining: Long) : InputStream() {
    override fun read(): Int {
        if (remaining == 0L) return -1
        val result = input.read()
        if (result < 0) throw EOFException("truncated_kernel")
        remaining--
        return result
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (remaining == 0L) return -1
        val read = input.read(buffer, offset, minOf(length.toLong(), remaining).toInt())
        if (read < 0) throw EOFException("truncated_kernel")
        remaining -= read
        return read
    }
}

/** Android kernels also use the legacy LZ4 format (8 MiB independent blocks). */
private class LegacyKernelLz4InputStream(private val source: InputStream) : InputStream() {
    private var block: InputStream? = null
    private var blockBytes = 0
    private var ended = false
    private var expandedBytes = 0L

    init {
        repeat(4) { if (source.read() < 0) throw EOFException("truncated_kernel") }
    }

    override fun read(): Int {
        val byte = ByteArray(1)
        return if (read(byte, 0, 1) < 0) -1 else byte[0].toInt() and 255
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        while (!ended) {
            if (block == null) {
                val first = source.read()
                if (first < 0) { ended = true; return -1 }
                var size = first.toLong()
                for (index in 1..3) {
                    val byte = source.read()
                    if (byte < 0) throw EOFException("truncated_kernel")
                    size = size or (byte.toLong() shl (index * 8))
                }
                if (size == 0L) { ended = true; return -1 }
                // Linux's lz4 build rule may append the uncompressed size.
                if (size == expandedBytes && expandedBytes > 0) {
                    source.mark(1)
                    if (source.read() < 0) { ended = true; return -1 }
                    source.reset()
                }
                if (size > 8 * 1024 * 1024 + 65536) throw IOException("invalid_lz4_block")
                block = BlockLZ4CompressorInputStream(KernelLimitedInputStream(source, size))
                blockBytes = 0
            }
            val read = requireNotNull(block).read(buffer, offset, length)
            if (read >= 0) {
                blockBytes += read
                expandedBytes += read
                if (blockBytes > 8 * 1024 * 1024) throw IOException("invalid_lz4_block")
                return read
            }
            block?.close()
            block = null
        }
        return -1
    }

    override fun close() { block?.close() }
}
