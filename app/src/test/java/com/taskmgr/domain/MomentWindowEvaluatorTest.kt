package com.taskmgr.domain

import com.taskmgr.domain.logic.DaysOfWeekMask
import com.taskmgr.domain.logic.MomentWindowEvaluator
import com.taskmgr.domain.model.Moment
import com.taskmgr.domain.model.ReminderMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

class MomentWindowEvaluatorTest {

    private val zone = ZoneOffset.UTC

    @Test
    fun normalWindow_checksDaysAndBoundsCorrectly() {
        val mondayMask = DaysOfWeekMask.fromDays(setOf(DayOfWeek.MONDAY))
        val moment = Moment(
            id = 1L,
            name = "Manhã",
            startMinutesOfDay = 8 * 60,
            endMinutesOfDay = 12 * 60,
            daysOfWeekMask = mondayMask,
            reminderMode = ReminderMode.ONCE_AT_START
        )

        val monday = LocalDate.of(2026, 11, 16).toEpochDay()
        val tuesday = monday + 1

        assertTrue(MomentWindowEvaluator.isWithinWindow(moment, monday, 8 * 60))
        assertTrue(MomentWindowEvaluator.isWithinWindow(moment, monday, 10 * 60))
        assertTrue(MomentWindowEvaluator.isWithinWindow(moment, monday, 12 * 60))
        assertFalse(MomentWindowEvaluator.isWithinWindow(moment, monday, 7 * 60 + 59))
        assertFalse(MomentWindowEvaluator.isWithinWindow(moment, monday, 12 * 60 + 1))
        assertFalse(MomentWindowEvaluator.isWithinWindow(moment, tuesday, 9 * 60))
    }

    @Test
    fun midnightCrossingWindow_evaluatesDayDAndDayDPlusOne() {
        val fridayMask = DaysOfWeekMask.fromDays(setOf(DayOfWeek.FRIDAY))
        val moment = Moment(
            id = 2L,
            name = "Madrugada",
            startMinutesOfDay = 22 * 60,
            endMinutesOfDay = 2 * 60,
            daysOfWeekMask = fridayMask,
            reminderMode = ReminderMode.INTERVAL_MINUTES,
            intervalMinutes = 60
        )

        val friday = LocalDate.of(2026, 11, 20).toEpochDay()
        val saturday = friday + 1
        val sunday = friday + 2

        assertTrue(MomentWindowEvaluator.crossesMidnight(moment))
        assertTrue(MomentWindowEvaluator.isWithinWindow(moment, friday, 22 * 60))
        assertTrue(MomentWindowEvaluator.isWithinWindow(moment, friday, 23 * 60 + 59))
        assertFalse(MomentWindowEvaluator.isWithinWindow(moment, friday, 1 * 60))

        assertTrue(MomentWindowEvaluator.isWithinWindow(moment, saturday, 0))
        assertTrue(MomentWindowEvaluator.isWithinWindow(moment, saturday, 2 * 60))
        assertFalse(MomentWindowEvaluator.isWithinWindow(moment, saturday, 2 * 60 + 1))
        assertFalse(MomentWindowEvaluator.isWithinWindow(moment, saturday, 23 * 60))
        assertFalse(MomentWindowEvaluator.isWithinWindow(moment, sunday, 1 * 60))
    }

    @Test
    fun midnightCrossingWindow_generatesIntervalTriggersAcrossMidnight() {
        val fridayMask = DaysOfWeekMask.fromDays(setOf(DayOfWeek.FRIDAY))
        val moment = Moment(
            id = 3L,
            name = "Noite",
            startMinutesOfDay = 23 * 60,
            endMinutesOfDay = 1 * 60,
            daysOfWeekMask = fridayMask,
            reminderMode = ReminderMode.INTERVAL_MINUTES,
            intervalMinutes = 60
        )

        val friday = LocalDate.of(2026, 11, 20)
        val triggers = MomentWindowEvaluator.triggerInstantsForWindowStartingOn(
            moment = moment,
            startEpochDay = friday.toEpochDay(),
            zoneId = zone
        )

        val expected = listOf(
            ZonedDateTime.of(friday, LocalTime.of(23, 0), zone).toInstant().toEpochMilli(),
            ZonedDateTime.of(friday.plusDays(1), LocalTime.of(0, 0), zone).toInstant().toEpochMilli(),
            ZonedDateTime.of(friday.plusDays(1), LocalTime.of(1, 0), zone).toInstant().toEpochMilli()
        )
        assertEquals(expected, triggers)
    }

    @Test
    fun specificTimes_filtersTimesInsideOvernightWindow() {
        val allMask = DaysOfWeekMask.ALL_DAYS
        val moment = Moment(
            id = 4L,
            name = "Turnos",
            startMinutesOfDay = 22 * 60,
            endMinutesOfDay = 3 * 60,
            daysOfWeekMask = allMask,
            reminderMode = ReminderMode.SPECIFIC_TIMES,
            specificTimesMinutesOfDay = listOf(21 * 60, 22 * 60 + 30, 1 * 60 + 15, 4 * 60)
        )

        val day = LocalDate.of(2026, 11, 20)
        val triggers = MomentWindowEvaluator.triggerInstantsForWindowStartingOn(moment, day.toEpochDay(), zone)

        assertEquals(2, triggers.size)
        val nowBeforeMidnight = ZonedDateTime.of(day, LocalTime.of(23, 0), zone).toInstant().toEpochMilli()
        val next = MomentWindowEvaluator.nextTriggerAfter(moment, nowBeforeMidnight, zone)
        assertNotNull(next)
        assertEquals(
            ZonedDateTime.of(day.plusDays(1), LocalTime.of(1, 15), zone).toInstant().toEpochMilli(),
            next
        )
    }

    @Test
    fun endAtMidnight_treatsZeroAsEndOfSameDay() {
        val fridayMask = DaysOfWeekMask.fromDays(setOf(DayOfWeek.FRIDAY))
        val moment = Moment(
            id = 5L,
            name = "Dia todo até meia-noite",
            startMinutesOfDay = 6 * 60,
            endMinutesOfDay = 0,
            daysOfWeekMask = fridayMask,
            reminderMode = ReminderMode.INTERVAL_MINUTES,
            intervalMinutes = 60
        )

        val friday = LocalDate.of(2026, 11, 20)
        val saturday = friday.plusDays(1)

        assertFalse(MomentWindowEvaluator.crossesMidnight(moment))
        assertTrue(MomentWindowEvaluator.isWithinWindow(moment, friday.toEpochDay(), 6 * 60))
        assertTrue(MomentWindowEvaluator.isWithinWindow(moment, friday.toEpochDay(), 23 * 60 + 59))
        assertFalse(MomentWindowEvaluator.isWithinWindow(moment, saturday.toEpochDay(), 0))
        assertFalse(MomentWindowEvaluator.isWithinWindow(moment, saturday.toEpochDay(), 10 * 60))

        val triggers = MomentWindowEvaluator.triggerInstantsForWindowStartingOn(
            moment = moment,
            startEpochDay = friday.toEpochDay(),
            zoneId = zone
        )
        assertEquals(18, triggers.size) // 06:00 .. 23:00 on Friday only
        assertEquals(
            ZonedDateTime.of(friday, LocalTime.of(23, 0), zone).toInstant().toEpochMilli(),
            triggers.last()
        )
    }
}
