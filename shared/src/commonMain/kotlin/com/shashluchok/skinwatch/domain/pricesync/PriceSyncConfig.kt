package com.shashluchok.skinwatch.domain.pricesync

import kotlin.time.Duration.Companion.hours

/**
 * Single source of truth for the sync interval -- referenced by the staleness check
 * ([SyncPriceSnapshotsIfStaleInteractor]) and every platform scheduler. Fixed for this milestone;
 * a future milestone can make this user-configurable by widening this one seam instead of touching
 * every call site.
 *
 * Not `internal` -- platform-scheduler code outside the `shared` module (Android's
 * `schedulePeriodicPriceSync`, the desktop timer) needs it too.
 */
val PRICE_SYNC_INTERVAL = 6.hours
