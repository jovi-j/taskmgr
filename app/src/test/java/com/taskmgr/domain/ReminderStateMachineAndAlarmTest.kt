package com.taskmgr.domain

import com.taskmgr.domain.logic.DaysOfWeekMask
import com.taskmgr.domain.logic.HabitLogState
import com.taskmgr.domain.logic.NextAlarmCalculator
import com.taskmgr.domain.logic.ReminderStateMachine
import com.taskmgr.domain.logic.TaskState
import com.taskmgr.domain.model.CompletionType
import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.SnoozeOption
import com.taskmgr.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ReminderStateMachineAndAlarmTest {

    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 11, 20)
    private val todayEpochDay = today.toEpochDay()
    private val t0 = ZonedDateTime.of(today, LocalTime.of(10, 0), zone).toInstant().toEpochMilli()

    @Test
    fun ac03_habitSnoozeUntilTomorrow_dismissesDayAndSchedulesNextActiveDay() {
        val habit = Habit(
            id = 10L,
            title = "Meditar",
            startEpochDay = todayEpochDay,
            activeDaysMask = DaysOfWeekMask.ALL_DAYS,
            scheduledMinutesOfDay = 9 * 60,
            repeatIntervalMinutes = 30,
            aggressiveFullScreen = true,
            completionType = CompletionType.BOOLEAN,
            incrementAmount = 1.0,
            incrementUnitLabel = "Sim",
            targetAmount = 1.0,
            targetUnitLabel = "Sim"
        )

        val snoozedLog = ReminderStateMachine.reduceHabitLogOnSnooze(
            habit = habit,
            existing = null,
            epochDay = todayEpochDay,
            option = SnoozeOption.UNTIL_TOMORROW,
            nowEpochMillis = t0
        )

        assertTrue(snoozedLog.dismissedForDay)
        assertFalse(snoozedLog.isCompleted)
        assertEquals(HabitLogState.DISMISSED_FOR_DAY, ReminderStateMachine.computeHabitLogState(snoozedLog))

        val nextAlarm = NextAlarmCalculator.calculateNextForHabit(
            habit = habit,
            logForCurrentDay = snoozedLog,
            currentEpochDay = todayEpochDay,
            moment = null,
            nowEpochMillis = t0,
            zoneId = zone
        )
        assertNotNull(nextAlarm)
        assertEquals(todayEpochDay + 1, nextAlarm!!.targetEpochDay)
        val expectedMillis = ZonedDateTime.of(today.plusDays(1), LocalTime.of(9, 0), zone)
            .toInstant()
            .toEpochMilli()
        assertEquals(expectedMillis, nextAlarm.triggerAtEpochMillis)
    }

    @Test
    fun ac04_shortAndMediumSnoozeDurations_scheduleExactMillis() {
        val habit = Habit(
            id = 11L,
            title = "Revisar email",
            startEpochDay = todayEpochDay,
            activeDaysMask = DaysOfWeekMask.ALL_DAYS,
            scheduledMinutesOfDay = 10 * 60,
            completionType = CompletionType.BOOLEAN,
            incrementAmount = 1.0,
            incrementUnitLabel = "Sim",
            targetAmount = 1.0,
            targetUnitLabel = "Sim"
        )

        val shortOptions = listOf(
            SnoozeOption.MINUTES_5 to 5L,
            SnoozeOption.MINUTES_10 to 10L,
            SnoozeOption.MINUTES_30 to 30L,
            SnoozeOption.HOURS_1 to 60L,
            SnoozeOption.HOURS_3 to 180L
        )

        for ((option, expectedMinutes) in shortOptions) {
            val log = ReminderStateMachine.reduceHabitLogOnSnooze(
                habit = habit,
                existing = null,
                epochDay = todayEpochDay,
                option = option,
                nowEpochMillis = t0
            )
            assertFalse(log.isCompleted)
            assertFalse(log.dismissedForDay)
            assertEquals(HabitLogState.SNOOZED_SHORT, ReminderStateMachine.computeHabitLogState(log))
            assertEquals(t0 + expectedMinutes * 60_000L, log.snoozedUntilEpochMillis)

            val nextAlarm = NextAlarmCalculator.calculateNextForHabit(
                habit = habit,
                logForCurrentDay = log,
                currentEpochDay = todayEpochDay,
                moment = null,
                nowEpochMillis = t0,
                zoneId = zone
            )
            assertNotNull(nextAlarm)
            assertEquals(t0 + expectedMinutes * 60_000L, nextAlarm!!.triggerAtEpochMillis)
        }
    }

    @Test
    fun taskStateMachine_handlesCompleteAndSnoozeAndInv01() {
        val task = Task(
            id = 1L,
            title = "Pagar conta",
            startEpochDay = todayEpochDay,
            endEpochDay = todayEpochDay + 2,
            scheduledMinutesOfDay = 14 * 60
        )

        assertEquals(
            TaskState.PENDING,
            ReminderStateMachine.computeTaskState(task, todayEpochDay, 10 * 60, t0)
        )

        val snoozed = ReminderStateMachine.reduceTaskOnSnooze(task, SnoozeOption.MINUTES_30, t0, zone)
        assertEquals(t0 + 30 * 60_000L, snoozed.snoozedUntilEpochMillis)
        assertEquals(
            TaskState.SNOOZED,
            ReminderStateMachine.computeTaskState(snoozed, todayEpochDay, 10 * 60, t0)
        )

        val completed = ReminderStateMachine.reduceTaskOnComplete(snoozed, true)
        assertTrue(completed.isCompleted)
        assertNull(completed.snoozedUntilEpochMillis)
        assertEquals(
            TaskState.COMPLETED,
            ReminderStateMachine.computeTaskState(completed, todayEpochDay, 10 * 60, t0)
        )

        assertNull(NextAlarmCalculator.calculateNextForTask(completed, null, t0, zone))
    }

    @Test
    fun ac06_retroactiveManualEdit_recalculatesCompletionDeterministically() {
        val habit = Habit(
            id = 12L,
            title = "Beber água",
            startEpochDay = todayEpochDay - 5,
            activeDaysMask = DaysOfWeekMask.ALL_DAYS,
            completionType = CompletionType.CUSTOM_UNIT,
            incrementAmount = 250.0,
            incrementUnitLabel = "+250 ml",
            targetAmount = 2000.0,
            targetUnitLabel = "2000 ml"
        )

        val pastDay = todayEpochDay - 2
        val editedComplete = ReminderStateMachine.reduceHabitLogOnManualEdit(
            habit = habit,
            epochDay = pastDay,
            accumulatedAmount = 2000.0,
            dismissedForDay = false,
            nowEpochMillis = t0
        )
        assertTrue(editedComplete.isCompleted)
        assertEquals(t0, editedComplete.updatedAtEpochMillis)
        assertEquals(HabitLogState.COMPLETED, ReminderStateMachine.computeHabitLogState(editedComplete))

        val editedIncomplete = ReminderStateMachine.reduceHabitLogOnManualEdit(
            habit = habit,
            epochDay = pastDay,
            accumulatedAmount = 1500.0,
            dismissedForDay = false,
            nowEpochMillis = t0 + 1000L
        )
        assertFalse(editedIncomplete.isCompleted)
        assertEquals(HabitLogState.IN_PROGRESS, ReminderStateMachine.computeHabitLogState(editedIncomplete))
    }

    @Test
    fun taskAdvanceReminders_schedules1Day2HoursAnd30MinBeforeAppointment() {
        val appointmentDate = LocalDate.of(2026, 11, 24)
        val appointmentEpochDay = appointmentDate.toEpochDay()
        val task = Task(
            id = 99L,
            title = "Marcar consulta médica",
            startEpochDay = appointmentEpochDay,
            endEpochDay = appointmentEpochDay,
            scheduledMinutesOfDay = 15 * 60, // 15:00
            advanceReminders = listOf(
                com.taskmgr.domain.model.TaskAdvanceReminder(daysBefore = 1),
                com.taskmgr.domain.model.TaskAdvanceReminder(hoursBefore = 2),
                com.taskmgr.domain.model.TaskAdvanceReminder(minutesBefore = 30)
            )
        )

        // 1. Before 23/11/2026 15:00 -> next alarm is 23/11/2026 15:00 (1 day before)
        val nowSunday = ZonedDateTime.of(LocalDate.of(2026, 11, 22), LocalTime.of(12, 0), zone)
            .toInstant().toEpochMilli()
        val alarm1DayBefore = NextAlarmCalculator.calculateNextForTask(task, null, nowSunday, zone)
        assertNotNull(alarm1DayBefore)
        assertTrue(alarm1DayBefore!!.isAdvanceReminder)
        val expected1DayBefore = ZonedDateTime.of(LocalDate.of(2026, 11, 23), LocalTime.of(15, 0), zone)
            .toInstant().toEpochMilli()
        assertEquals(expected1DayBefore, alarm1DayBefore.triggerAtEpochMillis)

        // 2. Just after 23/11/2026 15:00 -> next alarm is 24/11/2026 13:00 (2 hours before)
        val alarm2HoursBefore = NextAlarmCalculator.calculateNextForTask(
            task,
            null,
            expected1DayBefore + 1_000L,
            zone
        )
        assertNotNull(alarm2HoursBefore)
        assertTrue(alarm2HoursBefore!!.isAdvanceReminder)
        val expected2HoursBefore = ZonedDateTime.of(appointmentDate, LocalTime.of(13, 0), zone)
            .toInstant().toEpochMilli()
        assertEquals(expected2HoursBefore, alarm2HoursBefore.triggerAtEpochMillis)

        // 3. Just after 24/11/2026 13:00 -> next alarm is 24/11/2026 14:30 (30 minutes before)
        val alarm30MinBefore = NextAlarmCalculator.calculateNextForTask(
            task,
            null,
            expected2HoursBefore + 1_000L,
            zone
        )
        assertNotNull(alarm30MinBefore)
        assertTrue(alarm30MinBefore!!.isAdvanceReminder)
        val expected30MinBefore = ZonedDateTime.of(appointmentDate, LocalTime.of(14, 30), zone)
            .toInstant().toEpochMilli()
        assertEquals(expected30MinBefore, alarm30MinBefore.triggerAtEpochMillis)

        // 4. Just after 24/11/2026 14:30 -> next alarm is 24/11/2026 15:00 (the event itself)
        val alarmEventTime = NextAlarmCalculator.calculateNextForTask(
            task,
            null,
            expected30MinBefore + 1_000L,
            zone
        )
        assertNotNull(alarmEventTime)
        assertFalse(alarmEventTime!!.isAdvanceReminder)
        val expectedEventTime = ZonedDateTime.of(appointmentDate, LocalTime.of(15, 0), zone)
            .toInstant().toEpochMilli()
        assertEquals(expectedEventTime, alarmEventTime.triggerAtEpochMillis)
    }

    @Test
    fun unscheduledTask_neverTriggersAlarmAndStaysPending() {
        val unscheduled = Task(
            id = 100L,
            title = "Ideia sem data nem hora",
            startEpochDay = Task.UNSCHEDULED_EPOCH_DAY,
            endEpochDay = Task.UNSCHEDULED_EPOCH_DAY,
            scheduledMinutesOfDay = null
        )
        assertFalse(unscheduled.hasDate)
        assertTrue(unscheduled.isUnscheduledInbox)
        assertEquals(
            TaskState.PENDING,
            ReminderStateMachine.computeTaskState(unscheduled, todayEpochDay, 12 * 60, t0)
        )
        assertNull(NextAlarmCalculator.calculateNextForTask(unscheduled, null, t0, zone))
    }
}
