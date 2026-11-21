package com.taskmgr.domain

import com.taskmgr.data.sync.TaskCalendarEventMapper
import com.taskmgr.domain.export.MarkdownTransferEngine
import com.taskmgr.domain.export.TaskMgrBackupSnapshot
import com.taskmgr.domain.logic.DaysOfWeekMask
import com.taskmgr.domain.model.CompletionType
import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.HabitGroup
import com.taskmgr.domain.model.HabitLog
import com.taskmgr.domain.model.Moment
import com.taskmgr.domain.model.ReminderMode
import com.taskmgr.domain.model.Task
import com.taskmgr.domain.model.TaskUrgency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MarkdownTransferAndCalendarSyncTest {

    @Test
    fun taskUrgency_defaultsToLow_andMatchesEisenhowerQuadrants() {
        val day = LocalDate.of(2026, 11, 20).toEpochDay()
        val defaultTask = Task(
            title = "Tarefa padrão",
            startEpochDay = day,
            endEpochDay = day
        )
        assertEquals(TaskUrgency.LOW, defaultTask.urgency)
        assertEquals("Urgência Baixa", TaskUrgency.LOW.titlePtBr)
        assertEquals("Não importante e Não Urgente", TaskUrgency.LOW.matrixTitlePtBr)
        assertEquals("Azul", TaskUrgency.LOW.colorNamePtBr)

        assertEquals("Urgência Muito Alta", TaskUrgency.VERY_HIGH.titlePtBr)
        assertEquals("Importante e Urgente", TaskUrgency.VERY_HIGH.matrixTitlePtBr)
        assertEquals("Vermelho", TaskUrgency.VERY_HIGH.colorNamePtBr)

        assertEquals("Urgência Alta", TaskUrgency.HIGH.titlePtBr)
        assertEquals("Não importante e Urgente", TaskUrgency.HIGH.matrixTitlePtBr)
        assertEquals("Amarelo", TaskUrgency.HIGH.colorNamePtBr)

        assertEquals("Urgência Média", TaskUrgency.MEDIUM.titlePtBr)
        assertEquals("Importante e Não Urgente", TaskUrgency.MEDIUM.matrixTitlePtBr)
        assertEquals("Verde", TaskUrgency.MEDIUM.colorNamePtBr)
    }

    @Test
    fun taskMgrMarkdown_roundTrip_preservesMomentsGroupsTasksHabitsAndLogs() {
        val refDay = LocalDate.of(2026, 11, 20).toEpochDay()
        val moment = Moment(
            id = 10L,
            name = "Manhã Focada",
            startMinutesOfDay = 360,
            endMinutesOfDay = 720,
            daysOfWeekMask = DaysOfWeekMask.ALL_DAYS,
            reminderMode = ReminderMode.INTERVAL_MINUTES,
            intervalMinutes = 45,
            aggressiveFullScreen = true,
            vibrationEnabled = true,
            isEnabled = true
        )
        val group = HabitGroup(
            id = 20L,
            name = "Casa",
            colorArgb = 0xFFF87171L,
            iconGlyph = "\uF02DC"
        )
        val task = Task(
            id = 30L,
            title = "Pagar conta de energia",
            notes = "Vence dia 20\nUsar PIX",
            startEpochDay = refDay,
            endEpochDay = refDay + 1,
            scheduledMinutesOfDay = 540,
            momentId = 10L,
            repeatIntervalMinutes = 90,
            aggressiveFullScreen = true,
            vibrationEnabled = false,
            isCompleted = false,
            urgency = TaskUrgency.VERY_HIGH
        )
        val habit = Habit(
            id = 40L,
            title = "Beber água",
            notes = "Garrafa térmica na mesa",
            startEpochDay = refDay - 5,
            activeDaysMask = DaysOfWeekMask.ALL_DAYS,
            momentId = 10L,
            groupId = 20L,
            iconGlyph = "\uF02D1",
            scheduledMinutesOfDay = 480,
            repeatIntervalMinutes = 60,
            aggressiveFullScreen = true,
            vibrationEnabled = true,
            completionType = CompletionType.CUSTOM_UNIT,
            incrementAmount = 250.0,
            incrementUnitLabel = "+250 ml",
            targetAmount = 2000.0,
            targetUnitLabel = "2000 ml",
            isActive = true
        )
        val log = HabitLog(
            habitId = 40L,
            epochDay = refDay,
            accumulatedAmount = 2250.0,
            isCompleted = true,
            dismissedForDay = false,
            snoozedUntilEpochMillis = null,
            updatedAtEpochMillis = 1732100000000L
        )

        val snapshot = TaskMgrBackupSnapshot(
            moments = listOf(moment),
            habitGroups = listOf(group),
            tasks = listOf(task),
            habits = listOf(habit),
            habitLogs = listOf(log),
            referenceEpochDay = refDay,
            exportedAtIso = "2026-11-20T20:00:00"
        )

        val exported = MarkdownTransferEngine.exportTaskMgrMarkdown(snapshot)
        assertTrue(exported.contains("format: taskmgr-v1"))
        assertTrue(exported.contains("- [ ] **Pagar conta de energia**"))

        val imported = MarkdownTransferEngine.parseTaskMgrMarkdown(exported, refDay)
        assertTrue(imported.isFullTaskMgrFormat)
        assertEquals(listOf(moment), imported.moments)
        assertEquals(listOf(group), imported.habitGroups)
        assertEquals(listOf(task), imported.tasks)
        assertEquals(listOf(habit), imported.habits)
        assertEquals(listOf(log), imported.habitLogs)
    }

    @Test
    fun notionObsidianMarkdown_exportsCleanGfmWithoutInternalComments_andParsesBackTasks() {
        val refDay = LocalDate.of(2026, 11, 20).toEpochDay()
        val taskPending = Task(
            id = 1L,
            title = "Revisar PR no GitHub",
            notes = "Conferir testes unitários",
            startEpochDay = refDay,
            endEpochDay = refDay,
            scheduledMinutesOfDay = 600,
            isCompleted = false
        )
        val taskDone = Task(
            id = 2L,
            title = "Comprar café",
            notes = null,
            startEpochDay = refDay,
            endEpochDay = refDay,
            scheduledMinutesOfDay = null,
            isCompleted = true
        )

        val md = MarkdownTransferEngine.exportNotionObsidianMarkdown(
            TaskMgrBackupSnapshot(
                tasks = listOf(taskPending, taskDone),
                referenceEpochDay = refDay
            )
        )

        assertFalse(md.contains("<!-- TASKMGR_"))
        assertTrue(md.contains("- [ ] **Revisar PR no GitHub**"))
        assertTrue(md.contains("- [x] **Comprar café**"))

        val parsedTasks = MarkdownTransferEngine.parseGfmMarkdownChecklists(md, refDay)
        assertEquals(2, parsedTasks.size)

        val first = parsedTasks[0]
        assertEquals("Revisar PR no GitHub", first.title)
        assertEquals("Conferir testes unitários", first.notes)
        assertEquals(refDay, first.startEpochDay)
        assertEquals(600, first.scheduledMinutesOfDay)
        assertFalse(first.isCompleted)

        val second = parsedTasks[1]
        assertEquals("Comprar café", second.title)
        assertTrue(second.isCompleted)
    }

    @Test
    fun parseGfmMarkdownChecklists_parsesExternalNotionAndObsidianChecklists() {
        val defaultDay = LocalDate.of(2026, 11, 20).toEpochDay()
        val externalMd = """
            # Lista do Obsidian / Notion
            - [ ] Estudar Kotlin Coroutines
              > Ler documentação de StateFlow
            * [x] Enviar relatório mensal 2026-11-22 14:30
            1. [ ] Agendar dentista — Dr. Marcos
        """.trimIndent()

        val parsed = MarkdownTransferEngine.parseGfmMarkdownChecklists(externalMd, defaultDay)
        assertEquals(3, parsed.size)

        assertEquals("Estudar Kotlin Coroutines", parsed[0].title)
        assertEquals("Ler documentação de StateFlow", parsed[0].notes)
        assertFalse(parsed[0].isCompleted)

        assertTrue(parsed[1].isCompleted)
        assertEquals(LocalDate.of(2026, 11, 22).toEpochDay(), parsed[1].startEpochDay)
        assertEquals(14 * 60 + 30, parsed[1].scheduledMinutesOfDay)

        assertEquals("Agendar dentista", parsed[2].title)
        assertEquals("Dr. Marcos", parsed[2].notes)
    }

    @Test
    fun taskCalendarEventMapper_mapsTimedAndAllDayTasksCorrectly() {
        val day = LocalDate.of(2026, 11, 20).toEpochDay()
        val zone = ZoneId.of("America/Sao_Paulo")
        val timedTask = Task(
            id = 7L,
            title = "Reunião de Sprint",
            notes = "Alinhar entregas",
            startEpochDay = day,
            endEpochDay = day,
            scheduledMinutesOfDay = 9 * 60,
            isCompleted = true
        )

        val payload = TaskCalendarEventMapper.buildEventPayload(timedTask, moment = null, zoneId = zone)
        assertEquals("✅ Reunião de Sprint", payload.title)
        assertFalse(payload.allDay)
        assertEquals(zone.id, payload.timeZoneId)
        assertEquals("Reunião de Sprint", TaskCalendarEventMapper.stripCompletionPrefix(payload.title))
        assertEquals("Alinhar entregas", TaskCalendarEventMapper.stripTaskMgrFooter(payload.description))

        val allDayTask = timedTask.copy(scheduledMinutesOfDay = null, isCompleted = false)
        val allDayPayload = TaskCalendarEventMapper.buildEventPayload(allDayTask, moment = null, zoneId = zone)
        assertTrue(allDayPayload.allDay)
        assertEquals("UTC", allDayPayload.timeZoneId)
    }
}
