package com.shashluchok.skinwatch.presentation.screen.inventory

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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashluchok.skinwatch.presentation.component.bottomsheet.BottomSheetRequest
import com.shashluchok.skinwatch.presentation.component.bottomsheet.LocalBottomSheetHost
import com.shashluchok.skinwatch.presentation.screen.inventory.component.EditItemBottomSheetContent
import com.shashluchok.skinwatch.presentation.screen.inventory.component.InventoryItemCard
import com.shashluchok.skinwatch.presentation.screen.inventory.component.PriceHistoryBottomSheetContent
import com.shashluchok.skinwatch.presentation.screen.inventory.component.SyncStatusBar
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_inventory__empty_state
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

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
    val listState = rememberLazyListState()

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
        if (state.items.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(InventoryScreen.Tag.LIST),
                contentPadding = contentPadding,
            ) {
                items(items = state.items, key = { it.item.id }) { listItem ->
                    InventoryItemCard(
                        listItem = listItem,
                        onClick = { onAction(InventoryViewModel.Action.OnItemClick(listItem.item)) },
                        modifier = Modifier.testTag(InventoryScreen.Tag.itemCard(listItem.item.id)),
                    )
                }
            }
        }
    }

    state.editSheet?.let { sheet ->
        RegisterEditSheet(sheet = sheet, onAction = onAction)
    }

    state.priceHistorySheet?.let { sheet ->
        RegisterPriceHistorySheet(sheet = sheet, onAction = onAction)
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
    LocalBottomSheetHost.current.Show(
        BottomSheetRequest(
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
private fun RegisterPriceHistorySheet(
    sheet: InventoryViewModel.PriceHistorySheetState,
    onAction: (InventoryViewModel.Action) -> Unit,
) {
    LocalBottomSheetHost.current.Show(
        BottomSheetRequest(
            onDismissRequest = { onAction(InventoryViewModel.Action.OnDismissPriceHistorySheet) },
            content = { PriceHistoryBottomSheetContent(sheet = sheet) },
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
