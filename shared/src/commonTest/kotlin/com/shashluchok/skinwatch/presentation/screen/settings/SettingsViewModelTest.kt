package com.shashluchok.skinwatch.presentation.screen.settings

import com.shashluchok.skinwatch.domain.steam.SteamCurrency
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
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
    fun `initial state exposes the resolved default currency and no override`() = runTest(dispatcher) {
        val fixture = SettingsViewModelFixture(initialSelectedCurrency = null, defaultCurrency = SteamCurrency.GBP)
        val viewModel = fixture.newViewModel()

        assertNull(viewModel.stateFlow.value.selectedCurrency)
        assertEquals(SteamCurrency.GBP, viewModel.stateFlow.value.resolvedAutoCurrency)
        assertFalse(viewModel.stateFlow.value.isCurrencyPickerVisible)
    }

    @Test
    fun `initial state reflects an existing saved override`() = runTest(dispatcher) {
        val fixture = SettingsViewModelFixture(initialSelectedCurrency = SteamCurrency.EUR)
        val viewModel = fixture.newViewModel()

        dispatcher.scheduler.runCurrent()

        assertEquals(SteamCurrency.EUR, viewModel.stateFlow.value.selectedCurrency)
    }

    @Test
    fun `selectedCurrency tracks changes made outside this screen`() = runTest(dispatcher) {
        val fixture = SettingsViewModelFixture(initialSelectedCurrency = null)
        val viewModel = fixture.newViewModel()

        fixture.settingsRepository.setSelectedCurrency(SteamCurrency.RUB)
        dispatcher.scheduler.runCurrent()

        assertEquals(SteamCurrency.RUB, viewModel.stateFlow.value.selectedCurrency)
    }

    @Test
    fun `OnCurrencyRowClick opens the picker`() = runTest(dispatcher) {
        val viewModel = SettingsViewModelFixture().newViewModel()

        viewModel.onAction(SettingsViewModel.Action.OnCurrencyRowClick)

        assertTrue(viewModel.stateFlow.value.isCurrencyPickerVisible)
    }

    @Test
    fun `OnDismissCurrencyPicker closes the picker without changing the override`() = runTest(dispatcher) {
        val fixture = SettingsViewModelFixture(initialSelectedCurrency = SteamCurrency.USD)
        val viewModel = fixture.newViewModel()
        viewModel.onAction(SettingsViewModel.Action.OnCurrencyRowClick)

        viewModel.onAction(SettingsViewModel.Action.OnDismissCurrencyPicker)

        assertFalse(viewModel.stateFlow.value.isCurrencyPickerVisible)
        assertEquals(SteamCurrency.USD, fixture.settingsRepository.selectedCurrency.first())
    }

    @Test
    fun `OnCurrencyOptionSelected saves the picked currency and closes the picker`() = runTest(dispatcher) {
        val fixture = SettingsViewModelFixture(initialSelectedCurrency = null)
        val viewModel = fixture.newViewModel()
        viewModel.onAction(SettingsViewModel.Action.OnCurrencyRowClick)

        viewModel.onAction(SettingsViewModel.Action.OnCurrencyOptionSelected(SteamCurrency.EUR))
        dispatcher.scheduler.runCurrent()

        assertEquals(SteamCurrency.EUR, fixture.settingsRepository.selectedCurrency.first())
        assertFalse(viewModel.stateFlow.value.isCurrencyPickerVisible)
    }

    @Test
    fun `OnCurrencyOptionSelected with null clears the override back to auto`() = runTest(dispatcher) {
        val fixture = SettingsViewModelFixture(initialSelectedCurrency = SteamCurrency.RUB)
        val viewModel = fixture.newViewModel()
        viewModel.onAction(SettingsViewModel.Action.OnCurrencyRowClick)

        viewModel.onAction(SettingsViewModel.Action.OnCurrencyOptionSelected(null))
        dispatcher.scheduler.runCurrent()

        assertNull(fixture.settingsRepository.selectedCurrency.first())
    }
}
