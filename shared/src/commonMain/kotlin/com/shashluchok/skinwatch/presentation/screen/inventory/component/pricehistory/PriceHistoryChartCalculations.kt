package com.shashluchok.skinwatch.presentation.screen.inventory.component.pricehistory

import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshot
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import com.shashluchok.skinwatch.presentation.util.pad2
import com.shashluchok.skinwatch.presentation.util.toFullDateLabel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong
import kotlin.time.Instant

private const val MINOR_UNITS_PER_MAJOR_UNIT = 100.0

/** Headroom above the higher of the all-time market high and the purchase price, per the requested 15-20% range. */
private const val Y_AXIS_HEADROOM_MULTIPLIER = 1.18

/**
 * Tolerance (in major currency units) for treating a Y-axis tick as "the purchase price" -- guards
 * against floating-point noise from the minor-to-major-unit division, not a real price difference.
 */
internal const val PURCHASE_PRICE_TICK_EPSILON = 0.01

/**
 * The Y-axis maximum: the higher of the all-time highest `lowestPrice` reading and the purchase
 * price, plus [Y_AXIS_HEADROOM_MULTIPLIER] headroom, both in major currency units. A missing
 * purchase price simply drops out of the comparison rather than forcing a fallback value, since
 * `0` can never be the larger of the two once any priced snapshot exists.
 */
internal fun priceHistoryYAxisMax(snapshots: List<PriceSnapshot>, purchasePrice: Money?): Double {
    val maxLowestPriceMajorUnits = snapshots
        .maxOfOrNull { (it.lowestPrice?.minorUnits ?: 0L) / MINOR_UNITS_PER_MAJOR_UNIT }
        ?: 0.0
    val purchasePriceMajorUnits = (purchasePrice?.minorUnits ?: 0L) / MINOR_UNITS_PER_MAJOR_UNIT
    return max(maxLowestPriceMajorUnits, purchasePriceMajorUnits) * Y_AXIS_HEADROOM_MULTIPLIER
}

/** Total Y-axis labels when the axis is centered on the purchase price -- always odd, so one lands exactly on it. */
internal const val PURCHASE_PRICE_CENTERED_TICK_COUNT = 7

/** Labels separating the centered purchase price from the bottom/top-most label, one side at a time. */
private const val PURCHASE_PRICE_CENTERED_STEPS_PER_SIDE = (PURCHASE_PRICE_CENTERED_TICK_COUNT - 1) / 2

/**
 * Fallback step (as a fraction of the purchase price), used only when every reading and the
 * purchase price are the exact same value -- otherwise the axis would collapse onto a single point.
 */
private const val DEGENERATE_STEP_FRACTION = 0.05

/**
 * The all-time lowest/highest `lowestPrice` reading, in major currency units -- `0.0..0.0` if every
 * snapshot is missing a price (shouldn't happen in practice: the chart only renders once at least
 * one snapshot is priced, see `PriceHistoryDetailScreen`).
 */
internal fun priceHistoryReadingRange(snapshots: List<PriceSnapshot>): ClosedFloatingPointRange<Double> {
    val prices = snapshots.mapNotNull { it.lowestPrice?.minorUnits }.map { it / MINOR_UNITS_PER_MAJOR_UNIT }
    return (prices.minOrNull() ?: 0.0)..(prices.maxOrNull() ?: 0.0)
}

/**
 * The single Y-axis step used both below and above a centered purchase price:
 * [PURCHASE_PRICE_CENTERED_STEPS_PER_SIDE] equal steps separate it from the bottom/top-most label,
 * one of which is pure margin -- so only `[PURCHASE_PRICE_CENTERED_STEPS_PER_SIDE] - 1` steps need
 * to literally cover the real distance to [minPrice]/[maxPrice]. Whichever side needs the bigger
 * step to do that wins, so that side ends up with exactly one step of margin beyond its real
 * reading, and the other side gets no less. Falls back to [DEGENERATE_STEP_FRACTION] of
 * [purchasePrice] when neither side needs a positive step (every reading equals the purchase price).
 */
internal fun purchasePriceCenteredYAxisStep(minPrice: Double, maxPrice: Double, purchasePrice: Double): Double {
    val realSteps = PURCHASE_PRICE_CENTERED_STEPS_PER_SIDE - 1
    val stepToCoverMin = (purchasePrice - minPrice) / realSteps
    val stepToCoverMax = (maxPrice - purchasePrice) / realSteps
    val step = max(stepToCoverMin, stepToCoverMax)
    return if (step > 0.0) step else purchasePrice * DEGENERATE_STEP_FRACTION
}

/**
 * The Y-axis range for a purchase-price-centered chart: [purchasePrice] plus/minus
 * [PURCHASE_PRICE_CENTERED_STEPS_PER_SIDE] increments of [step]. Paired with a
 * `VerticalAxis.ItemPlacer.count`(`count = { PURCHASE_PRICE_CENTERED_TICK_COUNT }`) (see
 * `priceHistoryStartAxisItemPlacer`), which places that many evenly spaced labels across exactly
 * this range, landing the purchase price exactly on the centered one -- except when [step] is large
 * enough that the lower bound would go below `0.0` (a real price never does), in which case the
 * lower bound is clamped there and the purchase price is no longer exactly centered. That only
 * happens for a history swung heavily to one side of the purchase price, and an off-center axis
 * beats a negative one, which the chart library fails to render at all.
 */
internal fun purchasePriceCenteredYAxisRange(purchasePrice: Double, step: Double): ClosedFloatingPointRange<Double> {
    val halfRange = PURCHASE_PRICE_CENTERED_STEPS_PER_SIDE * step
    val start = (purchasePrice - halfRange).coerceAtLeast(0.0)
    return start..(purchasePrice + halfRange)
}

/**
 * How many evenly spaced labels fit along an axis of [axisLength] when each one needs
 * [maxLabelLength] of room plus [minLabelGap] of clearance from its neighbours -- always at least
 * 2, so the axis keeps a first and last label (e.g. the earliest and latest reading) even when
 * there's no room for more.
 */
internal fun maxEvenlySpacedLabelCount(axisLength: Float, maxLabelLength: Float, minLabelGap: Float): Int =
    (axisLength / (maxLabelLength + minLabelGap)).toInt().coerceAtLeast(2)

/**
 * [count] values evenly spaced across `[minX, maxX]` inclusive -- used for the X axis's date
 * labels. An earlier version snapped each division point to the nearest real reading instead, so a
 * label would never appear for a date with no snapshot; in practice that made the spacing visibly
 * uneven whenever readings weren't themselves evenly distributed in time (two labels could snap to
 * neighbouring readings close together while another gap stayed much wider than average). Even
 * spacing in time reads better than exact reading alignment, so labels are no longer snapped.
 */
internal fun evenlySpacedXValues(minX: Double, maxX: Double, count: Int): List<Double> {
    if (count <= 1 || minX == maxX) return listOf(maxX)
    val step = (maxX - minX) / (count - 1)
    return (0 until count).map { index -> minX + index * step }
}

/** Divisor/multiplier for rounding a price to 1 decimal digit -- see [formatAxisPrice]. */
private const val TENTHS_PER_UNIT = 10

/**
 * Rounds [value] to exactly 1 decimal digit, always shown (even for a whole number) -- used for the
 * Y axis's price labels, whose values are computed from step arithmetic (see
 * [purchasePriceCenteredYAxisStep]) and can otherwise land on long, ugly fractions.
 */
internal fun formatAxisPrice(value: Double): String {
    val roundedTenths = (value * TENTHS_PER_UNIT).roundToLong()
    val sign = if (roundedTenths < 0) "-" else ""
    val absTenths = abs(roundedTenths)
    val whole = absTenths / TENTHS_PER_UNIT
    val fraction = absTenths % TENTHS_PER_UNIT
    return "$sign$whole.$fraction"
}

/** "dd.MM", fixed UTC -- see design addendum section 1 for why UTC (not local timezone). */
internal fun Instant.toAxisDateLabel(): String {
    val date = toLocalDateTime(TimeZone.UTC).date
    return "${date.day.pad2()}.${date.month.number.pad2()}"
}

/**
 * "HH:mm", fixed UTC -- same rationale as [toAxisDateLabel]. The X axis itself only shows the date
 * (see `priceHistoryBottomAxis`); this is used alongside `toFullDateLabel` in the tap tooltip,
 * where the exact time is worth the extra line.
 */
internal fun Instant.toTimeLabel(): String {
    val time = toLocalDateTime(TimeZone.UTC)
    return "${time.hour.pad2()}:${time.minute.pad2()}"
}

/**
 * "<price> <currency>" -- the tap tooltip's first line, tinted per [priceComparedToPurchase] in
 * `priceHistoryMarker`.
 */
internal fun formatPriceHistoryTooltipPrice(price: Double, currency: SteamCurrency): String =
    "$price ${currency.name}"

/** "<dd.MM.yyyy> <HH:mm>" -- the tap tooltip's second line, the exact reading timestamp. */
internal fun formatPriceHistoryTooltipTimestamp(capturedAt: Instant): String =
    "${capturedAt.toFullDateLabel()} ${capturedAt.toTimeLabel()}"

/**
 * Whether [price] is above, below, or exactly at [purchasePrice] -- drives the tooltip price's
 * tint in `priceHistoryMarker`.
 */
internal enum class PriceComparedToPurchase { ABOVE, BELOW, EQUAL }

internal fun priceComparedToPurchase(price: Double, purchasePrice: Double): PriceComparedToPurchase = when {
    price > purchasePrice -> PriceComparedToPurchase.ABOVE
    price < purchasePrice -> PriceComparedToPurchase.BELOW
    else -> PriceComparedToPurchase.EQUAL
}
