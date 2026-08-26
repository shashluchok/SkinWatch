package com.shashluchok.skinwatch.presentation.screen.settings.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import com.shashluchok.skinwatch.presentation.component.BottomSheetTitleBar
import com.shashluchok.skinwatch.presentation.screen.settings.currencyLabel
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_settings__currency_auto_option
import com.shashluchok.skinwatch.resources.dev__screen_settings__currency_picker__title
import org.jetbrains.compose.resources.stringResource

/** Content of the currency picker bottom sheet. */
@Composable
internal fun CurrencyPickerBottomSheetContent(
    selectedCurrency: SteamCurrency?,
    resolvedAutoCurrency: SteamCurrency,
    onOptionSelect: (SteamCurrency?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current

    Column(modifier = modifier.testTag(CurrencyPickerBottomSheetContent.Tag.ROOT)) {
        BottomSheetTitleBar(title = stringResource(Res.string.dev__screen_settings__currency_picker__title))
        Column(modifier = Modifier.padding(dimens.padding.medium)) {
            CurrencyOptionRow(
                label = stringResource(
                    Res.string.dev__screen_settings__currency_auto_option,
                    stringResource(currencyLabel(resolvedAutoCurrency)),
                ),
                isSelected = selectedCurrency == null,
                onClick = { onOptionSelect(null) },
                testTag = CurrencyPickerBottomSheetContent.Tag.AUTO_OPTION,
            )
            SteamCurrency.entries.forEach { currency ->
                CurrencyOptionRow(
                    label = stringResource(currencyLabel(currency)),
                    isSelected = selectedCurrency == currency,
                    onClick = { onOptionSelect(currency) },
                    testTag = "${CurrencyPickerBottomSheetContent.Tag.OPTION_PREFIX}${currency.name}",
                )
            }
        }
    }
}

@Composable
private fun CurrencyOptionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = dimens.padding.small)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Text(
            modifier = Modifier.padding(start = dimens.padding.small),
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

internal object CurrencyPickerBottomSheetContent {
    object Tag {
        const val ROOT = "CurrencyPickerBottomSheetContent"
        const val AUTO_OPTION = "$ROOT.autoOption"
        const val OPTION_PREFIX = "$ROOT.option."
    }
}
