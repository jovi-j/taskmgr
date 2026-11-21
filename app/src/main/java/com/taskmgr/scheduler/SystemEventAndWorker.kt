package com.taskmgr.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jovij.taskmgr.TaskMgrApplication
import java.util.concurrent.TimeUnit

class ReconcileAlarmsWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? TaskMgrApplication ?: return Result.failure()
        app.container.reminderScheduler.reconcileAll(System.currentTimeMillis())
        return Result.success()
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "taskmgr_periodic_reconcile"
        private const val IMMEDIATE_WORK_NAME = "taskmgr_immediate_reconcile"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReconcileAlarmsWorker>(
                12,
                TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun enqueueImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<ReconcileAlarmsWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}

class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            ReconcileAlarmsWorker.enqueueImmediate(context)
            ReconcileAlarmsWorker.schedulePeriodic(context)
        }
    }
}
