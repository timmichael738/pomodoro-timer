package com.kotlinapps.pomodoro.core.domain

import com.kotlinapps.pomodoro.core.model.TimerPhase
import com.kotlinapps.pomodoro.core.timer.TimerState
import com.kotlinapps.pomodoro.core.timer.TimerStatus

/** Pure rules for moving through focus → break → focus …, with a long break every N sessions. */
object PomodoroCycle {

    /** The idle state that follows [state] when its phase finishes (`completed = true`) or is skipped. */
    fun advance(state: TimerState, sessionsUntilLongBreak: Int, completed: Boolean): TimerState =
        when (state.phase) {
            TimerPhase.FOCUS -> {
                val done = if (completed) state.completedInCycle + 1 else state.completedInCycle
                val nextPhase = if (completed && done >= sessionsUntilLongBreak) {
                    TimerPhase.LONG_BREAK
                } else {
                    TimerPhase.SHORT_BREAK
                }
                TimerState(phase = nextPhase, completedInCycle = done, status = TimerStatus.Idle)
            }
            TimerPhase.SHORT_BREAK -> state.copy(phase = TimerPhase.FOCUS, status = TimerStatus.Idle)
            // A long break closes the cycle.
            TimerPhase.LONG_BREAK -> TimerState(phase = TimerPhase.FOCUS, completedInCycle = 0)
        }
}
