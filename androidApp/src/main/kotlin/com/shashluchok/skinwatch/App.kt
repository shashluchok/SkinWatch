package com.shashluchok.skinwatch

import android.app.Application
import com.shashluchok.skinwatch.di.AndroidAppModule
import com.shashluchok.skinwatch.di.domain.AppConfigurationProviderImpl

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidAppModule.init(
            context = this@App,
            appConfigurationProvider = AppConfigurationProviderImpl(),
        )
    }
}
