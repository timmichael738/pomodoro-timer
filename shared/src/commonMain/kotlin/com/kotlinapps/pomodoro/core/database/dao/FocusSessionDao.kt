package com.kotlinapps.pomodoro.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kotlinapps.pomodoro.core.database.model.FocusSessionEntity
import com.kotlinapps.pomodoro.core.database.model.FocusTotalsRow
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {

    @Insert
    suspend fun insert(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions WHERE completed_at >= :fromMillis ORDER BY completed_at ASC")
    fun observeSince(fromMillis: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT COUNT(*) AS sessions, COALESCE(SUM(duration_minutes), 0) AS minutes FROM focus_sessions")
    fun observeTotals(): Flow<FocusTotalsRow>

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAll()
}
