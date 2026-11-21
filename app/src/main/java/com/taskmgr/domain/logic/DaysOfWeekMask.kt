package com.taskmgr.domain.logic

import java.time.DayOfWeek
import java.time.LocalDate

object DaysOfWeekMask {
    const val ALL_DAYS: Int = 0b1111111 // 127 (Mon..Sun)

    fun bitFor(dayOfWeek: DayOfWeek): Int = 1 shl (dayOfWeek.value - 1)

    fun fromDays(days: Set<DayOfWeek>): Int {
        var mask = 0
        for (day in days) {
            mask = mask or bitFor(day)
        }
        return mask
    }

    fun toDays(mask: Int): Set<DayOfWeek> =
        DayOfWeek.entries.filterTo(linkedSetOf()) { (mask and bitFor(it)) != 0 }

    fun contains(mask: Int, dayOfWeek: DayOfWeek): Boolean =
        (mask and bitFor(dayOfWeek)) != 0

    fun containsEpochDay(mask: Int, epochDay: Long): Boolean {
        val dayOfWeek = LocalDate.ofEpochDay(epochDay).dayOfWeek
        return contains(mask, dayOfWeek)
    }

    fun toggle(mask: Int, dayOfWeek: DayOfWeek): Int =
        mask xor bitFor(dayOfWeek)

    fun nextMatchingEpochDay(
        mask: Int,
        fromEpochDayInclusive: Long,
        maxLookaheadDays: Int = 14
    ): Long? {
        if ((mask and ALL_DAYS) == 0) return null
        for (offset in 0..maxLookaheadDays) {
            val candidate = fromEpochDayInclusive + offset
            if (containsEpochDay(mask, candidate)) {
                return candidate
            }
        }
        return null
    }
}
