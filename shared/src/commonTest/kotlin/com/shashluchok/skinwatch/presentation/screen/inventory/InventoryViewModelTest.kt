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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

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
            note = null,
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
            purchasePrice = null,
            note = null,
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
            purchasePrice = null,
            note = null,
        )
        inventoryRepository.addItem(
            marketHashName = hashName,
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = null,
            note = null,
        )

        dispatcher.scheduler.runCurrent()

        assertEquals(2, viewModel.stateFlow.value.items.size)
        assertEquals(1, priceSnapshotRepository.observeCallCounts[hashName])
    }

    @Test
    fun `OnItemClick opens the edit sheet prefilled from the clicked item`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        val id = inventoryRepository.addItem(
            marketHashName = "USP-S | Kill Confirmed (Minimal Wear)",
            iconUrl = "https://example.com/icon.png",
            quantity = 3,
            purchasePrice = Money(minorUnits = 2500, currency = SteamCurrency.USD),
            note = "gift",
        )
        dispatcher.scheduler.runCurrent()
        val item = viewModel.stateFlow.value.items
            .single()
            .item

        viewModel.onAction(InventoryViewModel.Action.OnItemClick(item))

        val sheet = viewModel.stateFlow.value.editSheet
        check(sheet != null)
        assertEquals(id, sheet.item.id)
        assertEquals("3", sheet.quantity)
        assertEquals("25.0", sheet.purchasePrice)
        assertEquals("gift", sheet.note)
    }

    @Test
    fun `saving Edit with valid fields calls updateItem and closes the sheet`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        val id = inventoryRepository.addItem(
            marketHashName = "M4A4 | Howl (Field-Tested)",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = Money(minorUnits = 100_000, currency = SteamCurrency.USD),
            note = null,
        )
        dispatcher.scheduler.runCurrent()
        val item = viewModel.stateFlow.value.items
            .single()
            .item
        viewModel.onAction(InventoryViewModel.Action.OnItemClick(item))

        viewModel.onAction(InventoryViewModel.Action.OnQuantityChanged("4"))
        viewModel.onAction(InventoryViewModel.Action.OnNoteChanged("resold once"))
        viewModel.onAction(InventoryViewModel.Action.OnSaveClick)
        dispatcher.scheduler.runCurrent()

        val updated = inventoryRepository.updatedItems.single()
        assertEquals(id, updated.id)
        assertEquals(4, updated.quantity)
        assertEquals("resold once", updated.note)
        assertEquals(Money(minorUnits = 100_000, currency = SteamCurrency.USD), updated.purchasePrice)
        assertNull(viewModel.stateFlow.value.editSheet)
    }

    @Test
    fun `an invalid quantity blocks Edit save and sets a validation error`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        inventoryRepository.addItem(
            marketHashName = "Item",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = null,
            note = null,
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

    @Test
    fun `OnDeleteClick shows the confirmation flag without deleting`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        inventoryRepository.addItem(
            marketHashName = "Item",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = null,
            note = null,
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

    @Test
    fun `OnDeleteCancelled clears the confirmation flag and keeps the sheet open`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        inventoryRepository.addItem(
            marketHashName = "Item",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = null,
            note = null,
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

    @Test
    fun `OnDeleteConfirmed removes the item and closes the sheet`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        val id = inventoryRepository.addItem(
            marketHashName = "Item",
            iconUrl = "https://example.com/icon.png",
            quantity = 1,
            purchasePrice = null,
            note = null,
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
