package com.shashluchok.skinwatch.data.steam

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

@OptIn(ExperimentalCoroutinesApi::class)
class SteamRateLimiterTest {
    @Test
    fun `first call for an endpoint does not wait`() = runTest {
        val timeSource = TestTimeSource()
        val limiter = SteamRateLimiter(minInterval = { 8.seconds }, timeSource = timeSource)

        limiter.awaitTurn(SteamEndpoint.PRICE_OVERVIEW)

        assertTrue(testScheduler.currentTime < 1_000L)
    }

    @Test
    fun `second call before the minimum interval waits out the remainder`() = runTest {
        val timeSource = TestTimeSource()
        val limiter = SteamRateLimiter(minInterval = { 8.seconds }, timeSource = timeSource)

        limiter.awaitTurn(SteamEndpoint.PRICE_OVERVIEW)
        timeSource += 3.seconds
        limiter.awaitTurn(SteamEndpoint.PRICE_OVERVIEW)

        // Second call should wait for remaining 5 seconds (8 - 3)
        assertTrue(testScheduler.currentTime >= 5_000L)
    }

    @Test
    fun `call after the minimum interval already elapsed does not wait`() = runTest {
        val timeSource = TestTimeSource()
        val limiter = SteamRateLimiter(minInterval = { 8.seconds }, timeSource = timeSource)

        limiter.awaitTurn(SteamEndpoint.PRICE_OVERVIEW)
        timeSource += 9.seconds
        limiter.awaitTurn(SteamEndpoint.PRICE_OVERVIEW)

        // No additional delay should occur; if delay was incorrectly applied it would push currentTime well past 1000ms
        assertTrue(testScheduler.currentTime < 1_000L)
    }
}
