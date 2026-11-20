package com.kotlinapps.pomodoro.core.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.time.Clock
import kotlin.time.Instant
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

/** iOS implementation: a one-shot local notification scheduled with UNUserNotificationCenter. */
internal class LocalPhaseNotifier : PhaseNotifier {

    private val center get() = UNUserNotificationCenter.currentNotificationCenter()

    override fun schedule(at: Instant, content: NotificationContent) {
        // The trigger interval must be strictly positive.
        val seconds = (at - Clock.System.now()).inWholeMilliseconds / 1000.0
        if (seconds <= 0.0) return

        val notificationContent = UNMutableNotificationContent()
        notificationContent.setTitle(content.title)
        notificationContent.setBody(content.body)
        notificationContent.setSound(UNNotificationSound.defaultSound)

        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(seconds, false)
        val request = UNNotificationRequest.requestWithIdentifier(REQUEST_ID, notificationContent, trigger)
        // Re-using the identifier replaces any previously scheduled alert.
        center.addNotificationRequest(request, null)
    }

    override fun cancel() {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(REQUEST_ID))
    }

    private companion object {
        const val REQUEST_ID = "pomodoro.phase_end"
    }
}

@Composable
actual fun rememberNotificationPermissionRequester(): () -> Unit = remember {
    {
        // Prompts only the first time; afterwards this returns the stored decision immediately.
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
        ) { _, _ -> }
    }
}
