package com.shashluchok.skinwatch.presentation.screen.settings

import com.shashluchok.skinwatch.domain.settings.FakeSettingsRepository
import com.shashluchok.skinwatch.domain.settings.ObserveSelectedCurrencyInteractor
import com.shashluchok.skinwatch.domain.settings.SetSelectedCurrencyInteractor
import com.shashluchok.skinwatch.domain.steam.FakeSteamMarketRepository
import com.shashluchok.skinwatch.domain.steam.GetDefaultCurrencyInteractor
import com.shashluchok.skinwatch.domain.steam.SteamCurrency

/**
 * Wires the real interactors [SettingsViewModel] depends on over this project's existing fake
 * repositories -- same reasoning as `InventoryViewModelFixture`: these interactors are plain
 * orchestration with no branching worth doubling on their own, so tests configure and assert
 * against the fakes at the repository boundary instead.
 */
internal class SettingsViewModelFixture(
    initialSelectedCurrency: SteamCurrency? = null,
    defaultCurrency: SteamCurrency = SteamCurrency.USD,
) {
    val settingsRepository = FakeSettingsRepository(initialCurrency = initialSelectedCurrency)
    val steamMarketRepository = FakeSteamMarketRepository(defaultCurrency = defaultCurrency)

    fun newViewModel() = SettingsViewModel(
        observeSelectedCurrency = ObserveSelectedCurrencyInteractor(settingsRepository = settingsRepository),
        setSelectedCurrency = SetSelectedCurrencyInteractor(settingsRepository = settingsRepository),
        getDefaultCurrency = GetDefaultCurrencyInteractor(steamMarketRepository = steamMarketRepository),
    )
}
