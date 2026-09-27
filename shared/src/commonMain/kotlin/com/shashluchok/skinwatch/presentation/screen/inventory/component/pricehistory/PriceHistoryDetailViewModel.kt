package com.shashluchok.skinwatch.presentation.screen.inventory.component.pricehistory

import androidx.lifecycle.viewModelScope
import com.shashluchok.skinwatch.domain.inventory.InventoryItem
import com.shashluchok.skinwatch.domain.pricesnapshot.ObservePriceHistoryInteractor
import com.shashluchok.skinwatch.presentation.screen.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class PriceHistoryDetailViewModel(
    private val observePriceHistory: ObservePriceHistoryInteractor,
) : BaseViewModel<PriceHistoryDetailViewModel.State, PriceHistoryDetailViewModel.Action>() {
    sealed interface State {
        data class Content(
            val readings: List<PriceReading> = emptyList(),
        ) : State

        data object Loading : State
    }

    sealed interface Action {
        data class OnDisplay(
            val item: InventoryItem,
        ) : Action

        data object OnDismiss : Action
    }

    override val mutableStateFlow: MutableStateFlow<State> = MutableStateFlow(State.Loading)

    override fun onAction(action: Action) {
        when (action) {
            is Action.OnDisplay -> onDisplay(action.item)
            Action.OnDismiss -> onDismiss()
        }
    }

    private fun onDisplay(item: InventoryItem) {
        viewModelScope.launch {
            val readings = observePriceHistory(item.marketHashName).first().toPriceReadings()
            mutableStateFlow.update {
                State.Content(
                    readings = readings,
                )
            }
        }
    }

    private fun onDismiss() {
        mutableStateFlow.update {
            State.Loading
        }
    }
}
