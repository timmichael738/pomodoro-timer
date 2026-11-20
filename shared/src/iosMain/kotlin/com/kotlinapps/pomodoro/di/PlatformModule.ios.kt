package com.kotlinapps.pomodoro.di

import androidx.room.Room
import androidx.room.RoomDatabase
import com.kotlinapps.pomodoro.core.database.PomodoroDatabase
import com.kotlinapps.pomodoro.core.notification.LocalPhaseNotifier
import com.kotlinapps.pomodoro.core.notification.PhaseNotifier
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

internal actual val platformModule: Module = module {
    single<RoomDatabase.Builder<PomodoroDatabase>> {
        Room.databaseBuilder<PomodoroDatabase>(
            name = "${documentDirectory()}/${PomodoroDatabase.FILE_NAME}",
        )
    }
    single(DataStorePath) { "${documentDirectory()}/$DATASTORE_FILE_NAME" }
    single<PhaseNotifier> { LocalPhaseNotifier() }
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(url?.path) { "Documents directory not found" }
}
