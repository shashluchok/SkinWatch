package com.shashluchok.skinwatch.domain.steam

import com.shashluchok.skinwatch.domain.settings.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SearchMarketItemsInteractorTest {
    private val settingsRepository = FakeSettingsRepository(initialCurrency = SteamCurrency.EUR)
    private val steamMarketRepository = FakeSteamMarketRepository(defaultCurrency = SteamCurrency.USD)
    private val resolveDisplayCurrency = ResolveDisplayCurrencyInteractor(
        settingsRepository = settingsRepository,
        steamMarketRepository = steamMarketRepository,
    )
    private val interactor = SearchMarketItemsInteractor(
        steamMarketRepository = steamMarketRepository,
        resolveDisplayCurrency = resolveDisplayCurrency,
    )

    @Test
    fun `searches the market repository in the resolved display currency`() = runTest {
        interactor(query = "AK-47")

        assertEquals(listOf("AK-47"), steamMarketRepository.searchCalls)
        assertEquals(listOf(SteamCurrency.EUR), steamMarketRepository.searchCurrencies)
    }

    @Test
    fun `returns the market repository search result as-is`() = runTest {
        val item = SteamMarketItem(
            marketHashName = "AK-47 | Redline (Field-Tested)",
            displayName = "AK-47 | Redline (Field-Tested)",
            iconUrl = "https://example.com/icon.png",
            sellListingsCount = 100,
            sellPrice = Money(minorUnits = 1000, currency = SteamCurrency.EUR),
        )
        steamMarketRepository.searchResult = SteamMarketResult.Success(listOf(item))

        val result = interactor(query = "AK-47")

        val success = assertIs<SteamMarketResult.Success<List<SteamMarketItem>>>(result)
        assertEquals(listOf(item), success.data)
    }
}
