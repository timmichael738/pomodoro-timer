package com.kotlinapps.pomodoro.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kotlinapps.pomodoro.core.model.TimerPhase
import com.kotlinapps.pomodoro.core.timer.TimerState
import com.kotlinapps.pomodoro.core.timer.TimerStatus
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/** Persists the timer so it survives process death (e.g. the OS reclaiming a backgrounded app). */
interface TimerStateStore {
    suspend fun load(): TimerState?
    suspend fun save(state: TimerState)
}

internal class DataStoreTimerStateStore(
    private val dataStore: DataStore<Preferences>,
) : TimerStateStore {

    private object Keys {
        val PHASE = stringPreferencesKey("timer_phase")
        val COMPLETED_IN_CYCLE = intPreferencesKey("timer_completed_in_cycle")
        val STATUS = stringPreferencesKey("timer_status")
        val ENDS_AT = longPreferencesKey("timer_ends_at")
        val REMAINING = longPreferencesKey("timer_remaining_ms")
        val TOTAL = longPreferencesKey("timer_total_ms")
    }

    private enum class StatusKind { IDLE, RUNNING, PAUSED }

    override suspend fun load(): TimerState? = runCatching {
        val prefs = dataStore.data.first()
        val phase = prefs[Keys.PHASE]?.let { name -> TimerPhase.entries.firstOrNull { it.name == name } }
            ?: return null
        val total = prefs[Keys.TOTAL]?.milliseconds
        val status = when (prefs[Keys.STATUS]?.let { runCatching { StatusKind.valueOf(it) }.getOrNull() }) {
            StatusKind.RUNNING -> {
                val endsAt = prefs[Keys.ENDS_AT]
                if (endsAt != null && total != null) {
                    TimerStatus.Running(Instant.fromEpochMilliseconds(endsAt), total)
                } else {
                    TimerStatus.Idle
                }
            }
            StatusKind.PAUSED -> {
                val remaining = prefs[Keys.REMAINING]?.milliseconds
                if (remaining != null && total != null) TimerStatus.Paused(remaining, total) else TimerStatus.Idle
            }
            StatusKind.IDLE, null -> TimerStatus.Idle
        }
        TimerState(
            phase = phase,
            completedInCycle = (prefs[Keys.COMPLETED_IN_CYCLE] ?: 0).coerceAtLeast(0),
            status = status,
        )
    }.getOrNull() // Unreadable state is not worth crashing over: start fresh.

    override suspend fun save(state: TimerState) {
        dataStore.edit { prefs ->
            prefs[Keys.PHASE] = state.phase.name
            prefs[Keys.COMPLETED_IN_CYCLE] = state.completedInCycle
            prefs.remove(Keys.ENDS_AT)
            prefs.remove(Keys.REMAINING)
            prefs.remove(Keys.TOTAL)
            when (val status = state.status) {
                TimerStatus.Idle -> prefs[Keys.STATUS] = StatusKind.IDLE.name
                is TimerStatus.Running -> {
                    prefs[Keys.STATUS] = StatusKind.RUNNING.name
                    prefs[Keys.ENDS_AT] = status.endsAt.toEpochMilliseconds()
                    prefs[Keys.TOTAL] = status.total.inWholeMilliseconds
                }
                is TimerStatus.Paused -> {
                    prefs[Keys.STATUS] = StatusKind.PAUSED.name
                    prefs[Keys.REMAINING] = status.remaining.inWholeMilliseconds
                    prefs[Keys.TOTAL] = status.total.inWholeMilliseconds
                }
            }
        }
    }
}
