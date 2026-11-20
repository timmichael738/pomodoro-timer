package com.kotlinapps.pomodoro.feature.timer.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kotlinapps.pomodoro.feature.timer.TimerRoute
import kotlinx.serialization.Serializable

@Serializable
data object TimerDestination

fun NavGraphBuilder.timerScreen(onOpenStats: () -> Unit, onOpenSettings: () -> Unit) {
    composable<TimerDestination> {
        TimerRoute(onOpenStats = onOpenStats, onOpenSettings = onOpenSettings)
    }
}
