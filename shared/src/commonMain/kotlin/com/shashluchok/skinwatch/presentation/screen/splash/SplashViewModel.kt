package com.shashluchok.skinwatch.presentation.screen.splash

import androidx.lifecycle.viewModelScope
import com.shashluchok.skinwatch.presentation.screen.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class SplashViewModel : BaseViewModel<SplashViewModel.State, SplashViewModel.Action>() {
    data class State(
        val isReady: Boolean = false,
    )

    sealed interface Action

    override val mutableStateFlow: MutableStateFlow<State> = MutableStateFlow(State())

    init {
        viewModelScope.launch {
            delay(SPLASH_DURATION)
            state = state.copy(isReady = true)
        }
    }

    override fun onAction(action: Action) = Unit

    companion object {
        internal val SPLASH_DURATION = 4.seconds + 500.milliseconds
    }
}
