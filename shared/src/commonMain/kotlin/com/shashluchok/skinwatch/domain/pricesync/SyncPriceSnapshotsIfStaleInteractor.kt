package com.shashluchok.skinwatch.domain.pricesync

import com.shashluchok.skinwatch.domain.synclog.SyncLogRepository
import com.shashluchok.skinwatch.domain.synclog.SyncLogTag
import com.shashluchok.skinwatch.domain.synclog.info
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/**
 * The only staleness-gated trigger -- platform schedulers and the manual "sync now" action always
 * call [SyncPriceSnapshotsInteractor] unconditionally instead.
 */
internal class SyncPriceSnapshotsIfStaleInteractor(
    private val priceSyncStatusRepository: PriceSyncStatusRepository,
    private val syncPriceSnapshots: SyncPriceSnapshotsInteractor,
    private val syncLog: SyncLogRepository = SyncLogRepository.EMPTY,
) {
    suspend operator fun invoke() {
        val lastCompletedAt = priceSyncStatusRepository.lastCompletedAt.first()
        val now = Clock.System.now()
        val isStale = lastCompletedAt == null || now - lastCompletedAt >= PRICE_SYNC_INTERVAL
        syncLog.info(
            tag = SyncLogTag.TRIGGER,
            message = "staleness check: lastCompletedAt=" +
                (lastCompletedAt?.let { "$it (${now - it} ago)" } ?: "never") +
                ", interval=$PRICE_SYNC_INTERVAL, ${if (isStale) "syncing" else "leaving it alone"}",
        )
        if (isStale) syncPriceSnapshots(trigger = SyncTrigger.APP_OPEN_STALE_CHECK)
    }
}
