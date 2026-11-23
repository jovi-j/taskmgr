package com.taskmgr.domain.logic

import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.HabitLog
import com.taskmgr.domain.model.Moment
import com.taskmgr.domain.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

data class ScheduledAlarmSpec(
    val triggerAtEpochMillis: Long,
    val targetEpochDay: Long,
    val isAdvanceReminder: Boolean = false
)

object NextAlarmCalculator {

    fun calculateNextForTask(
        task: Task,
        moment: Moment?,
        nowEpochMillis: Long,
        zoneId: ZoneId,
        skipTodayEpochDay: Boolean = false
    ): ScheduledAlarmSpec? {
        // INV-01: Completed or unscheduled tasks never trigger alarms
        if (task.isCompleted || !task.hasDate) return null

        val snoozeMillis = task.snoozedUntilEpochMillis
        val effectiveEndDay = if (task.hasEndDate && task.endEpochDay < Task.OPEN_ENDED_EPOCH_DAY) {
            task.endEpochDay
        } else {
            Long.MAX_VALUE
        }
        val todayEpochDay = Instant.ofEpochMilli(nowEpochMillis).atZone(zoneId).toLocalDate().toEpochDay()
        if (snoozeMillis != null && snoozeMillis > nowEpochMillis) {
            val targetDay = Instant.ofEpochMilli(snoozeMillis).atZone(zoneId).toLocalDate().toEpochDay()
            if (targetDay <= effectiveEndDay && (!skipTodayEpochDay || targetDay > todayEpochDay)) {
                return ScheduledAlarmSpec(snoozeMillis, targetDay, isAdvanceReminder = false)
            }
            if (targetDay > effectiveEndDay) return null
        }

        val minAllowedDay = if (skipTodayEpochDay) todayEpochDay + 1L else todayEpochDay
        val firstDay = maxOf(minAllowedDay, task.startEpochDay)
        if (firstDay > effectiveEndDay) return null

        val searchHorizonEndDay = minOf(effectiveEndDay, firstDay + 14L)

        val regularCandidates = mutableListOf<Long>()
        val advanceCandidates = mutableListOf<Long>()

        val scheduledMin = task.scheduledMinutesOfDay
        val effectiveEventMin = scheduledMin ?: moment?.takeIf { it.isEnabled }?.startMinutesOfDay

        if (effectiveEventMin != null &&
            effectiveEventMin in 0 until MomentWindowEvaluator.MINUTES_IN_DAY &&
            task.advanceReminders.isNotEmpty()
        ) {
            val maxAdvDays = (task.advanceReminders.maxOfOrNull { it.daysBefore } ?: 0).toLong()
            val advSearchHorizon = minOf(effectiveEndDay, firstDay + 14L + maxAdvDays)
            for (day in firstDay..advSearchHorizon) {
                if (!DaysOfWeekMask.containsEpochDay(task.activeDaysMask, day)) continue
                val eventMillis = toZonedDateTime(day, effectiveEventMin, zoneId).toInstant().toEpochMilli()
                if (eventMillis <= nowEpochMillis) continue

                for (advance in task.advanceReminders) {
                    val offsetMinutes = advance.totalMinutesBefore
                    if (offsetMinutes <= 0L) continue
                    val firstAdvanceMillis = eventMillis - offsetMinutes * 60_000L
                    if (firstAdvanceMillis > nowEpochMillis && firstAdvanceMillis < eventMillis) {
                        advanceCandidates.add(firstAdvanceMillis)
                    } else {
                        val repMin = advance.repeatIntervalMinutes
                        if (repMin != null && repMin > 0) {
                            var nextRep = firstAdvanceMillis + repMin * 60_000L
                            while (nextRep < eventMillis) {
                                if (nextRep > nowEpochMillis) {
                                    advanceCandidates.add(nextRep)
                                    break
                                }
                                nextRep += repMin * 60_000L
                            }
                        }
                    }
                }
            }
        }

        if (scheduledMin != null && scheduledMin in 0 until MomentWindowEvaluator.MINUTES_IN_DAY) {
            for (day in firstDay..searchHorizonEndDay) {
                if (!DaysOfWeekMask.containsEpochDay(task.activeDaysMask, day)) continue
                val baseZdt = toZonedDateTime(day, scheduledMin, zoneId)
                val baseMillis = baseZdt.toInstant().toEpochMilli()
                if (baseMillis > nowEpochMillis) {
                    regularCandidates.add(baseMillis)
                }
                val repeatMin = task.repeatIntervalMinutes
                if (repeatMin != null && repeatMin > 0) {
                    val endOfDayMillis = toZonedDateTime(day, 23 * 60 + 59, zoneId).toInstant().toEpochMilli()
                    var nextRepeat = baseMillis + repeatMin * 60_000L
                    while (nextRepeat <= endOfDayMillis) {
                        if (nextRepeat > nowEpochMillis) {
                            regularCandidates.add(nextRepeat)
                            break
                        }
                        nextRepeat += repeatMin * 60_000L
                    }
                }
            }
        }

        if (moment != null && moment.isEnabled) {
            val momentNext = MomentWindowEvaluator.nextTriggerAfter(
                moment = moment,
                nowEpochMillis = nowEpochMillis,
                zoneId = zoneId,
                allowedEpochDaysMask = task.activeDaysMask,
                minStartEpochDay = firstDay,
                maxEndEpochDay = effectiveEndDay,
                overrideIntervalMinutes = if (scheduledMin == null) task.repeatIntervalMinutes else null
            )
            if (momentNext != null) {
                regularCandidates.add(momentNext)
            }
        }

        val earliestAdvance = advanceCandidates.minOrNull()
        val earliestRegular = regularCandidates.minOrNull()

        val (earliest, isAdvance) = when {
            earliestAdvance != null && (earliestRegular == null || earliestAdvance < earliestRegular) ->
                earliestAdvance to true
            earliestRegular != null ->
                earliestRegular to false
            else -> return null
        }

        val earliestDay = Instant.ofEpochMilli(earliest).atZone(zoneId).toLocalDate().toEpochDay()
        return ScheduledAlarmSpec(
            triggerAtEpochMillis = earliest,
            targetEpochDay = earliestDay,
            isAdvanceReminder = isAdvance
        )
    }

    fun calculateNextForHabit(
        habit: Habit,
        logForCurrentDay: HabitLog?,
        currentEpochDay: Long,
        moment: Moment?,
        nowEpochMillis: Long,
        zoneId: ZoneId
    ): ScheduledAlarmSpec? {
        if (!habit.isActive) return null
        if ((habit.activeDaysMask and DaysOfWeekMask.ALL_DAYS) == 0) return null

        val todayEpochDay = Instant.ofEpochMilli(nowEpochMillis).atZone(zoneId).toLocalDate().toEpochDay()
        val effectiveCurrentDay = maxOf(currentEpochDay, todayEpochDay)

        // INV-01 & INV-02: If completed or dismissedForDay on effectiveCurrentDay, skip effectiveCurrentDay
        val skipCurrentDay = logForCurrentDay != null &&
            logForCurrentDay.epochDay == effectiveCurrentDay &&
            (logForCurrentDay.isCompleted || logForCurrentDay.dismissedForDay)

        // Short snooze on current day (only if not completed and not dismissedForDay)
        if (!skipCurrentDay && logForCurrentDay != null && logForCurrentDay.epochDay == effectiveCurrentDay) {
            val snoozeUntil = logForCurrentDay.snoozedUntilEpochMillis
            if (snoozeUntil != null && snoozeUntil > nowEpochMillis) {
                val targetDay = Instant.ofEpochMilli(snoozeUntil).atZone(zoneId).toLocalDate().toEpochDay()
                return ScheduledAlarmSpec(snoozeUntil, targetDay)
            }
        }

        val minSearchDay = maxOf(
            habit.startEpochDay,
            if (skipCurrentDay) effectiveCurrentDay + 1 else effectiveCurrentDay
        )

        val candidates = mutableListOf<Long>()

        val nextActiveDay = DaysOfWeekMask.nextMatchingEpochDay(habit.activeDaysMask, minSearchDay, maxLookaheadDays = 14)
        val scheduledMin = habit.scheduledMinutesOfDay
        if (nextActiveDay != null && scheduledMin != null && scheduledMin in 0 until MomentWindowEvaluator.MINUTES_IN_DAY) {
            var checkDay: Long? = nextActiveDay
            var attempts = 0
            while (checkDay != null && attempts < 2) {
                val baseMillis = toZonedDateTime(checkDay, scheduledMin, zoneId).toInstant().toEpochMilli()
                if (baseMillis > nowEpochMillis) {
                    candidates.add(baseMillis)
                } else if (habit.repeatIntervalMinutes != null && habit.repeatIntervalMinutes > 0) {
                    val endOfDayMillis = toZonedDateTime(checkDay, 23 * 60 + 59, zoneId).toInstant().toEpochMilli()
                    var nextRepeat = baseMillis + habit.repeatIntervalMinutes * 60_000L
                    while (nextRepeat <= endOfDayMillis) {
                        if (nextRepeat > nowEpochMillis) {
                            candidates.add(nextRepeat)
                            break
                        }
                        nextRepeat += habit.repeatIntervalMinutes * 60_000L
                    }
                }
                checkDay = DaysOfWeekMask.nextMatchingEpochDay(habit.activeDaysMask, checkDay + 1, maxLookaheadDays = 14)
                attempts++
            }
        }

        if (moment != null && moment.isEnabled) {
            val momentNext = MomentWindowEvaluator.nextTriggerAfter(
                moment = moment,
                nowEpochMillis = nowEpochMillis,
                zoneId = zoneId,
                allowedEpochDaysMask = habit.activeDaysMask,
                minStartEpochDay = minSearchDay,
                overrideIntervalMinutes = if (scheduledMin == null) habit.repeatIntervalMinutes else null
            )
            if (momentNext != null) {
                candidates.add(momentNext)
            }
        }

        val earliest = candidates.minOrNull() ?: return null
        val earliestDay = Instant.ofEpochMilli(earliest).atZone(zoneId).toLocalDate().toEpochDay()
        return ScheduledAlarmSpec(earliest, earliestDay)
    }

    private fun toZonedDateTime(epochDay: Long, minutesOfDay: Int, zoneId: ZoneId): ZonedDateTime {
        val clampedMin = minutesOfDay.coerceIn(0, MomentWindowEvaluator.MINUTES_IN_DAY - 1)
        return ZonedDateTime.of(
            LocalDate.ofEpochDay(epochDay),
            LocalTime.of(clampedMin / 60, clampedMin % 60),
            zoneId
        )
    }
}
