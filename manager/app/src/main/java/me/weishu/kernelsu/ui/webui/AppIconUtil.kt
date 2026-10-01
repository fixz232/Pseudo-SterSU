package me.weishu.kernelsu.ui.webui

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import me.weishu.kernelsu.ui.util.AppIconCache
import me.weishu.kernelsu.ui.util.withMainUserUid
import me.weishu.kernelsu.ui.viewmodel.SuperUserViewModel

object AppIconUtil {
    // Limit cache size to 200 icons
    private const val CACHE_SIZE = 200
    private val iconCache = LruCache<String?, Bitmap?>(CACHE_SIZE)

    @Synchronized
    fun loadAppIconSync(context: Context, packageName: String, sizePx: Int): Bitmap? {
        val cached = iconCache.get(packageName)
        if (cached != null) return cached

        try {
            val appInfo = SuperUserViewModel.apps
                .find { it.packageName == packageName }
                ?.packageInfo
                ?.applicationInfo
                ?: return null
            val icon = AppIconCache.loadIconSync(context, appInfo.withMainUserUid(context), sizePx)
            iconCache.put(packageName, icon)
            return icon
        } catch (_: Exception) {
            return null
        }
    }
}
