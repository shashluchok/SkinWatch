package com.shashluchok.skinwatch.presentation.screen.splash

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun stateIsNotReadyImmediatelyAfterCreation() =
        runTest(dispatcher) {
            val viewModel = SplashViewModel()

            assertFalse(viewModel.stateFlow.value.isReady)
        }

    @Test
    fun stateBecomesReadyAfterSplashDurationElapses() =
        runTest(dispatcher) {
            val viewModel = SplashViewModel()

            dispatcher.scheduler.advanceTimeBy(SplashViewModel.SPLASH_DURATION)
            dispatcher.scheduler.runCurrent()

            assertTrue(viewModel.stateFlow.value.isReady)
        }
}
