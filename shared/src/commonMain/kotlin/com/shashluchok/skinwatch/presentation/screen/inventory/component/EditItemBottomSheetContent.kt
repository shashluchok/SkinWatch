package com.shashluchok.skinwatch.presentation.screen.inventory.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.shashluchok.skinwatch.presentation.component.BottomSheetTitleBar
import com.shashluchok.skinwatch.presentation.component.ItemDetailsForm
import com.shashluchok.skinwatch.presentation.screen.inventory.InventoryViewModel
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_inventory__edit_sheet__title
import com.shashluchok.skinwatch.resources.dev__screen_inventory__item_form__save_button
import org.jetbrains.compose.resources.stringResource

/** Deletion is only reachable from the item card's long-press context menu, not from here. */
@Composable
internal fun EditItemBottomSheetContent(
    sheet: InventoryViewModel.EditSheetState,
    onQuantityChange: (String) -> Unit,
    onPurchasePriceChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current

    Column(modifier = modifier.testTag(EditItemBottomSheetContent.Tag.ROOT)) {
        BottomSheetTitleBar(title = stringResource(Res.string.dev__screen_inventory__edit_sheet__title))
        Column(modifier = Modifier.padding(dimens.padding.medium)) {
            Text(text = sheet.item.marketHashName, style = MaterialTheme.typography.bodyLarge)
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
                    .testTag(EditItemBottomSheetContent.Tag.SAVE_BUTTON),
                onClick = onSaveClick,
            ) {
                Text(text = stringResource(Res.string.dev__screen_inventory__item_form__save_button))
            }
        }
    }
}

internal object EditItemBottomSheetContent {
    object Tag {
        const val ROOT = "EditItemBottomSheetContent"
        const val SAVE_BUTTON = "$ROOT.saveButton"
    }
}
