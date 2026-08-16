package com.shashluchok.skinwatch.domain.steam

import com.shashluchok.skinwatch.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Resolves the currency prices should be shown and stored in: the user's saved override if one
 * exists, otherwise the currency inferred from the device's region.
 */
internal class ResolveDisplayCurrencyInteractor(
    private val settingsRepository: SettingsRepository,
    private val steamMarketRepository: SteamMarketRepository,
) {
    suspend operator fun invoke(): SteamCurrency =
        settingsRepository.selectedCurrency.first() ?: steamMarketRepository.defaultCurrency
}
