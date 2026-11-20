package com.kotlinapps.pomodoro.core.data.repository

import com.kotlinapps.pomodoro.core.database.dao.FocusSessionDao
import com.kotlinapps.pomodoro.core.database.model.asEntity
import com.kotlinapps.pomodoro.core.database.model.asExternalModel
import com.kotlinapps.pomodoro.core.model.FocusSession
import com.kotlinapps.pomodoro.core.model.FocusTotals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

interface FocusSessionRepository {
    fun observeSessionsSince(from: Instant): Flow<List<FocusSession>>
    fun observeTotals(): Flow<FocusTotals>
    suspend fun addSession(session: FocusSession)
    suspend fun clearHistory()
}

internal class OfflineFocusSessionRepository(
    private val dao: FocusSessionDao,
) : FocusSessionRepository {

    override fun observeSessionsSince(from: Instant): Flow<List<FocusSession>> =
        dao.observeSince(from.toEpochMilliseconds()).map { rows -> rows.map { it.asExternalModel() } }

    override fun observeTotals(): Flow<FocusTotals> = dao.observeTotals().map { it.asExternalModel() }

    override suspend fun addSession(session: FocusSession) = dao.insert(session.asEntity())

    override suspend fun clearHistory() = dao.deleteAll()
}
