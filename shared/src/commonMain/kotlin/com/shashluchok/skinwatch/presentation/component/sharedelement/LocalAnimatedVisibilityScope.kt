package com.shashluchok.skinwatch.presentation.component.sharedelement

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The [AnimatedVisibilityScope] a shared element flies within, for the one case where it can't be
 * a plain function argument: content stored behind an opaque `@Composable () -> Unit` (see
 * `ModalRequest.content`), whose signature is deliberately not tied to shared elements. Composables
 * called directly (e.g. `InventoryItemCard`) take it as a regular parameter instead.
 */
internal val LocalAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope> {
    error("No AnimatedVisibilityScope provided")
}
