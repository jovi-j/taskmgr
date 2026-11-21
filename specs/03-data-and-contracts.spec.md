# specs/03-data-and-contracts.spec.md
version: 1.0.0
status: draft
system: taskmgr (Android)

## 1. Modelos de Domínio e Contratos Kotlin

```kotlin
package com.taskmgr.domain.model

enum class ReminderMode {
    ONCE_AT_START,
    INTERVAL_MINUTES,
    SPECIFIC_TIMES
}

enum class CompletionType {
    BOOLEAN,
    COUNT_PER_DAY,
    CUSTOM_UNIT
}

enum class SnoozeOption(val minutes: Long?, val untilTomorrow: Boolean) {
    MINUTES_5(5L, false),
    MINUTES_10(10L, false),
    MINUTES_30(30L, false),
    HOURS_1(60L, false),
    HOURS_3(180L, false),
    UNTIL_TOMORROW(null, true)
}

data class Moment(
    val id: Long = 0L,
    val name: String,
    val startMinutesOfDay: Int,
    val endMinutesOfDay: Int,
    val daysOfWeekMask: Int,
    val reminderMode: ReminderMode,
    val intervalMinutes: Int? = null,
    val specificTimesMinutesOfDay: List<Int> = emptyList(),
    val aggressiveFullScreen: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val isEnabled: Boolean = true
)

data class Task(
    val id: Long = 0L,
    val title: String,
    val notes: String? = null,
    val startEpochDay: Long,
    val endEpochDay: Long,
    val scheduledMinutesOfDay: Int? = null,
    val momentId: Long? = null,
    val repeatIntervalMinutes: Int? = null,
    val aggressiveFullScreen: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val isCompleted: Boolean = false,
    val snoozedUntilEpochMillis: Long? = null
)

data class Habit(
    val id: Long = 0L,
    val title: String,
    val notes: String? = null,
    val startEpochDay: Long,
    val activeDaysMask: Int,
    val momentId: Long? = null,
    val scheduledMinutesOfDay: Int? = null,
    val repeatIntervalMinutes: Int? = null,
    val aggressiveFullScreen: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val completionType: CompletionType,
    val incrementAmount: Double,
    val incrementUnitLabel: String,
    val targetAmount: Double,
    val targetUnitLabel: String,
    val isActive: Boolean = true
)

data class HabitLog(
    val habitId: Long,
    val epochDay: Long,
    val accumulatedAmount: Double,
    val isCompleted: Boolean,
    val dismissedForDay: Boolean,
    val snoozedUntilEpochMillis: Long? = null,
    val updatedAtEpochMillis: Long
)

data class FullScreenReminderPayload(
    val itemType: ReminderItemType,
    val itemId: Long,
    val epochDay: Long,
    val title: String,
    val momentName: String?,
    val progressText: String?,
    val incrementButtonLabel: String?,
    val vibrationEnabled: Boolean
)

enum class ReminderItemType {
    TASK,
    HABIT
}