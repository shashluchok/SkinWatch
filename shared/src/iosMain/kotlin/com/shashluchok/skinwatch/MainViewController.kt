package com.shashluchok.skinwatch

import androidx.compose.ui.window.ComposeUIViewController
import com.shashluchok.skinwatch.di.iosModule
import com.shashluchok.skinwatch.presentation.navigation.AppContent

@Suppress("ktlint:standard:function-naming", "FunctionNaming")
fun MainViewController() = ComposeUIViewController { AppContent(platformModule = iosModule) }
