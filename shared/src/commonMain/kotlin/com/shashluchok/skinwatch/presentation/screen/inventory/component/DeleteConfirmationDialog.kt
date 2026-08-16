package com.shashluchok.skinwatch.presentation.screen.inventory.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__dialog_delete_item__cancel_button
import com.shashluchok.skinwatch.resources.dev__dialog_delete_item__confirm_button
import com.shashluchok.skinwatch.resources.dev__dialog_delete_item__message
import com.shashluchok.skinwatch.resources.dev__dialog_delete_item__title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier.testTag(DeleteConfirmationDialog.Tag.ROOT),
        onDismissRequest = onCancel,
        title = { Text(text = stringResource(Res.string.dev__dialog_delete_item__title)) },
        text = { Text(text = stringResource(Res.string.dev__dialog_delete_item__message)) },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag(DeleteConfirmationDialog.Tag.CONFIRM_BUTTON),
                onClick = onConfirm,
            ) {
                Text(text = stringResource(Res.string.dev__dialog_delete_item__confirm_button))
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.testTag(DeleteConfirmationDialog.Tag.CANCEL_BUTTON),
                onClick = onCancel,
            ) {
                Text(text = stringResource(Res.string.dev__dialog_delete_item__cancel_button))
            }
        },
    )
}

internal object DeleteConfirmationDialog {
    object Tag {
        const val ROOT = "DeleteConfirmationDialog"
        const val CONFIRM_BUTTON = "$ROOT.confirmButton"
        const val CANCEL_BUTTON = "$ROOT.cancelButton"
    }
}
