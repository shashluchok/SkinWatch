package com.shashluchok.skinwatch.domain.settings

import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObserveSelectedCurrencyInteractorTest {
    @Test
    fun `emits the repository's current selected currency`() = runTest {
        val settingsRepository = FakeSettingsRepository(initialCurrency = SteamCurrency.EUR)
        val interactor = ObserveSelectedCurrencyInteractor(settingsRepository = settingsRepository)

        assertEquals(SteamCurrency.EUR, interactor().first())
    }

    @Test
    fun `emits null when no override is saved`() = runTest {
        val settingsRepository = FakeSettingsRepository(initialCurrency = null)
        val interactor = ObserveSelectedCurrencyInteractor(settingsRepository = settingsRepository)

        assertNull(interactor().first())
    }
}
