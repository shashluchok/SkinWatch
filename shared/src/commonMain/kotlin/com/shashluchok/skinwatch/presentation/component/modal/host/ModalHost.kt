package com.shashluchok.skinwatch.presentation.component.modal.host

import androidx.compose.runtime.Composable

/**
 * Content registry for the single app-level modal (bottom sheet or alert). [Show] is the only way
 * to present content: it stays visible for as long as the caller stays in composition, then clears
 * itself.
 */
internal interface ModalHost {
    val currentRequest: ModalRequest?

    @Composable
    fun Show(request: ModalRequest)

    companion object {
        val EMPTY = object : ModalHost {
            override val currentRequest: ModalRequest? = null

            @Composable
            override fun Show(request: ModalRequest) = Unit
        }
    }
}
