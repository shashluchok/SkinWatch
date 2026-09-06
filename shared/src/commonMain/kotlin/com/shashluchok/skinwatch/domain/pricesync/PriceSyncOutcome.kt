package com.shashluchok.skinwatch.domain.pricesync

/** What a [SyncPriceSnapshotsInteractor] run achieved, so callers can decide whether to retry. */
internal enum class PriceSyncOutcome {
    /** Every item was fetched. Only this outcome may advance the last-completed timestamp. */
    Completed,

    /** At least one item failed -- worth another attempt once conditions change. */
    HadFailures,

    /** Nothing was attempted: an empty inventory, or another run already holding the lock. */
    Skipped,
}
