package me.weishu.kernelsu.ui.util

import android.content.ContentResolver
import android.net.Uri
import android.os.CancellationSignal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

/** Xiaomi's documented local weather provider. Type 1 refreshes stale local data. */
internal class SidebarXiaomiWeatherClient(
    private val resolver: ContentResolver,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun fetch(config: SidebarWeatherConfig): SidebarWeatherReading = withContext(Dispatchers.IO) {
        if (config.source != SidebarWeatherSource.Xiaomi || !config.canFetch()) {
            throw SidebarWeatherException(SidebarWeatherError.Permission)
        }
        val signal = CancellationSignal()
        val cancellation = coroutineContext[Job]?.invokeOnCompletion { signal.cancel() }
        try {
            val fields = resolver.query(URI, COLUMNS, null, null, null, signal)?.use { cursor ->
                if (!cursor.moveToFirst()) throw SidebarWeatherException(SidebarWeatherError.Provider)
                COLUMNS.associateWith { column ->
                    cursor.getColumnIndex(column).takeIf { it >= 0 }?.let(cursor::getString)
                }
            } ?: throw SidebarWeatherException(SidebarWeatherError.Provider)
            parseXiaomiWeather(fields, now())
        } catch (e: CancellationException) {
            throw e
        } catch (e: SidebarWeatherException) {
            throw e
        } catch (e: SecurityException) {
            throw SidebarWeatherException(SidebarWeatherError.Permission)
        } catch (_: Exception) {
            throw SidebarWeatherException(SidebarWeatherError.Provider)
        } finally {
            cancellation?.dispose()
        }
    }

    companion object {
        // Xiaomi documents /1 as the preferred local-first query; it may update stale data.
        private val URI = Uri.parse("content://weather/actualWeatherData/1")
        private val COLUMNS = arrayOf("temperature", "description", "city_name", "temperature_unit", "publish_time", "weather_type")
    }
}

internal fun parseXiaomiWeather(fields: Map<String, String?>, now: Long): SidebarWeatherReading {
    val raw = fields["temperature"]?.trim().orEmpty()
    val match = XIAOMI_TEMPERATURE.matchEntire(raw)
        ?: throw SidebarWeatherException(SidebarWeatherError.Temperature)
    val temperature = match.groupValues[1].toDoubleOrNull()
        ?: throw SidebarWeatherException(SidebarWeatherError.Temperature)
    if (!temperature.isFinite() || temperature !in -200.0..200.0) {
        throw SidebarWeatherException(SidebarWeatherError.Temperature)
    }
    val fahrenheit = when (fields["temperature_unit"]?.trim()) {
        "0" -> true
        "1" -> false
        else -> match.groupValues[2].let { it == "\u2109" || it == "\u00B0F" }
    }
    fun safeText(value: String?): String = value.orEmpty()
        .filterNot { it.isISOControl() || it in '\u202A'..'\u202E' || it in '\u2066'..'\u2069' }
        .trim().take(80)
    return SidebarWeatherReading(
        temperature = temperature,
        description = safeText(fields["description"]),
        location = safeText(fields["city_name"]),
        fetchedAt = fields["publish_time"]?.toLongOrNull()?.takeIf { it > 0 && it <= now + 300_000L } ?: now,
        fahrenheit = fahrenheit,
        weatherType = fields["weather_type"]?.trim()?.toIntOrNull(),
    )
}

private val XIAOMI_TEMPERATURE = Regex("([+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+))\\s*(\\u2103|\\u2109|\\u00B0C|\\u00B0F)?")
