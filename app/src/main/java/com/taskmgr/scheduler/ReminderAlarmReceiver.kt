package com.taskmgr.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.example.taskmgr.TaskMgrApplication
import com.taskmgr.data.local.toDomain
import com.taskmgr.data.local.toEntity
import com.taskmgr.domain.logic.HabitCompletionCalculator
import com.taskmgr.domain.logic.ReminderStateMachine
import com.taskmgr.domain.model.ReminderItemType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
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
                when (itemType) {
                    ReminderItemType.TASK -> {
                        val task = container.database.taskDao().getById(itemId)?.toDomain()
                        // INV-01: Completed tasks never trigger alarms
                        if (task == null || task.isCompleted || !task.hasDate) {
                            container.reminderScheduler.cancelTaskReminders(itemId)
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
        const val EXTRA_ITEM_TYPE = "extra_item_type"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_EPOCH_DAY = "extra_epoch_day"
        const val EXTRA_IS_ADVANCE_REMINDER = "extra_is_advance_reminder"
    }
}
