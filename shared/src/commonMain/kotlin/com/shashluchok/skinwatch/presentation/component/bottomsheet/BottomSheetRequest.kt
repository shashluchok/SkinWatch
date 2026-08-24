package com.shashluchok.skinwatch.presentation.component.bottomsheet

import androidx.compose.runtime.Composable

/**
 * What the single app-level [PartialHeightModalBottomSheet] instance should currently render, and
 * who should be notified when it is dismissed.
 */
internal data class BottomSheetRequest(
    val onDismissRequest: () -> Unit,
    val content: @Composable () -> Unit,
)
