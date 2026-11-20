package com.kotlinapps.pomodoro.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlinapps.pomodoro.core.data.repository.FocusSessionRepository
import com.kotlinapps.pomodoro.core.domain.FocusStats
import com.kotlinapps.pomodoro.core.model.DailyFocus
import com.kotlinapps.pomodoro.core.model.FocusTotals
import com.kotlinapps.pomodoro.core.time.TimeProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus

sealed interface StatsUiState {
    data object Loading : StatsUiState
    data object Empty : StatsUiState
    data object Error : StatsUiState

    data class Success(
        /** Exactly [DAYS] entries, oldest first; the last one is today. */
        val days: List<DailyFocus>,
        val allTime: FocusTotals,
    ) : StatsUiState {
        val today: DailyFocus get() = days.last()
        val week: FocusTotals get() = FocusTotals(days.sumOf { it.sessions }, days.sumOf { it.minutes })
    }

    companion object {
        const val DAYS = 7
    }
}

class StatsViewModel(
    private val sessionRepository: FocusSessionRepository,
    time: TimeProvider,
) : ViewModel() {

    private val today = time.today()

    val uiState: StateFlow<StatsUiState> = combine(
        sessionRepository.observeSessionsSince(time.startOf(today.minus(DatePeriod(days = StatsUiState.DAYS - 1)))),
        sessionRepository.observeTotals(),
    ) { recent, totals ->
        if (totals.sessions == 0) {
            StatsUiState.Empty
        } else {
            StatsUiState.Success(
                days = FocusStats.dailyTotals(recent, today, time.timeZone(), StatsUiState.DAYS),
                allTime = totals,
            )
        }
    }
        .catch { emit(StatsUiState.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState.Loading)

    fun onClearHistory() {
        viewModelScope.launch { sessionRepository.clearHistory() }
    }
}
