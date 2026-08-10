package com.shashluchok.skinwatch.presentation.screen.settings

import com.shashluchok.skinwatch.presentation.screen.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow

internal class SettingsViewModel : BaseViewModel<SettingsViewModel.State, SettingsViewModel.Action>() {
    data object State

    sealed interface Action

    override val mutableStateFlow: MutableStateFlow<State> = MutableStateFlow(State)

    override fun onAction(action: Action) = Unit
}
