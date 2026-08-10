package com.shashluchok.skinwatch.presentation.screen.watchlist

import com.shashluchok.skinwatch.presentation.screen.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow

internal class WatchlistViewModel : BaseViewModel<WatchlistViewModel.State, WatchlistViewModel.Action>() {
    data object State

    sealed interface Action

    override val mutableStateFlow: MutableStateFlow<State> = MutableStateFlow(State)

    override fun onAction(action: Action) = Unit
}
