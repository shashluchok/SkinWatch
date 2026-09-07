package com.shashluchok.skinwatch.domain.synclog

import com.shashluchok.skinwatch.domain.inventory.InventoryRepository
import com.shashluchok.skinwatch.domain.pricesync.ItemSyncStatus
import com.shashluchok.skinwatch.domain.pricesync.ItemSyncStatusRepository
import com.shashluchok.skinwatch.domain.pricesync.PRICE_SYNC_INTERVAL
import com.shashluchok.skinwatch.domain.pricesync.PriceSyncStatusRepository
import com.shashluchok.skinwatch.domain.pricesync.isDueAt
import com.shashluchok.skinwatch.domain.pricesync.isWrittenOff
import com.shashluchok.skinwatch.domain.pricesync.retryDelay
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Dumps the state the sync decides on -- who is tracked, who last succeeded when, who last failed
 * with what -- into the log on demand.
 *
 * The per-run entries only say what happened while someone was watching; this says what the sync
 * would do if it ran right now, which is what an intermittent "this one item never updates" report
 * actually needs.
 */
internal class InspectSyncStateInteractor(
    private val inventoryRepository: InventoryRepository,
    private val itemSyncStatusRepository: ItemSyncStatusRepository,
    private val priceSyncStatusRepository: PriceSyncStatusRepository,
    private val platformInspector: PlatformSyncStateInspector,
    private val syncLog: SyncLogRepository,
) {
    suspend operator fun invoke() {
        val now = Clock.System.now()
        val lastCompletedAt = priceSyncStatusRepository.lastCompletedAt.first()
        syncLog.info(
            tag = SyncLogTag.SESSION,
            message = "--- state snapshot --- lastCompletedAt=" +
                (lastCompletedAt?.let { "$it (${now - it} ago)" } ?: "never") +
                ", interval=$PRICE_SYNC_INTERVAL",
        )

        val marketHashNames = inventoryRepository.getDistinctMarketHashNames()
        val statuses = itemSyncStatusRepository.getAll()
        marketHashNames.forEach { marketHashName ->
            syncLog.info(
                tag = SyncLogTag.SESSION,
                message = statuses[marketHashName].describe(now),
                marketHashName = marketHashName,
            )
        }

        val orphaned = statuses.keys - marketHashNames.toSet()
        if (orphaned.isNotEmpty()) {
            syncLog.warn(
                tag = SyncLogTag.SESSION,
                message = "${orphaned.size} status row(s) with no matching inventory item: " +
                    orphaned.joinToString(),
            )
        }

        platformInspector.inspect()
    }
}

/**
 * Every line ends with the answer [isDueAt] would give, asked rather than restated: this description
 * once carried its own copy of the rule, drifted from it, and reported items as due that the sync
 * was in fact leaving alone.
 */
private fun ItemSyncStatus?.describe(now: Instant): String {
    val detail = when (this) {
        null -> "no status row at all"
        is ItemSyncStatus.Synced -> "last attempt succeeded at $attemptedAt (${now - attemptedAt} ago)"
        is ItemSyncStatus.Failed ->
            "last attempt failed at $attemptedAt (${now - attemptedAt} ago) with $error, " +
                "$consecutiveFailures in a row, retryAfter=$retryDelay, writtenOff=$isWrittenOff, " +
                "lastSuccessAt=" + (lastSuccessAt?.let { "$it (${now - it} ago)" } ?: "never")
    }

    return "$detail, due=${isDueAt(now)}"
}
