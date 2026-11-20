package com.kotlinapps.pomodoro

import android.app.Application
import com.kotlinapps.pomodoro.di.initKoin
import org.koin.android.ext.koin.androidContext

class PomodoroApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@PomodoroApplication)
        }
    }
}
