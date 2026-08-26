package com.shashluchok.skinwatch.domain.debug

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeDebugSettingsRepository(
    initialShowSplashScreen: Boolean = true,
    initialMockDataEnabled: Boolean = false,
) : DebugSettingsRepository {
    private val settingsFlow = MutableStateFlow(
        DebugSettingsRepository.DebugSettings(
            showSplashScreen = initialShowSplashScreen,
            mockDataEnabled = initialMockDataEnabled,
        ),
    )

    override val settings: Flow<DebugSettingsRepository.DebugSettings> = settingsFlow

    override suspend fun update(settings: DebugSettingsRepository.DebugSettings) {
        settingsFlow.value = settings
    }
}
