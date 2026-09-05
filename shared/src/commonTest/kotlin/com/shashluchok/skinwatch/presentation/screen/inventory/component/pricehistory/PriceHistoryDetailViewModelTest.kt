package com.shashluchok.skinwatch.presentation.screen.inventory.component.pricehistory

import com.shashluchok.skinwatch.domain.inventory.InventoryItem
import com.shashluchok.skinwatch.domain.pricesnapshot.FakePriceSnapshotRepository
import com.shashluchok.skinwatch.domain.pricesnapshot.ObservePriceHistoryInteractor
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private const val SAMPLE_HASH_NAME = "AWP | Asiimov (Field-Tested)"

private val SAMPLE_ITEM = InventoryItem(
    id = 1L,
    marketHashName = SAMPLE_HASH_NAME,
    iconUrl = "https://example.com/icon.png",
    addedAt = Instant.fromEpochMilliseconds(0),
    quantity = 1,
    purchasePrice = Money(minorUnits = 100, currency = SteamCurrency.USD),
)

@OptIn(ExperimentalCoroutinesApi::class)
class PriceHistoryDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val priceSnapshotRepository = FakePriceSnapshotRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = PriceHistoryDetailViewModel(
        observePriceHistory = ObservePriceHistoryInteractor(priceSnapshotRepository = priceSnapshotRepository),
    )

    @Test
    fun `state starts as Loading before any item is displayed`() = runTest(dispatcher) {
        val viewModel = newViewModel()

        assertEquals(PriceHistoryDetailViewModel.State.Loading, viewModel.stateFlow.value)
    }

    @Test
    fun `OnDisplay loads the price history recorded for the item`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        priceSnapshotRepository.emitSnapshot(
            marketHashName = SAMPLE_HASH_NAME,
            lowestPrice = Money(minorUnits = 5000, currency = SteamCurrency.USD),
            capturedAt = Instant.fromEpochMilliseconds(1_000),
        )

        viewModel.onAction(PriceHistoryDetailViewModel.Action.OnDisplay(SAMPLE_ITEM))
        dispatcher.scheduler.runCurrent()

        val state = viewModel.stateFlow.value
        check(state is PriceHistoryDetailViewModel.State.Content)
        assertEquals(1, state.snapshots.size)
        assertEquals(Money(minorUnits = 5000, currency = SteamCurrency.USD), state.snapshots.single().lowestPrice)
    }

    @Test
    fun `OnDisplay for an item with no snapshots yields empty content rather than staying Loading`() =
        runTest(dispatcher) {
            val viewModel = newViewModel()

            viewModel.onAction(PriceHistoryDetailViewModel.Action.OnDisplay(SAMPLE_ITEM))
            dispatcher.scheduler.runCurrent()

            assertEquals(PriceHistoryDetailViewModel.State.Content(snapshots = emptyList()), viewModel.stateFlow.value)
        }

    @Test
    fun `OnDismiss resets the state so a reopened detail does not show the previous item's history`() =
        runTest(dispatcher) {
            val viewModel = newViewModel()
            priceSnapshotRepository.emitSnapshot(
                marketHashName = SAMPLE_HASH_NAME,
                lowestPrice = Money(minorUnits = 5000, currency = SteamCurrency.USD),
                capturedAt = Instant.fromEpochMilliseconds(1_000),
            )
            viewModel.onAction(PriceHistoryDetailViewModel.Action.OnDisplay(SAMPLE_ITEM))
            dispatcher.scheduler.runCurrent()

            viewModel.onAction(PriceHistoryDetailViewModel.Action.OnDismiss)
            dispatcher.scheduler.runCurrent()

            assertEquals(PriceHistoryDetailViewModel.State.Loading, viewModel.stateFlow.value)
        }
}
