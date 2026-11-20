package com.kotlinapps.pomodoro.core.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/** A completed (not skipped) focus phase. */
data class FocusSession(
    val completedAt: Instant,
    val durationMinutes: Int,
)

/** Focus totals for one calendar day. */
data class DailyFocus(
    val date: LocalDate,
    val sessions: Int,
    val minutes: Int,
)

data class FocusTotals(
    val sessions: Int,
    val minutes: Int,
)
