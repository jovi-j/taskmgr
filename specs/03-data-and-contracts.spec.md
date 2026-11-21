# specs/03-data-and-contracts.spec.md
version: 1.1.1
status: implemented
system: taskmgr (Android v0.0.2)

## 1. Modelos de Domínio (`com.taskmgr.domain.model`)

```kotlin
package com.taskmgr.domain.model

enum class ReminderMode {
    ONCE_AT_START,
    INTERVAL_MINUTES,
    SPECIFIC_TIMES
}

enum class CompletionType {
    BOOLEAN,
    COUNT_PER_DAY,
    CUSTOM_UNIT
}

enum class SnoozeOption(val minutes: Long?, val untilTomorrow: Boolean) {
    MINUTES_5(5L, false),
    MINUTES_10(10L, false),
    MINUTES_30(30L, false),
    HOURS_1(60L, false),
    HOURS_3(180L, false),
    UNTIL_TOMORROW(null, true)
}

data class Moment(
    val id: Long = 0L,
    val name: String,
    val startMinutesOfDay: Int,
    val endMinutesOfDay: Int,
    val daysOfWeekMask: Int,
    val reminderMode: ReminderMode,
    val intervalMinutes: Int? = null,
    val specificTimesMinutesOfDay: List<Int> = emptyList(),
    val aggressiveFullScreen: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val isEnabled: Boolean = true
)

data class Task(
    val id: Long = 0L,
    val title: String,
    val notes: String? = null,
    val startEpochDay: Long,
    val endEpochDay: Long,
    val scheduledMinutesOfDay: Int? = null,
    val momentId: Long? = null,
    val repeatIntervalMinutes: Int? = null,
    val aggressiveFullScreen: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val isCompleted: Boolean = false,
    val snoozedUntilEpochMillis: Long? = null
)

data class HabitGroup(
    val id: Long = 0L,
    val name: String,
    val colorArgb: Long,
    val iconGlyph: String
)

data class Habit(
    val id: Long = 0L,
    val title: String,
    val notes: String? = null,
    val startEpochDay: Long,
    val activeDaysMask: Int,
    val momentId: Long? = null,
    val groupId: Long? = null,
    val iconGlyph: String? = null,
    val scheduledMinutesOfDay: Int? = null,
    val repeatIntervalMinutes: Int? = null,
    val aggressiveFullScreen: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val completionType: CompletionType,
    val incrementAmount: Double,
    val incrementUnitLabel: String,
    val targetAmount: Double,
    val targetUnitLabel: String,
    val isActive: Boolean = true
)

data class HabitLog(
    val habitId: Long,
    val epochDay: Long,
    val accumulatedAmount: Double,
    val isCompleted: Boolean,
    val dismissedForDay: Boolean,
    val snoozedUntilEpochMillis: Long? = null,
    val updatedAtEpochMillis: Long
)

data class FullScreenReminderPayload(
    val itemType: ReminderItemType,
    val itemId: Long,
    val epochDay: Long,
    val title: String,
    val momentName: String?,
    val progressText: String?,
    val incrementButtonLabel: String?,
    val vibrationEnabled: Boolean
)

enum class ReminderItemType {
    TASK,
    HABIT
}
```

## 2. Banco de Dados Room (`TaskMgrDatabase` v3)
- **Versão atual:** `3` (`MIGRATION_1_2` adiciona `habit_groups` e colunas `groupId`/`iconGlyph` em `habits`; `MIGRATION_2_3` adiciona `calendar_sync_events`).
- **Tabelas:**
    - `moments` (`MomentEntity`)
    - `habit_groups` (`HabitGroupEntity`)
    - `tasks` (`TaskEntity`, `FOREIGN KEY(momentId) ON DELETE SET NULL`)
    - `habits` (`HabitEntity`, `FOREIGN KEY(momentId) ON DELETE SET NULL`, `FOREIGN KEY(groupId) ON DELETE SET NULL`)
    - `habit_logs` (`HabitLogEntity`, PK composta `[habitId, epochDay]`, `FOREIGN KEY(habitId) ON DELETE CASCADE`)
    - `calendar_sync_events` (`CalendarSyncEventEntity`, PK composta `[itemType, itemId]`)

## 3. Contratos de Repositório, Agendamento e Exportação (`com.taskmgr.domain.contract` & `com.taskmgr.domain.export`)

```kotlin
package com.taskmgr.domain.contract

import com.taskmgr.domain.model.FullScreenReminderPayload
import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.HabitGroup
import com.taskmgr.domain.model.HabitLog
import com.taskmgr.domain.model.Moment
import com.taskmgr.domain.model.ReminderItemType
import com.taskmgr.domain.model.SnoozeOption
import com.taskmgr.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface MomentRepository {
    fun observeMoments(): Flow<List<Moment>>
    suspend fun upsert(moment: Moment): Long
    suspend fun delete(momentId: Long)
}

interface HabitGroupRepository {
    fun observeGroups(): Flow<List<HabitGroup>>
    suspend fun upsert(group: HabitGroup): Long
    suspend fun delete(groupId: Long)
}

interface TaskRepository {
    fun observeTasksForDay(epochDay: Long): Flow<List<Task>>
    fun observeAllTasks(): Flow<List<Task>>
    suspend fun getAllTasks(): List<Task>
    suspend fun getById(taskId: Long): Task?
    suspend fun upsert(task: Task): Long
    suspend fun setCompleted(taskId: Long, completed: Boolean, completedAtEpochMillis: Long?)
    suspend fun snooze(taskId: Long, option: SnoozeOption, nowEpochMillis: Long)
    suspend fun delete(taskId: Long)
}

interface HabitRepository {
    fun observeHabits(): Flow<List<Habit>>
    fun observeAllHabitLogs(): Flow<List<HabitLog>>
    fun observeHabitLogsForDay(epochDay: Long): Flow<List<HabitLog>>
    fun observeHabitLogsForRange(startEpochDay: Long, endEpochDay: Long): Flow<List<HabitLog>>
    suspend fun getHabitById(habitId: Long): Habit?
    suspend fun getLog(habitId: Long, epochDay: Long): HabitLog?
    suspend fun upsertHabit(habit: Habit): Long
    suspend fun incrementHabit(habitId: Long, epochDay: Long, nowEpochMillis: Long): HabitLog
    suspend fun updateHabitLogManual(
        habitId: Long,
        epochDay: Long,
        accumulatedAmount: Double,
        dismissedForDay: Boolean,
        nowEpochMillis: Long
    ): HabitLog
    suspend fun snoozeHabit(habitId: Long, epochDay: Long, option: SnoozeOption, nowEpochMillis: Long): HabitLog
    suspend fun deleteHabit(habitId: Long)
}

interface ReminderScheduler {
    suspend fun scheduleNextForTask(taskId: Long)
    suspend fun scheduleNextForHabit(habitId: Long, epochDay: Long)
    suspend fun cancelTaskReminders(taskId: Long)
    suspend fun cancelHabitRemindersForDay(habitId: Long, epochDay: Long)
    suspend fun reconcileAll(nowEpochMillis: Long)
}

interface FullScreenAlertDispatcher {
    fun canUseFullScreenIntent(): Boolean
    fun triggerFullScreenReminder(payload: FullScreenReminderPayload)
    fun dismissReminder(itemType: ReminderItemType, itemId: Long)
}
```

## 4. Sistema Visual & Paleta OLED (`com.example.taskmgr.ui.theme`)
- `OledBackground`: `#000000` (Fundo principal OLED)
- `OledSurface`: `#0E1117` (Superfície 1 — Cartões de tarefas, hábitos e inputs)
- `OledSurfaceElevated`: `#161B26` (Superfície 2 — Modais, diálogos e menus)
- `OledBorder`: `#1F2937` (Bordas de `1.dp` e divisores)
- `IndigoPrimary`: `#818CF8` (Destaque principal, FAB, checkboxes e abas ativas)
- `IndigoContainer`: `#1E1B4B` (Container primário de chips e indicadores ativos)
- `TextPrimary`: `#F8FAFC`
- `TextSecondary`: `#94A3B8`
- `TextDisabled`: `#475569`
- `PriorityHigh`: `#F87171`
- `PriorityMedium`: `#FBBF24`
- `PriorityLow`: `#60A5FA`
- `TaskCompleted`: `#34D399`
- **Fontes:** `UbuntuSansNerdFontFamily` e `UbuntuSansMonoNerdFontFamily` (`NerdIcons` e `NerdIconCatalog` em `NerdFontIcons.kt`).
