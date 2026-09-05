package com.shashluchok.skinwatch.presentation.screen.inventory

import androidx.lifecycle.viewModelScope
import com.shashluchok.skinwatch.domain.inventory.InventoryItem
import com.shashluchok.skinwatch.domain.inventory.InventoryListItem
import com.shashluchok.skinwatch.domain.inventory.ObserveInventoryListInteractor
import com.shashluchok.skinwatch.domain.inventory.RemoveInventoryItemInteractor
import com.shashluchok.skinwatch.domain.inventory.UpdateInventoryItemInteractor
import com.shashluchok.skinwatch.domain.pricesync.ObserveLastSyncedAtInteractor
import com.shashluchok.skinwatch.domain.pricesync.SyncPriceSnapshotsInteractor
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.presentation.component.ValidationError
import com.shashluchok.skinwatch.presentation.screen.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Instant

internal const val MIN_LOADER_DURATION_MS = 1000L

internal class InventoryViewModel(
    private val observeInventoryList: ObserveInventoryListInteractor,
    private val updateInventoryItem: UpdateInventoryItemInteractor,
    private val removeInventoryItem: RemoveInventoryItemInteractor,
    private val syncPriceSnapshots: SyncPriceSnapshotsInteractor,
    private val observeLastSyncedAt: ObserveLastSyncedAtInteractor,
) : BaseViewModel<InventoryViewModel.State, InventoryViewModel.Action>() {
    data class State(
        val content: Content = Content.Loading,
        val editSheet: EditSheetState? = null,
        val contextMenuItem: InventoryItem? = null,
        val deleteConfirmationItem: InventoryItem? = null,
        val priceHistoryDetailAlertItem: InventoryItem? = null,
        val lastSyncedAt: Instant? = null,
        val isSyncing: Boolean = false,
    ) {
        sealed interface Content {
            data object Loading : Content

            data object Empty : Content

            data class Items(
                val items: List<InventoryListItem>,
            ) : Content
        }
    }

    data class EditSheetState(
        val item: InventoryItem,
        val quantity: String,
        val purchasePrice: String,
        val validationError: ValidationError? = null,
    )

    sealed interface Action {
        data class OnItemClick(
            val item: InventoryItem,
        ) : Action

        data class OnItemLongClick(
            val item: InventoryItem,
        ) : Action

        data object OnContextMenuEditClick : Action

        data object OnContextMenuDeleteClick : Action

        data object OnContextMenuDismiss : Action

        data class OnQuantityChanged(
            val value: String,
        ) : Action

        data class OnPurchasePriceChanged(
            val value: String,
        ) : Action

        data object OnSaveClick : Action

        data object OnDeleteConfirmed : Action

        data object OnDeleteCancelled : Action

        data object OnDismissSheet : Action

        data object OnDismissPriceHistoryDetail : Action

        data object OnSyncNowClick : Action
    }

    override val mutableStateFlow: MutableStateFlow<State> = MutableStateFlow(State())

    init {
        subscribeToInventoryList()
        subscribeToLastSyncedAt()
        subscribeToSyncStatus()
    }

    private fun subscribeToInventoryList() {
        // The loader is held for a minimum stretch so a list that resolves in a few milliseconds
        // does not flash a skeleton frame and vanish.
        val minLoaderElapsed = flow {
            emit(false)
            delay(MIN_LOADER_DURATION_MS)
            emit(true)
        }
        combine(observeInventoryList(), minLoaderElapsed) { items, isLoaderElapsed ->
            when {
                !isLoaderElapsed -> State.Content.Loading
                items.isEmpty() -> State.Content.Empty
                else -> State.Content.Items(items = items)
            }
        }.onEach { content -> state = state.copy(content = content) }
            .launchIn(viewModelScope)
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
            is Action.OnItemLongClick -> onItemLongClick(action.item)
            Action.OnContextMenuEditClick -> onContextMenuEditClick()
            Action.OnContextMenuDeleteClick -> onContextMenuDeleteClick()
            Action.OnContextMenuDismiss -> onContextMenuDismiss()
            is Action.OnQuantityChanged -> onQuantityChanged(action.value)
            is Action.OnPurchasePriceChanged -> onPurchasePriceChanged(action.value)
            Action.OnSaveClick -> onSaveClick()
            Action.OnDeleteConfirmed -> onDeleteConfirmed()
            Action.OnDeleteCancelled -> onDeleteCancelled()
            Action.OnDismissSheet -> onDismissSheet()
            Action.OnDismissPriceHistoryDetail -> onDismissPriceHistoryDetail()
            Action.OnSyncNowClick -> onSyncNowClick()
        }
    }

    private fun onItemClick(item: InventoryItem) {
        state = state.copy(priceHistoryDetailAlertItem = item)
    }

    private fun onDismissPriceHistoryDetail() {
        state = state.copy(priceHistoryDetailAlertItem = null)
    }

    private fun onItemLongClick(item: InventoryItem) {
        state = state.copy(contextMenuItem = item)
    }

    private fun onContextMenuDismiss() {
        state = state.copy(contextMenuItem = null)
    }

    private fun onContextMenuEditClick() {
        val item = state.contextMenuItem ?: return
        state = state.copy(
            contextMenuItem = null,
            editSheet = EditSheetState(
                item = item,
                quantity = item.quantity.toString(),
                purchasePrice = item.purchasePrice.toEditableAmountString(),
            ),
        )
    }

    private fun onContextMenuDeleteClick() {
        val item = state.contextMenuItem ?: return
        state = state.copy(contextMenuItem = null, deleteConfirmationItem = item)
    }

    private fun Money.toEditableAmountString(): String = (minorUnits / MINOR_UNITS_PER_MAJOR_UNIT).toString()

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

    private fun onDeleteCancelled() {
        state = state.copy(deleteConfirmationItem = null)
    }

    private fun onDeleteConfirmed() {
        val item = state.deleteConfirmationItem ?: return
        viewModelScope.launch {
            removeInventoryItem(item.id)
            state = state.copy(deleteConfirmationItem = null)
        }
    }

    private companion object {
        const val MIN_QUANTITY = 1
        const val MINOR_UNITS_PER_MAJOR_UNIT = 100.0
    }
}
