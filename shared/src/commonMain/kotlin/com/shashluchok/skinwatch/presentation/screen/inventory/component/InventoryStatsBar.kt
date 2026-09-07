package com.shashluchok.skinwatch.presentation.screen.inventory.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.shashluchok.skinwatch.domain.inventory.InventoryStats
import com.shashluchok.skinwatch.presentation.component.AnimatedFadeText
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.presentation.theme.LocalMotion
import com.shashluchok.skinwatch.presentation.theme.LocalSemanticColors
import com.shashluchok.skinwatch.presentation.theme.tabularNumeric
import com.shashluchok.skinwatch.presentation.util.formatMoney
import com.shashluchok.skinwatch.presentation.util.formatSignedDelta
import kotlin.time.Duration.Companion.milliseconds

// The three figures fade one after another rather than as a block: the total is the one being read,
// and the cost and the difference land after it as its consequences.
private val SPENT_START_DELAY = 80.milliseconds
private val DELTA_START_DELAY = 160.milliseconds
private const val DELTA_COLOR_LABEL = "InventoryStatsBar.deltaColor"

/**
 * Inventory totals, shown inside the app top bar to the right of the tab title.
 *
 * Right-aligned and sized to its content: the title keeps the left of the bar, and the figures line
 * up against the opposite edge so the two read as a header row rather than as a stack.
 *
 * Draws no background of its own -- the bar it sits in already provides one, and the list is meant
 * to be seen passing under the blur rather than meeting an opaque edge.
 */
@Composable
internal fun InventoryStatsBar(stats: InventoryStats, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.testTag(InventoryStatsBar.Tag.ROOT),
        horizontalAlignment = Alignment.End,
    ) {
        // Set in the tab title's own style: the two sit on one line of the bar and read as a single
        // header -- what you are looking at on the left, what it is worth on the right.
        AnimatedFadeText(
            modifier = Modifier.testTag(InventoryStatsBar.Tag.CURRENT_VALUE),
            text = formatMoney(stats.currentValue),
            style = MaterialTheme.typography.titleLarge.tabularNumeric,
            color = MaterialTheme.colorScheme.onSurface,
            contentAlignment = Alignment.TopEnd,
        )
        DeltaRow(stats = stats)
    }
}

@Composable
private fun DeltaRow(stats: InventoryStats, modifier: Modifier = Modifier) {
    val semanticColors = LocalSemanticColors.current
    val targetDeltaColor = when {
        stats.delta.minorUnits > 0 -> semanticColors.positive
        stats.delta.minorUnits < 0 -> semanticColors.negative
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    // Crossed on the same schedule as the text it belongs to: AnimatedContent keeps the outgoing
    // string composed, so a colour that switched the instant the target changed would repaint the
    // old figure -- the one still reading with the old sign -- before it had finished fading out.
    val deltaColor by animateColorAsState(
        targetValue = targetDeltaColor,
        animationSpec = tween(
            durationMillis = LocalMotion.current.duration.standard,
            delayMillis = DELTA_START_DELAY.inWholeMilliseconds.toInt(),
        ),
        label = DELTA_COLOR_LABEL,
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(LocalDimens.current.padding.extraSmall),
    ) {
        AnimatedFadeText(
            text = formatMoney(stats.spent),
            style = MaterialTheme.typography.labelMedium.tabularNumeric,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            startDelay = SPENT_START_DELAY,
            contentAlignment = Alignment.TopEnd,
        )
        AnimatedFadeText(
            modifier = Modifier.testTag(InventoryStatsBar.Tag.DELTA),
            text = formatSignedDelta(delta = stats.delta, fraction = stats.deltaFraction),
            style = MaterialTheme.typography.labelMedium.tabularNumeric,
            color = deltaColor,
            startDelay = DELTA_START_DELAY,
            contentAlignment = Alignment.TopEnd,
        )
    }
}

internal object InventoryStatsBar {
    object Tag {
        const val ROOT = "InventoryStatsBar"
        const val CURRENT_VALUE = "$ROOT.currentValue"
        const val DELTA = "$ROOT.delta"
    }
}
