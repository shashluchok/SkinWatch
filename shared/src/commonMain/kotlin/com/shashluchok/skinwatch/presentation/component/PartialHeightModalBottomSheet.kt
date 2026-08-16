package com.shashluchok.skinwatch.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private const val PARTIAL_HEIGHT_FRACTION = 2f / 3f

/**
 * A [ModalBottomSheet] that always opens to two-thirds of the screen height rather than sizing
 * itself to its content, and lets the user drag it the rest of the way to fill the screen.
 * Material3's own partial-expand height is driven by content measurement, not a fixed fraction,
 * so the fraction is applied to the content itself and swapped to full height once the sheet's
 * target state reaches [SheetValue.Expanded] -- the sheet's own modifier still needs
 * [Modifier.fillMaxHeight] for that expansion to have somewhere to grow into.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PartialHeightModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier.fillMaxHeight(),
        contentWindowInsets = { WindowInsets.safeDrawing },
    ) {
        val heightFraction = if (sheetState.targetValue == SheetValue.Expanded) 1f else PARTIAL_HEIGHT_FRACTION
        Box(modifier = Modifier.fillMaxHeight(heightFraction)) {
            content()
        }
    }
}
