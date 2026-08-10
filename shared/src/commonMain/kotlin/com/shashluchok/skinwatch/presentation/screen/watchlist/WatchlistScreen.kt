package com.shashluchok.skinwatch.presentation.screen.watchlist

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
import com.shashluchok.skinwatch.resources.dev__screen_watchlist__empty_state
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun WatchlistScreen(
    modifier: Modifier = Modifier,
    viewModel: WatchlistViewModel = koinViewModel(),
) {
    val state = viewModel.stateFlow.collectAsStateWithLifecycle().value
    WatchlistScreen(
        modifier = modifier,
        state = state,
        onAction = viewModel::onAction,
    )
}

@Suppress("UnusedParameter")
@Composable
private fun WatchlistScreen(
    state: WatchlistViewModel.State,
    onAction: (WatchlistViewModel.Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(WatchlistScreen.Tag.ROOT),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = LocalDimens.current.padding.medium)
                .testTag(WatchlistScreen.Tag.EMPTY_STATE),
            text = stringResource(Res.string.dev__screen_watchlist__empty_state),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal object WatchlistScreen {
    object Tag {
        const val ROOT = "WatchlistScreen"
        const val EMPTY_STATE = "$ROOT.emptyState"
    }
}
