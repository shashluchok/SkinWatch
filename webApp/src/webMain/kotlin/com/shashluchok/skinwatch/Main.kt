package com.shashluchok.skinwatch

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.shashluchok.skinwatch.di.WebModule
import com.shashluchok.skinwatch.presentation.navigation.AppContent

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    WebModule.init()
    ComposeViewport {
        AppContent()
    }
}
