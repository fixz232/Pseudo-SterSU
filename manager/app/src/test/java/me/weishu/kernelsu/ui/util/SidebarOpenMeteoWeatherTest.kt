package me.weishu.kernelsu.ui.util

import kotlinx.coroutines.runBlocking
import me.weishu.kernelsu.ui.component.bottombar.icon
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class SidebarOpenMeteoWeatherTest {
    private val config = SidebarWeatherConfig(
        enabled = true, source = SidebarWeatherSource.OpenMeteo, openMeteoAccepted = true,
        latitude = "39.9", longitude = "116.4", locationLabel = "Beijing",
    )
    private val clients = mutableListOf<OkHttpClient>()

    @After
    fun closeClients() {
        clients.forEach {
            it.dispatcher.cancelAll()
            it.dispatcher.executorService.shutdown()
            it.connectionPool.evictAll()
        }
    }

    @Test
    fun consentGatesRequestsAndSettingsRoundTrip() {
        val pending = config.copy(openMeteoAccepted = false)
        assertFalse(pending.canFetch())
        assertFalse(SidebarWeatherConfig.fromJson(pending.toJson().toString()).canFetch())
        assertTrue(config.canFetch())
        assertEquals(config, SidebarWeatherConfig.fromJson(config.toJson().toString()))
        assertEquals(config.cacheKey(), pending.copy(refreshMinutes = 60).cacheKey())
        assertNotEquals(config.cacheKey(), config.copy(latitude = "40.0").cacheKey())
        assertNotEquals(config.cacheKey(), config.copy(longitude = "117.0").cacheKey())
        assertNotEquals(config.cacheKey(), config.copy(fahrenheit = true).cacheKey())
        assertNotEquals(config.cacheKey(), config.copy(locationLabel = "Other").cacheKey())
        assertEquals(config.cacheKey(), config.copy(url = "https://other.test", headerValue = "secret").cacheKey())
    }

    @Test
    fun coordinatesAreValidatedAndOnlySentToTheFixedEndpoint() {
        val url = openMeteoUrl(config)
        assertEquals("https", url.scheme)
        assertEquals("api.open-meteo.com", url.host)
        assertEquals("/v1/forecast", url.encodedPath)
        assertEquals("39.9", url.queryParameter("latitude"))
        assertEquals("116.4", url.queryParameter("longitude"))
        assertEquals("temperature_2m,weather_code,is_day", url.queryParameter("current"))
        assertEquals("auto", url.queryParameter("timezone"))
        assertFalse(url.toString().contains("Beijing"))
        assertEquals("fahrenheit", openMeteoUrl(config.copy(fahrenheit = true)).queryParameter("temperature_unit"))
        listOf("", "NaN", "Infinity", "90.1", "-90.1", "abc", "1\n2").forEach {
            expectError(SidebarWeatherError.Coordinates) { openMeteoUrl(config.copy(latitude = it)) }
        }
        listOf("", "180.1", "-180.1", "NaN").forEach {
            expectError(SidebarWeatherError.Coordinates) { openMeteoUrl(config.copy(longitude = it)) }
        }
        assertEquals("-90.0", openMeteoUrl(config.copy(latitude = "-90")).queryParameter("latitude"))
        assertEquals("180.0", openMeteoUrl(config.copy(longitude = "180")).queryParameter("longitude"))
    }

    @Test
    fun currentFieldsMapToTheSidebarReadingAndNightIcon() {
        val reading = parseOpenMeteoWeather(body(code = 0, day = 0), config, 123L)
        assertEquals(21.5, reading.temperature, 0.0)
        assertEquals("Beijing", reading.location)
        assertEquals(false, reading.fahrenheit)
        assertEquals(0, reading.openMeteoCode)
        assertEquals(false, reading.isDay)
        assertEquals(123L, reading.fetchedAt)
        assertEquals(SidebarWeatherCondition.Night, reading.condition)
        assertNotNull(reading.condition.icon())
        val fahrenheit = parseOpenMeteoWeather(body(code = 61, unit = "°F"), config.copy(fahrenheit = true), 456L)
        assertEquals(true, fahrenheit.fahrenheit)
        assertEquals(SidebarWeatherCondition.Rain, fahrenheit.condition)
    }

    @Test
    fun documentedWmoCodesMapToSmallWeatherIcons() {
        val expected = mapOf(
            SidebarWeatherCondition.Clear to listOf(0, 1),
            SidebarWeatherCondition.Cloud to listOf(2, 3),
            SidebarWeatherCondition.Fog to listOf(45, 48),
            SidebarWeatherCondition.Rain to listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82),
            SidebarWeatherCondition.Snow to listOf(71, 73, 75, 77, 85, 86),
            SidebarWeatherCondition.Storm to listOf(95, 96, 99),
        )
        expected.forEach { (condition, codes) -> codes.forEach { code ->
            assertEquals("code $code", condition, openMeteoWeatherCondition(code, true))
            assertNotNull(openMeteoWeatherCondition(code, true).icon())
        } }
        assertEquals(SidebarWeatherCondition.Night, openMeteoWeatherCondition(1, false))
        assertEquals(SidebarWeatherCondition.Unknown, openMeteoWeatherCondition(100, true))
        assertNotNull(openMeteoWeatherCondition(100, true).icon())
    }

    @Test
    fun invalidWeatherDataCannotBeShownWithWrongIconOrUnit() {
        listOf(
            body(code = 0).replace("\"weather_code\":0", "\"weather_code\":null"),
            body(code = 0).replace("\"is_day\":1", "\"is_day\":2"),
            body(code = 0).replace("\"is_day\":1", "\"is_day\":0.5"),
            body(code = 0).replace("\"°C\"", "\"°F\""),
        ).forEach { expectError(SidebarWeatherError.Json) { parseOpenMeteoWeather(it, config, 1L) } }
        expectError(SidebarWeatherError.Temperature) {
            parseOpenMeteoWeather(body(code = 0).replace("\"temperature_2m\":21.5", "\"temperature_2m\":null"), config, 1L)
        }
    }

    @Test
    fun requestUsesOnlyOpenMeteoFieldsAfterConsent() = runBlocking {
        val client = OkHttpClient.Builder().followRedirects(false).retryOnConnectionFailure(false)
            .addInterceptor { chain ->
                val request = chain.request()
                assertEquals("GET", request.method)
                assertEquals("api.open-meteo.com", request.url.host)
                assertEquals("application/json", request.header("Accept"))
                assertNull(request.header("Authorization"))
                assertNull(request.header("Cookie"))
                assertEquals("temperature_2m,weather_code,is_day", request.url.queryParameter("current"))
                Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                    .body(body(code = 3).toResponseBody()).build()
            }.build().also(clients::add)
        val weatherClient = SidebarWeatherClient(client) { 123L }
        expectError(SidebarWeatherError.Permission) {
            runBlocking { weatherClient.fetchOpenMeteo(config.copy(openMeteoAccepted = false)) }
        }
        assertEquals(SidebarWeatherCondition.Cloud, weatherClient.fetchOpenMeteo(config).condition)
    }

    private fun body(code: Int, day: Int = 1, unit: String = "°C") =
        """{"current_units":{"temperature_2m":"$unit"},"current":{"temperature_2m":21.5,"weather_code":$code,"is_day":$day}}"""

    private fun expectError(expected: SidebarWeatherError, block: () -> Unit) {
        try { block(); fail("Expected $expected") }
        catch (e: SidebarWeatherException) { assertEquals(expected, e.reason) }
    }
}
