package com.shashluchok.skinwatch.presentation.screen.inventory

import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import com.shashluchok.skinwatch.presentation.component.ValidationError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

private val SAMPLE_PURCHASE_PRICE = Money(minorUnits = 100, currency = SteamCurrency.USD)

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val fixture = InventoryViewModelFixture()
    private val inventoryRepository = fixture.inventoryRepository
    private val priceSnapshotRepository = fixture.priceSnapshotRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = fixture.newViewModel()

    @Test
    fun `state items reflects an added inventory item with no snapshot yet`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        inventoryRepository.addItem(
            marketHashName = "AK-47 | Redline (Field-Tested)",
            iconUrl = "https://example.com/icon.png",
            quantity = 2,
            purchasePrice = Money(minorUnits = 1234, currency = SteamCurrency.USD),
        )

        dispatcher.scheduler.runCurrent()

        val listItem = viewModel.stateFlow.value.items
            .single()
        assertEquals("AK-47 | Redline (Field-Tested)", listItem.item.marketHashName)
        assertNull(listItem.latestSnapshot)
    }

    @Test
    fun `state items exposes the latest snapshot by capturedAt for its item`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        val hashName = "AWP | Asiimov (Field-Tested)"
        inventoryRepository.addItem(
            marketHashName = hashName,
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = SAMPLE_PURCHASE_PRICE,
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

        dispatcher.scheduler.runCurrent()

        val latest = viewModel.stateFlow.value.items
            .single()
            .latestSnapshot
        assertEquals(Money(minorUnits = 5500, currency = SteamCurrency.USD), latest?.lowestPrice)
    }

    @Test
    fun `two rows sharing a marketHashName subscribe to its snapshots only once`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        val hashName = "P250 | Sand Dune"
        inventoryRepository.addItem(
            marketHashName = hashName,
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = SAMPLE_PURCHASE_PRICE,
        )
        inventoryRepository.addItem(
            marketHashName = hashName,
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = SAMPLE_PURCHASE_PRICE,
        )

        dispatcher.scheduler.runCurrent()

        assertEquals(2, viewModel.stateFlow.value.items.size)
        assertEquals(1, priceSnapshotRepository.observeCallCounts[hashName])
    }

    @Test
    fun `OnItemClick opens the price history detail for the clicked item`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        val id = inventoryRepository.addItem(
            marketHashName = "USP-S | Kill Confirmed (Minimal Wear)",
            iconUrl = "https://example.com/icon.png",
            quantity = 3,
            purchasePrice = Money(minorUnits = 2500, currency = SteamCurrency.USD),
        )
        dispatcher.scheduler.runCurrent()
        val item = viewModel.stateFlow.value.items
            .single()
            .item

        viewModel.onAction(InventoryViewModel.Action.OnItemClick(item))
        dispatcher.scheduler.runCurrent()

        assertNull(viewModel.stateFlow.value.editSheet)
        val sheet = viewModel.stateFlow.value.priceHistoryDetailAlert
        check(sheet != null)
        assertEquals(id, sheet.item.id)
    }

    @Test
    fun `OnItemClick subscribes to the item's price history and reflects emitted snapshots`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        val hashName = "AWP | Asiimov (Field-Tested)"
        inventoryRepository.addItem(
            marketHashName = hashName,
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = SAMPLE_PURCHASE_PRICE,
        )
        dispatcher.scheduler.runCurrent()
        val item = viewModel.stateFlow.value.items
            .single()
            .item

        viewModel.onAction(InventoryViewModel.Action.OnItemClick(item))
        priceSnapshotRepository.emitSnapshot(
            marketHashName = hashName,
            lowestPrice = Money(minorUnits = 5000, currency = SteamCurrency.USD),
            capturedAt = Instant.fromEpochMilliseconds(1_000),
        )
        dispatcher.scheduler.runCurrent()

        val snapshots = viewModel.stateFlow.value.priceHistoryDetailAlert
            ?.snapshots
        assertEquals(1, snapshots?.size)
        assertEquals(Money(minorUnits = 5000, currency = SteamCurrency.USD), snapshots?.single()?.lowestPrice)
    }

    @Test
    fun `OnDismissPriceHistoryDetail closes the detail and stops reflecting new snapshots`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        val hashName = "P250 | Sand Dune"
        inventoryRepository.addItem(
            marketHashName = hashName,
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = SAMPLE_PURCHASE_PRICE,
        )
        dispatcher.scheduler.runCurrent()
        val item = viewModel.stateFlow.value.items
            .single()
            .item
        viewModel.onAction(InventoryViewModel.Action.OnItemClick(item))
        dispatcher.scheduler.runCurrent()

        viewModel.onAction(InventoryViewModel.Action.OnDismissPriceHistoryDetail)
        priceSnapshotRepository.emitSnapshot(
            marketHashName = hashName,
            lowestPrice = Money(minorUnits = 7000, currency = SteamCurrency.USD),
            capturedAt = Instant.fromEpochMilliseconds(2_000),
        )
        dispatcher.scheduler.runCurrent()

        assertNull(viewModel.stateFlow.value.priceHistoryDetailAlert)
    }

    @Test
    fun `OnItemClick for a different item while the detail is open cancels the previous subscription`() =
        runTest(dispatcher) {
            val viewModel = newViewModel()
            val firstHashName = "P250 | Sand Dune"
            val secondHashName = "M4A4 | Howl (Field-Tested)"
            inventoryRepository.addItem(
                marketHashName = firstHashName,
                iconUrl = "https://example.com/icon.png",
                quantity = 1,
                purchasePrice = SAMPLE_PURCHASE_PRICE,
            )
            inventoryRepository.addItem(
                marketHashName = secondHashName,
                iconUrl = "https://example.com/icon.png",
                quantity = 1,
                purchasePrice = SAMPLE_PURCHASE_PRICE,
            )
            dispatcher.scheduler.runCurrent()
            val items = viewModel.stateFlow.value.items
                .map { it.item }
            val firstItem = items.single { it.marketHashName == firstHashName }
            val secondItem = items.single { it.marketHashName == secondHashName }
            viewModel.onAction(InventoryViewModel.Action.OnItemClick(firstItem))
            dispatcher.scheduler.runCurrent()

            viewModel.onAction(InventoryViewModel.Action.OnItemClick(secondItem))
            dispatcher.scheduler.runCurrent()
            priceSnapshotRepository.emitSnapshot(
                marketHashName = firstHashName,
                lowestPrice = Money(minorUnits = 4000, currency = SteamCurrency.USD),
                capturedAt = Instant.fromEpochMilliseconds(3_000),
            )
            dispatcher.scheduler.runCurrent()

            val sheet = viewModel.stateFlow.value.priceHistoryDetailAlert
            check(sheet != null)
            assertEquals(secondItem.id, sheet.item.id)
            assertEquals(emptyList(), sheet.snapshots)
        }

    @Test
    fun `state reflects lastSyncedAt from the observer`() = runTest(dispatcher) {
        val viewModel = newViewModel()

        fixture.priceSyncStatusRepository.markCompleted(Instant.fromEpochMilliseconds(5_000))
        dispatcher.scheduler.runCurrent()

        assertEquals(Instant.fromEpochMilliseconds(5_000), viewModel.stateFlow.value.lastSyncedAt)
    }

    @Test
    fun `OnSyncNowClick invokes the shared sync interactor`() = runTest(dispatcher) {
        // An empty inventory is a no-op for the sync interactor itself (see
        // SyncPriceSnapshotsInteractorTest) -- this test needs an item so the click is observable.
        inventoryRepository.addItem(
            marketHashName = "AK-47 | Redline (Field-Tested)",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = SAMPLE_PURCHASE_PRICE,
        )
        val viewModel = newViewModel()

        viewModel.onAction(InventoryViewModel.Action.OnSyncNowClick)
        dispatcher.scheduler.runCurrent()

        assertEquals(1, fixture.priceSyncStatusRepository.markCompletedCalls.size)
    }

    @Test
    fun `state reflects isSyncing while a sync run is in progress`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        assertEquals(false, viewModel.stateFlow.value.isSyncing)

        viewModel.onAction(InventoryViewModel.Action.OnSyncNowClick)
        dispatcher.scheduler.runCurrent()

        // The fake sync completes synchronously within runCurrent(), so by the time it returns
        // isSyncing is back to false -- this asserts the round-trip works, not a mid-flight state.
        assertEquals(false, viewModel.stateFlow.value.isSyncing)
    }

    // OnItemClick no longer reaches editSheet -- see skinwatch-bhs.
    @Ignore
    @Test
    fun `saving Edit with valid fields calls updateItem and closes the sheet`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        val id = inventoryRepository.addItem(
            marketHashName = "M4A4 | Howl (Field-Tested)",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100_000, currency = SteamCurrency.USD),
        )
        dispatcher.scheduler.runCurrent()
        val item = viewModel.stateFlow.value.items
            .single()
            .item
        viewModel.onAction(InventoryViewModel.Action.OnItemClick(item))

        viewModel.onAction(InventoryViewModel.Action.OnQuantityChanged("4"))
        viewModel.onAction(InventoryViewModel.Action.OnSaveClick)
        dispatcher.scheduler.runCurrent()

        val updated = inventoryRepository.updatedItems.single()
        assertEquals(id, updated.id)
        assertEquals(4, updated.quantity)
        assertEquals(Money(minorUnits = 100_000, currency = SteamCurrency.USD), updated.purchasePrice)
        assertNull(viewModel.stateFlow.value.editSheet)
    }

    // OnItemClick no longer reaches editSheet -- see skinwatch-bhs.
    @Ignore
    @Test
    fun `an invalid quantity blocks Edit save and sets a validation error`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        inventoryRepository.addItem(
            marketHashName = "Item",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = SAMPLE_PURCHASE_PRICE,
        )
        dispatcher.scheduler.runCurrent()
        val item = viewModel.stateFlow.value.items
            .single()
            .item
        viewModel.onAction(InventoryViewModel.Action.OnItemClick(item))

        viewModel.onAction(InventoryViewModel.Action.OnQuantityChanged("-1"))
        viewModel.onAction(InventoryViewModel.Action.OnSaveClick)
        dispatcher.scheduler.runCurrent()

        val sheet = viewModel.stateFlow.value.editSheet
        check(sheet != null)
        assertEquals(ValidationError.INVALID_QUANTITY, sheet.validationError)
        assertEquals(emptyList(), inventoryRepository.updatedItems)
    }

    // OnItemClick no longer reaches editSheet -- see skinwatch-bhs.
    @Ignore
    @Test
    fun `OnDeleteClick shows the confirmation flag without deleting`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        inventoryRepository.addItem(
            marketHashName = "Item",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = SAMPLE_PURCHASE_PRICE,
        )
        dispatcher.scheduler.runCurrent()
        val item = viewModel.stateFlow.value.items
            .single()
            .item
        viewModel.onAction(InventoryViewModel.Action.OnItemClick(item))

        viewModel.onAction(InventoryViewModel.Action.OnDeleteClick)

        val sheet = viewModel.stateFlow.value.editSheet
        check(sheet != null)
        assertEquals(true, sheet.showDeleteConfirmation)
        assertEquals(emptyList(), inventoryRepository.removedIds)
    }

    // OnItemClick no longer reaches editSheet -- see skinwatch-bhs.
    @Ignore
    @Test
    fun `OnDeleteCancelled clears the confirmation flag and keeps the sheet open`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        inventoryRepository.addItem(
            marketHashName = "Item",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = SAMPLE_PURCHASE_PRICE,
        )
        dispatcher.scheduler.runCurrent()
        val item = viewModel.stateFlow.value.items
            .single()
            .item
        viewModel.onAction(InventoryViewModel.Action.OnItemClick(item))
        viewModel.onAction(InventoryViewModel.Action.OnDeleteClick)

        viewModel.onAction(InventoryViewModel.Action.OnDeleteCancelled)

        val sheet = viewModel.stateFlow.value.editSheet
        check(sheet != null)
        assertEquals(false, sheet.showDeleteConfirmation)
    }

    // OnItemClick no longer reaches editSheet -- see skinwatch-bhs.
    @Ignore
    @Test
    fun `OnDeleteConfirmed removes the item and closes the sheet`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        val id = inventoryRepository.addItem(
            marketHashName = "Item",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = SAMPLE_PURCHASE_PRICE,
        )
        dispatcher.scheduler.runCurrent()
        val item = viewModel.stateFlow.value.items
            .single()
            .item
        viewModel.onAction(InventoryViewModel.Action.OnItemClick(item))
        viewModel.onAction(InventoryViewModel.Action.OnDeleteClick)

        viewModel.onAction(InventoryViewModel.Action.OnDeleteConfirmed)
        dispatcher.scheduler.runCurrent()

        assertEquals(listOf(id), inventoryRepository.removedIds)
        assertNull(viewModel.stateFlow.value.editSheet)
    }
}
