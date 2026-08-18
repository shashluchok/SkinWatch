package com.shashluchok.skinwatch.presentation.screen.inventory.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import coil3.compose.AsyncImage
import com.shashluchok.skinwatch.domain.inventory.InventoryListItem
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_inventory__item_card__market_price_label
import com.shashluchok.skinwatch.resources.dev__screen_inventory__item_card__no_price_data
import com.shashluchok.skinwatch.resources.dev__screen_inventory__item_card__open_price_history__content_description
import com.shashluchok.skinwatch.resources.dev__screen_inventory__item_card__purchase_price_label
import org.jetbrains.compose.resources.stringResource

private const val MINOR_UNITS_PER_MAJOR_UNIT = 100.0

/**
 * Normalized (fraction of glyph width/height) points for [PriceHistoryGlyph]'s static up-down-up
 * polyline -- deliberately not the same "breathing glow" treatment as `NavTab.GlowIcon`, which is
 * reserved for a single selected tab at a time, not many simultaneously visible cards.
 */
private val priceHistoryGlyphPoints = listOf(
    Offset(x = 0f, y = 0.70f),
    Offset(x = 0.33f, y = 0.30f),
    Offset(x = 0.66f, y = 0.55f),
    Offset(x = 1f, y = 0.15f),
)

@Composable
internal fun InventoryItemCard(
    listItem: InventoryListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current
    val openPriceHistoryLabel =
        stringResource(Res.string.dev__screen_inventory__item_card__open_price_history__content_description)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.padding.medium, vertical = dimens.padding.small)
            .clickable(onClickLabel = openPriceHistoryLabel, role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(dimens.padding.medium),
            horizontalArrangement = Arrangement.spacedBy(dimens.padding.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = listItem.item.iconUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(dimens.iconSize.extraLarge)
                    .clip(RoundedCornerShape(dimens.radius.small))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag(InventoryItemCard.Tag.ICON),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = listItem.item.marketHashName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "x${listItem.item.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(modifier = Modifier.padding(top = dimens.padding.small)) {
                    Column(modifier = Modifier.padding(end = dimens.padding.medium)) {
                        Text(
                            text = stringResource(Res.string.dev__screen_inventory__item_card__purchase_price_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = formatMoney(listItem.item.purchasePrice),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(Res.string.dev__screen_inventory__item_card__market_price_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = listItem.latestSnapshot?.lowestPrice?.let(::formatMoney)
                                ?: stringResource(Res.string.dev__screen_inventory__item_card__no_price_data),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            PriceHistoryGlyph(modifier = Modifier.size(dimens.iconSize.small))
        }
    }
}

/**
 * Static "sparkline" hint glyph indicating the card opens the price history chart on tap --
 * unlike `NavTab.GlowIcon`'s animated glow (reserved for a single selected tab at a time), this
 * stays static since many cards are visible on screen simultaneously. Decorative: the tap action
 * is already announced on the card itself via [Role.Button]/`onClickLabel`.
 */
@Composable
private fun PriceHistoryGlyph(modifier: Modifier = Modifier) {
    val strokeColor = MaterialTheme.colorScheme.onSurfaceVariant
    val strokeWidth = LocalDimens.current.border.thin
    Canvas(modifier = modifier.testTag(InventoryItemCard.Tag.PRICE_HISTORY_GLYPH)) {
        val path = Path().apply {
            priceHistoryGlyphPoints.forEachIndexed { index, point ->
                val offset = Offset(x = point.x * size.width, y = point.y * size.height)
                if (index == 0) moveTo(x = offset.x, y = offset.y) else lineTo(x = offset.x, y = offset.y)
            }
        }
        drawPath(path = path, color = strokeColor, style = Stroke(width = strokeWidth.toPx()))
    }
}

/**
 * Plain, locale-independent formatting for now (e.g. "49.00 USD") -- exact currency symbol/
 * position/locale formatting is a follow-up visual-design pass, not decided by this screen.
 */
private fun formatMoney(money: Money): String {
    val major = money.minorUnits / MINOR_UNITS_PER_MAJOR_UNIT
    return "$major ${money.currency.name}"
}

internal object InventoryItemCard {
    object Tag {
        private const val ROOT = "InventoryItemCard"
        const val ICON = "$ROOT.icon"
        const val PRICE_HISTORY_GLYPH = "$ROOT.priceHistoryGlyph"
    }
}
