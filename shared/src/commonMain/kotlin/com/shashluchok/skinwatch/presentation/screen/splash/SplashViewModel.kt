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

    sealed interface Action {
        data object ContentRevealed : Action
    }

    override val mutableStateFlow: MutableStateFlow<State> = MutableStateFlow(State())

    // isReady requires both signals: SPLASH_DURATION is a minimum floor (so a fast reel load
    // still gets a brief, deliberate pause before navigating away), and ContentRevealed guards
    // against the reel/text reveal taking longer than that floor -- e.g. on a slow asset parse --
    // which would otherwise cut the reel or the wordmark/tagline reveal off mid-animation.
    private var isMinDurationElapsed = false
    private var isContentRevealed = false

    init {
        viewModelScope.launch {
            delay(SPLASH_DURATION)
            isMinDurationElapsed = true
            updateReadiness()
        }
    }

    override fun onAction(action: Action) {
        when (action) {
            Action.ContentRevealed -> {
                isContentRevealed = true
                updateReadiness()
            }
        }
    }

    private fun updateReadiness() {
        if (isMinDurationElapsed && isContentRevealed) {
            state = state.copy(isReady = true)
        }
    }

    companion object {
        internal val SPLASH_DURATION = 4.seconds + 500.milliseconds
    }
}
