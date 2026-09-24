package me.weishu.kernelsu.ui.webmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import me.weishu.kernelsu.ui.util.ManagerPlugin
import me.weishu.kernelsu.ui.util.ManagerPluginRegistry

class WebManagerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!ManagerPluginRegistry(context).contains(ManagerPlugin.RemoteManagementSuite.id)) {
            Log.i(TAG, "web manager plugin is not installed; skipping boot start")
            return
        }
        if (!WebManagerPreferences.isAutoStartEnabled(context)) return
        runCatching { WebManagerService.start(context) }
            .onFailure { Log.e(TAG, "failed to start web manager after boot", it) }
    }

    private companion object {
        const val TAG = "ApkeSU-WebManager"
    }
}
