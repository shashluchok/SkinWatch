package com.shashluchok.skinwatch.presentation.screen.main

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.shashluchok.skinwatch.presentation.component.modal.host.LocalModalHost
import com.shashluchok.skinwatch.presentation.component.modal.host.ModalHostImpl
import com.shashluchok.skinwatch.presentation.component.modal.host.ModalRequest
import com.shashluchok.skinwatch.presentation.component.sharedelement.LocalSharedElementConfig
import com.shashluchok.skinwatch.presentation.component.sharedelement.LocalSharedElementKeyTransition
import com.shashluchok.skinwatch.presentation.component.sharedelement.SharedElementConfig
import com.shashluchok.skinwatch.presentation.theme.LocalMotion

private const val ALERT_KEY_TRANSITION_LABEL = "AlertKeyTransition"

/**
 * All ambient services [MainScreen]'s subtree needs (modal host, shared-element scope/bounds
 * transform, the alert key transition) provided in one place, rather than scattered across
 * [MainScreen]'s own body. Deliberately not folded into `AppTheme`: that also wraps `SplashScreen`,
 * which has no use for any of this modal/shared-element machinery.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun ProvideMainScreenAmbients(content: @Composable () -> Unit) {
    val modalHost = remember { ModalHostImpl() }
    val motion = LocalMotion.current

    val boundsTransform = remember(motion) {
        BoundsTransform { _, _ ->
            tween(durationMillis = motion.duration.standard, easing = motion.easing.standard)
        }
    }

    val sharedElementKeyTransition = updateTransition(
        targetState = (modalHost.currentRequest?.appearance as? ModalRequest.Appearance.Alert)?.key,
        label = ALERT_KEY_TRANSITION_LABEL,
    )

    SharedTransitionLayout {
        val sharedElementConfig = SharedElementConfig(
            scope = this@SharedTransitionLayout,
            boundsTransform = boundsTransform,
        )
        CompositionLocalProvider(
            LocalModalHost provides modalHost,
            LocalSharedElementConfig provides sharedElementConfig,
            LocalSharedElementKeyTransition provides sharedElementKeyTransition,
        ) {
            content()
        }
    }
}
