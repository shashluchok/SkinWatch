package com.shashluchok.skinwatch.presentation.screen.settings

import com.shashluchok.skinwatch.domain.AppConfigurationProvider
import com.shashluchok.skinwatch.domain.exchangerate.ExchangeRateError
import com.shashluchok.skinwatch.domain.exchangerate.ExchangeRateResult
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

    @Test
    fun `OnCurrencyOptionSelected applies immediately when there is nothing to convert`() = runTest(dispatcher) {
        val fixture = SettingsViewModelFixture(hasConvertibleData = false)
        val viewModel = fixture.newViewModel()
        viewModel.onAction(SettingsViewModel.Action.OnCurrencyRowClick)

        viewModel.onAction(SettingsViewModel.Action.OnCurrencyOptionSelected(SteamCurrency.EUR))
        dispatcher.scheduler.runCurrent()

        assertEquals(SteamCurrency.EUR, fixture.settingsRepository.selectedCurrency.first())
        assertFalse(viewModel.stateFlow.value.isCurrencyChangeDialogVisible)
    }

    @Test
    fun `OnCurrencyOptionSelected opens the confirmation dialog when there is data to convert`() = runTest(dispatcher) {
        val fixture = SettingsViewModelFixture(hasConvertibleData = true, initialSelectedCurrency = SteamCurrency.USD)
        val viewModel = fixture.newViewModel()
        viewModel.onAction(SettingsViewModel.Action.OnCurrencyRowClick)

        viewModel.onAction(SettingsViewModel.Action.OnCurrencyOptionSelected(SteamCurrency.EUR))
        dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.stateFlow.value.isCurrencyChangeDialogVisible)
        assertEquals(SteamCurrency.EUR, viewModel.stateFlow.value.pendingCurrency)
        // The override is not saved yet -- only the confirmation flips it.
        assertEquals(SteamCurrency.USD, fixture.settingsRepository.selectedCurrency.first())
    }

    @Test
    fun `OnCurrencyChangeConfirmed with an explicit currency converts to it and saves it as the override`() =
        runTest(dispatcher) {
            val rates = mapOf(SteamCurrency.USD to 1.08)
            val fixture = SettingsViewModelFixture(
                hasConvertibleData = true,
                ratesResult = ExchangeRateResult.Success(rates),
            )
            val viewModel = fixture.newViewModel()
            viewModel.onAction(SettingsViewModel.Action.OnCurrencyRowClick)
            viewModel.onAction(SettingsViewModel.Action.OnCurrencyOptionSelected(SteamCurrency.EUR))
            dispatcher.scheduler.runCurrent()

            viewModel.onAction(SettingsViewModel.Action.OnCurrencyChangeConfirmed)
            dispatcher.scheduler.runCurrent()

            assertEquals(SteamCurrency.EUR, fixture.exchangeRateRepository.lastRequestedBase)
            val call = fixture.currencyConversionRepository.convertAllCalls.single()
            assertEquals(SteamCurrency.EUR, call.targetCurrency)
            assertEquals(SteamCurrency.EUR, call.newSelectedCurrency)
            assertFalse(viewModel.stateFlow.value.isCurrencyChangeDialogVisible)
            assertEquals(SettingsViewModel.ConversionStatus.Idle, viewModel.stateFlow.value.conversionStatus)
        }

    @Test
    fun `OnCurrencyChangeConfirmed with Auto resolves the target currency before converting`() = runTest(dispatcher) {
        val fixture = SettingsViewModelFixture(
            hasConvertibleData = true,
            defaultCurrency = SteamCurrency.GBP,
            ratesResult = ExchangeRateResult.Success(emptyMap()),
        )
        val viewModel = fixture.newViewModel()
        viewModel.onAction(SettingsViewModel.Action.OnCurrencyRowClick)
        viewModel.onAction(SettingsViewModel.Action.OnCurrencyOptionSelected(null))
        dispatcher.scheduler.runCurrent()

        viewModel.onAction(SettingsViewModel.Action.OnCurrencyChangeConfirmed)
        dispatcher.scheduler.runCurrent()

        assertEquals(SteamCurrency.GBP, fixture.exchangeRateRepository.lastRequestedBase)
        val call = fixture.currencyConversionRepository.convertAllCalls.single()
        assertEquals(SteamCurrency.GBP, call.targetCurrency)
        assertEquals(null, call.newSelectedCurrency)
    }

    @Test
    fun `OnCurrencyChangeConfirmed on failure keeps the dialog open with the error`() = runTest(dispatcher) {
        val fixture = SettingsViewModelFixture(
            hasConvertibleData = true,
            ratesResult = ExchangeRateResult.Failure(ExchangeRateError.Network),
        )
        val viewModel = fixture.newViewModel()
        viewModel.onAction(SettingsViewModel.Action.OnCurrencyRowClick)
        viewModel.onAction(SettingsViewModel.Action.OnCurrencyOptionSelected(SteamCurrency.EUR))
        dispatcher.scheduler.runCurrent()

        viewModel.onAction(SettingsViewModel.Action.OnCurrencyChangeConfirmed)
        dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.stateFlow.value.isCurrencyChangeDialogVisible)
        assertEquals(
            SettingsViewModel.ConversionStatus.Failed(ExchangeRateError.Network),
            viewModel.stateFlow.value.conversionStatus,
        )
        assertTrue(fixture.currencyConversionRepository.convertAllCalls.isEmpty())
    }

    @Test
    fun `OnCurrencyChangeCancelled closes the dialog without converting anything`() = runTest(dispatcher) {
        val fixture = SettingsViewModelFixture(hasConvertibleData = true)
        val viewModel = fixture.newViewModel()
        viewModel.onAction(SettingsViewModel.Action.OnCurrencyRowClick)
        viewModel.onAction(SettingsViewModel.Action.OnCurrencyOptionSelected(SteamCurrency.EUR))
        dispatcher.scheduler.runCurrent()

        viewModel.onAction(SettingsViewModel.Action.OnCurrencyChangeCancelled)

        assertFalse(viewModel.stateFlow.value.isCurrencyChangeDialogVisible)
        assertTrue(fixture.currencyConversionRepository.convertAllCalls.isEmpty())
        assertEquals(null, fixture.settingsRepository.selectedCurrency.first())
    }

    @Test
    fun `OnDebugRowClick opens the debug panel`() = runTest(dispatcher) {
        val viewModel = SettingsViewModelFixture().newViewModel()

        viewModel.onAction(SettingsViewModel.Action.OnDebugRowClick)

        assertTrue(viewModel.stateFlow.value.isDebugPanelVisible)
    }

    @Test
    fun `OnDismissDebugPanel closes the debug panel`() = runTest(dispatcher) {
        val viewModel = SettingsViewModelFixture().newViewModel()
        viewModel.onAction(SettingsViewModel.Action.OnDebugRowClick)

        viewModel.onAction(SettingsViewModel.Action.OnDismissDebugPanel)

        assertFalse(viewModel.stateFlow.value.isDebugPanelVisible)
    }

    @Test
    fun `initial state exposes isDebugPanelAvailable as true when the debug panel is available`() =
        runTest(dispatcher) {
            val available = object : AppConfigurationProvider {
                override val configuration = AppConfigurationProvider.AppConfiguration(isDebug = true)
            }
            val viewModel = SettingsViewModelFixture(appConfigurationProvider = available).newViewModel()

            assertTrue(viewModel.stateFlow.value.isDebugPanelAvailable)
        }

    @Test
    fun `initial state exposes isDebugPanelAvailable as false when the debug panel is unavailable`() =
        runTest(dispatcher) {
            val viewModel =
                SettingsViewModelFixture(appConfigurationProvider = AppConfigurationProvider.EMPTY).newViewModel()

            assertFalse(viewModel.stateFlow.value.isDebugPanelAvailable)
        }
}
