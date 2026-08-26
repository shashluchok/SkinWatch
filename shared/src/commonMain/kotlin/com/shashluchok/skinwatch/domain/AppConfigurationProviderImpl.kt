package com.shashluchok.skinwatch.domain

internal class AppConfigurationProviderImpl(
    isDebug: Boolean,
) : AppConfigurationProvider {
    override val configuration = AppConfigurationProvider.AppConfiguration(isDebug = isDebug)
}
