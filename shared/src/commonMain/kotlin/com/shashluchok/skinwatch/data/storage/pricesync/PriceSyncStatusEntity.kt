package com.shashluchok.skinwatch.data.storage.pricesync

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlin.time.Instant

/** [PriceSyncStatusEntity] is always exactly one row, keyed by this fixed id. */
internal const val PRICE_SYNC_STATUS_ROW_ID = 0

@Entity(tableName = "PriceSyncStatus")
internal data class PriceSyncStatusEntity(
    @PrimaryKey val id: Int = PRICE_SYNC_STATUS_ROW_ID,
    val lastCompletedAt: Instant,
)
