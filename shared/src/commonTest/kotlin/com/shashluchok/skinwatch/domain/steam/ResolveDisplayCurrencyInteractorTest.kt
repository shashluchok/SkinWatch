package com.shashluchok.skinwatch.domain.steam

import com.shashluchok.skinwatch.domain.settings.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolveDisplayCurrencyInteractorTest {
    @Test
    fun `returns the saved currency override when one is set`() = runTest {
        val settingsRepository = FakeSettingsRepository(initialCurrency = SteamCurrency.EUR)
        val steamMarketRepository = FakeSteamMarketRepository(defaultCurrency = SteamCurrency.USD)
        val interactor = ResolveDisplayCurrencyInteractor(
            settingsRepository = settingsRepository,
            steamMarketRepository = steamMarketRepository,
        )

        assertEquals(SteamCurrency.EUR, interactor())
    }

    @Test
    fun `falls back to the market repository default currency when no override is saved`() = runTest {
        val settingsRepository = FakeSettingsRepository(initialCurrency = null)
        val steamMarketRepository = FakeSteamMarketRepository(defaultCurrency = SteamCurrency.GBP)
        val interactor = ResolveDisplayCurrencyInteractor(
            settingsRepository = settingsRepository,
            steamMarketRepository = steamMarketRepository,
        )

        assertEquals(SteamCurrency.GBP, interactor())
    }
}
