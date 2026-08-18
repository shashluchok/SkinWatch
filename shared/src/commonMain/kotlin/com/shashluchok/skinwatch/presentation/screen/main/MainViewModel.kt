package com.shashluchok.skinwatch.presentation.screen.main

import androidx.lifecycle.viewModelScope
import com.shashluchok.skinwatch.domain.inventory.AddInventoryItemInteractor
import com.shashluchok.skinwatch.domain.steam.SearchMarketItemsInteractor
import com.shashluchok.skinwatch.domain.steam.SteamMarketError
import com.shashluchok.skinwatch.domain.steam.SteamMarketItem
import com.shashluchok.skinwatch.domain.steam.SteamMarketResult
import com.shashluchok.skinwatch.presentation.component.ValidationError
import com.shashluchok.skinwatch.presentation.screen.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
internal class MainViewModel(
    private val searchMarketItems: SearchMarketItemsInteractor,
    private val addInventoryItem: AddInventoryItemInteractor,
) : BaseViewModel<MainViewModel.State, MainViewModel.Action>() {
    data class State(
        val addSheet: AddSheetState? = null,
    )

    sealed interface AddSheetState {
        data class AddSearch(
            val query: String = "",
            val status: SearchStatus = SearchStatus.Idle,
        ) : AddSheetState

        data class AddDetails(
            val selected: SteamMarketItem,
            val previousSearch: AddSearch,
            val quantity: String = "1",
            val purchasePrice: String = "",
            val validationError: ValidationError? = null,
        ) : AddSheetState
    }

    sealed interface SearchStatus {
        data object Idle : SearchStatus

        data object Searching : SearchStatus

        data object TakingLonger : SearchStatus

        data class Loaded(
            val results: List<SteamMarketItem>,
        ) : SearchStatus

        data class Failed(
            val error: SteamMarketError,
        ) : SearchStatus
    }

    sealed interface Action {
        data object OnAddClick : Action

        data object OnDismissSheet : Action

        data class OnSearchQueryChanged(
            val query: String,
        ) : Action

        data class OnSearchResultSelected(
            val result: SteamMarketItem,
        ) : Action

        data object OnAddDetailsBackClick : Action

        data class OnQuantityChanged(
            val value: String,
        ) : Action

        data class OnPurchasePriceChanged(
            val value: String,
        ) : Action

        data object OnSaveClick : Action
    }

    override val mutableStateFlow: MutableStateFlow<State> = MutableStateFlow(State())

    private val searchQueryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            searchQueryFlow.debounce(SEARCH_DEBOUNCE).collectLatest(::performSearch)
        }
    }

    override fun onAction(action: Action) {
        when (action) {
            Action.OnAddClick -> onAddClick()
            Action.OnDismissSheet -> onDismissSheet()
            is Action.OnSearchQueryChanged -> onSearchQueryChanged(action.query)
            is Action.OnSearchResultSelected -> onSearchResultSelected(action.result)
            Action.OnAddDetailsBackClick -> onAddDetailsBackClick()
            is Action.OnQuantityChanged -> onQuantityChanged(action.value)
            is Action.OnPurchasePriceChanged -> onPurchasePriceChanged(action.value)
            Action.OnSaveClick -> onSaveClick()
        }
    }

    private fun onAddClick() {
        state = state.copy(addSheet = AddSheetState.AddSearch())
    }

    private fun onDismissSheet() {
        state = state.copy(addSheet = null)
    }

    /**
     * Guarded update: no-ops if `state.addSheet` is no longer of type [T] (e.g. it was dismissed or
     * moved to a different step while a suspend call was in flight).
     */
    private inline fun <reified T : AddSheetState> updateSheet(transform: (T) -> T) {
        val current = state.addSheet as? T ?: return
        state = state.copy(addSheet = transform(current))
    }

    private fun onSearchQueryChanged(query: String) {
        updateSheet<AddSheetState.AddSearch> { it.copy(query = query) }
        searchQueryFlow.value = query
    }

    private suspend fun performSearch(query: String) {
        if (query.isBlank()) {
            updateSheet<AddSheetState.AddSearch> { it.copy(status = SearchStatus.Idle) }
            return
        }
        updateSheet<AddSheetState.AddSearch> { it.copy(status = SearchStatus.Searching) }
        coroutineScope {
            launch {
                delay(TAKING_LONGER_THRESHOLD)
                updateSheet<AddSheetState.AddSearch> { it.copy(status = SearchStatus.TakingLonger) }
            }
            val result = searchMarketItems(query)
            updateSheet<AddSheetState.AddSearch> { it.copy(status = result.toSearchStatus()) }
        }
    }

    private fun SteamMarketResult<List<SteamMarketItem>>.toSearchStatus(): SearchStatus = when (this) {
        is SteamMarketResult.Success -> SearchStatus.Loaded(results = data)
        is SteamMarketResult.Failure -> SearchStatus.Failed(error = error)
    }

    private fun onSearchResultSelected(result: SteamMarketItem) {
        val previousSearch = state.addSheet as? AddSheetState.AddSearch ?: return
        state = state.copy(addSheet = AddSheetState.AddDetails(selected = result, previousSearch = previousSearch))
    }

    private fun onAddDetailsBackClick() {
        val current = state.addSheet as? AddSheetState.AddDetails ?: return
        state = state.copy(addSheet = current.previousSearch)
    }

    private fun onQuantityChanged(value: String) {
        updateSheet<AddSheetState.AddDetails> { it.copy(quantity = value, validationError = null) }
    }

    private fun onPurchasePriceChanged(value: String) {
        updateSheet<AddSheetState.AddDetails> { it.copy(purchasePrice = value, validationError = null) }
    }

    private fun onSaveClick() {
        val sheet = state.addSheet as? AddSheetState.AddDetails ?: return
        saveNewItem(sheet)
    }

    private fun saveNewItem(sheet: AddSheetState.AddDetails) {
        val quantity = sheet.quantity.toIntOrNull()
        if (quantity == null || quantity < MIN_QUANTITY) {
            state = state.copy(addSheet = sheet.copy(validationError = ValidationError.INVALID_QUANTITY))
            return
        }
        val amount = sheet.purchasePrice.toValidatedAmountOrNull()
        if (amount == null) {
            state = state.copy(addSheet = sheet.copy(validationError = ValidationError.INVALID_PRICE))
            return
        }
        viewModelScope.launch {
            addInventoryItem(
                marketHashName = sheet.selected.marketHashName,
                iconUrl = sheet.selected.iconUrl,
                quantity = quantity,
                purchasePriceAmount = amount,
            )
            state = state.copy(addSheet = null)
        }
    }

    private fun String.toValidatedAmountOrNull(): Double? =
        takeIf { it.isNotBlank() }?.toDoubleOrNull()?.takeIf { it > 0 }

    private companion object {
        const val MIN_QUANTITY = 1
        val SEARCH_DEBOUNCE = 300.milliseconds
        val TAKING_LONGER_THRESHOLD = 1500.milliseconds
    }
}
