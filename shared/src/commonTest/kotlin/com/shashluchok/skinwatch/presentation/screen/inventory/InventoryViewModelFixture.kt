package com.shashluchok.skinwatch.presentation.screen.inventory

import com.shashluchok.skinwatch.domain.inventory.FakeInventoryRepository
import com.shashluchok.skinwatch.domain.inventory.ObserveInventoryListInteractor
import com.shashluchok.skinwatch.domain.inventory.RemoveInventoryItemInteractor
import com.shashluchok.skinwatch.domain.inventory.UpdateInventoryItemInteractor
import com.shashluchok.skinwatch.domain.pricesnapshot.FakePriceSnapshotRepository
import com.shashluchok.skinwatch.domain.pricesnapshot.ObservePriceHistoryInteractor
import com.shashluchok.skinwatch.domain.settings.FakeSettingsRepository
import com.shashluchok.skinwatch.domain.steam.FakeSteamMarketRepository
import com.shashluchok.skinwatch.domain.steam.ResolveDisplayCurrencyInteractor

/**
 * Wires the real interactors the [InventoryViewModel] depends on over this project's existing
 * fake repositories -- interactors here are plain orchestration with no branching worth doubling
 * on their own, so the fakes the tests actually configure and assert against stay at the
 * repository boundary, same as before this screen's business logic moved out of the ViewModel.
 */
internal class InventoryViewModelFixture {
    val inventoryRepository = FakeInventoryRepository()
    val priceSnapshotRepository = FakePriceSnapshotRepository()
    val steamMarketRepository = FakeSteamMarketRepository()
    val settingsRepository = FakeSettingsRepository()

    private val resolveDisplayCurrency = ResolveDisplayCurrencyInteractor(
        settingsRepository = settingsRepository,
        steamMarketRepository = steamMarketRepository,
    )

    fun newViewModel() = InventoryViewModel(
        observeInventoryList = ObserveInventoryListInteractor(
            inventoryRepository = inventoryRepository,
            priceSnapshotRepository = priceSnapshotRepository,
        ),
        updateInventoryItem = UpdateInventoryItemInteractor(
            inventoryRepository = inventoryRepository,
            resolveDisplayCurrency = resolveDisplayCurrency,
        ),
        removeInventoryItem = RemoveInventoryItemInteractor(inventoryRepository = inventoryRepository),
        observePriceHistory = ObservePriceHistoryInteractor(priceSnapshotRepository = priceSnapshotRepository),
    )
}
