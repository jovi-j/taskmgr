package com.taskmgr.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
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
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
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
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.jovij.taskmgr.MainActivity
import com.jovij.taskmgr.R
import com.jovij.taskmgr.TaskMgrApplication
import java.time.LocalDate

class CalendarTasksGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalendarTasksGlanceWidget()
}

class CalendarSelectDayAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val epochDay = parameters[KEY_EPOCH_DAY] ?: return
        CalendarWidgetUpdater.selectDay(context, glanceId, epochDay)
    }

    companion object {
        val KEY_EPOCH_DAY = ActionParameters.Key<Long>("epoch_day")
    }
}

class CalendarChangeMonthAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val delta = parameters[KEY_MONTH_DELTA] ?: return
        CalendarWidgetUpdater.changeMonth(context, glanceId, delta)
    }

    companion object {
        val KEY_MONTH_DELTA = ActionParameters.Key<Int>("month_delta")
    }
}

class CalendarJumpTodayAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        CalendarWidgetUpdater.jumpToToday(context, glanceId)
    }
}

class ToggleTaskWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[KEY_TASK_ID] ?: return
        val newCompleted = parameters[KEY_NEW_COMPLETED] ?: return
        val app = (context.applicationContext as? TaskMgrApplication) ?: return
        app.container.taskRepository.setCompleted(
            taskId = taskId,
            completed = newCompleted,
            completedAtEpochMillis = if (newCompleted) System.currentTimeMillis() else null
        )
        CalendarWidgetUpdater.syncAllWidgets(context)
    }

    companion object {
        val KEY_TASK_ID = ActionParameters.Key<Long>("task_id")
        val KEY_NEW_COMPLETED = ActionParameters.Key<Boolean>("new_completed")
    }
}

class CalendarTasksGlanceWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Responsive(RESPONSIVE_BUCKETS)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = LocalDate.now()
        val todayEpochDay = today.toEpochDay()
        val prefs = runCatching {
            getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        }.getOrNull()

        val lastToday = prefs?.get(CalendarWidgetPrefKeys.LAST_TODAY_EPOCH_DAY) ?: -1L
        val rolledOver = lastToday != todayEpochDay
        val year = if (rolledOver) today.year else (prefs?.get(CalendarWidgetPrefKeys.DISPLAY_YEAR) ?: today.year)
        val month = if (rolledOver) today.monthValue else (prefs?.get(CalendarWidgetPrefKeys.DISPLAY_MONTH) ?: today.monthValue).coerceIn(1, 12)
        val selectedDay = if (rolledOver) todayEpochDay else (prefs?.get(CalendarWidgetPrefKeys.SELECTED_EPOCH_DAY) ?: todayEpochDay)

        val initialSnapshot = CalendarWidgetUpdater.loadSnapshot(
            context = context,
            displayYear = year,
            displayMonth = month,
            selectedEpochDay = selectedDay,
            todayEpochDay = todayEpochDay
        )
        if (prefs == null || rolledOver) {
            CalendarWidgetUpdater.writeSnapshotToPreferences(context, id, initialSnapshot)
        }

        provideContent {
            val currentPrefs = currentState<Preferences>()
            val snapshot = CalendarWidgetUpdater.snapshotFromPreferences(currentPrefs) ?: initialSnapshot
            GlanceTheme {
                CalendarTasksWidgetContent(
                    context = context,
                    snapshot = snapshot
                )
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
            setTextViewText(android.R.id.text1, "Erro ao exibir widget de calendário")
        }
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, fallback)
    }

    companion object {
        private val RESPONSIVE_BUCKETS = setOf(
            DpSize(250.dp, 110.dp),
            DpSize(320.dp, 150.dp),
            DpSize(360.dp, 220.dp)
        )
    }
}

private fun fixedColor(color: Color) = ColorProvider(day = color, night = color)

private val CalendarWidgetBg = Color(0xFF14171F)
private val CalendarAccent = Color(0xFF7B93F5)
private val CalendarTodayText = Color(0xFF68B89C)
private val CalendarPrimaryText = Color(0xFFF3F4F6)
private val CalendarWeekendText = Color(0xFF6B7280)
private val CalendarSecondaryText = Color(0xFF9CA3AF)
private val CalendarCompletedText = Color(0xFF6B7280)
private val CalendarDivider = Color(0xFF232936)

@Composable
private fun CalendarTasksWidgetContent(
    context: Context,
    snapshot: CalendarWidgetSnapshot
) {
    val todayDate = LocalDate.ofEpochDay(snapshot.todayEpochDay)
    val monthTitle = CalendarWidgetCalculator.formatMonthTitlePtBr(
        year = snapshot.displayYear,
        month = snapshot.displayMonth,
        currentYear = todayDate.year
    )
    val weeks = CalendarWidgetCalculator.buildMonthGrid(snapshot.displayYear, snapshot.displayMonth)
    val selectedHeader = CalendarWidgetCalculator.formatSelectedDayHeaderPtBr(
        selectedEpochDay = snapshot.selectedEpochDay,
        todayEpochDay = snapshot.todayEpochDay
    )

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(22.dp)
            .background(fixedColor(CalendarWidgetBg))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // Left Pane: Interactive Month Calendar
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .padding(end = 6.dp)
        ) {
            // Month header: < novembro >
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(22.dp)
                        .clickable(
                            actionRunCallback<CalendarChangeMonthAction>(
                                actionParametersOf(CalendarChangeMonthAction.KEY_MONTH_DELTA to -1)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "‹",
                        style = TextStyle(
                            color = fixedColor(CalendarPrimaryText),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Text(
                    text = monthTitle,
                    maxLines = 1,
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(actionRunCallback<CalendarJumpTodayAction>()),
                    style = TextStyle(
                        color = fixedColor(CalendarAccent),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )

                Box(
                    modifier = GlanceModifier
                        .size(22.dp)
                        .clickable(
                            actionRunCallback<CalendarChangeMonthAction>(
                                actionParametersOf(CalendarChangeMonthAction.KEY_MONTH_DELTA to 1)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "›",
                        style = TextStyle(
                            color = fixedColor(CalendarPrimaryText),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // Weekday labels: d s t q q s s
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CalendarWidgetCalculator.WEEKDAY_LABELS_PT_BR.forEach { dayLetter ->
                    Text(
                        text = dayLetter,
                        modifier = GlanceModifier.defaultWeight(),
                        style = TextStyle(
                            color = fixedColor(CalendarWeekendText),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(2.dp))

            // Weeks grid (auto-distributes vertical height cleanly across 5 or 6 weeks)
            weeks.forEach { week ->
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    week.forEach { cell ->
                        if (cell == null) {
                            Spacer(modifier = GlanceModifier.defaultWeight())
                        } else {
                            val isToday = cell.epochDay == snapshot.todayEpochDay
                            val isSelected = cell.epochDay == snapshot.selectedEpochDay
                            val hasDot = cell.epochDay in snapshot.pendingDotEpochDays

                            val bgDrawableRes = when {
                                isToday && isSelected -> R.drawable.widget_day_today_selected_bg
                                isSelected -> R.drawable.widget_day_selected_bg
                                isToday -> R.drawable.widget_day_today_unselected_bg
                                else -> null
                            }
                            val textColor = when {
                                isSelected -> Color.White
                                isToday -> CalendarTodayText
                                cell.isWeekend -> CalendarWeekendText
                                else -> CalendarPrimaryText
                            }

                            Column(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .fillMaxHeight()
                                    .clickable(
                                        actionRunCallback<CalendarSelectDayAction>(
                                            actionParametersOf(CalendarSelectDayAction.KEY_EPOCH_DAY to cell.epochDay)
                                        )
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = if (bgDrawableRes != null) {
                                        GlanceModifier
                                            .size(20.dp)
                                            .background(ImageProvider(bgDrawableRes))
                                    } else {
                                        GlanceModifier.size(20.dp)
                                    },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cell.dayOfMonth.toString(),
                                        maxLines = 1,
                                        style = TextStyle(
                                            color = fixedColor(textColor),
                                            fontSize = 10.sp,
                                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                }

                                if (hasDot && !isSelected) {
                                    Image(
                                        provider = ImageProvider(R.drawable.widget_task_dot),
                                        contentDescription = null,
                                        modifier = GlanceModifier.size(3.5.dp)
                                    )
                                } else {
                                    Spacer(modifier = GlanceModifier.height(3.5.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Subtle vertical separator
        Box(
            modifier = GlanceModifier
                .width(1.dp)
                .fillMaxHeight()
                .padding(vertical = 4.dp)
                .background(fixedColor(CalendarDivider))
        ) {}

        // Right Pane: Tasks for selected day
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .padding(start = 8.dp)
        ) {
            // Compact header showing selected day + quick jump back to Today when viewing another day
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedHeader,
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(
                        color = fixedColor(
                            if (snapshot.selectedEpochDay == snapshot.todayEpochDay) {
                                CalendarTodayText
                            } else {
                                CalendarAccent
                            }
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                if (snapshot.selectedEpochDay != snapshot.todayEpochDay) {
                    Text(
                        text = "Hoje",
                        maxLines = 1,
                        modifier = GlanceModifier
                            .clickable(actionRunCallback<CalendarJumpTodayAction>())
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                        style = TextStyle(
                            color = fixedColor(CalendarTodayText),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(3.dp))

            if (snapshot.tasks.isEmpty()) {
                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity(openAppIntent)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sem tarefas para $selectedHeader",
                        maxLines = 2,
                        style = TextStyle(
                            color = fixedColor(CalendarSecondaryText),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    items(
                        items = snapshot.tasks,
                        itemId = { task -> task.id }
                    ) { task ->
                        CalendarWidgetTaskRow(
                            context = context,
                            task = task,
                            selectedEpochDay = snapshot.selectedEpochDay,
                            todayEpochDay = snapshot.todayEpochDay
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarWidgetTaskRow(
    context: Context,
    task: CalendarWidgetTaskItem,
    selectedEpochDay: Long,
    todayEpochDay: Long
) {
    val openTaskIntent = Intent(context, MainActivity::class.java).apply {
        action = "${MainActivity.ACTION_OPEN_TASK_PREFIX}${task.id}"
        putExtra(MainActivity.EXTRA_OPEN_TASK_ID, task.id)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
    }

    val badgeText = CalendarWidgetCalculator.formatTaskRowBadge(
        scheduledMinutesOfDay = task.scheduledMinutesOfDay,
        selectedEpochDay = selectedEpochDay,
        todayEpochDay = todayEpochDay
    )

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox: toggles task completion directly from the widget
        Image(
            provider = ImageProvider(
                if (task.isCompleted) {
                    R.drawable.widget_checkbox_checked
                } else {
                    R.drawable.widget_checkbox_unchecked
                }
            ),
            contentDescription = if (task.isCompleted) "Desmarcar tarefa" else "Concluir tarefa",
            modifier = GlanceModifier
                .size(18.dp)
                .clickable(
                    actionRunCallback<ToggleTaskWidgetAction>(
                        actionParametersOf(
                            ToggleTaskWidgetAction.KEY_TASK_ID to task.id,
                            ToggleTaskWidgetAction.KEY_NEW_COMPLETED to !task.isCompleted
                        )
                    )
                )
        )

        Spacer(modifier = GlanceModifier.width(7.dp))

        // Task title + badge: tapping opens the task inside the app
        Row(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(actionStartActivity(openTaskIntent)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = task.title,
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = fixedColor(if (task.isCompleted) CalendarCompletedText else CalendarPrimaryText),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
            )
            Spacer(modifier = GlanceModifier.width(4.dp))
            Text(
                text = badgeText,
                maxLines = 1,
                style = TextStyle(
                    color = fixedColor(CalendarSecondaryText),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )
            )
        }
    }
}
