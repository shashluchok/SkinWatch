package com.shashluchok.skinwatch.presentation.component.modal.host

import androidx.compose.runtime.Composable
import com.shashluchok.skinwatch.presentation.component.modal.alert.AlertPresentation
import com.shashluchok.skinwatch.presentation.component.modal.bottomsheet.BottomSheetPresentation
import dev.chrisbanes.haze.HazeState

/**
 * Single app-level render site for whatever [LocalModalHost]'s active request is -- a bottom
 * sheet or an alert, depending on its [ModalRequest.Appearance].
 * Both presentations are called unconditionally and each reads [LocalModalHost] itself to decide
 * whether it has anything to show, rather than this function narrowing the request up front.
 */
@Composable
internal fun ModalHostContent(hazeState: HazeState) {
    BottomSheetPresentation(hazeState = hazeState)
    AlertPresentation(hazeState = hazeState)
}
