package com.shashluchok.skinwatch.presentation.screen.main

import com.shashluchok.skinwatch.domain.catalog.FakeItemCatalogRepository
import com.shashluchok.skinwatch.domain.catalog.SearchCatalogItemsInteractor
import com.shashluchok.skinwatch.domain.inventory.AddInventoryItemInteractor
import com.shashluchok.skinwatch.domain.inventory.FakeInventoryRepository
import com.shashluchok.skinwatch.domain.pricesnapshot.FakePriceSnapshotRepository
import com.shashluchok.skinwatch.domain.settings.FakeSettingsRepository
import com.shashluchok.skinwatch.domain.steam.FakeSteamMarketRepository
import com.shashluchok.skinwatch.domain.steam.ResolveDisplayCurrencyInteractor

/**
 * Wires the real interactors the [MainViewModel] depends on over this project's existing fake
 * repositories -- interactors here are plain orchestration with no branching worth doubling on
 * their own, so the fakes the tests actually configure and assert against stay at the repository
 * boundary. [steamMarketRepository] is still needed for [AddInventoryItemInteractor]'s
 * `getPriceOverview` call on save -- only the search path moved to the catalog.
 */
internal class MainViewModelFixture {
    val inventoryRepository = FakeInventoryRepository()
    val priceSnapshotRepository = FakePriceSnapshotRepository()
    val steamMarketRepository = FakeSteamMarketRepository()
    val settingsRepository = FakeSettingsRepository()
    val catalogRepository = FakeItemCatalogRepository()

    private val resolveDisplayCurrency = ResolveDisplayCurrencyInteractor(
        settingsRepository = settingsRepository,
        steamMarketRepository = steamMarketRepository,
    )

    fun newViewModel() = MainViewModel(
        searchCatalogItems = SearchCatalogItemsInteractor(catalogRepository = catalogRepository),
        addInventoryItem = AddInventoryItemInteractor(
            inventoryRepository = inventoryRepository,
            steamMarketRepository = steamMarketRepository,
            priceSnapshotRepository = priceSnapshotRepository,
            resolveDisplayCurrency = resolveDisplayCurrency,
        ),
    )
}
