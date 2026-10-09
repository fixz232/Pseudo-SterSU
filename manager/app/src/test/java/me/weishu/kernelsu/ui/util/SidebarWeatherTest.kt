package me.weishu.kernelsu.ui.util

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import me.weishu.kernelsu.ui.component.bottombar.icon
import org.junit.Assert.*
import org.junit.After
import org.junit.Test
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLHandshakeException

class SidebarWeatherTest {
    private val config = SidebarWeatherConfig(enabled = true, url = "https://weather.example.test/current")
    private val testClients = mutableListOf<OkHttpClient>()

    @After
    fun closeTestClients() {
        testClients.forEach {
            it.dispatcher.cancelAll()
            it.dispatcher.executorService.shutdown()
            it.connectionPool.evictAll()
        }
    }

    @Test
    fun defaultsDoNotEnableNetworkAndOldConfigStillLoads() {
        assertFalse(SidebarWeatherConfig.fromJson(null).enabled)
        assertEquals(SidebarWeatherConfig(), SidebarWeatherConfig.fromJson("not json"))
        assertEquals(30, config.copy(refreshMinutes = 0).normalized().refreshMinutes)
        assertEquals(SidebarWeatherSource.CustomApi, SidebarWeatherConfig.fromJson("""{"enabled":true,"url":"https://weather.test"}""").source)
    }

    @Test
    fun configRoundTripsAndNeverPrintsCredentials() {
        val original = config.copy(headerName = "Authorization", headerValue = "Bearer private-token", locationLabel = "Test city")
        assertEquals(original, SidebarWeatherConfig.fromJson(original.toJson().toString()))
        assertFalse(original.toString().contains("private-token"))
        assertFalse(original.toString().contains("weather.example"))
    }

    @Test
    fun xiaomiRequiresExplicitAcceptanceAndRoundTrips() {
        val pending = config.copy(source = SidebarWeatherSource.Xiaomi)
        assertFalse(pending.canFetch())
        assertFalse(SidebarWeatherConfig.fromJson(pending.toJson().toString()).canFetch())
        val accepted = pending.copy(xiaomiAccepted = true)
        assertTrue(accepted.canFetch())
        assertEquals(accepted, SidebarWeatherConfig.fromJson(accepted.toJson().toString()))
        assertNotEquals(config.cacheKey(), accepted.cacheKey())
        assertEquals(pending.cacheKey(), accepted.cacheKey())
    }

    @Test
    fun xiaomiProviderFieldsHandleUnitsAndRejectInvalidTemperatures() {
        val celsius = parseXiaomiWeather(mapOf(
            "temperature" to "18\u2103", "temperature_unit" to "1",
            "description" to "\u591a\u4e91", "city_name" to "\u5b89\u5b81\u5e84\u5357\u8def",
        ), 123L)
        assertEquals(18.0, celsius.temperature, 0.0)
        assertEquals(false, celsius.fahrenheit)
        assertEquals("\u5b89\u5b81\u5e84\u5357\u8def", celsius.location)
        assertEquals(SidebarWeatherCondition.Cloud, celsius.condition)
        assertEquals(123L, celsius.fetchedAt)
        assertEquals(42L, parseXiaomiWeather(mapOf("temperature" to "18", "publish_time" to "42"), 123L).fetchedAt)
        assertEquals(123L, parseXiaomiWeather(mapOf("temperature" to "18", "publish_time" to "invalid"), 123L).fetchedAt)
        val fahrenheit = parseXiaomiWeather(mapOf("temperature" to "64\u2109", "temperature_unit" to "0"), 456L)
        assertEquals(64.0, fahrenheit.temperature, 0.0)
        assertEquals(true, fahrenheit.fahrenheit)
        val storm = parseXiaomiWeather(mapOf("temperature" to "18", "description" to "Clear", "weather_type" to "7"), 456L)
        assertEquals(7, storm.weatherType)
        assertEquals(SidebarWeatherCondition.Storm, storm.condition)
        assertEquals(80, parseXiaomiWeather(mapOf("temperature" to "2", "city_name" to "\u202E" + "a".repeat(100)), 1L).location.length)
        listOf(null, "", "NaN", "18 degrees", "999\u2103").forEach { value ->
            expectError(SidebarWeatherError.Temperature) {
                parseXiaomiWeather(mapOf("temperature" to value), 1L)
            }
        }
    }

    @Test
    fun allDocumentedXiaomiWeatherCodesHaveMatchingSmallIcons() {
        val expected = mapOf(
            SidebarWeatherCondition.Clear to listOf(0),
            SidebarWeatherCondition.Cloud to listOf(1, 2),
            SidebarWeatherCondition.Fog to listOf(3, 18, 19, 20, 21, 23, 24),
            SidebarWeatherCondition.Rain to listOf(4, 5, 6, 8, 9, 10, 11, 25),
            SidebarWeatherCondition.Storm to listOf(7),
            SidebarWeatherCondition.Snow to listOf(12, 13, 14, 15, 16, 17, 22),
        )
        expected.forEach { (condition, codes) ->
            codes.forEach { code ->
                val result = xiaomiWeatherCondition(code, "")
                assertEquals("code $code", condition, result)
                assertNotNull(result.icon())
            }
        }
        assertEquals((0..25).toList(), expected.values.flatten().sorted())
        assertEquals(SidebarWeatherCondition.Unknown, xiaomiWeatherCondition(99, "Clear"))
        assertEquals(SidebarWeatherCondition.Clear, xiaomiWeatherCondition(null, "Clear"))
        assertEquals(SidebarWeatherCondition.Night, xiaomiWeatherCondition(0, "Clear night"))
    }

    @Test
    fun onlyCredentialFreeHttpsEndpointsAreAccepted() {
        listOf("", "http://weather.test", "file:///data/file", "https://user:pass@weather.test", "https://weather.test/#secret", "https://weather.test/\n").forEach {
            assertEquals(it, SidebarWeatherError.Url, validateSidebarWeatherConfig(config.copy(url = it)))
        }
        assertNull(validateSidebarWeatherConfig(config.copy(url = "https://weather.test/now?key=private&city=123")))
    }

    @Test
    fun headerValidationRejectsInjectionAndPartialHeaders() {
        listOf(
            "Authorization" to "Bearer token\r\nX-Injected: yes",
            "Authorization" to "", "" to "secret", "Invalid Header" to "value",
            "Host" to "other.example", "Cookie" to "a=b", "Connection" to "upgrade",
            "Accept-Encoding" to "gzip", "X-Key" to "\u4e2d\u6587",
        ).forEach { (name, value) ->
            assertEquals(SidebarWeatherError.Header, validateSidebarWeatherConfig(config.copy(headerName = name, headerValue = value)))
        }
        assertNull(validateSidebarWeatherConfig(config.copy(headerName = "X-API-Key", headerValue = "secret")))
    }

    @Test
    fun parsesFlatNumbersAndNumericStringsWithoutFakeDefaults() {
        val reading = parseSidebarWeather("""{"temperature":-2.5,"description":"Snow","location":"Test"}""", config, 100L)
        assertEquals(-2.5, reading.temperature, 0.0)
        assertEquals("-2.5", reading.temperatureText())
        assertEquals("Test", reading.location)
        assertEquals(100L, reading.fetchedAt)
        assertEquals(SidebarWeatherCondition.Snow, reading.condition)
        assertEquals(0.0, parseSidebarWeather("""{"temperature":"0"}""", config, 1L).temperature, 0.0)
    }

    @Test
    fun parsesNestedPathsAndArrayIndices() {
        val settings = config.copy(temperaturePath = "$.results[0].now.temp", descriptionPath = "results[0].now.text", locationPath = "results[0].location.name")
        val reading = parseSidebarWeather("""{"results":[{"now":{"temp":"26.3","text":"Clear"},"location":{"name":"City"}}]}""", settings, 1)
        assertEquals(26.3, reading.temperature, 0.0)
        assertEquals("City", reading.location)
        assertEquals(SidebarWeatherCondition.Clear, reading.condition)
    }

    @Test
    fun pathValidationRejectsWildcardsEmptyRequiredPathsAndExcessiveDepth() {
        listOf("", "now..temp", "items[*].temp", "data[-1].temp", "a.".repeat(20) + "temp", "[0].temp").forEach {
            assertEquals(it, SidebarWeatherError.Path, validateSidebarWeatherConfig(config.copy(temperaturePath = it)))
        }
        assertNull(validateSidebarWeatherConfig(config.copy(descriptionPath = "", locationPath = "")))
        assertNull(weatherJsonValue(JSONObject("""{"data":[]}"""), "data[9999].temp"))
    }

    @Test
    fun missingNullMalformedAndNonFiniteTemperaturesAreRejected() {
        listOf("{}", """{"temperature":null}""", """{"temperature":true}""", """{"temperature":"NaN"}""", """{"temperature":1e309}""", """{"temperature":999}""", """{"temperature":"26 C"}""").forEach {
            expectError(SidebarWeatherError.Temperature) { parseSidebarWeather(it, config, 1) }
        }
    }

    @Test
    fun malformedHtmlTrailingContentAndDeepJsonAreRejected() {
        listOf("<html>Error</html>", "[]", "null", "{", """{"temperature":1} trailing""", "{\"a\":".repeat(64) + "0" + "}".repeat(64)).forEach {
            expectError(SidebarWeatherError.Json) { parseSidebarWeather(it, config, 1) }
        }
    }

    @Test
    fun optionalTextIsBoundedSanitizedAndLocationCanBeOverridden() {
        val json = JSONObject().put("temperature", 20).put("description", "\u202E" + "a".repeat(200)).put("location", "API city").toString()
        val reading = parseSidebarWeather(json, config.copy(locationLabel = "My city"), 1)
        assertEquals(80, reading.description.length)
        assertFalse(reading.description.contains('\u202E'))
        assertEquals("My city", reading.location)
        val optional = parseSidebarWeather("""{"temperature":20,"description":{},"location":null}""", config, 1)
        assertEquals("", optional.description)
        assertEquals("", optional.location)
    }

    @Test
    fun weatherIconsRecognizeChineseAndEnglishConditions() {
        assertEquals(SidebarWeatherCondition.Clear, weatherCondition("\u6674"))
        assertEquals(SidebarWeatherCondition.Cloud, weatherCondition("\u591a\u4e91"))
        assertEquals(SidebarWeatherCondition.Storm, weatherCondition("\u96f7\u9635\u96e8"))
        assertEquals(SidebarWeatherCondition.Snow, weatherCondition("\u96e8\u5939\u96ea"))
        assertEquals(SidebarWeatherCondition.Rain, weatherCondition("Light rain"))
        assertEquals(SidebarWeatherCondition.Fog, weatherCondition("Mist"))
        assertEquals(SidebarWeatherCondition.Night, weatherCondition("Clear night"))
        assertEquals(SidebarWeatherCondition.Unknown, weatherCondition(""))
    }

    @Test
    fun cacheIdentityIncludesCredentialsMappingsUnitsAndCityButNotSchedule() {
        assertEquals(config.cacheKey(), config.copy(refreshMinutes = 60, enabled = false).cacheKey())
        listOf(
            config.copy(url = "https://other.test"), config.copy(headerName = "X-Key", headerValue = "secret"),
            config.copy(temperaturePath = "data.temp"), config.copy(locationLabel = "other"), config.copy(fahrenheit = true),
        ).forEach { assertNotEquals(config.cacheKey(), it.cacheKey()) }
    }

    @Test
    fun networkRequestUsesGetAndOnlyTheConfiguredAuthentication() = runBlocking {
        val settings = config.copy(headerName = "Authorization", headerValue = "Bearer secret")
        val client = client { request ->
            assertEquals("GET", request.method)
            assertEquals("Bearer secret", request.header("Authorization"))
            assertEquals("application/json", request.header("Accept"))
            assertNull(request.header("Cookie"))
            response(request, 200, """{"temperature":23,"description":"Clear"}""")
        }
        assertEquals(23.0, SidebarWeatherClient(client) { 123 }.fetch(settings).temperature, 0.0)
    }

    @Test
    fun networkErrorsAreSpecificAndDoNotLeakResponseBodiesOrKeys() = runBlocking {
        for ((status, reason) in listOf(302 to SidebarWeatherError.Redirect, 401 to SidebarWeatherError.Http, 429 to SidebarWeatherError.Http, 500 to SidebarWeatherError.Http)) {
            val failure = try {
                SidebarWeatherClient(client { response(it, status, "secret-token") }).fetch(config)
                fail("Expected failure"); null
            } catch (e: SidebarWeatherException) { e }
            assertEquals(reason, failure!!.reason)
            assertFalse(failure.message.orEmpty().contains("secret-token"))
            if (reason == SidebarWeatherError.Http) assertEquals(status, failure.statusCode)
        }
    }

    @Test
    fun oversizedAndInvalidBodiesAreHandled() = runBlocking {
        for ((body, reason) in listOf("a".repeat(128 * 1024 + 1) to SidebarWeatherError.TooLarge, "<html>bad</html>" to SidebarWeatherError.Json)) {
            try {
                SidebarWeatherClient(client { response(it, 200, body) }).fetch(config)
                fail("Expected failure")
            } catch (e: SidebarWeatherException) { assertEquals(reason, e.reason) }
        }
    }

    @Test
    fun timeoutAndCertificateErrorsAreReportedWithoutBypass() = runBlocking {
        for ((exception, expected) in listOf(InterruptedIOException("timeout") to SidebarWeatherError.Timeout, SSLHandshakeException("secret-host") to SidebarWeatherError.Tls)) {
            try {
                SidebarWeatherClient(client { throw exception }).fetch(config)
                fail("Expected failure")
            } catch (e: SidebarWeatherException) { assertEquals(expected, e.reason) }
        }
    }

    @Test
    fun cancellationCancelsTheHttpCall() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = java.util.concurrent.CountDownLatch(1)
        val requestClient = client {
            entered.complete(Unit)
            release.await(5, TimeUnit.SECONDS)
            response(it, 200, """{"temperature":20}""")
        }
        val job = launch { SidebarWeatherClient(requestClient).fetch(config) }
        try {
            entered.await()
            job.cancelAndJoin()
            assertTrue(requestClient.dispatcher.runningCalls().single().isCanceled())
        } finally { release.countDown() }
    }

    private fun expectError(expected: SidebarWeatherError, block: () -> Unit) {
        try { block(); fail("Expected $expected") }
        catch (e: SidebarWeatherException) { assertEquals(expected, e.reason) }
    }

    private fun client(block: (okhttp3.Request) -> Response) = OkHttpClient.Builder()
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
        .addInterceptor { block(it.request()) }.build().also(testClients::add)

    private fun response(request: okhttp3.Request, status: Int, body: String) = Response.Builder()
        .request(request).protocol(Protocol.HTTP_1_1).code(status).message("test")
        .body(body.toResponseBody()).build()
}
