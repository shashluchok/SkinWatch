package com.shashluchok.skinwatch.data.storage.debug

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.shashluchok.skinwatch.data.storage.AppDatabase
import com.shashluchok.skinwatch.domain.debug.DebugSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugSettingsRepositoryImplTest {
    private fun newRepository(): DebugSettingsRepositoryImpl {
        val database = Room
            .inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        return DebugSettingsRepositoryImpl(dao = database.debugSettingsDao())
    }

    @Test
    fun `settings default to showSplashScreen true and mockDataEnabled false when nothing was ever saved`() =
        runTest {
            val repository = newRepository()

            val settings = repository.settings.first()

            assertTrue(settings.showSplashScreen)
            assertFalse(settings.mockDataEnabled)
        }

    @Test
    fun `update then settings emits the saved values`() = runTest {
        val repository = newRepository()

        repository.update(
            DebugSettingsRepository.DebugSettings(showSplashScreen = false, mockDataEnabled = true),
        )

        val settings = repository.settings.first()
        assertFalse(settings.showSplashScreen)
        assertTrue(settings.mockDataEnabled)
    }
}
