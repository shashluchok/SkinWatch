package com.shashluchok.skinwatch.presentation.screen.settings.component.synclog

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashluchok.skinwatch.domain.synclog.SyncLogEntry
import com.shashluchok.skinwatch.domain.synclog.SyncLogLevel
import com.shashluchok.skinwatch.domain.synclog.SyncLogTag
import com.shashluchok.skinwatch.presentation.theme.AppFontFamilies
import com.shashluchok.skinwatch.presentation.theme.LocalDimens
import com.shashluchok.skinwatch.resources.Res
import com.shashluchok.skinwatch.resources.dev__screen_sync_log__button__back
import com.shashluchok.skinwatch.resources.dev__screen_sync_log__button__clear
import com.shashluchok.skinwatch.resources.dev__screen_sync_log__button__share
import com.shashluchok.skinwatch.resources.dev__screen_sync_log__button__snapshot
import com.shashluchok.skinwatch.resources.dev__screen_sync_log__empty_state
import com.shashluchok.skinwatch.resources.dev__screen_sync_log__entry_count
import com.shashluchok.skinwatch.resources.dev__screen_sync_log__export_status__failed
import com.shashluchok.skinwatch.resources.dev__screen_sync_log__export_status__succeeded
import com.shashluchok.skinwatch.resources.dev__screen_sync_log__filter_chip__all
import com.shashluchok.skinwatch.resources.dev__screen_sync_log__title
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.seconds

private val LOG_LINE_FONT_SIZE = 10.sp
private val LOG_LINE_HEIGHT = 14.sp
private val EXPORT_STATUS_DURATION = 3.seconds

/**
 * Diagnostic price-sync log, shown inside the debug panel's sheet rather than as its own destination
 * -- it is a temporary investigation aid, not part of the app's navigation.
 */
@Composable
internal fun SyncLogContent(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SyncLogViewModel = koinViewModel(),
) {
    val state = viewModel.stateFlow.collectAsStateWithLifecycle().value
    SyncLogContent(
        modifier = modifier,
        state = state,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
    )
}

@Composable
private fun SyncLogContent(
    state: SyncLogViewModel.State,
    onAction: (SyncLogViewModel.Action) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current

    Column(
        modifier = modifier
            .testTag(SyncLogContent.Tag.ROOT)
            .padding(horizontal = dimens.padding.medium),
    ) {
        SyncLogHeader(
            entryCount = state.entries.size,
            visibleCount = state.visibleEntries.size,
            isExportSupported = state.isExportSupported,
            onBackClick = onBackClick,
            onShareClick = { onAction(SyncLogViewModel.Action.OnShareClick) },
            onClearClick = { onAction(SyncLogViewModel.Action.OnClearClick) },
            onSnapshotClick = { onAction(SyncLogViewModel.Action.OnSnapshotClick) },
        )
        SyncLogExportStatus(
            result = state.exportResult,
            onDismiss = { onAction(SyncLogViewModel.Action.OnExportResultShown) },
        )
        SyncLogLevelFilter(
            selected = state.levelFilter,
            onSelect = { onAction(SyncLogViewModel.Action.OnLevelFilterSelected(it)) },
        )
        SyncLogTagFilter(
            selected = state.tagFilter,
            onSelect = { onAction(SyncLogViewModel.Action.OnTagFilterSelected(it)) },
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = dimens.padding.small))
        SyncLogLines(entries = state.visibleEntries)
    }
}

@Composable
private fun SyncLogHeader(
    entryCount: Int,
    visibleCount: Int,
    isExportSupported: Boolean,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onClearClick: () -> Unit,
    onSnapshotClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.dev__screen_sync_log__title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(Res.string.dev__screen_sync_log__entry_count, visibleCount, entryCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                modifier = Modifier.testTag(SyncLogContent.Tag.BACK_BUTTON),
                onClick = onBackClick,
            ) {
                Text(text = stringResource(Res.string.dev__screen_sync_log__button__back))
            }
            if (isExportSupported) {
                TextButton(
                    modifier = Modifier.testTag(SyncLogContent.Tag.SHARE_BUTTON),
                    onClick = onShareClick,
                ) {
                    Text(text = stringResource(Res.string.dev__screen_sync_log__button__share))
                }
            }
            TextButton(
                modifier = Modifier.testTag(SyncLogContent.Tag.SNAPSHOT_BUTTON),
                onClick = onSnapshotClick,
            ) {
                Text(text = stringResource(Res.string.dev__screen_sync_log__button__snapshot))
            }
            TextButton(
                modifier = Modifier.testTag(SyncLogContent.Tag.CLEAR_BUTTON),
                onClick = onClearClick,
            ) {
                Text(text = stringResource(Res.string.dev__screen_sync_log__button__clear))
            }
        }
    }
}

@Composable
private fun SyncLogExportStatus(
    result: SyncLogViewModel.ExportResult?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (result == null) return

    val currentOnDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(result) {
        delay(EXPORT_STATUS_DURATION)
        currentOnDismiss()
    }
    Text(
        modifier = modifier.fillMaxWidth(),
        text = when (result) {
            SyncLogViewModel.ExportResult.SUCCEEDED ->
                stringResource(Res.string.dev__screen_sync_log__export_status__succeeded)

            SyncLogViewModel.ExportResult.FAILED ->
                stringResource(Res.string.dev__screen_sync_log__export_status__failed)
        },
        style = MaterialTheme.typography.bodySmall,
        color = when (result) {
            SyncLogViewModel.ExportResult.SUCCEEDED -> MaterialTheme.colorScheme.primary
            SyncLogViewModel.ExportResult.FAILED -> MaterialTheme.colorScheme.error
        },
    )
}

@Composable
private fun SyncLogLevelFilter(
    selected: SyncLogLevel?,
    onSelect: (SyncLogLevel?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(dimens.padding.extraSmall),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(text = stringResource(Res.string.dev__screen_sync_log__filter_chip__all)) },
        )
        SyncLogLevel.entries.forEach { level ->
            FilterChip(
                selected = selected == level,
                onClick = { onSelect(level.takeIf { it != selected }) },
                label = { Text(text = level.name) },
            )
        }
    }
}

@Composable
private fun SyncLogTagFilter(
    selected: SyncLogTag?,
    onSelect: (SyncLogTag?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(dimens.padding.extraSmall),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(text = stringResource(Res.string.dev__screen_sync_log__filter_chip__all)) },
        )
        SyncLogTag.entries.forEach { tag ->
            FilterChip(
                selected = selected == tag,
                onClick = { onSelect(tag.takeIf { it != selected }) },
                label = { Text(text = tag.name) },
            )
        }
    }
}

@Composable
private fun SyncLogLines(
    entries: List<IndexedValue<SyncLogEntry>>,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current

    if (entries.isEmpty()) {
        Text(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = dimens.padding.large),
            text = stringResource(Res.string.dev__screen_sync_log__empty_state),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        return
    }
    // Selectable so a single suspicious line can be copied out without exporting the whole file.
    SelectionContainer {
        LazyColumn(
            modifier = modifier.testTag(SyncLogContent.Tag.LINES),
            verticalArrangement = Arrangement.spacedBy(dimens.padding.tiny),
        ) {
            items(items = entries, key = { it.index }) { entry ->
                SyncLogLine(entry = entry.value)
            }
        }
    }
}

@Composable
private fun SyncLogLine(entry: SyncLogEntry, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.fillMaxWidth(),
        text = SyncLogFormatter.formatLine(entry),
        fontFamily = AppFontFamilies.jetBrainsMono,
        fontSize = LOG_LINE_FONT_SIZE,
        lineHeight = LOG_LINE_HEIGHT,
        color = entry.level.lineColor(),
    )
}

@Composable
private fun SyncLogLevel.lineColor(): Color = when (this) {
    SyncLogLevel.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    SyncLogLevel.WARN -> MaterialTheme.colorScheme.primary
    SyncLogLevel.ERROR -> MaterialTheme.colorScheme.error
}

internal object SyncLogContent {
    object Tag {
        const val ROOT = "SyncLogContent"
        const val BACK_BUTTON = "$ROOT.backButton"
        const val SHARE_BUTTON = "$ROOT.shareButton"
        const val CLEAR_BUTTON = "$ROOT.clearButton"
        const val SNAPSHOT_BUTTON = "$ROOT.snapshotButton"
        const val LINES = "$ROOT.lines"
    }
}
