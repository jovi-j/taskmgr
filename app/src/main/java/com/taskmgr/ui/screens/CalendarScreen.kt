package com.taskmgr.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskmgr.ui.theme.IndigoContainer
import com.example.taskmgr.ui.theme.IndigoOnPrimary
import com.example.taskmgr.ui.theme.IndigoPrimary
import com.example.taskmgr.ui.theme.OledBorder
import com.example.taskmgr.ui.theme.OledSurface
import com.example.taskmgr.ui.theme.OledSurfaceElevated
import com.example.taskmgr.ui.theme.PriorityLow
import com.example.taskmgr.ui.theme.PriorityMedium
import com.example.taskmgr.ui.theme.TaskCompleted
import com.example.taskmgr.ui.theme.TextDisabled
import com.example.taskmgr.ui.theme.TextPrimary
import com.example.taskmgr.ui.theme.TextSecondary
import com.taskmgr.domain.logic.HabitCompletionCalculator
import com.taskmgr.domain.model.CompletionType
import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.Task
import com.taskmgr.ui.components.HabitBadgeIcon
import com.taskmgr.ui.components.ManualHabitAmountDialog
import com.taskmgr.ui.components.NerdIcon
import com.taskmgr.ui.components.NerdIcons
import com.taskmgr.ui.viewmodel.CalendarDaySummary
import com.taskmgr.ui.viewmodel.CalendarUiState
import com.taskmgr.ui.viewmodel.DayDetailUiState
import com.taskmgr.ui.viewmodel.HabitWithLog
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CALENDAR_LOCALE_PT_BR: Locale = Locale.forLanguageTag("pt-BR")
private val MONTH_YEAR_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM yyyy", CALENDAR_LOCALE_PT_BR)
private val DD_MM_YYYY_CALENDAR_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val DAY_DETAIL_HEADER_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", CALENDAR_LOCALE_PT_BR)
private val WEEKDAY_SHORT_LABELS: List<String> =
    listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    calendarState: CalendarUiState,
    dayDetailState: DayDetailUiState?,
    habitRetentionSummaries: List<HabitWithLog>,
    onSelectMonth: (YearMonth) -> Unit,
    onDayClick: (Long) -> Unit,
    onDismissDayDetail: () -> Unit,
    onToggleTask: (Task) -> Unit,
    onUpdateHabitLogManual: (habitId: Long, epochDay: Long, amount: Double, dismissed: Boolean) -> Unit,
    onIncludeHabitFromPastDay: (Habit, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val todayEpochDay = remember { LocalDate.now().toEpochDay() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { onSelectMonth(calendarState.month.minusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mês anterior")
            }
            AnimatedContent(
                targetState = calendarState.month,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) {
                        if (forward) it / 3 else -it / 3
                    } + fadeIn(tween(200))) togetherWith
                        (slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) {
                            if (forward) -it / 3 else it / 3
                        } + fadeOut(tween(150)))
                },
                label = "calendar_month_title_transition"
            ) { targetMonth ->
                val monthTitle = remember(targetMonth) {
                    targetMonth.format(MONTH_YEAR_FORMATTER).replaceFirstChar { it.uppercase() }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NerdIcon(
                        glyph = NerdIcons.Calendar,
                        color = MaterialTheme.colorScheme.primary,
                        size = 20.sp
                    )
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            IconButton(onClick = { onSelectMonth(calendarState.month.plusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próximo mês")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            WEEKDAY_SHORT_LABELS.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedContent(
            targetState = calendarState.month,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) {
                    if (forward) it / 4 else -it / 4
                } + fadeIn(tween(240))) togetherWith
                    (slideOutHorizontally(tween(220, easing = FastOutSlowInEasing)) {
                        if (forward) -it / 4 else it / 4
                    } + fadeOut(tween(180)))
            },
            label = "calendar_month_grid_transition"
        ) { targetMonth ->
            val firstOfMonth = targetMonth.atDay(1)
            val leadingBlanks = firstOfMonth.dayOfWeek.value - 1
            val daysInMonth = targetMonth.lengthOfMonth()
            val totalCells = ((leadingBlanks + daysInMonth + 6) / 7) * 7

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (week in 0 until (totalCells / 7)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = week * 7 + col
                            val dayOfMonth = cellIndex - leadingBlanks + 1
                            if (dayOfMonth in 1..daysInMonth) {
                                val date = targetMonth.atDay(dayOfMonth)
                                val epochDay = date.toEpochDay()
                                val summary = calendarState.daySummaries[epochDay]
                                CalendarDayCell(
                                    dayOfMonth = dayOfMonth,
                                    isToday = epochDay == todayEpochDay,
                                    summary = summary,
                                    onClick = { onDayClick(epochDay) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendDot(color = TaskCompleted, label = "Concluído")
            LegendDot(color = PriorityMedium, label = "Parcial")
            LegendDot(color = PriorityLow, label = "Tarefas")
            LegendDot(color = TextSecondary, label = "Pendente/Disp.")
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NerdIcon(
                glyph = NerdIcons.History,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 15.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Toque em qualquer dia para ver e definir exatamente quanto foi feito.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NerdIcon(glyph = NerdIcons.Trophy, color = IndigoPrimary, size = 20.sp)
            Text(
                text = "Tempo mantido por hábito",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (habitRetentionSummaries.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = OledSurface),
                border = BorderStroke(1.dp, OledBorder)
            ) {
                Text(
                    text = "Crie hábitos para acompanhar sequência atual, recorde e total de dias mantidos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                habitRetentionSummaries.forEach { item ->
                    HabitRetentionCard(item = item)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (dayDetailState != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismissDayDetail,
            sheetState = sheetState,
            containerColor = OledSurfaceElevated
        ) {
            DayDetailSheetContent(
                state = dayDetailState,
                onToggleTask = onToggleTask,
                onUpdateHabitLogManual = onUpdateHabitLogManual,
                onIncludeHabitFromPastDay = onIncludeHabitFromPastDay,
                onClose = onDismissDayDetail
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitRetentionCard(item: HabitWithLog) {
    val habit = item.habit
    val stats = item.stats
    val startFormatted = remember(habit.startEpochDay) {
        LocalDate.ofEpochDay(habit.startEpochDay).format(DD_MM_YYYY_CALENDAR_FORMATTER)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OledSurface),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                HabitBadgeIcon(
                    iconGlyph = habit.iconGlyph ?: item.group?.iconGlyph,
                    groupColorArgb = item.group?.colorArgb,
                    size = 42.dp,
                    iconSize = 20.sp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = habit.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        item.group?.let { group ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(group.colorArgb.toInt()).copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(group.colorArgb.toInt()),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Desde $startFormatted • Adesão ${stats.completionRatePercent}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RetentionPill(
                    glyph = NerdIcons.Fire,
                    label = "Atual: ${stats.currentStreakDays}d",
                    tint = PriorityMedium
                )
                RetentionPill(
                    glyph = NerdIcons.Trophy,
                    label = "Recorde: ${stats.longestStreakDays}d",
                    tint = IndigoPrimary
                )
                RetentionPill(
                    glyph = NerdIcons.CheckCircle,
                    label = "Total: ${stats.totalCompletedDays}/${stats.activeDaysSinceStart}d",
                    tint = TaskCompleted
                )
            }
        }
    }
}

@Composable
private fun RetentionPill(
    glyph: String,
    label: String,
    tint: Color
) {
    Surface(
        color = OledSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            NerdIcon(glyph = glyph, color = tint, size = 14.sp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayOfMonth: Int,
    isToday: Boolean,
    summary: CalendarDaySummary?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isToday) IndigoPrimary else OledBorder
    val bgColor = if (isToday) IndigoContainer else OledSurface

    Box(
        modifier = modifier
            .aspectRatio(0.95f)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium
            )

            if (summary != null && (summary.taskCount > 0 || summary.activeHabitCount > 0)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    if (summary.taskCount > 0) {
                        val allTasksDone = summary.completedTaskCount == summary.taskCount
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (allTasksDone) TaskCompleted else PriorityLow
                                )
                        )
                    }
                    if (summary.activeHabitCount > 0) {
                        val habitColor = when {
                            summary.completedHabitCount == summary.activeHabitCount -> TaskCompleted
                            summary.completedHabitCount > 0 || summary.partialHabitCount > 0 -> PriorityMedium
                            else -> TextSecondary
                        }
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(habitColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DayDetailSheetContent(
    state: DayDetailUiState,
    onToggleTask: (Task) -> Unit,
    onUpdateHabitLogManual: (habitId: Long, epochDay: Long, amount: Double, dismissed: Boolean) -> Unit,
    onIncludeHabitFromPastDay: (Habit, Long) -> Unit,
    onClose: () -> Unit
) {
    val formattedDate = remember(state.epochDay) {
        LocalDate.ofEpochDay(state.epochDay)
            .format(DAY_DETAIL_HEADER_FORMATTER)
            .replaceFirstChar { it.uppercase() }
    }

    var editingHabit by remember { mutableStateOf<HabitWithLog?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NerdIcon(
                        glyph = NerdIcons.CalendarCheck,
                        color = MaterialTheme.colorScheme.primary,
                        size = 20.sp
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Edição retroativa de tarefas e hábitos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onClose) { Text("Fechar") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.tasks.isEmpty() && state.habits.isEmpty() && state.laterStartHabits.isEmpty()) {
            Text(
                text = "Nenhuma tarefa ou hábito ativo nesta data.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.habits.isNotEmpty()) {
                    item(key = "detail_habits_header", contentType = "detail_header") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            NerdIcon(
                                glyph = NerdIcons.Fire,
                                color = MaterialTheme.colorScheme.primary,
                                size = 16.sp
                            )
                            Text(
                                text = "Hábitos — selecione quanto foi feito",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    items(
                        items = state.habits,
                        key = { "detail_habit_${it.habit.id}" },
                        contentType = { "detail_habit" }
                    ) { item ->
                        RetroactiveHabitRow(
                            habitWithLog = item,
                            onSetAmount = { amount ->
                                onUpdateHabitLogManual(item.habit.id, state.epochDay, amount, false)
                            },
                            onOpenManualDialog = { editingHabit = item }
                        )
                    }
                }

                if (state.tasks.isNotEmpty()) {
                    item(key = "detail_tasks_header", contentType = "detail_header") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            NerdIcon(
                                glyph = NerdIcons.FormatListChecks,
                                color = MaterialTheme.colorScheme.primary,
                                size = 16.sp
                            )
                            Text(
                                text = "Tarefas",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    items(
                        items = state.tasks,
                        key = { "detail_task_${it.id}" },
                        contentType = { "detail_task" }
                    ) { task ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = OledSurface),
                            border = BorderStroke(1.dp, OledBorder)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Checkbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = { onToggleTask(task) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = IndigoPrimary,
                                        checkmarkColor = IndigoOnPrimary,
                                        uncheckedColor = TextSecondary
                                    )
                                )
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (task.isCompleted) TextDisabled else TextPrimary,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                )
                            }
                        }
                    }
                }

                if (state.laterStartHabits.isNotEmpty()) {
                    item(key = "detail_later_header", contentType = "detail_header") {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            NerdIcon(
                                glyph = NerdIcons.History,
                                color = TextSecondary,
                                size = 16.sp
                            )
                            Text(
                                text = "Hábitos iniciados depois desta data",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }
                    }
                    items(
                        items = state.laterStartHabits,
                        key = { "later_habit_${it.id}" },
                        contentType = { "later_habit" }
                    ) { habit ->
                        val group = habit.groupId?.let { state.habitGroupsById[it] }
                        val formattedStart = remember(habit.startEpochDay) {
                            LocalDate.ofEpochDay(habit.startEpochDay).format(DD_MM_YYYY_CALENDAR_FORMATTER)
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = OledSurface),
                            border = BorderStroke(1.dp, OledBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HabitBadgeIcon(
                                    iconGlyph = habit.iconGlyph ?: group?.iconGlyph,
                                    groupColorArgb = group?.colorArgb,
                                    size = 34.dp,
                                    iconSize = 16.sp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = habit.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Início atual: $formattedStart",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onIncludeHabitFromPastDay(habit, state.epochDay) }
                                ) {
                                    Text("Incluir aqui")
                                }
                            }
                        }
                    }
                }

                item(key = "detail_bottom_spacer", contentType = "spacer") {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    editingHabit?.let { target ->
        ManualHabitAmountDialog(
            habit = target.habit,
            currentAmount = target.accumulatedAmount,
            onDismiss = { editingHabit = null },
            onConfirm = { newAmount ->
                onUpdateHabitLogManual(target.habit.id, state.epochDay, newAmount, false)
                editingHabit = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RetroactiveHabitRow(
    habitWithLog: HabitWithLog,
    onSetAmount: (Double) -> Unit,
    onOpenManualDialog: () -> Unit
) {
    val habit: Habit = habitWithLog.habit
    val progress = HabitCompletionCalculator.formatProgressText(habit, habitWithLog.accumulatedAmount)
    val targetFraction = (habitWithLog.accumulatedAmount / habit.targetAmount.coerceAtLeast(1e-6))
        .toFloat()
        .coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "retroactive_habit_progress"
    )
    val statusText = when {
        habitWithLog.isCompleted -> "Concluído"
        habitWithLog.isDismissedForDay -> "Dispensado"
        habitWithLog.accumulatedAmount > 0.0 -> "Parcial"
        else -> "Pendente"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = OledSurface),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HabitBadgeIcon(
                    iconGlyph = habit.iconGlyph ?: habitWithLog.group?.iconGlyph,
                    groupColorArgb = habitWithLog.group?.colorArgb,
                    size = 38.dp,
                    iconSize = 18.sp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = progress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            NerdIcon(glyph = NerdIcons.Fire, color = PriorityMedium, size = 12.sp)
                            Text(
                                text = "Seq ${habitWithLog.stats.currentStreakDays}d",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            NerdIcon(glyph = NerdIcons.Trophy, color = IndigoPrimary, size = 12.sp)
                            Text(
                                text = "Rec ${habitWithLog.stats.longestStreakDays}d",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (habitWithLog.isCompleted) {
                    NerdIcon(
                        glyph = NerdIcons.CheckCircle,
                        color = TaskCompleted,
                        size = 20.sp
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = OledSurfaceElevated
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (habit.completionType != CompletionType.BOOLEAN) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { animatedFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (habitWithLog.isCompleted) TaskCompleted else IndigoPrimary,
                    trackColor = OledSurfaceElevated
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (habit.completionType) {
                CompletionType.BOOLEAN -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onSetAmount(0.0) },
                            modifier = Modifier.weight(1f)
                        ) {
                            NerdIcon(glyph = NerdIcons.Close, size = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Não feito")
                        }
                        FilledTonalButton(
                            onClick = { onSetAmount(habit.targetAmount) },
                            modifier = Modifier.weight(1f)
                        ) {
                            NerdIcon(glyph = NerdIcons.CheckCircle, size = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Feito")
                        }
                    }
                }

                CompletionType.COUNT_PER_DAY if habit.targetAmount.toInt() in 2..6 -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (count in 0..habit.targetAmount.toInt()) {
                            val selected = habitWithLog.accumulatedAmount.toInt() == count
                            FilterChip(
                                selected = selected,
                                onClick = { onSetAmount(count.toDouble()) },
                                label = { Text("$count") }
                            )
                        }
                        FilterChip(
                            selected = false,
                            onClick = onOpenManualDialog,
                            leadingIcon = { NerdIcon(glyph = NerdIcons.Pencil, size = 14.sp) },
                            label = { Text("Exato") }
                        )
                    }
                }

                else -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = {
                                onSetAmount((habitWithLog.accumulatedAmount - habit.incrementAmount).coerceAtLeast(0.0))
                            }
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrementar")
                        }
                        IconButton(
                            onClick = {
                                onSetAmount(habitWithLog.accumulatedAmount + habit.incrementAmount)
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Incrementar")
                        }
                        FilledTonalButton(
                            onClick = onOpenManualDialog,
                            modifier = Modifier.weight(1f)
                        ) {
                            NerdIcon(glyph = NerdIcons.Tune, size = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Valor exato")
                        }
                        OutlinedButton(
                            onClick = {
                                val nextVal = if (habitWithLog.isCompleted) 0.0 else habit.targetAmount
                                onSetAmount(nextVal)
                            }
                        ) {
                            Text(if (habitWithLog.isCompleted) "Zerar" else "100%")
                        }
                    }
                }
            }
        }
    }
}
