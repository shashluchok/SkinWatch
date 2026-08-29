package com.shashluchok.skinwatch.data.storage.debug

import com.shashluchok.skinwatch.domain.debug.DebugSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class DebugSettingsRepositoryImpl(
    private val dao: DebugSettingsDao,
    scope: CoroutineScope,
) : DebugSettingsRepository {
    private val cachedSettings: StateFlow<DebugSettingsRepository.DebugSettings?> = dao
        .observe()
        .map { entity -> entity?.toDomain() ?: DebugSettingsRepository.DebugSettings() }
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = null)

    override val settings: Flow<DebugSettingsRepository.DebugSettings> = cachedSettings.filterNotNull()

    override suspend fun update(settings: DebugSettingsRepository.DebugSettings) {
        dao.upsert(
            DebugSettingsEntity(
                showSplashScreen = settings.showSplashScreen,
                mockDataEnabled = settings.mockDataEnabled,
            ),
        )
    }

    private fun DebugSettingsEntity.toDomain() = DebugSettingsRepository.DebugSettings(
        showSplashScreen = showSplashScreen,
        mockDataEnabled = mockDataEnabled,
    )
}
