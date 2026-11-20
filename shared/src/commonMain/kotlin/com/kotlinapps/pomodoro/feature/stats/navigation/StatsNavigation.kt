package com.kotlinapps.pomodoro.feature.stats.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kotlinapps.pomodoro.feature.stats.StatsRoute
import kotlinx.serialization.Serializable

@Serializable
data object StatsDestination

fun NavController.navigateToStats() = navigate(StatsDestination)

fun NavGraphBuilder.statsScreen(onNavigateUp: () -> Unit) {
    composable<StatsDestination> {
        StatsRoute(onNavigateUp = onNavigateUp)
    }
}
