package com.shashluchok.skinwatch.presentation.screen.settings.component.synclog

import androidx.lifecycle.viewModelScope
import com.shashluchok.skinwatch.domain.synclog.InspectSyncStateInteractor
import com.shashluchok.skinwatch.domain.synclog.SyncLogEntry
import com.shashluchok.skinwatch.domain.synclog.SyncLogExporter
import com.shashluchok.skinwatch.domain.synclog.SyncLogLevel
import com.shashluchok.skinwatch.domain.synclog.SyncLogRepository
import com.shashluchok.skinwatch.domain.synclog.SyncLogTag
import com.shashluchok.skinwatch.presentation.screen.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class SyncLogViewModel(
    private val syncLogRepository: SyncLogRepository,
    private val syncLogExporter: SyncLogExporter,
    private val inspectSyncState: InspectSyncStateInteractor,
) : BaseViewModel<SyncLogViewModel.State, SyncLogViewModel.Action>() {
    /**
     * [visibleEntries] is materialised rather than derived on read: the log runs to thousands of
     * lines, and a filtered copy rebuilt on every recomposition would be rebuilt while scrolling it.
     * It only ever changes together with the three things it is derived from -- see [withEntries].
     *
     * Each entry keeps its position in the append-only [entries] list. That index is a stable
     * identity for the row: the list is shown newest-first, so a plain list position would move
     * under every row each time a line is written.
     */
    data class State(
        val entries: List<SyncLogEntry> = emptyList(),
        val visibleEntries: List<IndexedValue<SyncLogEntry>> = emptyList(),
        val levelFilter: SyncLogLevel? = null,
        val tagFilter: SyncLogTag? = null,
        val isExportSupported: Boolean = false,
        val exportResult: ExportResult? = null,
    )

    enum class ExportResult { SUCCEEDED, FAILED }

    sealed interface Action {
        data class OnLevelFilterSelected(
            val level: SyncLogLevel?,
        ) : Action

        data class OnTagFilterSelected(
            val tag: SyncLogTag?,
        ) : Action

        data object OnShareClick : Action

        data object OnClearClick : Action

        data object OnSnapshotClick : Action

        data object OnExportResultShown : Action
    }

    override val mutableStateFlow: MutableStateFlow<State> = MutableStateFlow(
        State(isExportSupported = syncLogExporter.isSupported),
    )

    init {
        subscribeToEntries()
        // Recorded on open so the log always starts with what the sync would decide right now, not
        // only with what happened while the app was being watched.
        takeStateSnapshot()
    }

    override fun onAction(action: Action) {
        when (action) {
            is Action.OnLevelFilterSelected -> state = state.withEntries(levelFilter = action.level)
            is Action.OnTagFilterSelected -> state = state.withEntries(tagFilter = action.tag)
            Action.OnShareClick -> onShareClick()
            Action.OnClearClick -> onClearClick()
            Action.OnSnapshotClick -> takeStateSnapshot()
            Action.OnExportResultShown -> state = state.copy(exportResult = null)
        }
    }

    private fun subscribeToEntries() {
        syncLogRepository
            .observeEntries()
            .onEach { state = state.withEntries(entries = it) }
            .launchIn(viewModelScope)
    }

    private fun takeStateSnapshot() {
        viewModelScope.launch { inspectSyncState() }
    }

    /** Exports everything, not the filtered view: a filter is for reading, not for what gets sent. */
    private fun onShareClick() {
        viewModelScope.launch {
            val succeeded = syncLogExporter.export(SyncLogFormatter.formatExport(state.entries))
            state = state.copy(
                exportResult = if (succeeded) ExportResult.SUCCEEDED else ExportResult.FAILED,
            )
        }
    }

    private fun onClearClick() {
        syncLogRepository.clear()
    }
}

/** The one place the filtered view is rebuilt, so it cannot drift from what it is derived from. */
private fun SyncLogViewModel.State.withEntries(
    entries: List<SyncLogEntry> = this.entries,
    levelFilter: SyncLogLevel? = this.levelFilter,
    tagFilter: SyncLogTag? = this.tagFilter,
): SyncLogViewModel.State = copy(
    entries = entries,
    levelFilter = levelFilter,
    tagFilter = tagFilter,
    visibleEntries = entries
        .withIndex()
        .filter { (_, entry) ->
            (levelFilter == null || entry.level == levelFilter) &&
                (tagFilter == null || entry.tag == tagFilter)
        }.asReversed(),
)
