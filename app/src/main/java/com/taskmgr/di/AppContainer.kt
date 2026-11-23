package com.taskmgr.di

import android.content.Context
import com.taskmgr.data.local.TaskMgrDatabase
import com.taskmgr.data.repository.HabitGroupRepositoryImpl
import com.taskmgr.data.repository.HabitRepositoryImpl
import com.taskmgr.data.repository.MomentRepositoryImpl
import com.taskmgr.data.repository.TaskRepositoryImpl
import com.taskmgr.data.sync.AndroidCalendarSyncManager
import com.taskmgr.scheduler.AutoSnoozeStore
import com.taskmgr.scheduler.FullScreenAlertDispatcherImpl
import com.taskmgr.scheduler.ReminderSchedulerImpl
import com.taskmgr.scheduler.VibrationController
import com.taskmgr.widget.CalendarWidgetUpdater
import com.taskmgr.widget.HabitWidgetUpdater

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: TaskMgrDatabase by lazy {
        TaskMgrDatabase.getInstance(appContext)
    }

    val vibrationController: VibrationController by lazy {
        VibrationController(appContext)
    }

    val autoSnoozeStore: AutoSnoozeStore by lazy {
        AutoSnoozeStore(appContext)
    }

    val alertDispatcher: FullScreenAlertDispatcherImpl by lazy {
        FullScreenAlertDispatcherImpl(appContext, vibrationController)
    }

    val reminderScheduler: ReminderSchedulerImpl by lazy {
        ReminderSchedulerImpl(
            context = appContext,
            momentDao = database.momentDao(),
            taskDao = database.taskDao(),
            habitDao = database.habitDao(),
            habitLogDao = database.habitLogDao(),
            autoSnoozeStore = autoSnoozeStore
        )
    }

    val momentRepository: MomentRepositoryImpl by lazy {
        MomentRepositoryImpl(
            momentDao = database.momentDao(),
            schedulerProvider = { reminderScheduler }
        )
    }

    val habitGroupRepository: HabitGroupRepositoryImpl by lazy {
        HabitGroupRepositoryImpl(
            habitGroupDao = database.habitGroupDao(),
            onHabitGroupsChanged = { HabitWidgetUpdater.requestUpdateAll(appContext) }
        )
    }

    val taskRepository: TaskRepositoryImpl by lazy {
        TaskRepositoryImpl(
            taskDao = database.taskDao(),
            momentDao = database.momentDao(),
            schedulerProvider = { reminderScheduler },
            alertDispatcherProvider = { alertDispatcher },
            onTasksChanged = { CalendarWidgetUpdater.requestUpdateAll(appContext) },
            onUserInteracted = { taskId ->
                autoSnoozeStore.resetUserInteracted(com.taskmgr.domain.model.ReminderItemType.TASK, taskId)
            }
        )
    }

    val habitRepository: HabitRepositoryImpl by lazy {
        HabitRepositoryImpl(
            habitDao = database.habitDao(),
            habitLogDao = database.habitLogDao(),
            schedulerProvider = { reminderScheduler },
            alertDispatcherProvider = { alertDispatcher },
            onHabitsChanged = { HabitWidgetUpdater.requestUpdateAll(appContext) },
            onUserInteracted = { habitId ->
                autoSnoozeStore.resetUserInteracted(com.taskmgr.domain.model.ReminderItemType.HABIT, habitId)
            }
        )
    }

    val calendarSyncManager: AndroidCalendarSyncManager by lazy {
        AndroidCalendarSyncManager(
            context = appContext,
            taskDao = database.taskDao(),
            momentDao = database.momentDao(),
            calendarSyncDao = database.calendarSyncDao()
        )
    }
}
