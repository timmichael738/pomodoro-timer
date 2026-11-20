package com.kotlinapps.pomodoro.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlinapps.pomodoro.core.data.repository.UserPreferencesRepository
import com.kotlinapps.pomodoro.core.model.ThemeMode
import com.kotlinapps.pomodoro.core.model.TimerSettings
import com.kotlinapps.pomodoro.core.model.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(val preferences: UserPreferences) : SettingsUiState
}

/** A stepper-adjustable timer setting, with its allowed range. */
enum class TimerLength(val range: IntRange) {
    FOCUS(TimerSettings.FocusMinutesRange),
    SHORT_BREAK(TimerSettings.BreakMinutesRange),
    LONG_BREAK(TimerSettings.BreakMinutesRange),
    SESSIONS(TimerSettings.SessionsRange),
    ;

    fun valueIn(settings: TimerSettings): Int = when (this) {
        FOCUS -> settings.focusMinutes
        SHORT_BREAK -> settings.shortBreakMinutes
        LONG_BREAK -> settings.longBreakMinutes
        SESSIONS -> settings.sessionsUntilLongBreak
    }

    fun applyTo(settings: TimerSettings, value: Int): TimerSettings {
        val v = value.coerceIn(range)
        return when (this) {
            FOCUS -> settings.copy(focusMinutes = v)
            SHORT_BREAK -> settings.copy(shortBreakMinutes = v)
            LONG_BREAK -> settings.copy(longBreakMinutes = v)
            SESSIONS -> settings.copy(sessionsUntilLongBreak = v)
        }
    }
}

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = userPreferencesRepository.userPreferences
        .map<UserPreferences, SettingsUiState> { SettingsUiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState.Loading)

    fun onThemeModeChange(themeMode: ThemeMode) {
        viewModelScope.launch { userPreferencesRepository.setThemeMode(themeMode) }
    }

    fun onDynamicColorChange(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setUseDynamicColor(enabled) }
    }

    /** Adds [delta] to [length], clamped to its range. Applied atomically in the repository. */
    fun onAdjust(length: TimerLength, delta: Int) {
        viewModelScope.launch {
            userPreferencesRepository.updateTimerSettings { length.applyTo(it, length.valueIn(it) + delta) }
        }
    }

    fun onAutoStartBreaksChange(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.updateTimerSettings { it.copy(autoStartBreaks = enabled) } }
    }

    fun onAutoStartFocusChange(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.updateTimerSettings { it.copy(autoStartFocus = enabled) } }
    }
}
