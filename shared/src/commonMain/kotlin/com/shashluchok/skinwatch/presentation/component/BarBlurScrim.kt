package com.shashluchok.skinwatch.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shashluchok.skinwatch.presentation.theme.LocalMotion
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur

private const val SCRIM_GRADIENT_STOP_COUNT = 14
private val BLUR_RADIUS = 16.dp

/**
 * How far past the bar's inner edge the scrim reaches, so the blur has already begun by the time
 * content arrives at the bar rather than starting abruptly at its edge.
 */
private val BLUR_SCRIM_OVERFLOW = 12.dp

/** Which screen edge the bar sits against, and therefore which way its scrim fades out. */
internal enum class BarEdge { Top, Bottom }

/**
 * The frosted backdrop shared by the app's top and bottom bars: content passing underneath is
 * blurred and faded into the bar's own colour rather than sliding under a hard opaque edge.
 *
 * Kept in one place so the two bars cannot drift apart -- they are read as a matching pair, and a
 * difference in radius, gradient shape or overflow between them is immediately visible.
 *
 * Call from a [BoxScope] that lays out the bar itself; the scrim sizes itself to that box.
 */
@Composable
internal fun BoxScope.BarBlurScrim(
    hazeState: HazeState,
    containerColor: Color,
    edge: BarEdge,
    modifier: Modifier = Modifier,
) {
    val gradientColorStops = rememberScrimGradientColorStops(containerColor = containerColor, edge = edge)
    val progressive = when (edge) {
        BarEdge.Top -> HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
        BarEdge.Bottom -> HazeProgressive.verticalGradient(startIntensity = 0f, endIntensity = 1f)
    }

    OverflowingInward(edge = edge, extraHeight = BLUR_SCRIM_OVERFLOW, modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .hazeBlur(
                    input = HazeInput.Sources(hazeState),
                    performanceMode = HazePerformanceMode.Balanced,
                    style = HazeBlurStyle {
                        blurRadius(BLUR_RADIUS)
                        progressive(progressive)
                    },
                ),
        ) {
            drawRect(brush = Brush.verticalGradient(colorStops = gradientColorStops))
        }
    }
}

/**
 * Lays the scrim out [extraHeight] taller than the bar and lets it hang past the bar's inner edge,
 * while still reporting the bar's own height so nothing else is pushed around by it.
 */
@Composable
private fun BoxScope.OverflowingInward(
    edge: BarEdge,
    extraHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        content = content,
        modifier = modifier.matchParentSize(),
    ) { measurables, constraints ->
        val extraHeightPx = extraHeight.roundToPx()
        val childConstraints = Constraints.fixed(
            width = constraints.maxWidth,
            height = constraints.maxHeight + extraHeightPx,
        )
        val placeable = measurables.single().measure(childConstraints)
        // A bottom bar grows upward out of its box, a top bar downward; either way the box keeps the
        // height it was given.
        val offsetY = if (edge == BarEdge.Bottom) -extraHeightPx else 0

        layout(width = constraints.maxWidth, height = constraints.maxHeight) {
            placeable.placeRelative(x = 0, y = offsetY)
        }
    }
}

/**
 * Opaque at the screen edge, clear at the inner one, eased rather than linear so the fade does not
 * band across the handful of stops it is drawn with.
 */
@Composable
private fun rememberScrimGradientColorStops(
    containerColor: Color,
    edge: BarEdge,
): Array<Pair<Float, Color>> {
    val easing = LocalMotion.current.easing.standard

    return remember(containerColor, easing, edge) {
        Array(SCRIM_GRADIENT_STOP_COUNT) { index ->
            val fraction = index / (SCRIM_GRADIENT_STOP_COUNT - 1f)
            val opacityFraction = if (edge == BarEdge.Bottom) fraction else 1f - fraction

            fraction to containerColor.copy(alpha = easing.transform(opacityFraction))
        }
    }
}
