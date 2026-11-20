package com.kotlinapps.pomodoro.core.notification

import androidx.compose.runtime.Composable
import com.kotlinapps.pomodoro.core.model.TimerPhase
import com.kotlinapps.pomodoro.resources.Res
import com.kotlinapps.pomodoro.resources.notification_break_done_body
import com.kotlinapps.pomodoro.resources.notification_break_done_title
import com.kotlinapps.pomodoro.resources.notification_channel_name
import com.kotlinapps.pomodoro.resources.notification_focus_done_body
import com.kotlinapps.pomodoro.resources.notification_focus_done_title
import org.jetbrains.compose.resources.getString
import kotlin.time.Instant

data class NotificationContent(
    val title: String,
    val body: String,
    /** User-visible name of the Android notification channel; unused on iOS. */
    val channelName: String = title,
)

/**
 * Schedules a system notification for when the running phase ends.
 *
 * The OS delivers it even if the app is in the background or has been killed, so the
 * timer works without a foreground service (Android) or background mode (iOS).
 * Implemented with AlarmManager on Android and UNUserNotificationCenter on iOS.
 */
interface PhaseNotifier {
    fun schedule(at: Instant, content: NotificationContent)
    fun cancel()
}

/** Text shown when [phase] ends. Separate from [PhaseNotifier] so the timer logic stays resource-free in tests. */
fun interface PhaseMessages {
    suspend fun forEndOf(phase: TimerPhase): NotificationContent
}

internal class ResourcePhaseMessages : PhaseMessages {
    override suspend fun forEndOf(phase: TimerPhase): NotificationContent {
        val focus = phase == TimerPhase.FOCUS
        return NotificationContent(
            title = getString(if (focus) Res.string.notification_focus_done_title else Res.string.notification_break_done_title),
            body = getString(if (focus) Res.string.notification_focus_done_body else Res.string.notification_break_done_body),
            channelName = getString(Res.string.notification_channel_name),
        )
    }
}

/**
 * Returns a callback that asks for notification permission (Android 13+ runtime permission,
 * iOS authorization). Safe to call repeatedly; the system only prompts once.
 */
@Composable
expect fun rememberNotificationPermissionRequester(): () -> Unit
