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
    fun `every category is fetched and its chunks inserted on full success, run marked completed`() = runTest {
        remoteSource.chunksByCategory[CatalogCategory.SKIN] =
            listOf(listOf(catalogItem("AK-47 | Redline", CatalogCategory.SKIN)))

        newInteractor().invoke()

        assertEquals(CatalogCategory.entries.size, remoteSource.fetchCalls.size)
        assertTrue(catalogRepository.clearCategoryCalls.contains(CatalogCategory.SKIN))
        assertTrue(catalogRepository.insertItemsCalls.any { it.first == CatalogCategory.SKIN })
        assertEquals(1, catalogSyncStatusRepository.markCompletedCalls.size)
    }

    @Test
    fun `chunks are inserted as they arrive, in order`() = runTest {
        val firstChunk = listOf(catalogItem("AK-47 | Redline", CatalogCategory.SKIN))
        val secondChunk = listOf(catalogItem("AWP | Asiimov", CatalogCategory.SKIN))
        remoteSource.chunksByCategory[CatalogCategory.SKIN] = listOf(firstChunk, secondChunk)

        newInteractor().invoke()

        val skinInserts = catalogRepository.insertItemsCalls.filter { it.first == CatalogCategory.SKIN }
        assertEquals(listOf(firstChunk, secondChunk), skinInserts.map { it.second })
    }

    @Test
    fun `a category whose fetch fails with zero chunks leaves its previously cached rows untouched`() = runTest {
        remoteSource.resultsByCategory[CatalogCategory.SKIN] = CatalogFetchResult.Failure(CatalogFetchError.Network)
        remoteSource.chunksByCategory[CatalogCategory.STICKER] =
            listOf(listOf(catalogItem("Sticker | Shooter", CatalogCategory.STICKER)))

        newInteractor().invoke()

        assertTrue(catalogRepository.clearCategoryCalls.none { it == CatalogCategory.SKIN })
        assertTrue(catalogRepository.insertItemsCalls.none { it.first == CatalogCategory.SKIN })
        assertTrue(catalogRepository.insertItemsCalls.any { it.first == CatalogCategory.STICKER })
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
