package com.shashluchok.skinwatch

import androidx.compose.ui.window.ComposeUIViewController
import com.shashluchok.skinwatch.di.IosModule
import com.shashluchok.skinwatch.presentation.navigation.AppContent
import platform.UIKit.UIViewController

@Suppress("ktlint:standard:function-naming", "FunctionNaming")
fun MainViewController(): UIViewController {
    IosModule.init()
    return ComposeUIViewController { AppContent() }
}
