package com.shashluchok.skinwatch.presentation.screen.inventory.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.shashluchok.skinwatch.presentation.component.ShimmerBlock
import com.shashluchok.skinwatch.presentation.component.ShimmerLine
import com.shashluchok.skinwatch.presentation.component.rememberShimmerSweep
import com.shashluchok.skinwatch.presentation.theme.LocalDimens

private const val TITLE_WIDTH_FRACTION = 0.75f
private const val QUANTITY_WIDTH_FRACTION = 0.15f
private const val PRICE_LABEL_WIDTH_FRACTION = 0.5f
private const val PRICE_VALUE_WIDTH_FRACTION = 0.8f

/**
 * Mirrors [InventoryItemCard]'s structure line for line so the real rows replace it without
 * shifting layout. Placeholder heights come from the same typography the card renders with rather
 * than fixed values, which would drift silently the next time a text style changes.
 */
@Composable
internal fun InventoryItemCardSkeleton(modifier: Modifier = Modifier) {
    val dimens = LocalDimens.current
    val typography = MaterialTheme.typography
    val sweepProvider = rememberShimmerSweep()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.padding.medium, vertical = dimens.padding.small),
        shape = RoundedCornerShape(dimens.radius.medium),
    ) {
        Row(
            modifier = Modifier.padding(dimens.padding.medium),
            horizontalArrangement = Arrangement.spacedBy(dimens.padding.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShimmerBlock(
                sweep = sweepProvider,
                modifier = Modifier
                    .size(dimens.iconSize.extraLarge)
                    .clip(RoundedCornerShape(dimens.radius.small)),
            )
            Column(modifier = Modifier.weight(1f)) {
                ShimmerLine(
                    sweep = sweepProvider,
                    lineHeight = typography.titleMedium.lineHeight,
                    widthFraction = TITLE_WIDTH_FRACTION,
                )
                ShimmerLine(
                    sweep = sweepProvider,
                    lineHeight = typography.bodyMedium.lineHeight,
                    widthFraction = QUANTITY_WIDTH_FRACTION,
                )
                Row(modifier = Modifier.padding(top = dimens.padding.small)) {
                    PriceColumn(
                        sweep = sweepProvider,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = dimens.padding.medium),
                    )
                    PriceColumn(
                        sweep = sweepProvider,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            ShimmerBlock(
                sweep = sweepProvider,
                modifier = Modifier
                    .size(dimens.iconSize.small)
                    .clip(RoundedCornerShape(dimens.radius.small)),
            )
        }
    }
}

@Composable
private fun PriceColumn(
    sweep: () -> Float,
    modifier: Modifier = Modifier,
) {
    val typography = MaterialTheme.typography
    Column(modifier = modifier) {
        ShimmerLine(
            sweep = sweep,
            lineHeight = typography.labelSmall.lineHeight,
            widthFraction = PRICE_LABEL_WIDTH_FRACTION,
        )
        ShimmerLine(
            sweep = sweep,
            lineHeight = typography.bodyLarge.lineHeight,
            widthFraction = PRICE_VALUE_WIDTH_FRACTION,
        )
    }
}
