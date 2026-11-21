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

enum class TaskUrgency(
    val titlePtBr: String,
    val matrixTitlePtBr: String,
    val shortLabelPtBr: String,
    val colorNamePtBr: String,
    val colorArgb: Long,
    val sortOrder: Int
) {
    VERY_HIGH(
        titlePtBr = "Urgência Muito Alta",
        matrixTitlePtBr = "Importante e Urgente",
        shortLabelPtBr = "Muito Alta",
        colorNamePtBr = "Vermelho",
        colorArgb = 0xFFEF4444L,
        sortOrder = 0
    ),
    HIGH(
        titlePtBr = "Urgência Alta",
        matrixTitlePtBr = "Importante e Não Urgente",
        shortLabelPtBr = "Alta",
        colorNamePtBr = "Amarelo",
        colorArgb = 0xFFFBBF24L,
        sortOrder = 1
    ),
    MEDIUM(
        titlePtBr = "Urgência Média",
        matrixTitlePtBr = "Não Importante e Urgente",
        shortLabelPtBr = "Média",
        colorNamePtBr = "Verde",
        colorArgb = 0xFF22C55EL,
        sortOrder = 2
    ),
    LOW(
        titlePtBr = "Urgência Baixa",
        matrixTitlePtBr = "Não Importante e Não Urgente",
        shortLabelPtBr = "Baixa",
        colorNamePtBr = "Azul",
        colorArgb = 0xFF3B82F6L,
        sortOrder = 3
    )
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

data class TaskAdvanceReminder(
    val daysBefore: Int = 0,
    val hoursBefore: Int = 0,
    val minutesBefore: Int = 0,
    val repeatIntervalMinutes: Int? = null
) {
    val totalMinutesBefore: Long
        get() = daysBefore.toLong() * 1440L + hoursBefore.toLong() * 60L + minutesBefore.toLong()

    fun formatLabelPtBr(): String {
        val parts = buildList {
            if (daysBefore > 0) add(if (daysBefore == 1) "1 dia" else "$daysBefore dias")
            if (hoursBefore > 0) add(if (hoursBefore == 1) "1 hora" else "$hoursBefore horas")
            if (minutesBefore > 0) add(if (minutesBefore == 1) "1 min" else "$minutesBefore min")
        }
        val offsetText = if (parts.isEmpty()) "No horário" else "${parts.joinToString(", ")} antes"
        val repeatText = repeatIntervalMinutes?.takeIf { it > 0 }?.let { rep ->
            val h = rep / 60
            val m = rep % 60
            val freq = when {
                h > 0 && m > 0 -> "${h}h ${m}min"
                h > 0 -> "${h}h"
                else -> "${m}min"
            }
            " (repetir a cada $freq)"
        }.orEmpty()
        return offsetText + repeatText
    }
}

data class Task(
    val id: Long = 0L,
    val title: String,
    val notes: String? = null,
    val startEpochDay: Long = UNSCHEDULED_EPOCH_DAY,
    val endEpochDay: Long = UNSCHEDULED_EPOCH_DAY,
    val scheduledMinutesOfDay: Int? = null,
    val momentId: Long? = null,
    val repeatIntervalMinutes: Int? = null,
    val aggressiveFullScreen: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val isCompleted: Boolean = false,
    val completedAtEpochMillis: Long? = null,
    val snoozedUntilEpochMillis: Long? = null,
    val urgency: TaskUrgency = TaskUrgency.LOW,
    val advanceReminders: List<TaskAdvanceReminder> = emptyList()
) {
    val hasDate: Boolean
        get() = startEpochDay >= 0L

    val isUnscheduledInbox: Boolean
        get() = !hasDate && scheduledMinutesOfDay == null

    fun isCompletedOnDay(
        epochDay: Long,
        zoneId: java.time.ZoneId = java.time.ZoneId.systemDefault()
    ): Boolean {
        if (!isCompleted) return false
        val completedMillis = completedAtEpochMillis
        return if (completedMillis != null && completedMillis > 0L) {
            val completedDay = java.time.Instant.ofEpochMilli(completedMillis)
                .atZone(zoneId)
                .toLocalDate()
                .toEpochDay()
            completedDay == epochDay
        } else {
            hasDate && epochDay in startEpochDay..endEpochDay
        }
    }

    fun shouldShowInEisenhowerMatrix(
        todayEpochDay: Long,
        zoneId: java.time.ZoneId = java.time.ZoneId.systemDefault()
    ): Boolean = !isCompleted || isCompletedOnDay(todayEpochDay, zoneId)

    companion object {
        const val UNSCHEDULED_EPOCH_DAY: Long = -1L
    }
}

data class HabitGroup(
    val id: Long = 0L,
    val name: String,
    val colorArgb: Long,
    val iconGlyph: String
)

data class Habit(
    val id: Long = 0L,
    val title: String,
    val notes: String? = null,
    val startEpochDay: Long,
    val activeDaysMask: Int,
    val momentId: Long? = null,
    val groupId: Long? = null,
    val iconGlyph: String? = null,
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
    val vibrationEnabled: Boolean,
    val showSnoozeOptions: Boolean = true,
    val acknowledgeOnly: Boolean = false
)

enum class ReminderItemType {
    TASK,
    HABIT
}
