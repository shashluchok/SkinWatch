package com.shashluchok.skinwatch

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.shashluchok.skinwatch.di.webModule
import com.shashluchok.skinwatch.presentation.navigation.AppContent

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        AppContent(platformModule = webModule)
    }
}
