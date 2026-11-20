package com.kotlinapps.pomodoro.testing

import com.kotlinapps.pomodoro.core.data.repository.FocusSessionRepository
import com.kotlinapps.pomodoro.core.data.repository.TimerStateStore
import com.kotlinapps.pomodoro.core.data.repository.UserPreferencesRepository
import com.kotlinapps.pomodoro.core.model.FocusSession
import com.kotlinapps.pomodoro.core.model.FocusTotals
import com.kotlinapps.pomodoro.core.model.ThemeMode
import com.kotlinapps.pomodoro.core.model.TimerSettings
import com.kotlinapps.pomodoro.core.model.UserPreferences
import com.kotlinapps.pomodoro.core.notification.NotificationContent
import com.kotlinapps.pomodoro.core.notification.PhaseNotifier
import com.kotlinapps.pomodoro.core.time.TimeProvider
import com.kotlinapps.pomodoro.core.timer.TimerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.datetime.TimeZone
import kotlin.time.Duration
import kotlin.time.Instant

class FakeUserPreferencesRepository(initial: UserPreferences = UserPreferences()) : UserPreferencesRepository {
    private val state = MutableStateFlow(initial)
    override val userPreferences: Flow<UserPreferences> = state

    override suspend fun setThemeMode(themeMode: ThemeMode) = state.update { it.copy(themeMode = themeMode) }

    override suspend fun setUseDynamicColor(useDynamicColor: Boolean) =
        state.update { it.copy(useDynamicColor = useDynamicColor) }

    override suspend fun updateTimerSettings(transform: (TimerSettings) -> TimerSettings) =
        state.update { it.copy(timerSettings = transform(it.timerSettings)) }
}

class FakeFocusSessionRepository(initial: List<FocusSession> = emptyList()) : FocusSessionRepository {
    val sessions = MutableStateFlow(initial)
    var failWith: Throwable? = null

    override fun observeSessionsSince(from: Instant): Flow<List<FocusSession>> = sessions.map { all ->
        failWith?.let { throw it }
        all.filter { it.completedAt >= from }
    }

    override fun observeTotals(): Flow<FocusTotals> = sessions.map { all ->
        failWith?.let { throw it }
        FocusTotals(all.size, all.sumOf { it.durationMinutes })
    }

    override suspend fun addSession(session: FocusSession) = sessions.update { it + session }

    override suspend fun clearHistory() = sessions.update { emptyList() }
}

class FakeTimerStateStore(var saved: TimerState? = null) : TimerStateStore {
    override suspend fun load(): TimerState? = saved
    override suspend fun save(state: TimerState) {
        saved = state
    }
}

class FakePhaseNotifier : PhaseNotifier {
    var scheduled: Pair<Instant, NotificationContent>? = null
        private set
    var cancelCount = 0
        private set

    override fun schedule(at: Instant, content: NotificationContent) {
        scheduled = at to content
    }

    override fun cancel() {
        scheduled = null
        cancelCount++
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
/** A clock driven by the coroutine test scheduler, so `delay` and "now" stay in lockstep. */
class TestTimeProvider(
    private val scheduler: TestCoroutineScheduler,
    private val start: Instant = Instant.parse("2026-11-20T09:00:00Z"),
    private val zone: TimeZone = TimeZone.UTC,
) : TimeProvider {
    /** Extra wall-clock time that coroutine delays did not see, e.g. while the app was suspended. */
    var skew: Duration = Duration.ZERO

    override fun now(): Instant =
        Instant.fromEpochMilliseconds(start.toEpochMilliseconds() + scheduler.currentTime) + skew
    override fun timeZone(): TimeZone = zone
}
