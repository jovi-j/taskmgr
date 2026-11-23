package com.taskmgr.domain

import com.taskmgr.domain.logic.AutoSnoozeDecision
import com.taskmgr.domain.logic.AutoSnoozePolicy
import com.taskmgr.domain.logic.NextAlarmCalculator
import com.taskmgr.domain.model.CompletionType
import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.HabitLog
import com.taskmgr.domain.model.Moment
import com.taskmgr.domain.model.ReminderMode
import com.taskmgr.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class AutoSnoozePolicyTest {

    private val zone = ZoneId.of("UTC")
    private val day = LocalDate.of(2026, 11, 23)

    @Test
    fun `ring timeout is 3 minutes and snooze doubles on each unanswered timeout`() {
        assertEquals(3L, AutoSnoozePolicy.RING_TIMEOUT_MINUTES)
        assertEquals(180_000L, AutoSnoozePolicy.RING_TIMEOUT_MILLIS)

        assertEquals(5L, AutoSnoozePolicy.computeSnoozeMinutes(0))
        assertEquals(10L, AutoSnoozePolicy.computeSnoozeMinutes(1))
        assertEquals(20L, AutoSnoozePolicy.computeSnoozeMinutes(2))
        assertEquals(40L, AutoSnoozePolicy.computeSnoozeMinutes(3))
        assertEquals(80L, AutoSnoozePolicy.computeSnoozeMinutes(4))
        assertEquals(160L, AutoSnoozePolicy.computeSnoozeMinutes(5))
    }

    @Test
    fun `evaluateTimeout returns SnoozeForMinutes with doubling duration while within the same day`() {
        val baseTimeMs = ZonedDateTime.of(day, LocalTime.of(9, 0), zone).toInstant().toEpochMilli()

        // 1st unanswered 3-min ring -> 5 min snooze
        val d1 = AutoSnoozePolicy.evaluateTimeout(0, baseTimeMs, zone)
        assertTrue(d1 is AutoSnoozeDecision.SnoozeForMinutes)
        d1 as AutoSnoozeDecision.SnoozeForMinutes
        assertEquals(5L, d1.snoozeMinutes)
        assertEquals(1, d1.nextCount)
        assertEquals(baseTimeMs + 5 * 60_000L, d1.triggerAtEpochMillis)

        // 2nd unanswered 3-min ring -> 10 min snooze
        val d2 = AutoSnoozePolicy.evaluateTimeout(d1.nextCount, d1.triggerAtEpochMillis + 3 * 60_000L, zone)
        assertTrue(d2 is AutoSnoozeDecision.SnoozeForMinutes)
        d2 as AutoSnoozeDecision.SnoozeForMinutes
        assertEquals(10L, d2.snoozeMinutes)
        assertEquals(2, d2.nextCount)

        // 3rd unanswered 3-min ring -> 20 min snooze
        val d3 = AutoSnoozePolicy.evaluateTimeout(d2.nextCount, d2.triggerAtEpochMillis + 3 * 60_000L, zone)
        assertTrue(d3 is AutoSnoozeDecision.SnoozeForMinutes)
        d3 as AutoSnoozeDecision.SnoozeForMinutes
        assertEquals(20L, d3.snoozeMinutes)
        assertEquals(3, d3.nextCount)
    }

    @Test
    fun `evaluateTimeout stops auto-snooze when doubling crosses into next day or past moment window`() {
        // 1) At 23:45, a 20-minute snooze (previousCount = 2) would cross into 00:05 of the next day
        val lateNightMs = ZonedDateTime.of(day, LocalTime.of(23, 45), zone).toInstant().toEpochMilli()
        val decisionMidnight = AutoSnoozePolicy.evaluateTimeout(
            previousAutoSnoozeCount = 2,
            nowEpochMillis = lateNightMs,
            zoneId = zone
        )
        assertTrue(decisionMidnight is AutoSnoozeDecision.ReachedNextDayStopNotifying)

        // 2) For a Moment ending at 22:00 ("Noite"), at 21:50 a 20-minute snooze would fall at 22:10
        val noiteMoment = Moment(
            id = 3L,
            name = "Noite",
            startMinutesOfDay = 18 * 60,
            endMinutesOfDay = 22 * 60,
            daysOfWeekMask = 0b1111111,
            reminderMode = ReminderMode.INTERVAL_MINUTES,
            intervalMinutes = 60
        )
        val at2150Ms = ZonedDateTime.of(day, LocalTime.of(21, 50), zone).toInstant().toEpochMilli()
        val decisionMomentEnd = AutoSnoozePolicy.evaluateTimeout(
            previousAutoSnoozeCount = 2,
            nowEpochMillis = at2150Ms,
            zoneId = zone,
            moment = noiteMoment
        )
        assertTrue(decisionMomentEnd is AutoSnoozeDecision.ReachedNextDayStopNotifying)
    }

    @Test
    fun `when auto-snooze stops for today, habit and recurring task still schedule tomorrow at normal time`() {
        val multiDayMoment = Moment(
            id = 1L,
            name = "Todo o dia",
            startMinutesOfDay = 6 * 60,
            endMinutesOfDay = 22 * 60,
            daysOfWeekMask = 0b1111111,
            reminderMode = ReminderMode.INTERVAL_MINUTES,
            intervalMinutes = 60
        )

        val singleDayTask = Task(
            id = 1L,
            title = "Tarefa única hoje",
            startEpochDay = day.toEpochDay(),
            endEpochDay = day.toEpochDay(),
            hasEndDate = true,
            scheduledMinutesOfDay = 8 * 60,
            repeatIntervalMinutes = 60
        )
        val recurringTask = Task(
            id = 2L,
            title = "Tarefa recorrente",
            startEpochDay = day.toEpochDay(),
            endEpochDay = Task.OPEN_ENDED_EPOCH_DAY,
            hasEndDate = false,
            momentId = 1L
        )
        val habit = Habit(
            id = 10L,
            title = "Beber água",
            startEpochDay = day.toEpochDay(),
            activeDaysMask = 0b1111111,
            momentId = 1L,
            completionType = CompletionType.BOOLEAN,
            incrementAmount = 1.0,
            incrementUnitLabel = "Sim",
            targetAmount = 1.0,
            targetUnitLabel = "Sim"
        )

        val lateNightMs = ZonedDateTime.of(day, LocalTime.of(23, 50), zone).toInstant().toEpochMilli()
        val tomorrow0600Ms = ZonedDateTime.of(day.plusDays(1), LocalTime.of(6, 0), zone).toInstant().toEpochMilli()

        // Single-day bounded task stops completely after today's auto-snooze limit
        val singleNext = NextAlarmCalculator.calculateNextForTask(
            task = singleDayTask,
            moment = null,
            nowEpochMillis = lateNightMs,
            zoneId = zone,
            skipTodayEpochDay = true
        )
        assertNull(singleNext)

        // Recurring task stops auto-snoozing today (never rings at 03:00 AM), but schedules tomorrow at 06:00
        val recurringNext = NextAlarmCalculator.calculateNextForTask(
            task = recurringTask,
            moment = multiDayMoment,
            nowEpochMillis = lateNightMs,
            zoneId = zone,
            skipTodayEpochDay = true
        )
        assertNotNull(recurringNext)
        assertEquals(day.plusDays(1).toEpochDay(), recurringNext?.targetEpochDay)
        assertEquals(tomorrow0600Ms, recurringNext?.triggerAtEpochMillis)

        // Habit with dismissedForDay = true on today skips any late-night snooze and schedules tomorrow at 06:00
        val todayDismissedHabitLog = HabitLog(
            habitId = habit.id,
            epochDay = day.toEpochDay(),
            accumulatedAmount = 0.0,
            isCompleted = false,
            dismissedForDay = true,
            snoozedUntilEpochMillis = null,
            updatedAtEpochMillis = lateNightMs
        )
        val habitNext = NextAlarmCalculator.calculateNextForHabit(
            habit = habit,
            logForCurrentDay = todayDismissedHabitLog,
            currentEpochDay = day.toEpochDay(),
            moment = multiDayMoment,
            nowEpochMillis = lateNightMs,
            zoneId = zone
        )
        assertNotNull(habitNext)
        assertEquals(day.plusDays(1).toEpochDay(), habitNext?.targetEpochDay)
        assertEquals(tomorrow0600Ms, habitNext?.triggerAtEpochMillis)
    }
}
