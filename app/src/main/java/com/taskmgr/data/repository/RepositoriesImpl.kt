package com.taskmgr.data.repository

import com.taskmgr.data.local.HabitDao
import com.taskmgr.data.local.HabitGroupDao
import com.taskmgr.data.local.HabitLogDao
import com.taskmgr.data.local.MomentDao
import com.taskmgr.data.local.TaskDao
import com.taskmgr.data.local.toDomain
import com.taskmgr.data.local.toEntity
import com.taskmgr.domain.contract.FullScreenAlertDispatcher
import com.taskmgr.domain.contract.HabitGroupRepository
import com.taskmgr.domain.contract.HabitRepository
import com.taskmgr.domain.contract.MomentRepository
import com.taskmgr.domain.contract.ReminderScheduler
import com.taskmgr.domain.contract.TaskRepository
import com.taskmgr.domain.logic.ReminderStateMachine
import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.HabitGroup
import com.taskmgr.domain.model.HabitLog
import com.taskmgr.domain.model.Moment
import com.taskmgr.domain.model.ReminderItemType
import com.taskmgr.domain.model.SnoozeOption
import com.taskmgr.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.ZoneId

class HabitGroupRepositoryImpl(
    private val habitGroupDao: HabitGroupDao,
    private val onHabitGroupsChanged: () -> Unit = {}
) : HabitGroupRepository {

    override fun observeGroups(): Flow<List<HabitGroup>> =
        habitGroupDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun upsert(group: HabitGroup): Long {
        val insertedId = habitGroupDao.upsert(group.toEntity())
        onHabitGroupsChanged()
        return if (group.id != 0L) group.id else insertedId
    }

    override suspend fun delete(groupId: Long) {
        habitGroupDao.deleteById(groupId)
        onHabitGroupsChanged()
    }
}

class MomentRepositoryImpl(
    private val momentDao: MomentDao,
    private val schedulerProvider: () -> ReminderScheduler
) : MomentRepository {

    override fun observeMoments(): Flow<List<Moment>> =
        momentDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun upsert(moment: Moment): Long {
        val insertedId = momentDao.upsert(moment.toEntity())
        val finalId = if (moment.id != 0L) moment.id else insertedId
        schedulerProvider().reconcileAll(System.currentTimeMillis())
        return finalId
    }

    override suspend fun delete(momentId: Long) {
        momentDao.deleteById(momentId)
        schedulerProvider().reconcileAll(System.currentTimeMillis())
    }
}

class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val momentDao: MomentDao,
    private val schedulerProvider: () -> ReminderScheduler,
    private val alertDispatcherProvider: () -> FullScreenAlertDispatcher,
    private val zoneIdProvider: () -> ZoneId = { ZoneId.systemDefault() },
    private val onTasksChanged: () -> Unit = {},
    private val onUserInteracted: (Long) -> Unit = {}
) : TaskRepository {

    override fun observeTasksForDay(epochDay: Long): Flow<List<Task>> =
        taskDao.observeForDay(epochDay).map { list ->
            val zone = zoneIdProvider()
            list.map { it.toDomain() }.filter { it.isActiveOnDay(epochDay, zone) }
        }

    override fun observeAllTasks(): Flow<List<Task>> =
        taskDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllTasks(): List<Task> =
        taskDao.getAll().map { it.toDomain() }

    fun observeTasksForRange(startEpochDay: Long, endEpochDay: Long): Flow<List<Task>> =
        taskDao.observeForRange(startEpochDay, endEpochDay).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(taskId: Long): Task? =
        taskDao.getById(taskId)?.toDomain()

    override suspend fun upsert(task: Task): Long {
        val normalizedTask = if (!task.hasDate) {
            task.copy(
                startEpochDay = Task.UNSCHEDULED_EPOCH_DAY,
                endEpochDay = Task.UNSCHEDULED_EPOCH_DAY,
                hasEndDate = false
            )
        } else {
            require(task.endEpochDay >= task.startEpochDay) {
                "endEpochDay (${task.endEpochDay}) must be >= startEpochDay (${task.startEpochDay})"
            }
            task
        }
        val existing = if (normalizedTask.id != 0L) taskDao.getById(normalizedTask.id) else null
        val rowId = taskDao.upsert(normalizedTask.toEntity(completedAtEpochMillis = existing?.completedAtEpochMillis))
        val finalId = if (normalizedTask.id != 0L) normalizedTask.id else rowId
        onUserInteracted(finalId)
        if (normalizedTask.isCompleted || !normalizedTask.hasDate) {
            schedulerProvider().cancelTaskReminders(finalId)
            if (normalizedTask.isCompleted) {
                alertDispatcherProvider().dismissReminder(ReminderItemType.TASK, finalId)
            }
        } else {
            schedulerProvider().scheduleNextForTask(finalId)
        }
        onTasksChanged()
        return finalId
    }

    override suspend fun setCompleted(taskId: Long, completed: Boolean, completedAtEpochMillis: Long?) {
        onUserInteracted(taskId)
        taskDao.setCompleted(taskId, completed, if (completed) completedAtEpochMillis else null)
        if (completed) {
            schedulerProvider().cancelTaskReminders(taskId)
            alertDispatcherProvider().dismissReminder(ReminderItemType.TASK, taskId)
        } else {
            schedulerProvider().scheduleNextForTask(taskId)
        }
        onTasksChanged()
    }

    override suspend fun snooze(taskId: Long, option: SnoozeOption, nowEpochMillis: Long) {
        onUserInteracted(taskId)
        val current = taskDao.getById(taskId)?.toDomain() ?: return
        if (current.isCompleted) return
        val moment = current.momentId?.let { momentDao.getById(it)?.toDomain() }
        val updated = ReminderStateMachine.reduceTaskOnSnooze(
            task = current,
            option = option,
            nowEpochMillis = nowEpochMillis,
            zoneId = zoneIdProvider(),
            moment = moment
        )
        taskDao.setSnoozedUntil(taskId, updated.snoozedUntilEpochMillis)
        alertDispatcherProvider().dismissReminder(ReminderItemType.TASK, taskId)
        schedulerProvider().scheduleNextForTask(taskId)
        onTasksChanged()
    }

    override suspend fun delete(taskId: Long) {
        onUserInteracted(taskId)
        schedulerProvider().cancelTaskReminders(taskId)
        alertDispatcherProvider().dismissReminder(ReminderItemType.TASK, taskId)
        taskDao.deleteById(taskId)
        onTasksChanged()
    }
}

class HabitRepositoryImpl(
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val schedulerProvider: () -> ReminderScheduler,
    private val alertDispatcherProvider: () -> FullScreenAlertDispatcher,
    private val onHabitsChanged: () -> Unit = {},
    private val onUserInteracted: (Long) -> Unit = {}
) : HabitRepository {

    override fun observeHabits(): Flow<List<Habit>> =
        habitDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeAllHabitLogs(): Flow<List<HabitLog>> =
        habitLogDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeHabitLogsForDay(epochDay: Long): Flow<List<HabitLog>> =
        habitLogDao.observeForDay(epochDay).map { list -> list.map { it.toDomain() } }

    override fun observeHabitLogsForRange(startEpochDay: Long, endEpochDay: Long): Flow<List<HabitLog>> =
        habitLogDao.observeForRange(startEpochDay, endEpochDay).map { list -> list.map { it.toDomain() } }

    override suspend fun getHabitById(habitId: Long): Habit? =
        habitDao.getById(habitId)?.toDomain()

    override suspend fun getLog(habitId: Long, epochDay: Long): HabitLog? =
        habitLogDao.getLog(habitId, epochDay)?.toDomain()

    override suspend fun upsertHabit(habit: Habit): Long {
        val rowId = habitDao.upsert(habit.toEntity())
        val finalId = if (habit.id != 0L) habit.id else rowId
        onUserInteracted(finalId)
        val todayEpochDay = java.time.LocalDate.now().toEpochDay()
        if (habit.isActive) {
            schedulerProvider().scheduleNextForHabit(finalId, todayEpochDay)
        } else {
            schedulerProvider().cancelHabitRemindersForDay(finalId, todayEpochDay)
        }
        onHabitsChanged()
        return finalId
    }

    override suspend fun incrementHabit(habitId: Long, epochDay: Long, nowEpochMillis: Long): HabitLog {
        onUserInteracted(habitId)
        val habit = habitDao.getById(habitId)?.toDomain()
            ?: error("Habit $habitId not found")
        val existing = habitLogDao.getLog(habitId, epochDay)?.toDomain()
        val updated = ReminderStateMachine.reduceHabitLogOnIncrement(
            habit = habit,
            existing = existing,
            epochDay = epochDay,
            nowEpochMillis = nowEpochMillis
        )
        habitLogDao.upsert(updated.toEntity())
        if (updated.isCompleted) {
            schedulerProvider().cancelHabitRemindersForDay(habitId, epochDay)
            alertDispatcherProvider().dismissReminder(ReminderItemType.HABIT, habitId)
        }
        schedulerProvider().scheduleNextForHabit(habitId, epochDay)
        onHabitsChanged()
        return updated
    }

    override suspend fun updateHabitLogManual(
        habitId: Long,
        epochDay: Long,
        accumulatedAmount: Double,
        dismissedForDay: Boolean,
        nowEpochMillis: Long
    ): HabitLog {
        onUserInteracted(habitId)
        val habit = habitDao.getById(habitId)?.toDomain()
            ?: error("Habit $habitId not found")
        val updated = ReminderStateMachine.reduceHabitLogOnManualEdit(
            habit = habit,
            epochDay = epochDay,
            accumulatedAmount = accumulatedAmount,
            dismissedForDay = dismissedForDay,
            nowEpochMillis = nowEpochMillis
        )
        habitLogDao.upsert(updated.toEntity())
        if (updated.isCompleted || updated.dismissedForDay) {
            schedulerProvider().cancelHabitRemindersForDay(habitId, epochDay)
            alertDispatcherProvider().dismissReminder(ReminderItemType.HABIT, habitId)
        }
        schedulerProvider().scheduleNextForHabit(habitId, epochDay)
        onHabitsChanged()
        return updated
    }

    override suspend fun snoozeHabit(
        habitId: Long,
        epochDay: Long,
        option: SnoozeOption,
        nowEpochMillis: Long
    ): HabitLog {
        onUserInteracted(habitId)
        val habit = habitDao.getById(habitId)?.toDomain()
            ?: error("Habit $habitId not found")
        val existing = habitLogDao.getLog(habitId, epochDay)?.toDomain()
        val updated = ReminderStateMachine.reduceHabitLogOnSnooze(
            habit = habit,
            existing = existing,
            epochDay = epochDay,
            option = option,
            nowEpochMillis = nowEpochMillis
        )
        habitLogDao.upsert(updated.toEntity())
        alertDispatcherProvider().dismissReminder(ReminderItemType.HABIT, habitId)
        if (updated.dismissedForDay) {
            schedulerProvider().cancelHabitRemindersForDay(habitId, epochDay)
        }
        schedulerProvider().scheduleNextForHabit(habitId, epochDay)
        onHabitsChanged()
        return updated
    }

    override suspend fun deleteHabit(habitId: Long) {
        onUserInteracted(habitId)
        val todayEpochDay = java.time.LocalDate.now().toEpochDay()
        schedulerProvider().cancelHabitRemindersForDay(habitId, todayEpochDay)
        alertDispatcherProvider().dismissReminder(ReminderItemType.HABIT, habitId)
        habitDao.deleteById(habitId)
        onHabitsChanged()
    }
}
