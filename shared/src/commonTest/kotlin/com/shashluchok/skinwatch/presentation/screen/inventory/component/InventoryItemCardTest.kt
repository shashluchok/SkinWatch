package com.shashluchok.skinwatch.presentation.screen.inventory.component

import androidx.compose.ui.graphics.Color
import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshot
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val positive = Color(0xFF128A6C)
private val negative = Color(0xFFB22B3A)
private val neutral = Color(0xFF888888)

private val purchasePrice = Money(minorUnits = 1000L, currency = SteamCurrency.USD)

private fun snapshotWithLowestPrice(minorUnits: Long?): PriceSnapshot = PriceSnapshot(
    marketHashName = "AK-47 | Redline",
    currency = SteamCurrency.USD,
    lowestPrice = minorUnits?.let { Money(minorUnits = it, currency = SteamCurrency.USD) },
    medianPrice = null,
    volume = null,
    capturedAt = Instant.fromEpochMilliseconds(0L),
)

class InventoryItemCardTest {
    @Test
    fun `returns positive color when latest lowest price is above purchase price`() {
        val color = priceHistoryGlyphColor(
            latestSnapshot = snapshotWithLowestPrice(minorUnits = 1500L),
            purchasePrice = purchasePrice,
            positive = positive,
            negative = negative,
            neutral = neutral,
        )

        assertEquals(expected = positive, actual = color)
    }

    @Test
    fun `returns negative color when latest lowest price is below purchase price`() {
        val color = priceHistoryGlyphColor(
            latestSnapshot = snapshotWithLowestPrice(minorUnits = 500L),
            purchasePrice = purchasePrice,
            positive = positive,
            negative = negative,
            neutral = neutral,
        )

        assertEquals(expected = negative, actual = color)
    }

    @Test
    fun `returns neutral color when latest lowest price equals purchase price`() {
        val color = priceHistoryGlyphColor(
            latestSnapshot = snapshotWithLowestPrice(minorUnits = 1000L),
            purchasePrice = purchasePrice,
            positive = positive,
            negative = negative,
            neutral = neutral,
        )

        assertEquals(expected = neutral, actual = color)
    }

    @Test
    fun `returns neutral color when there is no snapshot`() {
        val color = priceHistoryGlyphColor(
            latestSnapshot = null,
            purchasePrice = purchasePrice,
            positive = positive,
            negative = negative,
            neutral = neutral,
        )

        assertEquals(expected = neutral, actual = color)
    }

    @Test
    fun `returns neutral color when snapshot has no lowest price`() {
        val color = priceHistoryGlyphColor(
            latestSnapshot = snapshotWithLowestPrice(minorUnits = null),
            purchasePrice = purchasePrice,
            positive = positive,
            negative = negative,
            neutral = neutral,
        )

        assertEquals(expected = neutral, actual = color)
    }
}
