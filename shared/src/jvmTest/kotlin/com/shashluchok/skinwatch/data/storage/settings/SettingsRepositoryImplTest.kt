package com.shashluchok.skinwatch.data.storage.settings

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.shashluchok.skinwatch.data.storage.AppDatabase
import com.shashluchok.skinwatch.domain.steam.SteamCurrency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsRepositoryImplTest {
    private fun newRepository(): SettingsRepositoryImpl {
        val database = Room
            .inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        return SettingsRepositoryImpl(dao = database.settingsDao())
    }

    @Test
    fun `selectedCurrency starts as null when nothing was ever saved`() = runTest {
        val repository = newRepository()

        assertEquals(null, repository.selectedCurrency.first())
    }

    @Test
    fun `setSelectedCurrency then selectedCurrency emits the saved value`() = runTest {
        val repository = newRepository()

        repository.setSelectedCurrency(SteamCurrency.RUB)

        assertEquals(SteamCurrency.RUB, repository.selectedCurrency.first())
    }

    @Test
    fun `setSelectedCurrency with null resets back to no override`() = runTest {
        val repository = newRepository()
        repository.setSelectedCurrency(SteamCurrency.EUR)

        repository.setSelectedCurrency(null)

        assertEquals(null, repository.selectedCurrency.first())
    }
}
