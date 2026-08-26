package com.shashluchok.skinwatch.presentation.screen.settings.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_debug_panel__mock_data_row__title
import com.shashluchok.skinwatch.resources.dev__screen_debug_panel__show_splash_row__title
import com.shashluchok.skinwatch.resources.dev__screen_debug_panel__title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Content of the debug panel bottom sheet
 */
@Composable
internal fun DebugPanel(
    modifier: Modifier = Modifier,
    viewModel: DebugPanelViewModel = koinViewModel(),
) {
    val state = viewModel.stateFlow.collectAsStateWithLifecycle().value
    DebugPanel(
        modifier = modifier,
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun DebugPanel(
    state: DebugPanelViewModel.State,
    onAction: (DebugPanelViewModel.Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current

    Column(
        modifier = modifier
            .testTag(DebugPanel.Tag.ROOT)
            .padding(dimens.padding.medium),
    ) {
        Text(
            text = stringResource(Res.string.dev__screen_debug_panel__title),
            style = MaterialTheme.typography.titleLarge,
        )
        DebugSwitchRow(
            title = stringResource(Res.string.dev__screen_debug_panel__show_splash_row__title),
            checked = state.showSplashScreen,
            onCheckedChange = { onAction(DebugPanelViewModel.Action.OnShowSplashScreenToggled(it)) },
            testTag = DebugPanel.Tag.SHOW_SPLASH_SCREEN_SWITCH,
        )
        DebugSwitchRow(
            title = stringResource(Res.string.dev__screen_debug_panel__mock_data_row__title),
            checked = state.mockDataEnabled,
            onCheckedChange = { onAction(DebugPanelViewModel.Action.OnMockDataEnabledToggled(it)) },
            testTag = DebugPanel.Tag.MOCK_DATA_SWITCH,
        )
    }
}

@Composable
private fun DebugSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = dimens.padding.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(
            modifier = Modifier.testTag(testTag),
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

internal object DebugPanel {
    object Tag {
        const val ROOT = "DebugPanel"
        const val SHOW_SPLASH_SCREEN_SWITCH = "$ROOT.showSplashScreenSwitch"
        const val MOCK_DATA_SWITCH = "$ROOT.mockDataSwitch"
    }
}
