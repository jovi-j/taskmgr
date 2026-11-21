package com.jovij.taskmgr

import android.app.Application
import com.taskmgr.di.AppContainer
import com.taskmgr.scheduler.ReconcileAlarmsWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TaskMgrApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container.alertDispatcher.ensureNotificationChannels()
        ReconcileAlarmsWorker.schedulePeriodic(this)
        appScope.launch {
            container.reminderScheduler.reconcileAll(System.currentTimeMillis())
        }
    }
}
