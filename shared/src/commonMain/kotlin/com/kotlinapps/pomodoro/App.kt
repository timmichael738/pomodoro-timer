package com.kotlinapps.pomodoro

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlinapps.pomodoro.core.designsystem.theme.PomodoroTheme
import com.kotlinapps.pomodoro.core.model.ThemeMode
import com.kotlinapps.pomodoro.navigation.PomodoroNavHost
import org.koin.compose.viewmodel.koinViewModel

/** Root composable shared by Android and iOS. Applies the user's saved theme preferences. */
@Composable
fun App(viewModel: AppViewModel = koinViewModel()) {
    val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    // Wait for stored preferences before drawing, so the wrong theme never flashes.
    val prefs = preferences ?: return

    val darkTheme = when (prefs.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    PomodoroTheme(darkTheme = darkTheme, dynamicColor = prefs.useDynamicColor) {
        PomodoroNavHost()
    }
}
