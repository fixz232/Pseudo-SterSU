package me.weishu.kernelsu.ui.util

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.MessageDigest
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Only explicitly saved settings are used for requests; editor drafts never poll. */
data class SidebarWeatherConfig(
    val enabled: Boolean = false,
    val source: SidebarWeatherSource = SidebarWeatherSource.CustomApi,
    val xiaomiAccepted: Boolean = false,
    val openMeteoAccepted: Boolean = false,
    val latitude: String = "",
    val longitude: String = "",
    val url: String = "",
    val headerName: String = "",
    val headerValue: String = "",
    val temperaturePath: String = "temperature",
    val descriptionPath: String = "description",
    val locationPath: String = "location",
    val locationLabel: String = "",
    val fahrenheit: Boolean = false,
    val refreshMinutes: Int = 30,
) {
    fun canFetch(): Boolean = enabled && when (source) {
        SidebarWeatherSource.CustomApi -> true
        SidebarWeatherSource.Xiaomi -> xiaomiAccepted
        SidebarWeatherSource.OpenMeteo -> openMeteoAccepted
    }

    fun normalized() = copy(
        url = url.trim(), headerName = headerName.trim(), headerValue = headerValue.trim(),
        latitude = latitude.trim(), longitude = longitude.trim(),
        temperaturePath = temperaturePath.trim(), descriptionPath = descriptionPath.trim(),
        locationPath = locationPath.trim(), locationLabel = locationLabel.trim().take(48),
        refreshMinutes = refreshMinutes.takeIf { it in REFRESH_INTERVALS } ?: 30,
    )

    internal fun toJson(): JSONObject = JSONObject().apply {
        put("enabled", enabled)
        put("source", source.name)
        put("xiaomiAccepted", xiaomiAccepted)
        put("openMeteoAccepted", openMeteoAccepted)
        put("latitude", latitude)
        put("longitude", longitude)
        put("url", url)
        put("headerName", headerName)
        put("headerValue", headerValue)
        put("temperaturePath", temperaturePath)
        put("descriptionPath", descriptionPath)
        put("locationPath", locationPath)
        put("locationLabel", locationLabel)
        put("fahrenheit", fahrenheit)
        put("refreshMinutes", refreshMinutes)
    }

    // Cache identity covers credentials and mappings, without storing them in cache entries.
    internal fun cacheKey(): String {
        val json = normalized().toJson().apply {
            remove("enabled")
            remove("xiaomiAccepted")
            remove("openMeteoAccepted")
            remove("refreshMinutes")
            // Keep pre-Xiaomi custom API cache identities valid across upgrades.
            if (source != SidebarWeatherSource.OpenMeteo) {
                remove("latitude")
                remove("longitude")
            }
            if (source == SidebarWeatherSource.CustomApi) remove("source")
            if (source == SidebarWeatherSource.OpenMeteo) {
                listOf("url", "headerName", "headerValue", "temperaturePath", "descriptionPath", "locationPath")
                    .forEach(::remove)
            }
        }
        return MessageDigest.getInstance("SHA-256").digest(json.toString().toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    override fun toString(): String = "SidebarWeatherConfig(enabled=$enabled, credentials=redacted)"

    companion object {
        val REFRESH_INTERVALS = listOf(15, 30, 60, 120)

        internal fun fromJson(value: String?): SidebarWeatherConfig = runCatching {
            val json = JSONObject(value.orEmpty())
            SidebarWeatherConfig(
                enabled = json.optBoolean("enabled"),
                source = SidebarWeatherSource.entries.firstOrNull { it.name == json.optString("source") }
                    ?: SidebarWeatherSource.CustomApi,
                xiaomiAccepted = json.optBoolean("xiaomiAccepted"),
                openMeteoAccepted = json.optBoolean("openMeteoAccepted"),
                latitude = json.optString("latitude"),
                longitude = json.optString("longitude"),
                url = json.optString("url"),
                headerName = json.optString("headerName"),
                headerValue = json.optString("headerValue"),
                temperaturePath = json.optString("temperaturePath", "temperature"),
                descriptionPath = json.optString("descriptionPath", "description"),
                locationPath = json.optString("locationPath", "location"),
                locationLabel = json.optString("locationLabel"),
                fahrenheit = json.optBoolean("fahrenheit"),
                refreshMinutes = json.optInt("refreshMinutes", 30),
            ).normalized()
        }.getOrDefault(SidebarWeatherConfig())
    }
}

enum class SidebarWeatherSource { CustomApi, Xiaomi, OpenMeteo }

enum class SidebarWeatherCondition { Clear, Cloud, Rain, Snow, Storm, Fog, Night, Unknown }

data class SidebarWeatherReading(
    val temperature: Double,
    val description: String,
    val location: String,
    val fetchedAt: Long,
    val fahrenheit: Boolean? = null,
    val weatherType: Int? = null,
    val openMeteoCode: Int? = null,
    val isDay: Boolean? = null,
) {
    fun temperatureText(): String = DecimalFormat("0.#", DecimalFormatSymbols(Locale.ROOT)).format(temperature)

    val condition: SidebarWeatherCondition get() = openMeteoCode?.let { openMeteoWeatherCondition(it, isDay) }
        ?: xiaomiWeatherCondition(weatherType, description)
}

enum class SidebarWeatherError {
    Url, Header, Path, Temperature, Json, TooLarge, Redirect, Http, Timeout, Tls, Network,
    Provider, Permission, Coordinates,
}

class SidebarWeatherException(
    val reason: SidebarWeatherError,
    val statusCode: Int? = null,
) : IOException(reason.name) // Never expose URLs, response bodies, or credentials in errors.

internal fun validateSidebarWeatherConfig(config: SidebarWeatherConfig): SidebarWeatherError? {
    val url = config.url.toHttpUrlOrNull()
    if (config.url.length > 4096 || config.url.any(Char::isISOControl) || url == null ||
        !url.isHttps || url.username.isNotEmpty() || url.password.isNotEmpty() || url.fragment != null
    ) return SidebarWeatherError.Url
    if (config.headerName.isBlank() != config.headerValue.isBlank()) return SidebarWeatherError.Header
    if (config.headerName.isNotEmpty()) {
        if (!Regex("[!#$%&'*+.^_`|~0-9A-Za-z-]{1,64}").matches(config.headerName) ||
            config.headerValue.length > 4096 || config.headerValue.any { it.code !in 32..126 } ||
            config.headerName.lowercase(Locale.ROOT) in setOf(
                "host", "connection", "content-length", "transfer-encoding", "cookie", "accept-encoding"
            )
        ) return SidebarWeatherError.Header
    }
    if (!validWeatherPath(config.temperaturePath) ||
        listOf(config.descriptionPath, config.locationPath).any { it.isNotEmpty() && !validWeatherPath(it) }
    ) return SidebarWeatherError.Path
    return null
}

// Small, bounded JSON path subset: data.now.temp, results[0].temperature, $.current.temp.
private val WEATHER_PATH = Regex("(?:\\$\\.)?[A-Za-z_][A-Za-z0-9_-]*(?:\\[[0-9]{1,4}])*(?:\\.[A-Za-z_][A-Za-z0-9_-]*(?:\\[[0-9]{1,4}])*)*")
private val WEATHER_TOKEN = Regex("[A-Za-z_][A-Za-z0-9_-]*|\\[([0-9]+)]")
private fun validWeatherPath(path: String): Boolean =
    path.length <= 256 && WEATHER_PATH.matches(path) && WEATHER_TOKEN.findAll(path).count() <= 16

internal fun weatherJsonValue(root: Any, path: String): Any? {
    if (!validWeatherPath(path)) return null
    var value: Any? = root
    for (token in WEATHER_TOKEN.findAll(path.removePrefix("$."))) {
        value = if (token.value.startsWith("[")) {
            (value as? JSONArray)?.opt(token.groupValues[1].toInt())
        } else {
            (value as? JSONObject)?.opt(token.value)
        }
        if (value == null || value == JSONObject.NULL) return null
    }
    return value
}

internal fun parseSidebarWeather(json: String, config: SidebarWeatherConfig, now: Long): SidebarWeatherReading {
    // Bound nesting before handing untrusted JSON to the recursive platform parser.
    var depth = 0
    var quoted = false
    var escaped = false
    for (char in json) {
        if (quoted) {
            if (escaped) escaped = false else when (char) {
                '\\' -> escaped = true
                '"' -> quoted = false
            }
        } else when (char) {
            '"' -> quoted = true
            '{', '[' -> if (++depth > 32) throw SidebarWeatherException(SidebarWeatherError.Json)
            '}', ']' -> depth--
        }
    }
    val root = try {
        val tokens = JSONTokener(json)
        val value = tokens.nextValue()
        if (value !is JSONObject || tokens.nextClean() != '\u0000') throw SidebarWeatherException(SidebarWeatherError.Json)
        value
    } catch (_: org.json.JSONException) {
        throw SidebarWeatherException(SidebarWeatherError.Json)
    }
    val value = weatherJsonValue(root, config.temperaturePath)
    val temperature = when (value) {
        is Number -> value.toDouble()
        is String -> value.trim().toDoubleOrNull()
        else -> null
    } ?: throw SidebarWeatherException(SidebarWeatherError.Temperature)
    if (!temperature.isFinite() || temperature !in -200.0..200.0) {
        throw SidebarWeatherException(SidebarWeatherError.Temperature)
    }
    fun text(path: String): String = (weatherJsonValue(root, path) as? String).orEmpty()
        .filterNot { it.isISOControl() || it in '\u202A'..'\u202E' || it in '\u2066'..'\u2069' }
        .trim().take(80)
    return SidebarWeatherReading(
        temperature = temperature,
        description = text(config.descriptionPath),
        location = config.locationLabel.ifBlank { text(config.locationPath) },
        fetchedAt = now,
    )
}

internal fun weatherCondition(description: String): SidebarWeatherCondition {
    val text = description.lowercase(Locale.ROOT)
    return when {
        listOf("thunder", "storm", "\u96f7").any(text::contains) -> SidebarWeatherCondition.Storm
        listOf("snow", "sleet", "blizzard", "\u96ea", "\u51b0\u96f9").any(text::contains) -> SidebarWeatherCondition.Snow
        listOf("rain", "drizzle", "shower", "\u96e8").any(text::contains) -> SidebarWeatherCondition.Rain
        listOf("fog", "mist", "haze", "\u96fe", "\u973e", "\u6c99").any(text::contains) -> SidebarWeatherCondition.Fog
        listOf("cloud", "overcast", "\u4e91", "\u9634").any(text::contains) -> SidebarWeatherCondition.Cloud
        listOf("night", "\u591c").any(text::contains) -> SidebarWeatherCondition.Night
        listOf("sun", "clear", "\u6674").any(text::contains) -> SidebarWeatherCondition.Clear
        else -> SidebarWeatherCondition.Unknown
    }
}

/** Xiaomi weather_type values from the official weather phenomenon table. */
internal fun xiaomiWeatherCondition(type: Int?, description: String): SidebarWeatherCondition = when (type) {
    0 -> if (weatherCondition(description) == SidebarWeatherCondition.Night) SidebarWeatherCondition.Night
        else SidebarWeatherCondition.Clear
    1, 2 -> SidebarWeatherCondition.Cloud
    3, 18, 19, 20, 21, 23, 24 -> SidebarWeatherCondition.Fog
    4, 5, 6, 8, 9, 10, 11 -> SidebarWeatherCondition.Rain
    7 -> SidebarWeatherCondition.Storm
    12, 13, 14, 15, 16, 17, 22, 25 -> SidebarWeatherCondition.Snow
    99 -> SidebarWeatherCondition.Unknown
    else -> weatherCondition(description)
}

internal class SidebarWeatherClient(
    private val client: OkHttpClient = defaultClient,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun fetch(settings: SidebarWeatherConfig): SidebarWeatherReading {
        // Validate before trimming so that CR/LF in an entered header is never silently accepted.
        validateSidebarWeatherConfig(settings)?.let { throw SidebarWeatherException(it) }
        val config = settings.normalized()
        val request = Request.Builder().url(config.url).get().header("Accept", "application/json")
            .apply { if (config.headerName.isNotBlank()) header(config.headerName, config.headerValue) }.build()
        return request(request) { parseSidebarWeather(it, config, now()) }
    }

    suspend fun fetchOpenMeteo(config: SidebarWeatherConfig): SidebarWeatherReading {
        if (config.source != SidebarWeatherSource.OpenMeteo || !config.canFetch()) {
            throw SidebarWeatherException(SidebarWeatherError.Permission)
        }
        val request = Request.Builder().url(openMeteoUrl(config)).get().header("Accept", "application/json").build()
        return request(request) { parseOpenMeteoWeather(it, config, now()) }
    }

    private suspend fun request(request: Request, parse: (String) -> SidebarWeatherReading): SidebarWeatherReading {
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    val reason = when (e) {
                        is SocketTimeoutException, is java.io.InterruptedIOException -> SidebarWeatherError.Timeout
                        is SSLException -> SidebarWeatherError.Tls
                        is UnknownHostException -> SidebarWeatherError.Network
                        else -> SidebarWeatherError.Network
                    }
                    continuation.resumeWithException(SidebarWeatherException(reason))
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val reading = response.use {
                            if (it.code in 300..399) throw SidebarWeatherException(SidebarWeatherError.Redirect)
                            if (!it.isSuccessful) throw SidebarWeatherException(SidebarWeatherError.Http, it.code)
                            val source = it.body.source()
                            // Limit the decompressed body, including chunked or dishonest Content-Length responses.
                            source.request(MAX_RESPONSE_BYTES + 1)
                            if (source.buffer.size > MAX_RESPONSE_BYTES) throw SidebarWeatherException(SidebarWeatherError.TooLarge)
                            parse(source.readUtf8())
                        }
                        continuation.resume(reading)
                    } catch (e: Exception) {
                        val failure = when (e) {
                            is SidebarWeatherException -> e
                            is java.io.InterruptedIOException -> SidebarWeatherException(SidebarWeatherError.Timeout)
                            else -> SidebarWeatherException(SidebarWeatherError.Network)
                        }
                        continuation.resumeWithException(failure)
                    }
                }
            })
        }
    }

    companion object {
        private const val MAX_RESPONSE_BYTES = 128L * 1024L
        // Connection tests and automatic updates share a bounded client/pool.
        private val defaultClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .callTimeout(15, TimeUnit.SECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
                .retryOnConnectionFailure(false)
                .build()
        }
    }
}
