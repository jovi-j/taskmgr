package com.taskmgr.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "moments")
data class MomentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startMinutesOfDay: Int,
    val endMinutesOfDay: Int,
    val daysOfWeekMask: Int,
    val reminderMode: String, // ONCE_AT_START | INTERVAL_MINUTES | SPECIFIC_TIMES
    val intervalMinutes: Int? = null,
    val specificTimesCsv: String? = null,
    val aggressiveFullScreen: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val isEnabled: Boolean = true
)

@Entity(tableName = "habit_groups")
data class HabitGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Long,
    val iconGlyph: String
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = MomentEntity::class,
            parentColumns = ["id"],
            childColumns = ["momentId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("momentId"), Index("startEpochDay", "endEpochDay")]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String? = null,
    val startEpochDay: Long,
    val endEpochDay: Long,
    val scheduledMinutesOfDay: Int? = null,
    val momentId: Long? = null,
    val aggressiveFullScreen: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val repeatIntervalMinutes: Int? = null,
    val isCompleted: Boolean = false,
    val completedAtEpochMillis: Long? = null,
    val snoozedUntilEpochMillis: Long? = null,
    val urgency: String = "LOW",
    val advanceRemindersSpec: String? = null
)

@Entity(
    tableName = "habits",
    foreignKeys = [
        ForeignKey(
            entity = MomentEntity::class,
            parentColumns = ["id"],
            childColumns = ["momentId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = HabitGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("momentId"), Index("groupId")]
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String? = null,
    val startEpochDay: Long,
    val activeDaysMask: Int = 0b1111111,
    val momentId: Long? = null,
    val groupId: Long? = null,
    val iconGlyph: String? = null,
    val scheduledMinutesOfDay: Int? = null,
    val repeatIntervalMinutes: Int? = null,
    val aggressiveFullScreen: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val completionType: String, // BOOLEAN | COUNT_PER_DAY | CUSTOM_UNIT
    val incrementAmount: Double = 1.0,
    val incrementUnitLabel: String = "Sim",
    val targetAmount: Double = 1.0,
    val targetUnitLabel: String = "Sim",
    val isActive: Boolean = true
)

@Entity(
    tableName = "habit_logs",
    primaryKeys = ["habitId", "epochDay"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("epochDay")]
)
data class HabitLogEntity(
    val habitId: Long,
    val epochDay: Long,
    val accumulatedAmount: Double = 0.0,
    val isCompleted: Boolean = false,
    val dismissedForDay: Boolean = false,
    val snoozedUntilEpochMillis: Long? = null,
    val updatedAtEpochMillis: Long
)

@Entity(
    tableName = "calendar_sync_events",
    primaryKeys = ["itemType", "itemId"]
)
data class CalendarSyncEventEntity(
    val itemType: String,
    val itemId: Long,
    val calendarId: Long,
    val eventId: Long,
    val lastSyncedHash: Int,
    val lastSyncedAtEpochMillis: Long
)

