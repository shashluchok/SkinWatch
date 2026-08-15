package com.shashluchok.skinwatch.data.steam

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal enum class SteamEndpoint { SEARCH, PRICE_OVERVIEW }

// Valve does not publish official numbers; 8s is a community-documented safe minimum for
// priceoverview. search/render has no separately documented figure, so the same
// conservative interval is used for it too.
private val MIN_INTERVAL_PER_ENDPOINT = 8.seconds

private fun defaultMinInterval(endpoint: SteamEndpoint): Duration = when (endpoint) {
    SteamEndpoint.SEARCH, SteamEndpoint.PRICE_OVERVIEW -> MIN_INTERVAL_PER_ENDPOINT
}

// Each endpoint gets its own mutex so that a pending delay for one endpoint never blocks
// awaitTurn calls for an unrelated endpoint -- a single shared mutex would serialize independent
// endpoints together, defeating the "per-endpoint" guarantee.
private class EndpointThrottle {
    val mutex = Mutex()
    var lastCallMark: TimeMark? = null
}

/**
 * Guarantees at least [minInterval] elapses between two calls to [awaitTurn] for the same
 * [SteamEndpoint]. This is the client protecting itself from its own request rate, not a UI-level
 * debounce (that stays in M3, where user input lives).
 */
internal class SteamRateLimiter(
    private val minInterval: (SteamEndpoint) -> Duration = ::defaultMinInterval,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private val throttles = SteamEndpoint.entries.associateWith { EndpointThrottle() }

    suspend fun awaitTurn(endpoint: SteamEndpoint) {
        val throttle = throttles.getValue(endpoint)
        throttle.mutex.withLock {
            val last = throttle.lastCallMark
            if (last != null) {
                val remaining = minInterval(endpoint) - last.elapsedNow()
                if (remaining > Duration.ZERO) delay(remaining)
            }
            throttle.lastCallMark = timeSource.markNow()
        }
    }
}
