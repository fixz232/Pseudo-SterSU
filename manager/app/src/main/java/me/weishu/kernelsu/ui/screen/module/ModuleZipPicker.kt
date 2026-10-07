package me.weishu.kernelsu.ui.screen.module

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.dialog.rememberConfirmDialog
import me.weishu.kernelsu.ui.util.getFileName

/** The same ZIP selection and multi-module confirmation for the module FAB and sidebar. */
@Composable
internal fun rememberModuleZipPicker(onOpenFlash: (List<Uri>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val title = stringResource(R.string.module)
    val prompt = stringResource(R.string.module_install_prompt_with_name, "%s")
    val currentOnOpenFlash by rememberUpdatedState(onOpenFlash)
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val confirmDialog = rememberConfirmDialog(onConfirm = { currentOnOpenFlash(selectedUris) })
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data ?: return@rememberLauncherForActivityResult
        val uris = buildList {
            val clip = data.clipData
            if (clip != null) {
                for (index in 0 until clip.itemCount) {
                    clip.getItemAt(index)?.uri?.let(::add)
                }
            } else {
                data.data?.let(::add)
            }
        }
        when (uris.size) {
            0 -> Unit
            1 -> currentOnOpenFlash(uris)
            else -> {
                selectedUris = uris
                val names = uris.mapIndexed { index, uri ->
                    "\n${index + 1}. ${uri.getFileName(context)}"
                }.joinToString("")
                confirmDialog.showConfirm(title = title, content = prompt.format(names))
            }
        }
    }
    return {
        launcher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        })
    }
}
