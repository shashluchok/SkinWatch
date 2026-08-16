package com.shashluchok.skinwatch.domain.inventory

internal class RemoveInventoryItemInteractor(
    private val inventoryRepository: InventoryRepository,
) {
    suspend operator fun invoke(id: Long) = inventoryRepository.removeItem(id)
}
