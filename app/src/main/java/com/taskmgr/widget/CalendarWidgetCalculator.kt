package com.taskmgr.widget

import com.taskmgr.domain.model.Task
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

data class CalendarDayCell(
    val dayOfMonth: Int,
    val epochDay: Long,
    val isWeekend: Boolean
)

data class CalendarWidgetTaskItem(
    val id: Long,
    val title: String,
    val isCompleted: Boolean,
    val scheduledMinutesOfDay: Int?,
    val urgencyColorArgb: Long
)

object CalendarWidgetCalculator {

    val WEEKDAY_LABELS_PT_BR: List<String> = listOf("d", "s", "t", "q", "q", "s", "s")

    private val MONTH_NAMES_PT_BR: List<String> = listOf(
        "janeiro",
        "fevereiro",
        "março",
        "abril",
        "maio",
        "junho",
        "julho",
        "agosto",
        "setembro",
        "outubro",
        "novembro",
        "dezembro"
    )

    private val MONTH_SHORT_PT_BR: List<String> = listOf(
        "jan", "fev", "mar", "abr", "mai", "jun",
        "jul", "ago", "set", "out", "nov", "dez"
    )

    fun formatMonthTitlePtBr(year: Int, month: Int, currentYear: Int): String {
        val name = MONTH_NAMES_PT_BR.getOrElse((month - 1).coerceIn(0, 11)) { "" }
        return if (year == currentYear) {
            name
        } else {
            val shortName = MONTH_SHORT_PT_BR.getOrElse((month - 1).coerceIn(0, 11)) { name }
            "$shortName $year"
        }
    }

    fun formatSelectedDayHeaderPtBr(selectedEpochDay: Long, todayEpochDay: Long): String {
        return when (selectedEpochDay) {
            todayEpochDay -> "Hoje"
            todayEpochDay + 1L -> "Amanhã"
            todayEpochDay - 1L -> "Ontem"
            else -> {
                val date = LocalDate.ofEpochDay(selectedEpochDay)
                val shortMonth = MONTH_SHORT_PT_BR.getOrElse(date.monthValue - 1) { "" }
                "${date.dayOfMonth} $shortMonth"
            }
        }
    }

    fun formatTaskRowBadge(
        scheduledMinutesOfDay: Int?,
        selectedEpochDay: Long,
        todayEpochDay: Long
    ): String {
        if (scheduledMinutesOfDay != null) {
            val h = (scheduledMinutesOfDay / 60).coerceIn(0, 23)
            val m = (scheduledMinutesOfDay % 60).coerceIn(0, 59)
            return String.format(Locale.US, "%02d:%02d", h, m)
        }
        return when (selectedEpochDay) {
            todayEpochDay -> "Hoje"
            todayEpochDay + 1L -> "Amanhã"
            todayEpochDay - 1L -> "Ontem"
            else -> {
                val date = LocalDate.ofEpochDay(selectedEpochDay)
                String.format(Locale.US, "%02d/%02d", date.dayOfMonth, date.monthValue)
            }
        }
    }

    /**
     * Builds a Sunday-to-Saturday month grid with only the exact number of weeks required (4..6).
     */
    fun buildMonthGrid(year: Int, month: Int): List<List<CalendarDayCell?>> {
        val ym = YearMonth.of(year, month.coerceIn(1, 12))
        val firstDay = ym.atDay(1)
        val leadingBlanks = firstDay.dayOfWeek.value % 7 // Sunday = 0, Monday = 1, ..., Saturday = 6
        val daysInMonth = ym.lengthOfMonth()

        val cells = ArrayList<CalendarDayCell?>(leadingBlanks + daysInMonth + 6)
        repeat(leadingBlanks) { cells.add(null) }
        for (day in 1..daysInMonth) {
            val date = ym.atDay(day)
            val isWeekend = date.dayOfWeek == DayOfWeek.SUNDAY || date.dayOfWeek == DayOfWeek.SATURDAY
            cells.add(
                CalendarDayCell(
                    dayOfMonth = day,
                    epochDay = date.toEpochDay(),
                    isWeekend = isWeekend
                )
            )
        }
        while (cells.size % 7 != 0) {
            cells.add(null)
        }
        return cells.chunked(7)
    }

    /**
     * Computes the set of epochDays in [year]/[month] (from [todayEpochDay] onward)
     * that have at least one pending task.
     */
    fun computeMonthPendingDotEpochDays(
        year: Int,
        month: Int,
        todayEpochDay: Long,
        tasks: List<Task>
    ): Set<Long> {
        val ym = YearMonth.of(year, month.coerceIn(1, 12))
        val monthStart = ym.atDay(1).toEpochDay()
        val monthEnd = ym.atEndOfMonth().toEpochDay()
        val effectiveStart = maxOf(monthStart, todayEpochDay)
        if (effectiveStart > monthEnd) return emptySet()

        val result = HashSet<Long>()
        for (task in tasks) {
            if (task.isCompleted || !task.hasDate) continue
            val rangeStart = maxOf(task.startEpochDay, effectiveStart)
            val rangeEnd = minOf(task.endEpochDay, monthEnd)
            if (rangeStart <= rangeEnd) {
                for (day in rangeStart..rangeEnd) {
                    if (task.isActiveOnDay(day)) {
                        result.add(day)
                    }
                }
            }
        }
        return result
    }

    /**
     * Returns all scheduled tasks active on [selectedEpochDay], sorted cleanly with
     * pending tasks first, then by scheduled time, urgency, and id.
     */
    fun tasksForDay(selectedEpochDay: Long, tasks: List<Task>): List<CalendarWidgetTaskItem> {
        return tasks
            .asSequence()
            .filter { task -> task.isActiveOnDay(selectedEpochDay) }
            .sortedWith(
                compareBy<Task>(
                    { it.isCompleted },
                    { it.scheduledMinutesOfDay ?: 1440 },
                    { it.urgency.sortOrder },
                    { it.id }
                )
            )
            .map { task ->
                CalendarWidgetTaskItem(
                    id = task.id,
                    title = task.title,
                    isCompleted = task.isCompleted,
                    scheduledMinutesOfDay = task.scheduledMinutesOfDay,
                    urgencyColorArgb = task.urgency.colorArgb
                )
            }
            .toList()
    }

    fun serializeDotDays(days: Set<Long>): String =
        days.sorted().joinToString(",")

    fun deserializeDotDays(csv: String): Set<Long> {
        if (csv.isBlank()) return emptySet()
        return csv.split(',')
            .mapNotNull { it.trim().toLongOrNull() }
            .toSet()
    }

    fun serializeTasks(items: List<CalendarWidgetTaskItem>): String {
        if (items.isEmpty()) return ""
        return items.joinToString("\n") { item ->
            val escapedTitle = item.title
                .replace("\\", "\\\\")
                .replace("|", "\\p")
                .replace("\n", " ")
                .replace("\r", " ")
            listOf(
                item.id.toString(),
                if (item.isCompleted) "1" else "0",
                (item.scheduledMinutesOfDay ?: -1).toString(),
                item.urgencyColorArgb.toString(),
                escapedTitle
            ).joinToString("|")
        }
    }

    fun deserializeTasks(raw: String): List<CalendarWidgetTaskItem> {
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence().mapNotNull { line ->
            val parts = line.split('|', limit = 5)
            if (parts.size < 5) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val completed = parts[1] == "1"
            val mins = parts[2].toIntOrNull()?.takeIf { it >= 0 }
            val color = parts[3].toLongOrNull() ?: 0xFF818CF8L
            val title = parts[4]
                .replace("\\p", "|")
                .replace("\\\\", "\\")
            CalendarWidgetTaskItem(
                id = id,
                title = title,
                isCompleted = completed,
                scheduledMinutesOfDay = mins,
                urgencyColorArgb = color
            )
        }.toList()
    }
}
