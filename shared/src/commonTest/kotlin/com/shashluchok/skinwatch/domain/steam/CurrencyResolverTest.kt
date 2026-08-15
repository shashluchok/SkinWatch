package com.shashluchok.skinwatch.domain.steam

import kotlin.test.Test
import kotlin.test.assertEquals

class CurrencyResolverTest {
    @Test
    fun `maps RU region to RUB`() {
        assertEquals(SteamCurrency.RUB, resolveSteamCurrency(regionCode = "RU"))
    }

    @Test
    fun `maps GB region to GBP`() {
        assertEquals(SteamCurrency.GBP, resolveSteamCurrency(regionCode = "GB"))
    }

    @Test
    fun `maps every eurozone region to EUR`() {
        val eurozoneRegions = listOf(
            "AT",
            "BE",
            "BG",
            "CY",
            "DE",
            "EE",
            "ES",
            "FI",
            "FR",
            "GR",
            "HR",
            "IE",
            "IT",
            "LT",
            "LU",
            "LV",
            "MT",
            "NL",
            "PT",
            "SI",
            "SK",
        )

        eurozoneRegions.forEach { region ->
            assertEquals(SteamCurrency.EUR, resolveSteamCurrency(regionCode = region), "region=$region")
        }
    }

    @Test
    fun `maps US region to USD`() {
        assertEquals(SteamCurrency.USD, resolveSteamCurrency(regionCode = "US"))
    }

    @Test
    fun `falls back to USD for a region outside the supported currencies`() {
        assertEquals(SteamCurrency.USD, resolveSteamCurrency(regionCode = "JP"))
    }

    @Test
    fun `falls back to USD when the region is unknown`() {
        assertEquals(SteamCurrency.USD, resolveSteamCurrency(regionCode = null))
    }

    @Test
    fun `region matching is case-insensitive`() {
        assertEquals(SteamCurrency.RUB, resolveSteamCurrency(regionCode = "ru"))
    }
}
