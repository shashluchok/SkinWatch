package com.shashluchok.skinwatch.domain.synclog

import kotlin.time.Instant

/**
 * One line of the price-sync diagnostic trail.
 *
 * [message] is already-formatted prose rather than structured fields: the log exists to be read by a
 * human after the fact, never queried, so every call site is free to record whatever it knows.
 */
internal data class SyncLogEntry(
    val at: Instant,
    val level: SyncLogLevel,
    val tag: SyncLogTag,
    val message: String,
    val marketHashName: String? = null,
    /**
     * Filled in by the platform implementation rather than the call site -- what connectivity looked
     * like at the moment of the entry is exactly the context a "did not fetch after the network came
     * back" report is missing, and no domain call site can observe it.
     */
    val networkState: String? = null,
)

internal enum class SyncLogLevel { INFO, WARN, ERROR }

/** Which stage of the price-sync path produced an entry -- the log screen filters on these. */
internal enum class SyncLogTag {
    /** Process lifetime boundaries, so entries from separate app launches stay distinguishable. */
    SESSION,

    /** An entry point asking for a run, and whether it was let through. */
    TRIGGER,

    /** Progress of a single run from start to outcome. */
    RUN,

    /** Per-item staleness decisions -- who was picked up for this run and who was left alone. */
    DUE,

    /** The result of one item's price fetch, as the sync sees it. */
    ITEM,

    /** The Steam request underneath an item fetch: timings, status, and the mapped error. */
    HTTP,

    /** Time spent waiting for the shared Steam request throttle. */
    LIMITER,

    /** The add-item flow's own price fetch, which bypasses the sync run entirely. */
    ADD,

    /** Background worker execution, including its retry attempt count. */
    WORKER,

    /** Work being enqueued, and under which policy. */
    SCHEDULE,
}
