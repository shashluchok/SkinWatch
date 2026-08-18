package com.shashluchok.skinwatch.domain.inventory

import com.shashluchok.skinwatch.domain.steam.Money
import kotlin.time.Instant

internal data class InventoryItem(
    val id: Long,
    val marketHashName: String,
    val iconUrl: String,
    val addedAt: Instant,
    val quantity: Int,
    val purchasePrice: Money,
)
