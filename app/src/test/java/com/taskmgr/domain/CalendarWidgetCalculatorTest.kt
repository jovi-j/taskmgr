package com.taskmgr.domain

import com.taskmgr.domain.model.Task
import com.taskmgr.domain.model.TaskUrgency
import com.taskmgr.widget.CalendarWidgetCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CalendarWidgetCalculatorTest {

    @Test
    fun buildMonthGrid_november2026StartsOnSundayAndHas5Weeks() {
        val weeks = CalendarWidgetCalculator.buildMonthGrid(2026, 11)
        assertEquals(5, weeks.size)
        // Nov 1, 2026 is Sunday -> index 0
        assertEquals(1, weeks[0][0]?.dayOfMonth)
        assertTrue(weeks[0][0]?.isWeekend == true)
        assertEquals(21, weeks[2][6]?.dayOfMonth)
        assertEquals(30, weeks[4][1]?.dayOfMonth)
        assertEquals(null, weeks[4][2])
    }

    @Test
    fun computeMonthPendingDotEpochDays_marksTodayAndFuturePendingTasksOnly() {
        val today = LocalDate.of(2026, 11, 21).toEpochDay()
        val yesterday = LocalDate.of(2026, 11, 20).toEpochDay()
        val futureDay = LocalDate.of(2026, 11, 24).toEpochDay()
        val multiDayStart = LocalDate.of(2026, 11, 26).toEpochDay()
        val multiDayEnd = LocalDate.of(2026, 11, 27).toEpochDay()
        val completedFutureDay = LocalDate.of(2026, 11, 28).toEpochDay()

        val tasks = listOf(
            Task(id = 1L, title = "Passada", startEpochDay = yesterday, endEpochDay = yesterday, isCompleted = false),
            Task(id = 2L, title = "Hoje", startEpochDay = today, endEpochDay = today, isCompleted = false),
            Task(id = 3L, title = "Consulta", startEpochDay = futureDay, endEpochDay = futureDay, isCompleted = false),
            Task(id = 4L, title = "Projeto", startEpochDay = multiDayStart, endEpochDay = multiDayEnd, isCompleted = false),
            Task(id = 5L, title = "Concluída futura", startEpochDay = completedFutureDay, endEpochDay = completedFutureDay, isCompleted = true),
            Task(id = 6L, title = "Sem data", startEpochDay = Task.UNSCHEDULED_EPOCH_DAY, endEpochDay = Task.UNSCHEDULED_EPOCH_DAY)
        )

        val dots = CalendarWidgetCalculator.computeMonthPendingDotEpochDays(
            year = 2026,
            month = 11,
            todayEpochDay = today,
            tasks = tasks
        )

        assertFalse(yesterday in dots)
        assertTrue(today in dots)
        assertTrue(futureDay in dots)
        assertTrue(multiDayStart in dots)
        assertTrue(multiDayEnd in dots)
        assertFalse(completedFutureDay in dots)
    }

    @Test
    fun tasksForDay_sortsPendingBeforeCompletedAndSerializationRoundTrips() {
        val day = LocalDate.of(2026, 11, 24).toEpochDay()
        val tasks = listOf(
            Task(id = 1L, title = "Já feita", startEpochDay = day, endEpochDay = day, scheduledMinutesOfDay = 8 * 60, isCompleted = true),
            Task(id = 2L, title = "Reunião | 15h", startEpochDay = day, endEpochDay = day, scheduledMinutesOfDay = 15 * 60, isCompleted = false, urgency = TaskUrgency.VERY_HIGH),
            Task(id = 3L, title = "Sem hora", startEpochDay = day, endEpochDay = day, scheduledMinutesOfDay = null, isCompleted = false, urgency = TaskUrgency.LOW)
        )

        val dayItems = CalendarWidgetCalculator.tasksForDay(day, tasks)
        assertEquals(listOf(2L, 3L, 1L), dayItems.map { it.id })

        val serialized = CalendarWidgetCalculator.serializeTasks(dayItems)
        val restored = CalendarWidgetCalculator.deserializeTasks(serialized)
        assertEquals(dayItems, restored)
    }

    @Test
    fun labelsPtBr_formatMonthHeaderAndTaskBadge() {
        val today = LocalDate.of(2026, 11, 21).toEpochDay()
        assertEquals("novembro", CalendarWidgetCalculator.formatMonthTitlePtBr(2026, 11, 2026))
        assertEquals("jan 2027", CalendarWidgetCalculator.formatMonthTitlePtBr(2027, 1, 2026))
        assertEquals("Hoje", CalendarWidgetCalculator.formatSelectedDayHeaderPtBr(today, today))
        assertEquals("Amanhã", CalendarWidgetCalculator.formatSelectedDayHeaderPtBr(today + 1, today))
        assertEquals("24 nov", CalendarWidgetCalculator.formatSelectedDayHeaderPtBr(today + 3, today))
        assertEquals("15:00", CalendarWidgetCalculator.formatTaskRowBadge(15 * 60, today, today))
        assertEquals("Hoje", CalendarWidgetCalculator.formatTaskRowBadge(null, today, today))
        assertEquals("24/11", CalendarWidgetCalculator.formatTaskRowBadge(null, today + 3, today))
    }
}
