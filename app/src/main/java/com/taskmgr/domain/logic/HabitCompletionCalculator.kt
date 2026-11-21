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

    fun buildTaskPayload(task: Task, epochDay: Long, momentName: String?): FullScreenReminderPayload {
        return FullScreenReminderPayload(
            itemType = ReminderItemType.TASK,
            itemId = task.id,
            epochDay = epochDay,
            title = task.title,
            momentName = momentName,
            progressText = null,
            incrementButtonLabel = "Sim (Concluir)",
            vibrationEnabled = task.vibrationEnabled
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

    private fun isWholeNumber(value: Double): Boolean =
        abs(value - kotlin.math.round(value)) < EPSILON
}
