package com.shashluchok.skinwatch.di

import com.shashluchok.skinwatch.AppModule
import com.shashluchok.skinwatch.domain.AppConfigurationProvider
import com.shashluchok.skinwatch.domain.inventory.InventoryRepository
import com.shashluchok.skinwatch.domain.pricesnapshot.PriceSnapshotRepository
import org.koin.dsl.module

object WebModule : AppModule() {
    private val platformModule = module {
        single<InventoryRepository> { InventoryRepository.EMPTY }
        single<PriceSnapshotRepository> { PriceSnapshotRepository.EMPTY }
    }

    fun init(appConfigurationProvider: AppConfigurationProvider) = start(
        platformModule = platformModule,
        appConfigurationProvider = appConfigurationProvider,
    )
}
