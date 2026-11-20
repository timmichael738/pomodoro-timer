package com.kotlinapps.pomodoro.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.kotlinapps.pomodoro.feature.settings.navigation.navigateToSettings
import com.kotlinapps.pomodoro.feature.settings.navigation.settingsScreen
import com.kotlinapps.pomodoro.feature.stats.navigation.navigateToStats
import com.kotlinapps.pomodoro.feature.stats.navigation.statsScreen
import com.kotlinapps.pomodoro.feature.timer.navigation.TimerDestination
import com.kotlinapps.pomodoro.feature.timer.navigation.timerScreen

@Composable
fun PomodoroNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = TimerDestination,
        modifier = modifier,
    ) {
        timerScreen(
            onOpenStats = navController::navigateToStats,
            onOpenSettings = navController::navigateToSettings,
        )
        statsScreen(onNavigateUp = navController::navigateUp)
        settingsScreen(onNavigateUp = navController::navigateUp)
    }
}
