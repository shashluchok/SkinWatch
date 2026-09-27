package com.shashluchok.skinwatch.domain.pricesync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.hours

private const val HASH_NAME = "AK-47 | Redline (Field-Tested)"

class PriceFetchProgressTest {
    @Test
    fun `reports an item while its fetch is running`() = runTest {
        val progress = PriceFetchProgress()

        val fetch = launch { progress.track(marketHashName = HASH_NAME) { delay(1.hours) } }
        testScheduler.runCurrent()

        assertEquals(expected = setOf(HASH_NAME), actual = progress.inFlight.value)
        fetch.cancel()
    }

    @Test
    fun `stops reporting an item once its fetch returns`() = runTest {
        val progress = PriceFetchProgress()

        progress.track(marketHashName = HASH_NAME) { delay(1.hours) }

        assertEquals(expected = emptySet(), actual = progress.inFlight.value)
    }

    @Test
    fun `hands back what the fetch returned`() = runTest {
        val progress = PriceFetchProgress()

        val fetched = progress.track(marketHashName = HASH_NAME) { "price" }

        assertEquals(expected = "price", actual = fetched)
    }

    /** A left-behind flag would leave the card shimmering for as long as the screen is open. */
    @Test
    fun `stops reporting an item whose fetch threw`() = runTest {
        val progress = PriceFetchProgress()

        assertFailsWith<IllegalStateException> {
            progress.track(marketHashName = HASH_NAME) { error("no network") }
        }

        assertEquals(expected = emptySet(), actual = progress.inFlight.value)
    }

    @Test
    fun `stops reporting an item whose fetch was cancelled`() = runTest {
        val progress = PriceFetchProgress()

        val fetch = launch { progress.track(marketHashName = HASH_NAME) { delay(1.hours) } }
        testScheduler.runCurrent()
        fetch.cancel(CancellationException("screen left"))
        fetch.join()

        assertEquals(expected = emptySet(), actual = progress.inFlight.value)
    }

    /** An item added by hand fetches its first price while a scheduled run is already walking. */
    @Test
    fun `reports two fetches running at once`() = runTest {
        val progress = PriceFetchProgress()
        val otherHashName = "AWP | Asiimov (Field-Tested)"

        val run = launch { progress.track(marketHashName = HASH_NAME) { delay(1.hours) } }
        val add = launch { progress.track(marketHashName = otherHashName) { delay(1.hours) } }
        testScheduler.runCurrent()

        assertEquals(expected = setOf(HASH_NAME, otherHashName), actual = progress.inFlight.value)
        run.cancel()
        add.cancel()
    }
}
