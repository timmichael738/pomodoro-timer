package com.kotlinapps.pomodoro.core.timer

import com.kotlinapps.pomodoro.core.model.FocusSession
import com.kotlinapps.pomodoro.core.model.TimerPhase
import com.kotlinapps.pomodoro.core.model.TimerSettings
import com.kotlinapps.pomodoro.core.model.UserPreferences
import com.kotlinapps.pomodoro.core.notification.NotificationContent
import com.kotlinapps.pomodoro.core.notification.PhaseMessages
import com.kotlinapps.pomodoro.testing.FakeFocusSessionRepository
import com.kotlinapps.pomodoro.testing.FakePhaseNotifier
import com.kotlinapps.pomodoro.testing.FakeTimerStateStore
import com.kotlinapps.pomodoro.testing.FakeUserPreferencesRepository
import com.kotlinapps.pomodoro.testing.TestTimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class PomodoroTimerTest {

    private val sessions = FakeFocusSessionRepository()
    private val store = FakeTimerStateStore()
    private val notifier = FakePhaseNotifier()
    private val messages = PhaseMessages { phase -> NotificationContent(title = "${phase.name} done", body = "") }

    private class Env(val timer: PomodoroTimer, val time: TestTimeProvider)

    /** Builds a timer in the test's background scope and lets it restore its state. */
    private fun TestScope.createTimer(settings: TimerSettings = TimerSettings()): Env {
        val time = TestTimeProvider(testScheduler)
        val timer = PomodoroTimer(
            scope = backgroundScope,
            time = time,
            preferencesRepository = FakeUserPreferencesRepository(UserPreferences(timerSettings = settings)),
            stateStore = store,
            sessionRepository = sessions,
            notifier = notifier,
            messages = messages,
        )
        runCurrent()
        return Env(timer, time)
    }

    private fun TestScope.advance(duration: Duration) {
        advanceTimeBy(duration)
        runCurrent()
    }

    private val PomodoroTimer.current: TimerState get() = requireNotNull(state.value)

    @Test
    fun startsIdleInFocus_whenNothingSaved() = runTest {
        val env = createTimer()
        assertEquals(TimerState(), env.timer.current)
    }

    @Test
    fun focusRunsToCompletion_thenShortBreak_andSessionIsRecorded() = runTest {
        val env = createTimer()
        val startedAt = env.time.now()

        env.timer.start()
        runCurrent()
        val running = assertIs<TimerStatus.Running>(env.timer.current.status)
        assertEquals(startedAt + 25.minutes, running.endsAt)
        assertEquals(running.endsAt, notifier.scheduled?.first)
        assertEquals("FOCUS done", notifier.scheduled?.second?.title)

        advance(25.minutes - 1.minutes)
        assertEquals(TimerPhase.FOCUS, env.timer.current.phase)

        advance(1.minutes)
        assertEquals(TimerState(TimerPhase.SHORT_BREAK, completedInCycle = 1), env.timer.current)
        assertEquals(listOf(FocusSession(startedAt + 25.minutes, 25)), sessions.sessions.value)
        assertEquals(env.timer.current, store.saved)
    }

    @Test
    fun pauseAndResume_keepsRemainingTime() = runTest {
        val env = createTimer()
        env.timer.start()
        advance(10.minutes)

        env.timer.pause()
        assertEquals(TimerStatus.Paused(remaining = 15.minutes, total = 25.minutes), env.timer.current.status)
        assertNull(notifier.scheduled)

        advance(2.hours) // nothing happens while paused
        assertIs<TimerStatus.Paused>(env.timer.current.status)

        env.timer.start()
        runCurrent()
        assertEquals(TimerStatus.Running(env.time.now() + 15.minutes, 25.minutes), env.timer.current.status)

        advance(15.minutes)
        assertEquals(TimerPhase.SHORT_BREAK, env.timer.current.phase)
        assertEquals(25, sessions.sessions.value.single().durationMinutes)
    }

    @Test
    fun longBreakComesAfterConfiguredSessions_andResetsCycle() = runTest {
        val settings = TimerSettings(focusMinutes = 1, shortBreakMinutes = 1, longBreakMinutes = 2, sessionsUntilLongBreak = 2)
        val env = createTimer(settings)
        fun runPhase(length: Duration) {
            env.timer.start()
            advance(length)
        }

        runPhase(1.minutes)
        assertEquals(TimerState(TimerPhase.SHORT_BREAK, 1), env.timer.current)
        runPhase(1.minutes)
        assertEquals(TimerState(TimerPhase.FOCUS, 1), env.timer.current)
        runPhase(1.minutes)
        assertEquals(TimerState(TimerPhase.LONG_BREAK, 2), env.timer.current)
        runPhase(2.minutes)
        assertEquals(TimerState(TimerPhase.FOCUS, 0), env.timer.current)
        assertEquals(2, sessions.sessions.value.size)
    }

    @Test
    fun skip_movesOnWithoutRecording() = runTest {
        val env = createTimer()
        env.timer.start()
        advance(5.minutes)

        env.timer.skip()
        assertEquals(TimerState(TimerPhase.SHORT_BREAK, completedInCycle = 0), env.timer.current)
        assertTrue(sessions.sessions.value.isEmpty())
        assertNull(notifier.scheduled)

        advance(1.hours) // the cancelled completion never fires
        assertEquals(TimerPhase.SHORT_BREAK, env.timer.current.phase)
        assertTrue(sessions.sessions.value.isEmpty())
    }

    @Test
    fun reset_returnsCurrentPhaseToIdle() = runTest {
        val env = createTimer()
        env.timer.start()
        advance(5.minutes)

        env.timer.reset()
        assertEquals(TimerState(TimerPhase.FOCUS, 0, TimerStatus.Idle), env.timer.current)
        assertNull(notifier.scheduled)
    }

    @Test
    fun autoStartBreaks_startsBreakImmediately() = runTest {
        val env = createTimer(TimerSettings(autoStartBreaks = true))
        env.timer.start()
        advance(25.minutes)

        val state = env.timer.current
        assertEquals(TimerPhase.SHORT_BREAK, state.phase)
        assertEquals(TimerStatus.Running(env.time.now() + 5.minutes, 5.minutes), state.status)
        assertEquals("SHORT_BREAK done", notifier.scheduled?.second?.title)
    }

    @Test
    fun refresh_catchesUpWhenClockJumpedAhead() = runTest {
        val env = createTimer()
        env.timer.start()
        runCurrent()
        val endsAt = assertIs<TimerStatus.Running>(env.timer.current.status).endsAt

        // The app was suspended for 40 minutes: the clock moved, but no coroutine ran.
        env.time.skew = 40.minutes
        env.timer.refresh()
        runCurrent()

        assertEquals(TimerState(TimerPhase.SHORT_BREAK, 1), env.timer.current)
        assertEquals(endsAt, sessions.sessions.value.single().completedAt)
    }

    @Test
    fun restore_phaseThatEndedWhileKilled_isRecordedWithoutAutoStart() = runTest {
        val time = TestTimeProvider(testScheduler)
        val endedAt = time.now() - 3.minutes
        store.saved = TimerState(TimerPhase.FOCUS, 0, TimerStatus.Running(endedAt, 25.minutes))

        val env = createTimer(TimerSettings(autoStartBreaks = true))

        assertEquals(TimerState(TimerPhase.SHORT_BREAK, 1), env.timer.current)
        assertEquals(listOf(FocusSession(endedAt, 25)), sessions.sessions.value)
    }

    @Test
    fun restore_runningPhase_completesOnSchedule() = runTest {
        val time = TestTimeProvider(testScheduler)
        store.saved = TimerState(TimerPhase.SHORT_BREAK, 1, TimerStatus.Running(time.now() + 2.minutes, 5.minutes))

        val env = createTimer()
        assertIs<TimerStatus.Running>(env.timer.current.status)

        advance(2.minutes)
        assertEquals(TimerState(TimerPhase.FOCUS, 1), env.timer.current)
        assertTrue(sessions.sessions.value.isEmpty()) // breaks are not recorded
    }

    @Test
    fun idleDurationFollowsSettings_butRunningPhaseKeepsItsLength() = runTest {
        val prefs = FakeUserPreferencesRepository()
        val time = TestTimeProvider(testScheduler)
        val timer = PomodoroTimer(backgroundScope, time, prefs, store, sessions, notifier, messages)
        runCurrent()

        timer.start()
        prefs.updateTimerSettings { it.copy(focusMinutes = 50) }
        runCurrent()
        assertEquals(25.minutes, assertIs<TimerStatus.Running>(timer.current.status).total)

        advanceTimeBy(25.minutes)
        runCurrent()
        assertEquals(25, sessions.sessions.value.single().durationMinutes)
    }
}
