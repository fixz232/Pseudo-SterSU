package me.weishu.kernelsu.ui.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class SidebarWeatherState(
    val key: String = "",
    val reading: SidebarWeatherReading? = null,
    val loading: Boolean = false,
    val error: SidebarWeatherError? = null,
    val httpStatus: Int? = null,
    val attemptedAt: Long? = null,
)

/** Shared between the live sidebar, its preview and details: one request, not one per UI. */
internal class SidebarWeatherRepository(
    private val fetch: suspend (SidebarWeatherConfig) -> SidebarWeatherReading,
    private val now: () -> Long = System::currentTimeMillis,
    private val loadCache: (String) -> SidebarWeatherReading? = { null },
    private val saveCache: (String, SidebarWeatherReading) -> Unit = { _, _ -> },
) {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(SidebarWeatherState())
    val state = mutableState.asStateFlow()

    suspend fun accept(config: SidebarWeatherConfig, reading: SidebarWeatherReading) = mutex.withLock {
        val key = config.cacheKey()
        saveCache(key, reading)
        mutableState.value = SidebarWeatherState(key = key, reading = reading, attemptedAt = now())
    }

    suspend fun refresh(config: SidebarWeatherConfig, force: Boolean = false) = mutex.withLock {
        if (!config.enabled) return@withLock
        val key = config.cacheKey()
        if (mutableState.value.key != key) {
            val cached = loadCache(key)
            mutableState.value = SidebarWeatherState(key = key, reading = cached, attemptedAt = cached?.fetchedAt)
        }
        val previous = mutableState.value
        // Failed requests use the same interval. Recomposition/resume never becomes a retry storm.
        val interval = if (force) 10_000L else config.normalized().refreshMinutes * 60_000L
        if (previous.attemptedAt?.let { now() - it in 0 until interval } == true) return@withLock
        mutableState.value = previous.copy(loading = true, attemptedAt = now())
        try {
            val reading = fetch(config)
            saveCache(key, reading)
            mutableState.value = SidebarWeatherState(key = key, reading = reading, attemptedAt = now())
        } catch (e: CancellationException) {
            mutableState.value = previous
            throw e
        } catch (e: Exception) {
            val failure = e as? SidebarWeatherException
            mutableState.value = mutableState.value.copy(
                loading = false,
                error = failure?.reason ?: SidebarWeatherError.Network,
                httpStatus = failure?.statusCode,
            )
        }
    }
}
