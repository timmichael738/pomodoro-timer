package com.kotlinapps.pomodoro.di

import androidx.room.Room
import androidx.room.RoomDatabase
import com.kotlinapps.pomodoro.core.database.PomodoroDatabase
import com.kotlinapps.pomodoro.core.notification.AlarmPhaseNotifier
import com.kotlinapps.pomodoro.core.notification.PhaseNotifier
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformModule: Module = module {
    single<RoomDatabase.Builder<PomodoroDatabase>> {
        val context = androidContext()
        Room.databaseBuilder<PomodoroDatabase>(
            context = context,
            name = context.getDatabasePath(PomodoroDatabase.FILE_NAME).absolutePath,
        )
    }
    single(DataStorePath) { androidContext().filesDir.resolve(DATASTORE_FILE_NAME).absolutePath }
    single<PhaseNotifier> { AlarmPhaseNotifier(androidContext()) }
}
