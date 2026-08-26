package com.shashluchok.skinwatch.presentation.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashluchok.skinwatch.presentation.component.bottomsheet.BottomSheetRequest
import com.shashluchok.skinwatch.presentation.component.bottomsheet.LocalBottomSheetHost
import com.shashluchok.skinwatch.presentation.screen.settings.component.CurrencyChangeConfirmationDialog
import com.shashluchok.skinwatch.presentation.screen.settings.component.CurrencyPickerBottomSheetContent
import com.shashluchok.skinwatch.presentation.screen.settings.component.DebugPanel
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_settings__currency_auto_option
import com.shashluchok.skinwatch.resources.dev__screen_settings__currency_row__title
import com.shashluchok.skinwatch.resources.dev__screen_settings__debug_row__title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state = viewModel.stateFlow.collectAsStateWithLifecycle().value
    SettingsScreen(
        modifier = modifier,
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun SettingsScreen(
    state: SettingsViewModel.State,
    onAction: (SettingsViewModel.Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current

    Scaffold(modifier = modifier.testTag(SettingsScreen.Tag.ROOT)) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAction(SettingsViewModel.Action.OnCurrencyRowClick) }
                    .padding(dimens.padding.medium)
                    .testTag(SettingsScreen.Tag.CURRENCY_ROW),
            ) {
                Text(
                    text = stringResource(Res.string.dev__screen_settings__currency_row__title),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = state.currencyRowValue(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.isDebugPanelAvailable) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAction(SettingsViewModel.Action.OnDebugRowClick) }
                        .padding(dimens.padding.medium)
                        .testTag(SettingsScreen.Tag.DEBUG_ROW),
                    text = stringResource(Res.string.dev__screen_settings__debug_row__title),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }

    if (state.isCurrencyPickerVisible) {
        LocalBottomSheetHost.current.Show(
            BottomSheetRequest(
                onDismissRequest = { onAction(SettingsViewModel.Action.OnDismissCurrencyPicker) },
                content = {
                    CurrencyPickerBottomSheetContent(
                        selectedCurrency = state.selectedCurrency,
                        resolvedAutoCurrency = state.resolvedAutoCurrency,
                        onOptionSelect = { onAction(SettingsViewModel.Action.OnCurrencyOptionSelected(it)) },
                    )
                },
            ),
        )
    }

    if (state.isCurrencyChangeDialogVisible) {
        CurrencyChangeConfirmationDialog(
            status = state.conversionStatus,
            onConfirm = { onAction(SettingsViewModel.Action.OnCurrencyChangeConfirmed) },
            onCancel = { onAction(SettingsViewModel.Action.OnCurrencyChangeCancelled) },
        )
    }

    if (state.isDebugPanelVisible) {
        LocalBottomSheetHost.current.Show(
            BottomSheetRequest(
                onDismissRequest = { onAction(SettingsViewModel.Action.OnDismissDebugPanel) },
                content = { DebugPanel() },
            ),
        )
    }
}

@Composable
private fun SettingsViewModel.State.currencyRowValue(): String {
    val currency = selectedCurrency
    return if (currency != null) {
        stringResource(currencyLabel(currency))
    } else {
        stringResource(
            Res.string.dev__screen_settings__currency_auto_option,
            stringResource(currencyLabel(resolvedAutoCurrency)),
        )
    }
}

internal object SettingsScreen {
    object Tag {
        const val ROOT = "SettingsScreen"
        const val CURRENCY_ROW = "$ROOT.currencyRow"
        const val DEBUG_ROW = "$ROOT.debugRow"
    }
}
