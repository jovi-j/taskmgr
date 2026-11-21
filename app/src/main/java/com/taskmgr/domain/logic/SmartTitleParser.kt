package com.taskmgr.domain.logic

import com.taskmgr.domain.model.HabitGroup
import com.taskmgr.domain.model.Moment
import com.taskmgr.domain.model.TaskUrgency
import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate

enum class SmartTokenKind {
    START_DATE,
    END_DATE,
    EXACT_TIME,
    TASK_WEEKDAYS,
    HABIT_FREQUENCY,
    PERIOD,
    URGENCY,
    GROUP
}

data class SmartToken(
    val kind: SmartTokenKind,
    val startIndex: Int,
    val endIndex: Int,
    val rawText: String
)

data class DismissedSmartToken(
    val startIndex: Int,
    val endIndex: Int,
    val rawText: String
)

sealed interface SmartHabitFrequencySpec {
    data class CountPerDay(val count: Int) : SmartHabitFrequencySpec
    data class CustomUnit(
        val incrementValueText: String,
        val incrementUnitText: String,
        val targetValueText: String,
        val targetUnitText: String
    ) : SmartHabitFrequencySpec
}

data class SmartTaskParseResult(
    val originalText: String,
    val cleanTitle: String,
    val tokens: List<SmartToken>,
    val startEpochDay: Long? = null,
    val endEpochDay: Long? = null,
    val weekdaysMask: Int? = null,
    val scheduledMinutesOfDay: Int? = null,
    val urgency: TaskUrgency? = null
)

data class SmartHabitParseResult(
    val originalText: String,
    val cleanTitle: String,
    val tokens: List<SmartToken>,
    val startEpochDay: Long? = null,
    val scheduledMinutesOfDay: Int? = null,
    val momentId: Long? = null,
    val groupId: Long? = null,
    val frequencySpec: SmartHabitFrequencySpec? = null
)

data class SmartTextEditOutcome(
    val text: String,
    val cursorOffset: Int?,
    val dismissedTokens: List<DismissedSmartToken>,
    val wasTokenDismissed: Boolean
)

object SmartTitleParser {

    private const val DAY_NAME_PATTERN =
        "(?:segunda(?:-feira)?|terca(?:-feira)?|quarta(?:-feira)?|quinta(?:-feira)?|sexta(?:-feira)?|sabado|domingo)"

    private val END_DATE_REGEX = Regex(
        "\\bate\\s+(?:o\\s+dia\\s+|dia\\s+)?(\\d{1,2}/\\d{1,2}(?:/\\d{2,4})?|depois\\s+de\\s+amanha|amanha|hoje)\\b"
    )

    private val START_DATE_REGEX = Regex(
        "\\b(\\d{1,2}/\\d{1,2}/\\d{2,4}|\\d{1,2}/\\d{1,2}|depois\\s+de\\s+amanha|amanha|hoje|ontem)\\b"
    )

    private val EXACT_TIME_COLON_REGEX = Regex(
        "\\b(?:as\\s+)?(\\d{1,2}):(\\d{2})(?:\\s*h(?:oras?)?)?\\b"
    )

    private val EXACT_TIME_H_REGEX = Regex(
        "\\bas\\s+(\\d{1,2})\\s*h(?:\\s*(\\d{2}))?\\b"
    )

    private val TASK_WEEKDAYS_REGEX = Regex(
        "\\btodas?(?:\\s+as)?\\s+$DAY_NAME_PATTERN(?:\\s*(?:,\\s*e\\b|,|\\be\\b)\\s*$DAY_NAME_PATTERN)*\\b"
    )

    private val SINGLE_DAY_NAME_REGEX = Regex("\\b(segunda|terca|quarta|quinta|sexta|sabado|domingo)")

    private val URGENCY_REGEX = Regex(
        "\\burgencia\\s+(muito\\s+alta|alta|media|baixa)\\b"
    )

    private val HABIT_CUSTOM_UNIT_REGEX = Regex(
        "\\b(\\d+(?:[.,]\\d+)?)\\s+([a-z]+(?:\\s+[a-z]+)?)\\s+de\\s+(\\d+(?:[.,]\\d+)?)\\s+([a-z]+(?:\\s+[a-z]+)?)\\s+(?:ao|por)\\s+dia\\b"
    )

    private val HABIT_COUNT_PER_DAY_REGEX = Regex(
        "\\b(\\d+)\\s*(?:x|vez(?:es)?)\\s+(?:ao|por)\\s+dia\\b"
    )

    private val HABIT_PERIOD_REGEX = Regex(
        "\\b(?:pela\\s+(manha|tarde|noite)|(todo\\s+o\\s+dia))\\b"
    )

    private val WHITESPACE_COLLAPSE_REGEX = Regex("\\s{2,}")

    /**
     * Normalizes [text] to lowercase without diacritics while strictly preserving 1-to-1 character indices.
     */
    fun normalizePreservingLength(text: String): String {
        if (text.isEmpty()) return ""
        val sb = StringBuilder(text.length)
        for (i in 0 until text.length) {
            sb.append(normalizeChar(text[i]))
        }
        return sb.toString()
    }

    private fun normalizeChar(ch: Char): Char {
        if (ch.code < 128) return ch.lowercaseChar()
        val decomposed = Normalizer.normalize(ch.toString(), Normalizer.Form.NFD)
        val base = decomposed.firstOrNull {
            Character.getType(it) != Character.NON_SPACING_MARK.toInt()
        } ?: ch
        return base.lowercaseChar()
    }

    fun findDefaultHabitMomentId(moments: List<Moment>): Long? {
        return moments.firstOrNull { moment ->
            val norm = normalizePreservingLength(moment.name).trim().replace(WHITESPACE_COLLAPSE_REGEX, " ")
            norm == "todo o dia" || norm == "todo dia"
        }?.id
    }

    fun parseTaskTitle(
        text: String,
        todayEpochDay: Long,
        dismissedTokens: List<DismissedSmartToken> = emptyList()
    ): SmartTaskParseResult {
        if (text.isBlank()) {
            return SmartTaskParseResult(
                originalText = text,
                cleanTitle = text.trim(),
                tokens = emptyList()
            )
        }

        val normalized = normalizePreservingLength(text)
        val claimed = BooleanArray(text.length)
        val tokens = mutableListOf<SmartToken>()

        var startEpochDay: Long? = null
        var endEpochDay: Long? = null
        var weekdaysMask: Int? = null
        var scheduledMinutesOfDay: Int? = null
        var urgency: TaskUrgency? = null

        fun canClaim(start: Int, end: Int): Boolean {
            if (start < 0 || end > text.length || start >= end) return false
            val raw = text.substring(start, end)
            if (dismissedTokens.any { it.startIndex == start && it.endIndex == end && it.rawText == raw }) {
                return false
            }
            for (i in start until end) {
                if (claimed[i]) return false
            }
            return true
        }

        fun claim(kind: SmartTokenKind, start: Int, end: Int) {
            for (i in start until end) {
                claimed[i] = true
            }
            tokens.add(
                SmartToken(
                    kind = kind,
                    startIndex = start,
                    endIndex = end,
                    rawText = text.substring(start, end)
                )
            )
        }

        // 1. End Date ("até [data]") - must run before Start Date
        for (match in END_DATE_REGEX.findAll(normalized)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (!canClaim(start, end)) continue
            val dateGroup = match.groupValues[1]
            val parsedEpochDay = parseDateExpression(dateGroup, todayEpochDay) ?: continue
            endEpochDay = parsedEpochDay
            claim(SmartTokenKind.END_DATE, start, end)
        }

        // 2. Task Weekdays ("toda segunda e terça", "toda quarta, quinta e sexta")
        for (match in TASK_WEEKDAYS_REGEX.findAll(normalized)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (!canClaim(start, end)) continue
            val days = mutableSetOf<DayOfWeek>()
            for (dayMatch in SINGLE_DAY_NAME_REGEX.findAll(match.value)) {
                when (dayMatch.groupValues[1]) {
                    "segunda" -> days.add(DayOfWeek.MONDAY)
                    "terca" -> days.add(DayOfWeek.TUESDAY)
                    "quarta" -> days.add(DayOfWeek.WEDNESDAY)
                    "quinta" -> days.add(DayOfWeek.THURSDAY)
                    "sexta" -> days.add(DayOfWeek.FRIDAY)
                    "sabado" -> days.add(DayOfWeek.SATURDAY)
                    "domingo" -> days.add(DayOfWeek.SUNDAY)
                }
            }
            if (days.isNotEmpty()) {
                weekdaysMask = DaysOfWeekMask.fromDays(days)
                claim(SmartTokenKind.TASK_WEEKDAYS, start, end)
            }
        }

        // 3. Urgency ("urgência muito alta | alta | média | baixa")
        for (match in URGENCY_REGEX.findAll(normalized)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (!canClaim(start, end)) continue
            val level = match.groupValues[1].replace(WHITESPACE_COLLAPSE_REGEX, " ")
            val parsedUrgency = when (level) {
                "muito alta" -> TaskUrgency.VERY_HIGH
                "alta" -> TaskUrgency.HIGH
                "media" -> TaskUrgency.MEDIUM
                "baixa" -> TaskUrgency.LOW
                else -> null
            } ?: continue
            urgency = parsedUrgency
            claim(SmartTokenKind.URGENCY, start, end)
        }

        // 4. Exact Time ("às 15:00", "ás 15:00", "15:00", "às 15h")
        for (match in EXACT_TIME_COLON_REGEX.findAll(normalized)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (!canClaim(start, end)) continue
            val h = match.groupValues[1].toIntOrNull() ?: continue
            val m = match.groupValues[2].toIntOrNull() ?: continue
            if (h in 0..23 && m in 0..59) {
                scheduledMinutesOfDay = h * 60 + m
                claim(SmartTokenKind.EXACT_TIME, start, end)
            }
        }
        for (match in EXACT_TIME_H_REGEX.findAll(normalized)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (!canClaim(start, end)) continue
            val h = match.groupValues[1].toIntOrNull() ?: continue
            val m = match.groupValues[2].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
            if (h in 0..23 && m in 0..59) {
                scheduledMinutesOfDay = h * 60 + m
                claim(SmartTokenKind.EXACT_TIME, start, end)
            }
        }

        // 5. Start Date ("23/11/2026", "amanhã", "depois de amanhã", "hoje")
        for (match in START_DATE_REGEX.findAll(normalized)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (!canClaim(start, end)) continue
            val parsedEpochDay = parseDateExpression(match.groupValues[1], todayEpochDay) ?: continue
            startEpochDay = parsedEpochDay
            claim(SmartTokenKind.START_DATE, start, end)
        }

        val sortedTokens = tokens.sortedBy { it.startIndex }
        return SmartTaskParseResult(
            originalText = text,
            cleanTitle = buildCleanTitle(text, sortedTokens),
            tokens = sortedTokens,
            startEpochDay = startEpochDay,
            endEpochDay = endEpochDay,
            weekdaysMask = weekdaysMask,
            scheduledMinutesOfDay = scheduledMinutesOfDay,
            urgency = urgency
        )
    }

    fun parseHabitTitle(
        text: String,
        todayEpochDay: Long,
        moments: List<Moment>,
        habitGroups: List<HabitGroup>,
        dismissedTokens: List<DismissedSmartToken> = emptyList()
    ): SmartHabitParseResult {
        if (text.isBlank()) {
            return SmartHabitParseResult(
                originalText = text,
                cleanTitle = text.trim(),
                tokens = emptyList()
            )
        }

        val normalized = normalizePreservingLength(text)
        val claimed = BooleanArray(text.length)
        val tokens = mutableListOf<SmartToken>()

        var startEpochDay: Long? = null
        var scheduledMinutesOfDay: Int? = null
        var momentId: Long? = null
        var groupId: Long? = null
        var frequencySpec: SmartHabitFrequencySpec? = null

        fun canClaim(start: Int, end: Int): Boolean {
            if (start < 0 || end > text.length || start >= end) return false
            val raw = text.substring(start, end)
            if (dismissedTokens.any { it.startIndex == start && it.endIndex == end && it.rawText == raw }) {
                return false
            }
            for (i in start until end) {
                if (claimed[i]) return false
            }
            return true
        }

        fun claim(kind: SmartTokenKind, start: Int, end: Int) {
            for (i in start until end) {
                claimed[i] = true
            }
            tokens.add(
                SmartToken(
                    kind = kind,
                    startIndex = start,
                    endIndex = end,
                    rawText = text.substring(start, end)
                )
            )
        }

        // 1. Habit Custom Unit Frequency ("250 ml de 2500 ml ao dia", "10 capitulos de 100 capítulos ao dia")
        for (match in HABIT_CUSTOM_UNIT_REGEX.findAll(normalized)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (!canClaim(start, end)) continue
            val incGroup = match.groups[1] ?: continue
            val incUnitGroup = match.groups[2] ?: continue
            val tarGroup = match.groups[3] ?: continue
            val tarUnitGroup = match.groups[4] ?: continue

            val incValText = text.substring(incGroup.range.first, incGroup.range.last + 1).replace(',', '.')
            val tarValText = text.substring(tarGroup.range.first, tarGroup.range.last + 1).replace(',', '.')
            val incDouble = incValText.toDoubleOrNull()?.takeIf { it > 0.0 } ?: continue
            val tarDouble = tarValText.toDoubleOrNull()?.takeIf { it > 0.0 } ?: continue
            if (incDouble <= 0.0 || tarDouble <= 0.0) continue

            val origIncUnit = text.substring(incUnitGroup.range.first, incUnitGroup.range.last + 1).trim()
            val origTarUnit = text.substring(tarUnitGroup.range.first, tarUnitGroup.range.last + 1).trim()
            // Preserve the accented version if one of the two unit occurrences has accents (e.g. "capitulos" vs "capítulos")
            val bestUnit = if (origTarUnit.any { it.code >= 128 }) origTarUnit else origIncUnit

            frequencySpec = SmartHabitFrequencySpec.CustomUnit(
                incrementValueText = incValText.removeSuffix(".0"),
                incrementUnitText = bestUnit,
                targetValueText = tarValText.removeSuffix(".0"),
                targetUnitText = origTarUnit
            )
            claim(SmartTokenKind.HABIT_FREQUENCY, start, end)
        }

        // 2. Habit Count Per Day Frequency ("2x ao dia", "3 vezes ao dia")
        for (match in HABIT_COUNT_PER_DAY_REGEX.findAll(normalized)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (!canClaim(start, end)) continue
            val count = match.groupValues[1].toIntOrNull()?.takeIf { it >= 1 } ?: continue
            frequencySpec = SmartHabitFrequencySpec.CountPerDay(count)
            claim(SmartTokenKind.HABIT_FREQUENCY, start, end)
        }

        // 3. Habit Period ("pela manhã", "pela tarde", "pela noite", "todo o dia")
        for (match in HABIT_PERIOD_REGEX.findAll(normalized)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (!canClaim(start, end)) continue
            val targetName = match.groupValues[1].ifEmpty { match.groupValues[2] }
                .replace(WHITESPACE_COLLAPSE_REGEX, " ")
                .trim()
            val matchedMoment = moments.firstOrNull { moment ->
                val normMoment = normalizePreservingLength(moment.name)
                    .replace(WHITESPACE_COLLAPSE_REGEX, " ")
                    .trim()
                normMoment == targetName
            } ?: continue
            momentId = matchedMoment.id
            claim(SmartTokenKind.PERIOD, start, end)
        }

        // 4. Habit Group ("grupo casa", "grupo saúde")
        val sortedGroups = habitGroups
            .map { group ->
                val normName = normalizePreservingLength(group.name.trim())
                    .replace(WHITESPACE_COLLAPSE_REGEX, " ")
                group to normName
            }
            .filter { (_, normName) -> normName.isNotEmpty() }
            .sortedByDescending { (_, normName) -> normName.length }

        for ((group, normName) in sortedGroups) {
            val escapedWords = normName.split(" ").joinToString("\\s+") { Regex.escape(it) }
            val groupRegex = Regex("\\bgrupo\\s+$escapedWords\\b")
            for (match in groupRegex.findAll(normalized)) {
                val start = match.range.first
                val end = match.range.last + 1
                if (!canClaim(start, end)) continue
                groupId = group.id
                claim(SmartTokenKind.GROUP, start, end)
            }
        }

        // 5. Exact Time ("às 08:00", "08:00", "às 8h")
        for (match in EXACT_TIME_COLON_REGEX.findAll(normalized)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (!canClaim(start, end)) continue
            val h = match.groupValues[1].toIntOrNull() ?: continue
            val m = match.groupValues[2].toIntOrNull() ?: continue
            if (h in 0..23 && m in 0..59) {
                scheduledMinutesOfDay = h * 60 + m
                claim(SmartTokenKind.EXACT_TIME, start, end)
            }
        }
        for (match in EXACT_TIME_H_REGEX.findAll(normalized)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (!canClaim(start, end)) continue
            val h = match.groupValues[1].toIntOrNull() ?: continue
            val m = match.groupValues[2].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
            if (h in 0..23 && m in 0..59) {
                scheduledMinutesOfDay = h * 60 + m
                claim(SmartTokenKind.EXACT_TIME, start, end)
            }
        }

        // 6. Start Date ("21/11/2026", "amanhã", "depois de amanhã", "hoje", "ontem")
        for (match in START_DATE_REGEX.findAll(normalized)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (!canClaim(start, end)) continue
            val parsedEpochDay = parseDateExpression(match.groupValues[1], todayEpochDay) ?: continue
            startEpochDay = parsedEpochDay
            claim(SmartTokenKind.START_DATE, start, end)
        }

        val sortedTokens = tokens.sortedBy { it.startIndex }
        return SmartHabitParseResult(
            originalText = text,
            cleanTitle = buildCleanTitle(text, sortedTokens),
            tokens = sortedTokens,
            startEpochDay = startEpochDay,
            scheduledMinutesOfDay = scheduledMinutesOfDay,
            momentId = momentId,
            groupId = groupId,
            frequencySpec = frequencySpec
        )
    }

    /**
     * Handles an edit from [oldText] to [newText].
     * If the user pressed Backspace/Delete right in front of (at the end of) an active [SmartToken],
     * the literal text is preserved in the field, that token is added to [dismissedTokens],
     * and `wasTokenDismissed = true` is returned so the caller can revert the smart change.
     */
    fun processTextEdit(
        oldText: String,
        newText: String,
        activeTokens: List<SmartToken>,
        dismissedTokens: List<DismissedSmartToken>
    ): SmartTextEditOutcome {
        if (oldText.length - newText.length == 1) {
            val hitToken = activeTokens.firstOrNull { token ->
                token.endIndex in 1..oldText.length &&
                    oldText.removeRange(token.endIndex - 1, token.endIndex) == newText
            }
            if (hitToken != null) {
                val newDismissed = DismissedSmartToken(
                    startIndex = hitToken.startIndex,
                    endIndex = hitToken.endIndex,
                    rawText = hitToken.rawText
                )
                return SmartTextEditOutcome(
                    text = oldText,
                    cursorOffset = hitToken.endIndex,
                    dismissedTokens = (dismissedTokens + newDismissed).distinct(),
                    wasTokenDismissed = true
                )
            }
        }

        if (dismissedTokens.isEmpty()) {
            return SmartTextEditOutcome(
                text = newText,
                cursorOffset = null,
                dismissedTokens = emptyList(),
                wasTokenDismissed = false
            )
        }

        val delta = newText.length - oldText.length
        var commonPrefix = 0
        val minLen = minOf(oldText.length, newText.length)
        while (commonPrefix < minLen && oldText[commonPrefix] == newText[commonPrefix]) {
            commonPrefix++
        }

        val adjustedDismissed = dismissedTokens.mapNotNull { item ->
            when {
                item.endIndex <= commonPrefix &&
                    item.endIndex <= newText.length &&
                    newText.substring(item.startIndex, item.endIndex) == item.rawText -> item
                item.startIndex >= commonPrefix -> {
                    val shiftedStart = item.startIndex + delta
                    val shiftedEnd = item.endIndex + delta
                    if (shiftedStart >= 0 &&
                        shiftedEnd <= newText.length &&
                        newText.substring(shiftedStart, shiftedEnd) == item.rawText
                    ) {
                        item.copy(startIndex = shiftedStart, endIndex = shiftedEnd)
                    } else {
                        null
                    }
                }
                else -> null
            }
        }

        return SmartTextEditOutcome(
            text = newText,
            cursorOffset = null,
            dismissedTokens = adjustedDismissed,
            wasTokenDismissed = false
        )
    }

    fun buildCleanTitle(text: String, activeTokens: List<SmartToken>): String {
        if (activeTokens.isEmpty()) {
            return text.trim().replace(WHITESPACE_COLLAPSE_REGEX, " ")
        }
        val sorted = activeTokens.sortedBy { it.startIndex }
        val sb = StringBuilder(text.length)
        var cursor = 0
        for (token in sorted) {
            if (token.startIndex > cursor) {
                sb.append(text, cursor, token.startIndex)
            }
            cursor = maxOf(cursor, token.endIndex)
        }
        if (cursor < text.length) {
            sb.append(text, cursor, text.length)
        }
        return sb.toString()
            .replace(WHITESPACE_COLLAPSE_REGEX, " ")
            .trim()
    }

    private fun parseDateExpression(normalizedExpr: String, todayEpochDay: Long): Long? {
        val cleaned = normalizedExpr.trim().replace(WHITESPACE_COLLAPSE_REGEX, " ")
        return when (cleaned) {
            "hoje" -> todayEpochDay
            "amanha" -> todayEpochDay + 1L
            "depois de amanha" -> todayEpochDay + 2L
            "ontem" -> todayEpochDay - 1L
            else -> {
                val parts = cleaned.split("/")
                if (parts.size !in 2..3) return null
                val day = parts[0].toIntOrNull() ?: return null
                val month = parts[1].toIntOrNull() ?: return null
                val refDate = LocalDate.ofEpochDay(todayEpochDay)
                val year = if (parts.size == 3) {
                    val rawYear = parts[2].toIntOrNull() ?: return null
                    when {
                        parts[2].length == 2 -> 2000 + rawYear
                        rawYear in 1970..2100 -> rawYear
                        else -> return null
                    }
                } else {
                    refDate.year
                }
                runCatching { LocalDate.of(year, month, day).toEpochDay() }.getOrNull()
            }
        }
    }
}
