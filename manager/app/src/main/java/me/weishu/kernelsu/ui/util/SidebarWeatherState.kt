package me.weishu.kernelsu.ui.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.json.JSONObject

// Kept out of the general settings/theme exports. Android backup is disabled for the app.
internal const val SIDEBAR_WEATHER_CONFIG_KEY = "api_config"
private const val SIDEBAR_WEATHER_CACHE_KEY = "last_reading"

internal fun sidebarWeatherPreferences(context: Context): SharedPreferences =
    context.applicationContext.getSharedPreferences("sidebar_weather_private", Context.MODE_PRIVATE)

internal fun readSidebarWeatherConfig(context: Context): SidebarWeatherConfig =
    SidebarWeatherConfig.fromJson(sidebarWeatherPreferences(context).getString(SIDEBAR_WEATHER_CONFIG_KEY, null))

internal fun writeSidebarWeatherConfig(context: Context, config: SidebarWeatherConfig) {
    val preferences = sidebarWeatherPreferences(context)
    val serialized = config.normalized().toJson().toString()
    if (preferences.getString(SIDEBAR_WEATHER_CONFIG_KEY, null) != serialized) {
        preferences.edit { putString(SIDEBAR_WEATHER_CONFIG_KEY, serialized) }
    }
}

internal object SidebarWeatherRuntime {
    private var instance: SidebarWeatherRepository? = null

    @Synchronized
    fun repository(context: Context): SidebarWeatherRepository = instance ?: run {
        val preferences = sidebarWeatherPreferences(context)
        SidebarWeatherRepository(
            fetch = SidebarWeatherClient()::fetch,
            loadCache = { key ->
                runCatching {
                    val json = JSONObject(preferences.getString(SIDEBAR_WEATHER_CACHE_KEY, null).orEmpty())
                    if (json.optString("key") != key) null else {
                        SidebarWeatherReading(
                            temperature = json.getDouble("temperature"),
                            description = json.getString("description").take(80),
                            location = json.getString("location").take(80),
                            fetchedAt = json.getLong("fetchedAt"),
                        ).takeIf { it.temperature.isFinite() && it.temperature in -200.0..200.0 && it.fetchedAt > 0 }
                    }
                }.getOrNull()
            },
            saveCache = { key, reading ->
                val json = JSONObject().apply {
                    put("key", key)
                    put("temperature", reading.temperature)
                    put("description", reading.description)
                    put("location", reading.location)
                    put("fetchedAt", reading.fetchedAt)
                }
                preferences.edit { putString(SIDEBAR_WEATHER_CACHE_KEY, json.toString()) }
            },
        ).also { instance = it }
    }
}

@Composable
internal fun rememberSidebarWeatherState(config: SidebarWeatherConfig): SidebarWeatherState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val inspection = LocalInspectionMode.current
    val repository = remember(context.applicationContext) { SidebarWeatherRuntime.repository(context) }
    val state by repository.state.collectAsStateWithLifecycle()
    val key = remember(config) { config.cacheKey() }
    LaunchedEffect(config, lifecycleOwner, inspection) {
        if (config.enabled && !inspection) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    repository.refresh(config)
                    delay(60_000L)
                }
            }
        }
    }
    return if (config.enabled && state.key == key) state else SidebarWeatherState(key = key)
}
