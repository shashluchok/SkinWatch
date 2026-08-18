package com.shashluchok.skinwatch.domain.pricesync

import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/**
 * The only staleness-gated trigger -- platform schedulers and the manual "sync now" action always
 * call [SyncPriceSnapshotsInteractor] unconditionally instead.
 */
internal class SyncPriceSnapshotsIfStaleInteractor(
    private val priceSyncStatusRepository: PriceSyncStatusRepository,
    private val syncPriceSnapshots: SyncPriceSnapshotsInteractor,
) {
    suspend operator fun invoke() {
        val lastCompletedAt = priceSyncStatusRepository.lastCompletedAt.first()
        val isStale = lastCompletedAt == null || Clock.System.now() - lastCompletedAt >= PRICE_SYNC_INTERVAL
        if (isStale) syncPriceSnapshots()
    }
}
