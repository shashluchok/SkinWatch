package com.shashluchok.skinwatch.domain.pricesync

import com.shashluchok.skinwatch.domain.steam.SteamMarketError
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val NOW = Clock.System.now()

class PriceSyncRetryPolicyTest {
    private fun failed(
        failedAgo: Duration,
        consecutiveFailures: Int,
        error: SteamMarketError = SteamMarketError.Network,
        lastSuccessAgo: Duration? = null,
    ) = ItemSyncStatus.Failed(
        attemptedAt = NOW - failedAgo,
        lastSuccessAt = lastSuccessAgo?.let { NOW - it },
        error = error,
        consecutiveFailures = consecutiveFailures,
    )

    @Test
    fun `an item with no status at all is due`() {
        assertTrue(null.isDueAt(NOW))
    }

    @Test
    fun `a price fresher than the sync interval is not due`() {
        assertFalse(ItemSyncStatus.Synced(attemptedAt = NOW - 1.hours).isDueAt(NOW))
    }

    @Test
    fun `a price older than the sync interval is due`() {
        assertTrue(ItemSyncStatus.Synced(attemptedAt = NOW - PRICE_SYNC_INTERVAL - 1.seconds).isDueAt(NOW))
    }

    /** A failed attempt must not make the sync re-request a price it already holds and still trusts. */
    @Test
    fun `a failure on top of a still-fresh price does not make the item due`() {
        assertFalse(failed(failedAgo = Duration.ZERO, consecutiveFailures = 9, lastSuccessAgo = 1.hours).isDueAt(NOW))
    }

    /**
     * The heart of the policy: one bad attempt costs one more attempt, not a day. A network blip
     * misread as an unusable answer has to be survivable, because that misreading is exactly what
     * froze the whole inventory once already.
     */
    @Test
    fun `a first failure is retried on the very next run`() {
        assertTrue(failed(failedAgo = Duration.ZERO, consecutiveFailures = 1).isDueAt(NOW))
    }

    @Test
    fun `the wait grows with each further failure in a row`() {
        assertFalse(failed(failedAgo = 30.seconds, consecutiveFailures = 2).isDueAt(NOW))
        assertTrue(failed(failedAgo = 2.minutes, consecutiveFailures = 2).isDueAt(NOW))

        assertFalse(failed(failedAgo = 30.minutes, consecutiveFailures = 3).isDueAt(NOW))
        assertTrue(failed(failedAgo = 2.hours, consecutiveFailures = 3).isDueAt(NOW))

        assertFalse(failed(failedAgo = 12.hours, consecutiveFailures = 5).isDueAt(NOW))
        assertTrue(failed(failedAgo = 25.hours, consecutiveFailures = 5).isDueAt(NOW))
    }

    @Test
    fun `the wait stops growing once the item is written off`() {
        val far = failed(failedAgo = 25.hours, consecutiveFailures = 40)

        assertTrue(far.isDueAt(NOW))
    }

    /** The category sets where on the curve an item starts, never whether it gets another attempt. */
    @Test
    fun `an unusable answer starts further along the curve than a network failure`() {
        val network = failed(failedAgo = 30.seconds, consecutiveFailures = 1, error = SteamMarketError.Network)
        val unusable = failed(
            failedAgo = 30.seconds,
            consecutiveFailures = 1,
            error = SteamMarketError.InvalidResponse,
        )

        assertTrue(network.isDueAt(NOW))
        assertFalse(unusable.isDueAt(NOW))
    }

    @Test
    fun `an unusable answer is still retried once its longer wait has passed`() {
        assertTrue(
            failed(failedAgo = 2.hours, consecutiveFailures = 1, error = SteamMarketError.InvalidResponse)
                .isDueAt(NOW),
        )
    }

    @Test
    fun `a manual sync ignores the failure wait but still leaves a fresh price alone`() {
        val backedOff = failed(failedAgo = 1.seconds, consecutiveFailures = 40)
        val freshlyPriced = failed(failedAgo = 1.seconds, consecutiveFailures = 40, lastSuccessAgo = 1.hours)

        assertTrue(backedOff.isDueAt(now = NOW, ignoreFailureBackoff = true))
        assertFalse(freshlyPriced.isDueAt(now = NOW, ignoreFailureBackoff = true))
    }

    @Test
    fun `an item is written off only after it has failed its way to the end of the curve`() {
        assertFalse(failed(failedAgo = 1.days, consecutiveFailures = 1).isWrittenOff)
        assertTrue(failed(failedAgo = 1.days, consecutiveFailures = 5).isWrittenOff)
    }

    @Test
    fun `an unusable answer is written off sooner than a network failure`() {
        val failures = 3

        assertFalse(failed(failedAgo = 1.days, consecutiveFailures = failures).isWrittenOff)
        assertTrue(
            failed(failedAgo = 1.days, consecutiveFailures = failures, error = SteamMarketError.InvalidResponse)
                .isWrittenOff,
        )
    }

    @Test
    fun `only an unusable answer counts as beyond retrying`() {
        assertTrue(SteamMarketError.Network.isRetryable)
        assertTrue(SteamMarketError.RateLimited.isRetryable)
        assertTrue(SteamMarketError.Unknown(message = "boom").isRetryable)
        assertFalse(SteamMarketError.InvalidResponse.isRetryable)
    }
}
