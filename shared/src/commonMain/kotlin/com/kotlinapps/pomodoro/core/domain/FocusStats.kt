package com.kotlinapps.pomodoro.core.domain

import com.kotlinapps.pomodoro.core.model.DailyFocus
import com.kotlinapps.pomodoro.core.model.FocusSession
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

object FocusStats {

    /**
     * Groups [sessions] by local calendar day and returns exactly [days] entries ending at [today]
     * (oldest first), with zeros for days without focus time.
     */
    fun dailyTotals(
        sessions: List<FocusSession>,
        today: LocalDate,
        timeZone: TimeZone,
        days: Int = 7,
    ): List<DailyFocus> {
        require(days > 0) { "days must be positive" }
        val byDate = sessions.groupBy { it.completedAt.toLocalDateTime(timeZone).date }
        val first = today.minus(DatePeriod(days = days - 1))
        return (0 until days).map { offset ->
            val date = first.plus(DatePeriod(days = offset))
            val daySessions = byDate[date].orEmpty()
            DailyFocus(date = date, sessions = daySessions.size, minutes = daySessions.sumOf { it.durationMinutes })
        }
    }
}
