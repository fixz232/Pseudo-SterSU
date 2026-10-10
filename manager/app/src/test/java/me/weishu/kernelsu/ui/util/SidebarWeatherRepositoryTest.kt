package me.weishu.kernelsu.ui.util

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

class SidebarWeatherRepositoryTest {
    private val config = SidebarWeatherConfig(enabled = true, url = "https://weather.test/current")
    private fun reading(time: Long = 1_000L) = SidebarWeatherReading(20.0, "Clear", "City", time)

    @Test
    fun manualModeNeverRequestsWeather() = runBlocking {
        val repository = SidebarWeatherRepository(fetch = { error("Must not fetch") })
        repository.refresh(config.copy(enabled = false), force = true)
        repository.refresh(config.copy(source = SidebarWeatherSource.Xiaomi), force = true)
        repository.accept(config.copy(source = SidebarWeatherSource.Xiaomi), reading())
        repository.refresh(config.copy(source = SidebarWeatherSource.OpenMeteo), force = true)
        repository.accept(config.copy(source = SidebarWeatherSource.OpenMeteo), reading())
        assertNull(repository.state.value.reading)
    }

    @Test
    fun sharedPreviewAndSidebarRequestsAreCoalesced() = runBlocking {
        var requests = 0
        val repository = SidebarWeatherRepository(fetch = { requests++; yield(); reading() }, now = { 1_000L })
        listOf(async { repository.refresh(config) }, async { repository.refresh(config) }).awaitAll()
        assertEquals(1, requests)
        assertFalse(repository.state.value.loading)
        assertEquals(reading(), repository.state.value.reading)
    }

    @Test
    fun autoRefreshIntervalAndManualThrottleAreEnforced() = runBlocking {
        var time = 1_000L
        var requests = 0
        val repository = SidebarWeatherRepository(fetch = { requests++; reading(time) }, now = { time })
        repository.refresh(config)
        time += 9_999
        repository.refresh(config, force = true)
        assertEquals(1, requests)
        time++
        repository.refresh(config, force = true)
        assertEquals(2, requests)
        time += 60_000
        repository.refresh(config)
        assertEquals(2, requests)
        time += 30 * 60_000
        repository.refresh(config)
        assertEquals(3, requests)
    }

    @Test
    fun failedRefreshKeepsLastReadingAndDoesNotRetryOnEveryResume() = runBlocking {
        var time = 1_000L
        var requests = 0
        val repository = SidebarWeatherRepository(fetch = {
            if (++requests > 1) throw SidebarWeatherException(SidebarWeatherError.Http, 429)
            reading()
        }, now = { time })
        repository.refresh(config)
        time += 30 * 60_000
        repository.refresh(config)
        repeat(10) { repository.refresh(config) }
        assertEquals(2, requests)
        assertEquals(reading(), repository.state.value.reading)
        assertEquals(SidebarWeatherError.Http, repository.state.value.error)
        assertEquals(429, repository.state.value.httpStatus)
    }

    @Test
    fun changedEndpointsNeverShowAnotherEndpointsCachedWeather() = runBlocking {
        val repository = SidebarWeatherRepository(fetch = {
            if (it.url != config.url) throw SidebarWeatherException(SidebarWeatherError.Network)
            reading()
        }, now = { 1_000L })
        repository.refresh(config)
        repository.refresh(config.copy(url = "https://other.test"))
        assertNull(repository.state.value.reading)
        assertEquals(SidebarWeatherError.Network, repository.state.value.error)
    }

    @Test
    fun freshPersistedCacheAvoidsAnExtraStartupRequest() = runBlocking {
        val repository = SidebarWeatherRepository(
            fetch = { error("Fresh cache must not fetch") }, now = { 2_000L },
            loadCache = { key -> if (key == config.cacheKey()) reading() else null },
        )
        repository.refresh(config)
        assertEquals(reading(), repository.state.value.reading)
    }

    @Test
    fun testedConfigurationPublishesAndPersistsSuccessfulWeather() = runBlocking {
        var cached: SidebarWeatherReading? = null
        val repository = SidebarWeatherRepository(
            fetch = { error("Tested reading must not fetch again") }, now = { 1_000L },
            saveCache = { key, value -> assertEquals(config.cacheKey(), key); cached = value },
        )
        repository.accept(config, reading())
        repository.refresh(config)
        assertEquals(reading(), cached)
        assertEquals(reading(), repository.state.value.reading)
        assertNull(repository.state.value.error)
    }

    @Test
    fun lifecycleCancellationClearsLoadingAndAllowsRetryOnResume() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        var requests = 0
        val repository = SidebarWeatherRepository(fetch = {
            if (++requests == 1) { entered.complete(Unit); gate.await() }
            reading()
        }, now = { 1_000L })
        val job = launch { repository.refresh(config) }
        entered.await()
        assertTrue(repository.state.value.loading)
        job.cancelAndJoin()
        assertFalse(repository.state.value.loading)
        repository.refresh(config)
        assertEquals(2, requests)
        assertEquals(reading(), repository.state.value.reading)
    }

    @Test
    fun clockChangesDoNotLeaveRefreshPermanentlySuppressed() = runBlocking {
        var time = 100_000L
        var requests = 0
        val repository = SidebarWeatherRepository(fetch = { requests++; reading(time) }, now = { time })
        repository.refresh(config)
        time = 1_000L
        repository.refresh(config)
        assertEquals(2, requests)
    }
}
