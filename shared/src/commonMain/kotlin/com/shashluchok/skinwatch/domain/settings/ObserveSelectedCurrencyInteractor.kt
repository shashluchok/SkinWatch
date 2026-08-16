package com.shashluchok.skinwatch.domain.settings

import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlinx.coroutines.flow.Flow

internal class ObserveSelectedCurrencyInteractor(
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<SteamCurrency?> = settingsRepository.selectedCurrency
}
