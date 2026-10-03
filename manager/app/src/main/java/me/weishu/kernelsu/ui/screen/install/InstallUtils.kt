package me.weishu.kernelsu.ui.screen.install

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import android.provider.OpenableColumns
import androidx.annotation.StringRes
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.util.BootPatchMode

internal fun BootPatchMode.supportsInstallMethod(method: InstallMethod?): Boolean =
    this != BootPatchMode.NativeKpm || method is InstallMethod.SelectFile ||
        method is InstallMethod.DownloadFile && method.partition == "boot"

internal fun resolveDirectInstallPartition(
    method: InstallMethod,
    hasCustomSelected: Boolean,
    selectedPartitionName: String?,
    availablePartitions: List<String>,
): String? {
    if (method !is InstallMethod.DirectInstall &&
        method !is InstallMethod.DirectInstallToInactiveSlot
    ) {
        return null
    }
    // The displayed default can be transient while partition probes finish.
    // Let ksud choose from the current KMI and slot unless the user overrides it.
    if (!hasCustomSelected) return null
    return selectedPartitionName?.takeIf(availablePartitions::contains)
}

@Parcelize
internal sealed class InstallMethod : Parcelable {
    data class SelectFile(
        val uri: Uri? = null,
        @get:StringRes override val label: Int = R.string.select_file,
        override val summary: String?
    ) : InstallMethod()

    data class DownloadFile(
        val url: String? = null,
        val partition: String? = null,
        @get:StringRes override val label: Int = R.string.download_file,
        override val summary: String?
    ) : InstallMethod()

    data object DirectInstall : InstallMethod() {
        override val label: Int
            get() = R.string.direct_install
    }

    data object DirectInstallToInactiveSlot : InstallMethod() {
        override val label: Int
            get() = R.string.install_inactive_slot
    }

    data class AnyKernel(
        val uri: Uri? = null,
        override val label: Int = R.string.anykernel_install,
        override val summary: String? = null
    ) : InstallMethod()

    abstract val label: Int

    @IgnoredOnParcel
    open val summary: String? = null
}

fun isKoFile(context: Context, uri: Uri): Boolean {
    val seg = uri.lastPathSegment ?: ""
    if (seg.endsWith(".ko", ignoreCase = true)) return true

    return try {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx != -1 && cursor.moveToFirst()) {
                val name = cursor.getString(idx)
                name?.endsWith(".ko", ignoreCase = true) == true
            } else {
                false
            }
        } ?: false
    } catch (_: Throwable) {
        false
    }
}
