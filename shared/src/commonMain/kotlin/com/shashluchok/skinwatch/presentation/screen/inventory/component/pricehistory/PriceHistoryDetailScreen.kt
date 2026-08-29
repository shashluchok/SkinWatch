package com.shashluchok.skinwatch.presentation.screen.inventory.component.pricehistory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import coil3.compose.AsyncImage
import com.shashluchok.skinwatch.domain.inventory.InventoryItem
import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshot
import com.shashluchok.skinwatch.domain.steam.Money
import com.shashluchok.skinwatch.presentation.component.SharedElementKey
import com.shashluchok.skinwatch.presentation.component.sharedelement.LocalAnimatedVisibilityScope
import com.shashluchok.skinwatch.presentation.component.sharedelement.LocalSharedElementConfig
import com.shashluchok.skinwatch.presentation.screen.inventory.InventoryViewModel
import com.shashluchok.skinwatch.presentation.screen.inventory.component.PriceTrendGlyph
import com.shashluchok.skinwatch.presentation.theme.AppFontFamilies
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.presentation.theme.LocalMotion
import com.shashluchok.skinwatch.presentation.theme.tabularNumeric
import com.shashluchok.skinwatch.presentation.util.toFullDateLabel
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_inventory__price_history_detail__back__content_description
import com.shashluchok.skinwatch.resources.dev__screen_inventory__price_history_detail__empty_state
import com.shashluchok.skinwatch.resources.dev__screen_inventory__price_history_detail__purchase_price_label
import com.shashluchok.skinwatch.resources.dev__screen_inventory__price_history_detail__single_point_hint
import org.jetbrains.compose.resources.stringResource

private const val MINOR_UNITS_PER_MAJOR_UNIT = 100.0
private const val EMPTY_STATE_GLYPH_ALPHA = 0.5f
private const val DASH_SWATCH_WIDTH_DP = 20
private const val CONTAINER_MAX_WIDTH_FRACTION = 0.9f
private const val CONTAINER_MAX_HEIGHT_FRACTION = 0.8f

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PriceHistoryDetailScreen(
    detail: InventoryViewModel.PriceHistoryDetailState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotion.current
    val contentFadeSpec = remember(motion) {
        tween<Float>(durationMillis = motion.duration.standard, easing = motion.easing.standard)
    }
    val contentEnter = remember(contentFadeSpec) { fadeIn(contentFadeSpec) }
    val contentExit = remember(contentFadeSpec) { fadeOut(contentFadeSpec) }
    val bodyVisibleState = rememberBodyRevealState(itemId = detail.item.id)
    val bodyEnter = remember(motion) {
        fadeIn(tween(durationMillis = motion.duration.standard, delayMillis = motion.duration.standard))
    }
    // A snapshot with a null lowestPrice contributes nothing to the chart/value -- it must not
    // count as a "data point" for the 0/1/2+ branch below (see design addendum section 7).
    val pricedSnapshots = detail.snapshots.filter { it.lowestPrice != null }

    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = true,
        onBackCompleted = onBackClick,
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        IslandSurface(
            detail = detail,
            onBackClick = onBackClick,
            pricedSnapshots = pricedSnapshots,
            islandMaxWidth = maxWidth * CONTAINER_MAX_WIDTH_FRACTION,
            islandMaxHeight = maxHeight * CONTAINER_MAX_HEIGHT_FRACTION,
            contentEnter = contentEnter,
            contentExit = contentExit,
            bodyVisibleState = bodyVisibleState,
            bodyEnter = bodyEnter,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun IslandSurface(
    detail: InventoryViewModel.PriceHistoryDetailState,
    onBackClick: () -> Unit,
    pricedSnapshots: List<PriceSnapshot>,
    islandMaxWidth: Dp,
    islandMaxHeight: Dp,
    contentEnter: EnterTransition,
    contentExit: ExitTransition,
    bodyVisibleState: MutableTransitionState<Boolean>,
    bodyEnter: EnterTransition,
) {
    val (sharedTransitionScope, boundsTransform) = LocalSharedElementConfig.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val dimens = LocalDimens.current
    val islandShape = RoundedCornerShape(dimens.radius.extraLarge)

    with(sharedTransitionScope) {
        Surface(
            modifier = Modifier
                .widthIn(max = islandMaxWidth)
                .heightIn(max = islandMaxHeight)
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(
                        key = SharedElementKey.Container(itemId = detail.item.id),
                    ),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = boundsTransform,
                    enter = contentEnter,
                    exit = contentExit,
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                    clipInOverlayDuringTransition = OverlayClip(islandShape),
                ).pointerInput(Unit) { detectTapGestures {} }
                .testTag(PriceHistoryDetailScreen.Tag.ROOT),
            shape = islandShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = dimens.elevation.medium,
            shadowElevation = dimens.elevation.large,
        ) {
            Column {
                DetailHeader(
                    item = detail.item,
                    onBackClick = onBackClick,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AnimatedVisibility(
                    visibleState = bodyVisibleState,
                    enter = bodyEnter,
                    modifier = Modifier.weight(weight = 1f, fill = false),
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = dimens.padding.medium, vertical = dimens.padding.medium),
                    ) {
                        PriceHistoryBody(
                            pricedSnapshots = pricedSnapshots,
                            purchasePrice = detail.item.purchasePrice,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceHistoryBody(pricedSnapshots: List<PriceSnapshot>, purchasePrice: Money?) {
    when {
        pricedSnapshots.isEmpty() -> EmptyPriceHistory()
        pricedSnapshots.size == 1 -> SinglePricePoint(
            snapshot = pricedSnapshots.single(),
            purchasePrice = purchasePrice,
        )

        else -> PriceHistoryChart(
            snapshots = pricedSnapshots,
            purchasePrice = purchasePrice,
            modifier = Modifier.testTag(PriceHistoryDetailScreen.Tag.CHART),
        )
    }
}

/**
 * `false` for every new [itemId], then flipped to `true` immediately after -- paired with
 * [AnimatedVisibility]'s `enter`, whose `delayMillis` (not a bare `delay()` call) is what actually
 * defers the reveal, so the body only starts fading in once the container has visibly settled into
 * place rather than appearing simultaneously with its flight. A plain `visible = true` wouldn't
 * replay this on every open -- a boolean already true on first composition has no false-to-true
 * transition to animate.
 */
@Composable
private fun rememberBodyRevealState(itemId: Long): MutableTransitionState<Boolean> {
    val state = remember(itemId) { MutableTransitionState(initialState = false) }
    state.targetState = true
    return state
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun DetailHeader(
    item: InventoryItem,
    onBackClick: () -> Unit,
) {
    val (sharedTransitionScope, boundsTransform) = LocalSharedElementConfig.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val dimens = LocalDimens.current
    val backLabel = stringResource(Res.string.dev__screen_inventory__price_history_detail__back__content_description)

    with(sharedTransitionScope) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.padding.small, vertical = dimens.padding.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.padding.small),
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag(PriceHistoryDetailScreen.Tag.BACK_BUTTON),
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backLabel)
            }
            AsyncImage(
                model = item.iconUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(dimens.iconSize.extraLarge)
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(
                            key = SharedElementKey.Icon(itemId = item.id),
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = boundsTransform,
                    ).clip(RoundedCornerShape(dimens.radius.small))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
            )
            Text(
                modifier = Modifier
                    .weight(1f)
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(
                            key = SharedElementKey.Title(itemId = item.id),
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = boundsTransform,
                    ),
                text = item.marketHashName,
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

@Composable
private fun EmptyPriceHistory() {
    val dimens = LocalDimens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimens.padding.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyStateGlyph(
            modifier = Modifier
                .size(dimens.iconSize.extraLarge)
                .testTag(PriceHistoryDetailScreen.Tag.EMPTY_STATE),
        )
        Text(
            modifier = Modifier.padding(top = dimens.padding.small),
            text = stringResource(Res.string.dev__screen_inventory__price_history_detail__empty_state),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyStateGlyph(modifier: Modifier = Modifier) {
    val strokeColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = EMPTY_STATE_GLYPH_ALPHA)
    PriceTrendGlyph(color = strokeColor, modifier = modifier)
}

@Composable
private fun SinglePricePoint(
    snapshot: PriceSnapshot,
    purchasePrice: Money?,
) {
    val dimens = LocalDimens.current
    Column(modifier = Modifier.padding(top = dimens.padding.extraLarge)) {
        Text(
            modifier = Modifier.testTag(PriceHistoryDetailScreen.Tag.SINGLE_POINT_VALUE),
            text = snapshot.lowestPrice?.let(::formatMoney).orEmpty(),
            style = MaterialTheme.typography.headlineSmall
                .copy(fontFamily = AppFontFamilies.jetBrainsMono)
                .tabularNumeric,
        )
        Text(
            modifier = Modifier.padding(top = dimens.padding.tiny),
            text = snapshot.capturedAt.toFullDateLabel(),
            style = MaterialTheme.typography.bodySmall
                .copy(fontFamily = AppFontFamilies.jetBrainsMono)
                .tabularNumeric,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            modifier = Modifier.padding(top = dimens.padding.small),
            text = stringResource(Res.string.dev__screen_inventory__price_history_detail__single_point_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (purchasePrice != null) {
            PurchasePriceLegend(
                modifier = Modifier.padding(top = dimens.padding.medium),
                purchasePrice = purchasePrice,
            )
        }
    }
}

/** Dash swatch + label + amount, e.g. "┄┄ Цена покупки  49.00 USD" -- see design addendum section 4. */
@Composable
internal fun PurchasePriceLegend(
    purchasePrice: Money,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current
    Row(modifier = modifier) {
        DashSwatch(
            modifier = Modifier.size(width = DASH_SWATCH_WIDTH_DP.dp, height = dimens.border.thin),
        )
        Text(
            modifier = Modifier.padding(start = dimens.padding.small),
            text = stringResource(Res.string.dev__screen_inventory__price_history_detail__purchase_price_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            modifier = Modifier.padding(start = dimens.padding.extraSmall),
            text = formatMoney(purchasePrice),
            style = MaterialTheme.typography.labelSmall
                .copy(fontFamily = AppFontFamilies.jetBrainsMono)
                .tabularNumeric,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Miniature preview of the chart's own dashed purchase-price line, same dash/gap pattern. */
@Composable
private fun DashSwatch(modifier: Modifier = Modifier) {
    val dimens = LocalDimens.current
    val color = MaterialTheme.colorScheme.outline
    val strokeWidth = dimens.border.thin
    Canvas(modifier = modifier) {
        val y = size.height / 2f
        drawLine(
            color = color,
            start = Offset(x = 0f, y = y),
            end = Offset(x = size.width, y = y),
            strokeWidth = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(dimens.padding.small.toPx(), dimens.padding.extraSmall.toPx()),
            ),
        )
    }
}

/**
 * Plain, locale-independent formatting (e.g. "49.00 USD"), same pattern as `InventoryItemCard`'s
 * private `formatMoney`. Both copies should eventually move to one shared multiplatform money
 * formatter -- not in scope for this task.
 */
private fun formatMoney(money: Money): String {
    val major = money.minorUnits / MINOR_UNITS_PER_MAJOR_UNIT
    return "$major ${money.currency.name}"
}

internal object PriceHistoryDetailScreen {
    object Tag {
        const val ROOT = "PriceHistoryDetailScreen"
        const val BACK_BUTTON = "$ROOT.backButton"
        const val EMPTY_STATE = "$ROOT.emptyState"
        const val SINGLE_POINT_VALUE = "$ROOT.singlePointValue"
        const val CHART = "$ROOT.chart"
    }
}
