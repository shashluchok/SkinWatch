package com.shashluchok.skinwatch.domain.settings

import com.shashluchok.skinwatch.domain.steam.SteamCurrency

internal class SetSelectedCurrencyInteractor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(currency: SteamCurrency?) {
        settingsRepository.setSelectedCurrency(currency)
    }
}
