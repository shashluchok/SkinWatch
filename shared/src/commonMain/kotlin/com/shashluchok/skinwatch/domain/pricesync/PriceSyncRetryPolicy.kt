package com.shashluchok.skinwatch.domain.pricesync

import com.shashluchok.skinwatch.domain.steam.SteamMarketError
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * How long an item is left alone after failing, by how many times it has failed in a row.
 *
 * The first failure costs nothing: whatever went wrong is far more likely to be a moment than a
 * property of the item, and the cheapest way to find out is to ask again. Only an item that keeps
 * failing earns a long wait.
 *
 * Escalating on repetition rather than deciding from a single attempt is what keeps a misread error
 * survivable -- a run that failed everything because the device had no working DNS costs one extra
 * attempt per item, not a day of the whole inventory sitting frozen.
 */
private val RETRY_BACKOFF_BY_FAILURE_COUNT = listOf(
    Duration.ZERO,
    1.minutes,
    1.hours,
    6.hours,
    24.hours,
)

/**
 * How many steps along [RETRY_BACKOFF_BY_FAILURE_COUNT] an answer Steam gave but we could not use
 * starts at. It is the one failure that repeating is least likely to fix, so it earns a longer wait
 * sooner -- but it still travels the same curve, and still gets there by failing, not by verdict.
 */
private const val UNUSABLE_ANSWER_HEAD_START = 2

/**
 * Whether another attempt could plausibly produce a price.
 *
 * [SteamMarketError.InvalidResponse] is the one that most likely cannot: Steam answered, and the
 * answer is unusable for this item. Everything else is a condition of the moment -- and anything
 * unrecognised lands in [SteamMarketError.Unknown], which is deliberately on this side of the line:
 * failing towards trying again is the safe direction to be wrong in.
 */
internal val SteamMarketError.isRetryable: Boolean
    get() = when (this) {
        SteamMarketError.Network,
        SteamMarketError.RateLimited,
        is SteamMarketError.Unknown,
        -> true

        SteamMarketError.InvalidResponse -> false
    }

/** How long this failure asks to be left alone before the next attempt. */
internal val ItemSyncStatus.Failed.retryDelay: Duration
    get() = RETRY_BACKOFF_BY_FAILURE_COUNT[backoffStep]

/**
 * An item that has failed its way to the end of the curve. It no longer holds a run back from
 * counting as finished -- waiting on it would mean the last-completed timestamp never advancing
 * again -- but it is still retried, just rarely.
 */
internal val ItemSyncStatus.Failed.isWrittenOff: Boolean
    get() = backoffStep == RETRY_BACKOFF_BY_FAILURE_COUNT.lastIndex

/**
 * Whether this item is worth a request right now.
 *
 * A stored price that is still fresh always wins, even over a later failure -- a failed attempt must
 * not make the sync re-request a price it already has. Only once the price is stale (or was never
 * obtained) does the last failure decide, through its own backoff.
 *
 * [ignoreFailureBackoff] is what the manual "sync now" action passes: someone looking at the screen
 * and asking for it is a better reason to spend a request than any backoff is to withhold one.
 */
internal fun ItemSyncStatus?.isDueAt(now: Instant, ignoreFailureBackoff: Boolean = false): Boolean {
    val lastSuccessAt = this?.lastSuccessAt
    val priceIsStillFresh = lastSuccessAt != null && now - lastSuccessAt < PRICE_SYNC_INTERVAL
    val failure = this as? ItemSyncStatus.Failed

    return when {
        priceIsStillFresh -> false
        failure == null || ignoreFailureBackoff -> true
        else -> now - failure.attemptedAt >= failure.retryDelay
    }
}

private val ItemSyncStatus.Failed.backoffStep: Int
    get() {
        val headStart = if (error.isRetryable) 0 else UNUSABLE_ANSWER_HEAD_START

        return (consecutiveFailures - 1 + headStart)
            .coerceIn(minimumValue = 0, maximumValue = RETRY_BACKOFF_BY_FAILURE_COUNT.lastIndex)
    }
