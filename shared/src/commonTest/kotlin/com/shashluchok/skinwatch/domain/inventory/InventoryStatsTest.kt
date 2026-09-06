package com.shashluchok.skinwatch.domain.inventory

import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshot
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

private fun listItem(
    purchaseMinorUnits: Long,
    quantity: Int,
    latestMinorUnits: Long?,
): InventoryListItem = InventoryListItem(
    item = InventoryItem(
        id = purchaseMinorUnits,
        marketHashName = "AK-47 | Redline $purchaseMinorUnits",
        iconUrl = "https://example.com/icon.png",
        addedAt = Instant.fromEpochMilliseconds(0),
        quantity = quantity,
        purchasePrice = Money(minorUnits = purchaseMinorUnits, currency = SteamCurrency.USD),
    ),
    latestSnapshot = latestMinorUnits?.let {
        PriceSnapshot(
            marketHashName = "AK-47 | Redline $purchaseMinorUnits",
            currency = SteamCurrency.USD,
            lowestPrice = Money(minorUnits = it, currency = SteamCurrency.USD),
            medianPrice = null,
            volume = null,
            capturedAt = Instant.fromEpochMilliseconds(0),
        )
    },
)

class InventoryStatsTest {
    @Test
    fun `an empty inventory has no stats to show`() {
        assertNull(InventoryStats.from(emptyList()))
    }

    @Test
    fun `quantity multiplies both what was spent and what it is worth`() {
        val stats = InventoryStats.from(
            listOf(listItem(purchaseMinorUnits = 1000, quantity = 3, latestMinorUnits = 1500)),
        )

        check(stats != null)
        assertEquals(3000, stats.spent.minorUnits)
        assertEquals(4500, stats.currentValue.minorUnits)
        assertEquals(1500, stats.delta.minorUnits)
    }

    @Test
    fun `an item with no reading is counted at its purchase price`() {
        val stats = InventoryStats.from(
            listOf(
                listItem(purchaseMinorUnits = 1000, quantity = 1, latestMinorUnits = 1200),
                listItem(purchaseMinorUnits = 500, quantity = 2, latestMinorUnits = null),
            ),
        )

        check(stats != null)
        assertEquals(2000, stats.spent.minorUnits)
        // 1200 for the priced item, then the unpriced one at its own purchase price: 500 x 2.
        assertEquals(2200, stats.currentValue.minorUnits)
    }

    @Test
    fun `a loss is reported as a negative delta and fraction`() {
        val stats = InventoryStats.from(
            listOf(listItem(purchaseMinorUnits = 1000, quantity = 1, latestMinorUnits = 750)),
        )

        check(stats != null)
        assertEquals(-250, stats.delta.minorUnits)
        assertEquals(-0.25f, stats.deltaFraction)
    }

    @Test
    fun `nothing spent leaves the fraction undefined rather than dividing by zero`() {
        val stats = InventoryStats.from(
            listOf(listItem(purchaseMinorUnits = 0, quantity = 1, latestMinorUnits = 500)),
        )

        check(stats != null)
        assertEquals(500, stats.delta.minorUnits)
        assertNull(stats.deltaFraction)
    }

    @Test
    fun `stats carry the currency the inventory is stored in`() {
        val stats = InventoryStats.from(
            listOf(listItem(purchaseMinorUnits = 1000, quantity = 1, latestMinorUnits = 1000)),
        )

        check(stats != null)
        assertEquals(SteamCurrency.USD, stats.currentValue.currency)
        assertEquals(SteamCurrency.USD, stats.delta.currency)
    }
}
