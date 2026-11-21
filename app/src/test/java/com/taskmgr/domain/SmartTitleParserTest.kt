package com.taskmgr.domain

import com.taskmgr.domain.logic.DaysOfWeekMask
import com.taskmgr.domain.logic.NextAlarmCalculator
import com.taskmgr.domain.logic.SmartHabitFrequencySpec
import com.taskmgr.domain.logic.SmartTitleParser
import com.taskmgr.domain.logic.SmartTokenKind
import com.taskmgr.domain.model.DefaultMoments
import com.taskmgr.domain.model.HabitGroup
import com.taskmgr.domain.model.Task
import com.taskmgr.domain.model.TaskUrgency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class SmartTitleParserTest {

    private val referenceDate = LocalDate.of(2026, 11, 21)
    private val todayEpochDay = referenceDate.toEpochDay()
    private val seededMoments = DefaultMoments.ALL.mapIndexed { index, moment ->
        moment.copy(id = (index + 1).toLong())
    }
    private val sampleGroups = listOf(
        HabitGroup(id = 10L, name = "Casa", colorArgb = 0xFF689896L, iconGlyph = "home"),
        HabitGroup(id = 20L, name = "Saúde", colorArgb = 0xFF6E9887L, iconGlyph = "fitness")
    )

    @Test
    fun `habit example 1 - Limpar a caixa dos gatos 2x ao dia pela noite grupo casa`() {
        val input = "Limpar a caixa dos gatos 2x ao dia pela noite grupo casa"
        val result = SmartTitleParser.parseHabitTitle(
            text = input,
            todayEpochDay = todayEpochDay,
            moments = seededMoments,
            habitGroups = sampleGroups
        )

        assertEquals("Limpar a caixa dos gatos", result.cleanTitle)
        assertNull(result.startEpochDay)
        assertEquals(SmartHabitFrequencySpec.CountPerDay(2), result.frequencySpec)
        val noiteMoment = seededMoments.first { it.name == "Noite" }
        assertEquals(noiteMoment.id, result.momentId)
        assertEquals(10L, result.groupId)
    }

    @Test
    fun `task example 2 - Visitar meu tio 23_11_2026 toda segunda e terca as 15_00 ate 23_12_2026`() {
        val input = "Visitar meu tio 23/11/2026 toda segunda e terça ás 15:00 até 23/12/2026"
        val result = SmartTitleParser.parseTaskTitle(
            text = input,
            todayEpochDay = todayEpochDay
        )

        assertEquals("Visitar meu tio", result.cleanTitle)
        assertEquals(LocalDate.of(2026, 11, 23).toEpochDay(), result.startEpochDay)
        assertEquals(LocalDate.of(2026, 12, 23).toEpochDay(), result.endEpochDay)
        assertEquals(15 * 60, result.scheduledMinutesOfDay)
        val expectedMask = DaysOfWeekMask.fromDays(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
        assertEquals(expectedMask, result.weekdaysMask)
    }

    @Test
    fun `habit example 3 - Beber agua 250 ml de 2500 ml ao dia grupo saude`() {
        val input = "Beber água 250 ml de 2500 ml ao dia grupo saude"
        val result = SmartTitleParser.parseHabitTitle(
            text = input,
            todayEpochDay = todayEpochDay,
            moments = seededMoments,
            habitGroups = sampleGroups
        )

        assertEquals("Beber água", result.cleanTitle)
        assertEquals(
            SmartHabitFrequencySpec.CustomUnit(
                incrementValueText = "250",
                incrementUnitText = "ml",
                targetValueText = "2500",
                targetUnitText = "ml"
            ),
            result.frequencySpec
        )
        assertEquals(20L, result.groupId)
    }

    @Test
    fun `habit custom unit with accents and urgency for tasks`() {
        val habitInput = "Ler livro 10 capitulos de 100 capítulos ao dia pela manha"
        val habitResult = SmartTitleParser.parseHabitTitle(
            text = habitInput,
            todayEpochDay = todayEpochDay,
            moments = seededMoments,
            habitGroups = sampleGroups
        )

        assertEquals("Ler livro", habitResult.cleanTitle)
        assertEquals(
            SmartHabitFrequencySpec.CustomUnit(
                incrementValueText = "10",
                incrementUnitText = "capítulos",
                targetValueText = "100",
                targetUnitText = "capítulos"
            ),
            habitResult.frequencySpec
        )
        val manhaMoment = seededMoments.first { it.name == "Manhã" }
        assertEquals(manhaMoment.id, habitResult.momentId)

        val taskInput = "Entregar relatório depois de amanhã toda quarta, quinta e sexta urgencia muito alta"
        val taskResult = SmartTitleParser.parseTaskTitle(
            text = taskInput,
            todayEpochDay = todayEpochDay
        )

        assertEquals("Entregar relatório", taskResult.cleanTitle)
        assertEquals(referenceDate.plusDays(2).toEpochDay(), taskResult.startEpochDay)
        assertEquals(
            DaysOfWeekMask.fromDays(setOf(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)),
            taskResult.weekdaysMask
        )
        assertEquals(TaskUrgency.VERY_HIGH, taskResult.urgency)
    }

    @Test
    fun `pressing backspace at end of detected token keeps literal text and dismisses token`() {
        val oldText = "Comprar pão amanhã"
        val parsedBefore = SmartTitleParser.parseTaskTitle(
            text = oldText,
            todayEpochDay = todayEpochDay
        )
        assertEquals(1, parsedBefore.tokens.size)
        assertEquals(SmartTokenKind.START_DATE, parsedBefore.tokens.first().kind)
        assertEquals("Comprar pão", parsedBefore.cleanTitle)

        // Simulate user pressing Backspace right at the end of "amanhã"
        val newTextAfterBackspace = "Comprar pão amanh"
        val editOutcome = SmartTitleParser.processTextEdit(
            oldText = oldText,
            newText = newTextAfterBackspace,
            activeTokens = parsedBefore.tokens,
            dismissedTokens = emptyList()
        )

        // Literal text remains intact and "amanhã" occurrence is added to dismissedTokens
        assertTrue(editOutcome.wasTokenDismissed)
        assertEquals(oldText, editOutcome.text)
        assertEquals(1, editOutcome.dismissedTokens.size)
        assertEquals("amanhã", editOutcome.dismissedTokens.first().rawText)

        val parsedAfter = SmartTitleParser.parseTaskTitle(
            text = editOutcome.text,
            todayEpochDay = todayEpochDay,
            dismissedTokens = editOutcome.dismissedTokens
        )
        assertTrue(parsedAfter.tokens.isEmpty())
        assertNull(parsedAfter.startEpochDay)
        assertEquals("Comprar pão amanhã", parsedAfter.cleanTitle)
    }

    @Test
    fun `default habit moment finds Todo o dia when nothing is typed`() {
        val todoODiaMoment = seededMoments.first { it.name == "Todo o dia" }
        val defaultMomentId = SmartTitleParser.findDefaultHabitMomentId(seededMoments)
        assertEquals(todoODiaMoment.id, defaultMomentId)

        val withoutTodoODia = seededMoments.filter { it.id != todoODiaMoment.id }
        assertNull(SmartTitleParser.findDefaultHabitMomentId(withoutTodoODia))
    }

    @Test
    fun `task open-ended vs bounded end date and weekday filter`() {
        val zone = ZoneId.of("UTC")
        val monday = LocalDate.of(2026, 11, 23) // Monday
        val tuesday = LocalDate.of(2026, 11, 24) // Tuesday
        val wednesday = LocalDate.of(2026, 11, 25) // Wednesday

        val maskMonTue = DaysOfWeekMask.fromDays(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
        val recurringOpenTask = Task(
            id = 1L,
            title = "Visitar meu tio",
            startEpochDay = monday.toEpochDay(),
            endEpochDay = Task.OPEN_ENDED_EPOCH_DAY,
            hasEndDate = false,
            activeDaysMask = maskMonTue,
            scheduledMinutesOfDay = 15 * 60
        )

        assertTrue(recurringOpenTask.isActiveOnDay(monday.toEpochDay(), zone))
        assertTrue(recurringOpenTask.isActiveOnDay(tuesday.toEpochDay(), zone))
        assertFalse(recurringOpenTask.isActiveOnDay(wednesday.toEpochDay(), zone))

        // On Wednesday morning, next alarm should jump to next Monday at 15:00
        val wedMorningMs = ZonedDateTime.of(wednesday, LocalTime.of(9, 0), zone).toInstant().toEpochMilli()
        val nextTrigger = NextAlarmCalculator.calculateNextForTask(
            task = recurringOpenTask,
            moment = null,
            nowEpochMillis = wedMorningMs,
            zoneId = zone
        )
        assertNotNull(nextTrigger)
        val expectedNextMondayMs = ZonedDateTime.of(
            monday.plusWeeks(1),
            LocalTime.of(15, 0),
            zone
        ).toInstant().toEpochMilli()
        assertEquals(expectedNextMondayMs, nextTrigger?.triggerAtEpochMillis)
    }
}
