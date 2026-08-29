package com.shashluchok.skinwatch.presentation.component.modal.host

import androidx.compose.runtime.Composable

internal data class ModalRequest(
    val appearance: Appearance,
    val onDismissRequest: () -> Unit,
    val content: @Composable () -> Unit,
) {
    /**
     * How a [ModalRequest] should be rendered by the single app-level [ModalHost]. [Alert.key] drives
     * [com.shashluchok.skinwatch.presentation.component.sharedelement.LocalSharedElementKeyTransition],
     * keeping the source card's exit and the alert's enter on one shared animation clock.
     */
    internal sealed interface Appearance {
        data object BottomSheet : Appearance

        data class Alert(
            val key: Long,
        ) : Appearance
    }
}
