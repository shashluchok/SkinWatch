package com.shashluchok.skinwatch.data.storage

import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class ConvertersTest {
    @Test
    fun `instant round-trips through epoch milliseconds`() {
        val instant = Instant.fromEpochMilliseconds(1_755_000_000_000)

        assertEquals(instant, epochMillisToInstant(instantToEpochMillis(instant)))
    }

    @Test
    fun `SteamCurrency round-trips through its Steam numeric id`() {
        SteamCurrency.entries.forEach { currency ->
            assertEquals(currency, idToSteamCurrency(steamCurrencyToId(currency)), "currency=$currency")
        }
    }

    @Test
    fun `unknown currency id throws`() {
        assertFailsWith<IllegalArgumentException> { idToSteamCurrency(id = -1) }
    }
}
