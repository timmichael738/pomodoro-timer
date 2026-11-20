package com.kotlinapps.pomodoro.core.notification

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.kotlinapps.pomodoro.shared.R
import kotlin.time.Instant

/**
 * Android implementation: an AlarmManager alarm fires [PhaseAlarmReceiver] at the phase end,
 * which posts the notification. Works while the app is backgrounded or killed, with no foreground service.
 *
 * Exact alarms are used when allowed (USE_EXACT_ALARM is granted to timer apps on Android 13+);
 * otherwise an inexact, Doze-aware alarm is used, which may arrive slightly late.
 */
internal class AlarmPhaseNotifier(private val context: Context) : PhaseNotifier {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(at: Instant, content: NotificationContent) {
        val intent = PhaseAlarmReceiver.intent(context, content)
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val triggerAt = at.toEpochMilliseconds()
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    override fun cancel() {
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, PhaseAlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pending != null) {
            alarmManager.cancel(pending)
            pending.cancel()
        }
    }

    private companion object {
        const val REQUEST_CODE = 1
    }
}

/** Posts the "phase finished" notification. Declared in the app manifest. */
class PhaseAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        val manager = context.getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return // The user declined notifications; the in-app timer still advances.
        }

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: title,
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )

        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.let { PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE) }

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(Notification.CATEGORY_ALARM)
            .setAutoCancel(true)
            .apply { if (launch != null) setContentIntent(launch) }
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    internal companion object {
        private const val CHANNEL_ID = "timer_alerts"
        private const val NOTIFICATION_ID = 1
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_BODY = "body"
        private const val EXTRA_CHANNEL_NAME = "channel_name"

        fun intent(context: Context, content: NotificationContent): Intent =
            Intent(context, PhaseAlarmReceiver::class.java)
                .putExtra(EXTRA_TITLE, content.title)
                .putExtra(EXTRA_BODY, content.body)
                .putExtra(EXTRA_CHANNEL_NAME, content.channelName)
    }
}
