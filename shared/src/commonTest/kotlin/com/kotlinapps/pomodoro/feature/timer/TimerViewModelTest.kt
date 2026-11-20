package com.kotlinapps.pomodoro.feature.timer

import app.cash.turbine.test
import com.kotlinapps.pomodoro.core.model.FocusSession
import com.kotlinapps.pomodoro.core.model.TimerPhase
import com.kotlinapps.pomodoro.core.model.TimerSettings
import com.kotlinapps.pomodoro.core.notification.NotificationContent
import com.kotlinapps.pomodoro.core.timer.PomodoroTimer
import com.kotlinapps.pomodoro.core.timer.TimerState
import com.kotlinapps.pomodoro.core.timer.TimerStatus
import com.kotlinapps.pomodoro.testing.FakeFocusSessionRepository
import com.kotlinapps.pomodoro.testing.FakePhaseNotifier
import com.kotlinapps.pomodoro.testing.FakeTimerStateStore
import com.kotlinapps.pomodoro.testing.FakeUserPreferencesRepository
import com.kotlinapps.pomodoro.testing.MainDispatcherTest
import com.kotlinapps.pomodoro.testing.TestTimeProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class TimerViewModelTest : MainDispatcherTest() {

    private val now = Instant.parse("2026-11-20T10:00:00Z")

    @Test
    fun idle_showsFullConfiguredDuration() {
        val ui = TimerState().toUiState(TimerSettings(focusMinutes = 30), now, 0, 0)
        assertEquals(30.minutes, ui.remaining)
        assertEquals(0f, ui.progress)
        assertFalse(ui.isStarted)
        assertEquals(1800, ui.remainingSeconds)
    }

    @Test
    fun running_computesRemainingFromEndTime() {
        val state = TimerState(status = TimerStatus.Running(now + 10.minutes, 25.minutes))
        val ui = state.toUiState(TimerSettings(), now, 2, 50)
        assertEquals(10.minutes, ui.remaining)
        assertEquals(0.6f, ui.progress, 0.0001f)
        assertTrue(ui.isRunning)
        assertEquals(2, ui.todaySessions)
    }

    @Test
    fun running_pastEndTime_isClampedToZero() {
        val state = TimerState(status = TimerStatus.Running(now - 5.seconds, 25.minutes))
        val ui = state.toUiState(TimerSettings(), now, 0, 0)
        assertEquals(0, ui.remainingSeconds)
        assertEquals(1f, ui.progress)
    }

    @Test
    fun remainingSeconds_roundsUp() {
        val state = TimerState(status = TimerStatus.Paused(remaining = 59.1.seconds, total = 1.minutes))
        assertEquals(60, state.toUiState(TimerSettings(), now, 0, 0).remainingSeconds)
    }

    @Test
    fun formatClock_padsAndHandlesHours() {
        assertEquals("25:00", formatClock(1500))
        assertEquals("00:09", formatClock(9))
        assertEquals("1:30:00", formatClock(5400))
        assertEquals("00:00", formatClock(-3))
    }

    @Test
    fun uiState_reflectsTimerAndTodaysSessions() = runTest {
        val time = TestTimeProvider(testScheduler)
        val sessions = FakeFocusSessionRepository(
            listOf(
                FocusSession(time.now() - 1.minutes, 25),
                FocusSession(time.now() - 48.hours, 25), // two days ago: not "today"
            ),
        )
        val timer = PomodoroTimer(
            scope = backgroundScope,
            time = time,
            preferencesRepository = FakeUserPreferencesRepository(),
            stateStore = FakeTimerStateStore(),
            sessionRepository = sessions,
            notifier = FakePhaseNotifier(),
            messages = { NotificationContent("", "") },
        )
        val viewModel = TimerViewModel(timer, time, sessions)

        viewModel.uiState.test {
            var item = awaitItem()
            if (item is TimerUiState.Loading) item = awaitItem()
            val idle = assertIs<TimerUiState.Ready>(item)
            assertEquals(TimerPhase.FOCUS, idle.phase)
            assertEquals(1, idle.todaySessions)
            assertEquals(25, idle.todayMinutes)

            viewModel.onStartPause()
            val running = assertIs<TimerUiState.Ready>(awaitItem())
            assertTrue(running.isRunning)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
