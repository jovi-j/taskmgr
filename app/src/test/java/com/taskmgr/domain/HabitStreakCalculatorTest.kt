package com.taskmgr.domain

import com.taskmgr.domain.logic.DaysOfWeekMask
import com.taskmgr.domain.logic.HabitStreakCalculator
import com.taskmgr.domain.model.CompletionType
import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.HabitLog
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HabitStreakCalculatorTest {

    @Test
    fun streakCalculation_skipsInactiveDaysAndUpdatesOnRetroactiveEdit() {
        val monWedFri = DaysOfWeekMask.fromDays(
            setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        )
        val monday = LocalDate.of(2026, 11, 16).toEpochDay()
        val wednesday = monday + 2
        val friday = monday + 4

        val habit = Habit(
            id = 1L,
            title = "Treino",
            startEpochDay = monday,
            activeDaysMask = monWedFri,
            completionType = CompletionType.BOOLEAN,
            incrementAmount = 1.0,
            incrementUnitLabel = "Sim",
            targetAmount = 1.0,
            targetUnitLabel = "Sim"
        )

        val logs = listOf(
            HabitLog(1L, monday, 1.0, isCompleted = true, dismissedForDay = false, updatedAtEpochMillis = 100L),
            HabitLog(1L, wednesday, 1.0, isCompleted = true, dismissedForDay = false, updatedAtEpochMillis = 200L)
        )

        // On Friday before completing Friday, streak from Monday+Wednesday is 2
        val statsBeforeFriday = HabitStreakCalculator.calculate(habit, logs, friday)
        assertEquals(2, statsBeforeFriday.currentStreakDays)
        assertEquals(2, statsBeforeFriday.totalCompletedDays)
        assertEquals(2, statsBeforeFriday.longestStreakDays)
        assertEquals(3, statsBeforeFriday.activeDaysSinceStart)
        assertEquals(66, statsBeforeFriday.completionRatePercent)

        // Completing Friday raises streak to 3 and 100%
        val logsWithFriday = logs + HabitLog(
            1L, friday, 1.0, isCompleted = true, dismissedForDay = false, updatedAtEpochMillis = 300L
        )
        val statsAfterFriday = HabitStreakCalculator.calculate(habit, logsWithFriday, friday)
        assertEquals(3, statsAfterFriday.currentStreakDays)
        assertEquals(3, statsAfterFriday.totalCompletedDays)
        assertEquals(3, statsAfterFriday.longestStreakDays)
        assertEquals(100, statsAfterFriday.completionRatePercent)
    }

    @Test
    fun longestStreak_tracksHistoricalBestEvenAfterGap() {
        val start = LocalDate.of(2026, 11, 1).toEpochDay()
        val habit = Habit(
            id = 2L,
            title = "Leitura",
            startEpochDay = start,
            activeDaysMask = DaysOfWeekMask.ALL_DAYS,
            completionType = CompletionType.BOOLEAN,
            incrementAmount = 1.0,
            incrementUnitLabel = "Sim",
            targetAmount = 1.0,
            targetUnitLabel = "Sim"
        )

        // Completed days 0,1,2,3 (4-day streak), missed day 4, completed day 5
        val logs = listOf(0L, 1L, 2L, 3L, 5L).map { offset ->
            HabitLog(2L, start + offset, 1.0, isCompleted = true, dismissedForDay = false, updatedAtEpochMillis = 100L)
        }

        val stats = HabitStreakCalculator.calculate(habit, logs, start + 5L)
        assertEquals(1, stats.currentStreakDays)
        assertEquals(4, stats.longestStreakDays)
        assertEquals(5, stats.totalCompletedDays)
        assertEquals(6, stats.activeDaysSinceStart)
    }
}
