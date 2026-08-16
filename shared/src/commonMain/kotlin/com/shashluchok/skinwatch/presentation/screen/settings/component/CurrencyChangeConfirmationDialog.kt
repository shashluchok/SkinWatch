package com.shashluchok.skinwatch.presentation.screen.settings.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.shashluchok.skinwatch.presentation.screen.settings.SettingsViewModel
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__dialog_currency_change__cancel_button
import com.shashluchok.skinwatch.resources.dev__dialog_currency_change__confirm_button
import com.shashluchok.skinwatch.resources.dev__dialog_currency_change__error_message
import com.shashluchok.skinwatch.resources.dev__dialog_currency_change__message
import com.shashluchok.skinwatch.resources.dev__dialog_currency_change__title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CurrencyChangeConfirmationDialog(
    status: SettingsViewModel.ConversionStatus,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isInProgress = status == SettingsViewModel.ConversionStatus.InProgress

    AlertDialog(
        modifier = modifier.testTag(CurrencyChangeConfirmationDialog.Tag.ROOT),
        onDismissRequest = onCancel,
        title = { Text(text = stringResource(Res.string.dev__dialog_currency_change__title)) },
        text = {
            Column {
                Text(text = stringResource(Res.string.dev__dialog_currency_change__message))
                if (status is SettingsViewModel.ConversionStatus.Failed) {
                    Text(
                        text = stringResource(Res.string.dev__dialog_currency_change__error_message),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag(CurrencyChangeConfirmationDialog.Tag.CONFIRM_BUTTON),
                enabled = !isInProgress,
                onClick = onConfirm,
            ) {
                if (isInProgress) {
                    CircularProgressIndicator()
                } else {
                    Text(text = stringResource(Res.string.dev__dialog_currency_change__confirm_button))
                }
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.testTag(CurrencyChangeConfirmationDialog.Tag.CANCEL_BUTTON),
                enabled = !isInProgress,
                onClick = onCancel,
            ) {
                Text(text = stringResource(Res.string.dev__dialog_currency_change__cancel_button))
            }
        },
    )
}

internal object CurrencyChangeConfirmationDialog {
    object Tag {
        const val ROOT = "CurrencyChangeConfirmationDialog"
        const val CONFIRM_BUTTON = "$ROOT.confirmButton"
        const val CANCEL_BUTTON = "$ROOT.cancelButton"
    }
}
