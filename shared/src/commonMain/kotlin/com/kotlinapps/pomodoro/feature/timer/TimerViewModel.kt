package com.kotlinapps.pomodoro.feature.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlinapps.pomodoro.core.data.repository.FocusSessionRepository
import com.kotlinapps.pomodoro.core.model.TimerPhase
import com.kotlinapps.pomodoro.core.model.TimerSettings
import com.kotlinapps.pomodoro.core.time.TimeProvider
import com.kotlinapps.pomodoro.core.timer.PomodoroTimer
import com.kotlinapps.pomodoro.core.timer.TimerState
import com.kotlinapps.pomodoro.core.timer.TimerStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

sealed interface TimerUiState {
    data object Loading : TimerUiState

    data class Ready(
        val phase: TimerPhase,
        val isRunning: Boolean,
        /** False only for a phase that has not been started, so "Reset" has nothing to do. */
        val isStarted: Boolean,
        val remaining: Duration,
        val total: Duration,
        val completedInCycle: Int,
        val sessionsUntilLongBreak: Int,
        val todaySessions: Int,
        val todayMinutes: Int,
    ) : TimerUiState {
        /** Fraction of the phase that has elapsed, 0..1. */
        val progress: Float
            get() = if (total <= Duration.ZERO) 0f else (1f - (remaining / total).toFloat()).coerceIn(0f, 1f)

        /** Remaining time rounded up to whole seconds, so the display reads 25:00 at the start and 00:00 at the end. */
        val remainingSeconds: Long
            get() = (remaining.inWholeMilliseconds + 999) / 1000
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModel(
    private val timer: PomodoroTimer,
    private val time: TimeProvider,
    sessionRepository: FocusSessionRepository,
) : ViewModel() {

    /** Emits the current instant: every [TICK] while running, once otherwise. */
    private val now: Flow<Instant> = timer.state
        .map { it?.status is TimerStatus.Running }
        .distinctUntilChanged()
        .flatMapLatest { running ->
            if (running) {
                flow {
                    while (true) {
                        emit(time.now())
                        delay(TICK)
                    }
                }
            } else {
                flowOf(time.now())
            }
        }

    private val today = sessionRepository.observeSessionsSince(time.startOf(time.today()))

    val uiState: StateFlow<TimerUiState> = combine(
        timer.state.filterNotNull(),
        timer.settings.filterNotNull(),
        now,
        today,
    ) { state, settings, now, todaySessions ->
        state.toUiState(settings, now, todaySessions.size, todaySessions.sumOf { it.durationMinutes })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimerUiState.Loading)

    fun onStartPause() {
        when (timer.state.value?.status) {
            is TimerStatus.Running -> timer.pause()
            else -> timer.start()
        }
    }

    fun onReset() = timer.reset()

    fun onSkip() = timer.skip()

    /** Called when the screen returns to the foreground. */
    fun onResume() = timer.refresh()

    private companion object {
        val TICK = 200.milliseconds
    }
}

internal fun TimerState.toUiState(
    settings: TimerSettings,
    now: Instant,
    todaySessions: Int,
    todayMinutes: Int,
): TimerUiState.Ready {
    val (remaining, total) = when (val s = status) {
        TimerStatus.Idle -> settings.durationOf(phase).let { it to it }
        is TimerStatus.Paused -> s.remaining to s.total
        is TimerStatus.Running -> (s.endsAt - now).coerceIn(Duration.ZERO, s.total) to s.total
    }
    return TimerUiState.Ready(
        phase = phase,
        isRunning = status is TimerStatus.Running,
        isStarted = status !is TimerStatus.Idle,
        remaining = remaining,
        total = total,
        completedInCycle = completedInCycle,
        sessionsUntilLongBreak = settings.sessionsUntilLongBreak,
        todaySessions = todaySessions,
        todayMinutes = todayMinutes,
    )
}
