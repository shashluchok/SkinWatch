package com.shashluchok.skinwatch.domain.inventory

import com.shashluchok.skinwatch.domain.pricesync.FakeItemSyncStatusRepository
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

private const val HASH_NAME = "AK-47 | Redline (Field-Tested)"

class RemoveInventoryItemInteractorTest {
    private val inventoryRepository = FakeInventoryRepository()
    private val itemSyncStatusRepository = FakeItemSyncStatusRepository()
    private val interactor = RemoveInventoryItemInteractor(
        inventoryRepository = inventoryRepository,
        itemSyncStatusRepository = itemSyncStatusRepository,
    )

    private suspend fun addItem(marketHashName: String = HASH_NAME): InventoryItem {
        inventoryRepository.addItem(
            marketHashName = marketHashName,
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )

        return inventoryRepository
            .observeItems()
            .first()
            .last { it.marketHashName == marketHashName }
    }

    @Test
    fun `removes the item with the given id from the repository`() = runTest {
        val item = addItem()

        interactor(item)

        assertEquals(listOf(item.id), inventoryRepository.removedIds)
    }

    @Test
    fun `the sync status goes with the last item holding that hash`() = runTest {
        val item = addItem()
        itemSyncStatusRepository.markSynced(marketHashName = HASH_NAME, at = Clock.System.now())

        interactor(item)

        assertTrue(itemSyncStatusRepository.statuses.isEmpty())
    }

    /** Duplicates of one skin share a single status, so it has to outlive all but the last of them. */
    @Test
    fun `the sync status stays while another item still holds that hash`() = runTest {
        val first = addItem()
        addItem()
        itemSyncStatusRepository.markSynced(marketHashName = HASH_NAME, at = Clock.System.now())

        interactor(first)

        assertTrue(HASH_NAME in itemSyncStatusRepository.statuses)
    }

    @Test
    fun `removing one item leaves another skin's status alone`() = runTest {
        val item = addItem()
        val otherHashName = "AWP | Asiimov (Field-Tested)"
        addItem(otherHashName)
        itemSyncStatusRepository.markSynced(marketHashName = otherHashName, at = Clock.System.now())

        interactor(item)

        assertTrue(otherHashName in itemSyncStatusRepository.statuses)
    }
}
