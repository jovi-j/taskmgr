package com.taskmgr.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.taskmgr.MainActivity
import com.taskmgr.data.local.HabitDao
import com.taskmgr.data.local.HabitLogDao
import com.taskmgr.data.local.MomentDao
import com.taskmgr.data.local.TaskDao
import com.taskmgr.data.local.toDomain
import com.taskmgr.domain.contract.ReminderScheduler
import com.taskmgr.domain.logic.NextAlarmCalculator
import com.taskmgr.domain.model.ReminderItemType
import java.time.Instant
import java.time.ZoneId

class ReminderSchedulerImpl(
    private val context: Context,
    private val momentDao: MomentDao,
    private val taskDao: TaskDao,
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val zoneIdProvider: () -> ZoneId = { ZoneId.systemDefault() }
) : ReminderScheduler {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override suspend fun scheduleNextForTask(taskId: Long) {
        val task = taskDao.getById(taskId)?.toDomain()
        if (task == null || task.isCompleted) {
            cancelTaskReminders(taskId)
            return
        }
        val moment = task.momentId?.let { momentDao.getById(it)?.toDomain() }
        val spec = NextAlarmCalculator.calculateNextForTask(
            task = task,
            moment = moment,
            nowEpochMillis = System.currentTimeMillis(),
            zoneId = zoneIdProvider()
        )
        if (spec == null) {
            cancelTaskReminders(taskId)
            return
        }
        scheduleExactAlarm(
            itemType = ReminderItemType.TASK,
            itemId = task.id,
            epochDay = spec.targetEpochDay,
            triggerAtMillis = spec.triggerAtEpochMillis
        )
    }

    override suspend fun scheduleNextForHabit(habitId: Long, epochDay: Long) {
        val habit = habitDao.getById(habitId)?.toDomain()
        if (habit == null || !habit.isActive) {
            cancelHabitRemindersForDay(habitId, epochDay)
            return
        }
        val nowMillis = System.currentTimeMillis()
        val zoneId = zoneIdProvider()
        val todayEpochDay = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate().toEpochDay()
        val effectiveDay = maxOf(epochDay, todayEpochDay)
        val log = habitLogDao.getLog(habitId, effectiveDay)?.toDomain()
        val moment = habit.momentId?.let { momentDao.getById(it)?.toDomain() }

        val spec = NextAlarmCalculator.calculateNextForHabit(
            habit = habit,
            logForCurrentDay = log,
            currentEpochDay = effectiveDay,
            moment = moment,
            nowEpochMillis = nowMillis,
            zoneId = zoneId
        )
        if (spec == null) {
            cancelHabitRemindersForDay(habitId, effectiveDay)
            return
        }
        scheduleExactAlarm(
            itemType = ReminderItemType.HABIT,
            itemId = habit.id,
            epochDay = spec.targetEpochDay,
            triggerAtMillis = spec.triggerAtEpochMillis
        )
    }

    override suspend fun cancelTaskReminders(taskId: Long) {
        val pendingIntent = buildAlarmPendingIntent(
            itemType = ReminderItemType.TASK,
            itemId = taskId,
            epochDay = 0L,
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    override suspend fun cancelHabitRemindersForDay(habitId: Long, epochDay: Long) {
        val pendingIntent = buildAlarmPendingIntent(
            itemType = ReminderItemType.HABIT,
            itemId = habitId,
            epochDay = epochDay,
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    override suspend fun reconcileAll(nowEpochMillis: Long) {
        val zoneId = zoneIdProvider()
        val todayEpochDay = Instant.ofEpochMilli(nowEpochMillis).atZone(zoneId).toLocalDate().toEpochDay()

        val pendingTasks = taskDao.getAllPending()
        for (task in pendingTasks) {
            scheduleNextForTask(task.id)
        }

        val activeHabits = habitDao.getAllActive()
        for (habit in activeHabits) {
            scheduleNextForHabit(habit.id, todayEpochDay)
        }
    }

    private fun scheduleExactAlarm(
        itemType: ReminderItemType,
        itemId: Long,
        epochDay: Long,
        triggerAtMillis: Long
    ) {
        val alarmIntent = buildAlarmPendingIntent(
            itemType = itemType,
            itemId = itemId,
            epochDay = epochDay,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ) ?: return

        val showAppIntent = PendingIntent.getActivity(
            context,
            requestCodeFor(itemType, itemId),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (PermissionHelper.canScheduleExactAlarms(context)) {
            val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showAppIntent)
            alarmManager.setAlarmClock(clockInfo, alarmIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                alarmIntent
            )
        }
    }

    private fun buildAlarmPendingIntent(
        itemType: ReminderItemType,
        itemId: Long,
        epochDay: Long,
        flags: Int
    ): PendingIntent? {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderAlarmReceiver.EXTRA_ITEM_TYPE, itemType.name)
            putExtra(ReminderAlarmReceiver.EXTRA_ITEM_ID, itemId)
            putExtra(ReminderAlarmReceiver.EXTRA_EPOCH_DAY, epochDay)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(itemType, itemId),
            intent,
            flags
        )
    }

    companion object {
        private fun requestCodeFor(itemType: ReminderItemType, itemId: Long): Int {
            val base = if (itemType == ReminderItemType.TASK) 1_000_000 else 2_000_000
            return base + (itemId.toInt() and 0x7FFFF)
        }
    }
}
