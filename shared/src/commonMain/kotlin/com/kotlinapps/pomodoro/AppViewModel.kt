package com.kotlinapps.pomodoro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlinapps.pomodoro.core.data.repository.UserPreferencesRepository
import com.kotlinapps.pomodoro.core.model.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Exposes app-wide preferences (theme) to the root composable. Null until DataStore has been read. */
class AppViewModel(
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences?> = userPreferencesRepository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)
}
