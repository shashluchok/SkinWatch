package com.shashluchok.skinwatch.presentation.screen.inventory.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshot
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.presentation.theme.LocalSemanticColors

private const val BAR_MIN_SCALE = 0.65f
private const val BAR_BREATH_DURATION_MS = 1400
private const val BAR_PHASE_STEP_MS = 260
private val BAR_GAP = 1.dp
private val BAR_CORNER_RADIUS = 1.dp
private const val BAR_HEIGHT_SHORTEST = 0.4f
private const val BAR_HEIGHT_SHORT = 0.6f
private const val BAR_HEIGHT_TALL = 0.8f
private const val BAR_HEIGHT_TALLEST = 1f
private const val BAR_HEIGHT_FLAT_LOW = 0.65f
private const val BAR_HEIGHT_FLAT_HIGH = 0.75f
private val upHeights = listOf(BAR_HEIGHT_SHORTEST, BAR_HEIGHT_SHORT, BAR_HEIGHT_TALL, BAR_HEIGHT_TALLEST)
private val downHeights = listOf(BAR_HEIGHT_TALLEST, BAR_HEIGHT_TALL, BAR_HEIGHT_SHORT, BAR_HEIGHT_SHORTEST)
private val neutralHeights =
    listOf(BAR_HEIGHT_FLAT_LOW, BAR_HEIGHT_FLAT_HIGH, BAR_HEIGHT_FLAT_LOW, BAR_HEIGHT_FLAT_HIGH)

internal enum class PriceTrend { UP, DOWN, NEUTRAL }

internal fun priceTrend(latestSnapshot: PriceSnapshot?, purchasePrice: Money): PriceTrend {
    val lowestPrice = latestSnapshot?.lowestPrice ?: return PriceTrend.NEUTRAL
    return when {
        lowestPrice.minorUnits > purchasePrice.minorUnits -> PriceTrend.UP
        lowestPrice.minorUnits < purchasePrice.minorUnits -> PriceTrend.DOWN
        else -> PriceTrend.NEUTRAL
    }
}

/**
 * Small animated bar-chart hint glyph. Each bar breathes on its own continuous, gap-free
 * `infiniteRepeatable` reverse loop, phase-shifted via [StartOffsetType.FastForward] from its
 * neighbors -- driving all four off one shared timeline (e.g. a single Lottie composition) leaves
 * some bars holding still while others catch up, since each would only occupy a sub-window of that
 * shared timeline.
 */
@Composable
internal fun PriceTrendGlyph(
    trend: PriceTrend,
    modifier: Modifier = Modifier,
) {
    val semanticColors = LocalSemanticColors.current
    val neutralColor = MaterialTheme.colorScheme.onSurfaceVariant
    val color = when (trend) {
        PriceTrend.UP -> semanticColors.positive
        PriceTrend.DOWN -> semanticColors.negative
        PriceTrend.NEUTRAL -> neutralColor
    }
    val heightFractions = when (trend) {
        PriceTrend.UP -> upHeights
        PriceTrend.DOWN -> downHeights
        PriceTrend.NEUTRAL -> neutralHeights
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(BAR_GAP),
        verticalAlignment = Alignment.Bottom,
    ) {
        heightFractions.forEachIndexed { index, heightFraction ->
            val transition = rememberInfiniteTransition()
            val scale by transition.animateFloat(
                initialValue = BAR_MIN_SCALE,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = BAR_BREATH_DURATION_MS, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(
                        offsetMillis = index * BAR_PHASE_STEP_MS,
                        offsetType = StartOffsetType.FastForward,
                    ),
                ),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(heightFraction)
                    .graphicsLayer {
                        scaleY = scale
                        transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 1f)
                    }.background(color = color, shape = RoundedCornerShape(BAR_CORNER_RADIUS)),
            )
        }
    }
}
