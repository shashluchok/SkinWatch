package com.shashluchok.skinwatch.presentation.component.bottomsheet

import androidx.compose.runtime.Composable

/**
 * Content registry for the single app-level bottom sheet. [Show] is the only way to present
 * content: it stays visible for as long as the caller stays in composition, then clears itself.
 */
internal interface BottomSheetHost {
    val currentRequest: BottomSheetRequest?

    @Composable
    fun Show(request: BottomSheetRequest)

    companion object {
        val EMPTY = object : BottomSheetHost {
            override val currentRequest: BottomSheetRequest? = null

            @Composable
            override fun Show(request: BottomSheetRequest) = Unit
        }
    }
}
