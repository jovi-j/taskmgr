package com.taskmgr.domain.logic

import com.taskmgr.domain.model.Moment
import com.taskmgr.domain.model.ReminderMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object MomentWindowEvaluator {
    const val MINUTES_IN_DAY: Int = 1440

    fun effectiveEndMinutes(endMinutesOfDay: Int): Int =
        if (endMinutesOfDay == 0) MINUTES_IN_DAY - 1 else endMinutesOfDay

    fun crossesMidnight(moment: Moment): Boolean =
        effectiveEndMinutes(moment.endMinutesOfDay) < moment.startMinutesOfDay

    fun isWithinWindow(moment: Moment, epochDay: Long, minutesOfDay: Int): Boolean {
        if (!moment.isEnabled) return false
        if (minutesOfDay !in 0 until MINUTES_IN_DAY) return false

        val effectiveEnd = effectiveEndMinutes(moment.endMinutesOfDay)
        return if (!crossesMidnight(moment)) {
            DaysOfWeekMask.containsEpochDay(moment.daysOfWeekMask, epochDay) &&
                minutesOfDay in moment.startMinutesOfDay..effectiveEnd
        } else {
            val activeTodayStart =
                DaysOfWeekMask.containsEpochDay(moment.daysOfWeekMask, epochDay) &&
                    minutesOfDay >= moment.startMinutesOfDay
            val activeYesterdayTail =
                DaysOfWeekMask.containsEpochDay(moment.daysOfWeekMask, epochDay - 1) &&
                    minutesOfDay <= effectiveEnd
            activeTodayStart || activeYesterdayTail
        }
    }

    fun triggerInstantsForWindowStartingOn(
        moment: Moment,
        startEpochDay: Long,
        zoneId: ZoneId,
        overrideIntervalMinutes: Int? = null
    ): List<Long> {
        if (!moment.isEnabled) return emptyList()
        if (!DaysOfWeekMask.containsEpochDay(moment.daysOfWeekMask, startEpochDay)) {
            return emptyList()
        }

        val startDate = LocalDate.ofEpochDay(startEpochDay)
        val effectiveEnd = effectiveEndMinutes(moment.endMinutesOfDay)
        val cross = crossesMidnight(moment)

        val effectiveMode = if (overrideIntervalMinutes != null && overrideIntervalMinutes > 0) {
            ReminderMode.INTERVAL_MINUTES
        } else {
            moment.reminderMode
        }

        val minuteOffsets: List<Int> = when (effectiveMode) {
            ReminderMode.ONCE_AT_START -> listOf(0)
            ReminderMode.INTERVAL_MINUTES -> {
                val interval = (overrideIntervalMinutes ?: moment.intervalMinutes ?: 30).coerceAtLeast(1)
                val totalSpan = if (!cross) {
                    effectiveEnd - moment.startMinutesOfDay
                } else {
                    (MINUTES_IN_DAY - moment.startMinutesOfDay) + effectiveEnd
                }
                buildList {
                    var offset = 0
                    while (offset <= totalSpan) {
                        add(offset)
                        offset += interval
                    }
                }
            }
            ReminderMode.SPECIFIC_TIMES -> {
                val times = moment.specificTimesMinutesOfDay.distinct().sorted()
                times.mapNotNull { minuteOfDay ->
                    if (minuteOfDay !in 0 until MINUTES_IN_DAY) return@mapNotNull null
                    if (!cross) {
                        if (minuteOfDay in moment.startMinutesOfDay..effectiveEnd) {
                            minuteOfDay - moment.startMinutesOfDay
                        } else null
                    } else {
                        when {
                            minuteOfDay >= moment.startMinutesOfDay ->
                                minuteOfDay - moment.startMinutesOfDay
                            minuteOfDay <= effectiveEnd ->
                                (MINUTES_IN_DAY - moment.startMinutesOfDay) + minuteOfDay
                            else -> null
                        }
                    }
                }.sorted()
            }
        }

        val windowStartZdt = ZonedDateTime.of(
            startDate,
            LocalTime.of(moment.startMinutesOfDay / 60, moment.startMinutesOfDay % 60),
            zoneId
        )

        return minuteOffsets.map { offsetMin ->
            windowStartZdt.plusMinutes(offsetMin.toLong()).toInstant().toEpochMilli()
        }
    }

    fun nextTriggerAfter(
        moment: Moment,
        nowEpochMillis: Long,
        zoneId: ZoneId,
        allowedEpochDaysMask: Int? = null,
        minStartEpochDay: Long? = null,
        maxEndEpochDay: Long? = null,
        overrideIntervalMinutes: Int? = null,
        lookaheadDays: Int = 14
    ): Long? {
        if (!moment.isEnabled) return null
        val todayEpochDay = Instant.ofEpochMilli(nowEpochMillis).atZone(zoneId).toLocalDate().toEpochDay()
        val firstDayToCheck = (todayEpochDay - 1).coerceAtLeast(minStartEpochDay ?: Long.MIN_VALUE)
        val lastDayToCheck = (todayEpochDay + lookaheadDays).coerceAtMost(maxEndEpochDay ?: Long.MAX_VALUE)

        for (day in firstDayToCheck..lastDayToCheck) {
            if (allowedEpochDaysMask != null && !DaysOfWeekMask.containsEpochDay(allowedEpochDaysMask, day)) {
                continue
            }
            val instants = triggerInstantsForWindowStartingOn(
                moment = moment,
                startEpochDay = day,
                zoneId = zoneId,
                overrideIntervalMinutes = overrideIntervalMinutes
            )
            val next = instants.firstOrNull { it > nowEpochMillis }
            if (next != null) return next
        }
        return null
    }
}
