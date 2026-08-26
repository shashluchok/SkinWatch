package com.shashluchok.skinwatch.presentation.navigation

import com.shashluchok.skinwatch.domain.catalog.CatalogSyncScheduler
import com.shashluchok.skinwatch.domain.catalog.FakeCatalogSyncStatusRepository
import com.shashluchok.skinwatch.domain.catalog.FakeItemCatalogRemoteSource
import com.shashluchok.skinwatch.domain.catalog.FakeItemCatalogRepository
import com.shashluchok.skinwatch.domain.catalog.SyncCatalogItemsIfStaleInteractor
import com.shashluchok.skinwatch.domain.catalog.SyncCatalogItemsInteractor
import com.shashluchok.skinwatch.domain.debug.DebugSettingsRepository
import com.shashluchok.skinwatch.domain.debug.FakeDebugSettingsRepository
import com.shashluchok.skinwatch.domain.debug.ObserveDebugSettingsInteractor
import com.shashluchok.skinwatch.domain.inventory.FakeInventoryRepository
import com.shashluchok.skinwatch.domain.pricesnapshot.FakePriceSnapshotRepository
import com.shashluchok.skinwatch.domain.pricesync.FakePriceSyncStatusRepository
import com.shashluchok.skinwatch.domain.pricesync.PriceSyncScheduler
import com.shashluchok.skinwatch.domain.pricesync.SyncPriceSnapshotsIfStaleInteractor
import com.shashluchok.skinwatch.domain.pricesync.SyncPriceSnapshotsInteractor
import com.shashluchok.skinwatch.domain.settings.FakeSettingsRepository
import com.shashluchok.skinwatch.domain.steam.FakeSteamMarketRepository
import com.shashluchok.skinwatch.domain.steam.ResolveDisplayCurrencyInteractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(debugSettingsRepository: FakeDebugSettingsRepository) = AppViewModel(
        priceSyncScheduler = PriceSyncScheduler.EMPTY,
        syncPriceSnapshotsIfStale = SyncPriceSnapshotsIfStaleInteractor(
            priceSyncStatusRepository = FakePriceSyncStatusRepository(),
            syncPriceSnapshots = SyncPriceSnapshotsInteractor(
                inventoryRepository = FakeInventoryRepository(),
                steamMarketRepository = FakeSteamMarketRepository(),
                priceSnapshotRepository = FakePriceSnapshotRepository(),
                resolveDisplayCurrency = ResolveDisplayCurrencyInteractor(
                    settingsRepository = FakeSettingsRepository(),
                    steamMarketRepository = FakeSteamMarketRepository(),
                ),
                priceSyncStatusRepository = FakePriceSyncStatusRepository(),
            ),
        ),
        catalogSyncScheduler = CatalogSyncScheduler.EMPTY,
        syncCatalogItemsIfStale = SyncCatalogItemsIfStaleInteractor(
            catalogSyncStatusRepository = FakeCatalogSyncStatusRepository(),
            syncCatalogItems = SyncCatalogItemsInteractor(
                remoteSource = FakeItemCatalogRemoteSource(),
                catalogRepository = FakeItemCatalogRepository(),
                catalogSyncStatusRepository = FakeCatalogSyncStatusRepository(),
            ),
        ),
        observeDebugSettings = ObserveDebugSettingsInteractor(debugSettingsRepository = debugSettingsRepository),
    )

    @Test
    fun `isSplashVisible defaults to true before the repository value has loaded`() = runTest(dispatcher) {
        val viewModel = newViewModel(FakeDebugSettingsRepository(initialShowSplashScreen = true))

        assertTrue(viewModel.stateFlow.value.isSplashVisible)
    }

    @Test
    fun `isSplashVisible reflects a saved false value once collected`() = runTest(dispatcher) {
        val viewModel = newViewModel(FakeDebugSettingsRepository(initialShowSplashScreen = false))

        dispatcher.scheduler.runCurrent()

        assertFalse(viewModel.stateFlow.value.isSplashVisible)
    }

    @Test
    fun `SplashScreenAnimationFinished hides the splash regardless of the saved setting`() = runTest(dispatcher) {
        val viewModel = newViewModel(FakeDebugSettingsRepository(initialShowSplashScreen = true))
        dispatcher.scheduler.runCurrent()

        viewModel.onAction(AppViewModel.Action.SplashScreenAnimationFinished)

        assertFalse(viewModel.stateFlow.value.isSplashVisible)
    }

    @Test
    fun `changing an unrelated debug setting later does not bring the splash back`() = runTest(dispatcher) {
        val debugSettingsRepository = FakeDebugSettingsRepository(initialShowSplashScreen = true)
        val viewModel = newViewModel(debugSettingsRepository)
        dispatcher.scheduler.runCurrent()
        viewModel.onAction(AppViewModel.Action.SplashScreenAnimationFinished)

        // Re-persisting the same showSplashScreen value simulates flipping the unrelated
        // mockDataEnabled flag, since both fields live on the same DebugSettings entity -- see the
        // `debug` package doc comment on DebugSettingsRepository.
        debugSettingsRepository.update(DebugSettingsRepository.DebugSettings(showSplashScreen = true))
        dispatcher.scheduler.runCurrent()

        assertFalse(viewModel.stateFlow.value.isSplashVisible)
    }
}
