package com.kotlinapps.pomodoro.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kotlinapps.pomodoro.core.database.dao.FocusSessionDao
import com.kotlinapps.pomodoro.core.database.model.FocusSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [FocusSessionEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(PomodoroDatabaseConstructor::class)
abstract class PomodoroDatabase : RoomDatabase() {
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        const val FILE_NAME = "pomodoro.db"
    }
}

/** Room generates the `actual` implementations for each platform. */
@Suppress("KotlinNoActualForExpect")
expect object PomodoroDatabaseConstructor : RoomDatabaseConstructor<PomodoroDatabase> {
    override fun initialize(): PomodoroDatabase
}

/** Finishes a platform-specific builder with the shared configuration (bundled SQLite, IO dispatcher). */
fun RoomDatabase.Builder<PomodoroDatabase>.buildDatabase(): PomodoroDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
