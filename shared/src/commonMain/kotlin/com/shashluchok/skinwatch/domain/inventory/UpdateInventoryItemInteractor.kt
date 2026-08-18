package com.shashluchok.skinwatch.domain.inventory

import com.shashluchok.skinwatch.domain.steam.Money
import kotlin.math.roundToLong

internal class UpdateInventoryItemInteractor(
    private val inventoryRepository: InventoryRepository,
) {
    suspend operator fun invoke(
        item: InventoryItem,
        quantity: Int,
        purchasePriceAmount: Double,
    ) {
        val purchasePrice = Money(
            minorUnits = (purchasePriceAmount * MINOR_UNITS_PER_MAJOR_UNIT).roundToLong(),
            currency = item.purchasePrice.currency,
        )
        inventoryRepository.updateItem(item.copy(quantity = quantity, purchasePrice = purchasePrice))
    }

    private companion object {
        const val MINOR_UNITS_PER_MAJOR_UNIT = 100.0
    }
}
