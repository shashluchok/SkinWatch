package com.shashluchok.skinwatch.domain.exchangerate

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class HasConvertiblePricesInteractorTest {
    @Test
    fun `returns the repository's hasConvertibleData as-is`() = runTest {
        val repository = FakeCurrencyConversionRepository(hasData = true)
        val interactor = HasConvertiblePricesInteractor(currencyConversionRepository = repository)

        assertTrue(interactor())
    }
}
