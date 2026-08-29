package com.shashluchok.skinwatch.presentation.component.modal.host

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Ambient access to the single app-level [ModalHost]. Justified as a `CompositionLocal` (unlike
 * plumbing between two specific known points) because it is a genuine ambient service that needs
 * to be reachable from arbitrary screen/sheet-content depth, not a fixed two-point handoff.
 */
internal val LocalModalHost = staticCompositionLocalOf<ModalHost> {
    error("No ModalHost provided")
}
