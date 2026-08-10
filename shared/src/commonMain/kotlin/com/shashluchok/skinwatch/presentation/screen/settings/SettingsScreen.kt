package com.shashluchok.skinwatch.presentation.screen.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_settings__empty_state
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

@Suppress("UnusedParameter")
@Composable
private fun SettingsScreen(
    state: SettingsViewModel.State,
    onAction: (SettingsViewModel.Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(SettingsScreen.Tag.ROOT),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = LocalDimens.current.padding.medium)
                .testTag(SettingsScreen.Tag.EMPTY_STATE),
            text = stringResource(Res.string.dev__screen_settings__empty_state),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal object SettingsScreen {
    object Tag {
        const val ROOT = "SettingsScreen"
        const val EMPTY_STATE = "$ROOT.emptyState"
    }
}
