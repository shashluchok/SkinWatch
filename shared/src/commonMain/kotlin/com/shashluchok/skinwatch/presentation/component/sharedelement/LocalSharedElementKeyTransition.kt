package com.shashluchok.skinwatch.presentation.component.sharedelement

import androidx.compose.animation.core.Transition
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The single [Transition] driving both endpoints of a shared-element flight -- the source's exit
 * and the destination's enter -- keyed by whichever id currently identifies the item in flight, so
 * their shared-bounds animation never drifts out of sync frame to frame.
 */
internal val LocalSharedElementKeyTransition = staticCompositionLocalOf<Transition<Long?>> {
    error("No LocalSharedElementKeyTransition provided")
}
