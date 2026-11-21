package com.taskmgr.data.local

import com.taskmgr.domain.model.CompletionType
import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.HabitGroup
import com.taskmgr.domain.model.HabitLog
import com.taskmgr.domain.model.Moment
import com.taskmgr.domain.model.ReminderMode
import com.taskmgr.domain.model.Task

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

fun TaskEntity.toDomain(): Task = Task(
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
    snoozedUntilEpochMillis = snoozedUntilEpochMillis
)

fun Task.toEntity(completedAtEpochMillis: Long? = null): TaskEntity = TaskEntity(
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
    completedAtEpochMillis = completedAtEpochMillis,
    snoozedUntilEpochMillis = snoozedUntilEpochMillis
)

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
