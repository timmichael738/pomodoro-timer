package com.kotlinapps.pomodoro.core.domain

import com.kotlinapps.pomodoro.core.model.TimerPhase
import com.kotlinapps.pomodoro.core.timer.TimerState
import com.kotlinapps.pomodoro.core.timer.TimerStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class PomodoroCycleTest {

    private val running = TimerStatus.Running(Instant.fromEpochSeconds(0), 25.minutes)

    @Test
    fun completedFocus_beforeTarget_goesToShortBreak() {
        val next = PomodoroCycle.advance(TimerState(TimerPhase.FOCUS, 1, running), 4, completed = true)
        assertEquals(TimerState(TimerPhase.SHORT_BREAK, 2, TimerStatus.Idle), next)
    }

    @Test
    fun completedFocus_reachingTarget_goesToLongBreak() {
        val next = PomodoroCycle.advance(TimerState(TimerPhase.FOCUS, 3, running), 4, completed = true)
        assertEquals(TimerState(TimerPhase.LONG_BREAK, 4), next)
    }

    @Test
    fun completedFocus_beyondLoweredTarget_stillGoesToLongBreak() {
        val next = PomodoroCycle.advance(TimerState(TimerPhase.FOCUS, 5), 2, completed = true)
        assertEquals(TimerPhase.LONG_BREAK, next.phase)
    }

    @Test
    fun skippedFocus_isNotCounted_andNeverEarnsLongBreak() {
        val next = PomodoroCycle.advance(TimerState(TimerPhase.FOCUS, 3, running), 4, completed = false)
        assertEquals(TimerState(TimerPhase.SHORT_BREAK, 3), next)
    }

    @Test
    fun shortBreak_returnsToFocus_keepingCount() {
        val next = PomodoroCycle.advance(TimerState(TimerPhase.SHORT_BREAK, 2, running), 4, completed = true)
        assertEquals(TimerState(TimerPhase.FOCUS, 2), next)
    }

    @Test
    fun longBreak_completedOrSkipped_startsNewCycle() {
        listOf(true, false).forEach { completed ->
            val next = PomodoroCycle.advance(TimerState(TimerPhase.LONG_BREAK, 4, running), 4, completed)
            assertEquals(TimerState(TimerPhase.FOCUS, 0), next)
        }
    }
}
