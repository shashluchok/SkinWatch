package com.shashluchok.skinwatch.presentation.screen.inventory.component

import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshot
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

private val purchasePrice = Money(minorUnits = 1000L, currency = SteamCurrency.USD)

private fun snapshotWithLowestPrice(minorUnits: Long?): PriceSnapshot = PriceSnapshot(
    marketHashName = "AK-47 | Redline",
    currency = SteamCurrency.USD,
    lowestPrice = minorUnits?.let { Money(minorUnits = it, currency = SteamCurrency.USD) },
    medianPrice = null,
    volume = null,
    capturedAt = Instant.fromEpochMilliseconds(0L),
)

class PriceTrendGlyphTest {
    @Test
    fun `trends up when the latest lowest price is above the purchase price`() {
        val trend = priceTrend(
            latestSnapshot = snapshotWithLowestPrice(minorUnits = 1500L),
            purchasePrice = purchasePrice,
        )

        assertEquals(expected = PriceTrend.UP, actual = trend)
    }

    @Test
    fun `trends down when the latest lowest price is below the purchase price`() {
        val trend = priceTrend(
            latestSnapshot = snapshotWithLowestPrice(minorUnits = 500L),
            purchasePrice = purchasePrice,
        )

        assertEquals(expected = PriceTrend.DOWN, actual = trend)
    }

    @Test
    fun `stays neutral when the latest lowest price equals the purchase price`() {
        val trend = priceTrend(
            latestSnapshot = snapshotWithLowestPrice(minorUnits = 1000L),
            purchasePrice = purchasePrice,
        )

        assertEquals(expected = PriceTrend.NEUTRAL, actual = trend)
    }

    @Test
    fun `has no trend when there is no snapshot`() {
        val trend = priceTrend(latestSnapshot = null, purchasePrice = purchasePrice)

        assertNull(trend)
    }

    @Test
    fun `has no trend when the snapshot carries no lowest price`() {
        val trend = priceTrend(
            latestSnapshot = snapshotWithLowestPrice(minorUnits = null),
            purchasePrice = purchasePrice,
        )

        assertNull(trend)
    }
}
