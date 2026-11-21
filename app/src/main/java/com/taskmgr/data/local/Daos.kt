package com.taskmgr.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MomentDao {
    @Query("SELECT * FROM moments ORDER BY startMinutesOfDay ASC, id ASC")
    fun observeAll(): Flow<List<MomentEntity>>

    @Query("SELECT * FROM moments ORDER BY startMinutesOfDay ASC, id ASC")
    suspend fun getAll(): List<MomentEntity>

    @Query("SELECT * FROM moments WHERE id = :momentId")
    suspend fun getById(momentId: Long): MomentEntity?

    @Upsert
    suspend fun upsert(entity: MomentEntity): Long

    @Query("DELETE FROM moments WHERE id = :momentId")
    suspend fun deleteById(momentId: Long)
}

@Dao
interface HabitGroupDao {
    @Query("SELECT * FROM habit_groups ORDER BY name ASC, id ASC")
    fun observeAll(): Flow<List<HabitGroupEntity>>

    @Query("SELECT * FROM habit_groups ORDER BY name ASC, id ASC")
    suspend fun getAll(): List<HabitGroupEntity>

    @Query("SELECT * FROM habit_groups WHERE id = :groupId")
    suspend fun getById(groupId: Long): HabitGroupEntity?

    @Upsert
    suspend fun upsert(entity: HabitGroupEntity): Long

    @Query("DELETE FROM habit_groups WHERE id = :groupId")
    suspend fun deleteById(groupId: Long)
}

@Dao
interface TaskDao {
    @Query(
        """
        SELECT * FROM tasks
        WHERE startEpochDay >= 0 AND startEpochDay <= :epochDay AND endEpochDay >= :epochDay
        ORDER BY isCompleted ASC, COALESCE(scheduledMinutesOfDay, 1440) ASC, id ASC
        """
    )
    fun observeForDay(epochDay: Long): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE startEpochDay >= 0 AND startEpochDay <= :endEpochDay AND endEpochDay >= :startEpochDay
        ORDER BY startEpochDay ASC, id ASC
        """
    )
    fun observeForRange(startEpochDay: Long, endEpochDay: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY startEpochDay ASC, id ASC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY startEpochDay ASC, id ASC")
    suspend fun getAll(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0")
    suspend fun getAllPending(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getById(taskId: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeById(taskId: Long): Flow<TaskEntity?>

    @Upsert
    suspend fun upsert(entity: TaskEntity): Long

    @Query(
        """
        UPDATE tasks
        SET isCompleted = :completed,
            completedAtEpochMillis = :completedAtEpochMillis,
            snoozedUntilEpochMillis = CASE WHEN :completed = 1 THEN NULL ELSE snoozedUntilEpochMillis END
        WHERE id = :taskId
        """
    )
    suspend fun setCompleted(taskId: Long, completed: Boolean, completedAtEpochMillis: Long?)

    @Query("UPDATE tasks SET snoozedUntilEpochMillis = :snoozedUntilEpochMillis WHERE id = :taskId")
    suspend fun setSnoozedUntil(taskId: Long, snoozedUntilEpochMillis: Long?)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: Long)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY id ASC")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY id ASC")
    suspend fun getAll(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE isActive = 1")
    suspend fun getAllActive(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :habitId")
    suspend fun getById(habitId: Long): HabitEntity?

    @Query("SELECT * FROM habits WHERE id = :habitId")
    fun observeById(habitId: Long): Flow<HabitEntity?>

    @Upsert
    suspend fun upsert(entity: HabitEntity): Long

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteById(habitId: Long)
}

@Dao
interface HabitLogDao {
    @Query("SELECT * FROM habit_logs")
    fun observeAll(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE epochDay = :epochDay")
    fun observeForDay(epochDay: Long): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE epochDay BETWEEN :startEpochDay AND :endEpochDay")
    fun observeForRange(startEpochDay: Long, endEpochDay: Long): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND epochDay = :epochDay")
    fun observeLog(habitId: Long, epochDay: Long): Flow<HabitLogEntity?>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun getLog(habitId: Long, epochDay: Long): HabitLogEntity?

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId")
    suspend fun getLogsForHabit(habitId: Long): List<HabitLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HabitLogEntity)
}

@Dao
interface CalendarSyncDao {
    @Query("SELECT * FROM calendar_sync_events")
    suspend fun getAll(): List<CalendarSyncEventEntity>

    @Query("SELECT * FROM calendar_sync_events WHERE itemType = :itemType AND itemId = :itemId")
    suspend fun get(itemType: String, itemId: Long): CalendarSyncEventEntity?

    @Upsert
    suspend fun upsert(entity: CalendarSyncEventEntity)

    @Query("DELETE FROM calendar_sync_events WHERE itemType = :itemType AND itemId = :itemId")
    suspend fun delete(itemType: String, itemId: Long)

    @Query("DELETE FROM calendar_sync_events WHERE calendarId = :calendarId")
    suspend fun deleteForCalendar(calendarId: Long)

    @Query("DELETE FROM calendar_sync_events")
    suspend fun deleteAll()
}

