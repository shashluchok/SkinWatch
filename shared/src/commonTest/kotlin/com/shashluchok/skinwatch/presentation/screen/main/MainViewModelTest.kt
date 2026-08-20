package com.shashluchok.skinwatch.presentation.screen.main

import com.shashluchok.skinwatch.domain.catalog.CatalogCategory
import com.shashluchok.skinwatch.domain.catalog.CatalogItem
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import com.shashluchok.skinwatch.domain.steam.SteamMarketError
import com.shashluchok.skinwatch.domain.steam.SteamMarketResult
import com.shashluchok.skinwatch.domain.steam.SteamPriceOverview
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
import kotlin.time.Duration.Companion.milliseconds

private val SEARCH_DEBOUNCE = 300.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val fixture = MainViewModelFixture()
    private val inventoryRepository = fixture.inventoryRepository
    private val priceSnapshotRepository = fixture.priceSnapshotRepository
    private val steamMarketRepository = fixture.steamMarketRepository
    private val catalogRepository = fixture.catalogRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        catalogRepository.emptyResult = false // most tests assume a populated catalog by default
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = fixture.newViewModel()

    @Test
    fun `OnAddClick opens AddSearch with an empty query`() = runTest(dispatcher) {
        val viewModel = newViewModel()

        viewModel.onAction(MainViewModel.Action.OnAddClick)

        val sheet = viewModel.stateFlow.value.addSheet
        check(sheet is MainViewModel.AddSheetState.AddSearch)
        assertEquals("", sheet.query)
    }

    @Test
    fun `OnDismissSheet closes an open sheet`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        viewModel.onAction(MainViewModel.Action.OnAddClick)

        viewModel.onAction(MainViewModel.Action.OnDismissSheet)

        assertNull(viewModel.stateFlow.value.addSheet)
    }

    @Test
    fun `typing a query debounces then searches the catalog and sets Loaded on a match`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        catalogRepository.searchResult = listOf(sampleCatalogItem())
        viewModel.onAction(MainViewModel.Action.OnAddClick)

        viewModel.onAction(MainViewModel.Action.OnSearchQueryChanged("AK-47"))
        dispatcher.scheduler.advanceTimeBy(SEARCH_DEBOUNCE)
        dispatcher.scheduler.runCurrent()

        assertEquals(listOf("AK-47"), catalogRepository.searchCalls)
        val sheet = viewModel.stateFlow.value.addSheet
        check(sheet is MainViewModel.AddSheetState.AddSearch)
        val status = sheet.status
        check(status is MainViewModel.SearchStatus.Loaded)
        assertEquals(listOf(sampleCatalogItem()), status.results)
    }

    @Test
    fun `a blank query sets status back to Idle without calling search`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        viewModel.onAction(MainViewModel.Action.OnAddClick)

        viewModel.onAction(MainViewModel.Action.OnSearchQueryChanged(""))
        dispatcher.scheduler.advanceTimeBy(SEARCH_DEBOUNCE)
        dispatcher.scheduler.runCurrent()

        assertEquals(emptyList(), catalogRepository.searchCalls)
        val sheet = viewModel.stateFlow.value.addSheet
        check(sheet is MainViewModel.AddSheetState.AddSearch)
        assertEquals(MainViewModel.SearchStatus.Idle, sheet.status)
    }

    @Test
    fun `an empty catalog on a non-blank query sets status to CatalogUnavailable`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        catalogRepository.emptyResult = true
        catalogRepository.searchResult = emptyList()
        viewModel.onAction(MainViewModel.Action.OnAddClick)

        viewModel.onAction(MainViewModel.Action.OnSearchQueryChanged("AK-47"))
        dispatcher.scheduler.advanceTimeBy(SEARCH_DEBOUNCE)
        dispatcher.scheduler.runCurrent()

        val sheet = viewModel.stateFlow.value.addSheet
        check(sheet is MainViewModel.AddSheetState.AddSearch)
        assertEquals(MainViewModel.SearchStatus.CatalogUnavailable, sheet.status)
    }

    @Test
    fun `selecting a search result opens AddDetails with quantity defaulted to 1`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        viewModel.onAction(MainViewModel.Action.OnAddClick)

        viewModel.onAction(MainViewModel.Action.OnSearchResultSelected(sampleCatalogItem()))

        val sheet = viewModel.stateFlow.value.addSheet
        check(sheet is MainViewModel.AddSheetState.AddDetails)
        assertEquals(sampleCatalogItem(), sheet.selected)
        assertEquals("1", sheet.quantity)
        assertEquals("", sheet.purchasePrice)
    }

    @Test
    fun `OnAddDetailsBackClick restores the previous search step with its query and status`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        catalogRepository.searchResult = listOf(sampleCatalogItem())
        viewModel.onAction(MainViewModel.Action.OnAddClick)
        viewModel.onAction(MainViewModel.Action.OnSearchQueryChanged("AK-47"))
        dispatcher.scheduler.advanceTimeBy(SEARCH_DEBOUNCE)
        dispatcher.scheduler.runCurrent()
        viewModel.onAction(MainViewModel.Action.OnSearchResultSelected(sampleCatalogItem()))

        viewModel.onAction(MainViewModel.Action.OnAddDetailsBackClick)

        val sheet = viewModel.stateFlow.value.addSheet
        check(sheet is MainViewModel.AddSheetState.AddSearch)
        assertEquals("AK-47", sheet.query)
        check(sheet.status is MainViewModel.SearchStatus.Loaded)
    }

    @Test
    fun `an invalid quantity blocks save and sets a validation error`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        viewModel.onAction(MainViewModel.Action.OnAddClick)
        viewModel.onAction(MainViewModel.Action.OnSearchResultSelected(sampleCatalogItem()))

        viewModel.onAction(MainViewModel.Action.OnQuantityChanged("0"))
        viewModel.onAction(MainViewModel.Action.OnSaveClick)
        dispatcher.scheduler.runCurrent()

        val sheet = viewModel.stateFlow.value.addSheet
        check(sheet is MainViewModel.AddSheetState.AddDetails)
        assertEquals(ValidationError.INVALID_QUANTITY, sheet.validationError)
        assertEquals(emptyList(), inventoryRepository.observeItems().value)
    }

    @Test
    fun `a non-numeric purchase price blocks save and sets a validation error`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        viewModel.onAction(MainViewModel.Action.OnAddClick)
        viewModel.onAction(MainViewModel.Action.OnSearchResultSelected(sampleCatalogItem()))

        viewModel.onAction(MainViewModel.Action.OnPurchasePriceChanged("not a number"))
        viewModel.onAction(MainViewModel.Action.OnSaveClick)
        dispatcher.scheduler.runCurrent()

        val sheet = viewModel.stateFlow.value.addSheet
        check(sheet is MainViewModel.AddSheetState.AddDetails)
        assertEquals(ValidationError.INVALID_PRICE, sheet.validationError)
    }

    @Test
    fun `a blank purchase price blocks save and sets a validation error`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        viewModel.onAction(MainViewModel.Action.OnAddClick)
        viewModel.onAction(MainViewModel.Action.OnSearchResultSelected(sampleCatalogItem()))

        viewModel.onAction(MainViewModel.Action.OnSaveClick)
        dispatcher.scheduler.runCurrent()

        val sheet = viewModel.stateFlow.value.addSheet
        check(sheet is MainViewModel.AddSheetState.AddDetails)
        assertEquals(ValidationError.INVALID_PRICE, sheet.validationError)
        assertEquals(emptyList(), inventoryRepository.observeItems().value)
    }

    @Test
    fun `saving with valid fields adds the item, records a snapshot on success, and closes the sheet`() =
        runTest(dispatcher) {
            val viewModel = newViewModel()
            steamMarketRepository.priceOverviewResult = SteamMarketResult.Success(
                SteamPriceOverview(
                    lowestPrice = Money(minorUnits = 4900, currency = SteamCurrency.USD),
                    medianPrice = Money(minorUnits = 5000, currency = SteamCurrency.USD),
                    volume = 42,
                ),
            )
            viewModel.onAction(MainViewModel.Action.OnAddClick)
            viewModel.onAction(MainViewModel.Action.OnSearchResultSelected(sampleCatalogItem()))
            viewModel.onAction(MainViewModel.Action.OnQuantityChanged("2"))
            viewModel.onAction(MainViewModel.Action.OnPurchasePriceChanged("12.50"))

            viewModel.onAction(MainViewModel.Action.OnSaveClick)
            dispatcher.scheduler.runCurrent()

            val added = inventoryRepository.observeItems().value.single()
            assertEquals(sampleCatalogItem().marketHashName, added.marketHashName)
            assertEquals(2, added.quantity)
            assertEquals(Money(minorUnits = 1250, currency = SteamCurrency.USD), added.purchasePrice)
            val recorded = priceSnapshotRepository.recorded.single()
            assertEquals(Money(minorUnits = 4900, currency = SteamCurrency.USD), recorded.lowestPrice)
            assertNull(viewModel.stateFlow.value.addSheet)
        }

    @Test
    fun `a failed price overview still saves the item but records no snapshot`() = runTest(dispatcher) {
        val viewModel = newViewModel()
        steamMarketRepository.priceOverviewResult = SteamMarketResult.Failure(SteamMarketError.Network)
        viewModel.onAction(MainViewModel.Action.OnAddClick)
        viewModel.onAction(MainViewModel.Action.OnSearchResultSelected(sampleCatalogItem()))
        viewModel.onAction(MainViewModel.Action.OnPurchasePriceChanged("12.50"))

        viewModel.onAction(MainViewModel.Action.OnSaveClick)
        dispatcher.scheduler.runCurrent()

        assertEquals(1, inventoryRepository.observeItems().value.size)
        assertEquals(emptyList(), priceSnapshotRepository.recorded)
        assertNull(viewModel.stateFlow.value.addSheet)
    }

    private fun sampleCatalogItem() = CatalogItem(
        marketHashName = "AK-47 | Redline (Field-Tested)",
        displayName = "AK-47 | Redline (Field-Tested)",
        iconUrl = "https://example.com/icon.png",
        category = CatalogCategory.SKIN,
    )
}
