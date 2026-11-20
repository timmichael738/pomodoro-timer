package com.kotlinapps.pomodoro.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kotlinapps.pomodoro.core.model.ThemeMode
import com.kotlinapps.pomodoro.core.model.TimerSettings
import com.kotlinapps.pomodoro.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okio.IOException

internal class DataStoreUserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val FOCUS_MINUTES = intPreferencesKey("focus_minutes")
        val SHORT_BREAK_MINUTES = intPreferencesKey("short_break_minutes")
        val LONG_BREAK_MINUTES = intPreferencesKey("long_break_minutes")
        val SESSIONS_UNTIL_LONG_BREAK = intPreferencesKey("sessions_until_long_break")
        val AUTO_START_BREAKS = booleanPreferencesKey("auto_start_breaks")
        val AUTO_START_FOCUS = booleanPreferencesKey("auto_start_focus")
    }

    override val userPreferences: Flow<UserPreferences> = dataStore.data
        // A corrupted/unreadable file falls back to defaults instead of crashing the app.
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            val defaults = UserPreferences()
            UserPreferences(
                themeMode = prefs[Keys.THEME_MODE]
                    ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                    ?: defaults.themeMode,
                useDynamicColor = prefs[Keys.USE_DYNAMIC_COLOR] ?: defaults.useDynamicColor,
                timerSettings = prefs.readTimerSettings(),
            )
        }

    private fun Preferences.readTimerSettings(): TimerSettings {
        val d = TimerSettings()
        // Values are clamped so a hand-edited or outdated file can never produce a zero-length phase.
        return TimerSettings(
            focusMinutes = (this[Keys.FOCUS_MINUTES] ?: d.focusMinutes).coerceIn(TimerSettings.FocusMinutesRange),
            shortBreakMinutes = (this[Keys.SHORT_BREAK_MINUTES] ?: d.shortBreakMinutes)
                .coerceIn(TimerSettings.BreakMinutesRange),
            longBreakMinutes = (this[Keys.LONG_BREAK_MINUTES] ?: d.longBreakMinutes)
                .coerceIn(TimerSettings.BreakMinutesRange),
            sessionsUntilLongBreak = (this[Keys.SESSIONS_UNTIL_LONG_BREAK] ?: d.sessionsUntilLongBreak)
                .coerceIn(TimerSettings.SessionsRange),
            autoStartBreaks = this[Keys.AUTO_START_BREAKS] ?: d.autoStartBreaks,
            autoStartFocus = this[Keys.AUTO_START_FOCUS] ?: d.autoStartFocus,
        )
    }

    private fun MutablePreferences.writeTimerSettings(s: TimerSettings) {
        this[Keys.FOCUS_MINUTES] = s.focusMinutes
        this[Keys.SHORT_BREAK_MINUTES] = s.shortBreakMinutes
        this[Keys.LONG_BREAK_MINUTES] = s.longBreakMinutes
        this[Keys.SESSIONS_UNTIL_LONG_BREAK] = s.sessionsUntilLongBreak
        this[Keys.AUTO_START_BREAKS] = s.autoStartBreaks
        this[Keys.AUTO_START_FOCUS] = s.autoStartFocus
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = themeMode.name }
    }

    override suspend fun setUseDynamicColor(useDynamicColor: Boolean) {
        dataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = useDynamicColor }
    }

    override suspend fun updateTimerSettings(transform: (TimerSettings) -> TimerSettings) {
        // Read-modify-write inside one edit transaction, so rapid taps never lose an update.
        dataStore.edit { prefs -> prefs.writeTimerSettings(transform(prefs.readTimerSettings())) }
    }
}
