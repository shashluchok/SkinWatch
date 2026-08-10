package com.shashluchok.skinwatch.presentation.screen.inventory

import com.shashluchok.skinwatch.presentation.screen.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow

internal class InventoryViewModel : BaseViewModel<InventoryViewModel.State, InventoryViewModel.Action>() {
    data object State

    sealed interface Action

    override val mutableStateFlow: MutableStateFlow<State> = MutableStateFlow(State)

    override fun onAction(action: Action) = Unit
}
