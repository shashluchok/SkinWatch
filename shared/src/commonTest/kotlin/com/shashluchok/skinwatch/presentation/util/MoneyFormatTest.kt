package com.shashluchok.skinwatch.presentation.util

import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlin.test.Test
import kotlin.test.assertEquals

private fun usd(minorUnits: Long) = Money(minorUnits = minorUnits, currency = SteamCurrency.USD)

class MoneyFormatTest {
    @Test
    fun `minor units are rendered as major units with the currency name`() {
        assertEquals("49.0 USD", formatMoney(usd(4900)))
        assertEquals("0.05 USD", formatMoney(usd(5)))
    }

    @Test
    fun `a gain is signed and carries its percentage`() {
        assertEquals("+4.0 USD (+8.8%)", formatSignedDelta(delta = usd(400), fraction = 0.0889f))
    }

    @Test
    fun `a loss reads as negative in both the amount and the percentage`() {
        assertEquals("-4.0 USD (-8.8%)", formatSignedDelta(delta = usd(-400), fraction = -0.0889f))
    }

    @Test
    fun `an unknown baseline drops the percentage rather than showing a misleading zero`() {
        assertEquals("+4.0 USD", formatSignedDelta(delta = usd(400), fraction = null))
    }

    @Test
    fun `no change is rendered as a plus, not a minus`() {
        assertEquals("+0.0 USD (+0.0%)", formatSignedDelta(delta = usd(0), fraction = 0f))
    }
}
