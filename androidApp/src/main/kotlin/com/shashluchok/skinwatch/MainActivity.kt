package com.shashluchok.skinwatch

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.shashluchok.skinwatch.presentation.navigation.AppContent
import com.shashluchok.skinwatch.presentation.screen.splash.SplashReelCache
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val isDarkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        splashScreen.setKeepOnScreenCondition { !SplashReelCache.isReady(isDarkTheme = isDarkTheme) }
        lifecycleScope.launch { SplashReelCache.preload(isDarkTheme = isDarkTheme) }

        setContent {
            AppContent()
        }
    }
}
