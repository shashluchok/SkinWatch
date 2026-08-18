package com.shashluchok.skinwatch.presentation.screen.main.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.shashluchok.skinwatch.domain.steam.SteamMarketItem
import com.shashluchok.skinwatch.presentation.component.ItemDetailsForm
import com.shashluchok.skinwatch.presentation.component.PartialHeightModalBottomSheet
import com.shashluchok.skinwatch.presentation.screen.main.MainViewModel
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_inventory__add_details__title
import com.shashluchok.skinwatch.resources.dev__screen_inventory__add_search__empty_results
import com.shashluchok.skinwatch.resources.dev__screen_inventory__add_search__error
import com.shashluchok.skinwatch.resources.dev__screen_inventory__add_search__query_placeholder
import com.shashluchok.skinwatch.resources.dev__screen_inventory__add_search__taking_longer
import com.shashluchok.skinwatch.resources.dev__screen_inventory__add_search__title
import com.shashluchok.skinwatch.resources.dev__screen_inventory__item_form__save_button
import org.jetbrains.compose.resources.stringResource

private const val ADD_ITEM_BOTTOM_SHEET_STEP_LABEL = "AddItemBottomSheetStep"

@Composable
internal fun AddItemBottomSheet(
    sheet: MainViewModel.AddSheetState,
    onQueryChange: (String) -> Unit,
    onResultSelect: (SteamMarketItem) -> Unit,
    onBackClick: () -> Unit,
    onQuantityChange: (String) -> Unit,
    onPurchasePriceChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PartialHeightModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(AddItemBottomSheet.Tag.ROOT),
    ) {
        AnimatedContent(
            targetState = sheet,
            contentKey = { it::class },
            label = ADD_ITEM_BOTTOM_SHEET_STEP_LABEL,
        ) { targetSheet ->
            when (targetSheet) {
                is MainViewModel.AddSheetState.AddSearch -> AddSearchStep(
                    sheet = targetSheet,
                    onQueryChange = onQueryChange,
                    onResultSelect = onResultSelect,
                )

                is MainViewModel.AddSheetState.AddDetails -> AddDetailsStep(
                    sheet = targetSheet,
                    onBackClick = onBackClick,
                    onQuantityChange = onQuantityChange,
                    onPurchasePriceChange = onPurchasePriceChange,
                    onSaveClick = onSaveClick,
                )
            }
        }
    }
}

@Composable
private fun AddSearchStep(
    sheet: MainViewModel.AddSheetState.AddSearch,
    onQueryChange: (String) -> Unit,
    onResultSelect: (SteamMarketItem) -> Unit,
) {
    val dimens = LocalDimens.current

    Column(modifier = Modifier.padding(dimens.padding.medium)) {
        Text(
            text = stringResource(Res.string.dev__screen_inventory__add_search__title),
            style = MaterialTheme.typography.titleLarge,
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimens.padding.small)
                .testTag(AddItemBottomSheet.Tag.SEARCH_QUERY_FIELD),
            value = sheet.query,
            onValueChange = onQueryChange,
            label = { Text(text = stringResource(Res.string.dev__screen_inventory__add_search__query_placeholder)) },
            keyboardOptions = KeyboardOptions.Default,
        )
        when (val status = sheet.status) {
            MainViewModel.SearchStatus.Idle -> Unit
            MainViewModel.SearchStatus.Searching -> CircularProgressIndicator(
                modifier = Modifier.padding(top = dimens.padding.medium),
            )

            MainViewModel.SearchStatus.TakingLonger -> Column {
                CircularProgressIndicator(modifier = Modifier.padding(top = dimens.padding.medium))
                Text(
                    modifier = Modifier.padding(top = dimens.padding.small),
                    text = stringResource(Res.string.dev__screen_inventory__add_search__taking_longer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is MainViewModel.SearchStatus.Loaded -> if (status.results.isEmpty()) {
                Text(
                    modifier = Modifier.padding(top = dimens.padding.medium),
                    text = stringResource(Res.string.dev__screen_inventory__add_search__empty_results),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(top = dimens.padding.small)
                        .testTag(AddItemBottomSheet.Tag.SEARCH_RESULTS_LIST),
                ) {
                    items(items = status.results, key = { it.marketHashName }) { result ->
                        SearchResultRow(
                            result = result,
                            onClick = { onResultSelect(result) },
                        )
                    }
                }
            }

            is MainViewModel.SearchStatus.Failed -> Text(
                modifier = Modifier.padding(top = dimens.padding.medium),
                text = stringResource(Res.string.dev__screen_inventory__add_search__error),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun AddDetailsStep(
    sheet: MainViewModel.AddSheetState.AddDetails,
    onBackClick: () -> Unit,
    onQuantityChange: (String) -> Unit,
    onPurchasePriceChange: (String) -> Unit,
    onSaveClick: () -> Unit,
) {
    val dimens = LocalDimens.current

    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = true,
        onBackCompleted = onBackClick,
    )

    Column(modifier = Modifier.padding(dimens.padding.medium)) {
        Text(
            text = stringResource(Res.string.dev__screen_inventory__add_details__title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(text = sheet.selected.displayName, style = MaterialTheme.typography.bodyLarge)
        ItemDetailsForm(
            quantity = sheet.quantity,
            purchasePrice = sheet.purchasePrice,
            validationError = sheet.validationError,
            onQuantityChange = onQuantityChange,
            onPurchasePriceChange = onPurchasePriceChange,
        )
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimens.padding.small)
                .testTag(AddItemBottomSheet.Tag.SAVE_BUTTON),
            onClick = onSaveClick,
        ) {
            Text(text = stringResource(Res.string.dev__screen_inventory__item_form__save_button))
        }
    }
}

internal object AddItemBottomSheet {
    object Tag {
        const val ROOT = "AddItemBottomSheet"
        const val SEARCH_QUERY_FIELD = "$ROOT.searchQueryField"
        const val SEARCH_RESULTS_LIST = "$ROOT.searchResultsList"
        const val SAVE_BUTTON = "$ROOT.saveButton"
    }
}
