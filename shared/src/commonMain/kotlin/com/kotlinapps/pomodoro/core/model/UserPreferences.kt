package com.kotlinapps.pomodoro.core.model

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val timerSettings: TimerSettings = TimerSettings(),
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }
