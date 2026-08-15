package com.shashluchok.skinwatch.data.steam

import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SteamPriceParserTest {
    @Test
    fun `parses plain USD price`() {
        assertEquals(
            Money(minorUnits = 5193, currency = SteamCurrency.USD),
            SteamPriceParser.parse(text = "\$51.93", currency = SteamCurrency.USD),
        )
    }

    @Test
    fun `parses USD price with thousands separator`() {
        assertEquals(
            Money(minorUnits = 180001, currency = SteamCurrency.USD),
            SteamPriceParser.parse(text = "\$1,800.01", currency = SteamCurrency.USD),
        )
    }

    @Test
    fun `parses GBP price`() {
        assertEquals(
            Money(minorUnits = 3064, currency = SteamCurrency.GBP),
            SteamPriceParser.parse(text = "£30.64", currency = SteamCurrency.GBP),
        )
    }

    @Test
    fun `parses EUR price with suffix symbol and comma decimal`() {
        assertEquals(
            Money(minorUnits = 3583, currency = SteamCurrency.EUR),
            SteamPriceParser.parse(text = "35,83€", currency = SteamCurrency.EUR),
        )
    }

    @Test
    fun `parses EUR round amount using the dash-dash cents placeholder`() {
        assertEquals(
            Money(minorUnits = 156000, currency = SteamCurrency.EUR),
            SteamPriceParser.parse(text = "1 560,--€", currency = SteamCurrency.EUR),
        )
    }

    @Test
    fun `parses RUB price with textual suffix`() {
        assertEquals(
            Money(minorUnits = 341934, currency = SteamCurrency.RUB),
            SteamPriceParser.parse(text = "3419,34 руб.", currency = SteamCurrency.RUB),
        )
    }

    @Test
    fun `throws on text that does not match the currency format`() {
        assertFailsWith<IllegalArgumentException> {
            SteamPriceParser.parse(text = "not a price", currency = SteamCurrency.USD)
        }
    }
}
