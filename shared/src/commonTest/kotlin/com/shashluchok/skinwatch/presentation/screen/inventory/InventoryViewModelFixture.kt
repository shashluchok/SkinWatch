package com.shashluchok.skinwatch.presentation.screen.inventory

import com.shashluchok.skinwatch.domain.inventory.FakeInventoryRepository
import com.shashluchok.skinwatch.domain.inventory.ObserveInventoryListInteractor
import com.shashluchok.skinwatch.domain.inventory.RemoveInventoryItemInteractor
import com.shashluchok.skinwatch.domain.inventory.UpdateInventoryItemInteractor
import com.shashluchok.skinwatch.domain.pricesnapshot.FakePriceSnapshotRepository
import com.shashluchok.skinwatch.domain.pricesync.FakePriceSyncStatusRepository
import com.shashluchok.skinwatch.domain.pricesync.ObserveLastSyncedAtInteractor
import com.shashluchok.skinwatch.domain.pricesync.SyncPriceSnapshotsInteractor
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
    val priceSyncStatusRepository = FakePriceSyncStatusRepository()

    private val resolveDisplayCurrency = ResolveDisplayCurrencyInteractor(
        settingsRepository = settingsRepository,
        steamMarketRepository = steamMarketRepository,
    )

    private val syncPriceSnapshots = SyncPriceSnapshotsInteractor(
        inventoryRepository = inventoryRepository,
        steamMarketRepository = steamMarketRepository,
        priceSnapshotRepository = priceSnapshotRepository,
        resolveDisplayCurrency = resolveDisplayCurrency,
        priceSyncStatusRepository = priceSyncStatusRepository,
    )

    fun newViewModel() = InventoryViewModel(
        observeInventoryList = ObserveInventoryListInteractor(
            inventoryRepository = inventoryRepository,
            priceSnapshotRepository = priceSnapshotRepository,
        ),
        updateInventoryItem = UpdateInventoryItemInteractor(inventoryRepository = inventoryRepository),
        removeInventoryItem = RemoveInventoryItemInteractor(inventoryRepository = inventoryRepository),
        syncPriceSnapshots = syncPriceSnapshots,
        observeLastSyncedAt = ObserveLastSyncedAtInteractor(priceSyncStatusRepository = priceSyncStatusRepository),
    )
}
