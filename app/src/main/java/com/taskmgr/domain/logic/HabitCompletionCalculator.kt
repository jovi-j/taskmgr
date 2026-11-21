package com.taskmgr.domain.logic

import com.taskmgr.domain.model.CompletionType
import com.taskmgr.domain.model.FullScreenReminderPayload
import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.HabitLog
import com.taskmgr.domain.model.ReminderItemType
import com.taskmgr.domain.model.Task
import kotlin.math.abs
import kotlin.math.floor

object HabitCompletionCalculator {
    const val EPSILON: Double = 1e-6

    fun isCompleted(accumulatedAmount: Double, targetAmount: Double): Boolean {
        require(targetAmount > 0.0) { "targetAmount must be > 0" }
        return accumulatedAmount + EPSILON >= targetAmount
    }

    fun clampAmount(value: Double): Double = if (value < 0.0) 0.0 else value

    fun formatProgressText(habit: Habit, accumulatedAmount: Double): String {
        return when (habit.completionType) {
            CompletionType.BOOLEAN -> {
                val done = if (isCompleted(accumulatedAmount, habit.targetAmount)) 1 else 0
                "$done / 1 ${habit.targetUnitLabel.ifBlank { "Sim" }}"
            }

            CompletionType.COUNT_PER_DAY -> {
                val current = formatCleanNumber(accumulatedAmount)
                val target = formatCleanNumber(habit.targetAmount)
                val unit = habit.targetUnitLabel.ifBlank { "Sim" }
                "$current / $target $unit"
            }

            CompletionType.CUSTOM_UNIT -> {
                val unitSuffix = extractUnitSuffix(habit.targetUnitLabel, habit.incrementUnitLabel)
                val hasDecimal = !isWholeNumber(accumulatedAmount) ||
                    !isWholeNumber(habit.targetAmount) ||
                    !isWholeNumber(habit.incrementAmount) ||
                    (habit.incrementUnitLabel.trim().startsWith("+") && habit.targetUnitLabel.trim().any { it.isDigit() })
                val currentStr = if (hasDecimal) {
                    accumulatedAmount.toString()
                } else {
                    formatCleanNumber(accumulatedAmount)
                }
                val targetStr = if (currentStr.contains('.')) {
                    habit.targetAmount.toString()
                } else {
                    formatCleanNumber(habit.targetAmount)
                }
                if (unitSuffix.isNotEmpty()) "$currentStr / $targetStr $unitSuffix" else "$currentStr / $targetStr"
            }
        }
    }

    fun formatIncrementButtonLabel(habit: Habit): String {
        return when (habit.completionType) {
            CompletionType.BOOLEAN -> "Sim (Concluir)"
            CompletionType.COUNT_PER_DAY -> {
                val amount = formatCleanNumber(habit.incrementAmount)
                val unit = habit.incrementUnitLabel.ifBlank { "Sim" }
                if (unit.startsWith("+")) unit else "+$amount $unit"
            }
            CompletionType.CUSTOM_UNIT -> {
                val label = habit.incrementUnitLabel.trim()
                if (label.startsWith("+")) {
                    label
                } else {
                    val amountStr = formatCleanNumber(habit.incrementAmount)
                    val stripped = label.removePrefix(amountStr).trim()
                    val unitPart = if (stripped.isEmpty()) label else stripped
                    if (label.startsWith(amountStr)) "+$label" else "+$amountStr $unitPart"
                }
            }
        }
    }

    fun buildTaskPayload(
        task: Task,
        epochDay: Long,
        momentName: String?,
        isAdvanceReminder: Boolean = false
    ): FullScreenReminderPayload {
        val scheduleSummary = buildString {
            if (task.hasDate) {
                val date = java.time.LocalDate.ofEpochDay(task.startEpochDay)
                append(
                    String.format(
                        java.util.Locale.US,
                        "%02d/%02d/%04d",
                        date.dayOfMonth,
                        date.monthValue,
                        date.year
                    )
                )
            }
            task.scheduledMinutesOfDay?.let { mins ->
                val h = (mins / 60).coerceIn(0, 23)
                val m = (mins % 60).coerceIn(0, 59)
                if (isNotEmpty()) append(" às ")
                append(String.format(java.util.Locale.US, "%02d:%02d", h, m))
            }
        }.takeIf { it.isNotBlank() }

        return FullScreenReminderPayload(
            itemType = ReminderItemType.TASK,
            itemId = task.id,
            epochDay = epochDay,
            title = task.title,
            momentName = momentName,
            progressText = scheduleSummary,
            incrementButtonLabel = "Sim",
            vibrationEnabled = task.vibrationEnabled,
            showSnoozeOptions = false,
            acknowledgeOnly = isAdvanceReminder
        )
    }

    fun buildHabitPayload(
        habit: Habit,
        log: HabitLog?,
        epochDay: Long,
        momentName: String?
    ): FullScreenReminderPayload {
        val current = log?.accumulatedAmount ?: 0.0
        return FullScreenReminderPayload(
            itemType = ReminderItemType.HABIT,
            itemId = habit.id,
            epochDay = epochDay,
            title = habit.title,
            momentName = momentName,
            progressText = formatProgressText(habit, current),
            incrementButtonLabel = formatIncrementButtonLabel(habit),
            vibrationEnabled = habit.vibrationEnabled
        )
    }

    fun remainingAmount(habit: Habit, accumulatedAmount: Double): Double {
        return (habit.targetAmount - accumulatedAmount).coerceAtLeast(0.0)
    }

    fun progressFraction(habit: Habit, accumulatedAmount: Double): Float {
        if (habit.targetAmount <= 0.0) return 0f
        return when (habit.completionType) {
            CompletionType.BOOLEAN -> if (isCompleted(accumulatedAmount, habit.targetAmount)) 1f else 0f
            CompletionType.COUNT_PER_DAY, CompletionType.CUSTOM_UNIT -> {
                if (isCompleted(accumulatedAmount, habit.targetAmount)) {
                    1f
                } else {
                    (accumulatedAmount / habit.targetAmount).toFloat().coerceIn(0f, 1f)
                }
            }
        }
    }

    fun formatRemainingText(habit: Habit, accumulatedAmount: Double): String {
        if (isCompleted(accumulatedAmount, habit.targetAmount)) {
            return "Concluído!"
        }
        return when (habit.completionType) {
            CompletionType.BOOLEAN -> "Falta concluir"
            CompletionType.COUNT_PER_DAY -> {
                val remaining = remainingAmount(habit, accumulatedAmount)
                val remStr = formatCompactDecimal(remaining)
                val isOne = isApproximatelyOne(remaining)
                val unit = if (isOne) {
                    val incLabel = habit.incrementUnitLabel.trim()
                    when {
                        incLabel.isNotEmpty() && !incLabel.startsWith("+") && incLabel.none { it.isDigit() } -> incLabel
                        habit.targetUnitLabel.equals("vezes", ignoreCase = true) -> "vez"
                        else -> habit.targetUnitLabel.ifBlank { "vez" }
                    }
                } else {
                    habit.targetUnitLabel.ifBlank { "vezes" }
                }
                val verb = if (isOne) "Falta" else "Faltam"
                "$verb $remStr $unit"
            }
            CompletionType.CUSTOM_UNIT -> {
                val remaining = remainingAmount(habit, accumulatedAmount)
                val remStr = formatCompactDecimal(remaining)
                val unitSuffix = extractUnitSuffix(habit.targetUnitLabel, habit.incrementUnitLabel)
                val verb = if (isApproximatelyOne(remaining)) "Falta" else "Faltam"
                if (unitSuffix.isNotEmpty()) "$verb $remStr $unitSuffix" else "$verb $remStr"
            }
        }
    }

    fun formatWidgetProgressText(habit: Habit, accumulatedAmount: Double): String {
        return when (habit.completionType) {
            CompletionType.BOOLEAN -> {
                val done = if (isCompleted(accumulatedAmount, habit.targetAmount)) 1 else 0
                "$done/1"
            }
            CompletionType.COUNT_PER_DAY -> {
                val current = formatCompactDecimal(clampAmount(accumulatedAmount))
                val target = formatCompactDecimal(habit.targetAmount)
                "$current/$target"
            }
            CompletionType.CUSTOM_UNIT -> {
                val current = formatCompactDecimal(clampAmount(accumulatedAmount))
                val target = formatCompactDecimal(habit.targetAmount)
                val unitSuffix = extractUnitSuffix(habit.targetUnitLabel, habit.incrementUnitLabel)
                if (unitSuffix.isNotEmpty()) "$current/$target $unitSuffix" else "$current/$target"
            }
        }
    }

    fun formatWidgetDayLabel(streakDays: Int, isCompletedToday: Boolean): String {
        val currentDayNumber = if (isCompletedToday) {
            streakDays.coerceAtLeast(1)
        } else {
            streakDays.coerceAtLeast(0) + 1
        }
        return "Dia $currentDayNumber"
    }

    fun formatCompactIncrementHint(habit: Habit): String {
        return when (habit.completionType) {
            CompletionType.BOOLEAN -> "Sim"
            CompletionType.COUNT_PER_DAY, CompletionType.CUSTOM_UNIT -> formatIncrementButtonLabel(habit)
        }
    }

    private fun extractUnitSuffix(targetUnitLabel: String, incrementUnitLabel: String): String {
        val cleanedTarget = targetUnitLabel.trim()
        val withoutLeadingNumber = cleanedTarget
            .replace(Regex("^[0-9]+([.,][0-9]+)?\\s*"), "")
            .trim()
        if (withoutLeadingNumber.isNotEmpty()) return withoutLeadingNumber

        val cleanedIncrement = incrementUnitLabel.trim()
            .removePrefix("+")
            .replace(Regex("^[0-9]+([.,][0-9]+)?\\s*"), "")
            .trim()
        return cleanedIncrement
    }

    fun formatCleanNumber(value: Double): String {
        return if (isWholeNumber(value)) {
            floor(value + EPSILON).toLong().toString()
        } else {
            value.toString()
        }
    }

    private fun formatCompactDecimal(value: Double): String {
        if (isWholeNumber(value)) {
            return floor(value + EPSILON).toLong().toString()
        }
        val rounded = kotlin.math.round(value * 100.0) / 100.0
        return if (isWholeNumber(rounded)) {
            floor(rounded + EPSILON).toLong().toString()
        } else {
            rounded.toString()
        }
    }

    private fun isApproximatelyOne(value: Double): Boolean = abs(value - 1.0) < EPSILON

    private fun isWholeNumber(value: Double): Boolean =
        abs(value - kotlin.math.round(value)) < EPSILON
}
