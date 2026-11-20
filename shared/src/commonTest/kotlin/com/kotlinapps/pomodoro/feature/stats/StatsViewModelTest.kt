package com.kotlinapps.pomodoro.feature.stats

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.kotlinapps.pomodoro.core.model.FocusSession
import com.kotlinapps.pomodoro.core.model.FocusTotals
import com.kotlinapps.pomodoro.testing.FakeFocusSessionRepository
import com.kotlinapps.pomodoro.testing.MainDispatcherTest
import com.kotlinapps.pomodoro.testing.TestTimeProvider
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class StatsViewModelTest : MainDispatcherTest() {

    private suspend fun ReceiveTurbine<StatsUiState>.awaitLoaded(): StatsUiState {
        val item = awaitItem()
        return if (item is StatsUiState.Loading) awaitItem() else item
    }

    private fun TestScope.viewModel(repository: FakeFocusSessionRepository) =
        StatsViewModel(repository, TestTimeProvider(testScheduler))

    @Test
    fun noSessions_isEmpty() = runTest {
        viewModel(FakeFocusSessionRepository()).uiState.test {
            assertEquals(StatsUiState.Empty, awaitLoaded())
        }
    }

    @Test
    fun sessions_areSummarised() = runTest {
        val now = TestTimeProvider(testScheduler).now() // 2026-11-20T09:00Z
        val repository = FakeFocusSessionRepository(
            listOf(
                FocusSession(now - 1.hours, 25),
                FocusSession(now - 2.days, 50),
                FocusSession(now - 30.days, 25), // counts all-time only
            ),
        )
        viewModel(repository).uiState.test {
            val state = assertIs<StatsUiState.Success>(awaitLoaded())
            assertEquals(7, state.days.size)
            assertEquals(25, state.today.minutes)
            assertEquals(FocusTotals(sessions = 2, minutes = 75), state.week)
            assertEquals(FocusTotals(sessions = 3, minutes = 100), state.allTime)
        }
    }

    @Test
    fun clearHistory_returnsToEmpty() = runTest {
        val now = TestTimeProvider(testScheduler).now()
        val repository = FakeFocusSessionRepository(listOf(FocusSession(now, 25)))
        val viewModel = viewModel(repository)
        viewModel.uiState.test {
            assertIs<StatsUiState.Success>(awaitLoaded())
            viewModel.onClearHistory()
            assertEquals(StatsUiState.Empty, awaitItem())
        }
    }

    @Test
    fun repositoryFailure_showsError() = runTest {
        val repository = FakeFocusSessionRepository().apply { failWith = IllegalStateException("db") }
        viewModel(repository).uiState.test {
            assertEquals(StatsUiState.Error, awaitLoaded())
        }
    }
}
