package com.kotlinapps.pomodoro.core.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** User-configurable lengths and behavior of the Pomodoro cycle. Defaults follow the classic technique. */
data class TimerSettings(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val sessionsUntilLongBreak: Int = 4,
    val autoStartBreaks: Boolean = false,
    val autoStartFocus: Boolean = false,
) {
    fun durationOf(phase: TimerPhase): Duration = when (phase) {
        TimerPhase.FOCUS -> focusMinutes
        TimerPhase.SHORT_BREAK -> shortBreakMinutes
        TimerPhase.LONG_BREAK -> longBreakMinutes
    }.minutes

    fun autoStarts(phase: TimerPhase): Boolean =
        if (phase == TimerPhase.FOCUS) autoStartFocus else autoStartBreaks

    companion object {
        val FocusMinutesRange = 1..90
        val BreakMinutesRange = 1..60
        val SessionsRange = 2..8
    }
}

enum class TimerPhase { FOCUS, SHORT_BREAK, LONG_BREAK }
