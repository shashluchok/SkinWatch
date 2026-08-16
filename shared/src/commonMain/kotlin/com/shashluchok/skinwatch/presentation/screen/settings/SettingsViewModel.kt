package com.shashluchok.skinwatch.presentation.screen.settings

import androidx.lifecycle.viewModelScope
import com.shashluchok.skinwatch.domain.settings.ObserveSelectedCurrencyInteractor
import com.shashluchok.skinwatch.domain.settings.SetSelectedCurrencyInteractor
import com.shashluchok.skinwatch.domain.steam.GetDefaultCurrencyInteractor
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import com.shashluchok.skinwatch.presentation.screen.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class SettingsViewModel(
    private val observeSelectedCurrency: ObserveSelectedCurrencyInteractor,
    private val setSelectedCurrency: SetSelectedCurrencyInteractor,
    getDefaultCurrency: GetDefaultCurrencyInteractor,
) : BaseViewModel<SettingsViewModel.State, SettingsViewModel.Action>() {
    data class State(
        val selectedCurrency: SteamCurrency? = null,
        val resolvedAutoCurrency: SteamCurrency,
        val isCurrencyPickerVisible: Boolean = false,
    )

    sealed interface Action {
        data object OnCurrencyRowClick : Action

        data class OnCurrencyOptionSelected(
            val currency: SteamCurrency?,
        ) : Action

        data object OnDismissCurrencyPicker : Action
    }

    override val mutableStateFlow: MutableStateFlow<State> =
        MutableStateFlow(State(resolvedAutoCurrency = getDefaultCurrency()))

    init {
        observeSelectedCurrency()
            .onEach { currency -> state = state.copy(selectedCurrency = currency) }
            .launchIn(viewModelScope)
    }

    override fun onAction(action: Action) {
        when (action) {
            Action.OnCurrencyRowClick -> state = state.copy(isCurrencyPickerVisible = true)
            is Action.OnCurrencyOptionSelected -> onCurrencyOptionSelected(action.currency)
            Action.OnDismissCurrencyPicker -> state = state.copy(isCurrencyPickerVisible = false)
        }
    }

    private fun onCurrencyOptionSelected(currency: SteamCurrency?) {
        viewModelScope.launch {
            setSelectedCurrency(currency)
            state = state.copy(isCurrencyPickerVisible = false)
        }
    }
}
