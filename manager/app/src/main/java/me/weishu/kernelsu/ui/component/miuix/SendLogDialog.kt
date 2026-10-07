package me.weishu.kernelsu.ui.component.miuix

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.component.dialog.LoadingDialogHandle
import me.weishu.kernelsu.ui.util.getBugreportFile
import me.weishu.kernelsu.ui.util.rememberSidebarWidgetConfig
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun SendLogDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    loadingDialog: LoadingDialogHandle,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logSavedText = stringResource(R.string.log_saved)
    val sendLogText = stringResource(R.string.send_log)
    val exportBugreportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/gzip")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            loadingDialog.show()
            context.contentResolver.openOutputStream(uri)?.use { output ->
                getBugreportFile(context).inputStream().use {
                    it.copyTo(output)
                }
            }
            loadingDialog.hide()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, logSavedText, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val saveLog: () -> Unit = {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm")
        val current = LocalDateTime.now().format(formatter)
        exportBugreportLauncher.launch("KernelSU_bugreport_${current}.tar.gz")
        onDismissRequest()
    }
    val shareLog: () -> Unit = {
        scope.launch {
            onDismissRequest()
            val bugreport = loadingDialog.withLoading {
                withContext(Dispatchers.IO) { getBugreportFile(context) }
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                bugreport
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, uri)
                setDataAndType(uri, "application/gzip")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, sendLogText))
        }
    }
    if (LocalInterfaceStyle.current == InterfaceStyle.SidebarWidget.value) {
        SidebarSendLogDialog(show, onDismissRequest, saveLog, shareLog)
        return
    }
    OverlayDialog(
        show = show,
        onDismissRequest = onDismissRequest,
        insideMargin = DpSize(0.dp, 0.dp),
        content = {
            MiuixText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 12.dp),
                text = stringResource(R.string.send_log),
                fontSize = MiuixTheme.textStyles.title4.fontSize,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = colorScheme.onSurface
            )
            ArrowPreference(
                title = stringResource(id = R.string.save_log),
                startAction = {
                    MiuixIcon(
                        Icons.Rounded.Save,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 16.dp),
                        tint = colorScheme.onSurface
                    )
                },
                onClick = saveLog,
                insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            )
            ArrowPreference(
                title = stringResource(id = R.string.send_log),
                startAction = {
                    MiuixIcon(
                        Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 16.dp),
                        tint = colorScheme.onSurface
                    )
                },
                onClick = shareLog,
                insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            )
            MiuixTextButton(
                text = stringResource(id = android.R.string.cancel),
                onClick = {
                    onDismissRequest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 24.dp)
                    .padding(horizontal = 24.dp)
            )
        }
    )
}

@Composable
private fun SidebarSendLogDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    if (!show) return

    val sidebar = rememberSidebarWidgetConfig()
    val railWidth = (if (LocalConfiguration.current.smallestScreenWidthDp >= 600) 96.dp else 80.dp) *
        LocalDensity.current.fontScale.coerceIn(1f, 1.2f)
    val railAtStart = sidebar.side.isAtStart(LocalLayoutDirection.current)
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize().pointerInput(onDismissRequest) {
                    detectTapGestures(onTap = { onDismissRequest() })
                }
            )
            Box(
                modifier = Modifier.fillMaxSize().padding(
                    start = if (railAtStart) railWidth else 0.dp,
                    end = if (railAtStart) 0.dp else railWidth,
                ).padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth().pointerInput(Unit) {
                        detectTapGestures(onTap = {})
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 2.dp,
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.Text(
                            text = stringResource(R.string.send_log),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                        SidebarLogAction(Icons.Rounded.Save, stringResource(R.string.save_log), onSave)
                        SidebarLogAction(Icons.Rounded.Share, stringResource(R.string.send_log), onShare)
                        androidx.compose.material3.TextButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 52.dp),
                        ) {
                            androidx.compose.material3.Text(stringResource(android.R.string.cancel))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarLogAction(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        androidx.compose.material3.Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        androidx.compose.material3.Icon(
            Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
