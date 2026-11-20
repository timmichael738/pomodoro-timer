package com.kotlinapps.pomodoro.core.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The current instant and the user's time zone. Injected everywhere time matters,
 * so tests can drive the timer with virtual time.
 */
interface TimeProvider {
    fun now(): Instant
    fun timeZone(): TimeZone

    fun today(): LocalDate = now().toLocalDateTime(timeZone()).date

    fun startOf(date: LocalDate): Instant = date.atStartOfDayIn(timeZone())
}

class SystemTimeProvider : TimeProvider {
    override fun now(): Instant = Clock.System.now()
    override fun timeZone(): TimeZone = TimeZone.currentSystemDefault()
}
