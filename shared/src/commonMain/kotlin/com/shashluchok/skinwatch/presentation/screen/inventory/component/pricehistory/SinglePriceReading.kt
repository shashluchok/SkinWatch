package com.shashluchok.skinwatch.presentation.screen.inventory.component.pricehistory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshot
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.presentation.theme.LocalSemanticColors
import com.shashluchok.skinwatch.presentation.theme.tabularNumeric
import com.shashluchok.skinwatch.presentation.util.formatMoney
import com.shashluchok.skinwatch.presentation.util.formatSignedDelta
import com.shashluchok.skinwatch.presentation.util.relativeTimeText
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_inventory__price_history_detail__single_reading__purchase_label
import com.shashluchok.skinwatch.resources.dev__screen_inventory__price_history_detail__single_reading__updated
import org.jetbrains.compose.resources.stringResource

/**
 * The one reading there is, shown as a number rather than as a chart.
 *
 * A single point is not a history: plotting it leaves axes, a grid and one lone dot, which reads as
 * a broken chart rather than as "there is one price so far". Occupies [CHART_HEIGHT] so the island
 * stays one size across every state the modal can show.
 */
@Composable
internal fun SinglePriceReading(
    snapshot: PriceSnapshot,
    purchasePrice: Money?,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(CHART_HEIGHT)
            .padding(horizontal = dimens.padding.medium)
            .testTag(SinglePriceReading.Tag.ROOT),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val price = snapshot.lowestPrice
        if (price != null) {
            Text(
                modifier = Modifier.testTag(SinglePriceReading.Tag.PRICE),
                text = formatMoney(price),
                style = MaterialTheme.typography.headlineMedium.tabularNumeric,
            )
        }
        Text(
            modifier = Modifier.padding(top = dimens.padding.extraSmall),
            text = stringResource(
                Res.string.dev__screen_inventory__price_history_detail__single_reading__updated,
                relativeTimeText(snapshot.capturedAt),
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (purchasePrice != null && price != null) {
            PurchaseComparison(
                purchasePrice = purchasePrice,
                currentPrice = price,
                modifier = Modifier.padding(top = dimens.padding.medium),
            )
        }
    }
}

@Composable
private fun PurchaseComparison(
    purchasePrice: Money,
    currentPrice: Money,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current
    val delta = Money(
        minorUnits = currentPrice.minorUnits - purchasePrice.minorUnits,
        currency = currentPrice.currency,
    )
    val fraction = purchasePrice.minorUnits
        .takeIf { it != 0L }
        ?.let { delta.minorUnits.toFloat() / it }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dimens.padding.extraSmall),
    ) {
        Text(
            text = stringResource(
                Res.string.dev__screen_inventory__price_history_detail__single_reading__purchase_label,
                formatMoney(purchasePrice),
            ),
            style = MaterialTheme.typography.labelMedium.tabularNumeric,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            modifier = Modifier.testTag(SinglePriceReading.Tag.DELTA),
            text = formatSignedDelta(delta = delta, fraction = fraction),
            style = MaterialTheme.typography.labelMedium.tabularNumeric,
            color = deltaColor(delta),
        )
    }
}

@Composable
private fun deltaColor(delta: Money): Color {
    val semanticColors = LocalSemanticColors.current

    return when {
        delta.minorUnits > 0 -> semanticColors.positive
        delta.minorUnits < 0 -> semanticColors.negative
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

internal object SinglePriceReading {
    object Tag {
        const val ROOT = "SinglePriceReading"
        const val PRICE = "$ROOT.price"
        const val DELTA = "$ROOT.delta"
    }
}
