package com.taskmgr.domain

import com.taskmgr.domain.logic.DaysOfWeekMask
import com.taskmgr.domain.logic.HabitCompletionCalculator
import com.taskmgr.domain.logic.ReminderStateMachine
import com.taskmgr.domain.model.CompletionType
import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.HabitLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitCompletionAndFormattingTest {

    @Test
    fun ac01_customUnitWater_incrementsAndFormatsProgress() {
        val habit = Habit(
            id = 1L,
            title = "Beber água",
            startEpochDay = 20_000L,
            activeDaysMask = DaysOfWeekMask.ALL_DAYS,
            completionType = CompletionType.CUSTOM_UNIT,
            incrementAmount = 250.0,
            incrementUnitLabel = "+250 ml",
            targetAmount = 2000.0,
            targetUnitLabel = "2000 ml"
        )
        val initialLog = HabitLog(
            habitId = 1L,
            epochDay = 20_000L,
            accumulatedAmount = 500.0,
            isCompleted = false,
            dismissedForDay = false,
            updatedAtEpochMillis = 1000L
        )

        val updated = ReminderStateMachine.reduceHabitLogOnIncrement(
            habit = habit,
            existing = initialLog,
            epochDay = 20_000L,
            nowEpochMillis = 2000L
        )

        assertEquals(750.0, updated.accumulatedAmount, 1e-6)
        assertFalse(updated.isCompleted)
        assertEquals("750.0 / 2000.0 ml", HabitCompletionCalculator.formatProgressText(habit, updated.accumulatedAmount))
        assertEquals("+250 ml", HabitCompletionCalculator.formatIncrementButtonLabel(habit))
    }

    @Test
    fun ac02_customUnitWater_autoCompletesAtTarget() {
        val habit = Habit(
            id = 1L,
            title = "Beber água",
            startEpochDay = 20_000L,
            activeDaysMask = DaysOfWeekMask.ALL_DAYS,
            completionType = CompletionType.CUSTOM_UNIT,
            incrementAmount = 250.0,
            incrementUnitLabel = "+250 ml",
            targetAmount = 2000.0,
            targetUnitLabel = "2000 ml"
        )
        val logAt1750 = HabitLog(
            habitId = 1L,
            epochDay = 20_000L,
            accumulatedAmount = 1750.0,
            isCompleted = false,
            dismissedForDay = false,
            updatedAtEpochMillis = 1000L
        )

        val completedLog = ReminderStateMachine.reduceHabitLogOnIncrement(
            habit = habit,
            existing = logAt1750,
            epochDay = 20_000L,
            nowEpochMillis = 2000L
        )

        assertEquals(2000.0, completedLog.accumulatedAmount, 1e-6)
        assertTrue(completedLog.isCompleted)
    }

    @Test
    fun distinctIncrementAndTargetUnits_chaptersGoal() {
        val habit = Habit(
            id = 2L,
            title = "Ler livro",
            startEpochDay = 20_000L,
            activeDaysMask = DaysOfWeekMask.ALL_DAYS,
            completionType = CompletionType.CUSTOM_UNIT,
            incrementAmount = 1.0,
            incrementUnitLabel = "capítulo",
            targetAmount = 10.0,
            targetUnitLabel = "capítulos"
        )

        assertEquals("+1 capítulo", HabitCompletionCalculator.formatIncrementButtonLabel(habit))
        assertEquals("3 / 10 capítulos", HabitCompletionCalculator.formatProgressText(habit, 3.0))
        assertFalse(HabitCompletionCalculator.isCompleted(9.0, habit.targetAmount))
        assertTrue(HabitCompletionCalculator.isCompleted(10.0, habit.targetAmount))
    }

    @Test
    fun booleanAndCountPerDay_completionModes() {
        val boolHabit = Habit(
            id = 3L,
            title = "Tomar vitamina",
            startEpochDay = 20_000L,
            activeDaysMask = DaysOfWeekMask.ALL_DAYS,
            completionType = CompletionType.BOOLEAN,
            incrementAmount = 1.0,
            incrementUnitLabel = "Sim",
            targetAmount = 1.0,
            targetUnitLabel = "Sim"
        )
        assertEquals("Sim (Concluir)", HabitCompletionCalculator.formatIncrementButtonLabel(boolHabit))
        val boolDone = ReminderStateMachine.reduceHabitLogOnIncrement(boolHabit, null, 20_000L, 5000L)
        assertTrue(boolDone.isCompleted)
        assertEquals("1 / 1 Sim", HabitCompletionCalculator.formatProgressText(boolHabit, boolDone.accumulatedAmount))

        val countHabit = Habit(
            id = 4L,
            title = "Alongamento",
            startEpochDay = 20_000L,
            activeDaysMask = DaysOfWeekMask.ALL_DAYS,
            completionType = CompletionType.COUNT_PER_DAY,
            incrementAmount = 1.0,
            incrementUnitLabel = "Sim",
            targetAmount = 3.0,
            targetUnitLabel = "Sim"
        )
        assertEquals("+1 Sim", HabitCompletionCalculator.formatIncrementButtonLabel(countHabit))
        val step1 = ReminderStateMachine.reduceHabitLogOnIncrement(countHabit, null, 20_000L, 5000L)
        assertFalse(step1.isCompleted)
        assertEquals("1 / 3 Sim", HabitCompletionCalculator.formatProgressText(countHabit, step1.accumulatedAmount))
        val step2 = ReminderStateMachine.reduceHabitLogOnIncrement(countHabit, step1, 20_000L, 6000L)
        val step3 = ReminderStateMachine.reduceHabitLogOnIncrement(countHabit, step2, 20_000L, 7000L)
        assertTrue(step3.isCompleted)
    }

    @Test
    fun epsilonTolerance_completesWithinFloatingPointDrift() {
        assertTrue(HabitCompletionCalculator.isCompleted(1.9999995, 2.0))
        assertFalse(HabitCompletionCalculator.isCompleted(1.999, 2.0))
    }
}
