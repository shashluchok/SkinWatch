package com.shashluchok.skinwatch.presentation.screen.inventory.component.pricehistory

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_inventory__price_history_detail__empty_state
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

private val SWEEP_LINE_STROKE = 1.8.dp
private val SWEEP_TRAIL_WIDTH = 104.dp
private val GHOST_LINE_STROKE = 2.2.dp
private val GHOST_POINT_RADIUS = 3.2.dp
private val GHOST_REFERENCE_STROKE = 1.dp
private val GHOST_REVEAL_WIDTH = 150.dp

private const val TRAIL_MID_ALPHA = 0.16f
private const val TRAIL_EDGE_ALPHA = 0.62f
private const val SWEEP_DURATION_MS = 5400

// The sweep starts just off the left edge and exits past the right one.
private const val SWEEP_START_FRACTION = -0.08f
private const val SWEEP_TRAVEL_FRACTION = 1.14f

private const val GHOST_ALPHA = 0.62f
private const val GHOST_AREA_TOP_ALPHA = 0.20f
private const val GHOST_REFERENCE_ALPHA = 0.34f
private const val GHOST_POINT_COUNT = 8
private const val GHOST_INSET_FRACTION = 0.06f
private const val GHOST_TOP_FRACTION = 0.22f
private const val GHOST_BOTTOM_FRACTION = 0.86f
private const val GHOST_REFERENCE_FRACTION = 0.62f

// Two waves of unrelated frequency: the readings come out irregular the way real ones are, rather
// than tracing one obvious curve.
private const val GHOST_WAVE_FAST_STEP = 1.9f
private const val GHOST_WAVE_SLOW_STEP = 0.7f
private const val GHOST_WAVE_FAST_WEIGHT = 0.34f
private const val GHOST_WAVE_SLOW_WEIGHT = 0.66f

private const val REVEAL_PEAK_STOP = 0.96f
private const val BEAM_TAPER_MIDPOINT = 0.5f
private const val BEAM_ALPHA_FALLOFF = 0.55f

/**
 * The instrument, idling: a tapered beam crossing the empty frame, with the shape of a price chart
 * surfacing under it and dimming again behind.
 *
 * The ghost is drawn in one muted colour rather than in the live profit/loss palette -- it carries
 * the chart's form so the scan reads as a scan, without dressing up as an actual reading.
 *
 * Occupies [CHART_HEIGHT] so the island stays one size across every state the modal can show.
 */
@Composable
internal fun PriceHistoryEmptyState(modifier: Modifier = Modifier) {
    val dimens = LocalDimens.current
    val transition = rememberInfiniteTransition()
    val sweep = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            // Linear on purpose: an ease-in-out spends its slowest moments at the edges, which is
            // exactly where the beam is dimmest, and the two together read as a stall before
            // anything starts.
            animation = tween(durationMillis = SWEEP_DURATION_MS, easing = LinearEasing),
        ),
    )
    // Read in the draw lambda rather than in composition: the sweep runs every frame, and this
    // keeps it out of recomposition entirely.
    val sweepProvider = remember(sweep) { { sweep.value } }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CHART_HEIGHT)
            .testTag(PriceHistoryDetailScreen.Tag.EMPTY_STATE),
        contentAlignment = Alignment.Center,
    ) {
        ScanBackdrop(sweep = sweepProvider)
        Text(
            modifier = Modifier.padding(horizontal = dimens.padding.large),
            text = stringResource(Res.string.dev__screen_inventory__price_history_detail__empty_state),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BoxScope.ScanBackdrop(sweep: () -> Float) {
    val accentColor = MaterialTheme.colorScheme.primary
    val ghostColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = Modifier.matchParentSize()) {
        val progress = sweep()
        val sweepX = (SWEEP_START_FRACTION + progress * SWEEP_TRAVEL_FRACTION) * size.width
        // Brightest passing the middle, gone by either edge. Flattened so it reaches a readable
        // brightness early instead of creeping in over the first second.
        val beamAlpha = sin(progress * PI.toFloat()).pow(BEAM_ALPHA_FALLOFF)

        drawGhostChart(sweepX = sweepX, color = ghostColor, alpha = beamAlpha)
        drawBeam(sweepX = sweepX, color = accentColor, alpha = beamAlpha)
    }
}

/**
 * Paints the chart at full strength into its own layer, then keeps only the part the beam has just
 * passed over. Masking after the fact is what lets the area fill keep its own vertical gradient --
 * one brush cannot fade in two directions at once.
 */
private fun DrawScope.drawGhostChart(sweepX: Float, color: Color, alpha: Float) {
    if (alpha <= 0f) return

    val points = ghostPoints()
    val canvas = drawContext.canvas
    canvas.saveLayer(bounds = Rect(offset = Offset.Zero, size = size), paint = Paint())

    val areaPath = Path().apply {
        moveTo(x = points.first().x, y = size.height)
        points.forEach { lineTo(x = it.x, y = it.y) }
        lineTo(x = points.last().x, y = size.height)
        close()
    }
    drawPath(
        path = areaPath,
        brush = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = GHOST_AREA_TOP_ALPHA), Color.Transparent),
            startY = points.minOf { it.y },
            endY = size.height,
        ),
    )

    val referenceY = size.height * GHOST_REFERENCE_FRACTION
    drawLine(
        color = color.copy(alpha = GHOST_REFERENCE_ALPHA),
        start = Offset(x = 0f, y = referenceY),
        end = Offset(x = size.width, y = referenceY),
        strokeWidth = GHOST_REFERENCE_STROKE.toPx(),
    )

    val linePath = Path().apply {
        moveTo(x = points.first().x, y = points.first().y)
        points.drop(1).forEach { lineTo(x = it.x, y = it.y) }
    }
    drawPath(
        path = linePath,
        color = color.copy(alpha = GHOST_ALPHA),
        style = Stroke(width = GHOST_LINE_STROKE.toPx()),
    )
    points.forEach { point ->
        drawCircle(
            color = color.copy(alpha = GHOST_ALPHA),
            radius = GHOST_POINT_RADIUS.toPx(),
            center = point,
        )
    }

    // Everything outside the trailing band is erased, so the chart exists only where the beam has
    // just been and dissolves as it moves on.
    drawRect(
        brush = Brush.horizontalGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                REVEAL_PEAK_STOP to Color.Black.copy(alpha = alpha),
                1f to Color.Transparent,
            ),
            startX = sweepX - GHOST_REVEAL_WIDTH.toPx(),
            endX = sweepX,
        ),
        blendMode = BlendMode.DstIn,
    )
    canvas.restore()
}

/** Tapered rather than square-ended: both the trail and the leading edge fade out top and bottom. */
private fun DrawScope.drawBeam(sweepX: Float, color: Color, alpha: Float) {
    if (alpha <= 0f) return

    val trailWidth = SWEEP_TRAIL_WIDTH.toPx()
    val canvas = drawContext.canvas
    canvas.saveLayer(bounds = Rect(offset = Offset.Zero, size = size), paint = Paint())

    drawRect(
        brush = Brush.horizontalGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                TRAIL_EDGE_ALPHA to color.copy(alpha = TRAIL_MID_ALPHA),
                1f to color.copy(alpha = TRAIL_EDGE_ALPHA),
            ),
            startX = sweepX - trailWidth,
            endX = sweepX,
        ),
        topLeft = Offset(x = sweepX - trailWidth, y = 0f),
        size = Size(width = trailWidth, height = size.height),
    )
    drawLine(
        color = color,
        start = Offset(x = sweepX, y = 0f),
        end = Offset(x = sweepX, y = size.height),
        strokeWidth = SWEEP_LINE_STROKE.toPx(),
    )

    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                BEAM_TAPER_MIDPOINT to Color.Black.copy(alpha = alpha),
                1f to Color.Transparent,
            ),
        ),
        blendMode = BlendMode.DstIn,
    )
    canvas.restore()
}

private fun DrawScope.ghostPoints(): List<Offset> {
    val inset = size.width * GHOST_INSET_FRACTION
    val span = size.width - inset * 2f
    val top = size.height * GHOST_TOP_FRACTION
    val bottom = size.height * GHOST_BOTTOM_FRACTION

    return List(GHOST_POINT_COUNT) { index ->
        val wave = sin(index * GHOST_WAVE_FAST_STEP) * GHOST_WAVE_FAST_WEIGHT +
            sin(index * GHOST_WAVE_SLOW_STEP) * GHOST_WAVE_SLOW_WEIGHT
        val level = (wave + 1f) / 2f

        Offset(
            x = inset + span * index / (GHOST_POINT_COUNT - 1f),
            y = bottom - (bottom - top) * level,
        )
    }
}
