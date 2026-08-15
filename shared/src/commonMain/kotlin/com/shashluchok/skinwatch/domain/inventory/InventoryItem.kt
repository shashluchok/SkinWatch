package com.shashluchok.skinwatch.domain.inventory

import com.shashluchok.skinwatch.domain.steam.Money
import kotlin.time.Instant

/**
 * A row the user owns. `marketHashName` is deliberately not unique across rows -- the same skin
 * bought twice at different prices is two rows, not one row with `quantity = 2` and a lost price.
 */
internal data class InventoryItem(
    val id: Long,
    val marketHashName: String,
    val addedAt: Instant,
    val quantity: Int,
    val purchasePrice: Money?,
    val note: String?,
)
