package com.shashluchok.skinwatch.presentation.screen.inventory.component

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import coil3.compose.AsyncImage
import com.shashluchok.skinwatch.domain.inventory.InventoryListItem
import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshot
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.presentation.component.SharedElementKey
import com.shashluchok.skinwatch.presentation.component.SingleLineFadeText
import com.shashluchok.skinwatch.presentation.component.sharedelement.LocalSharedElementConfig
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.presentation.theme.LocalMotion
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_inventory__item_card__delete
import com.shashluchok.skinwatch.resources.dev__screen_inventory__item_card__edit
import com.shashluchok.skinwatch.resources.dev__screen_inventory__item_card__market_price_label
import com.shashluchok.skinwatch.resources.dev__screen_inventory__item_card__no_price_data
import com.shashluchok.skinwatch.resources.dev__screen_inventory__item_card__open_price_history__content_description
import com.shashluchok.skinwatch.resources.dev__screen_inventory__item_card__purchase_price_label
import org.jetbrains.compose.resources.stringResource

private const val MINOR_UNITS_PER_MAJOR_UNIT = 100.0

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun InventoryItemCard(
    listItem: InventoryListItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isContextMenuExpanded: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismissContextMenu: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    val (sharedTransitionScope, boundsTransform) = LocalSharedElementConfig.current
    val dimens = LocalDimens.current
    val motion = LocalMotion.current
    val contentFadeSpec = remember(motion) {
        tween<Float>(durationMillis = motion.duration.standard, easing = motion.easing.standard)
    }
    val openPriceHistoryLabel =
        stringResource(Res.string.dev__screen_inventory__item_card__open_price_history__content_description)
    val cardShape = RoundedCornerShape(dimens.radius.medium)

    with(sharedTransitionScope) {
        Box(modifier = modifier) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.padding.medium, vertical = dimens.padding.small)
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            key = SharedElementKey.Container(itemId = listItem.item.id),
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = boundsTransform,
                        enter = fadeIn(contentFadeSpec),
                        exit = fadeOut(contentFadeSpec),
                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                        clipInOverlayDuringTransition = OverlayClip(cardShape),
                    ).combinedClickable(
                        onClickLabel = openPriceHistoryLabel,
                        role = Role.Button,
                        onLongClick = onLongClick,
                        onClick = onClick,
                    ),
                shape = cardShape,
            ) {
                InventoryItemCardRow(
                    listItem = listItem,
                    sharedTransitionScope = sharedTransitionScope,
                    boundsTransform = boundsTransform,
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
            InventoryItemCardContextMenu(
                expanded = isContextMenuExpanded,
                onEditClick = onEditClick,
                onDeleteClick = onDeleteClick,
                onDismissRequest = onDismissContextMenu,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun InventoryItemCardRow(
    listItem: InventoryListItem,
    sharedTransitionScope: SharedTransitionScope,
    boundsTransform: BoundsTransform,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current
    with(sharedTransitionScope) {
        Row(
            modifier = modifier.padding(dimens.padding.medium),
            horizontalArrangement = Arrangement.spacedBy(dimens.padding.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = listItem.item.iconUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(dimens.iconSize.extraLarge)
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(
                            key = SharedElementKey.Icon(itemId = listItem.item.id),
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = boundsTransform,
                    ).clip(RoundedCornerShape(dimens.radius.small))
                    .testTag(InventoryItemCard.Tag.ICON),
            )
            Column(modifier = Modifier.weight(1f)) {
                SingleLineFadeText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(
                                key = SharedElementKey.Title(itemId = listItem.item.id),
                            ),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = boundsTransform,
                        ),
                    text = listItem.item.marketHashName,
                    fadeColor = CardDefaults.cardColors().containerColor,
                    style = MaterialTheme.typography.titleMedium,
                )
                InventoryItemCardPrices(listItem = listItem)
            }
            PriceHistoryGlyph(
                listItem = listItem,
                modifier = Modifier.size(dimens.iconSize.small),
            )
        }
    }
}

@Composable
private fun InventoryItemCardContextMenu(
    expanded: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        DropdownMenuItem(
            text = { Text(text = stringResource(Res.string.dev__screen_inventory__item_card__edit)) },
            onClick = onEditClick,
            modifier = Modifier.testTag(InventoryItemCard.Tag.CONTEXT_MENU_EDIT),
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(Res.string.dev__screen_inventory__item_card__delete)) },
            onClick = onDeleteClick,
            modifier = Modifier.testTag(InventoryItemCard.Tag.CONTEXT_MENU_DELETE),
        )
    }
}

@Composable
private fun InventoryItemCardPrices(
    listItem: InventoryListItem,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current
    Column(modifier = modifier) {
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
}

@Composable
private fun PriceHistoryGlyph(
    listItem: InventoryListItem,
    modifier: Modifier = Modifier,
) {
    val trend = priceTrend(
        latestSnapshot = listItem.latestSnapshot,
        purchasePrice = listItem.item.purchasePrice,
    )
    PriceTrendGlyph(
        trend = trend,
        modifier = modifier.testTag(InventoryItemCard.Tag.PRICE_HISTORY_GLYPH),
    )
}

internal fun priceHistoryGlyphColor(
    latestSnapshot: PriceSnapshot?,
    purchasePrice: Money,
    positive: Color,
    negative: Color,
    neutral: Color,
): Color = when (priceTrend(latestSnapshot = latestSnapshot, purchasePrice = purchasePrice)) {
    PriceTrend.UP -> positive
    PriceTrend.DOWN -> negative
    PriceTrend.NEUTRAL -> neutral
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
        const val CONTEXT_MENU_EDIT = "$ROOT.contextMenu.edit"
        const val CONTEXT_MENU_DELETE = "$ROOT.contextMenu.delete"
    }
}
