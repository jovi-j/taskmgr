package com.taskmgr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskmgr.data.sync.DeviceCalendarAccount
import com.taskmgr.di.AppContainer
import com.taskmgr.domain.export.MarkdownTransferEngine
import com.taskmgr.domain.export.TaskMgrBackupSnapshot
import com.taskmgr.domain.logic.DaysOfWeekMask
import com.taskmgr.domain.logic.HabitCompletionCalculator
import com.taskmgr.domain.logic.HabitStats
import com.taskmgr.domain.logic.HabitStreakCalculator
import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.HabitGroup
import com.taskmgr.domain.model.HabitLog
import com.taskmgr.domain.model.Moment
import com.taskmgr.domain.model.Task
import com.taskmgr.scheduler.VibrationPattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

data class HabitWithLog(
    val habit: Habit,
    val log: HabitLog?,
    val stats: HabitStats = HabitStats(0, 0),
    val group: HabitGroup? = null
) {
    val accumulatedAmount: Double get() = log?.accumulatedAmount ?: 0.0
    val isCompleted: Boolean get() = log?.isCompleted == true
    val isDismissedForDay: Boolean get() = log?.dismissedForDay == true
}

data class TodayUiState(
    val selectedEpochDay: Long = LocalDate.now().toEpochDay(),
    val moments: List<Moment> = emptyList(),
    val habitGroups: List<HabitGroup> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val habits: List<HabitWithLog> = emptyList()
)

data class CalendarDaySummary(
    val epochDay: Long,
    val taskCount: Int,
    val completedTaskCount: Int,
    val activeHabitCount: Int,
    val completedHabitCount: Int,
    val partialHabitCount: Int,
    val dismissedHabitCount: Int
)

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val daySummaries: Map<Long, CalendarDaySummary> = emptyMap()
)

data class DayDetailUiState(
    val epochDay: Long,
    val tasks: List<Task> = emptyList(),
    val habits: List<HabitWithLog> = emptyList(),
    val laterStartHabits: List<Habit> = emptyList(),
    val habitGroupsById: Map<Long, HabitGroup> = emptyMap()
)

data class ExportSyncUiState(
    val hasCalendarPermissions: Boolean = false,
    val availableCalendars: List<DeviceCalendarAccount> = emptyList(),
    val selectedCalendarId: Long? = null,
    val autoSyncTasksEnabled: Boolean = false,
    val isSyncingCalendar: Boolean = false,
    val lastSyncSummary: String? = null,
    val statusMessage: String? = null,
    val isStatusError: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class TaskMgrViewModel(private val container: AppContainer) : ViewModel() {

    private val _selectedEpochDay = MutableStateFlow(LocalDate.now().toEpochDay())
    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    private val _dayDetailEpochDay = MutableStateFlow<Long?>(null)

    private val _vibrationPattern = MutableStateFlow(container.vibrationController.getConfiguredPattern())
    val vibrationPattern: StateFlow<VibrationPattern> = _vibrationPattern

    private val _showOnboarding = MutableStateFlow(!container.vibrationController.hasCompletedOnboarding())
    val showOnboarding: StateFlow<Boolean> = _showOnboarding

    private val _exportSyncUiState = MutableStateFlow(
        ExportSyncUiState(
            hasCalendarPermissions = container.calendarSyncManager.hasCalendarPermissions(),
            selectedCalendarId = container.calendarSyncManager.getSelectedCalendarId(),
            autoSyncTasksEnabled = container.calendarSyncManager.isAutoSyncEnabled(),
            lastSyncSummary = container.calendarSyncManager.getLastSyncSummary()
        )
    )
    val exportSyncUiState: StateFlow<ExportSyncUiState> = _exportSyncUiState

    init {
        refreshCalendarAccounts()
    }

    val moments: StateFlow<List<Moment>> = container.momentRepository.observeMoments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val habitGroups: StateFlow<List<HabitGroup>> = container.habitGroupRepository.observeGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allTasks: StateFlow<List<Task>> = container.taskRepository.observeAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allHabits: StateFlow<List<Habit>> = container.habitRepository.observeHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allHabitLogs: StateFlow<List<HabitLog>> = container.habitRepository.observeAllHabitLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val habitRetentionSummaries: StateFlow<List<HabitWithLog>> = combine(
        allHabits,
        allHabitLogs,
        habitGroups
    ) { habitList, allLogs, groups ->
        val today = LocalDate.now().toEpochDay()
        val groupsById = groups.associateBy { it.id }
        val logsByHabit = allLogs.groupBy { it.habitId }
        habitList
            .filter { it.isActive }
            .map { habit ->
                val habitLogs = logsByHabit[habit.id].orEmpty()
                val stats = HabitStreakCalculator.calculate(habit, habitLogs, today)
                HabitWithLog(
                    habit = habit,
                    log = habitLogs.firstOrNull { it.epochDay == today },
                    stats = stats,
                    group = habit.groupId?.let { groupsById[it] }
                )
            }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allHabitsWithStats: StateFlow<List<HabitWithLog>> = combine(
        allHabits,
        allHabitLogs,
        habitGroups
    ) { habitList, allLogs, groups ->
        val today = LocalDate.now().toEpochDay()
        val groupsById = groups.associateBy { it.id }
        val logsByHabit = allLogs.groupBy { it.habitId }
        habitList.map { habit ->
            val habitLogs = logsByHabit[habit.id].orEmpty()
            val stats = HabitStreakCalculator.calculate(habit, habitLogs, today)
            HabitWithLog(
                habit = habit,
                log = habitLogs.firstOrNull { it.epochDay == today },
                stats = stats,
                group = habit.groupId?.let { groupsById[it] }
            )
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayUiState: StateFlow<TodayUiState> = combine(
        _selectedEpochDay,
        moments,
        habitGroups,
        _selectedEpochDay.flatMapLatest { day -> container.taskRepository.observeTasksForDay(day) },
        combine(allHabits, allHabitLogs) { habits, logs -> habits to logs }
    ) { day, momentList, groupList, taskList, (habitList, allLogs) ->
        val groupsById = groupList.associateBy { it.id }
        val logsByHabitAll = allLogs.groupBy { it.habitId }
        val activeHabitsForDay = habitList
            .filter { habit ->
                habit.isActive &&
                    habit.startEpochDay <= day &&
                    DaysOfWeekMask.containsEpochDay(habit.activeDaysMask, day)
            }
            .map { habit ->
                val habitLogs = logsByHabitAll[habit.id].orEmpty()
                val logForDay = habitLogs.firstOrNull { it.epochDay == day }
                val stats = HabitStreakCalculator.calculate(habit, habitLogs, day)
                HabitWithLog(
                    habit = habit,
                    log = logForDay,
                    stats = stats,
                    group = habit.groupId?.let { groupsById[it] }
                )
            }

        TodayUiState(
            selectedEpochDay = day,
            moments = momentList,
            habitGroups = groupList,
            tasks = taskList,
            habits = activeHabitsForDay
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    val calendarUiState: StateFlow<CalendarUiState> = _selectedMonth.flatMapLatest { month ->
        val startDay = month.atDay(1).toEpochDay()
        val endDay = month.atEndOfMonth().toEpochDay()
        combine(
            container.taskRepository.observeTasksForRange(startDay, endDay),
            allHabits,
            container.habitRepository.observeHabitLogsForRange(startDay, endDay)
        ) { tasksInRange, habits, logsInRange ->
            val logsByDayAndHabit = logsInRange.groupBy { it.epochDay }
                .mapValues { (_, list) -> list.associateBy { it.habitId } }

            val summaries = buildMap {
                for (day in startDay..endDay) {
                    val dayTasks = tasksInRange.filter { it.isActiveOnDay(day) }
                    val dayHabits = habits.filter {
                        it.isActive &&
                            it.startEpochDay <= day &&
                            DaysOfWeekMask.containsEpochDay(it.activeDaysMask, day)
                    }
                    val dayLogs = logsByDayAndHabit[day].orEmpty()

                    var completedHabits = 0
                    var partialHabits = 0
                    var dismissedHabits = 0
                    for (habit in dayHabits) {
                        val log = dayLogs[habit.id]
                        when {
                            log?.isCompleted == true -> completedHabits++
                            log?.dismissedForDay == true -> dismissedHabits++
                            (log?.accumulatedAmount ?: 0.0) > 0.0 -> partialHabits++
                        }
                    }

                    put(
                        day,
                        CalendarDaySummary(
                            epochDay = day,
                            taskCount = dayTasks.size,
                            completedTaskCount = dayTasks.count { it.isCompleted },
                            activeHabitCount = dayHabits.size,
                            completedHabitCount = completedHabits,
                            partialHabitCount = partialHabits,
                            dismissedHabitCount = dismissedHabits
                        )
                    )
                }
            }
            CalendarUiState(month = month, daySummaries = summaries)
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    val dayDetailUiState: StateFlow<DayDetailUiState?> = _dayDetailEpochDay.flatMapLatest { day ->
        if (day == null) {
            flowOf(null)
        } else {
            combine(
                container.taskRepository.observeTasksForDay(day),
                allHabits,
                allHabitLogs,
                habitGroups
            ) { tasks, habits, allLogs, groups ->
                val groupsById = groups.associateBy { it.id }
                val logsByHabitAll = allLogs.groupBy { it.habitId }
                val activeHabits = habits.filter {
                    it.isActive &&
                        it.startEpochDay <= day &&
                        DaysOfWeekMask.containsEpochDay(it.activeDaysMask, day)
                }.map { habit ->
                    val habitLogs = logsByHabitAll[habit.id].orEmpty()
                    HabitWithLog(
                        habit = habit,
                        log = habitLogs.firstOrNull { it.epochDay == day },
                        stats = HabitStreakCalculator.calculate(habit, habitLogs, day),
                        group = habit.groupId?.let { groupsById[it] }
                    )
                }
                val laterStartHabits = habits.filter {
                    it.isActive &&
                        it.startEpochDay > day &&
                        DaysOfWeekMask.containsEpochDay(it.activeDaysMask, day)
                }

                DayDetailUiState(
                    epochDay = day,
                    tasks = tasks,
                    habits = activeHabits,
                    laterStartHabits = laterStartHabits,
                    habitGroupsById = groupsById
                )
            }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun dismissOnboarding() {
        container.vibrationController.setOnboardingCompleted()
        _showOnboarding.value = false
    }

    fun selectDay(epochDay: Long) {
        _selectedEpochDay.value = epochDay
    }

    suspend fun getTaskById(taskId: Long): Task? = withContext(Dispatchers.IO) {
        container.taskRepository.getById(taskId)
    }

    fun selectMonth(month: YearMonth) {
        _selectedMonth.value = month
    }

    fun openDayDetail(epochDay: Long) {
        _dayDetailEpochDay.value = epochDay
    }

    fun closeDayDetail() {
        _dayDetailEpochDay.value = null
    }

    fun upsertTask(task: Task) {
        viewModelScope.launch {
            container.taskRepository.upsert(task)
            triggerAutoSyncIfEnabled()
        }
    }

    fun toggleTaskCompleted(task: Task) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            container.taskRepository.setCompleted(
                taskId = task.id,
                completed = !task.isCompleted,
                completedAtEpochMillis = if (!task.isCompleted) now else null
            )
            triggerAutoSyncIfEnabled()
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            container.taskRepository.delete(taskId)
            triggerAutoSyncIfEnabled()
        }
    }

    fun upsertHabit(habit: Habit) {
        viewModelScope.launch {
            container.habitRepository.upsertHabit(habit)
        }
    }

    fun includeHabitFromPastDay(habit: Habit, epochDay: Long) {
        viewModelScope.launch {
            val updated = habit.copy(startEpochDay = minOf(habit.startEpochDay, epochDay))
            container.habitRepository.upsertHabit(updated)
        }
    }

    fun incrementHabit(habitId: Long, epochDay: Long) {
        viewModelScope.launch {
            container.habitRepository.incrementHabit(
                habitId = habitId,
                epochDay = epochDay,
                nowEpochMillis = System.currentTimeMillis()
            )
        }
    }

    fun updateHabitLogManual(
        habitId: Long,
        epochDay: Long,
        accumulatedAmount: Double,
        dismissedForDay: Boolean
    ) {
        viewModelScope.launch {
            container.habitRepository.updateHabitLogManual(
                habitId = habitId,
                epochDay = epochDay,
                accumulatedAmount = accumulatedAmount,
                dismissedForDay = dismissedForDay,
                nowEpochMillis = System.currentTimeMillis()
            )
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            container.habitRepository.deleteHabit(habitId)
        }
    }

    fun upsertHabitGroup(group: HabitGroup, onCreated: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = container.habitGroupRepository.upsert(group)
            onCreated?.invoke(id)
        }
    }

    fun deleteHabitGroup(groupId: Long) {
        viewModelScope.launch {
            container.habitGroupRepository.delete(groupId)
        }
    }

    fun upsertMoment(moment: Moment) {
        viewModelScope.launch {
            container.momentRepository.upsert(moment)
        }
    }

    fun deleteMoment(momentId: Long) {
        viewModelScope.launch {
            container.momentRepository.delete(momentId)
        }
    }

    fun setVibrationPattern(pattern: VibrationPattern) {
        container.vibrationController.setConfiguredPattern(pattern)
        _vibrationPattern.value = pattern
    }

    fun testVibration() {
        container.vibrationController.vibrateReminder(_vibrationPattern.value)
    }

    fun stopVibration() {
        container.vibrationController.cancel()
    }

    fun triggerTestReminder(task: Task) {
        viewModelScope.launch {
            val momentName = task.momentId?.let { id -> moments.value.firstOrNull { it.id == id }?.name }
            val payload = HabitCompletionCalculator.buildTaskPayload(
                task = task,
                epochDay = _selectedEpochDay.value,
                momentName = momentName,
                isAdvanceReminder = true
            )
            container.alertDispatcher.triggerFullScreenReminder(payload)
        }
    }

    fun triggerTestReminder(habitWithLog: HabitWithLog, epochDay: Long) {
        viewModelScope.launch {
            val momentName = habitWithLog.habit.momentId?.let { id ->
                moments.value.firstOrNull { it.id == id }?.name
            }
            val payload = HabitCompletionCalculator.buildHabitPayload(
                habit = habitWithLog.habit,
                log = habitWithLog.log,
                epochDay = epochDay,
                momentName = momentName
            )
            container.alertDispatcher.triggerFullScreenReminder(payload)
        }
    }

    fun refreshCalendarAccounts() {
        viewModelScope.launch {
            val hasPerm = container.calendarSyncManager.hasCalendarPermissions()
            val calendars = if (hasPerm) {
                container.calendarSyncManager.listWritableCalendars()
            } else {
                emptyList()
            }
            val currentSelected = container.calendarSyncManager.getSelectedCalendarId()
            val effectiveSelected = calendars.firstOrNull { it.id == currentSelected }?.id
                ?: calendars.firstOrNull { it.isPrimary }?.id
                ?: calendars.firstOrNull()?.id
            if (effectiveSelected != null && effectiveSelected != currentSelected) {
                container.calendarSyncManager.setSelectedCalendarId(effectiveSelected)
            }
            _exportSyncUiState.value = _exportSyncUiState.value.copy(
                hasCalendarPermissions = hasPerm,
                availableCalendars = calendars,
                selectedCalendarId = effectiveSelected,
                autoSyncTasksEnabled = container.calendarSyncManager.isAutoSyncEnabled(),
                lastSyncSummary = container.calendarSyncManager.getLastSyncSummary()
            )
        }
    }

    fun selectCalendarForSync(calendarId: Long) {
        container.calendarSyncManager.setSelectedCalendarId(calendarId)
        _exportSyncUiState.value = _exportSyncUiState.value.copy(selectedCalendarId = calendarId)
    }

    fun setAutoSyncTasksEnabled(enabled: Boolean) {
        container.calendarSyncManager.setAutoSyncEnabled(enabled)
        _exportSyncUiState.value = _exportSyncUiState.value.copy(autoSyncTasksEnabled = enabled)
        if (enabled) {
            syncCalendarNow()
        }
    }

    fun syncCalendarNow() {
        viewModelScope.launch {
            _exportSyncUiState.value = _exportSyncUiState.value.copy(
                isSyncingCalendar = true,
                statusMessage = null
            )
            val result = container.calendarSyncManager.syncTasksNow()
            _exportSyncUiState.value = _exportSyncUiState.value.copy(
                isSyncingCalendar = false,
                lastSyncSummary = container.calendarSyncManager.getLastSyncSummary(),
                statusMessage = result.message,
                isStatusError = !result.success
            )
        }
    }

    private suspend fun triggerAutoSyncIfEnabled() {
        if (container.calendarSyncManager.isAutoSyncEnabled() &&
            container.calendarSyncManager.hasCalendarPermissions()
        ) {
            val result = container.calendarSyncManager.syncTasksNow()
            if (result.success) {
                _exportSyncUiState.value = _exportSyncUiState.value.copy(
                    lastSyncSummary = container.calendarSyncManager.getLastSyncSummary()
                )
            }
        }
    }

    fun setExportSyncStatusMessage(message: String?, isError: Boolean = false) {
        _exportSyncUiState.value = _exportSyncUiState.value.copy(
            statusMessage = message,
            isStatusError = isError
        )
    }

    suspend fun buildTaskMgrMarkdownExport(): String = withContext(Dispatchers.IO) {
        val snapshot = collectBackupSnapshot()
        MarkdownTransferEngine.exportTaskMgrMarkdown(snapshot)
    }

    suspend fun buildNotionObsidianMarkdownExport(): String = withContext(Dispatchers.IO) {
        val snapshot = collectBackupSnapshot()
        MarkdownTransferEngine.exportNotionObsidianMarkdown(snapshot)
    }

    private suspend fun collectBackupSnapshot(): TaskMgrBackupSnapshot {
        val allMoments = container.momentRepository.observeMoments().first()
        val allGroups = container.habitGroupRepository.observeGroups().first()
        val allTasks = container.taskRepository.getAllTasks()
        val allHabitsList = container.habitRepository.observeHabits().first()
        val allLogsList = container.habitRepository.observeAllHabitLogs().first()
        return TaskMgrBackupSnapshot(
            moments = allMoments,
            habitGroups = allGroups,
            tasks = allTasks,
            habits = allHabitsList,
            habitLogs = allLogsList,
            referenceEpochDay = LocalDate.now().toEpochDay()
        )
    }

    fun importTaskMgrMarkdownContent(markdownContent: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val bundle = MarkdownTransferEngine.parseTaskMgrMarkdown(
                markdown = markdownContent,
                defaultEpochDay = _selectedEpochDay.value
            )
            if (!bundle.isFullTaskMgrFormat && bundle.tasks.isEmpty()) {
                _exportSyncUiState.value = _exportSyncUiState.value.copy(
                    statusMessage = "Nenhum dado válido encontrado no arquivo .taskmgr.md.",
                    isStatusError = true
                )
                return@launch
            }

            if (bundle.isFullTaskMgrFormat) {
                val existingMoments = container.momentRepository.observeMoments().first()
                val momentIdMap = mutableMapOf<Long, Long>()
                for (moment in bundle.moments) {
                    val existing = existingMoments.firstOrNull {
                        it.name.equals(moment.name, ignoreCase = true)
                    }
                    val targetId = if (existing != null) {
                        container.momentRepository.upsert(moment.copy(id = existing.id))
                    } else {
                        container.momentRepository.upsert(moment.copy(id = 0L))
                    }
                    if (moment.id != 0L) {
                        momentIdMap[moment.id] = targetId
                    }
                }

                val existingGroups = container.habitGroupRepository.observeGroups().first()
                val groupIdMap = mutableMapOf<Long, Long>()
                for (group in bundle.habitGroups) {
                    val existing = existingGroups.firstOrNull {
                        it.name.equals(group.name, ignoreCase = true)
                    }
                    val targetId = if (existing != null) {
                        container.habitGroupRepository.upsert(group.copy(id = existing.id))
                    } else {
                        container.habitGroupRepository.upsert(group.copy(id = 0L))
                    }
                    if (group.id != 0L) {
                        groupIdMap[group.id] = targetId
                    }
                }

                val existingTasks = container.taskRepository.getAllTasks()
                var importedTasksCount = 0
                for (task in bundle.tasks) {
                    val mappedMomentId = task.momentId?.let { momentIdMap[it] }
                    val duplicate = existingTasks.firstOrNull {
                        it.title == task.title &&
                            it.startEpochDay == task.startEpochDay &&
                            it.endEpochDay == task.endEpochDay
                    }
                    val taskToSave = task.copy(
                        id = duplicate?.id ?: 0L,
                        momentId = mappedMomentId
                    )
                    container.taskRepository.upsert(taskToSave)
                    importedTasksCount++
                }

                val existingHabits = container.habitRepository.observeHabits().first()
                val habitIdMap = mutableMapOf<Long, Long>()
                var importedHabitsCount = 0
                for (habit in bundle.habits) {
                    val mappedMomentId = habit.momentId?.let { momentIdMap[it] }
                    val mappedGroupId = habit.groupId?.let { groupIdMap[it] }
                    val existing = existingHabits.firstOrNull {
                        it.title.equals(habit.title, ignoreCase = true)
                    }
                    val savedHabitId = container.habitRepository.upsertHabit(
                        habit.copy(
                            id = existing?.id ?: 0L,
                            momentId = mappedMomentId,
                            groupId = mappedGroupId
                        )
                    )
                    if (habit.id != 0L) {
                        habitIdMap[habit.id] = savedHabitId
                    }
                    importedHabitsCount++
                }

                var importedLogsCount = 0
                for (log in bundle.habitLogs) {
                    val targetHabitId = habitIdMap[log.habitId] ?: continue
                    container.habitRepository.updateHabitLogManual(
                        habitId = targetHabitId,
                        epochDay = log.epochDay,
                        accumulatedAmount = log.accumulatedAmount,
                        dismissedForDay = log.dismissedForDay,
                        nowEpochMillis = log.updatedAtEpochMillis
                    )
                    importedLogsCount++
                }

                triggerAutoSyncIfEnabled()
                _exportSyncUiState.value = _exportSyncUiState.value.copy(
                    statusMessage = "Importado .taskmgr.md: $importedTasksCount tarefas, $importedHabitsCount hábitos, " +
                        "${bundle.habitGroups.size} grupos, ${bundle.moments.size} momentos e $importedLogsCount registros.",
                    isStatusError = false
                )
            } else {
                var importedCount = 0
                for (task in bundle.tasks) {
                    container.taskRepository.upsert(task.copy(id = 0L))
                    importedCount++
                }
                triggerAutoSyncIfEnabled()
                _exportSyncUiState.value = _exportSyncUiState.value.copy(
                    statusMessage = "Importadas $importedCount tarefas de checklist GFM.",
                    isStatusError = false
                )
            }
        }
    }

    fun importGfmMarkdownContent(markdownContent: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val tasks = MarkdownTransferEngine.parseGfmMarkdownChecklists(
                markdown = markdownContent,
                defaultEpochDay = _selectedEpochDay.value
            )
            if (tasks.isEmpty()) {
                _exportSyncUiState.value = _exportSyncUiState.value.copy(
                    statusMessage = "Nenhum item de checklist GFM (- [ ] ou - [x]) encontrado no arquivo .md.",
                    isStatusError = true
                )
                return@launch
            }
            val existingTasks = container.taskRepository.getAllTasks()
            var importedCount = 0
            for (task in tasks) {
                val duplicate = existingTasks.firstOrNull {
                    it.title == task.title &&
                        it.startEpochDay == task.startEpochDay &&
                        it.endEpochDay == task.endEpochDay
                }
                container.taskRepository.upsert(task.copy(id = duplicate?.id ?: 0L))
                importedCount++
            }
            triggerAutoSyncIfEnabled()
            _exportSyncUiState.value = _exportSyncUiState.value.copy(
                statusMessage = "Importadas $importedCount tarefas do arquivo .md (Notion/Obsidian).",
                isStatusError = false
            )
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TaskMgrViewModel(container) as T
        }
    }
}
