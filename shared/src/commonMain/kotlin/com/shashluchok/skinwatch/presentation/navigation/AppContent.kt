package com.shashluchok.skinwatch.presentation.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.shashluchok.skinwatch.data.image.ImageHttpClientFactory
import com.shashluchok.skinwatch.di.appModules
import com.shashluchok.skinwatch.presentation.screen.main.MainScreen
import com.shashluchok.skinwatch.presentation.screen.splash.SplashScreen
import com.shashluchok.skinwatch.presentation.theme.AppTheme
import com.shashluchok.skinwatch.presentation.theme.LocalMotion
import org.koin.compose.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinConfiguration

// KtorNetworkFetcherFactory(httpClient = ...) resolves to an overload whose
// concurrentRequestStrategy parameter defaults to the @ExperimentalCoilApi
// ConcurrentRequestStrategy.UNCOORDINATED -- this call doesn't opt into any experimental
// concurrency behavior itself, it just has to acknowledge that default exists.
@OptIn(ExperimentalCoilApi::class)
@Composable
fun AppContent(
    modifier: Modifier = Modifier,
    platformModule: Module? = null,
) {
    KoinApplication(
        configuration = koinConfiguration {
            modules(
                listOfNotNull(
                    platformModule,
                ) + appModules,
            )
        },
    ) {
        setSingletonImageLoaderFactory { context ->
            ImageLoader
                .Builder(context)
                .components { add(KtorNetworkFetcherFactory(httpClient = ImageHttpClientFactory.create())) }
                .crossfade(true)
                .build()
        }
        AppTheme {
            val motion = LocalMotion.current
            // Splash is intentionally not a NavKey/backstack entry: it must never be reachable
            // via back navigation once the app has moved on to MainScreen.
            var isSplashDone by rememberSaveable { mutableStateOf(false) }

            Crossfade(
                targetState = isSplashDone,
                animationSpec = tween(
                    durationMillis = motion.duration.deliberate,
                    easing = motion.easing.emphasizedDecelerate,
                ),
            ) { done ->
                if (!done) {
                    SplashScreen(
                        onFinish = { isSplashDone = true },
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
}
