package com.shashluchok.skinwatch.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module

internal val appModules =
    listOf(
        dataModule,
        domainModule,
        viewModelModule,
    )

internal fun initKoin(platformModule: Module, appModule: Module) {
    startKoin { modules(appModules + platformModule + appModule) }
}
