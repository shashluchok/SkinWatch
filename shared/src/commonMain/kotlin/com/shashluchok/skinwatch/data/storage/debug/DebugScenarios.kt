package com.shashluchok.skinwatch.data.storage.debug

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

internal const val DEBUG_ICON_URL = "https://community.akamai.steamstatic.com/economy/image/abc123"
internal const val DEBUG_NAME_PREFIX = "DEBUG │ "
private const val MIN_PRICE_MINOR_UNITS = 50L
private const val TIMESTAMP_JITTER_HOURS = 6
private const val SINGLE_SNAPSHOT_DAYS_AGO = 2
private const val SINGLE_SNAPSHOT_PRICE_MINOR_UNITS = 27_00L

internal data class DebugScenario(
    val marketHashName: String,
    val purchasePriceMinorUnits: Long,
    val snapshots: List<Pair<Instant, Long?>>,
)

internal fun debugScenarios(now: Instant): List<DebugScenario> =
    volatileScenarios(now) +
        flatScenarios(now) +
        oscillatingScenarios(now) +
        shapedScenarios(now) +
        extremeScenarios(now) +
        edgeCaseScenarios(now)

private fun volatileScenarios(now: Instant): List<DebugScenario> = listOf(
    // Long, volatile history, net up -- the "big swings, ends in profit" case.
    DebugScenario(
        marketHashName = "${DEBUG_NAME_PREFIX}Many prices, volatile, profit",
        purchasePriceMinorUnits = 30_00,
        snapshots = walk(
            now = now,
            spanDays = 70,
            count = 45,
            startMinorUnits = 28_00,
            driftMinorUnitsPerStep = 45,
            noiseMinorUnits = 400,
            seed = 1,
        ),
    ),
    // Long, volatile history, net down -- mirror of the above.
    DebugScenario(
        marketHashName = "${DEBUG_NAME_PREFIX}Many prices, volatile, loss",
        purchasePriceMinorUnits = 60_00,
        snapshots = walk(
            now = now,
            spanDays = 70,
            count = 45,
            startMinorUnits = 62_00,
            driftMinorUnitsPerStep = -50,
            noiseMinorUnits = 500,
            seed = 2,
        ),
    ),
    // Short, volatile history, profit -- few points but still a real swing between them.
    DebugScenario(
        marketHashName = "${DEBUG_NAME_PREFIX}Few prices, volatile, profit",
        purchasePriceMinorUnits = 15_00,
        snapshots = walk(
            now = now,
            spanDays = 12,
            count = 4,
            startMinorUnits = 14_00,
            driftMinorUnitsPerStep = 150,
            noiseMinorUnits = 300,
            seed = 3,
        ),
    ),
    // Short, volatile history, loss.
    DebugScenario(
        marketHashName = "${DEBUG_NAME_PREFIX}Few prices, volatile, loss",
        purchasePriceMinorUnits = 45_00,
        snapshots = walk(
            now = now,
            spanDays = 12,
            count = 4,
            startMinorUnits = 47_00,
            driftMinorUnitsPerStep = -200,
            noiseMinorUnits = 300,
            seed = 4,
        ),
    ),
)

private fun flatScenarios(now: Instant): List<DebugScenario> = listOf(
    // Long, flat/quiet history, tiny net profit -- tests the color split with a subtle line.
    DebugScenario(
        marketHashName = "${DEBUG_NAME_PREFIX}Many prices, flat, slight profit",
        purchasePriceMinorUnits = 100_00,
        snapshots = walk(
            now = now,
            spanDays = 70,
            count = 40,
            startMinorUnits = 99_50,
            driftMinorUnitsPerStep = 3,
            noiseMinorUnits = 60,
            seed = 5,
        ),
    ),
    // Long, flat/quiet history, tiny net loss.
    DebugScenario(
        marketHashName = "${DEBUG_NAME_PREFIX}Many prices, flat, slight loss",
        purchasePriceMinorUnits = 100_00,
        snapshots = walk(
            now = now,
            spanDays = 70,
            count = 40,
            startMinorUnits = 100_50,
            driftMinorUnitsPerStep = -3,
            noiseMinorUnits = 60,
            seed = 6,
        ),
    ),
    // Short, flat/quiet history -- barely moves, still needs to render sensibly at high zoom.
    DebugScenario(
        marketHashName = "${DEBUG_NAME_PREFIX}Few prices, flat",
        purchasePriceMinorUnits = 20_00,
        snapshots = walk(
            now = now,
            spanDays = 6,
            count = 3,
            startMinorUnits = 20_10,
            driftMinorUnitsPerStep = 0,
            noiseMinorUnits = 20,
            seed = 7,
        ),
    ),
)

// Periodic swings that repeatedly cross the purchase price, rather than trending to one side --
// exercises the line-color split (LineFill.double/AreaFill.double) switching color multiple times
// along the same line instead of just once, which none of the drift-biased scenarios above do.
private fun oscillatingScenarios(now: Instant): List<DebugScenario> = listOf(
    // Purchase price sits at the oscillation's center -- crosses it on every cycle.
    DebugScenario(
        marketHashName = "${DEBUG_NAME_PREFIX}Many prices, oscillating around purchase price",
        purchasePriceMinorUnits = 50_00,
        snapshots = oscillate(
            now = now,
            spanDays = 80,
            count = 50,
            centerMinorUnits = 50_00,
            amplitudeMinorUnits = 15_00,
            cycles = 4,
            noiseMinorUnits = 150,
            seed = 9,
        ),
    ),
    // Same idea, but many small fast wiggles instead of a few broad swings.
    DebugScenario(
        marketHashName = "${DEBUG_NAME_PREFIX}Many prices, choppy small oscillation",
        purchasePriceMinorUnits = 40_00,
        snapshots = oscillate(
            now = now,
            spanDays = 60,
            count = 60,
            centerMinorUnits = 40_00,
            amplitudeMinorUnits = 5_00,
            cycles = 10,
            noiseMinorUnits = 80,
            seed = 10,
        ),
    ),
    // The opposite extreme -- a couple of huge, slow swings rather than many small ones.
    DebugScenario(
        marketHashName = "${DEBUG_NAME_PREFIX}Many prices, big slow swings",
        purchasePriceMinorUnits = 80_00,
        snapshots = oscillate(
            now = now,
            spanDays = 90,
            count = 50,
            centerMinorUnits = 80_00,
            amplitudeMinorUnits = 55_00,
            cycles = 2,
            noiseMinorUnits = 200,
            seed = 11,
        ),
    ),
)

// Hand-scripted shapes (via waypoints) rather than a random walk -- specific chart silhouettes
// worth eyeballing that a biased/oscillating walk wouldn't reliably produce on its own. Each
// waypoint is a one-off illustrative (fraction, price) pair, not a value worth a named constant.
@Suppress("MagicNumber")
private fun shapedScenarios(now: Instant): List<DebugScenario> = listOf(
    // Shoots far above purchase price, then crashes back down close to where it started.
    DebugScenario(
        marketHashName = "${DEBUG_NAME_PREFIX}Sharp spike then crash",
        purchasePriceMinorUnits = 25_00,
        snapshots = piecewiseWalk(
            now = now,
            spanDays = 40,
            count = 30,
            waypoints = listOf(0f to 24_00L, 0.5f to 120_00L, 1f to 27_00L),
            noiseMinorUnits = 150,
            seed = 12,
        ),
    ),
    // Drops well below purchase price, then recovers back above it -- a V shape.
    DebugScenario(
        marketHashName = "${DEBUG_NAME_PREFIX}Crash then recovery",
        purchasePriceMinorUnits = 60_00,
        snapshots = piecewiseWalk(
            now = now,
            spanDays = 50,
            count = 35,
            waypoints = listOf(0f to 58_00L, 0.4f to 18_00L, 1f to 70_00L),
            noiseMinorUnits = 150,
            seed = 13,
        ),
    ),
    // Sits flat for most of its history, then jumps to a new level near the end -- tests a sudden
    // regime change rather than a gradual trend.
    DebugScenario(
        marketHashName = "${DEBUG_NAME_PREFIX}Long flat then sudden jump",
        purchasePriceMinorUnits = 35_00,
        snapshots = piecewiseWalk(
            now = now,
            spanDays = 90,
            count = 40,
            waypoints = listOf(0f to 34_00L, 0.75f to 35_00L, 0.8f to 80_00L, 1f to 82_00L),
            noiseMinorUnits = 80,
            seed = 14,
        ),
    ),
)

// Cases that stress the axes/formatting rather than the line's shape.
private fun extremeScenarios(now: Instant): List<DebugScenario> = listOf(
    // Enough points to check rendering doesn't get cluttered/slow with a genuinely dense history.
    DebugScenario(
        marketHashName = "${DEBUG_NAME_PREFIX}Very dense history (150 points)",
        purchasePriceMinorUnits = 45_00,
        snapshots = walk(
            now = now,
            spanDays = 90,
            count = 150,
            startMinorUnits = 43_00,
            driftMinorUnitsPerStep = 5,
            noiseMinorUnits = 200,
            seed = 15,
        ),
    ),
    // Low end of the Y-axis -- prices measured in single rubles, close to the price floor.
    DebugScenario(
        marketHashName = "${DEBUG_NAME_PREFIX}Very cheap item",
        purchasePriceMinorUnits = 1_50,
        snapshots = walk(
            now = now,
            spanDays = 30,
            count = 20,
            startMinorUnits = 1_20,
            driftMinorUnitsPerStep = 2,
            noiseMinorUnits = 30,
            seed = 16,
        ),
    ),
    // High end of the Y-axis -- a expensive item with four-figure ruble prices.
    DebugScenario(
        marketHashName = "${DEBUG_NAME_PREFIX}Very expensive item",
        purchasePriceMinorUnits = 15_000_00,
        snapshots = walk(
            now = now,
            spanDays = 60,
            count = 35,
            startMinorUnits = 14_500_00,
            driftMinorUnitsPerStep = 800,
            noiseMinorUnits = 12_000,
            seed = 17,
        ),
    ),
)

private fun edgeCaseScenarios(now: Instant): List<DebugScenario> = listOf(
    // Exactly at purchase price -- the "neutral" glyph-color branch (neither profit nor loss).
    DebugScenario(
        marketHashName = "${DEBUG_NAME_PREFIX}Breakeven",
        purchasePriceMinorUnits = 50_00,
        snapshots = walk(
            now = now,
            spanDays = 20,
            count = 10,
            startMinorUnits = 50_00,
            driftMinorUnitsPerStep = 0,
            noiseMinorUnits = 0,
            seed = 8,
        ),
    ),
    // A single reading -- exercises the chart's single-point layout, not the multi-point line.
    DebugScenario(
        marketHashName = "${DEBUG_NAME_PREFIX}Single snapshot",
        purchasePriceMinorUnits = 25_00,
        snapshots = listOf(now - SINGLE_SNAPSHOT_DAYS_AGO.days to SINGLE_SNAPSHOT_PRICE_MINOR_UNITS),
    ),
    // No readings at all -- exercises the chart's empty state.
    DebugScenario(
        marketHashName = "${DEBUG_NAME_PREFIX}No snapshots yet",
        purchasePriceMinorUnits = 10_00,
        snapshots = emptyList(),
    ),
)

/**
 * A simple biased random walk: [count] readings spread irregularly (not evenly) across the last
 * [spanDays], each step drifting by [driftMinorUnitsPerStep] on average plus up to
 * [noiseMinorUnits] of jitter either way -- close enough to how real Steam prices wobble day to
 * day to be useful for eyeballing the chart, without needing real market data.
 */
private fun walk(
    now: Instant,
    spanDays: Int,
    count: Int,
    startMinorUnits: Long,
    driftMinorUnitsPerStep: Long,
    noiseMinorUnits: Long,
    seed: Int,
): List<Pair<Instant, Long?>> {
    if (count == 0) return emptyList()
    val random = Random(seed)
    var priceMinorUnits = startMinorUnits
    val offsetsBackFromNow = (0 until count)
        .map { index ->
            val idealDaysAgo = spanDays * (count - index) / count.toDouble()
            val jitterHours = random.nextInt(-TIMESTAMP_JITTER_HOURS, TIMESTAMP_JITTER_HOURS).hours
            (idealDaysAgo.days) - jitterHours
        }.sortedDescending()
    return offsetsBackFromNow.map { offset ->
        val jitter = if (noiseMinorUnits > 0) random.nextLong(-noiseMinorUnits, noiseMinorUnits) else 0L
        priceMinorUnits = (priceMinorUnits + driftMinorUnitsPerStep + jitter).coerceAtLeast(MIN_PRICE_MINOR_UNITS)
        (now - offset) to priceMinorUnits
    }
}

/**
 * [count] readings spread irregularly across the last [spanDays], following a sine wave centered
 * on [centerMinorUnits] with the given [amplitudeMinorUnits] and number of full [cycles] over the
 * whole span, plus up to [noiseMinorUnits] of jitter -- unlike [walk], this reliably crosses
 * [centerMinorUnits] repeatedly instead of drifting to one side, useful for a purchase price the
 * line should cross back and forth rather than settle above or below.
 */
private fun oscillate(
    now: Instant,
    spanDays: Int,
    count: Int,
    centerMinorUnits: Long,
    amplitudeMinorUnits: Long,
    cycles: Int,
    noiseMinorUnits: Long,
    seed: Int,
): List<Pair<Instant, Long?>> {
    if (count == 0) return emptyList()
    val random = Random(seed)
    val offsetsBackFromNow = jitteredOffsetsBackFromNow(spanDays = spanDays, count = count, random = random)
    return offsetsBackFromNow.mapIndexed { index, offset ->
        val phase = 2.0 * PI * cycles * index / (count - 1).coerceAtLeast(1)
        val jitter = if (noiseMinorUnits > 0) random.nextLong(-noiseMinorUnits, noiseMinorUnits) else 0L
        val price = (centerMinorUnits + (amplitudeMinorUnits * sin(phase)).toLong() + jitter)
            .coerceAtLeast(MIN_PRICE_MINOR_UNITS)
        (now - offset) to price
    }
}

/**
 * [count] readings spread irregularly across the last [spanDays], linearly interpolated between
 * [waypoints] (fraction of the span from `0f` to `1f`, mapped to a target price) plus up to
 * [noiseMinorUnits] of jitter -- for hand-scripted shapes (a spike, a crash-and-recover, a sudden
 * jump) that a random/periodic walk wouldn't reliably produce.
 */
private fun piecewiseWalk(
    now: Instant,
    spanDays: Int,
    count: Int,
    waypoints: List<Pair<Float, Long>>,
    noiseMinorUnits: Long,
    seed: Int,
): List<Pair<Instant, Long?>> {
    if (count == 0) return emptyList()
    val random = Random(seed)
    val offsetsBackFromNow = jitteredOffsetsBackFromNow(spanDays = spanDays, count = count, random = random)
    return offsetsBackFromNow.mapIndexed { index, offset ->
        val fraction = index / (count - 1f).coerceAtLeast(1f)
        val jitter = if (noiseMinorUnits > 0) random.nextLong(-noiseMinorUnits, noiseMinorUnits) else 0L
        val price = (interpolateWaypoints(waypoints, fraction) + jitter).coerceAtLeast(MIN_PRICE_MINOR_UNITS)
        (now - offset) to price
    }
}

private fun interpolateWaypoints(waypoints: List<Pair<Float, Long>>, fraction: Float): Long {
    val (beforeFraction, beforePrice) = waypoints.last { it.first <= fraction }
    val after = waypoints.firstOrNull { it.first > fraction } ?: return beforePrice
    val (afterFraction, afterPrice) = after
    val segmentProgress = ((fraction - beforeFraction) / (afterFraction - beforeFraction)).coerceIn(0f, 1f)
    return beforePrice + ((afterPrice - beforePrice) * segmentProgress).toLong()
}

/** Shared by [oscillate] and [piecewiseWalk]: the same irregular-but-chronological spacing [walk] uses. */
private fun jitteredOffsetsBackFromNow(spanDays: Int, count: Int, random: Random): List<Duration> =
    (0 until count)
        .map { index ->
            val idealDaysAgo = spanDays * (count - index) / count.toDouble()
            val jitterHours = random.nextInt(-TIMESTAMP_JITTER_HOURS, TIMESTAMP_JITTER_HOURS).hours
            (idealDaysAgo.days) - jitterHours
        }.sortedDescending()
