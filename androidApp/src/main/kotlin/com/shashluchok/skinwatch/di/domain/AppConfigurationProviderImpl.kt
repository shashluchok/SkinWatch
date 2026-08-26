package com.shashluchok.skinwatch.di.domain

import com.shashluchok.skinwatch.BuildConfig
import com.shashluchok.skinwatch.domain.AppConfigurationProvider

class AppConfigurationProviderImpl : AppConfigurationProvider {
    override val configuration =
        AppConfigurationProvider.AppConfiguration(isDebug = BuildConfig.DEBUG)
}
