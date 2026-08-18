package com.shashluchok.skinwatch

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.shashluchok.skinwatch.di.DesktopModule
import com.shashluchok.skinwatch.presentation.navigation.AppContent

fun main() {
    DesktopModule.init()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "SkinWatch",
        ) {
            AppContent()
        }
    }
}
