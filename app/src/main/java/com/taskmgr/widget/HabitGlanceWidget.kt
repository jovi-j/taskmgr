package com.taskmgr.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.jovij.taskmgr.MainActivity
import com.jovij.taskmgr.TaskMgrApplication
import com.taskmgr.domain.logic.HabitCompletionCalculator
import com.taskmgr.domain.model.CompletionType
import java.time.LocalDate

class HabitGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitGlanceWidget()
}

class IncrementHabitWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val habitId = parameters[KEY_HABIT_ID] ?: return
        val app = context.applicationContext as? TaskMgrApplication ?: return
        val todayEpochDay = LocalDate.now().toEpochDay()
        val habit = app.container.habitRepository.getHabitById(habitId) ?: run {
            HabitWidgetUpdater.syncAllWidgets(context)
            return
        }

        // Match clicking "Sim" on a habit: for BOOLEAN habits that are already completed today,
        // keep them completed without inflating count; for COUNT_PER_DAY and CUSTOM_UNIT, increment.
        val existing = app.container.habitRepository.getLog(habitId, todayEpochDay)
        if (habit.completionType != CompletionType.BOOLEAN || existing?.isCompleted != true) {
            app.container.habitRepository.incrementHabit(
                habitId = habitId,
                epochDay = todayEpochDay,
                nowEpochMillis = System.currentTimeMillis()
            )
        }
        HabitWidgetUpdater.syncAllWidgets(context)
    }

    companion object {
        val KEY_HABIT_ID = ActionParameters.Key<Long>("habit_id")
    }
}

private data class IconRenderCacheKey(
    val glyph: String,
    val fgArgb: Int,
    val fillArgb: Int,
    val borderArgb: Int,
    val isCompleted: Boolean
)

class HabitGlanceWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Responsive(RESPONSIVE_BUCKETS)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)

        // Ensure widget reflects today's state even after midnight rollover or process restart.
        val existingPrefs = runCatching {
            getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        }.getOrNull()
        val configuredHabitId = existingPrefs?.get(HabitWidgetPrefKeys.HABIT_ID) ?: -1L
        val todayEpochDay = LocalDate.now().toEpochDay()
        var initialSnapshot: HabitWidgetSnapshot? = null
        if (configuredHabitId > 0L) {
            val fresh = HabitWidgetUpdater.loadSnapshot(context, configuredHabitId, todayEpochDay)
            initialSnapshot = fresh
            val storedEpochDay = existingPrefs?.get(HabitWidgetPrefKeys.EPOCH_DAY) ?: -1L
            val storedTitle = existingPrefs?.get(HabitWidgetPrefKeys.TITLE).orEmpty()
            val storedExists = existingPrefs?.get(HabitWidgetPrefKeys.HABIT_EXISTS) ?: false
            if (storedEpochDay != todayEpochDay || storedTitle != fresh.title || storedExists != fresh.habitExists) {
                HabitWidgetUpdater.writeSnapshotToPreferences(context, id, fresh)
            }
        }

        // Cache the tiny rendered Nerd Font icon bitmap outside provideContent so
        // SizeMode.Responsive buckets deduplicate the same Bitmap instance.
        var cachedIconKey: IconRenderCacheKey? = null
        var cachedIconBitmap: Bitmap? = null

        fun getOrRenderIcon(snapshot: HabitWidgetSnapshot): Bitmap {
            val bgArgb = snapshot.groupColorArgb.toInt()
            val fgArgb = HabitWidgetIconRenderer.computeContrastContentColorArgb(bgArgb)
            val fillArgb = HabitWidgetIconRenderer.withAlpha(fgArgb, if (snapshot.isCompleted) 0.24f else 0.16f)
            val borderArgb = HabitWidgetIconRenderer.withAlpha(fgArgb, if (snapshot.isCompleted) 0.72f else 0.34f)
            val key = IconRenderCacheKey(
                glyph = snapshot.iconGlyph,
                fgArgb = fgArgb,
                fillArgb = fillArgb,
                borderArgb = borderArgb,
                isCompleted = snapshot.isCompleted
            )
            val existingBitmap = cachedIconBitmap
            return if (cachedIconKey != key || existingBitmap == null) {
                HabitWidgetIconRenderer.renderHabitIconBitmap(
                    context = context,
                    glyph = key.glyph,
                    foregroundArgb = key.fgArgb,
                    containerFillArgb = key.fillArgb,
                    borderArgb = key.borderArgb,
                    isCompleted = key.isCompleted,
                    targetSizeDp = 56
                ).also {
                    cachedIconKey = key
                    cachedIconBitmap = it
                }
            } else {
                existingBitmap
            }
        }

        // Pre-render once before provideContent when already configured.
        initialSnapshot?.takeIf { it.habitExists }?.let { getOrRenderIcon(it) }

        provideContent {
            val prefs = currentState<Preferences>()
            val snapshot = HabitWidgetUpdater.snapshotFromPreferences(prefs) ?: initialSnapshot
            GlanceTheme {
                if (snapshot == null || !snapshot.habitExists) {
                    UnconfiguredHabitWidgetContent(
                        context = context,
                        appWidgetId = appWidgetId,
                        wasDeleted = snapshot != null
                    )
                } else {
                    val iconBitmap = getOrRenderIcon(snapshot)
                    ConfiguredHabitWidgetContent(
                        snapshot = snapshot,
                        iconBitmap = iconBitmap
                    )
                }
            }
        }
    }

    override fun onCompositionError(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int,
        throwable: Throwable
    ) {
        val fallback = RemoteViews(context.packageName, android.R.layout.simple_list_item_1).apply {
            setTextViewText(android.R.id.text1, "Erro ao exibir widget do hábito")
        }
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, fallback)
    }

    companion object {
        private val RESPONSIVE_BUCKETS = setOf(
            DpSize(110.dp, 110.dp),
            DpSize(160.dp, 160.dp),
            DpSize(240.dp, 160.dp)
        )
    }
}

private fun fixedColorProvider(color: Color) = ColorProvider(day = color, night = color)

@Composable
private fun ConfiguredHabitWidgetContent(
    snapshot: HabitWidgetSnapshot,
    iconBitmap: Bitmap
) {
    val bgArgb = snapshot.groupColorArgb.toInt()
    val fgArgb = HabitWidgetIconRenderer.computeContrastContentColorArgb(bgArgb)
    val bgColor = Color(bgArgb)
    val fgColor = Color(fgArgb)
    val subtleFgColor = fgColor.copy(alpha = 0.78f)
    val progressTrackColor = fgColor.copy(alpha = 0.24f)

    val dayLabel = HabitCompletionCalculator.formatWidgetDayLabel(
        streakDays = snapshot.streakDays,
        isCompletedToday = snapshot.isCompleted
    )
    val topRightLabel = if (snapshot.isCompleted) "✓" else snapshot.incrementHint

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(22.dp)
            .background(fixedColorProvider(bgColor))
            .clickable(actionStartActivity<MainActivity>())
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // Top metadata row: streak on the left, increment hint or checkmark on the right
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dayLabel,
                maxLines = 1,
                style = TextStyle(
                    color = fixedColorProvider(subtleFgColor),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            if (topRightLabel.isNotBlank()) {
                Text(
                    text = topRightLabel,
                    maxLines = 1,
                    style = TextStyle(
                        color = fixedColorProvider(if (snapshot.isCompleted) fgColor else subtleFgColor),
                        fontSize = 11.sp,
                        fontWeight = if (snapshot.isCompleted) FontWeight.Bold else FontWeight.Medium
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(2.dp))

        // Habit title
        Text(
            text = snapshot.title,
            maxLines = 1,
            style = TextStyle(
                color = fixedColorProvider(fgColor),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        )

        // Centered icon button: tapping it counts as clicking "Sim" on the habit
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(iconBitmap),
                contentDescription = "Marcar Sim para ${snapshot.title}",
                modifier = GlanceModifier
                    .size(56.dp)
                    .clickable(
                        actionRunCallback<IncrementHabitWidgetAction>(
                            actionParametersOf(IncrementHabitWidgetAction.KEY_HABIT_ID to snapshot.habitId)
                        )
                    )
            )
        }

        // Compact progress counter (e.g., "0/3", "500/2000 ml", "0/1")
        Text(
            text = snapshot.progressText.ifBlank { snapshot.remainingText },
            maxLines = 1,
            modifier = GlanceModifier.fillMaxWidth(),
            style = TextStyle(
                color = fixedColorProvider(fgColor.copy(alpha = 0.92f)),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = GlanceModifier.height(5.dp))

        // Progress bar at the bottom of the widget
        LinearProgressIndicator(
            progress = snapshot.progressFraction,
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(7.dp),
            color = fixedColorProvider(fgColor),
            backgroundColor = fixedColorProvider(progressTrackColor)
        )
    }
}

@Composable
private fun UnconfiguredHabitWidgetContent(
    context: Context,
    appWidgetId: Int,
    wasDeleted: Boolean
) {
    val configureIntent = Intent(context, HabitWidgetConfigureActivity::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(22.dp)
            .background(fixedColorProvider(Color(0xFF0E1117)))
            .clickable(actionStartActivity(configureIntent))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "taskmgr",
            style = TextStyle(
                color = fixedColorProvider(Color(0xFF818CF8)),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        Text(
            text = if (wasDeleted) {
                "Hábito removido.\nToque para escolher outro."
            } else {
                "Toque para selecionar um hábito"
            },
            maxLines = 3,
            style = TextStyle(
                color = fixedColorProvider(Color(0xFFF3F4F6)),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        )
    }
}
