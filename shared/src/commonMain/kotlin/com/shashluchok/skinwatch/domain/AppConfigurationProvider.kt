package com.shashluchok.skinwatch.domain

interface AppConfigurationProvider {
    data class AppConfiguration(
        val isDebug: Boolean,
    )

    val configuration: AppConfiguration

    companion object {
        val EMPTY = object : AppConfigurationProvider {
            override val configuration: AppConfiguration = AppConfiguration(isDebug = false)
        }
    }
}
