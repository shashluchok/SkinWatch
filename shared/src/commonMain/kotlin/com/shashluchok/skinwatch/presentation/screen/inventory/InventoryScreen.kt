package com.shashluchok.skinwatch.presentation.screen.inventory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashluchok.skinwatch.presentation.screen.inventory.component.EditItemBottomSheet
import com.shashluchok.skinwatch.presentation.screen.inventory.component.InventoryItemCard
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_inventory__empty_state
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun InventoryScreen(
    modifier: Modifier = Modifier,
    onScrollingUp: (Boolean) -> Unit = {},
    viewModel: InventoryViewModel = koinViewModel(),
) {
    val state = viewModel.stateFlow.collectAsStateWithLifecycle().value
    InventoryScreen(
        modifier = modifier,
        state = state,
        onAction = viewModel::onAction,
        onScrollingUp = onScrollingUp,
    )
}

@Composable
private fun InventoryScreen(
    state: InventoryViewModel.State,
    onAction: (InventoryViewModel.Action) -> Unit,
    onScrollingUp: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val isScrollingUp by listState.isScrollingUp()
    val currentOnAddVisibilityChange by rememberUpdatedState(onScrollingUp)

    LaunchedEffect(isScrollingUp) {
        currentOnAddVisibilityChange(isScrollingUp)
    }

    Scaffold(
        modifier = modifier.testTag(InventoryScreen.Tag.ROOT),
    ) { contentPadding ->
        if (state.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
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
        EditItemBottomSheet(
            sheet = sheet,
            onQuantityChange = { onAction(InventoryViewModel.Action.OnQuantityChanged(it)) },
            onPurchasePriceChange = { onAction(InventoryViewModel.Action.OnPurchasePriceChanged(it)) },
            onNoteChange = { onAction(InventoryViewModel.Action.OnNoteChanged(it)) },
            onSaveClick = { onAction(InventoryViewModel.Action.OnSaveClick) },
            onDeleteClick = { onAction(InventoryViewModel.Action.OnDeleteClick) },
            onDeleteConfirm = { onAction(InventoryViewModel.Action.OnDeleteConfirmed) },
            onDeleteCancel = { onAction(InventoryViewModel.Action.OnDeleteCancelled) },
            onDismiss = { onAction(InventoryViewModel.Action.OnDismissSheet) },
        )
    }
}

/**
 * True while idle or scrolling toward the top of the list, false while scrolling down --
 * standard derived-state technique for collapsing/hiding a FAB alongside list scroll direction,
 * since [LazyListState] has no built-in scroll-direction signal.
 */
@Composable
private fun LazyListState.isScrollingUp(): State<Boolean> {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            val scrollingUp = if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousScrollOffset >= firstVisibleItemScrollOffset
            }
            previousIndex = firstVisibleItemIndex
            previousScrollOffset = firstVisibleItemScrollOffset
            scrollingUp
        }
    }
}

internal object InventoryScreen {
    object Tag {
        const val ROOT = "InventoryScreen"
        const val EMPTY_STATE = "$ROOT.emptyState"
        const val LIST = "$ROOT.list"

        fun itemCard(id: Long) = "$ROOT.itemCard.$id"
    }
}
