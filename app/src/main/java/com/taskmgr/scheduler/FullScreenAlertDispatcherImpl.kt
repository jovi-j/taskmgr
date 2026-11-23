package com.taskmgr.scheduler

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jovij.taskmgr.R
import com.taskmgr.domain.contract.FullScreenAlertDispatcher
import com.taskmgr.domain.logic.AutoSnoozePolicy
import com.taskmgr.domain.model.FullScreenReminderPayload
import com.taskmgr.domain.model.ReminderItemType
import com.taskmgr.ui.fullscreen.FullScreenReminderActivity

class FullScreenAlertDispatcherImpl(
    private val context: Context,
    private val vibrationController: VibrationController
) : FullScreenAlertDispatcher {

    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val alarmAudioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val fullScreenChannel = NotificationChannel(
                CHANNEL_FULL_SCREEN_ALARM,
                "Alarmes em Tela Cheia",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Lembretes críticos em tela cheia sobre a tela de bloqueio"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
                enableVibration(false)
                setSound(alarmSound, alarmAudioAttrs)
            }

            val standardChannel = NotificationChannel(
                CHANNEL_STANDARD_REMINDER,
                "Lembretes Padrão",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações regulares de tarefas e hábitos"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(fullScreenChannel)
            notificationManager.createNotificationChannel(standardChannel)
        }
    }

    override fun canUseFullScreenIntent(): Boolean =
        PermissionHelper.canUseFullScreenIntent(context)

    override fun triggerFullScreenReminder(payload: FullScreenReminderPayload) {
        triggerReminder(payload = payload, aggressiveFullScreen = true)
    }

    fun triggerStandardReminder(payload: FullScreenReminderPayload) {
        triggerReminder(payload = payload, aggressiveFullScreen = false)
    }

    @SuppressLint("MissingPermission")
    private fun triggerReminder(
        payload: FullScreenReminderPayload,
        aggressiveFullScreen: Boolean
    ) {
        ensureNotificationChannels()

        val activityIntent = FullScreenReminderActivity.createIntent(context, payload)
        val requestCode = notificationIdFor(payload.itemType, payload.itemId)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (payload.vibrationEnabled) {
            vibrationController.vibrateReminder()
        }

        val channelId = if (aggressiveFullScreen) CHANNEL_FULL_SCREEN_ALARM else CHANNEL_STANDARD_REMINDER
        val typeLabel = if (payload.itemType == ReminderItemType.HABIT) "Hábito" else "Tarefa"
        val bodyText = listOfNotNull(
            payload.momentName?.let { "Momento: $it" },
            payload.progressText
        ).joinToString(" • ").ifBlank { typeLabel }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_taskmgr_notification)
            .setContentTitle("$typeLabel: ${payload.title}")
            .setContentText(bodyText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(if (aggressiveFullScreen) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .setTimeoutAfter(AutoSnoozePolicy.RING_TIMEOUT_MILLIS)

        if (aggressiveFullScreen) {
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        if (PermissionHelper.hasPostNotifications(context)) {
            try {
                NotificationManagerCompat.from(context).notify(requestCode, builder.build())
            } catch (_: SecurityException) {
                // Permission revoked mid-flight
            }
        }

        // Schedule exact 3-minute timeout alarm so that even if FullScreenReminderActivity is not
        // visible (e.g. standard notification or background launch blocked), vibration stops and
        // exponential auto-snooze (5m, 10m, 20m...) is applied after 3 minutes of no interaction.
        scheduleRingTimeoutAlarm(payload)

        // Launch full-screen activity directly in foreground even while user is using the phone
        // (requires SYSTEM_ALERT_WINDOW on Android 10+ / MIUI background pop-up permission on Xiaomi).
        if (aggressiveFullScreen) {
            runCatching {
                context.startActivity(activityIntent)
            }
        }
    }

    private fun scheduleRingTimeoutAlarm(payload: FullScreenReminderPayload) {
        val timeoutIntent = buildRingTimeoutPendingIntent(
            itemType = payload.itemType,
            itemId = payload.itemId,
            epochDay = payload.epochDay,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        val timeoutAtMillis = System.currentTimeMillis() + AutoSnoozePolicy.RING_TIMEOUT_MILLIS

        if (PermissionHelper.canScheduleExactAlarms(context)) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeoutAtMillis,
                timeoutIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeoutAtMillis,
                timeoutIntent
            )
        }
    }

    fun cancelRingTimeoutAlarm(itemType: ReminderItemType, itemId: Long) {
        val pending = buildRingTimeoutPendingIntent(
            itemType = itemType,
            itemId = itemId,
            epochDay = 0L,
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) {
            alarmManager.cancel(pending)
            pending.cancel()
        }
    }

    private fun buildRingTimeoutPendingIntent(
        itemType: ReminderItemType,
        itemId: Long,
        epochDay: Long,
        flags: Int
    ): PendingIntent? {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_RING_TIMEOUT
            putExtra(ReminderAlarmReceiver.EXTRA_ITEM_TYPE, itemType.name)
            putExtra(ReminderAlarmReceiver.EXTRA_ITEM_ID, itemId)
            putExtra(ReminderAlarmReceiver.EXTRA_EPOCH_DAY, epochDay)
        }
        return PendingIntent.getBroadcast(
            context,
            timeoutRequestCodeFor(itemType, itemId),
            intent,
            flags
        )
    }

    fun cancelStatusBarNotification(itemType: ReminderItemType, itemId: Long) {
        cancelRingTimeoutAlarm(itemType, itemId)
        notificationManager.cancel(notificationIdFor(itemType, itemId))
    }

    override fun dismissReminder(itemType: ReminderItemType, itemId: Long) {
        vibrationController.cancel()
        cancelStatusBarNotification(itemType, itemId)
        val closeIntent = Intent(ACTION_DISMISS_REMINDER).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_ITEM_TYPE, itemType.name)
            putExtra(EXTRA_ITEM_ID, itemId)
        }
        context.sendBroadcast(closeIntent)
    }

    companion object {
        const val CHANNEL_FULL_SCREEN_ALARM = "taskmgr_full_screen_alarms"
        const val CHANNEL_STANDARD_REMINDER = "taskmgr_standard_reminders"
        const val ACTION_DISMISS_REMINDER = "com.taskmgr.ACTION_DISMISS_REMINDER"
        const val EXTRA_ITEM_TYPE = "extra_item_type"
        const val EXTRA_ITEM_ID = "extra_item_id"

        fun notificationIdFor(itemType: ReminderItemType, itemId: Long): Int {
            val prefix = if (itemType == ReminderItemType.TASK) 100_000 else 200_000
            return prefix + (itemId.toInt() and 0xFFFF)
        }

        private fun timeoutRequestCodeFor(itemType: ReminderItemType, itemId: Long): Int {
            val prefix = if (itemType == ReminderItemType.TASK) 300_000 else 400_000
            return prefix + (itemId.toInt() and 0xFFFF)
        }
    }
}
