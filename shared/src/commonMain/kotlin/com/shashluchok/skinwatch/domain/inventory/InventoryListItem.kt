package com.shashluchok.skinwatch.domain.inventory

import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshot

/** An owned [InventoryItem] paired with the most recently captured price reading for it, if any. */
internal data class InventoryListItem(
    val item: InventoryItem,
    val latestSnapshot: PriceSnapshot?,
)
