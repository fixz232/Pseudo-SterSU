package me.weishu.kernelsu.ui.webui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.util.DownloadCompletionAction
import me.weishu.kernelsu.ui.util.DownloadManager
import me.weishu.kernelsu.ui.util.DownloadService
import me.weishu.kernelsu.ui.util.resolveDownloadMimeType
import java.io.ByteArrayInputStream
import java.io.File

class WebUIDownloadInterface(private val state: WebUIState) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val webView get() = state.webView

    @JavascriptInterface
    fun download(url: String, fileName: String?, mimeType: String?) {
        val currentWebView = webView ?: return
        val context = currentWebView.context
        val target = resolveDownloadTarget(fileName)
        val cookie = CookieManager.getInstance().getCookie(url)
        val userAgent = WebSettings.getDefaultUserAgent(context)
        DownloadManager.enqueue(
            context = context,
            url = url,
            fileName = target.name,
            targetPath = target.absolutePath,
            mimeType = mimeType,
            cookie = cookie,
            userAgent = userAgent,
            completionAction = DownloadCompletionAction.OPEN_FILE,
        )
    }

    @JavascriptInterface
    fun save(base64: String, fileName: String?) {
        val currentWebView = webView ?: return
        val target = resolveDownloadTarget(fileName)
        val context = currentWebView.context
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val downloadId = DownloadManager.registerLocalSave(
            fileName = target.name,
            targetPath = target.absolutePath,
            completionAction = DownloadCompletionAction.OPEN_FILE,
        )

        ensureNotificationChannel(notificationManager, context)
        notificationManager.notify(downloadId, buildProgressNotification(context, target.name, 0))

        scope.launch {
            runCatching {
                val decoded = Base64.decode(base64, Base64.DEFAULT)
                var lastProgress = -1
                ByteArrayInputStream(decoded).use { input ->
                    writeWebUIDownload(target, input) { written ->
                        val progress = if (decoded.isEmpty()) {
                            100
                        } else {
                            ((written * 100L) / decoded.size.toLong()).toInt().coerceIn(0, 100)
                        }
                        DownloadManager.updateProgress(downloadId, progress)
                        if (progress - lastProgress >= 2 || progress == 100) {
                            notificationManager.notify(downloadId, buildProgressNotification(context, target.name, progress))
                            lastProgress = progress
                        }
                    }
                }
            }.onSuccess {
                val uri = Uri.fromFile(target)
                DownloadManager.markCompleted(downloadId, uri)
                notificationManager.notify(
                    downloadId,
                    buildCompletionNotification(context, downloadId, target),
                )
                postToast(currentWebView.context.getString(R.string.download_complete_content, target.name))
            }.onFailure { throwable ->
                Log.e("WebUIDownload", "Failed to save ${target.absolutePath}", throwable)
                DownloadManager.markFailed(downloadId, throwable.message ?: "Unknown error")
                notificationManager.notify(downloadId, buildFailureNotification(context, target.name))
                postToast(currentWebView.context.getString(R.string.download_failed_content, target.name))
            }
        }
    }

    fun destroy() {
        scope.cancel()
    }

    private fun postToast(message: String) {
        webView?.let { currentWebView ->
            currentWebView.post {
                Toast.makeText(currentWebView.context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resolveDownloadTarget(fileName: String?): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return resolveWebUIDownloadFile(downloadsDir, fileName)
    }

    private fun ensureNotificationChannel(notificationManager: NotificationManager, context: Context) {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                DownloadService.CHANNEL_ID,
                context.getString(R.string.download_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
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
        target: File,
    ): Notification {
        val contentUri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            target,
        )
        val openIntent = Intent.createChooser(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, resolveDownloadMimeType(target.name, null))
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
            .setContentText(context.getString(R.string.download_complete_content, target.name))
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
