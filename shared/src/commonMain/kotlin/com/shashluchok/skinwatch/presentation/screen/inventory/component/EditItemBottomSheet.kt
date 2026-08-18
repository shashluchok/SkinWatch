package com.shashluchok.skinwatch.presentation.screen.inventory.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.shashluchok.skinwatch.presentation.component.ItemDetailsForm
import com.shashluchok.skinwatch.presentation.component.PartialHeightModalBottomSheet
import com.shashluchok.skinwatch.presentation.screen.inventory.InventoryViewModel
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_inventory__edit_sheet__delete_button
import com.shashluchok.skinwatch.resources.dev__screen_inventory__edit_sheet__title
import com.shashluchok.skinwatch.resources.dev__screen_inventory__item_form__save_button
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EditItemBottomSheet(
    sheet: InventoryViewModel.EditSheetState,
    onQuantityChange: (String) -> Unit,
    onPurchasePriceChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteCancel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current

    PartialHeightModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(EditItemBottomSheet.Tag.ROOT),
    ) {
        Column(modifier = Modifier.padding(dimens.padding.medium)) {
            Text(
                text = stringResource(Res.string.dev__screen_inventory__edit_sheet__title),
                style = MaterialTheme.typography.titleLarge,
            )
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
                    .testTag(EditItemBottomSheet.Tag.SAVE_BUTTON),
                onClick = onSaveClick,
            ) {
                Text(text = stringResource(Res.string.dev__screen_inventory__item_form__save_button))
            }
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimens.padding.small)
                    .testTag(EditItemBottomSheet.Tag.DELETE_BUTTON),
                onClick = onDeleteClick,
            ) {
                Text(text = stringResource(Res.string.dev__screen_inventory__edit_sheet__delete_button))
            }
        }
    }

    if (sheet.showDeleteConfirmation) {
        DeleteConfirmationDialog(
            onConfirm = onDeleteConfirm,
            onCancel = onDeleteCancel,
        )
    }
}

internal object EditItemBottomSheet {
    object Tag {
        const val ROOT = "EditItemBottomSheet"
        const val SAVE_BUTTON = "$ROOT.saveButton"
        const val DELETE_BUTTON = "$ROOT.deleteButton"
    }
}
