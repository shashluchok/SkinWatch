package com.shashluchok.skinwatch.domain.settings

import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SetSelectedCurrencyInteractorTest {
    @Test
    fun `saves the given currency as the new override`() = runTest {
        val settingsRepository = FakeSettingsRepository()
        val interactor = SetSelectedCurrencyInteractor(settingsRepository = settingsRepository)

        interactor(SteamCurrency.RUB)

        assertEquals(SteamCurrency.RUB, settingsRepository.selectedCurrency.first())
    }

    @Test
    fun `saves null to clear the override back to auto`() = runTest {
        val settingsRepository = FakeSettingsRepository(initialCurrency = SteamCurrency.RUB)
        val interactor = SetSelectedCurrencyInteractor(settingsRepository = settingsRepository)

        interactor(null)

        assertNull(settingsRepository.selectedCurrency.first())
    }
}
