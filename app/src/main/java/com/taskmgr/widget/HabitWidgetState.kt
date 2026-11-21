package com.taskmgr.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
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
import com.taskmgr.domain.logic.HabitCompletionCalculator
import com.taskmgr.domain.logic.HabitStreakCalculator
import com.taskmgr.ui.components.NerdIcons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

const val DEFAULT_WIDGET_GROUP_COLOR_ARGB: Long = 0xFF1E1B4BL

data class HabitWidgetSnapshot(
    val habitId: Long,
    val habitExists: Boolean,
    val title: String,
    val iconGlyph: String,
    val groupColorArgb: Long,
    val remainingText: String,
    val progressText: String,
    val incrementHint: String,
    val progressFraction: Float,
    val streakDays: Int,
    val isCompleted: Boolean,
    val epochDay: Long
)

object HabitWidgetPrefKeys {
    val HABIT_ID = longPreferencesKey("habit_id")
    val HABIT_EXISTS = booleanPreferencesKey("habit_exists")
    val TITLE = stringPreferencesKey("title")
    val ICON_GLYPH = stringPreferencesKey("icon_glyph")
    val GROUP_COLOR_ARGB = longPreferencesKey("group_color_argb")
    val REMAINING_TEXT = stringPreferencesKey("remaining_text")
    val PROGRESS_TEXT = stringPreferencesKey("progress_text")
    val INCREMENT_HINT = stringPreferencesKey("increment_hint")
    val PROGRESS_FRACTION = floatPreferencesKey("progress_fraction")
    val STREAK_DAYS = intPreferencesKey("streak_days")
    val IS_COMPLETED = booleanPreferencesKey("is_completed")
    val EPOCH_DAY = longPreferencesKey("epoch_day")
    val REVISION = longPreferencesKey("revision")
}

object HabitWidgetUpdater {

    private val updaterScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun requestUpdateAll(context: Context) {
        val appContext = context.applicationContext
        updaterScope.launch {
            runCatching { syncAllWidgets(appContext) }
        }
    }

    suspend fun loadSnapshot(
        context: Context,
        habitId: Long,
        epochDay: Long = LocalDate.now().toEpochDay()
    ): HabitWidgetSnapshot {
        val app = context.applicationContext as? TaskMgrApplication
            ?: return missingSnapshot(habitId, epochDay)
        val db = app.container.database
        val habit = db.habitDao().getById(habitId)?.toDomain()
            ?: return missingSnapshot(habitId, epochDay)
        val group = habit.groupId?.let { db.habitGroupDao().getById(it)?.toDomain() }
        val allLogs = db.habitLogDao().getLogsForHabit(habitId).map { it.toDomain() }
        val todayLog = allLogs.firstOrNull { it.epochDay == epochDay }
        val accumulated = todayLog?.accumulatedAmount ?: 0.0
        val completed = todayLog?.isCompleted == true ||
            HabitCompletionCalculator.isCompleted(accumulated, habit.targetAmount)
        val stats = HabitStreakCalculator.calculate(habit, allLogs, epochDay)

        val resolvedGlyph = habit.iconGlyph?.takeIf { it.isNotBlank() }
            ?: group?.iconGlyph?.takeIf { it.isNotBlank() }
            ?: NerdIcons.Fire
        val resolvedColor = group?.colorArgb ?: DEFAULT_WIDGET_GROUP_COLOR_ARGB

        return HabitWidgetSnapshot(
            habitId = habit.id,
            habitExists = true,
            title = habit.title,
            iconGlyph = resolvedGlyph,
            groupColorArgb = resolvedColor,
            remainingText = HabitCompletionCalculator.formatWidgetProgressText(habit, accumulated),
            progressText = HabitCompletionCalculator.formatWidgetProgressText(habit, accumulated),
            incrementHint = HabitCompletionCalculator.formatCompactIncrementHint(habit),
            progressFraction = HabitCompletionCalculator.progressFraction(habit, accumulated),
            streakDays = stats.currentStreakDays,
            isCompleted = completed,
            epochDay = epochDay
        )
    }

    suspend fun bindHabitToWidget(
        context: Context,
        appWidgetId: Int,
        habitId: Long
    ) {
        val appContext = context.applicationContext
        val manager = GlanceAppWidgetManager(appContext)
        val glanceId = runCatching { manager.getGlanceIdBy(appWidgetId) }.getOrNull() ?: return
        val snapshot = loadSnapshot(appContext, habitId)
        writeSnapshotAndUpdate(appContext, glanceId, snapshot)
    }

    suspend fun syncWidget(
        context: Context,
        glanceId: GlanceId
    ) {
        val appContext = context.applicationContext
        val prefs = runCatching {
            getAppWidgetState(appContext, PreferencesGlanceStateDefinition, glanceId)
        }.getOrNull()
        val habitId = prefs?.get(HabitWidgetPrefKeys.HABIT_ID) ?: -1L
        if (habitId <= 0L) {
            HabitGlanceWidget().update(appContext, glanceId)
            return
        }
        val snapshot = loadSnapshot(appContext, habitId)
        writeSnapshotAndUpdate(appContext, glanceId, snapshot)
    }

    suspend fun syncAllWidgets(context: Context) {
        val appContext = context.applicationContext
        val manager = GlanceAppWidgetManager(appContext)
        val glanceIds = runCatching {
            manager.getGlanceIds(HabitGlanceWidget::class.java)
        }.getOrDefault(emptyList())
        for (glanceId in glanceIds) {
            runCatching { syncWidget(appContext, glanceId) }
        }
    }

    internal suspend fun writeSnapshotToPreferences(
        context: Context,
        glanceId: GlanceId,
        snapshot: HabitWidgetSnapshot
    ) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[HabitWidgetPrefKeys.HABIT_ID] = snapshot.habitId
                this[HabitWidgetPrefKeys.HABIT_EXISTS] = snapshot.habitExists
                this[HabitWidgetPrefKeys.TITLE] = snapshot.title
                this[HabitWidgetPrefKeys.ICON_GLYPH] = snapshot.iconGlyph
                this[HabitWidgetPrefKeys.GROUP_COLOR_ARGB] = snapshot.groupColorArgb
                this[HabitWidgetPrefKeys.REMAINING_TEXT] = snapshot.remainingText
                this[HabitWidgetPrefKeys.PROGRESS_TEXT] = snapshot.progressText
                this[HabitWidgetPrefKeys.INCREMENT_HINT] = snapshot.incrementHint
                this[HabitWidgetPrefKeys.PROGRESS_FRACTION] = snapshot.progressFraction
                this[HabitWidgetPrefKeys.STREAK_DAYS] = snapshot.streakDays
                this[HabitWidgetPrefKeys.IS_COMPLETED] = snapshot.isCompleted
                this[HabitWidgetPrefKeys.EPOCH_DAY] = snapshot.epochDay
                this[HabitWidgetPrefKeys.REVISION] = (this[HabitWidgetPrefKeys.REVISION] ?: 0L) + 1L
            }
        }
    }

    private suspend fun writeSnapshotAndUpdate(
        context: Context,
        glanceId: GlanceId,
        snapshot: HabitWidgetSnapshot
    ) {
        writeSnapshotToPreferences(context, glanceId, snapshot)
        HabitGlanceWidget().update(context, glanceId)
    }

    internal fun snapshotFromPreferences(prefs: Preferences): HabitWidgetSnapshot? {
        val habitId = prefs[HabitWidgetPrefKeys.HABIT_ID] ?: -1L
        if (habitId <= 0L) return null
        return HabitWidgetSnapshot(
            habitId = habitId,
            habitExists = prefs[HabitWidgetPrefKeys.HABIT_EXISTS] ?: false,
            title = prefs[HabitWidgetPrefKeys.TITLE].orEmpty(),
            iconGlyph = prefs[HabitWidgetPrefKeys.ICON_GLYPH].orEmpty().ifBlank { NerdIcons.Fire },
            groupColorArgb = prefs[HabitWidgetPrefKeys.GROUP_COLOR_ARGB] ?: DEFAULT_WIDGET_GROUP_COLOR_ARGB,
            remainingText = prefs[HabitWidgetPrefKeys.REMAINING_TEXT].orEmpty(),
            progressText = prefs[HabitWidgetPrefKeys.PROGRESS_TEXT].orEmpty(),
            incrementHint = prefs[HabitWidgetPrefKeys.INCREMENT_HINT].orEmpty(),
            progressFraction = (prefs[HabitWidgetPrefKeys.PROGRESS_FRACTION] ?: 0f).coerceIn(0f, 1f),
            streakDays = prefs[HabitWidgetPrefKeys.STREAK_DAYS] ?: 0,
            isCompleted = prefs[HabitWidgetPrefKeys.IS_COMPLETED] ?: false,
            epochDay = prefs[HabitWidgetPrefKeys.EPOCH_DAY] ?: 0L
        )
    }

    private fun missingSnapshot(habitId: Long, epochDay: Long): HabitWidgetSnapshot =
        HabitWidgetSnapshot(
            habitId = habitId,
            habitExists = false,
            title = "",
            iconGlyph = NerdIcons.Fire,
            groupColorArgb = DEFAULT_WIDGET_GROUP_COLOR_ARGB,
            remainingText = "",
            progressText = "",
            incrementHint = "",
            progressFraction = 0f,
            streakDays = 0,
            isCompleted = false,
            epochDay = epochDay
        )
}
