package com.taskmgr.domain.contract

import com.taskmgr.domain.model.FullScreenReminderPayload
import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.HabitGroup
import com.taskmgr.domain.model.HabitLog
import com.taskmgr.domain.model.Moment
import com.taskmgr.domain.model.ReminderItemType
import com.taskmgr.domain.model.SnoozeOption
import com.taskmgr.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface MomentRepository {
    fun observeMoments(): Flow<List<Moment>>
    suspend fun upsert(moment: Moment): Long
    suspend fun delete(momentId: Long)
}

interface HabitGroupRepository {
    fun observeGroups(): Flow<List<HabitGroup>>
    suspend fun upsert(group: HabitGroup): Long
    suspend fun delete(groupId: Long)
}

interface TaskRepository {
    fun observeTasksForDay(epochDay: Long): Flow<List<Task>>
    fun observeAllTasks(): Flow<List<Task>>
    suspend fun getAllTasks(): List<Task>
    suspend fun getById(taskId: Long): Task?
    suspend fun upsert(task: Task): Long
    suspend fun setCompleted(taskId: Long, completed: Boolean, completedAtEpochMillis: Long?)
    suspend fun snooze(taskId: Long, option: SnoozeOption, nowEpochMillis: Long)
    suspend fun delete(taskId: Long)
}

interface HabitRepository {
    fun observeHabits(): Flow<List<Habit>>
    fun observeAllHabitLogs(): Flow<List<HabitLog>>
    fun observeHabitLogsForDay(epochDay: Long): Flow<List<HabitLog>>
    fun observeHabitLogsForRange(startEpochDay: Long, endEpochDay: Long): Flow<List<HabitLog>>
    suspend fun getHabitById(habitId: Long): Habit?
    suspend fun getLog(habitId: Long, epochDay: Long): HabitLog?
    suspend fun upsertHabit(habit: Habit): Long
    suspend fun incrementHabit(habitId: Long, epochDay: Long, nowEpochMillis: Long): HabitLog
    suspend fun updateHabitLogManual(
        habitId: Long,
        epochDay: Long,
        accumulatedAmount: Double,
        dismissedForDay: Boolean,
        nowEpochMillis: Long
    ): HabitLog
    suspend fun snoozeHabit(habitId: Long, epochDay: Long, option: SnoozeOption, nowEpochMillis: Long): HabitLog
    suspend fun deleteHabit(habitId: Long)
}

interface ReminderScheduler {
    suspend fun scheduleNextForTask(taskId: Long)
    suspend fun scheduleNextForHabit(habitId: Long, epochDay: Long)
    suspend fun cancelTaskReminders(taskId: Long)
    suspend fun cancelHabitRemindersForDay(habitId: Long, epochDay: Long)
    suspend fun reconcileAll(nowEpochMillis: Long)
}

interface FullScreenAlertDispatcher {
    fun canUseFullScreenIntent(): Boolean
    fun triggerFullScreenReminder(payload: FullScreenReminderPayload)
    fun dismissReminder(itemType: ReminderItemType, itemId: Long)
}
