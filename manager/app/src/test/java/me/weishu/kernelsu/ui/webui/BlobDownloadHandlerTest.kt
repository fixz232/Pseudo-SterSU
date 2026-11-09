package me.weishu.kernelsu.ui.webui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class BlobDownloadHandlerTest {
    private val id = "c980a9a3-8896-4c3d-b06a-d35d88f3c240"

    @Test
    fun bridgeUrlAndInternalIdResolveToTheSameSession() {
        assertEquals(id, blobDownloadSessionId(id))
        assertEquals(id, blobDownloadSessionId("https://blob-download.kernelsu.internal/$id"))
    }

    @Test
    fun unrelatedUrlsAndMalformedIdsAreRejected() {
        listOf("", "../$id", "1-1-1-1-1", "https://example.org/$id",
            "http://blob-download.kernelsu.internal/$id",
            "https://blob-download.kernelsu.internal/$id?other=1").forEach {
            assertNull(blobDownloadSessionId(it))
        }
    }

    @Test
    fun completedUrlCanBeConsumedAndClosingItRemovesTheTemporaryFile() {
        val dir = Files.createTempDirectory("webui-blob-test").toFile()
        try {
            val handle = BlobDownloadHandler.registerBlobDownload("example.zip", "application/zip", dir)
            assertNull(BlobDownloadHandler.completedDownload(handle))
            assertTrue(BlobDownloadHandler.completeDownload(handle))
            assertFalse(BlobDownloadHandler.completeDownload(handle))
            val completed = requireNotNull(BlobDownloadHandler.completedDownload(handle))
            assertEquals("example.zip", completed.fileName)
            assertEquals("application/zip", completed.mimeType)
            assertEquals(0L, completed.size)
            completed.openStream().use { assertEquals(-1, it.read()) }
            assertNull(BlobDownloadHandler.completedDownload(handle))
            assertTrue(dir.listFiles().orEmpty().isEmpty())
        } finally {
            BlobDownloadHandler.cleanup()
            dir.deleteRecursively()
        }
    }

    @Test
    fun cancellingUrlRemovesSessionAndTemporaryFile() {
        val dir = Files.createTempDirectory("webui-blob-test").toFile()
        try {
            val handle = BlobDownloadHandler.registerBlobDownload("example.zip", null, dir)
            BlobDownloadHandler.cancelDownload(handle)
            assertFalse(BlobDownloadHandler.completeDownload(handle))
            assertTrue(dir.listFiles().orEmpty().isEmpty())
        } finally {
            BlobDownloadHandler.cleanup()
            dir.deleteRecursively()
        }
    }
}
