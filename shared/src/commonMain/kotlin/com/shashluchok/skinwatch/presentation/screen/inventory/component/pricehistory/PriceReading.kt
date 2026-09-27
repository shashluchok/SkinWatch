package com.shashluchok.skinwatch.presentation.screen.inventory.component.pricehistory

import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshot
import com.shashluchok.skinwatch.domain.steam.Money
import kotlin.time.Instant

/**
 * A snapshot that actually carries a price, and so has something to show.
 *
 * Steam answers with no price at all for an item with no live listings. Nothing is recorded for such
 * an answer any more, but a history taken before that was true still holds priceless snapshots --
 * and a priceless snapshot is not a reading. Keeping the distinction in the type is what stops one
 * being drawn as a price of zero.
 */
internal data class PriceReading(
    val price: Money,
    val capturedAt: Instant,
)

internal fun List<PriceSnapshot>.toPriceReadings(): List<PriceReading> = mapNotNull { snapshot ->
    snapshot.lowestPrice?.let { PriceReading(price = it, capturedAt = snapshot.capturedAt) }
}
