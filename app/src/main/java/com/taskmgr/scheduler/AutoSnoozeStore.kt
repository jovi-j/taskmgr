package com.taskmgr.scheduler

import android.content.Context
import com.taskmgr.domain.model.ReminderItemType

class AutoSnoozeStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getConsecutiveCount(itemType: ReminderItemType, itemId: Long, epochDay: Long): Int {
        val storedDay = prefs.getLong(dayKey(itemType, itemId), -1L)
        if (storedDay != epochDay) return 0
        return prefs.getInt(countKey(itemType, itemId), 0).coerceAtLeast(0)
    }

    fun setConsecutiveCount(itemType: ReminderItemType, itemId: Long, epochDay: Long, count: Int) {
        prefs.edit()
            .putLong(dayKey(itemType, itemId), epochDay)
            .putInt(countKey(itemType, itemId), count)
            .apply()
    }

    fun resetUserInteracted(itemType: ReminderItemType, itemId: Long) {
        prefs.edit()
            .remove(dayKey(itemType, itemId))
            .remove(countKey(itemType, itemId))
            .remove(dismissedDayKey(itemType, itemId))
            .apply()
    }

    fun isTaskDismissedForDay(taskId: Long, epochDay: Long): Boolean {
        return prefs.getLong(dismissedDayKey(ReminderItemType.TASK, taskId), -1L) == epochDay
    }

    fun setTaskDismissedForDay(taskId: Long, epochDay: Long) {
        prefs.edit()
            .putLong(dismissedDayKey(ReminderItemType.TASK, taskId), epochDay)
            .remove(countKey(ReminderItemType.TASK, taskId))
            .apply()
    }

    private fun countKey(itemType: ReminderItemType, itemId: Long): String =
        "auto_snooze_count_${itemType.name}_$itemId"

    private fun dayKey(itemType: ReminderItemType, itemId: Long): String =
        "auto_snooze_day_${itemType.name}_$itemId"

    private fun dismissedDayKey(itemType: ReminderItemType, itemId: Long): String =
        "auto_snooze_dismissed_day_${itemType.name}_$itemId"

    companion object {
        private const val PREFS_NAME = "taskmgr_auto_snooze"
    }
}
