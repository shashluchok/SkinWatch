package com.shashluchok.skinwatch

import com.shashluchok.skinwatch.di.initKoin
import com.shashluchok.skinwatch.domain.AppConfigurationProvider
import org.koin.core.module.Module
import org.koin.dsl.module

open class AppModule {
    internal open fun start(platformModule: Module, appConfigurationProvider: AppConfigurationProvider) {
        val appModule = module {
            single<AppConfigurationProvider> { appConfigurationProvider }
        }
        initKoin(platformModule = platformModule, appModule = appModule)
    }
}
