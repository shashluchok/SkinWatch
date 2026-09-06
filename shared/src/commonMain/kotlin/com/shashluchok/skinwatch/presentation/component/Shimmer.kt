package com.shashluchok.skinwatch.presentation.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.shashluchok.skinwatch.presentation.theme.LocalDimens

private const val SHIMMER_DURATION_MS = 1200
private const val SHIMMER_HIGHLIGHT_ALPHA = 0.35f
private const val SHIMMER_SWEEP_START = -1f
private const val SHIMMER_SWEEP_END = 2f

/**
 * One sweep shared by every placeholder in a skeleton.
 *
 * Returned as a lambda rather than a value so callers read it inside `drawBehind`, keeping the
 * animation in the draw phase instead of recomposing the whole skeleton every frame. A skeleton
 * should also hoist this once at its root: an infinite transition per placeholder means dozens of
 * independent animations that visibly drift out of step.
 */
@Composable
internal fun rememberShimmerSweep(): () -> Float {
    val transition = rememberInfiniteTransition()
    val sweep = transition.animateFloat(
        initialValue = SHIMMER_SWEEP_START,
        targetValue = SHIMMER_SWEEP_END,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_DURATION_MS, easing = LinearEasing),
        ),
    )

    return remember(sweep) { { sweep.value } }
}

@Composable
internal fun ShimmerBlock(
    sweep: () -> Float,
    modifier: Modifier = Modifier,
) {
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val highlightColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = SHIMMER_HIGHLIGHT_ALPHA)

    Box(
        modifier = modifier.drawBehind {
            val sweepX = sweep() * size.width
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(baseColor, highlightColor, baseColor),
                    start = Offset(x = sweepX - size.width, y = 0f),
                    end = Offset(x = sweepX + size.width, y = 0f),
                ),
            )
        },
    )
}

/**
 * Occupies exactly one line of [lineHeight], drawing the bar inset within it so a skeleton's total
 * height matches the text it stands in for.
 */
@Composable
internal fun ShimmerLine(
    sweep: () -> Float,
    lineHeight: TextUnit,
    widthFraction: Float,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current
    val height = with(LocalDensity.current) { lineHeight.toDp() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.CenterStart,
    ) {
        ShimmerBlock(
            sweep = sweep,
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .height((height - dimens.padding.extraSmall).coerceAtLeast(0.dp))
                .clip(RoundedCornerShape(dimens.radius.small)),
        )
    }
}
