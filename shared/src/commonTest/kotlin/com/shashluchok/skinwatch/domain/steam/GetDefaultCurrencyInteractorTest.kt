package com.shashluchok.skinwatch.domain.steam

import kotlin.test.Test
import kotlin.test.assertEquals

class GetDefaultCurrencyInteractorTest {
    @Test
    fun `returns the market repository's default currency`() {
        val steamMarketRepository = FakeSteamMarketRepository(defaultCurrency = SteamCurrency.GBP)
        val interactor = GetDefaultCurrencyInteractor(steamMarketRepository = steamMarketRepository)

        assertEquals(SteamCurrency.GBP, interactor())
    }
}
