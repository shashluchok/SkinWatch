package com.shashluchok.skinwatch

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.shashluchok.skinwatch.di.DesktopModule
import com.shashluchok.skinwatch.domain.AppConfigurationProvider
import com.shashluchok.skinwatch.presentation.navigation.AppContent

fun main() {
    DesktopModule.init(
        appConfigurationProvider = AppConfigurationProvider.EMPTY,
    )
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "SkinWatch",
        ) {
            AppContent()
        }
    }
}
