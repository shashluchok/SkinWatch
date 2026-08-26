package com.shashluchok.skinwatch.data.storage.debug

import com.shashluchok.skinwatch.domain.debug.DebugSettingsRepository
import com.shashluchok.skinwatch.domain.debug.FakeDebugSettingsRepository
import com.shashluchok.skinwatch.domain.inventory.FakeInventoryRepository
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InventoryRepositoryMediatorTest {
    @Test
    fun `observeItems delegates to the real repository while mock data is disabled`() = runTest {
        val realRepository = FakeInventoryRepository()
        realRepository.addItem(
            marketHashName = "AK-47 | Redline",
            iconUrl = "icon",
            quantity = 1,
            purchasePrice = Money(minorUnits = 10_00, currency = SteamCurrency.USD),
        )
        val mediator = InventoryRepositoryMediator(
            realRepository = realRepository,
            debugRepository = FakeInventoryRepository(),
            debugSettingsRepository = FakeDebugSettingsRepository(initialMockDataEnabled = false),
        )

        val items = mediator.observeItems().first()

        assertEquals(1, items.size)
        assertEquals("AK-47 | Redline", items.first().marketHashName)
    }

    @Test
    fun `observeItems delegates to the debug repository while mock data is enabled`() = runTest {
        val debugRepository = FakeInventoryRepository()
        debugRepository.addItem(
            marketHashName = "DEBUG │ Something",
            iconUrl = "icon",
            quantity = 1,
            purchasePrice = Money(minorUnits = 10_00, currency = SteamCurrency.USD),
        )
        val mediator = InventoryRepositoryMediator(
            realRepository = FakeInventoryRepository(),
            debugRepository = debugRepository,
            debugSettingsRepository = FakeDebugSettingsRepository(initialMockDataEnabled = true),
        )

        val items = mediator.observeItems().first()

        assertEquals(1, items.size)
        assertEquals("DEBUG │ Something", items.first().marketHashName)
    }

    @Test
    fun `addItem after toggling mock data mid-session goes to the newly active repository`() = runTest {
        val realRepository = FakeInventoryRepository()
        val debugRepository = FakeInventoryRepository()
        val debugSettingsRepository = FakeDebugSettingsRepository(initialMockDataEnabled = false)
        val mediator = InventoryRepositoryMediator(
            realRepository = realRepository,
            debugRepository = debugRepository,
            debugSettingsRepository = debugSettingsRepository,
        )
        mediator.addItem(
            marketHashName = "Real item",
            iconUrl = "icon",
            quantity = 1,
            purchasePrice = Money(minorUnits = 10_00, currency = SteamCurrency.USD),
        )

        debugSettingsRepository.update(
            DebugSettingsRepository.DebugSettings(mockDataEnabled = true),
        )
        mediator.addItem(
            marketHashName = "Debug item",
            iconUrl = "icon",
            quantity = 1,
            purchasePrice = Money(minorUnits = 5_00, currency = SteamCurrency.USD),
        )

        assertTrue(realRepository.observeItems().first().any { it.marketHashName == "Real item" })
        assertTrue(debugRepository.observeItems().first().any { it.marketHashName == "Debug item" })
    }
}
