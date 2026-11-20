package com.kotlinapps.pomodoro.feature.settings

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.kotlinapps.pomodoro.core.model.ThemeMode
import com.kotlinapps.pomodoro.core.model.TimerSettings
import com.kotlinapps.pomodoro.core.model.UserPreferences
import com.kotlinapps.pomodoro.testing.FakeUserPreferencesRepository
import com.kotlinapps.pomodoro.testing.MainDispatcherTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsViewModelTest : MainDispatcherTest() {

    private val repository = FakeUserPreferencesRepository()
    private val viewModel = SettingsViewModel(repository)

    /** Skips the initial Loading value, which may or may not be observed depending on timing. */
    private suspend fun ReceiveTurbine<SettingsUiState>.awaitPreferences(): UserPreferences {
        var item = awaitItem()
        if (item is SettingsUiState.Loading) item = awaitItem()
        return (item as SettingsUiState.Success).preferences
    }

    @Test
    fun uiState_initiallyLoading() {
        assertEquals(SettingsUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun uiState_emitsDefaultPreferences() = runTest {
        viewModel.uiState.test { assertEquals(UserPreferences(), awaitPreferences()) }
    }

    @Test
    fun themeChanges_areReflectedInState() = runTest {
        viewModel.uiState.test {
            awaitPreferences()
            viewModel.onThemeModeChange(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, awaitPreferences().themeMode)
            viewModel.onDynamicColorChange(false)
            assertEquals(false, awaitPreferences().useDynamicColor)
        }
    }

    @Test
    fun adjust_changesValue_andClampsToRange() = runTest {
        viewModel.uiState.test {
            awaitPreferences()
            viewModel.onAdjust(TimerLength.FOCUS, 5)
            assertEquals(30, awaitPreferences().timerSettings.focusMinutes)

            viewModel.onAdjust(TimerLength.SESSIONS, -100)
            assertEquals(TimerSettings.SessionsRange.first, awaitPreferences().timerSettings.sessionsUntilLongBreak)

            viewModel.onAdjust(TimerLength.LONG_BREAK, 1000)
            assertEquals(TimerSettings.BreakMinutesRange.last, awaitPreferences().timerSettings.longBreakMinutes)
        }
    }

    @Test
    fun autoStartToggles_areSaved() = runTest {
        viewModel.uiState.test {
            awaitPreferences()
            viewModel.onAutoStartBreaksChange(true)
            assertEquals(true, awaitPreferences().timerSettings.autoStartBreaks)
            viewModel.onAutoStartFocusChange(true)
            assertEquals(true, awaitPreferences().timerSettings.autoStartFocus)
        }
    }

    @Test
    fun stepFor_usesFiveMinuteStepsAboveTen() {
        assertEquals(1, stepFor(TimerLength.FOCUS, 5, increasing = true))
        assertEquals(5, stepFor(TimerLength.FOCUS, 10, increasing = true))
        assertEquals(1, stepFor(TimerLength.FOCUS, 10, increasing = false))
        assertEquals(5, stepFor(TimerLength.FOCUS, 15, increasing = false))
        assertEquals(1, stepFor(TimerLength.SESSIONS, 6, increasing = true))
    }
}
