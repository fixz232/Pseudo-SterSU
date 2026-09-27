package me.weishu.kernelsu.ui.webmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.ui.util.ManagerPlugin
import me.weishu.kernelsu.ui.util.ManagerPluginRegistry
import me.weishu.kernelsu.ui.util.checkManagerPluginCompatibility
import me.weishu.kernelsu.ui.util.getInstalledKsudStatus

class WebManagerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        try {
            Thread({
                try {
                    startIfCompatible(appContext)
                } catch (error: Throwable) {
                    Log.e(TAG, "failed to check web manager boot state", error)
                } finally {
                    pendingResult.finish()
                }
            }, "SterSU-WebManager-Boot").apply {
                isDaemon = true
                start()
            }
        } catch (error: Throwable) {
            pendingResult.finish()
            Log.e(TAG, "failed to schedule web manager boot check", error)
        }
    }

    private fun startIfCompatible(context: Context) {
        if (!WebManagerPreferences.isAutoStartEnabled(context)) return

        val installed = ManagerPluginRegistry(context)
            .list()
            .firstOrNull { it.plugin.id == ManagerPlugin.RemoteManagementSuite.id }
        if (installed == null) {
            Log.i(TAG, "web manager plugin is not installed; skipping boot start")
            return
        }

        val ksudStatus = runCatching { getInstalledKsudStatus() }
            .getOrElse { error ->
                Log.w(TAG, "unable to read ksud status; skipping web manager boot start", error)
                return
            }
        val compatibility = checkManagerPluginCompatibility(
            plugin = installed.plugin,
            managerVersionCode = BuildConfig.VERSION_CODE,
            ksudStatus = ksudStatus,
        )
        if (!compatibility.isCompatible) {
            Log.w(TAG, "web manager plugin is incompatible; skipping boot start: $compatibility")
            return
        }

        runCatching { WebManagerService.start(context) }
            .onFailure { Log.e(TAG, "failed to start web manager after boot", it) }
    }

    private companion object {
        const val TAG = "SterSU-WebManager"
    }
}
