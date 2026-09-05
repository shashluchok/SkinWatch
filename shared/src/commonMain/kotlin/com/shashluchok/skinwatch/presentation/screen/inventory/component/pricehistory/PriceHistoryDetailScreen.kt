package com.shashluchok.skinwatch.presentation.screen.inventory.component.pricehistory

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.shashluchok.skinwatch.presentation.screen.inventory.component.PriceTrend
import com.shashluchok.skinwatch.presentation.screen.inventory.component.PriceTrendGlyph
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.presentation.theme.LocalMotion
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_inventory__price_history_detail__empty_state
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val EMPTY_STATE_GLYPH_ALPHA = 0.5f
private const val CONTAINER_MAX_WIDTH_FRACTION = 0.9f
private const val CONTAINER_MAX_HEIGHT_FRACTION = 0.8f

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PriceHistoryDetailScreen(
    item: InventoryItem,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PriceHistoryDetailViewModel = koinViewModel(
        parameters = { parametersOf(item) },
    ),
) {
    val state = viewModel.stateFlow.collectAsStateWithLifecycle().value

    val animatedVisibility = LocalAnimatedVisibilityScope.current

    LaunchedEffect(Unit) {
        snapshotFlow { animatedVisibility.transition.currentState }.collect {
            if (it == EnterExitState.Visible) {
                viewModel.onAction(PriceHistoryDetailViewModel.Action.OnDisplay(item))
            }
        }
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = true,
        onBackCompleted = onBackClick,
    )

    PriceHistoryDetailScreen(
        item = item,
        modifier = modifier,
        state = state,
    )

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onAction(PriceHistoryDetailViewModel.Action.OnDismiss)
        }
    }
}

@Composable
private fun PriceHistoryDetailScreen(
    item: InventoryItem,
    state: PriceHistoryDetailViewModel.State,
    modifier: Modifier = Modifier,
) {
    Container(
        modifier = modifier.fillMaxSize(),
        itemId = item.id,
    ) {
        Content(
            item = item,
            state = state,
        )
    }
}

@Composable
private fun Container(
    itemId: Long,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val motion = LocalMotion.current
        val contentFadeSpec = remember(motion) {
            tween<Float>(durationMillis = motion.duration.standard, easing = motion.easing.standard)
        }
        val contentEnter = remember(contentFadeSpec) { fadeIn(contentFadeSpec) }
        val contentExit = remember(contentFadeSpec) { fadeOut(contentFadeSpec) }

        val (sharedTransitionScope, boundsTransform) = LocalSharedElementConfig.current
        val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
        val dimens = LocalDimens.current
        val islandShape = RoundedCornerShape(dimens.radius.extraLarge)
        with(sharedTransitionScope) {
            Surface(
                modifier = Modifier
                    .widthIn(max = maxWidth * CONTAINER_MAX_WIDTH_FRACTION)
                    .heightIn(max = maxHeight * CONTAINER_MAX_HEIGHT_FRACTION)
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            key = SharedElementKey.Container(itemId = itemId),
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
                content = content,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Content(
    item: InventoryItem,
    state: PriceHistoryDetailViewModel.State,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        DetailHeader(
            item = item,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        AnimatedContent(
            targetState = state,
        ) { snapshotsState ->
            when (snapshotsState) {
                is PriceHistoryDetailViewModel.State.Content -> {
                    // Scrollable, so the body is measured against an unbounded height. Beyond
                    // letting tall content scroll on short screens, this is what keeps the chart at
                    // its intrinsic height while the closing shared-bounds transition shrinks this
                    // island toward the list card: measured against those collapsing bounds, the
                    // chart's vertical axis ends up handing a negative height to its title's text
                    // measurement, which throws.
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        PriceHistoryBody(
                            pricedSnapshots = snapshotsState.snapshots,
                            purchasePrice = item.purchasePrice,
                        )
                    }
                }
                PriceHistoryDetailViewModel.State.Loading -> {}
            }
        }
    }
}

@Composable
private fun PriceHistoryBody(pricedSnapshots: List<PriceSnapshot>, purchasePrice: Money?) {
    when {
        pricedSnapshots.isEmpty() -> EmptyPriceHistory()
        else -> PriceHistoryChart(
            snapshots = pricedSnapshots,
            purchasePrice = purchasePrice,
            modifier = Modifier.testTag(PriceHistoryDetailScreen.Tag.CHART),
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun DetailHeader(
    item: InventoryItem,
) {
    val (sharedTransitionScope, boundsTransform) = LocalSharedElementConfig.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val dimens = LocalDimens.current

    with(sharedTransitionScope) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.padding.small, vertical = dimens.padding.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.padding.small),
        ) {
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
                    ).clip(RoundedCornerShape(dimens.radius.small)),
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
                style = MaterialTheme.typography.titleMedium,
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
    PriceTrendGlyph(
        trend = PriceTrend.NEUTRAL,
        modifier = modifier.alpha(EMPTY_STATE_GLYPH_ALPHA),
    )
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
