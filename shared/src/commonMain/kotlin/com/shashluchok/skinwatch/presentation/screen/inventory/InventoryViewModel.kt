package com.shashluchok.skinwatch.presentation.screen.inventory

import androidx.lifecycle.viewModelScope
import com.shashluchok.skinwatch.domain.inventory.InventoryItem
import com.shashluchok.skinwatch.domain.inventory.InventoryListItem
import com.shashluchok.skinwatch.domain.inventory.ObserveInventoryListInteractor
import com.shashluchok.skinwatch.domain.inventory.RemoveInventoryItemInteractor
import com.shashluchok.skinwatch.domain.inventory.UpdateInventoryItemInteractor
import com.shashluchok.skinwatch.domain.pricesnapshot.ObservePriceHistoryInteractor
import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshot
import com.shashluchok.skinwatch.domain.pricesync.ObserveLastSyncedAtInteractor
import com.shashluchok.skinwatch.domain.pricesync.SyncPriceSnapshotsInteractor
import com.shashluchok.skinwatch.presentation.component.ValidationError
import com.shashluchok.skinwatch.presentation.screen.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Instant

internal class InventoryViewModel(
    private val observeInventoryList: ObserveInventoryListInteractor,
    private val updateInventoryItem: UpdateInventoryItemInteractor,
    private val removeInventoryItem: RemoveInventoryItemInteractor,
    private val observePriceHistory: ObservePriceHistoryInteractor,
    private val syncPriceSnapshots: SyncPriceSnapshotsInteractor,
    private val observeLastSyncedAt: ObserveLastSyncedAtInteractor,
) : BaseViewModel<InventoryViewModel.State, InventoryViewModel.Action>() {
    data class State(
        val items: List<InventoryListItem> = emptyList(),
        val editSheet: EditSheetState? = null,
        val priceHistoryDetailAlert: PriceHistoryDetailState? = null,
        val lastSyncedAt: Instant? = null,
        val isSyncing: Boolean = false,
    )

    data class EditSheetState(
        val item: InventoryItem,
        val quantity: String,
        val purchasePrice: String,
        val validationError: ValidationError? = null,
        val showDeleteConfirmation: Boolean = false,
    )

    data class PriceHistoryDetailState(
        val item: InventoryItem,
        val snapshots: List<PriceSnapshot> = emptyList(),
        val isLoading: Boolean = true,
    )

    sealed interface Action {
        data class OnItemClick(
            val item: InventoryItem,
        ) : Action

        data class OnQuantityChanged(
            val value: String,
        ) : Action

        data class OnPurchasePriceChanged(
            val value: String,
        ) : Action

        data object OnSaveClick : Action

        data object OnDeleteClick : Action

        data object OnDeleteConfirmed : Action

        data object OnDeleteCancelled : Action

        data object OnDismissSheet : Action

        data object OnDismissPriceHistoryDetail : Action

        data object OnSyncNowClick : Action
    }

    override val mutableStateFlow: MutableStateFlow<State> = MutableStateFlow(State())

    private var priceHistoryJob: Job? = null

    init {
        subscribeToInventoryList()
        subscribeToLastSyncedAt()
        subscribeToSyncStatus()
    }

    private fun subscribeToInventoryList() {
        observeInventoryList().onEach { items -> state = state.copy(items = items) }.launchIn(viewModelScope)
    }

    private fun subscribeToLastSyncedAt() {
        observeLastSyncedAt()
            .onEach { lastSyncedAt -> state = state.copy(lastSyncedAt = lastSyncedAt) }
            .launchIn(viewModelScope)
    }

    private fun subscribeToSyncStatus() {
        syncPriceSnapshots.isSyncing
            .onEach { isSyncing -> state = state.copy(isSyncing = isSyncing) }
            .launchIn(viewModelScope)
    }

    override fun onAction(action: Action) {
        when (action) {
            is Action.OnItemClick -> onItemClick(action.item)
            is Action.OnQuantityChanged -> onQuantityChanged(action.value)
            is Action.OnPurchasePriceChanged -> onPurchasePriceChanged(action.value)
            Action.OnSaveClick -> onSaveClick()
            Action.OnDeleteClick -> onDeleteClick()
            Action.OnDeleteConfirmed -> onDeleteConfirmed()
            Action.OnDeleteCancelled -> onDeleteCancelled()
            Action.OnDismissSheet -> onDismissSheet()
            Action.OnDismissPriceHistoryDetail -> onDismissPriceHistoryDetail()
            Action.OnSyncNowClick -> onSyncNowClick()
        }
    }

    private fun onItemClick(item: InventoryItem) {
        state = state.copy(priceHistoryDetailAlert = PriceHistoryDetailState(item = item))
        priceHistoryJob?.cancel()
        priceHistoryJob = observePriceHistory(item.marketHashName)
            .onEach { snapshots ->
                state = state.copy(
                    priceHistoryDetailAlert = state.priceHistoryDetailAlert?.copy(
                        snapshots = snapshots,
                        isLoading = false,
                    ),
                )
            }.launchIn(viewModelScope)
    }

    private fun onDismissPriceHistoryDetail() {
        priceHistoryJob?.cancel()
        priceHistoryJob = null
        state = state.copy(priceHistoryDetailAlert = null)
    }

    /**
     * A tap during an already-in-flight run is safe -- [SyncPriceSnapshotsInteractor]'s own mutex
     * makes the second call a no-op, so no extra guard is needed here.
     */
    private fun onSyncNowClick() {
        viewModelScope.launch { syncPriceSnapshots() }
    }

    private fun onDismissSheet() {
        state = state.copy(editSheet = null)
    }

    private fun updateSheet(transform: (EditSheetState) -> EditSheetState) {
        val current = state.editSheet ?: return
        state = state.copy(editSheet = transform(current))
    }

    private fun onQuantityChanged(value: String) {
        updateSheet { it.copy(quantity = value, validationError = null) }
    }

    private fun onPurchasePriceChanged(value: String) {
        updateSheet { it.copy(purchasePrice = value, validationError = null) }
    }

    private fun onSaveClick() {
        val sheet = state.editSheet ?: return
        saveEditedItem(sheet)
    }

    private fun saveEditedItem(sheet: EditSheetState) {
        val quantity = sheet.quantity.toIntOrNull()
        if (quantity == null || quantity < MIN_QUANTITY) {
            state = state.copy(editSheet = sheet.copy(validationError = ValidationError.INVALID_QUANTITY))
            return
        }
        val amount = sheet.purchasePrice.toValidatedAmountOrNull()
        if (amount == null) {
            state = state.copy(editSheet = sheet.copy(validationError = ValidationError.INVALID_PRICE))
            return
        }
        viewModelScope.launch {
            updateInventoryItem(item = sheet.item, quantity = quantity, purchasePriceAmount = amount)
            state = state.copy(editSheet = null)
        }
    }

    private fun String.toValidatedAmountOrNull(): Double? =
        takeIf { it.isNotBlank() }?.toDoubleOrNull()?.takeIf { it > 0 }

    private fun onDeleteClick() {
        updateSheet { it.copy(showDeleteConfirmation = true) }
    }

    private fun onDeleteCancelled() {
        updateSheet { it.copy(showDeleteConfirmation = false) }
    }

    private fun onDeleteConfirmed() {
        val sheet = state.editSheet ?: return
        viewModelScope.launch {
            removeInventoryItem(sheet.item.id)
            state = state.copy(editSheet = null)
        }
    }

    private companion object {
        const val MIN_QUANTITY = 1
    }
}
