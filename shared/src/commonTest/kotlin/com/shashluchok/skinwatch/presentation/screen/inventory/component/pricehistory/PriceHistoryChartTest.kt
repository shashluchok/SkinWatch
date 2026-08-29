package com.shashluchok.skinwatch.presentation.screen.inventory.component.pricehistory

import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshot
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private fun snapshotWithLowestPrice(minorUnits: Long): PriceSnapshot = PriceSnapshot(
    marketHashName = "AK-47 | Redline",
    currency = SteamCurrency.USD,
    lowestPrice = Money(minorUnits = minorUnits, currency = SteamCurrency.USD),
    medianPrice = null,
    volume = null,
    capturedAt = Instant.fromEpochMilliseconds(0L),
)

class PriceHistoryChartTest {
    @Test
    fun `yAxisMax is 118 percent of the all-time highest lowest price when it exceeds purchase price`() {
        val snapshots = listOf(
            snapshotWithLowestPrice(minorUnits = 5_000L),
            snapshotWithLowestPrice(minorUnits = 10_000L),
        )
        val purchasePrice = Money(minorUnits = 4_000L, currency = SteamCurrency.USD)

        val yAxisMax = priceHistoryYAxisMax(snapshots = snapshots, purchasePrice = purchasePrice)

        assertEquals(expected = 118.0, actual = yAxisMax, absoluteTolerance = 0.0001)
    }

    @Test
    fun `yAxisMax is 118 percent of purchase price when it exceeds the all-time highest lowest price`() {
        val snapshots = listOf(snapshotWithLowestPrice(minorUnits = 3_000L))
        val purchasePrice = Money(minorUnits = 10_000L, currency = SteamCurrency.USD)

        val yAxisMax = priceHistoryYAxisMax(snapshots = snapshots, purchasePrice = purchasePrice)

        assertEquals(expected = 118.0, actual = yAxisMax, absoluteTolerance = 0.0001)
    }

    @Test
    fun `yAxisMax falls back to the all-time highest lowest price when purchase price is null`() {
        val snapshots = listOf(snapshotWithLowestPrice(minorUnits = 10_000L))

        val yAxisMax = priceHistoryYAxisMax(snapshots = snapshots, purchasePrice = null)

        assertEquals(expected = 118.0, actual = yAxisMax, absoluteTolerance = 0.0001)
    }

    @Test
    fun `priceHistoryReadingRange spans the all-time lowest to highest lowestPrice reading`() {
        val snapshots = listOf(
            snapshotWithLowestPrice(minorUnits = 5_000L),
            snapshotWithLowestPrice(minorUnits = 10_000L),
            snapshotWithLowestPrice(minorUnits = 3_000L),
        )

        val range = priceHistoryReadingRange(snapshots)

        assertEquals(expected = 30.0..100.0, actual = range)
    }

    @Test
    fun `purchasePriceCenteredYAxisStep picks whichever side needs the bigger step`() {
        val step = purchasePriceCenteredYAxisStep(minPrice = 10.0, maxPrice = 50.0, purchasePrice = 20.0)

        // Covering the 30-unit gap up to maxPrice in 2 real steps needs a bigger step (15) than
        // covering the 10-unit gap down to minPrice in 2 real steps (5).
        assertEquals(expected = 15.0, actual = step, absoluteTolerance = 0.0001)
    }

    @Test
    fun `purchasePriceCenteredYAxisStep stays positive when the purchase price is below every reading`() {
        val step = purchasePriceCenteredYAxisStep(minPrice = 50.0, maxPrice = 100.0, purchasePrice = 20.0)

        assertEquals(expected = 40.0, actual = step, absoluteTolerance = 0.0001)
    }

    @Test
    fun `purchasePriceCenteredYAxisStep stays positive when the purchase price is above every reading`() {
        val step = purchasePriceCenteredYAxisStep(minPrice = 10.0, maxPrice = 30.0, purchasePrice = 100.0)

        assertEquals(expected = 45.0, actual = step, absoluteTolerance = 0.0001)
    }

    @Test
    fun `purchasePriceCenteredYAxisStep falls back to a fraction of the price when every reading matches it`() {
        val step = purchasePriceCenteredYAxisStep(minPrice = 20.0, maxPrice = 20.0, purchasePrice = 20.0)

        assertEquals(expected = 1.0, actual = step, absoluteTolerance = 0.0001)
    }

    @Test
    fun `purchasePriceCenteredYAxisRange centers the purchase price with 3 steps on each side`() {
        val range = purchasePriceCenteredYAxisRange(purchasePrice = 100.0, step = 15.0)

        assertEquals(expected = 55.0..145.0, actual = range)
    }

    @Test
    fun `purchasePriceCenteredYAxisRange clamps a would-be-negative lower bound to 0`() {
        val range = purchasePriceCenteredYAxisRange(purchasePrice = 20.0, step = 15.0)

        assertEquals(expected = 0.0..65.0, actual = range)
    }

    @Test
    fun `maxEvenlySpacedLabelCount fits as many labels as the axis length allows`() {
        val count = maxEvenlySpacedLabelCount(axisLength = 310f, maxLabelLength = 40f, minLabelGap = 10f)

        assertEquals(expected = 6, actual = count)
    }

    @Test
    fun `maxEvenlySpacedLabelCount coerces to 2 when almost nothing fits`() {
        val count = maxEvenlySpacedLabelCount(axisLength = 10f, maxLabelLength = 100f, minLabelGap = 20f)

        assertEquals(expected = 2, actual = count)
    }

    @Test
    fun `evenlySpacedXValues splits the range into count minus one equal parts`() {
        val values = evenlySpacedXValues(minX = 0.0, maxX = 100.0, count = 5)

        assertEquals(expected = listOf(0.0, 25.0, 50.0, 75.0, 100.0), actual = values)
    }

    @Test
    fun `evenlySpacedXValues spacing stays even regardless of how readings are distributed`() {
        // Real readings could be clustered anywhere between minX and maxX -- evenlySpacedXValues
        // doesn't take them into account at all, so the spacing is always exactly even.
        val values = evenlySpacedXValues(minX = 0.0, maxX = 10.0, count = 3)

        assertEquals(expected = listOf(0.0, 5.0, 10.0), actual = values)
    }

    @Test
    fun `evenlySpacedXValues returns the range endpoints when count is 2`() {
        val values = evenlySpacedXValues(minX = 0.0, maxX = 40.0, count = 2)

        assertEquals(expected = listOf(0.0, 40.0), actual = values)
    }

    @Test
    fun `evenlySpacedXValues returns only maxX when count is 1`() {
        val values = evenlySpacedXValues(minX = 0.0, maxX = 10.0, count = 1)

        assertEquals(expected = listOf(10.0), actual = values)
    }

    @Test
    fun `evenlySpacedXValues returns a single value when minX equals maxX`() {
        val values = evenlySpacedXValues(minX = 5.0, maxX = 5.0, count = 5)

        assertEquals(expected = listOf(5.0), actual = values)
    }

    @Test
    fun `formatAxisPrice always shows 1 decimal digit for whole numbers`() {
        assertEquals(expected = "25.0", actual = formatAxisPrice(25.0))
    }

    @Test
    fun `formatAxisPrice rounds to exactly 1 decimal digit`() {
        assertEquals(expected = "25.3", actual = formatAxisPrice(25.333333))
    }

    @Test
    fun `formatAxisPrice rounds the second decimal digit up into the first`() {
        assertEquals(expected = "25.4", actual = formatAxisPrice(25.36))
    }

    @Test
    fun `formatAxisPrice keeps the sign and decimal for negative values`() {
        assertEquals(expected = "-12.5", actual = formatAxisPrice(-12.5))
    }

    @Test
    fun `formatAxisPrice always shows 1 decimal digit for negative whole numbers`() {
        assertEquals(expected = "-12.0", actual = formatAxisPrice(-12.0))
    }

    @Test
    fun `toTimeLabel formats hour and minute as zero-padded 24h HH-mm`() {
        val instant = LocalDateTime(2026, 1, 1, 19, 0).toInstant(TimeZone.UTC)

        assertEquals(expected = "19:00", actual = instant.toTimeLabel())
    }

    @Test
    fun `toTimeLabel zero-pads single-digit hour and minute`() {
        val instant = LocalDateTime(2026, 1, 1, 9, 5).toInstant(TimeZone.UTC)

        assertEquals(expected = "09:05", actual = instant.toTimeLabel())
    }

    @Test
    fun `formatPriceHistoryTooltipPrice combines price and currency`() {
        val label = formatPriceHistoryTooltipPrice(price = 49.0, currency = SteamCurrency.USD)

        assertEquals(expected = "49.0 USD", actual = label)
    }

    @Test
    fun `formatPriceHistoryTooltipTimestamp combines the full date and time`() {
        val instant = LocalDateTime(2026, 1, 1, 19, 0).toInstant(TimeZone.UTC)

        assertEquals(expected = "01.01.2026 19:00", actual = formatPriceHistoryTooltipTimestamp(instant))
    }

    @Test
    fun `priceComparedToPurchase is ABOVE when price exceeds the purchase price`() {
        assertEquals(
            expected = PriceComparedToPurchase.ABOVE,
            actual = priceComparedToPurchase(price = 60.0, purchasePrice = 50.0),
        )
    }

    @Test
    fun `priceComparedToPurchase is BELOW when price is under the purchase price`() {
        assertEquals(
            expected = PriceComparedToPurchase.BELOW,
            actual = priceComparedToPurchase(price = 40.0, purchasePrice = 50.0),
        )
    }

    @Test
    fun `priceComparedToPurchase is EQUAL when price matches the purchase price exactly`() {
        assertEquals(
            expected = PriceComparedToPurchase.EQUAL,
            actual = priceComparedToPurchase(price = 50.0, purchasePrice = 50.0),
        )
    }
}
