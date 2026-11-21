package com.taskmgr.di

import android.content.Context
import com.taskmgr.data.local.TaskMgrDatabase
import com.taskmgr.data.repository.HabitGroupRepositoryImpl
import com.taskmgr.data.repository.HabitRepositoryImpl
import com.taskmgr.data.repository.MomentRepositoryImpl
import com.taskmgr.data.repository.TaskRepositoryImpl
import com.taskmgr.data.sync.AndroidCalendarSyncManager
import com.taskmgr.scheduler.FullScreenAlertDispatcherImpl
import com.taskmgr.scheduler.ReminderSchedulerImpl
import com.taskmgr.scheduler.VibrationController

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: TaskMgrDatabase by lazy {
        TaskMgrDatabase.getInstance(appContext)
    }

    val vibrationController: VibrationController by lazy {
        VibrationController(appContext)
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
            habitLogDao = database.habitLogDao()
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
            habitGroupDao = database.habitGroupDao()
        )
    }

    val taskRepository: TaskRepositoryImpl by lazy {
        TaskRepositoryImpl(
            taskDao = database.taskDao(),
            momentDao = database.momentDao(),
            schedulerProvider = { reminderScheduler },
            alertDispatcherProvider = { alertDispatcher }
        )
    }

    val habitRepository: HabitRepositoryImpl by lazy {
        HabitRepositoryImpl(
            habitDao = database.habitDao(),
            habitLogDao = database.habitLogDao(),
            schedulerProvider = { reminderScheduler },
            alertDispatcherProvider = { alertDispatcher }
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
