package com.kotlinapps.pomodoro.core.timer

import com.kotlinapps.pomodoro.core.model.TimerPhase
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Everything needed to describe (and restore) the timer.
 *
 * A running timer stores its wall-clock end time rather than a countdown, so it stays correct
 * while the app is in the background or the process is killed.
 */
data class TimerState(
    val phase: TimerPhase = TimerPhase.FOCUS,
    /** Focus sessions finished in the current cycle; reset after a long break. */
    val completedInCycle: Int = 0,
    val status: TimerStatus = TimerStatus.Idle,
)

sealed interface TimerStatus {
    /** Not started; the phase will last its full configured duration. */
    data object Idle : TimerStatus

    data class Running(val endsAt: Instant, val total: Duration) : TimerStatus

    data class Paused(val remaining: Duration, val total: Duration) : TimerStatus
}
