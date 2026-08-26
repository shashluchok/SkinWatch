package com.shashluchok.skinwatch.presentation.screen.settings.component

import com.shashluchok.skinwatch.domain.debug.FakeDebugSettingsRepository
import com.shashluchok.skinwatch.domain.debug.ObserveDebugSettingsInteractor
import com.shashluchok.skinwatch.domain.debug.UpdateDebugSettingsInteractor

/**
 * Wires the real interactors [DebugPanelViewModel] depends on over a fake -- same reasoning as
 * `SettingsViewModelFixture`: these interactors are plain orchestration with no branching worth
 * doubling on their own, so tests configure and assert against the fake at the repository
 * boundary instead.
 */
internal class DebugPanelViewModelFixture(
    initialShowSplashScreen: Boolean = true,
    initialMockDataEnabled: Boolean = false,
) {
    val debugSettingsRepository = FakeDebugSettingsRepository(
        initialShowSplashScreen = initialShowSplashScreen,
        initialMockDataEnabled = initialMockDataEnabled,
    )

    fun newViewModel() = DebugPanelViewModel(
        observeDebugSettings = ObserveDebugSettingsInteractor(debugSettingsRepository = debugSettingsRepository),
        updateDebugSettings = UpdateDebugSettingsInteractor(debugSettingsRepository = debugSettingsRepository),
    )
}
