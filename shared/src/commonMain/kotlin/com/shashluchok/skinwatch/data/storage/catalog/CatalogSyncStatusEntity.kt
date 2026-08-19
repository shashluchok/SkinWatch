package com.shashluchok.skinwatch.data.storage.catalog

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlin.time.Instant

/** [CatalogSyncStatusEntity] is always exactly one row, keyed by this fixed id. */
internal const val CATALOG_SYNC_STATUS_ROW_ID = 0

@Entity(tableName = "CatalogSyncStatus")
internal data class CatalogSyncStatusEntity(
    @PrimaryKey val id: Int = CATALOG_SYNC_STATUS_ROW_ID,
    val lastCompletedAt: Instant,
)
