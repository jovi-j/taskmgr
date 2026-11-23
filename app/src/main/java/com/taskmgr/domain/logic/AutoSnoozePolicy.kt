package com.taskmgr.domain.logic

import com.taskmgr.domain.model.Moment
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

sealed interface AutoSnoozeDecision {
    data class SnoozeForMinutes(
        val nextCount: Int,
        val snoozeMinutes: Long,
        val triggerAtEpochMillis: Long,
        val targetEpochDay: Long
    ) : AutoSnoozeDecision

    data class ReachedNextDayStopNotifying(
        val currentEpochDay: Long,
        val wouldReachEpochDay: Long
    ) : AutoSnoozeDecision
}

object AutoSnoozePolicy {

    const val RING_TIMEOUT_MINUTES: Long = 3L
    const val RING_TIMEOUT_MILLIS: Long = RING_TIMEOUT_MINUTES * 60_000L
    const val BASE_SNOOZE_MINUTES: Long = 5L
    private const val MAX_SHIFT: Int = 20

    fun computeSnoozeMinutes(consecutiveAutoSnoozeCount: Int): Long {
        val shift = consecutiveAutoSnoozeCount.coerceIn(0, MAX_SHIFT)
        return BASE_SNOOZE_MINUTES shl shift
    }

    /**
     * Evaluates the exponential backoff auto-snooze after a reminder rings for 3 minutes without interaction:
     * - 1st timeout (previousCount = 0) -> 5 minutes
     * - 2nd timeout (previousCount = 1) -> 10 minutes
     * - 3rd timeout (previousCount = 2) -> 20 minutes
     * - 4th timeout (previousCount = 3) -> 40 minutes
     * ...
     *
     * Guardrails so a late auto-snooze never rings at e.g. 03:00 AM of the next day:
     * 1. If `triggerAtMillis` crosses into the next calendar day (`targetDay > currentDay`) OR
     * 2. If `moment` is provided and `triggerAtMillis` falls after the moment's window end on `currentDay`
     *    (`triggerMinutesOfDay > MomentWindowEvaluator.effectiveEndMinutes(moment.endMinutesOfDay)`),
     * returns [AutoSnoozeDecision.ReachedNextDayStopNotifying] so no further auto-snooze is placed for today,
     * while preserving tomorrow's normal scheduled time / moment window reminders.
     */
    fun evaluateTimeout(
        previousAutoSnoozeCount: Int,
        nowEpochMillis: Long,
        zoneId: ZoneId,
        moment: Moment? = null
    ): AutoSnoozeDecision {
        val currentZdt = Instant.ofEpochMilli(nowEpochMillis).atZone(zoneId)
        val currentDay = currentZdt.toLocalDate().toEpochDay()
        val snoozeMinutes = computeSnoozeMinutes(previousAutoSnoozeCount)
        val triggerAtMillis = nowEpochMillis + snoozeMinutes * 60_000L
        val targetZdt = Instant.ofEpochMilli(triggerAtMillis).atZone(zoneId)
        val targetDay = targetZdt.toLocalDate().toEpochDay()

        if (targetDay > currentDay) {
            return AutoSnoozeDecision.ReachedNextDayStopNotifying(
                currentEpochDay = currentDay,
                wouldReachEpochDay = targetDay
            )
        }

        if (moment != null && moment.isEnabled && !MomentWindowEvaluator.crossesMidnight(moment)) {
            val endMin = MomentWindowEvaluator.effectiveEndMinutes(moment.endMinutesOfDay)
            val endOfWindowMillis = ZonedDateTime.of(
                LocalDate.ofEpochDay(currentDay),
                LocalTime.of(endMin / 60, endMin % 60, 59),
                zoneId
            ).toInstant().toEpochMilli()
            if (triggerAtMillis > endOfWindowMillis) {
                return AutoSnoozeDecision.ReachedNextDayStopNotifying(
                    currentEpochDay = currentDay,
                    wouldReachEpochDay = currentDay + 1L
                )
            }
        }

        return AutoSnoozeDecision.SnoozeForMinutes(
            nextCount = previousAutoSnoozeCount + 1,
            snoozeMinutes = snoozeMinutes,
            triggerAtEpochMillis = triggerAtMillis,
            targetEpochDay = targetDay
        )
    }
}
