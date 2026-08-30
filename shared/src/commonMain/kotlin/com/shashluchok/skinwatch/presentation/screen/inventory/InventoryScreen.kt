package com.shashluchok.skinwatch.presentation.screen.inventory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashluchok.skinwatch.presentation.component.LocalBottomBarInset
import com.shashluchok.skinwatch.presentation.component.modal.host.LocalModalHost
import com.shashluchok.skinwatch.presentation.component.modal.host.ModalRequest
import com.shashluchok.skinwatch.presentation.component.sharedelement.LocalSharedElementKeyTransition
import com.shashluchok.skinwatch.presentation.screen.inventory.component.EditItemBottomSheetContent
import com.shashluchok.skinwatch.presentation.screen.inventory.component.InventoryItemCard
import com.shashluchok.skinwatch.presentation.screen.inventory.component.SyncStatusBar
import com.shashluchok.skinwatch.presentation.screen.inventory.component.pricehistory.PriceHistoryDetailScreen
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.presentation.theme.LocalMotion
import com.shashluchok.skinwatch.presentation.util.plusBottom
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_inventory__empty_state
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val ITEM_CARD_CONTENT_TYPE = "InventoryItemCard"

@Composable
internal fun InventoryScreen(
    modifier: Modifier = Modifier,
    viewModel: InventoryViewModel = koinViewModel(),
) {
    val state = viewModel.stateFlow.collectAsStateWithLifecycle().value
    InventoryScreen(
        modifier = modifier,
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun InventoryScreen(
    state: InventoryViewModel.State,
    onAction: (InventoryViewModel.Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sharedElementKeyTransition = LocalSharedElementKeyTransition.current
    val listState = rememberLazyListState()
    val motion = LocalMotion.current
    val cardVisibilityAnimationSpec = remember(motion) {
        tween<Float>(durationMillis = motion.duration.standard, easing = motion.easing.standard)
    }

    Scaffold(
        modifier = modifier.testTag(InventoryScreen.Tag.ROOT),
        topBar = {
            if (state.items.isNotEmpty()) {
                SyncStatusBar(
                    lastSyncedAt = state.lastSyncedAt,
                    isSyncing = state.isSyncing,
                    onSyncClick = { onAction(InventoryViewModel.Action.OnSyncNowClick) },
                )
            }
        },
    ) { contentPadding ->
        val listContentPadding = contentPadding.plusBottom(LocalBottomBarInset.current)

        if (state.items.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(listContentPadding),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(InventoryScreen.Tag.LIST),
                contentPadding = listContentPadding,
            ) {
                items(
                    items = state.items,
                    key = { it.item.id },
                    contentType = { ITEM_CARD_CONTENT_TYPE },
                ) { listItem ->
                    val onItemClick = remember(listItem.item) {
                        { onAction(InventoryViewModel.Action.OnItemClick(listItem.item)) }
                    }
                    sharedElementKeyTransition.AnimatedVisibility(
                        visible = { openedKey -> listItem.item.id != openedKey },
                        enter = fadeIn(cardVisibilityAnimationSpec),
                        exit = fadeOut(cardVisibilityAnimationSpec),
                        modifier = Modifier.animateItem(),
                    ) {
                        InventoryItemCard(
                            listItem = listItem,
                            onClick = onItemClick,
                            animatedVisibilityScope = this@AnimatedVisibility,
                            modifier = Modifier.testTag(InventoryScreen.Tag.itemCard(listItem.item.id)),
                        )
                    }
                }
            }
        }
    }

    state.editSheet?.let { sheet ->
        RegisterEditSheet(sheet = sheet, onAction = onAction)
    }
    state.priceHistoryDetailAlert?.let { detail ->
        RegisterPriceHistoryDetailAlert(
            detail = detail,
            onDismissRequest = { onAction(InventoryViewModel.Action.OnDismissPriceHistoryDetail) },
        )
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = LocalDimens.current.padding.medium)
                .testTag(InventoryScreen.Tag.EMPTY_STATE),
            text = stringResource(Res.string.dev__screen_inventory__empty_state),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RegisterEditSheet(
    sheet: InventoryViewModel.EditSheetState,
    onAction: (InventoryViewModel.Action) -> Unit,
) {
    LocalModalHost.current.Show(
        ModalRequest(
            appearance = ModalRequest.Appearance.BottomSheet,
            onDismissRequest = { onAction(InventoryViewModel.Action.OnDismissSheet) },
            content = {
                EditItemBottomSheetContent(
                    sheet = sheet,
                    onQuantityChange = { onAction(InventoryViewModel.Action.OnQuantityChanged(it)) },
                    onPurchasePriceChange = { onAction(InventoryViewModel.Action.OnPurchasePriceChanged(it)) },
                    onSaveClick = { onAction(InventoryViewModel.Action.OnSaveClick) },
                    onDeleteClick = { onAction(InventoryViewModel.Action.OnDeleteClick) },
                    onDeleteConfirm = { onAction(InventoryViewModel.Action.OnDeleteConfirmed) },
                    onDeleteCancel = { onAction(InventoryViewModel.Action.OnDeleteCancelled) },
                )
            },
        ),
    )
}

@Composable
private fun RegisterPriceHistoryDetailAlert(
    detail: InventoryViewModel.PriceHistoryDetailState,
    onDismissRequest: () -> Unit,
) {
    LocalModalHost.current.Show(
        ModalRequest(
            appearance = ModalRequest.Appearance.Alert(key = detail.item.id),
            onDismissRequest = onDismissRequest,
            content = {
                PriceHistoryDetailScreen(
                    detail = detail,
                    onBackClick = onDismissRequest,
                )
            },
        ),
    )
}

internal object InventoryScreen {
    object Tag {
        const val ROOT = "InventoryScreen"
        const val EMPTY_STATE = "$ROOT.emptyState"
        const val LIST = "$ROOT.list"

        fun itemCard(id: Long) = "$ROOT.itemCard.$id"
    }
}
