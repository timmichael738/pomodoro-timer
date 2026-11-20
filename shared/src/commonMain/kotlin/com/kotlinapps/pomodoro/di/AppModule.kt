package com.kotlinapps.pomodoro.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import com.kotlinapps.pomodoro.AppViewModel
import com.kotlinapps.pomodoro.core.data.repository.DataStoreTimerStateStore
import com.kotlinapps.pomodoro.core.data.repository.DataStoreUserPreferencesRepository
import com.kotlinapps.pomodoro.core.data.repository.FocusSessionRepository
import com.kotlinapps.pomodoro.core.data.repository.OfflineFocusSessionRepository
import com.kotlinapps.pomodoro.core.data.repository.TimerStateStore
import com.kotlinapps.pomodoro.core.data.repository.UserPreferencesRepository
import com.kotlinapps.pomodoro.core.database.PomodoroDatabase
import com.kotlinapps.pomodoro.core.database.buildDatabase
import com.kotlinapps.pomodoro.core.notification.PhaseMessages
import com.kotlinapps.pomodoro.core.notification.ResourcePhaseMessages
import com.kotlinapps.pomodoro.core.time.SystemTimeProvider
import com.kotlinapps.pomodoro.core.time.TimeProvider
import com.kotlinapps.pomodoro.core.timer.PomodoroTimer
import com.kotlinapps.pomodoro.feature.settings.SettingsViewModel
import com.kotlinapps.pomodoro.feature.stats.StatsViewModel
import com.kotlinapps.pomodoro.feature.timer.TimerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import okio.Path.Companion.toPath
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** Koin qualifier for the absolute path of the preferences DataStore file. */
internal val DataStorePath = named("dataStorePath")
internal const val DATASTORE_FILE_NAME = "pomodoro.preferences_pb"

/** Koin qualifier for the app-lifetime coroutine scope (main thread). */
internal val AppScope = named("appScope")

/**
 * Platform-specific pieces: a `RoomDatabase.Builder<PomodoroDatabase>`, the DataStore file path
 * (qualified with [DataStorePath]) and a `PhaseNotifier`.
 */
internal expect val platformModule: Module

internal val dataModule = module {
    single<CoroutineScope>(AppScope) { MainScope() }
    single<PomodoroDatabase> { get<RoomDatabase.Builder<PomodoroDatabase>>().buildDatabase() }
    single { get<PomodoroDatabase>().focusSessionDao() }
    // DataStore must be a singleton per file; two instances for one file throw at runtime.
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath(produceFile = { get<String>(DataStorePath).toPath() })
    }
    single<FocusSessionRepository> { OfflineFocusSessionRepository(get()) }
    single<UserPreferencesRepository> { DataStoreUserPreferencesRepository(get()) }
    single<TimerStateStore> { DataStoreTimerStateStore(get()) }
    single<TimeProvider> { SystemTimeProvider() }
    single<PhaseMessages> { ResourcePhaseMessages() }
    single {
        PomodoroTimer(
            scope = get(AppScope),
            time = get(),
            preferencesRepository = get(),
            stateStore = get(),
            sessionRepository = get(),
            notifier = get(),
            messages = get(),
        )
    }
}

internal val viewModelModule = module {
    viewModelOf(::AppViewModel)
    viewModelOf(::TimerViewModel)
    viewModelOf(::StatsViewModel)
    viewModelOf(::SettingsViewModel)
}

/** Single entry point used by both the Android Application and the iOS app. */
fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(platformModule, dataModule, viewModelModule)
    }
}
