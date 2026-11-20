package com.kotlinapps.pomodoro.core.data.repository

import com.kotlinapps.pomodoro.core.model.ThemeMode
import com.kotlinapps.pomodoro.core.model.TimerSettings
import com.kotlinapps.pomodoro.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val userPreferences: Flow<UserPreferences>

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setUseDynamicColor(useDynamicColor: Boolean)

    suspend fun updateTimerSettings(transform: (TimerSettings) -> TimerSettings)
}
