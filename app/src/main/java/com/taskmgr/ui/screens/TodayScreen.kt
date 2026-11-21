package com.taskmgr.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskmgr.ui.theme.IndigoContainer
import com.example.taskmgr.ui.theme.IndigoOnContainer
import com.example.taskmgr.ui.theme.IndigoOnPrimary
import com.example.taskmgr.ui.theme.IndigoPrimary
import com.example.taskmgr.ui.theme.OledBorder
import com.example.taskmgr.ui.theme.OledSurface
import com.example.taskmgr.ui.theme.OledSurfaceElevated
import com.example.taskmgr.ui.theme.PriorityMedium
import com.example.taskmgr.ui.theme.TaskCompleted
import com.example.taskmgr.ui.theme.TextDisabled
import com.example.taskmgr.ui.theme.TextPrimary
import com.example.taskmgr.ui.theme.TextSecondary
import com.example.taskmgr.ui.theme.UbuntuSansMonoNerdFontFamily
import com.taskmgr.domain.logic.HabitCompletionCalculator
import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.Moment
import com.taskmgr.domain.model.Task
import com.taskmgr.scheduler.AppPermissionStatus
import com.taskmgr.ui.components.DatePickerModalDialog
import com.taskmgr.ui.components.HabitBadgeIcon
import com.taskmgr.ui.components.NerdIcon
import com.taskmgr.ui.components.NerdIcons
import com.taskmgr.ui.components.formatMinutes
import com.taskmgr.ui.components.momentNerdGlyph
import com.taskmgr.ui.viewmodel.HabitWithLog
import com.taskmgr.ui.viewmodel.TodayUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(
    uiState: TodayUiState,
    permissionStatus: AppPermissionStatus,
    onSelectDay: (Long) -> Unit,
    onToggleTask: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onTestTaskAlarm: (Task) -> Unit,
    onIncrementHabit: (Long, Long) -> Unit,
    onEditHabit: (Habit) -> Unit,
    onDeleteHabit: (Long) -> Unit,
    onTestHabitAlarm: (HabitWithLog, Long) -> Unit,
    onOpenPermissionsSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayEpochDay = LocalDate.now().toEpochDay()
    val localePtBr = Locale.forLanguageTag("pt-BR")

    val momentsById = uiState.moments.associateBy { it.id }

    val momentGroups: List<Pair<Moment?, Pair<List<Task>, List<HabitWithLog>>>> = buildList {
        val groupedTasks = uiState.tasks.groupBy { it.momentId }
        val groupedHabits = uiState.habits.groupBy { it.habit.momentId }

        uiState.moments.forEach { moment ->
            val mTasks = groupedTasks[moment.id].orEmpty()
            val mHabits = groupedHabits[moment.id].orEmpty()
            if (mTasks.isNotEmpty() || mHabits.isNotEmpty()) {
                add(moment to (mTasks to mHabits))
            }
        }
        val looseTasks = groupedTasks[null].orEmpty() +
            uiState.tasks.filter { it.momentId != null && momentsById[it.momentId] == null }
        val looseHabits = groupedHabits[null].orEmpty() +
            uiState.habits.filter { it.habit.momentId != null && momentsById[it.habit.momentId] == null }
        if (looseTasks.isNotEmpty() || looseHabits.isNotEmpty()) {
            add(null to (looseTasks to looseHabits))
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "perm_banner") {
            AnimatedVisibility(
                visible = !permissionStatus.allCriticalGranted,
                enter = expandVertically(tween(260, easing = FastOutSlowInEasing)) + fadeIn(tween(220)),
                exit = shrinkVertically(tween(220)) + fadeOut(tween(180))
            ) {
                PermissionAlertBanner(
                    permissionStatus = permissionStatus,
                    onConfigureClick = onOpenPermissionsSettings
                )
            }
        }

        item(key = "day_header") {
            DayHeaderBar(
                selectedEpochDay = uiState.selectedEpochDay,
                todayEpochDay = todayEpochDay,
                localePtBr = localePtBr,
                onPrevDay = { onSelectDay(uiState.selectedEpochDay - 1) },
                onToday = { onSelectDay(todayEpochDay) },
                onNextDay = { onSelectDay(uiState.selectedEpochDay + 1) },
                onPickDate = { showDatePicker = true },
                showTodayButton = uiState.selectedEpochDay != todayEpochDay
            )
        }

        item(key = "summary_card") {
            val totalItems = uiState.tasks.size + uiState.habits.size
            val completedItems = uiState.tasks.count { it.isCompleted } +
                uiState.habits.count { it.isCompleted }
            DaySummaryCard(completed = completedItems, total = totalItems)
        }

        if (momentGroups.isEmpty()) {
            item(key = "empty_state") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = OledSurface),
                    border = BorderStroke(1.dp, OledBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        NerdIcon(
                            glyph = NerdIcons.CalendarCheck,
                            color = IndigoPrimary,
                            size = 38.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Nenhum item para este dia",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Use o botão + abaixo para criar uma Tarefa ou Hábito com alarme em tela cheia.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            momentGroups.forEach { (moment, pair) ->
                val (tasksInGroup, habitsInGroup) = pair
                item(key = "group_header_${moment?.id ?: -1}") {
                    GroupSectionHeader(
                        moment = moment,
                        modifier = Modifier.animateItem()
                    )
                }
                items(
                    items = habitsInGroup,
                    key = { "habit_${it.habit.id}" }
                ) { habitWithLog ->
                    HabitCard(
                        habitWithLog = habitWithLog,
                        onIncrement = { onIncrementHabit(habitWithLog.habit.id, uiState.selectedEpochDay) },
                        onEdit = { onEditHabit(habitWithLog.habit) },
                        onDelete = { onDeleteHabit(habitWithLog.habit.id) },
                        onTestAlarm = { onTestHabitAlarm(habitWithLog, uiState.selectedEpochDay) },
                        modifier = Modifier.animateItem()
                    )
                }
                items(
                    items = tasksInGroup,
                    key = { "task_${it.id}" }
                ) { task ->
                    TaskCard(
                        task = task,
                        onToggleCompleted = { onToggleTask(task) },
                        onEdit = { onEditTask(task) },
                        onDelete = { onDeleteTask(task.id) },
                        onTestAlarm = { onTestTaskAlarm(task) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    if (showDatePicker) {
        DatePickerModalDialog(
            initialEpochDay = uiState.selectedEpochDay,
            onDismiss = { showDatePicker = false },
            onConfirm = { picked ->
                onSelectDay(picked)
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun PermissionAlertBanner(
    permissionStatus: AppPermissionStatus,
    onConfigureClick: () -> Unit
) {
    val missing = buildList {
        if (!permissionStatus.canUseFullScreenIntent) add("Tela Cheia (Lockscreen)")
        if (!permissionStatus.canDrawOverlays) add("Sobrepor a Outros Apps")
        if (!permissionStatus.canScheduleExactAlarms) add("Alarmes Exatos")
        if (!permissionStatus.hasPostNotifications) add("Notificações")
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NerdIcon(
                    glyph = NerdIcons.AlertCircle,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    size = 20.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Permissões críticas pendentes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Falta conceder: ${missing.joinToString(", ")}. Sem isso o alarme TDAH pode não abrir sobre a tela bloqueada.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onConfigureClick) {
                NerdIcon(glyph = NerdIcons.Cog, size = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Configurar Permissões")
            }
        }
    }
}

@Composable
private fun DayHeaderBar(
    selectedEpochDay: Long,
    todayEpochDay: Long,
    localePtBr: Locale,
    onPrevDay: () -> Unit,
    onToday: () -> Unit,
    onNextDay: () -> Unit,
    onPickDate: () -> Unit,
    showTodayButton: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevDay) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Dia anterior")
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onPickDate)
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .animateContentSize(tween(220, easing = FastOutSlowInEasing))
        ) {
            AnimatedContent(
                targetState = selectedEpochDay,
                transitionSpec = {
                    val forward = targetState >= initialState
                    val slideIn = slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) {
                        if (forward) it / 3 else -it / 3
                    } + fadeIn(tween(200))
                    val slideOut = slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) {
                        if (forward) -it / 3 else it / 3
                    } + fadeOut(tween(150))
                    slideIn togetherWith slideOut
                },
                label = "day_header_transition"
            ) { targetDay ->
                val date = LocalDate.ofEpochDay(targetDay)
                val title = when (targetDay) {
                    todayEpochDay -> "Hoje"
                    todayEpochDay - 1 -> "Ontem"
                    todayEpochDay + 1 -> "Amanhã"
                    else -> date.format(DateTimeFormatter.ofPattern("EEE, dd/MM", localePtBr))
                }
                val subtitle = date.format(DateTimeFormatter.ofPattern("dd 'de' MMMM", localePtBr))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        NerdIcon(
                            glyph = NerdIcons.CalendarToday,
                            color = IndigoPrimary,
                            size = 13.sp
                        )
                        Text(
                            text = "$subtitle • trocar dia",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = showTodayButton,
                enter = expandVertically(tween(200)) + fadeIn(tween(180)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(140))
            ) {
                TextButton(onClick = onToday) {
                    NerdIcon(glyph = NerdIcons.History, size = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ir para Hoje")
                }
            }
        }
        IconButton(onClick = onNextDay) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próximo dia")
        }
    }
}

@Composable
private fun DaySummaryCard(completed: Int, total: Int) {
    val targetFraction = if (total > 0) completed.toFloat() / total.toFloat() else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "day_summary_progress"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = IndigoContainer),
        border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NerdIcon(
                        glyph = if (total > 0 && completed == total) NerdIcons.Trophy else NerdIcons.Target,
                        color = if (total > 0 && completed == total) TaskCompleted else IndigoPrimary,
                        size = 20.sp
                    )
                    Text(
                        text = "Progresso do dia",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IndigoOnContainer
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    NerdIcon(
                        glyph = NerdIcons.CheckCircle,
                        color = if (total > 0 && completed == total) TaskCompleted else IndigoPrimary,
                        size = 15.sp
                    )
                    Text(
                        text = "$completed / $total concluídos",
                        style = MaterialTheme.typography.labelLarge,
                        color = IndigoOnContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animatedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (total > 0 && completed == total) TaskCompleted else IndigoPrimary,
                trackColor = OledSurfaceElevated
            )
        }
    }
}

@Composable
private fun GroupSectionHeader(
    moment: Moment?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            NerdIcon(
                glyph = momentNerdGlyph(moment?.startMinutesOfDay),
                color = MaterialTheme.colorScheme.primary,
                size = 19.sp
            )
            Text(
                text = moment?.name ?: "Horários / Avulsos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (moment != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NerdIcon(
                    glyph = NerdIcons.ClockOutline,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 14.sp
                )
                Text(
                    text = "${formatMinutes(moment.startMinutesOfDay)} – ${formatMinutes(moment.endMinutesOfDay)}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = UbuntuSansMonoNerdFontFamily
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetaBadge(
    glyph: String,
    text: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    monospace: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        NerdIcon(glyph = glyph, color = tint, size = 13.sp)
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.let { base ->
                if (monospace) base.copy(fontFamily = UbuntuSansMonoNerdFontFamily) else base
            },
            color = tint
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskCard(
    task: Task,
    onToggleCompleted: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTestAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleColor by animateColorAsState(
        targetValue = if (task.isCompleted) TextDisabled else TextPrimary,
        animationSpec = tween(220),
        label = "task_title_color"
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(220, easing = FastOutSlowInEasing)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OledSurface),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggleCompleted() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = IndigoPrimary,
                        checkmarkColor = IndigoOnPrimary,
                        uncheckedColor = TextSecondary
                    )
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MetaBadge(glyph = NerdIcons.CheckboxMarkedOutline, text = "Tarefa")
                        task.scheduledMinutesOfDay?.let {
                            MetaBadge(glyph = NerdIcons.ClockOutline, text = formatMinutes(it), monospace = true)
                        }
                        if (task.endEpochDay > task.startEpochDay) {
                            MetaBadge(
                                glyph = NerdIcons.Calendar,
                                text = "${(task.endEpochDay - task.startEpochDay) + 1} dias"
                            )
                        }
                        if (task.aggressiveFullScreen) {
                            MetaBadge(
                                glyph = NerdIcons.BellAlert,
                                text = "Tela Cheia",
                                tint = IndigoPrimary
                            )
                        }
                        if (task.vibrationEnabled) {
                            MetaBadge(glyph = NerdIcons.Vibrate, text = "Vibra")
                        }
                    }
                }
                IconButton(onClick = onTestAlarm) {
                    NerdIcon(
                        glyph = NerdIcons.BellRing,
                        color = IndigoPrimary,
                        size = 19.sp
                    )
                }
                IconButton(onClick = onEdit) {
                    NerdIcon(
                        glyph = NerdIcons.Pencil,
                        color = TextSecondary,
                        size = 19.sp
                    )
                }
                IconButton(onClick = onDelete) {
                    NerdIcon(
                        glyph = NerdIcons.DeleteOutline,
                        color = MaterialTheme.colorScheme.error,
                        size = 19.sp
                    )
                }
            }
            if (!task.notes.isNullOrBlank()) {
                Text(
                    text = task.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (task.isCompleted) TextDisabled else TextSecondary,
                    modifier = Modifier.padding(start = 48.dp, top = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitCard(
    habitWithLog: HabitWithLog,
    onIncrement: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTestAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habit = habitWithLog.habit
    val progressText = HabitCompletionCalculator.formatProgressText(habit, habitWithLog.accumulatedAmount)
    val buttonLabel = HabitCompletionCalculator.formatIncrementButtonLabel(habit)
    val targetFraction = (habitWithLog.accumulatedAmount / habit.targetAmount.coerceAtLeast(1e-6))
        .toFloat()
        .coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "habit_card_progress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(220, easing = FastOutSlowInEasing)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OledSurface),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HabitBadgeIcon(
                    iconGlyph = habit.iconGlyph ?: habitWithLog.group?.iconGlyph,
                    groupColorArgb = habitWithLog.group?.colorArgb,
                    size = 42.dp,
                    iconSize = 20.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = habit.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (habitWithLog.isCompleted) {
                            NerdIcon(
                                glyph = NerdIcons.CheckCircle,
                                color = TaskCompleted,
                                size = 18.sp
                            )
                        } else if (habitWithLog.isDismissedForDay) {
                            Surface(
                                color = OledSurfaceElevated,
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    NerdIcon(glyph = NerdIcons.BellOffOutline, color = TextSecondary, size = 12.sp)
                                    Text(
                                        text = "Até amanhã",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        habitWithLog.group?.let { group ->
                            MetaBadge(
                                glyph = NerdIcons.Tag,
                                text = group.name,
                                tint = Color(group.colorArgb.toInt())
                            )
                        }
                        habit.scheduledMinutesOfDay?.let {
                            MetaBadge(glyph = NerdIcons.ClockOutline, text = formatMinutes(it), monospace = true)
                        }
                        MetaBadge(
                            glyph = NerdIcons.Fire,
                            text = "Seq: ${habitWithLog.stats.currentStreakDays}d",
                            tint = if (habitWithLog.stats.currentStreakDays > 0) PriorityMedium else TextSecondary
                        )
                        MetaBadge(
                            glyph = NerdIcons.Trophy,
                            text = "Rec: ${habitWithLog.stats.longestStreakDays}d • ${habitWithLog.stats.totalCompletedDays}d",
                            tint = TextSecondary
                        )
                        if (habit.aggressiveFullScreen) {
                            MetaBadge(
                                glyph = NerdIcons.BellAlert,
                                text = "Tela Cheia",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                IconButton(onClick = onTestAlarm) {
                    NerdIcon(
                        glyph = NerdIcons.Alarm,
                        color = MaterialTheme.colorScheme.primary,
                        size = 19.sp
                    )
                }
                IconButton(onClick = onEdit) {
                    NerdIcon(
                        glyph = NerdIcons.Pencil,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 19.sp
                    )
                }
                IconButton(onClick = onDelete) {
                    NerdIcon(
                        glyph = NerdIcons.DeleteOutline,
                        color = MaterialTheme.colorScheme.error,
                        size = 19.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { animatedFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color = if (habitWithLog.isCompleted) TaskCompleted else IndigoPrimary,
                        trackColor = OledSurfaceElevated
                    )
                }

                if (habitWithLog.isCompleted) {
                    OutlinedButton(onClick = onIncrement) {
                        NerdIcon(glyph = NerdIcons.CheckCircle, size = 15.sp)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(buttonLabel)
                    }
                } else {
                    FilledTonalButton(onClick = onIncrement) {
                        NerdIcon(glyph = NerdIcons.PlusCircle, size = 15.sp)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(buttonLabel)
                    }
                }
            }
        }
    }
}
