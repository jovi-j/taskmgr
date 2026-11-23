package com.taskmgr.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.jovij.taskmgr.TaskMgrApplication
import com.taskmgr.data.local.toDomain
import com.taskmgr.data.local.toEntity
import com.taskmgr.di.AppContainer
import com.taskmgr.domain.logic.AutoSnoozeDecision
import com.taskmgr.domain.logic.AutoSnoozePolicy
import com.taskmgr.domain.logic.HabitCompletionCalculator
import com.taskmgr.domain.logic.ReminderStateMachine
import com.taskmgr.domain.model.HabitLog
import com.taskmgr.domain.model.ReminderItemType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: ACTION_TRIGGER_REMINDER
        val itemTypeRaw = intent.getStringExtra(EXTRA_ITEM_TYPE) ?: return
        val itemType = runCatching { ReminderItemType.valueOf(itemTypeRaw) }.getOrNull() ?: return
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
        if (itemId <= 0L) return
        val epochDay = intent.getLongExtra(EXTRA_EPOCH_DAY, LocalDate.now().toEpochDay())
        val isAdvanceReminder = intent.getBooleanExtra(EXTRA_IS_ADVANCE_REMINDER, false)

        val app = context.applicationContext as? TaskMgrApplication ?: return
        val container = app.container
        val pendingResult = goAsync()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "taskmgr:ReminderAlarmWakeLock"
        )?.apply {
            setReferenceCounted(false)
            acquire(10_000L)
        }

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val nowMillis = System.currentTimeMillis()
                if (action == ACTION_RING_TIMEOUT) {
                    handleUnansweredRingTimeout(
                        container = container,
                        itemType = itemType,
                        itemId = itemId,
                        epochDay = epochDay,
                        nowMillis = nowMillis,
                        zoneId = ZoneId.systemDefault()
                    )
                    return@launch
                }

                val todayEpochDay = Instant.ofEpochMilli(nowMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .toEpochDay()

                when (itemType) {
                    ReminderItemType.TASK -> {
                        val task = container.database.taskDao().getById(itemId)?.toDomain()
                        // INV-01: Completed tasks or tasks auto-dismissed for the day never trigger alarms
                        if (task == null || task.isCompleted || !task.hasDate) {
                            container.reminderScheduler.cancelTaskReminders(itemId)
                            return@launch
                        }
                        if (container.autoSnoozeStore.isTaskDismissedForDay(itemId, todayEpochDay)) {
                            container.reminderScheduler.scheduleNextForTask(itemId)
                            return@launch
                        }
                        if (task.snoozedUntilEpochMillis != null && task.snoozedUntilEpochMillis <= nowMillis) {
                            container.database.taskDao().setSnoozedUntil(itemId, null)
                        }
                        val moment = task.momentId?.let {
                            container.database.momentDao().getById(it)?.toDomain()
                        }
                        val payload = HabitCompletionCalculator.buildTaskPayload(
                            task = task,
                            epochDay = epochDay,
                            momentName = moment?.name,
                            isAdvanceReminder = isAdvanceReminder
                        )
                        val aggressive = task.aggressiveFullScreen && (moment?.aggressiveFullScreen ?: true)
                        if (aggressive) {
                            container.alertDispatcher.triggerFullScreenReminder(payload)
                        } else {
                            container.alertDispatcher.triggerStandardReminder(payload)
                        }
                        container.reminderScheduler.scheduleNextForTask(itemId)
                    }

                    ReminderItemType.HABIT -> {
                        val habit = container.database.habitDao().getById(itemId)?.toDomain()
                        if (habit == null || !habit.isActive) {
                            container.reminderScheduler.cancelHabitRemindersForDay(itemId, epochDay)
                            return@launch
                        }
                        var log = container.database.habitLogDao().getLog(itemId, epochDay)?.toDomain()
                        // INV-01 & INV-02: Completed or dismissedForDay on epochDay must never trigger
                        if (log != null && (log.isCompleted || log.dismissedForDay)) {
                            container.reminderScheduler.scheduleNextForHabit(itemId, epochDay)
                            return@launch
                        }
                        if (log != null && log.snoozedUntilEpochMillis != null && log.snoozedUntilEpochMillis <= nowMillis) {
                            log = ReminderStateMachine.reduceHabitLogOnSnoozeTimerFired(habit, log, nowMillis)
                            container.database.habitLogDao().upsert(log.toEntity())
                        }
                        val moment = habit.momentId?.let {
                            container.database.momentDao().getById(it)?.toDomain()
                        }
                        val payload = HabitCompletionCalculator.buildHabitPayload(
                            habit = habit,
                            log = log,
                            epochDay = epochDay,
                            momentName = moment?.name
                        )
                        val aggressive = habit.aggressiveFullScreen && (moment?.aggressiveFullScreen ?: true)
                        if (aggressive) {
                            container.alertDispatcher.triggerFullScreenReminder(payload)
                        } else {
                            container.alertDispatcher.triggerStandardReminder(payload)
                        }
                        container.reminderScheduler.scheduleNextForHabit(itemId, epochDay)
                    }
                }
            } finally {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.taskmgr.ACTION_TRIGGER_REMINDER"
        const val ACTION_RING_TIMEOUT = "com.taskmgr.ACTION_RING_TIMEOUT"
        const val EXTRA_ITEM_TYPE = "extra_item_type"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_EPOCH_DAY = "extra_epoch_day"
        const val EXTRA_IS_ADVANCE_REMINDER = "extra_is_advance_reminder"

        /**
         * Invoked when an alarm/notification has rung for 3 minutes ([AutoSnoozePolicy.RING_TIMEOUT_MILLIS])
         * without any user interaction (neither closing, nor snoozing, nor confirming "Sim"):
         * 1. Stops vibration, cancels notification, and closes [com.taskmgr.ui.fullscreen.FullScreenReminderActivity]
         *    so the screen turns off.
         * 2. Applies doubling auto-snooze: 5m -> 10m -> 20m -> 40m -> ...
         * 3. Once the auto-snooze duration reaches the next calendar day, stops placing any further snooze
         *    and silences notifications for that task/habit for the remainder of the current day.
         */
        suspend fun handleUnansweredRingTimeout(
            container: AppContainer,
            itemType: ReminderItemType,
            itemId: Long,
            epochDay: Long,
            nowMillis: Long = System.currentTimeMillis(),
            zoneId: ZoneId = ZoneId.systemDefault()
        ) {
            container.alertDispatcher.dismissReminder(itemType, itemId)

            val currentDay = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate().toEpochDay()
            val previousCount = container.autoSnoozeStore.getConsecutiveCount(itemType, itemId, currentDay)

            when (itemType) {
                ReminderItemType.TASK -> {
                    val task = container.database.taskDao().getById(itemId)?.toDomain() ?: return
                    if (task.isCompleted || !task.hasDate) return
                    val moment = task.momentId?.let { container.database.momentDao().getById(it)?.toDomain() }
                    val decision = AutoSnoozePolicy.evaluateTimeout(
                        previousAutoSnoozeCount = previousCount,
                        nowEpochMillis = nowMillis,
                        zoneId = zoneId,
                        moment = moment
                    )

                    when (decision) {
                        is AutoSnoozeDecision.SnoozeForMinutes -> {
                            container.autoSnoozeStore.setConsecutiveCount(
                                itemType = ReminderItemType.TASK,
                                itemId = itemId,
                                epochDay = currentDay,
                                count = decision.nextCount
                            )
                            container.database.taskDao().setSnoozedUntil(itemId, decision.triggerAtEpochMillis)
                            container.reminderScheduler.scheduleNextForTask(itemId)
                        }

                        is AutoSnoozeDecision.ReachedNextDayStopNotifying -> {
                            container.autoSnoozeStore.setTaskDismissedForDay(itemId, currentDay)
                            container.database.taskDao().setSnoozedUntil(itemId, null)
                            container.reminderScheduler.scheduleNextForTask(itemId)
                        }
                    }
                }

                ReminderItemType.HABIT -> {
                    val habit = container.database.habitDao().getById(itemId)?.toDomain() ?: return
                    if (!habit.isActive) return
                    val existing = container.database.habitLogDao().getLog(itemId, currentDay)?.toDomain()
                    if (existing?.isCompleted == true || existing?.dismissedForDay == true) return
                    val currentAmount = existing?.accumulatedAmount ?: 0.0
                    val moment = habit.momentId?.let { container.database.momentDao().getById(it)?.toDomain() }
                    val decision = AutoSnoozePolicy.evaluateTimeout(
                        previousAutoSnoozeCount = previousCount,
                        nowEpochMillis = nowMillis,
                        zoneId = zoneId,
                        moment = moment
                    )

                    when (decision) {
                        is AutoSnoozeDecision.SnoozeForMinutes -> {
                            container.autoSnoozeStore.setConsecutiveCount(
                                itemType = ReminderItemType.HABIT,
                                itemId = itemId,
                                epochDay = currentDay,
                                count = decision.nextCount
                            )
                            val updatedLog = HabitLog(
                                habitId = itemId,
                                epochDay = currentDay,
                                accumulatedAmount = currentAmount,
                                isCompleted = false,
                                dismissedForDay = false,
                                snoozedUntilEpochMillis = decision.triggerAtEpochMillis,
                                updatedAtEpochMillis = nowMillis
                            )
                            container.database.habitLogDao().upsert(updatedLog.toEntity())
                            container.reminderScheduler.scheduleNextForHabit(itemId, currentDay)
                        }

                        is AutoSnoozeDecision.ReachedNextDayStopNotifying -> {
                            container.autoSnoozeStore.resetUserInteracted(ReminderItemType.HABIT, itemId)
                            val dismissedLog = HabitLog(
                                habitId = itemId,
                                epochDay = currentDay,
                                accumulatedAmount = currentAmount,
                                isCompleted = false,
                                dismissedForDay = true,
                                snoozedUntilEpochMillis = null,
                                updatedAtEpochMillis = nowMillis
                            )
                            container.database.habitLogDao().upsert(dismissedLog.toEntity())
                            container.reminderScheduler.cancelHabitRemindersForDay(itemId, currentDay)
                            container.reminderScheduler.scheduleNextForHabit(itemId, currentDay)
                        }
                    }
                }
            }
        }
    }
}
