package me.weishu.kernelsu.ui.webui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.util.DownloadCompletionAction
import me.weishu.kernelsu.ui.util.DownloadManager
import me.weishu.kernelsu.ui.util.DownloadService
import me.weishu.kernelsu.ui.util.resolveDownloadMimeType
import java.io.ByteArrayInputStream
import java.io.InputStream

class WebUIDownloadInterface(private val state: WebUIState) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val webView get() = state.webView

    companion object {
        // Use a different notification ID range to avoid collision with DownloadService
        private const val WEBUI_NOTIFICATION_ID_BASE = 200000
    }

    // Retain the previous bridge entry point for existing module WebUIs.
    @JavascriptInterface
    fun download(url: String, fileName: String?, mimeType: String?) {
        val currentWebView = webView ?: return
        val uri = url.toUri()
        if (uri.scheme != "http" && uri.scheme != "https") return
        val context = currentWebView.context
        DownloadManager.enqueue(
            context = context,
            url = url,
            fileName = sanitizeWebUIDownloadFileName(fileName),
            mimeType = mimeType,
            cookie = CookieManager.getInstance().getCookie(url),
            userAgent = WebSettings.getDefaultUserAgent(context),
            completionAction = DownloadCompletionAction.OPEN_FILE,
        )
    }

    @JavascriptInterface
    fun openExternal(url: String) {
        val currentWebView = webView ?: return
        val uri = url.toUri()
        if (uri.scheme != "http" && uri.scheme != "https") return

        runCatching {
            currentWebView.context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }.onFailure { throwable ->
            Log.e("WebUIDownload", "Failed to open $url", throwable)
        }
    }

    @JavascriptInterface
    fun save(base64: String, fileName: String?) {
        saveDownload(sanitizeWebUIDownloadFileName(fileName), null) {
            ByteArrayInputStream(Base64.decode(base64, Base64.DEFAULT))
        }
    }

    private fun saveDownload(
        fileName: String,
        providedMimeType: String?,
        size: Long? = null,
        openStream: () -> InputStream,
    ) {
        val currentWebView = webView ?: return
        val context = currentWebView.context
        val displayName = sanitizeWebUIDownloadFileName(fileName)
        val mimeType = resolveDownloadMimeType(displayName, providedMimeType)
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val downloadId = DownloadManager.registerLocalSave(
            fileName = displayName,
            mimeType = mimeType,
        )

        ensureNotificationChannel(notificationManager, context)
        notificationManager.notify(WEBUI_NOTIFICATION_ID_BASE + downloadId, buildProgressNotification(context, displayName, 0))

        scope.launch {
            var pendingUri: Uri? = null
            val savedUri = try {
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val values = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(collection, values)
                    ?: error("Failed to create download")
                pendingUri = uri
                var lastProgress = -1
                openStream().use { input ->
                    val totalBytes = size ?: input.available().toLong()
                    val output = context.contentResolver.openOutputStream(uri)
                        ?: error("Failed to open download")
                    output.use { stream ->
                        writeWebUIDownload(input, stream) { written ->
                            val progress = if (totalBytes == 0L) {
                                100
                            } else {
                                ((written * 100L) / totalBytes).toInt().coerceIn(0, 100)
                            }
                            DownloadManager.updateProgress(downloadId, progress)
                            if (progress - lastProgress >= 2 || progress == 100) {
                                notificationManager.notify(
                                    WEBUI_NOTIFICATION_ID_BASE + downloadId,
                                    buildProgressNotification(context, displayName, progress)
                                )
                                lastProgress = progress
                            }
                        }
                    }
                }
                val published = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                if (context.contentResolver.update(uri, published, null, null) != 1) {
                    error("Failed to publish download")
                }
                uri
            } catch (throwable: Throwable) {
                pendingUri?.let { context.contentResolver.delete(it, null, null) }
                Log.e("WebUIDownload", "Failed to save $displayName", throwable)
                DownloadManager.markFailed(downloadId, throwable.message ?: "Unknown error")
                notificationManager.notify(WEBUI_NOTIFICATION_ID_BASE + downloadId, buildFailureNotification(context, displayName))
                postToast(currentWebView.context.getString(R.string.download_failed_content, displayName))
                null
            }

            if (savedUri != null) {
                DownloadManager.markCompleted(downloadId, savedUri)
                notificationManager.notify(
                    WEBUI_NOTIFICATION_ID_BASE + downloadId,
                    buildCompletionNotification(context, downloadId, displayName, savedUri, mimeType)
                )
                postToast(currentWebView.context.getString(R.string.download_complete_content, displayName))
            }
        }
    }

    @JavascriptInterface
    fun startChunkedDownload(fileName: String?, mimeType: String?): String {
        val currentWebView = webView ?: return ""
        val sanitizedFileName = sanitizeWebUIDownloadFileName(fileName)
        return runCatching {
            BlobDownloadHandler.registerBlobDownload(
                sanitizedFileName,
                mimeType,
                currentWebView.context.cacheDir,
            )
        }.getOrElse {
            Log.e("WebUIDownload", "Failed to start chunked download", it)
            ""
        }
    }

    @JavascriptInterface
    fun writeDownloadChunk(downloadId: String, base64Chunk: String): Boolean {
        return BlobDownloadHandler.writeChunk(downloadId, base64Chunk)
    }

    @JavascriptInterface
    fun completeChunkedDownload(downloadId: String): Boolean {
        if (webView == null || !BlobDownloadHandler.completeDownload(downloadId)) return false
        val download = BlobDownloadHandler.completedDownload(downloadId) ?: return false
        saveDownload(download.fileName, download.mimeType, download.size, download.openStream)
        return true
    }

    @JavascriptInterface
    fun cancelChunkedDownload(downloadId: String) {
        BlobDownloadHandler.cancelDownload(downloadId)
    }

    fun destroy() {
        scope.cancel()
        BlobDownloadHandler.cleanup()
    }

    private fun postToast(message: String) {
        webView?.let { currentWebView ->
            currentWebView.post {
                Toast.makeText(currentWebView.context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun ensureNotificationChannel(notificationManager: NotificationManager, context: Context) {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                DownloadService.CHANNEL_ID,
                context.getString(R.string.download_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            )
        )
    }

    private fun buildProgressNotification(
        context: Context,
        fileName: String,
        progress: Int,
    ) = NotificationCompat.Builder(context, DownloadService.CHANNEL_ID)
        .setContentTitle(context.getString(R.string.download_progress_title, fileName))
        .setContentText("$progress%")
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setProgress(100, progress, progress == 0)
        .setOngoing(true)
        .setSilent(true)
        .build()

    private fun buildCompletionNotification(
        context: Context,
        downloadId: Int,
        fileName: String,
        contentUri: Uri,
        mimeType: String,
    ): android.app.Notification {
        val openIntent = Intent.createChooser(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            context.getString(R.string.open),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            downloadId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, DownloadService.CHANNEL_ID)
            .setContentTitle(context.getString(R.string.download_complete_title))
            .setContentText(context.getString(R.string.download_complete_content, fileName))
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_view, context.getString(R.string.open), pendingIntent)
            .build()
    }

    private fun buildFailureNotification(
        context: Context,
        fileName: String,
    ) = NotificationCompat.Builder(context, DownloadService.CHANNEL_ID)
        .setContentTitle(context.getString(R.string.download_failed_title))
        .setContentText(context.getString(R.string.download_failed_content, fileName))
        .setSmallIcon(android.R.drawable.stat_notify_error)
        .setAutoCancel(true)
        .build()
}
