package com.shashluchok.skinwatch

import android.app.Application
import com.shashluchok.skinwatch.di.AndroidModule

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidModule.init(this@App)
    }
}
