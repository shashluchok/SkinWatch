package com.shashluchok.skinwatch.presentation.screen.settings.component

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class DebugPanelViewModelTest {
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
    fun `initial state reflects the persisted settings`() = runTest(dispatcher) {
        val fixture = DebugPanelViewModelFixture(initialShowSplashScreen = false, initialMockDataEnabled = true)
        val viewModel = fixture.newViewModel()

        dispatcher.scheduler.runCurrent()

        assertFalse(viewModel.stateFlow.value.showSplashScreen)
        assertTrue(viewModel.stateFlow.value.mockDataEnabled)
    }

    @Test
    fun `OnShowSplashScreenToggled persists the new value`() = runTest(dispatcher) {
        val fixture = DebugPanelViewModelFixture(initialShowSplashScreen = true)
        val viewModel = fixture.newViewModel()

        viewModel.onAction(DebugPanelViewModel.Action.OnShowSplashScreenToggled(false))
        dispatcher.scheduler.runCurrent()

        assertFalse(
            fixture.debugSettingsRepository.settings
                .first()
                .showSplashScreen,
        )
        assertFalse(viewModel.stateFlow.value.showSplashScreen)
    }

    @Test
    fun `OnMockDataEnabledToggled persists the new value`() = runTest(dispatcher) {
        val fixture = DebugPanelViewModelFixture(initialMockDataEnabled = false)
        val viewModel = fixture.newViewModel()

        viewModel.onAction(DebugPanelViewModel.Action.OnMockDataEnabledToggled(true))
        dispatcher.scheduler.runCurrent()

        assertTrue(
            fixture.debugSettingsRepository.settings
                .first()
                .mockDataEnabled,
        )
        assertTrue(viewModel.stateFlow.value.mockDataEnabled)
    }
}
