package com.taskmgr.data.sync

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.taskmgr.data.local.CalendarSyncDao
import com.taskmgr.data.local.CalendarSyncEventEntity
import com.taskmgr.data.local.MomentDao
import com.taskmgr.data.local.TaskDao
import com.taskmgr.data.local.toDomain
import com.taskmgr.domain.logic.MomentWindowEvaluator
import com.taskmgr.domain.model.Moment
import com.taskmgr.domain.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

data class DeviceCalendarAccount(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    val isPrimary: Boolean,
    val colorArgb: Int
)

data class CalendarSyncResult(
    val success: Boolean,
    val calendarName: String,
    val createdEvents: Int = 0,
    val updatedEvents: Int = 0,
    val deletedEvents: Int = 0,
    val pulledUpdates: Int = 0,
    val message: String
)

data class TaskCalendarEventPayload(
    val title: String,
    val description: String,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val allDay: Boolean,
    val timeZoneId: String,
    val contentHash: Int
)

object TaskCalendarEventMapper {
    const val ITEM_TYPE_TASK = "TASK"
    private const val TASKMGR_MARKER_PREFIX = "[TaskMgr Task #"

    fun buildEventPayload(
        task: Task,
        moment: Moment?,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): TaskCalendarEventPayload {
        val statusPrefix = if (task.isCompleted) "✅ " else ""
        val cleanTitle = "$statusPrefix${task.title}".trim()

        val startMinutes = task.scheduledMinutesOfDay ?: moment?.startMinutesOfDay
        val isAllDay = startMinutes == null
        val effectiveSyncEndDay = if (task.endEpochDay >= Task.OPEN_ENDED_EPOCH_DAY) {
            task.startEpochDay
        } else {
            task.endEpochDay
        }

        val (startMillis, endMillis, tzId) = if (isAllDay) {
            // CalendarContract requires all-day events in UTC midnight boundaries (exclusive end day)
            val startUtc = LocalDate.ofEpochDay(task.startEpochDay)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
            val endUtc = LocalDate.ofEpochDay(effectiveSyncEndDay + 1L)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
            Triple(startUtc, endUtc, "UTC")
        } else {
            val normalizedStart = startMinutes.coerceIn(0, 1439)
            val startLocal = LocalDate.ofEpochDay(task.startEpochDay)
                .atTime(LocalTime.of(normalizedStart / 60, normalizedStart % 60))
                .atZone(zoneId)
            val endMinutes = if (task.scheduledMinutesOfDay != null) {
                (normalizedStart + 30).coerceAtMost(1439)
            } else if (moment != null) {
                val effEnd = MomentWindowEvaluator.effectiveEndMinutes(moment.endMinutesOfDay)
                if (effEnd > normalizedStart) effEnd else (normalizedStart + 30).coerceAtMost(1439)
            } else {
                (normalizedStart + 30).coerceAtMost(1439)
            }
            val endLocal = LocalDate.ofEpochDay(effectiveSyncEndDay)
                .atTime(LocalTime.of(endMinutes / 60, endMinutes % 60))
                .atZone(zoneId)
            val startEpoch = startLocal.toInstant().toEpochMilli()
            val endEpoch = maxOf(startEpoch + 15 * 60_000L, endLocal.toInstant().toEpochMilli())
            Triple(startEpoch, endEpoch, zoneId.id)
        }

        val descBuilder = StringBuilder()
        if (!task.notes.isNullOrBlank()) {
            descBuilder.appendLine(task.notes.trim())
            descBuilder.appendLine()
        }
        if (moment != null) {
            descBuilder.appendLine("Momento: ${moment.name}")
        }
        descBuilder.append("$TASKMGR_MARKER_PREFIX${task.id}]")
        val description = descBuilder.toString()

        val hash = listOf(
            cleanTitle,
            description,
            startMillis.toString(),
            endMillis.toString(),
            isAllDay.toString()
        ).joinToString("|").hashCode()

        return TaskCalendarEventPayload(
            title = cleanTitle,
            description = description,
            startEpochMillis = startMillis,
            endEpochMillis = endMillis,
            allDay = isAllDay,
            timeZoneId = tzId,
            contentHash = hash
        )
    }

    fun stripCompletionPrefix(calendarTitle: String): String =
        calendarTitle.removePrefix("✅ ").trim()

    fun stripTaskMgrFooter(calendarDescription: String?): String? {
        if (calendarDescription.isNullOrBlank()) return null
        val cleaned = calendarDescription
            .lines()
            .filterNot { line ->
                val trimmed = line.trim()
                trimmed.startsWith(TASKMGR_MARKER_PREFIX) || trimmed.startsWith("Momento: ")
            }
            .joinToString("\n")
            .trim()
        return cleaned.takeIf { it.isNotEmpty() }
    }
}

class AndroidCalendarSyncManager(
    context: Context,
    private val taskDao: TaskDao,
    private val momentDao: MomentDao,
    private val calendarSyncDao: CalendarSyncDao
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("taskmgr_calendar_sync", Context.MODE_PRIVATE)

    fun hasCalendarPermissions(): Boolean {
        val readGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val writeGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        return readGranted && writeGranted
    }

    fun getSelectedCalendarId(): Long? {
        val stored = prefs.getLong(KEY_SELECTED_CALENDAR_ID, -1L)
        return if (stored > 0L) stored else null
    }

    fun setSelectedCalendarId(calendarId: Long) {
        prefs.edit().putLong(KEY_SELECTED_CALENDAR_ID, calendarId).apply()
    }

    fun isAutoSyncEnabled(): Boolean =
        prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, false)

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC_ENABLED, enabled).apply()
    }

    fun getLastSyncSummary(): String? =
        prefs.getString(KEY_LAST_SYNC_SUMMARY, null)

    private fun saveLastSyncSummary(summary: String) {
        prefs.edit()
            .putString(KEY_LAST_SYNC_SUMMARY, summary)
            .putLong(KEY_LAST_SYNC_EPOCH_MILLIS, System.currentTimeMillis())
            .apply()
    }

    suspend fun listWritableCalendars(): List<DeviceCalendarAccount> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions()) return@withContext emptyList()

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        val selection = "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
        val selectionArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())

        val calendars = mutableListOf<DeviceCalendarAccount>()
        runCatching {
            appContext.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val nameIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accNameIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                val accTypeIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
                val primaryIdx = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
                val colorIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val displayName = cursor.getString(nameIdx).orEmpty().ifBlank { "Calendário #$id" }
                    val accountName = cursor.getString(accNameIdx).orEmpty().ifBlank { "Local" }
                    val accountType = cursor.getString(accTypeIdx).orEmpty()
                    val isPrimary = if (primaryIdx >= 0) cursor.getInt(primaryIdx) == 1 else false
                    val color = if (colorIdx >= 0 && !cursor.isNull(colorIdx)) {
                        cursor.getInt(colorIdx)
                    } else {
                        0xFF818CF8.toInt()
                    }
                    calendars.add(
                        DeviceCalendarAccount(
                            id = id,
                            displayName = displayName,
                            accountName = accountName,
                            accountType = accountType,
                            isPrimary = isPrimary,
                            colorArgb = color
                        )
                    )
                }
            }
        }
        calendars
    }

    /**
     * Synchronizes ONLY Tasks with the device/Google Calendar app via CalendarContract.Events.
     * - Pulls remote title/time/date edits made in the calendar app back into existing synced tasks.
     * - Pushes new or modified local tasks into the selected calendar.
     * - Deletes calendar events for tasks that were removed locally.
     */
    suspend fun syncTasksNow(): CalendarSyncResult = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions()) {
            return@withContext CalendarSyncResult(
                success = false,
                calendarName = "",
                message = "Conceda permissão de leitura e escrita na agenda para sincronizar."
            )
        }

        val calendars = listWritableCalendars()
        if (calendars.isEmpty()) {
            return@withContext CalendarSyncResult(
                success = false,
                calendarName = "",
                message = "Nenhuma conta de calendário gravável encontrada no dispositivo."
            )
        }

        val selectedId = getSelectedCalendarId()
        val targetCalendar = calendars.firstOrNull { it.id == selectedId }
            ?: calendars.firstOrNull { it.isPrimary }
            ?: calendars.first()

        if (selectedId != targetCalendar.id) {
            setSelectedCalendarId(targetCalendar.id)
        }

        val zoneId = ZoneId.systemDefault()
        val nowMillis = System.currentTimeMillis()
        val momentsById = momentDao.getAll().associate { it.id to it.toDomain() }
        val existingMappings = calendarSyncDao.getAll()
            .filter { it.itemType == TaskCalendarEventMapper.ITEM_TYPE_TASK }
            .associateBy { it.itemId }

        var createdCount = 0
        var updatedCount = 0
        var deletedCount = 0
        var pulledCount = 0

        // 1. Delete calendar events for tasks that no longer exist in Room or no longer have a date
        val initialTasksById = taskDao.getAll().associateBy { it.id }
        for ((taskId, mapping) in existingMappings) {
            val entity = initialTasksById[taskId]
            if (entity == null || entity.startEpochDay < 0L) {
                deleteCalendarEventById(mapping.eventId)
                calendarSyncDao.delete(TaskCalendarEventMapper.ITEM_TYPE_TASK, taskId)
                deletedCount++
            }
        }

        // 2. Bi-directional check: if local task has NOT changed since last sync, check if the user
        // edited the event inside Google/Android Calendar and pull the updated title/time/date.
        for ((taskId, taskEntity) in initialTasksById) {
            if (taskEntity.startEpochDay < 0L) continue
            val mapping = existingMappings[taskId] ?: continue
            if (mapping.calendarId != targetCalendar.id) continue
            val task = taskEntity.toDomain()
            val moment = task.momentId?.let { momentsById[it] }
            val expectedLocalPayload = TaskCalendarEventMapper.buildEventPayload(task, moment, zoneId)

            if (expectedLocalPayload.contentHash == mapping.lastSyncedHash) {
                val remoteEvent = readRemoteEvent(mapping.eventId)
                if (remoteEvent != null && !remoteEvent.deleted) {
                    val remoteCleanTitle = TaskCalendarEventMapper.stripCompletionPrefix(remoteEvent.title)
                        .ifBlank { task.title }
                    val remoteNotes = TaskCalendarEventMapper.stripTaskMgrFooter(remoteEvent.description)
                    val (remoteStartDay, remoteEndDay, remoteMinutes) = extractDateAndTimeFromRemote(
                        remoteEvent = remoteEvent,
                        fallbackTask = task,
                        zoneId = zoneId
                    )
                    val changedInCalendar = remoteCleanTitle != task.title ||
                        remoteNotes != task.notes ||
                        remoteStartDay != task.startEpochDay ||
                        remoteEndDay != task.endEpochDay ||
                        remoteMinutes != task.scheduledMinutesOfDay

                    if (changedInCalendar) {
                        val updatedEntity = taskEntity.copy(
                            title = remoteCleanTitle,
                            notes = remoteNotes,
                            startEpochDay = remoteStartDay,
                            endEpochDay = maxOf(remoteStartDay, remoteEndDay),
                            scheduledMinutesOfDay = remoteMinutes
                        )
                        taskDao.upsert(updatedEntity)
                        val refreshedTask = updatedEntity.toDomain()
                        val refreshedPayload = TaskCalendarEventMapper.buildEventPayload(
                            refreshedTask,
                            moment,
                            zoneId
                        )
                        updateCalendarEvent(mapping.eventId, targetCalendar.id, refreshedPayload)
                        calendarSyncDao.upsert(
                            CalendarSyncEventEntity(
                                itemType = TaskCalendarEventMapper.ITEM_TYPE_TASK,
                                itemId = taskId,
                                calendarId = targetCalendar.id,
                                eventId = mapping.eventId,
                                lastSyncedHash = refreshedPayload.contentHash,
                                lastSyncedAtEpochMillis = nowMillis
                            )
                        )
                        pulledCount++
                    }
                }
            }
        }

        // 3. Push local tasks (insert new or update changed/missing events)
        val currentTasks = taskDao.getAll().map { it.toDomain() }.filter { it.hasDate }
        val refreshedMappings = calendarSyncDao.getAll()
            .filter { it.itemType == TaskCalendarEventMapper.ITEM_TYPE_TASK }
            .associateBy { it.itemId }

        for (task in currentTasks) {
            val moment = task.momentId?.let { momentsById[it] }
            val payload = TaskCalendarEventMapper.buildEventPayload(task, moment, zoneId)
            val mapping = refreshedMappings[task.id]

            if (mapping == null) {
                val newEventId = insertCalendarEvent(targetCalendar.id, payload)
                if (newEventId != null) {
                    calendarSyncDao.upsert(
                        CalendarSyncEventEntity(
                            itemType = TaskCalendarEventMapper.ITEM_TYPE_TASK,
                            itemId = task.id,
                            calendarId = targetCalendar.id,
                            eventId = newEventId,
                            lastSyncedHash = payload.contentHash,
                            lastSyncedAtEpochMillis = nowMillis
                        )
                    )
                    createdCount++
                }
            } else {
                val remoteEvent = readRemoteEvent(mapping.eventId)
                if (remoteEvent == null || remoteEvent.deleted || mapping.calendarId != targetCalendar.id) {
                    if (mapping.calendarId != targetCalendar.id && remoteEvent != null && !remoteEvent.deleted) {
                        deleteCalendarEventById(mapping.eventId)
                    }
                    val recreatedId = insertCalendarEvent(targetCalendar.id, payload)
                    if (recreatedId != null) {
                        calendarSyncDao.upsert(
                            CalendarSyncEventEntity(
                                itemType = TaskCalendarEventMapper.ITEM_TYPE_TASK,
                                itemId = task.id,
                                calendarId = targetCalendar.id,
                                eventId = recreatedId,
                                lastSyncedHash = payload.contentHash,
                                lastSyncedAtEpochMillis = nowMillis
                            )
                        )
                        createdCount++
                    }
                } else if (mapping.lastSyncedHash != payload.contentHash) {
                    val updated = updateCalendarEvent(mapping.eventId, targetCalendar.id, payload)
                    if (updated) {
                        calendarSyncDao.upsert(
                            CalendarSyncEventEntity(
                                itemType = TaskCalendarEventMapper.ITEM_TYPE_TASK,
                                itemId = task.id,
                                calendarId = targetCalendar.id,
                                eventId = mapping.eventId,
                                lastSyncedHash = payload.contentHash,
                                lastSyncedAtEpochMillis = nowMillis
                            )
                        )
                        updatedCount++
                    }
                }
            }
        }

        val summary = buildString {
            append("Agenda '${targetCalendar.displayName}': ")
            append("${currentTasks.size} tarefas sincronizadas")
            val details = buildList {
                if (createdCount > 0) add("+$createdCount criadas")
                if (updatedCount > 0) add("$updatedCount atualizadas")
                if (pulledCount > 0) add("$pulledCount recebidas da agenda")
                if (deletedCount > 0) add("$deletedCount removidas")
            }
            if (details.isNotEmpty()) {
                append(" (${details.joinToString(", ")})")
            }
        }
        saveLastSyncSummary(summary)

        CalendarSyncResult(
            success = true,
            calendarName = targetCalendar.displayName,
            createdEvents = createdCount,
            updatedEvents = updatedCount,
            deletedEvents = deletedCount,
            pulledUpdates = pulledCount,
            message = summary
        )
    }

    private data class RemoteCalendarEventSnapshot(
        val eventId: Long,
        val title: String,
        val description: String?,
        val dtStartMillis: Long,
        val dtEndMillis: Long,
        val allDay: Boolean,
        val deleted: Boolean
    )

    private fun readRemoteEvent(eventId: Long): RemoteCalendarEventSnapshot? {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.DELETED
        )
        return runCatching {
            appContext.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val title = cursor.getString(1).orEmpty()
                val desc = if (cursor.isNull(2)) null else cursor.getString(2)
                val dtStart = cursor.getLong(3)
                val dtEnd = if (cursor.isNull(4)) dtStart else cursor.getLong(4)
                val allDay = cursor.getInt(5) == 1
                val deleted = cursor.getInt(6) == 1
                RemoteCalendarEventSnapshot(
                    eventId = eventId,
                    title = title,
                    description = desc,
                    dtStartMillis = dtStart,
                    dtEndMillis = dtEnd,
                    allDay = allDay,
                    deleted = deleted
                )
            }
        }.getOrNull()
    }

    private fun extractDateAndTimeFromRemote(
        remoteEvent: RemoteCalendarEventSnapshot,
        fallbackTask: Task,
        zoneId: ZoneId
    ): Triple<Long, Long, Int?> {
        if (remoteEvent.dtStartMillis <= 0L) {
            return Triple(fallbackTask.startEpochDay, fallbackTask.endEpochDay, fallbackTask.scheduledMinutesOfDay)
        }
        return if (remoteEvent.allDay) {
            val startDay = Instant.ofEpochMilli(remoteEvent.dtStartMillis)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .toEpochDay()
            val rawEndDay = Instant.ofEpochMilli(remoteEvent.dtEndMillis)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .toEpochDay()
            val endDay = maxOf(startDay, rawEndDay - 1L)
            Triple(startDay, endDay, null)
        } else {
            val startZdt: ZonedDateTime = Instant.ofEpochMilli(remoteEvent.dtStartMillis).atZone(zoneId)
            val endZdt: ZonedDateTime = Instant.ofEpochMilli(
                maxOf(remoteEvent.dtStartMillis, remoteEvent.dtEndMillis)
            ).atZone(zoneId)
            val startDay = startZdt.toLocalDate().toEpochDay()
            val endDay = maxOf(startDay, endZdt.toLocalDate().toEpochDay())
            val mins = startZdt.hour * 60 + startZdt.minute
            Triple(startDay, endDay, mins)
        }
    }

    private fun insertCalendarEvent(
        calendarId: Long,
        payload: TaskCalendarEventPayload
    ): Long? {
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, payload.title)
            put(CalendarContract.Events.DESCRIPTION, payload.description)
            put(CalendarContract.Events.DTSTART, payload.startEpochMillis)
            put(CalendarContract.Events.DTEND, payload.endEpochMillis)
            put(CalendarContract.Events.ALL_DAY, if (payload.allDay) 1 else 0)
            put(CalendarContract.Events.EVENT_TIMEZONE, payload.timeZoneId)
        }
        return runCatching {
            val uri = appContext.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLongOrNull()
        }.getOrNull()
    }

    private fun updateCalendarEvent(
        eventId: Long,
        calendarId: Long,
        payload: TaskCalendarEventPayload
    ): Boolean {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, payload.title)
            put(CalendarContract.Events.DESCRIPTION, payload.description)
            put(CalendarContract.Events.DTSTART, payload.startEpochMillis)
            put(CalendarContract.Events.DTEND, payload.endEpochMillis)
            put(CalendarContract.Events.ALL_DAY, if (payload.allDay) 1 else 0)
            put(CalendarContract.Events.EVENT_TIMEZONE, payload.timeZoneId)
        }
        return runCatching {
            appContext.contentResolver.update(uri, values, null, null) > 0
        }.getOrDefault(false)
    }

    private fun deleteCalendarEventById(eventId: Long): Boolean {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        return runCatching {
            appContext.contentResolver.delete(uri, null, null) > 0
        }.getOrDefault(false)
    }

    companion object {
        private const val KEY_SELECTED_CALENDAR_ID = "selected_calendar_id"
        private const val KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled"
        private const val KEY_LAST_SYNC_SUMMARY = "last_sync_summary"
        private const val KEY_LAST_SYNC_EPOCH_MILLIS = "last_sync_epoch_millis"
    }
}
