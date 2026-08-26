package com.shashluchok.skinwatch.presentation.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.shashluchok.skinwatch.data.image.ImageHttpClientFactory
import com.shashluchok.skinwatch.presentation.screen.main.MainScreen
import com.shashluchok.skinwatch.presentation.screen.splash.SplashScreen
import com.shashluchok.skinwatch.presentation.theme.AppTheme
import com.shashluchok.skinwatch.presentation.theme.LocalMotion
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalCoilApi::class)
@Composable
fun AppContent(modifier: Modifier = Modifier) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader
            .Builder(context)
            .components { add(KtorNetworkFetcherFactory(httpClient = ImageHttpClientFactory.create())) }
            .crossfade(true)
            .build()
    }

    val viewModel = koinViewModel<AppViewModel>()
    val state = viewModel.stateFlow.collectAsStateWithLifecycle().value

    AppContent(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
private fun AppContent(
    state: AppViewModel.State,
    onAction: (AppViewModel.Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppTheme {
        val motion = LocalMotion.current

        Crossfade(
            targetState = state.isSplashVisible,
            animationSpec = tween(
                durationMillis = motion.duration.deliberate,
                easing = motion.easing.emphasizedDecelerate,
            ),
        ) { splashVisible ->
            if (splashVisible) {
                SplashScreen(
                    onFinish = { onAction(AppViewModel.Action.SplashScreenAnimationFinished) },
                    modifier = modifier.fillMaxSize(),
                )
            } else {
                MainScreen(
                    modifier = modifier.fillMaxSize(),
                )
            }
        }
    }
}
