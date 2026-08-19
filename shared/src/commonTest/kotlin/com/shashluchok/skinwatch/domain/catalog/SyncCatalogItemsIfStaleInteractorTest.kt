package com.shashluchok.skinwatch.domain.catalog

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class SyncCatalogItemsIfStaleInteractorTest {
    private val catalogSyncStatusRepository = FakeCatalogSyncStatusRepository()
    private val syncCatalogItems = SyncCatalogItemsInteractor(
        remoteSource = FakeItemCatalogRemoteSource(),
        catalogRepository = FakeItemCatalogRepository(),
        catalogSyncStatusRepository = catalogSyncStatusRepository,
    )
    private val interactor = SyncCatalogItemsIfStaleInteractor(
        catalogSyncStatusRepository = catalogSyncStatusRepository,
        syncCatalogItems = syncCatalogItems,
    )

    @Test
    fun `syncs when nothing has ever been synced`() = runTest {
        interactor()

        assertEquals(1, catalogSyncStatusRepository.markCompletedCalls.size)
    }

    @Test
    fun `does not sync when the last sync is younger than the interval`() = runTest {
        catalogSyncStatusRepository.markCompleted(Clock.System.now())

        interactor()

        assertEquals(1, catalogSyncStatusRepository.markCompletedCalls.size) // only the setup call above
    }

    @Test
    fun `syncs when the last sync is at least as old as the interval`() = runTest {
        catalogSyncStatusRepository.markCompleted(Clock.System.now() - CATALOG_SYNC_INTERVAL)

        interactor()

        assertEquals(2, catalogSyncStatusRepository.markCompletedCalls.size) // setup call + this one
    }
}
