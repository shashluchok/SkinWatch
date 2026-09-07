package com.shashluchok.skinwatch.presentation.screen.inventory.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
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
 * Inventory totals above the list: what it is worth now, and how that compares to what was paid.
 * The estimate marker appears only while some items still lack a price reading -- see
 * [InventoryStats] for how those are counted.
 */
@Composable
internal fun InventoryStatsBar(stats: InventoryStats, modifier: Modifier = Modifier) {
    val dimens = LocalDimens.current

    Column(
        // The list scrolls under this bar, so without swallowing taps they land on whichever card
        // happens to be behind it.
        modifier = modifier
            .pointerInput(Unit) { detectTapGestures {} }
            .testTag(InventoryStatsBar.Tag.ROOT),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = dimens.padding.medium, vertical = dimens.padding.small),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimens.padding.extraSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedFadeText(
                    modifier = Modifier.testTag(InventoryStatsBar.Tag.CURRENT_VALUE),
                    text = formatMoney(stats.currentValue),
                    style = MaterialTheme.typography.headlineSmall.tabularNumeric,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            DeltaRow(stats = stats)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
        )
        AnimatedFadeText(
            modifier = Modifier.testTag(InventoryStatsBar.Tag.DELTA),
            text = formatSignedDelta(delta = stats.delta, fraction = stats.deltaFraction),
            style = MaterialTheme.typography.labelMedium.tabularNumeric,
            color = deltaColor,
            startDelay = DELTA_START_DELAY,
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
