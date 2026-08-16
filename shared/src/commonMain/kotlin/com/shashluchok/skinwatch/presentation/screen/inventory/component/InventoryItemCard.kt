package com.shashluchok.skinwatch.presentation.screen.inventory.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import coil3.compose.AsyncImage
import com.shashluchok.skinwatch.domain.inventory.InventoryListItem
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_inventory__item_card__market_price_label
import com.shashluchok.skinwatch.resources.dev__screen_inventory__item_card__no_price_data
import com.shashluchok.skinwatch.resources.dev__screen_inventory__item_card__purchase_price_label
import org.jetbrains.compose.resources.stringResource

private const val MINOR_UNITS_PER_MAJOR_UNIT = 100.0

@Composable
internal fun InventoryItemCard(
    listItem: InventoryListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.padding.medium, vertical = dimens.padding.small)
            .selectable(selected = false, onClick = onClick),
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
            Column {
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
                            text = listItem.item.purchasePrice?.let(::formatMoney)
                                ?: stringResource(Res.string.dev__screen_inventory__item_card__no_price_data),
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
        }
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
    }
}
