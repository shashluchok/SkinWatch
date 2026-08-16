package com.shashluchok.skinwatch.domain.exchangerate

import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ConvertMoneyTest {
    @Test
    fun `converts between currencies and rounds half-up`() {
        // 1 EUR = 2.0 USD -- 101 minor USD units / 2.0 = 50.5, half-up rounds to 51.
        val money = Money(minorUnits = 101, currency = SteamCurrency.USD)

        val converted = convertMoney(
            money = money,
            targetCurrency = SteamCurrency.EUR,
            targetPerUnitRates = mapOf(SteamCurrency.USD to 2.0),
        )

        assertEquals(Money(minorUnits = 51, currency = SteamCurrency.EUR), converted)
    }

    @Test
    fun `a row already in the target currency is returned unchanged, ignoring the rate map`() {
        val money = Money(minorUnits = 999, currency = SteamCurrency.EUR)

        val converted = convertMoney(
            money = money,
            targetCurrency = SteamCurrency.EUR,
            // Deliberately missing SteamCurrency.EUR -- proves the map is never consulted for a
            // same-currency conversion.
            targetPerUnitRates = emptyMap<SteamCurrency, Double>(),
        )

        assertSame(money, converted)
    }

    @Test
    fun `rounds half-up on the low side too`() {
        // 1 GBP = 4.0 USD -- 6 minor USD units / 4.0 = 1.5, half-up rounds to 2.
        val money = Money(minorUnits = 6, currency = SteamCurrency.USD)

        val converted = convertMoney(
            money = money,
            targetCurrency = SteamCurrency.GBP,
            targetPerUnitRates = mapOf(SteamCurrency.USD to 4.0),
        )

        assertEquals(Money(minorUnits = 2, currency = SteamCurrency.GBP), converted)
    }

    @Test
    fun `handles large minor unit amounts without overflow`() {
        // 1 USD = 1.0 USD -- large same-currency amount stays exact via the unchanged early return.
        val money = Money(minorUnits = Long.MAX_VALUE - 1, currency = SteamCurrency.USD)

        val converted = convertMoney(
            money = money,
            targetCurrency = SteamCurrency.USD,
            targetPerUnitRates = emptyMap<SteamCurrency, Double>(),
        )

        assertSame(money, converted)
    }

    @Test
    fun `converts a large minor unit amount across currencies`() {
        // 1 EUR = 95.3 RUB -- a large RUB amount converts down into EUR proportionally.
        val money = Money(minorUnits = 953_000_000L, currency = SteamCurrency.RUB)

        val converted = convertMoney(
            money = money,
            targetCurrency = SteamCurrency.EUR,
            targetPerUnitRates = mapOf(SteamCurrency.RUB to 95.3),
        )

        assertEquals(Money(minorUnits = 10_000_000L, currency = SteamCurrency.EUR), converted)
    }
}
