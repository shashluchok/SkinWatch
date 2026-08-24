package com.shashluchok.skinwatch.presentation.component.bottomsheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class BottomSheetHostImpl : BottomSheetHost {
    override var currentRequest: BottomSheetRequest? by mutableStateOf(null)
        private set

    @Composable
    override fun Show(request: BottomSheetRequest) {
        currentRequest = request
        DisposableEffect(Unit) {
            onDispose { currentRequest = null }
        }
    }
}
