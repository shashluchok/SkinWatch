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
import com.shashluchok.skinwatch.di.appModules
import com.shashluchok.skinwatch.presentation.screen.main.MainScreen
import com.shashluchok.skinwatch.presentation.screen.splash.SplashScreen
import com.shashluchok.skinwatch.presentation.theme.AppTheme
import com.shashluchok.skinwatch.presentation.theme.LocalMotion
import org.koin.compose.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinConfiguration

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
