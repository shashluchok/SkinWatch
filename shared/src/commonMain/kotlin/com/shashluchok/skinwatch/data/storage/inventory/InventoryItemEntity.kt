package com.shashluchok.skinwatch.data.storage.inventory

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlin.time.Instant

@Entity(tableName = "InventoryItem")
internal data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val marketHashName: String,
    val iconUrl: String,
    val addedAt: Instant,
    val quantity: Int,
    val purchasePriceMinorUnits: Long?,
    val purchasePriceCurrencyId: Int?,
    val note: String?,
)
