package com.taskmgr.domain.logic

import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.HabitLog
import com.taskmgr.domain.model.Moment
import com.taskmgr.domain.model.SnoozeOption
import com.taskmgr.domain.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

enum class TaskState {
    PENDING,
    SNOOZED,
    COMPLETED,
    EXPIRED_WINDOW
}

enum class HabitLogState {
    IN_PROGRESS,
    SNOOZED_SHORT,
    DISMISSED_FOR_DAY,
    COMPLETED
}

object ReminderStateMachine {

    fun computeTaskState(
        task: Task,
        currentEpochDay: Long,
        currentMinutesOfDay: Int,
        nowEpochMillis: Long,
        moment: Moment? = null
    ): TaskState {
        if (task.isCompleted) return TaskState.COMPLETED
        if (!task.hasDate) return TaskState.PENDING
        if (currentEpochDay > task.endEpochDay) return TaskState.EXPIRED_WINDOW
        if (task.snoozedUntilEpochMillis != null && task.snoozedUntilEpochMillis > nowEpochMillis) {
            return TaskState.SNOOZED
        }
        if (currentEpochDay == task.endEpochDay && moment != null && !MomentWindowEvaluator.crossesMidnight(moment)) {
            if (currentMinutesOfDay > moment.endMinutesOfDay && task.snoozedUntilEpochMillis == null) {
                return TaskState.EXPIRED_WINDOW
            }
        }
        return TaskState.PENDING
    }

    fun reduceTaskOnComplete(task: Task, completed: Boolean): Task {
        return task.copy(
            isCompleted = completed,
            snoozedUntilEpochMillis = if (completed) null else task.snoozedUntilEpochMillis
        )
    }

    fun reduceTaskOnSnooze(
        task: Task,
        option: SnoozeOption,
        nowEpochMillis: Long,
        zoneId: ZoneId,
        moment: Moment? = null
    ): Task {
        if (task.isCompleted) return task
        val snoozeTargetMillis = computeTaskSnoozeMillis(task, option, nowEpochMillis, zoneId, moment)
        return task.copy(snoozedUntilEpochMillis = snoozeTargetMillis)
    }

    fun computeTaskSnoozeMillis(
        task: Task,
        option: SnoozeOption,
        nowEpochMillis: Long,
        zoneId: ZoneId,
        moment: Moment? = null
    ): Long {
        if (!option.untilTomorrow) {
            val minutes = option.minutes ?: 5L
            return nowEpochMillis + minutes * 60_000L
        }
        val today = Instant.ofEpochMilli(nowEpochMillis).atZone(zoneId).toLocalDate()
        val tomorrowEpochDay = today.plusDays(1).toEpochDay()
        val nextDay = tomorrowEpochDay.coerceAtLeast(task.startEpochDay)
        val minuteOfDay = (task.scheduledMinutesOfDay ?: moment?.startMinutesOfDay ?: (9 * 60))
            .coerceIn(0, MomentWindowEvaluator.MINUTES_IN_DAY - 1)
        return ZonedDateTime.of(
            LocalDate.ofEpochDay(nextDay),
            LocalTime.of(minuteOfDay / 60, minuteOfDay % 60),
            zoneId
        ).toInstant().toEpochMilli()
    }

    fun computeHabitLogState(log: HabitLog): HabitLogState {
        return when {
            log.isCompleted -> HabitLogState.COMPLETED
            log.dismissedForDay -> HabitLogState.DISMISSED_FOR_DAY
            log.snoozedUntilEpochMillis != null -> HabitLogState.SNOOZED_SHORT
            else -> HabitLogState.IN_PROGRESS
        }
    }

    fun reduceHabitLogOnIncrement(
        habit: Habit,
        existing: HabitLog?,
        epochDay: Long,
        nowEpochMillis: Long
    ): HabitLog {
        val baseAmount = existing?.accumulatedAmount ?: 0.0
        val newAmount = HabitCompletionCalculator.clampAmount(baseAmount + habit.incrementAmount)
        val completed = HabitCompletionCalculator.isCompleted(newAmount, habit.targetAmount)
        return HabitLog(
            habitId = habit.id,
            epochDay = epochDay,
            accumulatedAmount = newAmount,
            isCompleted = completed,
            dismissedForDay = false,
            snoozedUntilEpochMillis = null,
            updatedAtEpochMillis = nowEpochMillis
        )
    }

    fun reduceHabitLogOnManualEdit(
        habit: Habit,
        epochDay: Long,
        accumulatedAmount: Double,
        dismissedForDay: Boolean,
        nowEpochMillis: Long
    ): HabitLog {
        val clamped = HabitCompletionCalculator.clampAmount(accumulatedAmount)
        val completed = HabitCompletionCalculator.isCompleted(clamped, habit.targetAmount)
        return HabitLog(
            habitId = habit.id,
            epochDay = epochDay,
            accumulatedAmount = clamped,
            isCompleted = completed,
            dismissedForDay = if (completed) false else dismissedForDay,
            snoozedUntilEpochMillis = null,
            updatedAtEpochMillis = nowEpochMillis
        )
    }

    fun reduceHabitLogOnSnooze(
        habit: Habit,
        existing: HabitLog?,
        epochDay: Long,
        option: SnoozeOption,
        nowEpochMillis: Long
    ): HabitLog {
        val currentAmount = existing?.accumulatedAmount ?: 0.0
        val alreadyCompleted = HabitCompletionCalculator.isCompleted(currentAmount, habit.targetAmount)
        if (alreadyCompleted) {
            return (existing ?: HabitLog(
                habitId = habit.id,
                epochDay = epochDay,
                accumulatedAmount = currentAmount,
                isCompleted = true,
                dismissedForDay = false,
                snoozedUntilEpochMillis = null,
                updatedAtEpochMillis = nowEpochMillis
            )).copy(isCompleted = true, snoozedUntilEpochMillis = null, updatedAtEpochMillis = nowEpochMillis)
        }

        return if (option.untilTomorrow) {
            HabitLog(
                habitId = habit.id,
                epochDay = epochDay,
                accumulatedAmount = currentAmount,
                isCompleted = false,
                dismissedForDay = true,
                snoozedUntilEpochMillis = null,
                updatedAtEpochMillis = nowEpochMillis
            )
        } else {
            val minutes = option.minutes ?: 5L
            HabitLog(
                habitId = habit.id,
                epochDay = epochDay,
                accumulatedAmount = currentAmount,
                isCompleted = false,
                dismissedForDay = false,
                snoozedUntilEpochMillis = nowEpochMillis + minutes * 60_000L,
                updatedAtEpochMillis = nowEpochMillis
            )
        }
    }

    fun reduceHabitLogOnSnoozeTimerFired(
        habit: Habit,
        existing: HabitLog,
        nowEpochMillis: Long
    ): HabitLog {
        val completed = HabitCompletionCalculator.isCompleted(existing.accumulatedAmount, habit.targetAmount)
        return existing.copy(
            isCompleted = completed,
            snoozedUntilEpochMillis = null,
            updatedAtEpochMillis = nowEpochMillis
        )
    }
}
