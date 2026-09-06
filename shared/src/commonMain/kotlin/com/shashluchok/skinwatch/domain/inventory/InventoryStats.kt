package com.shashluchok.skinwatch.domain.inventory

import com.shashluchok.skinwatch.domain.steam.Money

/**
 * Totals across the whole inventory.
 *
 * Every stored price is held in a single currency -- changing the display currency rewrites all
 * rows in one transaction (see `CurrencyConversionRepository.convertAll`) -- so the amounts here can
 * be summed directly, without per-item conversion.
 *
 */
internal data class InventoryStats(
    val currentValue: Money,
    val spent: Money,
    val delta: Money,
    val deltaFraction: Float?,
) {
    companion object {
        fun from(items: List<InventoryListItem>): InventoryStats? {
            val currency = items
                .firstOrNull()
                ?.item
                ?.purchasePrice
                ?.currency ?: return null

            var currentMinorUnits = 0L
            var spentMinorUnits = 0L

            items.forEach { listItem ->
                val quantity = listItem.item.quantity
                val purchase = listItem.item.purchasePrice.minorUnits
                val latest = listItem.latestSnapshot?.lowestPrice?.minorUnits
                currentMinorUnits += (latest ?: purchase) * quantity
                spentMinorUnits += purchase * quantity
            }

            val deltaMinorUnits = currentMinorUnits - spentMinorUnits

            return InventoryStats(
                currentValue = Money(minorUnits = currentMinorUnits, currency = currency),
                spent = Money(minorUnits = spentMinorUnits, currency = currency),
                delta = Money(minorUnits = deltaMinorUnits, currency = currency),
                deltaFraction = if (spentMinorUnits == 0L) {
                    null
                } else {
                    deltaMinorUnits.toFloat() / spentMinorUnits
                },
            )
        }
    }
}
