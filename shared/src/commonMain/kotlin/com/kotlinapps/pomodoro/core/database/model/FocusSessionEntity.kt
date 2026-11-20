package com.kotlinapps.pomodoro.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kotlinapps.pomodoro.core.model.FocusSession
import com.kotlinapps.pomodoro.core.model.FocusTotals
import kotlin.time.Instant

@Entity(
    tableName = "focus_sessions",
    indices = [Index("completed_at")],
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Epoch milliseconds (UTC). Stored as an instant so day grouping follows the user's current time zone. */
    @ColumnInfo(name = "completed_at") val completedAtMillis: Long,
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Int,
)

/** Result row of the all-time totals query. */
data class FocusTotalsRow(
    val sessions: Int,
    val minutes: Int,
)

internal fun FocusSessionEntity.asExternalModel() = FocusSession(
    completedAt = Instant.fromEpochMilliseconds(completedAtMillis),
    durationMinutes = durationMinutes,
)

internal fun FocusSession.asEntity() = FocusSessionEntity(
    completedAtMillis = completedAt.toEpochMilliseconds(),
    durationMinutes = durationMinutes,
)

internal fun FocusTotalsRow.asExternalModel() = FocusTotals(sessions = sessions, minutes = minutes)
