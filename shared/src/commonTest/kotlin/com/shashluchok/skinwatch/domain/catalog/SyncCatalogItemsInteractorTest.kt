package com.shashluchok.skinwatch.domain.catalog

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class SyncCatalogItemsInteractorTest {
    private val remoteSource = FakeItemCatalogRemoteSource()
    private val catalogRepository = FakeItemCatalogRepository()
    private val catalogSyncStatusRepository = FakeCatalogSyncStatusRepository()

    private fun newInteractor() = SyncCatalogItemsInteractor(
        remoteSource = remoteSource,
        catalogRepository = catalogRepository,
        catalogSyncStatusRepository = catalogSyncStatusRepository,
    )

    private fun catalogItem(name: String, category: CatalogCategory) = CatalogItem(
        marketHashName = name,
        displayName = name,
        iconUrl = "https://example.com/$name.png",
        category = category,
    )

    @Test
    fun `every category is fetched and replaced on full success, run marked completed`() = runTest {
        remoteSource.resultsByCategory[CatalogCategory.SKIN] =
            CatalogFetchResult.Success(listOf(catalogItem("AK-47 | Redline", CatalogCategory.SKIN)))

        newInteractor().invoke()

        assertEquals(CatalogCategory.entries.size, remoteSource.fetchCalls.size)
        assertTrue(catalogRepository.replaceCategoryCalls.any { it.first == CatalogCategory.SKIN })
        assertEquals(1, catalogSyncStatusRepository.markCompletedCalls.size)
    }

    @Test
    fun `a failed category is skipped, does not stop the rest, and is not replaced`() = runTest {
        remoteSource.resultsByCategory[CatalogCategory.SKIN] =
            CatalogFetchResult.Failure(CatalogFetchError.Network)
        remoteSource.resultsByCategory[CatalogCategory.STICKER] =
            CatalogFetchResult.Success(listOf(catalogItem("Sticker | Shooter", CatalogCategory.STICKER)))

        newInteractor().invoke()

        assertTrue(catalogRepository.replaceCategoryCalls.none { it.first == CatalogCategory.SKIN })
        assertTrue(catalogRepository.replaceCategoryCalls.any { it.first == CatalogCategory.STICKER })
        assertEquals(1, catalogSyncStatusRepository.markCompletedCalls.size)
    }

    @Test
    fun `every category failing does not mark the run completed`() = runTest {
        remoteSource.defaultResult = CatalogFetchResult.Failure(CatalogFetchError.Network)

        newInteractor().invoke()

        assertEquals(0, catalogSyncStatusRepository.markCompletedCalls.size)
    }

    @Test
    fun `a concurrent invoke while a run is in progress is a no-op`() = runTest {
        remoteSource.delayDuration = 1.hours
        val interactor = newInteractor()

        val firstRun = launch { interactor.invoke() }
        testScheduler.runCurrent() // let firstRun start and reach the delay, then pause there
        interactor.invoke() // runMutex.tryLock() fails -- returns immediately, no second pass
        testScheduler.advanceUntilIdle() // let firstRun's delay elapse and the run finish

        assertEquals(1, catalogSyncStatusRepository.markCompletedCalls.size)
        firstRun.join()
    }

    @Test
    fun `isSyncing is true while a run is suspended mid-flight, false once it completes`() = runTest {
        remoteSource.delayDuration = 1.hours
        val interactor = newInteractor()
        assertTrue(!interactor.isSyncing.value)

        val firstRun = launch { interactor.invoke() }
        testScheduler.runCurrent() // run reaches the delay and pauses -- still "in flight" here
        assertTrue(interactor.isSyncing.value)

        testScheduler.advanceUntilIdle() // let the delay elapse and the run finish
        assertTrue(!interactor.isSyncing.value)
        firstRun.join()
    }
}
