package com.shashluchok.skinwatch.presentation.component.sharedelement

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * [scope] and [boundsTransform] are constant for the whole app, provided once by
 * `MainScreenAmbients`. Kept together since every reader wants both at once.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
internal data class SharedElementConfig(
    val scope: SharedTransitionScope,
    val boundsTransform: BoundsTransform,
)

internal val LocalSharedElementConfig = staticCompositionLocalOf<SharedElementConfig> {
    error("No SharedElementConfig provided")
}
