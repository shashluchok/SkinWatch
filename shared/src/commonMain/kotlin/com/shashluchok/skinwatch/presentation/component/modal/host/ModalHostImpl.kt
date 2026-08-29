package com.shashluchok.skinwatch.presentation.component.modal.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class ModalHostImpl : ModalHost {
    override var currentRequest: ModalRequest? by mutableStateOf(null)
        private set

    @Composable
    override fun Show(request: ModalRequest) {
        currentRequest = request
        DisposableEffect(Unit) {
            onDispose { currentRequest = null }
        }
    }
}
