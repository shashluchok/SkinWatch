package com.shashluchok.skinwatch.domain.debug

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal interface DebugSettingsRepository {
    data class DebugSettings(
        val showSplashScreen: Boolean = true,
        val mockDataEnabled: Boolean = false,
    )

    val settings: Flow<DebugSettings>

    suspend fun update(settings: DebugSettings)

    companion object {
        val EMPTY = object : DebugSettingsRepository {
            override val settings: Flow<DebugSettings> = flowOf(DebugSettings())

            override suspend fun update(settings: DebugSettings) = Unit
        }
    }
}
