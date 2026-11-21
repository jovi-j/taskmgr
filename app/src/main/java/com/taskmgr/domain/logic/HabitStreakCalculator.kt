package com.taskmgr.domain.logic

import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.HabitLog

data class HabitStats(
    val currentStreakDays: Int,
    val totalCompletedDays: Int,
    val longestStreakDays: Int = 0,
    val activeDaysSinceStart: Int = 0,
    val completionRatePercent: Int = 0
)

object HabitStreakCalculator {

    fun calculate(
        habit: Habit,
        logs: List<HabitLog>,
        referenceEpochDay: Long
    ): HabitStats {
        if ((habit.activeDaysMask and DaysOfWeekMask.ALL_DAYS) == 0) {
            return HabitStats(0, 0, 0, 0, 0)
        }

        val logsByDay = logs.associateBy { it.epochDay }
        val totalCompleted = logs.count { it.isCompleted }

        var streak = 0
        var day = referenceEpochDay
        val minDay = habit.startEpochDay

        // If today is an active day and not completed yet, streak can still continue from the previous active day
        if (day >= minDay &&
            DaysOfWeekMask.containsEpochDay(habit.activeDaysMask, day) &&
            logsByDay[day]?.isCompleted != true
        ) {
            day -= 1L
        }

        while (day >= minDay) {
            if (DaysOfWeekMask.containsEpochDay(habit.activeDaysMask, day)) {
                val completed = logsByDay[day]?.isCompleted == true
                if (completed) {
                    streak++
                } else {
                    break
                }
            }
            day -= 1L
        }

        var longestStreak = 0
        var runningStreak = 0
        var activeDaysCount = 0
        var completedInRange = 0

        if (referenceEpochDay >= minDay) {
            for (cursor in minDay..referenceEpochDay) {
                if (DaysOfWeekMask.containsEpochDay(habit.activeDaysMask, cursor)) {
                    activeDaysCount++
                    if (logsByDay[cursor]?.isCompleted == true) {
                        completedInRange++
                        runningStreak++
                        if (runningStreak > longestStreak) {
                            longestStreak = runningStreak
                        }
                    } else {
                        runningStreak = 0
                    }
                }
            }
        }
        longestStreak = maxOf(longestStreak, streak)

        val ratePercent = if (activeDaysCount > 0) {
            ((completedInRange * 100.0) / activeDaysCount).toInt().coerceIn(0, 100)
        } else {
            0
        }

        return HabitStats(
            currentStreakDays = streak,
            totalCompletedDays = totalCompleted,
            longestStreakDays = longestStreak,
            activeDaysSinceStart = activeDaysCount,
            completionRatePercent = ratePercent
        )
    }
}
