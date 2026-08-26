package com.shashluchok.skinwatch.presentation.screen.settings

import androidx.lifecycle.viewModelScope
import com.shashluchok.skinwatch.domain.AppConfigurationProvider
import com.shashluchok.skinwatch.domain.exchangerate.ConvertStoredPricesInteractor
import com.shashluchok.skinwatch.domain.exchangerate.ConvertStoredPricesResult
import com.shashluchok.skinwatch.domain.exchangerate.ExchangeRateError
import com.shashluchok.skinwatch.domain.exchangerate.HasConvertiblePricesInteractor
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
    private val getDefaultCurrency: GetDefaultCurrencyInteractor,
    private val hasConvertiblePrices: HasConvertiblePricesInteractor,
    private val convertStoredPrices: ConvertStoredPricesInteractor,
    private val appConfigurationProvider: AppConfigurationProvider,
) : BaseViewModel<SettingsViewModel.State, SettingsViewModel.Action>() {
    data class State(
        val selectedCurrency: SteamCurrency? = null,
        val resolvedAutoCurrency: SteamCurrency,
        val isCurrencyPickerVisible: Boolean = false,
        val isCurrencyChangeDialogVisible: Boolean = false,
        val pendingCurrency: SteamCurrency? = null,
        val conversionStatus: ConversionStatus = ConversionStatus.Idle,
        val isDebugPanelVisible: Boolean = false,
        val isDebugPanelAvailable: Boolean = false,
    )

    sealed interface ConversionStatus {
        data object Idle : ConversionStatus

        data object InProgress : ConversionStatus

        data class Failed(
            val error: ExchangeRateError,
        ) : ConversionStatus
    }

    sealed interface Action {
        data object OnCurrencyRowClick : Action

        data class OnCurrencyOptionSelected(
            val currency: SteamCurrency?,
        ) : Action

        data object OnDismissCurrencyPicker : Action

        data object OnCurrencyChangeConfirmed : Action

        data object OnCurrencyChangeCancelled : Action

        data object OnDebugRowClick : Action

        data object OnDismissDebugPanel : Action
    }

    override val mutableStateFlow: MutableStateFlow<State> =
        MutableStateFlow(
            State(
                resolvedAutoCurrency = getDefaultCurrency(),
                isDebugPanelAvailable = appConfigurationProvider.configuration.isDebug,
            ),
        )

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
            Action.OnCurrencyChangeConfirmed -> onCurrencyChangeConfirmed()
            Action.OnCurrencyChangeCancelled -> onCurrencyChangeCancelled()
            Action.OnDebugRowClick -> state = state.copy(isDebugPanelVisible = true)
            Action.OnDismissDebugPanel -> state = state.copy(isDebugPanelVisible = false)
        }
    }

    private fun onCurrencyOptionSelected(currency: SteamCurrency?) {
        state = state.copy(isCurrencyPickerVisible = false)
        viewModelScope.launch {
            if (hasConvertiblePrices()) {
                state = state.copy(
                    isCurrencyChangeDialogVisible = true,
                    pendingCurrency = currency,
                    conversionStatus = ConversionStatus.Idle,
                )
            } else {
                setSelectedCurrency(currency)
            }
        }
    }

    private fun onCurrencyChangeConfirmed() {
        val pending = state.pendingCurrency
        viewModelScope.launch {
            state = state.copy(conversionStatus = ConversionStatus.InProgress)
            val target = pending ?: getDefaultCurrency()
            when (val result = convertStoredPrices(targetCurrency = target, newSelectedCurrency = pending)) {
                ConvertStoredPricesResult.Success -> state = state.copy(
                    isCurrencyChangeDialogVisible = false,
                    pendingCurrency = null,
                    conversionStatus = ConversionStatus.Idle,
                )
                is ConvertStoredPricesResult.Failure ->
                    state = state.copy(conversionStatus = ConversionStatus.Failed(result.error))
            }
        }
    }

    private fun onCurrencyChangeCancelled() {
        state = state.copy(
            isCurrencyChangeDialogVisible = false,
            pendingCurrency = null,
            conversionStatus = ConversionStatus.Idle,
        )
    }
}
