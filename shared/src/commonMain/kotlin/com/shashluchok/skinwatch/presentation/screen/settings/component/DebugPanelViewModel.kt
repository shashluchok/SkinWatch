package com.shashluchok.skinwatch.presentation.screen.settings.component

import androidx.lifecycle.viewModelScope
import com.shashluchok.skinwatch.domain.debug.DebugSettingsRepository
import com.shashluchok.skinwatch.domain.debug.ObserveDebugSettingsInteractor
import com.shashluchok.skinwatch.domain.debug.UpdateDebugSettingsInteractor
import com.shashluchok.skinwatch.presentation.screen.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class DebugPanelViewModel(
    private val observeDebugSettings: ObserveDebugSettingsInteractor,
    private val updateDebugSettings: UpdateDebugSettingsInteractor,
) : BaseViewModel<DebugPanelViewModel.State, DebugPanelViewModel.Action>() {
    data class State(
        val showSplashScreen: Boolean = true,
        val mockDataEnabled: Boolean = false,
    )

    sealed interface Action {
        data class OnShowSplashScreenToggled(
            val enabled: Boolean,
        ) : Action

        data class OnMockDataEnabledToggled(
            val enabled: Boolean,
        ) : Action
    }

    override val mutableStateFlow: MutableStateFlow<State> = MutableStateFlow(State())

    init {
        subscribeToDebugSettings()
    }

    private fun subscribeToDebugSettings() {
        observeDebugSettings()
            .onEach { settings ->
                state = state.copy(
                    showSplashScreen = settings.showSplashScreen,
                    mockDataEnabled = settings.mockDataEnabled,
                )
            }.launchIn(viewModelScope)
    }

    override fun onAction(action: Action) {
        when (action) {
            is Action.OnShowSplashScreenToggled -> onShowSplashScreenToggled(action.enabled)
            is Action.OnMockDataEnabledToggled -> onMockDataEnabledToggled(action.enabled)
        }
    }

    private fun onShowSplashScreenToggled(enabled: Boolean) {
        viewModelScope.launch {
            updateDebugSettings(
                DebugSettingsRepository.DebugSettings(
                    showSplashScreen = enabled,
                    mockDataEnabled = state.mockDataEnabled,
                ),
            )
        }
    }

    private fun onMockDataEnabledToggled(enabled: Boolean) {
        viewModelScope.launch {
            updateDebugSettings(
                DebugSettingsRepository.DebugSettings(
                    showSplashScreen = state.showSplashScreen,
                    mockDataEnabled = enabled,
                ),
            )
        }
    }
}
