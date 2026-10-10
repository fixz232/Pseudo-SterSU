package me.weishu.kernelsu.ui.util

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject

private val FORECAST_URL = "https://api.open-meteo.com/v1/forecast".toHttpUrl()

internal fun openMeteoUrl(config: SidebarWeatherConfig): HttpUrl {
    val latitude = config.latitude.trim().takeIf { it.length <= 32 }?.toDoubleOrNull()
    val longitude = config.longitude.trim().takeIf { it.length <= 32 }?.toDoubleOrNull()
    if (latitude == null || !latitude.isFinite() || latitude !in -90.0..90.0 ||
        longitude == null || !longitude.isFinite() || longitude !in -180.0..180.0
    ) throw SidebarWeatherException(SidebarWeatherError.Coordinates)
    return FORECAST_URL.newBuilder()
        .addQueryParameter("latitude", latitude.toString())
        .addQueryParameter("longitude", longitude.toString())
        .addQueryParameter("current", "temperature_2m,weather_code,is_day")
        .addQueryParameter("temperature_unit", if (config.fahrenheit) "fahrenheit" else "celsius")
        .addQueryParameter("timezone", "auto")
        .build()
}

internal fun parseOpenMeteoWeather(json: String, config: SidebarWeatherConfig, now: Long): SidebarWeatherReading {
    val temperature = parseSidebarWeather(
        json,
        config.copy(temperaturePath = "current.temperature_2m", descriptionPath = "", locationPath = ""),
        now,
    )
    val root = JSONObject(json)
    val current = root.optJSONObject("current") ?: throw SidebarWeatherException(SidebarWeatherError.Json)
    val rawCode = (current.opt("weather_code") as? Number)?.toDouble()
    val rawDay = (current.opt("is_day") as? Number)?.toDouble()
    val expectedUnit = if (config.fahrenheit) "°F" else "°C"
    if (rawCode == null || !rawCode.isFinite() || rawCode != rawCode.toInt().toDouble() ||
        (rawDay != 0.0 && rawDay != 1.0) || root.optJSONObject("current_units")?.optString("temperature_2m") != expectedUnit
    ) throw SidebarWeatherException(SidebarWeatherError.Json)
    val code = rawCode.toInt()
    return temperature.copy(
        description = openMeteoDescription(code),
        fahrenheit = config.fahrenheit,
        openMeteoCode = code,
        isDay = rawDay == 1.0,
    )
}

/** WMO interpretation codes documented by Open-Meteo. */
internal fun openMeteoWeatherCondition(code: Int, isDay: Boolean?): SidebarWeatherCondition = when (code) {
    0, 1 -> if (isDay == false) SidebarWeatherCondition.Night else SidebarWeatherCondition.Clear
    2, 3 -> SidebarWeatherCondition.Cloud
    45, 48 -> SidebarWeatherCondition.Fog
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> SidebarWeatherCondition.Rain
    71, 73, 75, 77, 85, 86 -> SidebarWeatherCondition.Snow
    95, 96, 99 -> SidebarWeatherCondition.Storm
    else -> SidebarWeatherCondition.Unknown
}

private fun openMeteoDescription(code: Int): String = when (code) {
    0 -> "Clear"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Fog"
    51, 53, 55, 56, 57 -> "Drizzle"
    61, 63, 65, 66, 67 -> "Rain"
    71, 73, 75, 77 -> "Snow"
    80, 81, 82 -> "Rain showers"
    85, 86 -> "Snow showers"
    95, 96, 99 -> "Thunderstorm"
    else -> "Weather unavailable"
}
