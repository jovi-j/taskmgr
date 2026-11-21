package com.taskmgr.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.jovij.taskmgr.TaskMgrApplication
import com.taskmgr.data.local.toDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CalendarWidgetSnapshot(
    val displayYear: Int,
    val displayMonth: Int,
    val selectedEpochDay: Long,
    val todayEpochDay: Long,
    val pendingDotEpochDays: Set<Long>,
    val tasks: List<CalendarWidgetTaskItem>
)

object CalendarWidgetPrefKeys {
    val DISPLAY_YEAR = intPreferencesKey("cal_display_year")
    val DISPLAY_MONTH = intPreferencesKey("cal_display_month")
    val SELECTED_EPOCH_DAY = longPreferencesKey("cal_selected_epoch_day")
    val LAST_TODAY_EPOCH_DAY = longPreferencesKey("cal_last_today_epoch_day")
    val DOT_DAYS_CSV = stringPreferencesKey("cal_dot_days_csv")
    val TASKS_SERIALIZED = stringPreferencesKey("cal_tasks_serialized")
    val REVISION = longPreferencesKey("cal_revision")
}

object CalendarWidgetUpdater {

    private val updaterScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun requestUpdateAll(context: Context) {
        val appContext = context.applicationContext
        updaterScope.launch {
            runCatching { syncAllWidgets(appContext) }
        }
    }

    suspend fun loadSnapshot(
        context: Context,
        displayYear: Int,
        displayMonth: Int,
        selectedEpochDay: Long,
        todayEpochDay: Long = LocalDate.now().toEpochDay()
    ): CalendarWidgetSnapshot {
        val app = (context.applicationContext as? TaskMgrApplication)
        val allTasks = app?.container?.database?.taskDao()?.getAll()?.map { it.toDomain() }.orEmpty()
        val dots = CalendarWidgetCalculator.computeMonthPendingDotEpochDays(
            year = displayYear,
            month = displayMonth,
            todayEpochDay = todayEpochDay,
            tasks = allTasks
        )
        val dayTasks = CalendarWidgetCalculator.tasksForDay(selectedEpochDay, allTasks)
        return CalendarWidgetSnapshot(
            displayYear = displayYear,
            displayMonth = displayMonth,
            selectedEpochDay = selectedEpochDay,
            todayEpochDay = todayEpochDay,
            pendingDotEpochDays = dots,
            tasks = dayTasks
        )
    }

    suspend fun syncWidget(
        context: Context,
        glanceId: GlanceId,
        overrideYear: Int? = null,
        overrideMonth: Int? = null,
        overrideSelectedEpochDay: Long? = null
    ) {
        val appContext = context.applicationContext
        val today = LocalDate.now()
        val todayEpochDay = today.toEpochDay()
        val prefs = runCatching {
            getAppWidgetState(appContext, PreferencesGlanceStateDefinition, glanceId)
        }.getOrNull()

        val lastToday = prefs?.get(CalendarWidgetPrefKeys.LAST_TODAY_EPOCH_DAY) ?: -1L
        val rolledOverDay = lastToday != todayEpochDay && overrideSelectedEpochDay == null && overrideYear == null

        val year = overrideYear ?: if (rolledOverDay) {
            today.year
        } else {
            prefs?.get(CalendarWidgetPrefKeys.DISPLAY_YEAR) ?: today.year
        }
        val month = overrideMonth ?: if (rolledOverDay) {
            today.monthValue
        } else {
            (prefs?.get(CalendarWidgetPrefKeys.DISPLAY_MONTH) ?: today.monthValue).coerceIn(1, 12)
        }
        val selectedDay = overrideSelectedEpochDay ?: if (rolledOverDay) {
            todayEpochDay
        } else {
            prefs?.get(CalendarWidgetPrefKeys.SELECTED_EPOCH_DAY) ?: todayEpochDay
        }

        val snapshot = loadSnapshot(
            context = appContext,
            displayYear = year,
            displayMonth = month,
            selectedEpochDay = selectedDay,
            todayEpochDay = todayEpochDay
        )
        writeSnapshotToPreferences(appContext, glanceId, snapshot)
        CalendarTasksGlanceWidget().update(appContext, glanceId)
    }

    suspend fun selectDay(context: Context, glanceId: GlanceId, epochDay: Long) {
        val date = LocalDate.ofEpochDay(epochDay)
        syncWidget(
            context = context,
            glanceId = glanceId,
            overrideYear = date.year,
            overrideMonth = date.monthValue,
            overrideSelectedEpochDay = epochDay
        )
    }

    suspend fun changeMonth(context: Context, glanceId: GlanceId, monthDelta: Int) {
        val appContext = context.applicationContext
        val today = LocalDate.now()
        val prefs = runCatching {
            getAppWidgetState(appContext, PreferencesGlanceStateDefinition, glanceId)
        }.getOrNull()
        val curYear = prefs?.get(CalendarWidgetPrefKeys.DISPLAY_YEAR) ?: today.year
        val curMonth = (prefs?.get(CalendarWidgetPrefKeys.DISPLAY_MONTH) ?: today.monthValue).coerceIn(1, 12)
        val targetYm = YearMonth.of(curYear, curMonth).plusMonths(monthDelta.toLong())
        val selectedDay = prefs?.get(CalendarWidgetPrefKeys.SELECTED_EPOCH_DAY) ?: today.toEpochDay()
        syncWidget(
            context = appContext,
            glanceId = glanceId,
            overrideYear = targetYm.year,
            overrideMonth = targetYm.monthValue,
            overrideSelectedEpochDay = selectedDay
        )
    }

    suspend fun jumpToToday(context: Context, glanceId: GlanceId) {
        val today = LocalDate.now()
        syncWidget(
            context = context,
            glanceId = glanceId,
            overrideYear = today.year,
            overrideMonth = today.monthValue,
            overrideSelectedEpochDay = today.toEpochDay()
        )
    }

    suspend fun syncAllWidgets(context: Context) {
        val appContext = context.applicationContext
        val manager = GlanceAppWidgetManager(appContext)
        val glanceIds = runCatching {
            manager.getGlanceIds(CalendarTasksGlanceWidget::class.java)
        }.getOrDefault(emptyList())
        for (glanceId in glanceIds) {
            runCatching { syncWidget(appContext, glanceId) }
        }
    }

    internal suspend fun writeSnapshotToPreferences(
        context: Context,
        glanceId: GlanceId,
        snapshot: CalendarWidgetSnapshot
    ) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[CalendarWidgetPrefKeys.DISPLAY_YEAR] = snapshot.displayYear
                this[CalendarWidgetPrefKeys.DISPLAY_MONTH] = snapshot.displayMonth
                this[CalendarWidgetPrefKeys.SELECTED_EPOCH_DAY] = snapshot.selectedEpochDay
                this[CalendarWidgetPrefKeys.LAST_TODAY_EPOCH_DAY] = snapshot.todayEpochDay
                this[CalendarWidgetPrefKeys.DOT_DAYS_CSV] =
                    CalendarWidgetCalculator.serializeDotDays(snapshot.pendingDotEpochDays)
                this[CalendarWidgetPrefKeys.TASKS_SERIALIZED] =
                    CalendarWidgetCalculator.serializeTasks(snapshot.tasks)
                this[CalendarWidgetPrefKeys.REVISION] = (this[CalendarWidgetPrefKeys.REVISION] ?: 0L) + 1L
            }
        }
    }

    internal fun snapshotFromPreferences(prefs: Preferences): CalendarWidgetSnapshot? {
        val year = prefs[CalendarWidgetPrefKeys.DISPLAY_YEAR] ?: return null
        val month = prefs[CalendarWidgetPrefKeys.DISPLAY_MONTH] ?: return null
        val selected = prefs[CalendarWidgetPrefKeys.SELECTED_EPOCH_DAY] ?: return null
        val today = prefs[CalendarWidgetPrefKeys.LAST_TODAY_EPOCH_DAY] ?: LocalDate.now().toEpochDay()
        return CalendarWidgetSnapshot(
            displayYear = year,
            displayMonth = month.coerceIn(1, 12),
            selectedEpochDay = selected,
            todayEpochDay = today,
            pendingDotEpochDays = CalendarWidgetCalculator.deserializeDotDays(
                prefs[CalendarWidgetPrefKeys.DOT_DAYS_CSV].orEmpty()
            ),
            tasks = CalendarWidgetCalculator.deserializeTasks(
                prefs[CalendarWidgetPrefKeys.TASKS_SERIALIZED].orEmpty()
            )
        )
    }
}
