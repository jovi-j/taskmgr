package com.taskmgr.data.local

import com.taskmgr.domain.model.CompletionType
import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.HabitGroup
import com.taskmgr.domain.model.HabitLog
import com.taskmgr.domain.model.Moment
import com.taskmgr.domain.model.ReminderMode
import com.taskmgr.domain.model.Task
import com.taskmgr.domain.model.TaskAdvanceReminder
import com.taskmgr.domain.model.TaskUrgency

fun serializeAdvanceRemindersSpec(reminders: List<TaskAdvanceReminder>): String? {
    val valid = reminders.filter { it.totalMinutesBefore > 0L }
    if (valid.isEmpty()) return null
    return valid.joinToString("|") { r ->
        "${r.daysBefore}:${r.hoursBefore}:${r.minutesBefore}:${r.repeatIntervalMinutes ?: ""}"
    }
}

fun parseAdvanceRemindersSpec(spec: String?): List<TaskAdvanceReminder> {
    if (spec.isNullOrBlank()) return emptyList()
    return spec.split("|").mapNotNull { token ->
        val parts = token.trim().split(":")
        if (parts.size < 3) return@mapNotNull null
        val d = parts[0].toIntOrNull()?.coerceAtLeast(0) ?: 0
        val h = parts[1].toIntOrNull()?.coerceAtLeast(0) ?: 0
        val m = parts[2].toIntOrNull()?.coerceAtLeast(0) ?: 0
        val rep = parts.getOrNull(3)?.takeIf { it.isNotBlank() }?.toIntOrNull()?.takeIf { it > 0 }
        TaskAdvanceReminder(
            daysBefore = d,
            hoursBefore = h,
            minutesBefore = m,
            repeatIntervalMinutes = rep
        ).takeIf { it.totalMinutesBefore > 0L }
    }
}

fun MomentEntity.toDomain(): Moment {
    val mode = runCatching { ReminderMode.valueOf(reminderMode) }
        .getOrElse {
            if (reminderMode == "INTERVAL") ReminderMode.INTERVAL_MINUTES
            else ReminderMode.ONCE_AT_START
        }
    val times = specificTimesCsv
        ?.split(",")
        ?.mapNotNull { it.trim().toIntOrNull() }
        ?: emptyList()
    return Moment(
        id = id,
        name = name,
        startMinutesOfDay = startMinutesOfDay,
        endMinutesOfDay = endMinutesOfDay,
        daysOfWeekMask = daysOfWeekMask,
        reminderMode = mode,
        intervalMinutes = intervalMinutes,
        specificTimesMinutesOfDay = times,
        aggressiveFullScreen = aggressiveFullScreen,
        vibrationEnabled = vibrationEnabled,
        isEnabled = isEnabled
    )
}

fun Moment.toEntity(): MomentEntity = MomentEntity(
    id = id,
    name = name,
    startMinutesOfDay = startMinutesOfDay,
    endMinutesOfDay = endMinutesOfDay,
    daysOfWeekMask = daysOfWeekMask,
    reminderMode = reminderMode.name,
    intervalMinutes = intervalMinutes,
    specificTimesCsv = specificTimesMinutesOfDay
        .takeIf { it.isNotEmpty() }
        ?.joinToString(","),
    aggressiveFullScreen = aggressiveFullScreen,
    vibrationEnabled = vibrationEnabled,
    isEnabled = isEnabled
)

fun HabitGroupEntity.toDomain(): HabitGroup = HabitGroup(
    id = id,
    name = name,
    colorArgb = colorArgb,
    iconGlyph = iconGlyph
)

fun HabitGroup.toEntity(): HabitGroupEntity = HabitGroupEntity(
    id = id,
    name = name,
    colorArgb = colorArgb,
    iconGlyph = iconGlyph
)

fun TaskEntity.toDomain(): Task {
    val parsedUrgency = runCatching { TaskUrgency.valueOf(urgency) }.getOrElse { TaskUrgency.LOW }
    return Task(
        id = id,
        title = title,
        notes = notes,
        startEpochDay = startEpochDay,
        endEpochDay = endEpochDay,
        scheduledMinutesOfDay = scheduledMinutesOfDay,
        momentId = momentId,
        repeatIntervalMinutes = repeatIntervalMinutes,
        aggressiveFullScreen = aggressiveFullScreen,
        vibrationEnabled = vibrationEnabled,
        isCompleted = isCompleted,
        completedAtEpochMillis = completedAtEpochMillis,
        snoozedUntilEpochMillis = snoozedUntilEpochMillis,
        urgency = parsedUrgency,
        advanceReminders = parseAdvanceRemindersSpec(advanceRemindersSpec)
    )
}

fun Task.toEntity(completedAtEpochMillis: Long? = this.completedAtEpochMillis): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        notes = notes,
        startEpochDay = startEpochDay,
        endEpochDay = endEpochDay,
        scheduledMinutesOfDay = scheduledMinutesOfDay,
        momentId = momentId,
        aggressiveFullScreen = aggressiveFullScreen,
        vibrationEnabled = vibrationEnabled,
        repeatIntervalMinutes = repeatIntervalMinutes,
        isCompleted = isCompleted,
        completedAtEpochMillis = if (isCompleted) completedAtEpochMillis else null,
        snoozedUntilEpochMillis = snoozedUntilEpochMillis,
        urgency = urgency.name,
        advanceRemindersSpec = serializeAdvanceRemindersSpec(advanceReminders)
    )
}

fun HabitEntity.toDomain(): Habit = Habit(
    id = id,
    title = title,
    notes = notes,
    startEpochDay = startEpochDay,
    activeDaysMask = activeDaysMask,
    momentId = momentId,
    groupId = groupId,
    iconGlyph = iconGlyph,
    scheduledMinutesOfDay = scheduledMinutesOfDay,
    repeatIntervalMinutes = repeatIntervalMinutes,
    aggressiveFullScreen = aggressiveFullScreen,
    vibrationEnabled = vibrationEnabled,
    completionType = runCatching { CompletionType.valueOf(completionType) }
        .getOrElse { CompletionType.BOOLEAN },
    incrementAmount = incrementAmount,
    incrementUnitLabel = incrementUnitLabel,
    targetAmount = targetAmount,
    targetUnitLabel = targetUnitLabel,
    isActive = isActive
)

fun Habit.toEntity(): HabitEntity = HabitEntity(
    id = id,
    title = title,
    notes = notes,
    startEpochDay = startEpochDay,
    activeDaysMask = activeDaysMask,
    momentId = momentId,
    groupId = groupId,
    iconGlyph = iconGlyph,
    scheduledMinutesOfDay = scheduledMinutesOfDay,
    repeatIntervalMinutes = repeatIntervalMinutes,
    aggressiveFullScreen = aggressiveFullScreen,
    vibrationEnabled = vibrationEnabled,
    completionType = completionType.name,
    incrementAmount = incrementAmount,
    incrementUnitLabel = incrementUnitLabel,
    targetAmount = targetAmount,
    targetUnitLabel = targetUnitLabel,
    isActive = isActive
)

fun HabitLogEntity.toDomain(): HabitLog = HabitLog(
    habitId = habitId,
    epochDay = epochDay,
    accumulatedAmount = accumulatedAmount,
    isCompleted = isCompleted,
    dismissedForDay = dismissedForDay,
    snoozedUntilEpochMillis = snoozedUntilEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis
)

fun HabitLog.toEntity(): HabitLogEntity = HabitLogEntity(
    habitId = habitId,
    epochDay = epochDay,
    accumulatedAmount = accumulatedAmount,
    isCompleted = isCompleted,
    dismissedForDay = dismissedForDay,
    snoozedUntilEpochMillis = snoozedUntilEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis
)
