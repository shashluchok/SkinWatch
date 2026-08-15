package com.shashluchok.skinwatch.data.storage.pricesnapshot

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import kotlin.time.Instant

@Entity(tableName = "PriceSnapshot", indices = [Index(value = ["marketHashName", "capturedAt"])])
internal data class PriceSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val marketHashName: String,
    val currencyId: Int,
    val lowestPriceMinorUnits: Long?,
    val medianPriceMinorUnits: Long?,
    val volume: Int?,
    val capturedAt: Instant,
)
