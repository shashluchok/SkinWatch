package com.shashluchok.skinwatch.domain.inventory

import com.shashluchok.skinwatch.domain.pricesnapshot.FakePriceSnapshotRepository
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class ObserveInventoryListInteractorTest {
    private val inventoryRepository = FakeInventoryRepository()
    private val priceSnapshotRepository = FakePriceSnapshotRepository()
    private val interactor = ObserveInventoryListInteractor(
        inventoryRepository = inventoryRepository,
        priceSnapshotRepository = priceSnapshotRepository,
    )

    @Test
    fun `reflects an added inventory item with no snapshot yet`() = runTest {
        inventoryRepository.addItem(
            marketHashName = "AK-47 | Redline (Field-Tested)",
            iconUrl = "https://example.com/icon.png",
            quantity = 2,
            purchasePrice = Money(minorUnits = 1234, currency = SteamCurrency.USD),
        )

        val listItem = interactor().first().single()

        assertEquals("AK-47 | Redline (Field-Tested)", listItem.item.marketHashName)
        assertNull(listItem.latestSnapshot)
    }

    @Test
    fun `exposes the latest snapshot by capturedAt for its item`() = runTest {
        val hashName = "AWP | Asiimov (Field-Tested)"
        inventoryRepository.addItem(
            marketHashName = hashName,
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )
        priceSnapshotRepository.emitSnapshot(
            marketHashName = hashName,
            lowestPrice = Money(minorUnits = 5000, currency = SteamCurrency.USD),
            capturedAt = Instant.fromEpochMilliseconds(1_000),
        )
        priceSnapshotRepository.emitSnapshot(
            marketHashName = hashName,
            lowestPrice = Money(minorUnits = 5500, currency = SteamCurrency.USD),
            capturedAt = Instant.fromEpochMilliseconds(2_000),
        )

        val latest = interactor().first().single().latestSnapshot

        assertEquals(Money(minorUnits = 5500, currency = SteamCurrency.USD), latest?.lowestPrice)
    }

    @Test
    fun `rows are built without subscribing to any item's snapshot history`() = runTest {
        val hashName = "P250 | Sand Dune"
        inventoryRepository.addItem(
            marketHashName = hashName,
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )
        inventoryRepository.addItem(
            marketHashName = hashName,
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
        )

        val items = interactor().first()

        assertEquals(2, items.size)
        // The latest reading per item arrives from one query, so no per-item history is subscribed
        // to -- previously two rows of the same item opened a flow each and reduced whole histories.
        assertEquals(emptyMap(), priceSnapshotRepository.observeCallCounts)
    }
}
