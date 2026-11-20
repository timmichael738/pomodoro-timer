package com.kotlinapps.pomodoro.core.timer

import com.kotlinapps.pomodoro.core.data.repository.FocusSessionRepository
import com.kotlinapps.pomodoro.core.data.repository.TimerStateStore
import com.kotlinapps.pomodoro.core.data.repository.UserPreferencesRepository
import com.kotlinapps.pomodoro.core.domain.PomodoroCycle
import com.kotlinapps.pomodoro.core.model.FocusSession
import com.kotlinapps.pomodoro.core.model.TimerPhase
import com.kotlinapps.pomodoro.core.model.TimerSettings
import com.kotlinapps.pomodoro.core.notification.PhaseMessages
import com.kotlinapps.pomodoro.core.notification.PhaseNotifier
import com.kotlinapps.pomodoro.core.time.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * The single source of truth for the running Pomodoro, scoped to the whole app (not a screen),
 * so navigating away never stops the timer.
 *
 * - A running phase is stored as an end [Instant], so time spent in the background is counted.
 * - State is persisted after every change and restored on launch.
 * - The OS notification for the phase end is scheduled up front; in the foreground a coroutine
 *   advances the cycle at exactly the same moment.
 *
 * All methods must be called from [scope]'s (single-threaded, main) dispatcher.
 */
class PomodoroTimer(
    private val scope: CoroutineScope,
    private val time: TimeProvider,
    preferencesRepository: UserPreferencesRepository,
    private val stateStore: TimerStateStore,
    private val sessionRepository: FocusSessionRepository,
    private val notifier: PhaseNotifier,
    private val messages: PhaseMessages,
) {
    /** Latest timer settings; null until preferences have been read. */
    val settings: StateFlow<TimerSettings?> = preferencesRepository.userPreferences
        .map { it.timerSettings }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val _state = MutableStateFlow<TimerState?>(null)

    /** Current timer state; null until the saved state has been restored. */
    val state: StateFlow<TimerState?> = _state.asStateFlow()

    private var completionJob: Job? = null
    private var notificationJob: Job? = null

    private val currentSettings: TimerSettings get() = settings.value ?: TimerSettings()

    init {
        scope.launch { restore() }
    }

    private suspend fun restore() {
        settings.filterNotNull().first()
        val saved = stateStore.load() ?: TimerState()
        _state.value = saved
        val status = saved.status
        if (status is TimerStatus.Running) {
            if (status.endsAt <= time.now()) {
                // Finished while the app was not running: record it, but don't chain into a new phase.
                finishPhase(endedAt = status.endsAt, allowAutoStart = false)
            } else {
                scheduleCompletion(status.endsAt)
            }
        }
        // StateFlow conflates, so a burst of changes results in only the latest state being written.
        scope.launch { _state.filterNotNull().collect { stateStore.save(it) } }
    }

    fun start() {
        val current = _state.value ?: return
        val (remaining, total) = when (val status = current.status) {
            TimerStatus.Idle -> currentSettings.durationOf(current.phase).let { it to it }
            is TimerStatus.Paused -> status.remaining to status.total
            is TimerStatus.Running -> return
        }
        val endsAt = time.now() + remaining
        _state.value = current.copy(status = TimerStatus.Running(endsAt, total))
        scheduleCompletion(endsAt)
        notificationJob?.cancel()
        notificationJob = scope.launch { notifier.schedule(endsAt, messages.forEndOf(current.phase)) }
    }

    fun pause() {
        val current = _state.value ?: return
        val status = current.status as? TimerStatus.Running ?: return
        val remaining = (status.endsAt - time.now()).coerceAtLeast(Duration.ZERO)
        cancelPending()
        _state.value = current.copy(status = TimerStatus.Paused(remaining, status.total))
    }

    /** Restarts the current phase from its full length, without starting it. */
    fun reset() {
        val current = _state.value ?: return
        cancelPending()
        _state.value = current.copy(status = TimerStatus.Idle)
    }

    /** Moves to the next phase without counting the current one as completed. */
    fun skip() {
        val current = _state.value ?: return
        cancelPending()
        _state.value = PomodoroCycle.advance(current, currentSettings.sessionsUntilLongBreak, completed = false)
    }

    /**
     * Catches up after the app returns to the foreground. Coroutine delays may not advance
     * while the process is suspended, so the end time is re-checked against the clock.
     */
    fun refresh() {
        val status = _state.value?.status as? TimerStatus.Running ?: return
        if (status.endsAt <= time.now()) {
            completionJob?.cancel()
            completionJob = null
            finishPhase(endedAt = status.endsAt, allowAutoStart = true)
        }
    }

    private fun scheduleCompletion(endsAt: Instant) {
        completionJob?.cancel()
        completionJob = scope.launch {
            delay(endsAt - time.now())
            completionJob = null
            finishPhase(endedAt = endsAt, allowAutoStart = true)
        }
    }

    private fun finishPhase(endedAt: Instant, allowAutoStart: Boolean) {
        val current = _state.value ?: return
        val status = current.status as? TimerStatus.Running ?: return
        // The OS notification for this phase is the user's alert; it is left to be delivered.
        notificationJob = null
        if (current.phase == TimerPhase.FOCUS) {
            val session = FocusSession(completedAt = endedAt, durationMinutes = status.total.inWholeMinutes.toInt())
            scope.launch { sessionRepository.addSession(session) }
        }
        val settings = currentSettings
        val next = PomodoroCycle.advance(current, settings.sessionsUntilLongBreak, completed = true)
        _state.value = next
        if (allowAutoStart && settings.autoStarts(next.phase)) start()
    }

    private fun cancelPending() {
        completionJob?.cancel()
        completionJob = null
        notificationJob?.cancel()
        notificationJob = null
        notifier.cancel()
    }
}
