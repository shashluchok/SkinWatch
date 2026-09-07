package com.shashluchok.skinwatch.domain.pricesync

/** What a [SyncPriceSnapshotsInteractor] run achieved, so callers can decide whether to retry. */
internal enum class PriceSyncOutcome {
    /**
     * Nothing is left that another attempt could fix. Items Steam cannot price do not hold this
     * back: they fail identically every time, so waiting on them would mean the last-completed
     * timestamp never advancing again.
     */
    Completed,

    /** At least one item failed for a reason another attempt could fix. */
    HadFailures,

    /** There was nothing to do -- an empty inventory, or every price already fresh. */
    NothingDue,

    /**
     * Another run held the lock, so this trigger did no work at all. Distinct from [NothingDue]
     * because the caller still has a reason to come back: the run in flight may abort partway.
     */
    AlreadyRunning,
}
