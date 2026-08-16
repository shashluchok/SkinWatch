package com.shashluchok.skinwatch.domain.inventory

import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.ResolveDisplayCurrencyInteractor
import kotlin.math.roundToLong

internal class UpdateInventoryItemInteractor(
    private val inventoryRepository: InventoryRepository,
    private val resolveDisplayCurrency: ResolveDisplayCurrencyInteractor,
) {
    suspend operator fun invoke(
        item: InventoryItem,
        quantity: Int,
        purchasePriceAmount: Double?,
        note: String?,
    ) {
        // Keep the item's existing purchase-price currency when it already had one -- editing
        // quantity/note should not silently re-denominate an existing purchase price. Only a
        // brand-new (previously null) purchase price picks up the current display currency.
        val currency = item.purchasePrice?.currency ?: resolveDisplayCurrency()
        val purchasePrice = purchasePriceAmount?.let {
            Money(minorUnits = (it * MINOR_UNITS_PER_MAJOR_UNIT).roundToLong(), currency = currency)
        }
        inventoryRepository.updateItem(item.copy(quantity = quantity, purchasePrice = purchasePrice, note = note))
    }

    private companion object {
        const val MINOR_UNITS_PER_MAJOR_UNIT = 100.0
    }
}
