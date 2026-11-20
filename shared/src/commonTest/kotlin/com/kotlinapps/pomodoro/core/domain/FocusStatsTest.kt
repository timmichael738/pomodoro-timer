package com.kotlinapps.pomodoro.core.domain

import com.kotlinapps.pomodoro.core.model.DailyFocus
import com.kotlinapps.pomodoro.core.model.FocusSession
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class FocusStatsTest {

    private val today = LocalDate(2026, 11, 20)

    private fun session(iso: String, minutes: Int = 25) = FocusSession(Instant.parse(iso), minutes)

    @Test
    fun noSessions_givesSevenZeroDaysEndingToday() {
        val days = FocusStats.dailyTotals(emptyList(), today, TimeZone.UTC)
        assertEquals(7, days.size)
        assertEquals(LocalDate(2026, 11, 14), days.first().date)
        assertEquals(today, days.last().date)
        assertEquals(0, days.sumOf { it.minutes })
    }

    @Test
    fun sessionsAreGroupedPerDay() {
        val days = FocusStats.dailyTotals(
            listOf(
                session("2026-11-20T08:00:00Z"),
                session("2026-11-20T12:00:00Z", minutes = 50),
                session("2026-11-18T23:59:00Z"),
            ),
            today,
            TimeZone.UTC,
        )
        assertEquals(DailyFocus(today, sessions = 2, minutes = 75), days.last())
        assertEquals(DailyFocus(LocalDate(2026, 11, 18), 1, 25), days[4])
    }

    @Test
    fun daysFollowTheGivenTimeZone() {
        // 23:30 UTC on the 19th is already the 20th in Tokyo.
        val tokyo = TimeZone.of("Asia/Tokyo")
        val days = FocusStats.dailyTotals(listOf(session("2026-11-19T23:30:00Z")), today, tokyo)
        assertEquals(1, days.last().sessions)
    }

    @Test
    fun sessionsOutsideTheWindowAreIgnored() {
        val days = FocusStats.dailyTotals(listOf(session("2026-11-01T10:00:00Z")), today, TimeZone.UTC)
        assertEquals(0, days.sumOf { it.sessions })
    }

    @Test
    fun nonPositiveDays_isRejected() {
        assertFailsWith<IllegalArgumentException> { FocusStats.dailyTotals(emptyList(), today, TimeZone.UTC, days = 0) }
    }
}
