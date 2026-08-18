package com.shashluchok.skinwatch.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_inventory__item_form__price_error
import com.shashluchok.skinwatch.resources.dev__screen_inventory__item_form__purchase_price_label
import com.shashluchok.skinwatch.resources.dev__screen_inventory__item_form__quantity_error
import com.shashluchok.skinwatch.resources.dev__screen_inventory__item_form__quantity_label
import org.jetbrains.compose.resources.stringResource

internal enum class ValidationError { INVALID_QUANTITY, INVALID_PRICE }

/**
 * Shared by the add-flow's details step (`MainScreen`) and the edit sheet (`InventoryScreen`) --
 * both edit the same two fields with the same copy (only quantity/purchasePrice are ever editable
 * from this screen).
 */
@Composable
internal fun ItemDetailsForm(
    quantity: String,
    purchasePrice: String,
    validationError: ValidationError?,
    onQuantityChange: (String) -> Unit,
    onPurchasePriceChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current

    Column(modifier = modifier.padding(dimens.padding.medium)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = quantity,
            onValueChange = onQuantityChange,
            label = { Text(text = stringResource(Res.string.dev__screen_inventory__item_form__quantity_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = validationError == ValidationError.INVALID_QUANTITY,
            supportingText = {
                if (validationError == ValidationError.INVALID_QUANTITY) {
                    Text(text = stringResource(Res.string.dev__screen_inventory__item_form__quantity_error))
                }
            },
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimens.padding.small),
            value = purchasePrice,
            onValueChange = onPurchasePriceChange,
            label = { Text(text = stringResource(Res.string.dev__screen_inventory__item_form__purchase_price_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = validationError == ValidationError.INVALID_PRICE,
            supportingText = {
                if (validationError == ValidationError.INVALID_PRICE) {
                    Text(text = stringResource(Res.string.dev__screen_inventory__item_form__price_error))
                }
            },
        )
    }
}
