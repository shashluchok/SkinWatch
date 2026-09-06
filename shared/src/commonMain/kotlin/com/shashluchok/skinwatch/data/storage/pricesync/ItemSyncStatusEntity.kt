package com.shashluchok.skinwatch.data.storage.pricesync

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
)
