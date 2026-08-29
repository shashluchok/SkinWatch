package com.shashluchok.skinwatch.data.storage.debug

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.shashluchok.skinwatch.data.storage.AppDatabase
import com.shashluchok.skinwatch.domain.debug.DebugSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugSettingsRepositoryImplTest {
    // stateIn(started = Eagerly) collects forever by design -- backgroundScope (not the TestScope
    // itself) is what runTest provides for exactly this: coroutines launched there are cancelled
    // when the test ends instead of being required to complete.
    private fun newRepository(scope: CoroutineScope): DebugSettingsRepositoryImpl {
        val database = Room
            .inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        return DebugSettingsRepositoryImpl(dao = database.debugSettingsDao(), scope = scope)
    }

    @Test
    fun `settings default to showSplashScreen true and mockDataEnabled false when nothing was ever saved`() =
        runTest {
            val repository = newRepository(scope = backgroundScope)

            val settings = repository.settings.first()

            assertTrue(settings.showSplashScreen)
            assertFalse(settings.mockDataEnabled)
        }

    @Test
    fun `update then settings emits the saved values`() = runTest {
        val repository = newRepository(scope = backgroundScope)

        repository.update(
            DebugSettingsRepository.DebugSettings(showSplashScreen = false, mockDataEnabled = true),
        )

        val settings = repository.settings.first { it.mockDataEnabled }
        assertFalse(settings.showSplashScreen)
        assertTrue(settings.mockDataEnabled)
    }
}
