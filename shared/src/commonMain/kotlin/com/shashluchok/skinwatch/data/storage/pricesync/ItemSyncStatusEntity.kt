package com.shashluchok.skinwatch.data.storage.pricesync

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlin.time.Instant

/**
 * One row per `marketHashName`. [lastErrorType] is null exactly when the last attempt succeeded --
 * [lastSuccessAt] cannot carry that distinction, since it deliberately keeps its value across later
 * failures.
 */
@Entity(tableName = "ItemSyncStatus")
internal data class ItemSyncStatusEntity(
    @PrimaryKey val marketHashName: String,
    val lastAttemptAt: Instant,
    val lastSuccessAt: Instant?,
    val lastErrorType: String?,
    val lastErrorMessage: String?,
    /**
     * Reset to zero by every success; see `PriceSyncRetryPolicy` for what it is spent on.
     *
     * The SQL default is declared rather than left to Kotlin's: `ADD COLUMN ... NOT NULL` has to
     * carry one, and Room validates the migrated table against this schema -- a default here and
     * none there is exactly the mismatch that makes it reject the database it just migrated.
     */
    @ColumnInfo(defaultValue = "0")
    val consecutiveFailures: Int = 0,
)
