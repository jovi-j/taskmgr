package com.taskmgr.domain.export

import com.taskmgr.domain.logic.DaysOfWeekMask
import com.taskmgr.domain.logic.HabitCompletionCalculator
import com.taskmgr.domain.logic.HabitStreakCalculator
import com.taskmgr.domain.model.CompletionType
import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.HabitGroup
import com.taskmgr.domain.model.HabitLog
import com.taskmgr.domain.model.Moment
import com.taskmgr.domain.model.ReminderMode
import com.taskmgr.domain.model.Task
import com.taskmgr.domain.model.TaskUrgency
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale

data class TaskMgrBackupSnapshot(
    val moments: List<Moment> = emptyList(),
    val habitGroups: List<HabitGroup> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val habitLogs: List<HabitLog> = emptyList(),
    val referenceEpochDay: Long = LocalDate.now().toEpochDay(),
    val exportedAtIso: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
)

data class ImportedTaskMgrBundle(
    val isFullTaskMgrFormat: Boolean,
    val moments: List<Moment> = emptyList(),
    val habitGroups: List<HabitGroup> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val habitLogs: List<HabitLog> = emptyList()
)

object MarkdownTransferEngine {

    private const val META_MOMENT_PREFIX = "<!-- TASKMGR_MOMENT: "
    private const val META_GROUP_PREFIX = "<!-- TASKMGR_GROUP: "
    private const val META_TASK_PREFIX = "<!-- TASKMGR_TASK: "
    private const val META_HABIT_PREFIX = "<!-- TASKMGR_HABIT: "
    private const val META_LOG_PREFIX = "<!-- TASKMGR_LOG: "
    private const val META_SUFFIX = " -->"

    private val gfmCheckboxRegex = Regex(
        """^\s*(?:[-*+]|\d+\.)\s+\[([ xX])](\s+)(.+)$"""
    )

    /**
     * Generates a clean GitHub Flavored Markdown (.md) document optimized for Notion and Obsidian.
     */
    fun exportNotionObsidianMarkdown(snapshot: TaskMgrBackupSnapshot): String {
        return buildMarkdownDocument(snapshot = snapshot, includeTaskMgrMetadata = false)
    }

    /**
     * Generates a full `.taskmgr.md` document: 100% valid GitHub Flavored Markdown with embedded
     * lossless metadata comments so another instance of the app can restore all groups, moments,
     * tasks, habits, and habit history logs without losing any detail.
     */
    fun exportTaskMgrMarkdown(snapshot: TaskMgrBackupSnapshot): String {
        return buildMarkdownDocument(snapshot = snapshot, includeTaskMgrMetadata = true)
    }

    /**
     * Parses a `.taskmgr.md` file (restoring all groups, moments, habits, logs, and tasks with full details).
     * If the file does not contain `.taskmgr.md` metadata blocks, falls back to parsing standard GFM checklists.
     */
    fun parseTaskMgrMarkdown(
        markdown: String,
        defaultEpochDay: Long = LocalDate.now().toEpochDay()
    ): ImportedTaskMgrBundle {
        val moments = mutableListOf<Moment>()
        val groups = mutableListOf<HabitGroup>()
        val tasks = mutableListOf<Task>()
        val habits = mutableListOf<Habit>()
        val logs = mutableListOf<HabitLog>()

        markdown.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith(META_MOMENT_PREFIX) && line.endsWith(META_SUFFIX) -> {
                    val map = parseKeyValueMap(line.removePrefix(META_MOMENT_PREFIX).removeSuffix(META_SUFFIX))
                    parseMomentFromMap(map)?.let(moments::add)
                }
                line.startsWith(META_GROUP_PREFIX) && line.endsWith(META_SUFFIX) -> {
                    val map = parseKeyValueMap(line.removePrefix(META_GROUP_PREFIX).removeSuffix(META_SUFFIX))
                    parseGroupFromMap(map)?.let(groups::add)
                }
                line.startsWith(META_TASK_PREFIX) && line.endsWith(META_SUFFIX) -> {
                    val map = parseKeyValueMap(line.removePrefix(META_TASK_PREFIX).removeSuffix(META_SUFFIX))
                    parseTaskFromMap(map, defaultEpochDay)?.let(tasks::add)
                }
                line.startsWith(META_HABIT_PREFIX) && line.endsWith(META_SUFFIX) -> {
                    val map = parseKeyValueMap(line.removePrefix(META_HABIT_PREFIX).removeSuffix(META_SUFFIX))
                    parseHabitFromMap(map, defaultEpochDay)?.let(habits::add)
                }
                line.startsWith(META_LOG_PREFIX) && line.endsWith(META_SUFFIX) -> {
                    val map = parseKeyValueMap(line.removePrefix(META_LOG_PREFIX).removeSuffix(META_SUFFIX))
                    parseLogFromMap(map)?.let(logs::add)
                }
            }
        }

        val hasFullMetadata = moments.isNotEmpty() ||
            groups.isNotEmpty() ||
            tasks.isNotEmpty() ||
            habits.isNotEmpty() ||
            logs.isNotEmpty() ||
            markdown.contains("format: taskmgr-v1")

        if (hasFullMetadata) {
            return ImportedTaskMgrBundle(
                isFullTaskMgrFormat = true,
                moments = moments,
                habitGroups = groups,
                tasks = tasks,
                habits = habits,
                habitLogs = logs
            )
        }

        val gfmTasks = parseGfmMarkdownChecklists(markdown, defaultEpochDay)
        return ImportedTaskMgrBundle(
            isFullTaskMgrFormat = false,
            tasks = gfmTasks
        )
    }

    /**
     * Parses standard GitHub Flavored Markdown (`.md`) from Notion or Obsidian, extracting all
     * GFM checklist items (`- [ ]` and `- [x]`) as tasks, including optional inline date/time tags
     * and indented notes.
     */
    fun parseGfmMarkdownChecklists(
        markdown: String,
        defaultEpochDay: Long = LocalDate.now().toEpochDay()
    ): List<Task> {
        val result = mutableListOf<Task>()
        val lines = markdown.lines()
        var index = 0

        while (index < lines.size) {
            val line = lines[index]
            val match = gfmCheckboxRegex.find(line)
            if (match != null) {
                val isCompleted = match.groupValues[1].equals("x", ignoreCase = true)
                val rawBody = match.groupValues[3].trim()

                val parsedLine = extractCleanTaskFromGfmLine(rawBody, defaultEpochDay)
                val noteLines = mutableListOf<String>()
                if (parsedLine.inlineNote != null) {
                    noteLines.add(parsedLine.inlineNote)
                }

                var lookahead = index + 1
                while (lookahead < lines.size) {
                    val nextRaw = lines[lookahead]
                    val nextTrim = nextRaw.trim()
                    if (nextTrim.isEmpty()) break
                    if (gfmCheckboxRegex.matches(nextRaw)) break
                    if (nextTrim.startsWith("#") || nextTrim.startsWith("---") || nextTrim.startsWith("|")) break
                    if (nextTrim.startsWith("<!--")) {
                        lookahead++
                        continue
                    }
                    val isIndentedOrQuote = nextRaw.startsWith("  ") ||
                        nextRaw.startsWith("\t") ||
                        nextTrim.startsWith(">")
                    if (isIndentedOrQuote) {
                        val cleanedNote = nextTrim.removePrefix(">").trim()
                        if (cleanedNote.isNotEmpty()) {
                            noteLines.add(cleanedNote)
                        }
                        lookahead++
                    } else {
                        break
                    }
                }

                if (parsedLine.title.isNotBlank()) {
                    result.add(
                        Task(
                            id = 0L,
                            title = parsedLine.title,
                            notes = noteLines.takeIf { it.isNotEmpty() }?.joinToString("\n"),
                            startEpochDay = parsedLine.startEpochDay,
                            endEpochDay = parsedLine.endEpochDay,
                            scheduledMinutesOfDay = parsedLine.scheduledMinutesOfDay,
                            momentId = null,
                            repeatIntervalMinutes = null,
                            aggressiveFullScreen = true,
                            vibrationEnabled = true,
                            isCompleted = isCompleted,
                            snoozedUntilEpochMillis = null,
                            urgency = parsedLine.urgency
                        )
                    )
                }
                index = lookahead
            } else {
                index++
            }
        }
        return result
    }

    private data class ParsedGfmLine(
        val title: String,
        val startEpochDay: Long,
        val endEpochDay: Long,
        val scheduledMinutesOfDay: Int?,
        val inlineNote: String?,
        val urgency: TaskUrgency = TaskUrgency.LOW
    )

    private fun extractCleanTaskFromGfmLine(rawBody: String, defaultEpochDay: Long): ParsedGfmLine {
        var working = rawBody

        // Extract optional trailing note after " — " or " - Nota: "
        var inlineNote: String? = null
        val emDashIndex = working.indexOf(" — ")
        if (emDashIndex > 0) {
            inlineNote = working.substring(emDashIndex + 3).trim().takeIf { it.isNotEmpty() }
            working = working.substring(0, emDashIndex).trim()
        }

        // Look for ISO date YYYY-MM-DD or range YYYY-MM-DD..YYYY-MM-DD
        var startDay = defaultEpochDay
        var endDay = defaultEpochDay
        val dateRangeRegex = Regex("""`?(?:📅\s*)?(\d{4}-\d{2}-\d{2})(?:\s*(?:\.\.|a|->|→)\s*(\d{4}-\d{2}-\d{2}))?`?""")
        val dateMatch = dateRangeRegex.find(working)
        if (dateMatch != null) {
            val startParsed = runCatching { LocalDate.parse(dateMatch.groupValues[1]).toEpochDay() }.getOrNull()
            val endParsed = dateMatch.groupValues.getOrNull(2)
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { LocalDate.parse(it).toEpochDay() }.getOrNull() }
            if (startParsed != null) {
                startDay = startParsed
                endDay = if (endParsed != null && endParsed >= startParsed) endParsed else startParsed
            }
        }

        // Look for HH:mm time tag
        var scheduledMinutes: Int? = null
        val timeRegex = Regex("""`?(?:⏰\s*)?([01]\d|2[0-3]):([0-5]\d)`?""")
        val timeMatch = timeRegex.find(working)
        if (timeMatch != null) {
            val h = timeMatch.groupValues[1].toInt()
            val m = timeMatch.groupValues[2].toInt()
            scheduledMinutes = h * 60 + m
        }

        val urgencyBadgeMatch = Regex("""`⚡\s*([^`]+)`""").find(working)
        val parsedUrgency = urgencyBadgeMatch?.groupValues?.getOrNull(1)?.trim()?.let { label ->
            TaskUrgency.entries.firstOrNull {
                it.titlePtBr.equals(label, ignoreCase = true) ||
                    it.matrixTitlePtBr.equals(label, ignoreCase = true)
            }
        } ?: TaskUrgency.LOW

        // Clean formatting badges (`⏰ 08:00`, `📅 2026-11-20`, `**title**`, etc.)
        var cleanTitle = working
            .replace(Regex("""`⏰\s*\d{2}:\d{2}`"""), "")
            .replace(Regex("""`📅\s*\d{4}-\d{2}-\d{2}(?:\s*(?:\.\.|a|->|→)\s*\d{4}-\d{2}-\d{2})?`"""), "")
            .replace(Regex("""`⏱️[^`]+`"""), "")
            .replace(Regex("""`🔁[^`]+`"""), "")
            .replace(Regex("""`⚡[^`]+`"""), "")
            .trim()

        if (cleanTitle.startsWith("**") && cleanTitle.endsWith("**") && cleanTitle.length > 4) {
            cleanTitle = cleanTitle.substring(2, cleanTitle.length - 2).trim()
        } else if (cleanTitle.startsWith("**")) {
            val closeBold = cleanTitle.indexOf("**", startIndex = 2)
            if (closeBold > 2) {
                val mainPart = cleanTitle.substring(2, closeBold).trim()
                val rest = cleanTitle.substring(closeBold + 2).trim()
                cleanTitle = if (rest.isEmpty()) mainPart else "$mainPart $rest".trim()
            }
        }

        if (cleanTitle.isBlank()) {
            cleanTitle = rawBody.replace("**", "").trim()
        }

        return ParsedGfmLine(
            title = cleanTitle,
            startEpochDay = startDay,
            endEpochDay = endDay,
            scheduledMinutesOfDay = scheduledMinutes,
            inlineNote = inlineNote,
            urgency = parsedUrgency
        )
    }

    private fun buildMarkdownDocument(
        snapshot: TaskMgrBackupSnapshot,
        includeTaskMgrMetadata: Boolean
    ): String {
        val sb = StringBuilder()
        val momentsById = snapshot.moments.associateBy { it.id }
        val groupsById = snapshot.habitGroups.associateBy { it.id }
        val logsByHabit = snapshot.habitLogs.groupBy { it.habitId }
        val refDate = LocalDate.ofEpochDay(snapshot.referenceEpochDay)

        // YAML Frontmatter (supported natively by Obsidian and Notion)
        sb.appendLine("---")
        sb.appendLine("title: \"TaskMgr - Exportação\"")
        sb.appendLine("exported_at: \"${snapshot.exportedAtIso}\"")
        sb.appendLine("reference_date: \"$refDate\"")
        if (includeTaskMgrMetadata) {
            sb.appendLine("format: taskmgr-v1")
        }
        sb.appendLine("tags:")
        sb.appendLine("  - taskmgr")
        sb.appendLine("  - produtividade")
        sb.appendLine("---")
        sb.appendLine()

        sb.appendLine("# 📋 TaskMgr — Painel de Tarefas e Hábitos")
        sb.appendLine()
        val completedTasks = snapshot.tasks.count { it.isCompleted }
        val pendingTasks = snapshot.tasks.size - completedTasks
        sb.appendLine(
            "> **Resumo:** ${snapshot.tasks.size} tarefas ($pendingTasks pendentes, $completedTasks concluídas) • " +
                "${snapshot.habits.size} hábitos • ${snapshot.habitGroups.size} grupos • ${snapshot.moments.size} momentos"
        )
        sb.appendLine()

        // 1. Tasks Section (GFM Checklists)
        sb.appendLine("## ✅ Tarefas")
        sb.appendLine()
        if (snapshot.tasks.isEmpty()) {
            sb.appendLine("_Nenhuma tarefa cadastrada._")
            sb.appendLine()
        } else {
            val sortedTasks = snapshot.tasks.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenBy { it.startEpochDay }
                    .thenBy { it.scheduledMinutesOfDay ?: Int.MAX_VALUE }
                    .thenBy { it.id }
            )
            for (task in sortedTasks) {
                val check = if (task.isCompleted) "x" else " "
                val badges = mutableListOf<String>()
                val dateBadge = if (!task.hasDate) {
                    "`📅 Sem data`"
                } else {
                    val startStr = LocalDate.ofEpochDay(task.startEpochDay).toString()
                    val endStr = LocalDate.ofEpochDay(task.endEpochDay).toString()
                    if (task.startEpochDay == task.endEpochDay) {
                        "`📅 $startStr`"
                    } else {
                        "`📅 $startStr..$endStr`"
                    }
                }
                badges.add(dateBadge)

                task.scheduledMinutesOfDay?.let { mins ->
                    badges.add("`⏰ ${formatMinutes(mins)}`")
                }
                task.momentId?.let { mId ->
                    momentsById[mId]?.let { m ->
                        badges.add("`⏱️ ${escapeInlineCode(m.name)}`")
                    }
                }
                task.repeatIntervalMinutes?.let { rep ->
                    badges.add("`🔁 ${formatIntervalShort(rep)}`")
                }
                badges.add("`⚡ ${task.urgency.titlePtBr}`")

                val badgeSuffix = if (badges.isNotEmpty()) " " + badges.joinToString(" ") else ""
                sb.appendLine("- [$check] **${escapeMarkdownText(task.title)}**$badgeSuffix")
                if (!task.notes.isNullOrBlank()) {
                    task.notes.lines().forEach { noteLine ->
                        if (noteLine.isNotBlank()) {
                            sb.appendLine("  > ${noteLine.trim()}")
                        }
                    }
                }
                if (includeTaskMgrMetadata) {
                    sb.appendLine("  " + serializeTaskMeta(task))
                }
            }
            sb.appendLine()
        }

        // 2. Habits Section (GFM Table + Optional Metadata)
        sb.appendLine("## 🔥 Hábitos e Retenção")
        sb.appendLine()
        if (snapshot.habits.isEmpty()) {
            sb.appendLine("_Nenhum hábito cadastrado._")
            sb.appendLine()
        } else {
            sb.appendLine("| Hábito | Grupo | Meta Diária | Momento / Horário | Sequência Atual | Recorde | Dias Concluídos |")
            sb.appendLine("| :--- | :--- | :--- | :--- | :---: | :---: | :---: |")
            for (habit in snapshot.habits.sortedBy { it.id }) {
                val groupName = habit.groupId?.let { groupsById[it]?.name } ?: "Sem grupo"
                val targetDesc = when (habit.completionType) {
                    CompletionType.BOOLEAN -> "1x (${habit.targetUnitLabel.ifBlank { "Sim" }})"
                    CompletionType.COUNT_PER_DAY, CompletionType.CUSTOM_UNIT ->
                        "${HabitCompletionCalculator.formatCleanNumber(habit.targetAmount)} ${habit.targetUnitLabel}"
                }
                val scheduleDesc = buildList {
                    habit.scheduledMinutesOfDay?.let { add(formatMinutes(it)) }
                    habit.momentId?.let { mId -> momentsById[mId]?.let { add(it.name) } }
                    habit.repeatIntervalMinutes?.let { add("a cada ${formatIntervalShort(it)}") }
                }.ifEmpty { listOf("Livre") }.joinToString(" • ")

                val habitLogs = logsByHabit[habit.id].orEmpty()
                val stats = HabitStreakCalculator.calculate(habit, habitLogs, snapshot.referenceEpochDay)

                sb.appendLine(
                    "| ${escapeTableCell(habit.title)} | ${escapeTableCell(groupName)} | " +
                        "${escapeTableCell(targetDesc)} | ${escapeTableCell(scheduleDesc)} | " +
                        "${stats.currentStreakDays}d | ${stats.longestStreakDays}d | ${stats.totalCompletedDays}d |"
                )
            }
            sb.appendLine()

            if (includeTaskMgrMetadata) {
                for (habit in snapshot.habits.sortedBy { it.id }) {
                    sb.appendLine(serializeHabitMeta(habit))
                }
                sb.appendLine()
            }
        }

        // 3. Habit Groups Section
        sb.appendLine("## 🏷️ Grupos de Hábitos")
        sb.appendLine()
        if (snapshot.habitGroups.isEmpty()) {
            sb.appendLine("_Nenhum grupo cadastrado._")
            sb.appendLine()
        } else {
            sb.appendLine("| Grupo | Cor (Hex) |")
            sb.appendLine("| :--- | :--- |")
            for (group in snapshot.habitGroups.sortedBy { it.id }) {
                val hex = String.format(Locale.US, "#%06X", (group.colorArgb and 0xFFFFFFL))
                sb.appendLine("| ${escapeTableCell(group.name)} | `$hex` |")
            }
            sb.appendLine()
            if (includeTaskMgrMetadata) {
                for (group in snapshot.habitGroups.sortedBy { it.id }) {
                    sb.appendLine(serializeGroupMeta(group))
                }
                sb.appendLine()
            }
        }

        // 4. Moments Section
        sb.appendLine("## ⏱️ Momentos")
        sb.appendLine()
        if (snapshot.moments.isEmpty()) {
            sb.appendLine("_Nenhum momento cadastrado._")
            sb.appendLine()
        } else {
            sb.appendLine("| Momento | Janela | Dias Ativos | Modo de Lembrete | Status |")
            sb.appendLine("| :--- | :---: | :--- | :--- | :---: |")
            for (moment in snapshot.moments.sortedBy { it.startMinutesOfDay }) {
                val windowStr = "${formatMinutes(moment.startMinutesOfDay)} - ${formatMinutes(moment.endMinutesOfDay)}"
                val daysStr = formatDaysMaskShort(moment.daysOfWeekMask)
                val modeStr = when (moment.reminderMode) {
                    ReminderMode.ONCE_AT_START -> "No início"
                    ReminderMode.INTERVAL_MINUTES ->
                        "A cada ${formatIntervalShort(moment.intervalMinutes ?: 15)}"
                    ReminderMode.SPECIFIC_TIMES ->
                        moment.specificTimesMinutesOfDay.joinToString(", ") { formatMinutes(it) }
                            .ifBlank { "Horários fixos" }
                }
                val statusStr = if (moment.isEnabled) "Ativo" else "Pausado"
                sb.appendLine(
                    "| ${escapeTableCell(moment.name)} | `$windowStr` | ${escapeTableCell(daysStr)} | " +
                        "${escapeTableCell(modeStr)} | $statusStr |"
                )
            }
            sb.appendLine()
            if (includeTaskMgrMetadata) {
                for (moment in snapshot.moments.sortedBy { it.id }) {
                    sb.appendLine(serializeMomentMeta(moment))
                }
                sb.appendLine()
            }
        }

        // 5. Habit Logs History
        if (snapshot.habitLogs.isNotEmpty()) {
            val habitsById = snapshot.habits.associateBy { it.id }
            sb.appendLine("## 📅 Histórico de Execução de Hábitos")
            sb.appendLine()
            sb.appendLine("| Data | Hábito | Progresso | Status |")
            sb.appendLine("| :--- | :--- | :--- | :---: |")
            val sortedLogs = snapshot.habitLogs.sortedWith(
                compareByDescending<HabitLog> { it.epochDay }.thenBy { it.habitId }
            )
            for (log in sortedLogs) {
                val habit = habitsById[log.habitId]
                val habitName = habit?.title ?: "Hábito #${log.habitId}"
                val dateStr = LocalDate.ofEpochDay(log.epochDay).toString()
                val progressStr = if (habit != null) {
                    HabitCompletionCalculator.formatProgressText(habit, log.accumulatedAmount)
                } else {
                    HabitCompletionCalculator.formatCleanNumber(log.accumulatedAmount)
                }
                val statusStr = when {
                    log.isCompleted -> "✅ Concluído"
                    log.dismissedForDay -> "⏭️ Dispensado"
                    log.accumulatedAmount > 0.0 -> "⏳ Parcial"
                    else -> "—"
                }
                sb.appendLine(
                    "| `$dateStr` | ${escapeTableCell(habitName)} | ${escapeTableCell(progressStr)} | $statusStr |"
                )
            }
            sb.appendLine()
            if (includeTaskMgrMetadata) {
                for (log in sortedLogs) {
                    sb.appendLine(serializeLogMeta(log))
                }
                sb.appendLine()
            }
        }

        return sb.toString().trimEnd() + "\n"
    }

    private fun serializeMomentMeta(moment: Moment): String {
        val timesCsv = moment.specificTimesMinutesOfDay.joinToString(",")
        return buildMetaComment(
            prefix = META_MOMENT_PREFIX,
            pairs = listOf(
                "id" to moment.id.toString(),
                "name_b64" to encodeB64(moment.name),
                "start" to moment.startMinutesOfDay.toString(),
                "end" to moment.endMinutesOfDay.toString(),
                "days" to moment.daysOfWeekMask.toString(),
                "mode" to moment.reminderMode.name,
                "interval" to (moment.intervalMinutes?.toString() ?: ""),
                "times" to timesCsv,
                "fullScreen" to moment.aggressiveFullScreen.toString(),
                "vibrate" to moment.vibrationEnabled.toString(),
                "enabled" to moment.isEnabled.toString()
            )
        )
    }

    private fun parseMomentFromMap(map: Map<String, String>): Moment? {
        val id = map["id"]?.toLongOrNull() ?: return null
        val name = decodeB64(map["name_b64"]).takeIf { it.isNotBlank() } ?: return null
        val start = map["start"]?.toIntOrNull() ?: 0
        val end = map["end"]?.toIntOrNull() ?: 0
        val days = map["days"]?.toIntOrNull() ?: DaysOfWeekMask.ALL_DAYS
        val mode = map["mode"]?.let {
            runCatching { ReminderMode.valueOf(it) }.getOrNull()
        } ?: ReminderMode.ONCE_AT_START
        val interval = map["interval"]?.takeIf { it.isNotBlank() }?.toIntOrNull()
        val times = map["times"].orEmpty()
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
        val fullScreen = map["fullScreen"]?.toBooleanStrictOrNull() ?: true
        val vibrate = map["vibrate"]?.toBooleanStrictOrNull() ?: true
        val enabled = map["enabled"]?.toBooleanStrictOrNull() ?: true

        return Moment(
            id = id,
            name = name,
            startMinutesOfDay = start,
            endMinutesOfDay = end,
            daysOfWeekMask = days,
            reminderMode = mode,
            intervalMinutes = interval,
            specificTimesMinutesOfDay = times,
            aggressiveFullScreen = fullScreen,
            vibrationEnabled = vibrate,
            isEnabled = enabled
        )
    }

    private fun serializeGroupMeta(group: HabitGroup): String {
        return buildMetaComment(
            prefix = META_GROUP_PREFIX,
            pairs = listOf(
                "id" to group.id.toString(),
                "name_b64" to encodeB64(group.name),
                "color" to group.colorArgb.toString(),
                "icon_b64" to encodeB64(group.iconGlyph)
            )
        )
    }

    private fun parseGroupFromMap(map: Map<String, String>): HabitGroup? {
        val id = map["id"]?.toLongOrNull() ?: return null
        val name = decodeB64(map["name_b64"]).takeIf { it.isNotBlank() } ?: return null
        val color = map["color"]?.toLongOrNull() ?: 0xFF818CF8L
        val icon = decodeB64(map["icon_b64"]).ifBlank { "\uF015" }
        return HabitGroup(
            id = id,
            name = name,
            colorArgb = color,
            iconGlyph = icon
        )
    }

    private fun serializeTaskMeta(task: Task): String {
        val advanceSpec = com.taskmgr.data.local.serializeAdvanceRemindersSpec(task.advanceReminders).orEmpty()
        return buildMetaComment(
            prefix = META_TASK_PREFIX,
            pairs = listOf(
                "id" to task.id.toString(),
                "title_b64" to encodeB64(task.title),
                "notes_b64" to encodeB64(task.notes.orEmpty()),
                "startDay" to task.startEpochDay.toString(),
                "endDay" to task.endEpochDay.toString(),
                "schedMin" to (task.scheduledMinutesOfDay?.toString() ?: ""),
                "momentId" to (task.momentId?.toString() ?: ""),
                "repeatMin" to (task.repeatIntervalMinutes?.toString() ?: ""),
                "fullScreen" to task.aggressiveFullScreen.toString(),
                "vibrate" to task.vibrationEnabled.toString(),
                "completed" to task.isCompleted.toString(),
                "snoozed" to (task.snoozedUntilEpochMillis?.toString() ?: ""),
                "urgency" to task.urgency.name,
                "advance" to advanceSpec
            )
        )
    }

    private fun parseTaskFromMap(map: Map<String, String>, defaultEpochDay: Long): Task? {
        val id = map["id"]?.toLongOrNull() ?: 0L
        val title = decodeB64(map["title_b64"]).takeIf { it.isNotBlank() } ?: return null
        val notes = decodeB64(map["notes_b64"]).takeIf { it.isNotBlank() }
        val startDay = map["startDay"]?.toLongOrNull() ?: defaultEpochDay
        val endDay = if (startDay < 0L) {
            Task.UNSCHEDULED_EPOCH_DAY
        } else {
            map["endDay"]?.toLongOrNull()?.coerceAtLeast(startDay) ?: startDay
        }
        val schedMin = map["schedMin"]?.takeIf { it.isNotBlank() }?.toIntOrNull()
        val momentId = map["momentId"]?.takeIf { it.isNotBlank() }?.toLongOrNull()
        val repeatMin = map["repeatMin"]?.takeIf { it.isNotBlank() }?.toIntOrNull()
        val fullScreen = map["fullScreen"]?.toBooleanStrictOrNull() ?: true
        val vibrate = map["vibrate"]?.toBooleanStrictOrNull() ?: true
        val completed = map["completed"]?.toBooleanStrictOrNull() ?: false
        val snoozed = map["snoozed"]?.takeIf { it.isNotBlank() }?.toLongOrNull()
        val parsedUrgency = map["urgency"]?.let {
            runCatching { TaskUrgency.valueOf(it) }.getOrNull()
        } ?: TaskUrgency.LOW
        val advanceReminders = com.taskmgr.data.local.parseAdvanceRemindersSpec(map["advance"])

        return Task(
            id = id,
            title = title,
            notes = notes,
            startEpochDay = startDay,
            endEpochDay = endDay,
            scheduledMinutesOfDay = schedMin,
            momentId = momentId,
            repeatIntervalMinutes = repeatMin,
            aggressiveFullScreen = fullScreen,
            vibrationEnabled = vibrate,
            isCompleted = completed,
            snoozedUntilEpochMillis = snoozed,
            urgency = parsedUrgency,
            advanceReminders = advanceReminders
        )
    }

    private fun serializeHabitMeta(habit: Habit): String {
        return buildMetaComment(
            prefix = META_HABIT_PREFIX,
            pairs = listOf(
                "id" to habit.id.toString(),
                "title_b64" to encodeB64(habit.title),
                "notes_b64" to encodeB64(habit.notes.orEmpty()),
                "startDay" to habit.startEpochDay.toString(),
                "activeDays" to habit.activeDaysMask.toString(),
                "momentId" to (habit.momentId?.toString() ?: ""),
                "groupId" to (habit.groupId?.toString() ?: ""),
                "icon_b64" to encodeB64(habit.iconGlyph.orEmpty()),
                "schedMin" to (habit.scheduledMinutesOfDay?.toString() ?: ""),
                "repeatMin" to (habit.repeatIntervalMinutes?.toString() ?: ""),
                "fullScreen" to habit.aggressiveFullScreen.toString(),
                "vibrate" to habit.vibrationEnabled.toString(),
                "type" to habit.completionType.name,
                "incAmt" to habit.incrementAmount.toString(),
                "incUnit_b64" to encodeB64(habit.incrementUnitLabel),
                "targetAmt" to habit.targetAmount.toString(),
                "targetUnit_b64" to encodeB64(habit.targetUnitLabel),
                "active" to habit.isActive.toString()
            )
        )
    }

    private fun parseHabitFromMap(map: Map<String, String>, defaultEpochDay: Long): Habit? {
        val id = map["id"]?.toLongOrNull() ?: 0L
        val title = decodeB64(map["title_b64"]).takeIf { it.isNotBlank() } ?: return null
        val notes = decodeB64(map["notes_b64"]).takeIf { it.isNotBlank() }
        val startDay = map["startDay"]?.toLongOrNull() ?: defaultEpochDay
        val activeDays = map["activeDays"]?.toIntOrNull() ?: DaysOfWeekMask.ALL_DAYS
        val momentId = map["momentId"]?.takeIf { it.isNotBlank() }?.toLongOrNull()
        val groupId = map["groupId"]?.takeIf { it.isNotBlank() }?.toLongOrNull()
        val icon = decodeB64(map["icon_b64"]).takeIf { it.isNotBlank() }
        val schedMin = map["schedMin"]?.takeIf { it.isNotBlank() }?.toIntOrNull()
        val repeatMin = map["repeatMin"]?.takeIf { it.isNotBlank() }?.toIntOrNull()
        val fullScreen = map["fullScreen"]?.toBooleanStrictOrNull() ?: true
        val vibrate = map["vibrate"]?.toBooleanStrictOrNull() ?: true
        val type = map["type"]?.let {
            runCatching { CompletionType.valueOf(it) }.getOrNull()
        } ?: CompletionType.BOOLEAN
        val incAmt = map["incAmt"]?.toDoubleOrNull() ?: 1.0
        val incUnit = decodeB64(map["incUnit_b64"]).ifBlank { "Sim" }
        val targetAmt = map["targetAmt"]?.toDoubleOrNull() ?: 1.0
        val targetUnit = decodeB64(map["targetUnit_b64"]).ifBlank { "Sim" }
        val active = map["active"]?.toBooleanStrictOrNull() ?: true

        return Habit(
            id = id,
            title = title,
            notes = notes,
            startEpochDay = startDay,
            activeDaysMask = activeDays,
            momentId = momentId,
            groupId = groupId,
            iconGlyph = icon,
            scheduledMinutesOfDay = schedMin,
            repeatIntervalMinutes = repeatMin,
            aggressiveFullScreen = fullScreen,
            vibrationEnabled = vibrate,
            completionType = type,
            incrementAmount = incAmt,
            incrementUnitLabel = incUnit,
            targetAmount = targetAmt,
            targetUnitLabel = targetUnit,
            isActive = active
        )
    }

    private fun serializeLogMeta(log: HabitLog): String {
        return buildMetaComment(
            prefix = META_LOG_PREFIX,
            pairs = listOf(
                "habitId" to log.habitId.toString(),
                "day" to log.epochDay.toString(),
                "amount" to log.accumulatedAmount.toString(),
                "completed" to log.isCompleted.toString(),
                "dismissed" to log.dismissedForDay.toString(),
                "snoozed" to (log.snoozedUntilEpochMillis?.toString() ?: ""),
                "updatedAt" to log.updatedAtEpochMillis.toString()
            )
        )
    }

    private fun parseLogFromMap(map: Map<String, String>): HabitLog? {
        val habitId = map["habitId"]?.toLongOrNull() ?: return null
        val day = map["day"]?.toLongOrNull() ?: return null
        val amount = map["amount"]?.toDoubleOrNull() ?: 0.0
        val completed = map["completed"]?.toBooleanStrictOrNull() ?: false
        val dismissed = map["dismissed"]?.toBooleanStrictOrNull() ?: false
        val snoozed = map["snoozed"]?.takeIf { it.isNotBlank() }?.toLongOrNull()
        val updatedAt = map["updatedAt"]?.toLongOrNull() ?: System.currentTimeMillis()
        return HabitLog(
            habitId = habitId,
            epochDay = day,
            accumulatedAmount = amount,
            isCompleted = completed,
            dismissedForDay = dismissed,
            snoozedUntilEpochMillis = snoozed,
            updatedAtEpochMillis = updatedAt
        )
    }

    private fun buildMetaComment(prefix: String, pairs: List<Pair<String, String>>): String {
        val payload = pairs.joinToString(";") { (k, v) -> "$k=$v" }
        return "$prefix$payload$META_SUFFIX"
    }

    private fun parseKeyValueMap(raw: String): Map<String, String> {
        val result = linkedMapOf<String, String>()
        raw.split(";").forEach { entry ->
            val eqIdx = entry.indexOf('=')
            if (eqIdx > 0) {
                val key = entry.substring(0, eqIdx).trim()
                val value = entry.substring(eqIdx + 1).trim()
                result[key] = value
            }
        }
        return result
    }

    private fun encodeB64(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun decodeB64(encoded: String?): String {
        if (encoded.isNullOrBlank()) return ""
        return runCatching {
            String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
        }.getOrDefault("")
    }

    private fun formatMinutes(minutesOfDay: Int): String {
        val normalized = ((minutesOfDay % 1440) + 1440) % 1440
        val h = normalized / 60
        val m = normalized % 60
        return String.format(Locale.US, "%02d:%02d", h, m)
    }

    private fun formatIntervalShort(totalMinutes: Int): String {
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}min"
            h > 0 -> "${h}h"
            else -> "${m}min"
        }
    }

    private fun formatDaysMaskShort(mask: Int): String {
        if ((mask and DaysOfWeekMask.ALL_DAYS) == DaysOfWeekMask.ALL_DAYS) return "Todos os dias"
        val labels = mapOf(
            DayOfWeek.MONDAY to "Seg",
            DayOfWeek.TUESDAY to "Ter",
            DayOfWeek.WEDNESDAY to "Qua",
            DayOfWeek.THURSDAY to "Qui",
            DayOfWeek.FRIDAY to "Sex",
            DayOfWeek.SATURDAY to "Sáb",
            DayOfWeek.SUNDAY to "Dom"
        )
        return DayOfWeek.entries
            .filter { DaysOfWeekMask.contains(mask, it) }
            .joinToString(", ") { labels.getValue(it) }
            .ifBlank { "Nenhum" }
    }

    private fun escapeTableCell(text: String): String =
        text.replace("|", "\\|").replace("\n", " ").trim()

    private fun escapeMarkdownText(text: String): String =
        text.replace("\n", " ").trim()

    private fun escapeInlineCode(text: String): String =
        text.replace("`", "'").replace("\n", " ").trim()
}
