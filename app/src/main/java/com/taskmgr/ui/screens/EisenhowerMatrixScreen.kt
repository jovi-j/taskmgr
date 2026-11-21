package com.taskmgr.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.taskmgr.ui.theme.IndigoContainer
import com.example.taskmgr.ui.theme.IndigoOnContainer
import com.example.taskmgr.ui.theme.IndigoPrimary
import com.example.taskmgr.ui.theme.OledBackground
import com.example.taskmgr.ui.theme.OledBorder
import com.example.taskmgr.ui.theme.OledSurface
import com.example.taskmgr.ui.theme.OledSurfaceElevated
import com.example.taskmgr.ui.theme.TaskCompleted
import com.example.taskmgr.ui.theme.TextPrimary
import com.example.taskmgr.ui.theme.TextSecondary
import com.taskmgr.domain.model.Task
import com.taskmgr.domain.model.TaskUrgency
import com.taskmgr.ui.components.DatePickerModalDialog
import com.taskmgr.ui.components.NerdIcon
import com.taskmgr.ui.components.NerdIcons
import com.taskmgr.ui.components.TimePickerModalDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ShortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun urgencyGlyph(urgency: TaskUrgency): String = when (urgency) {
    TaskUrgency.VERY_HIGH -> NerdIcons.Fire
    TaskUrgency.HIGH -> NerdIcons.AlertCircle
    TaskUrgency.MEDIUM -> NerdIcons.ClockOutline
    TaskUrgency.LOW -> NerdIcons.Layers
}

private fun formatMinutesPtBr(minutesOfDay: Int): String {
    val h = (minutesOfDay / 60).coerceIn(0, 23)
    val m = (minutesOfDay % 60).coerceIn(0, 59)
    return String.format(Locale.US, "%02d:%02d", h, m)
}

@Composable
fun EisenhowerMatrixScreen(
    tasks: List<Task>,
    onCreateTaskInQuadrant: (TaskUrgency) -> Unit,
    onToggleTask: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onUpdateTask: (Task) -> Unit = {},
    onDeleteTask: (Long) -> Unit = {},
    onTestTaskAlarm: (Task) -> Unit = {}
) {
    val todayEpochDay = remember { LocalDate.now().toEpochDay() }

    val visibleTasks = remember(tasks, todayEpochDay) {
        tasks.filter { task -> task.shouldShowInEisenhowerMatrix(todayEpochDay) }
    }

    val tasksByUrgency = remember(visibleTasks) {
        val comparator = compareBy<Task> { it.isCompleted }
            .thenBy { if (it.hasDate) it.startEpochDay else Long.MAX_VALUE }
            .thenBy { it.scheduledMinutesOfDay ?: Int.MAX_VALUE }
            .thenBy { it.id }
        TaskUrgency.entries.associateWith { urgency ->
            visibleTasks.filter { it.urgency == urgency }.sortedWith(comparator)
        }
    }

    val unscheduledInboxTasks = remember(visibleTasks) {
        visibleTasks.filter { !it.isCompleted && it.isUnscheduledInbox }
            .sortedBy { it.id }
    }

    var showPlanDayDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBackground)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header bar with "Planejar o dia" action button
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = OledSurface,
            border = BorderStroke(1.dp, OledBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    NerdIcon(
                        glyph = NerdIcons.Layers,
                        color = IndigoPrimary,
                        size = 18.sp
                    )
                    Column {
                        Text(
                            text = "Matriz de Eisenhower",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (unscheduledInboxTasks.isEmpty()) {
                                "Caixa de entrada limpa"
                            } else {
                                "${unscheduledInboxTasks.size} sem data/hora na Caixa de entrada"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    onClick = { showPlanDayDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    color = IndigoContainer,
                    border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NerdIcon(
                            glyph = NerdIcons.CalendarToday,
                            color = IndigoPrimary,
                            size = 14.sp
                        )
                        Text(
                            text = if (unscheduledInboxTasks.isNotEmpty()) {
                                "Planejar o dia (${unscheduledInboxTasks.size})"
                            } else {
                                "Planejar o dia"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = IndigoOnContainer
                        )
                    }
                }
            }
        }

        // Top Row:
        // Left: Importante e Urgente (VERY_HIGH - Vermelho)
        // Right: Importante e Não Urgente (HIGH - Amarelo)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MatrixQuadrantCell(
                urgency = TaskUrgency.VERY_HIGH,
                tasks = tasksByUrgency[TaskUrgency.VERY_HIGH].orEmpty(),
                onAddTask = { onCreateTaskInQuadrant(TaskUrgency.VERY_HIGH) },
                onToggleTask = onToggleTask,
                onEditTask = onEditTask,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            MatrixQuadrantCell(
                urgency = TaskUrgency.HIGH,
                tasks = tasksByUrgency[TaskUrgency.HIGH].orEmpty(),
                onAddTask = { onCreateTaskInQuadrant(TaskUrgency.HIGH) },
                onToggleTask = onToggleTask,
                onEditTask = onEditTask,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        // Bottom Row:
        // Left: Não Importante e Urgente (MEDIUM - Verde)
        // Right: Não Importante e Não Urgente (LOW - Azul)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MatrixQuadrantCell(
                urgency = TaskUrgency.MEDIUM,
                tasks = tasksByUrgency[TaskUrgency.MEDIUM].orEmpty(),
                onAddTask = { onCreateTaskInQuadrant(TaskUrgency.MEDIUM) },
                onToggleTask = onToggleTask,
                onEditTask = onEditTask,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            MatrixQuadrantCell(
                urgency = TaskUrgency.LOW,
                tasks = tasksByUrgency[TaskUrgency.LOW].orEmpty(),
                onAddTask = { onCreateTaskInQuadrant(TaskUrgency.LOW) },
                onToggleTask = onToggleTask,
                onEditTask = onEditTask,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }

    if (showPlanDayDialog) {
        PlanDayDialog(
            unscheduledTasks = unscheduledInboxTasks,
            onDismiss = { showPlanDayDialog = false },
            onDelegateTask = { updatedTask ->
                onUpdateTask(updatedTask)
            }
        )
    }
}

@Composable
private fun MatrixQuadrantCell(
    urgency: TaskUrgency,
    tasks: List<Task>,
    onAddTask: () -> Unit,
    onToggleTask: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(urgency.colorArgb.toInt())

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = OledSurface),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Quadrant Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accentColor.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = urgency.matrixTitlePtBr,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp
                    )
                    Text(
                        text = urgency.titlePtBr,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onAddTask,
                    modifier = Modifier.size(26.dp)
                ) {
                    NerdIcon(
                        glyph = NerdIcons.PlusCircle,
                        color = accentColor,
                        size = 16.sp
                    )
                }
            }

            HorizontalDivider(color = accentColor.copy(alpha = 0.35f), thickness = 1.dp)

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onAddTask)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sem tarefas",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = tasks,
                        key = { task -> task.id },
                        contentType = { "matrix_cell_task" }
                    ) { task ->
                        MatrixTaskCellItem(
                            task = task,
                            accentColor = accentColor,
                            onToggle = { onToggleTask(task) },
                            onClick = { onEditTask(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixTaskCellItem(
    task: Task,
    accentColor: Color,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    val dateLabel = remember(task.startEpochDay) {
        if (task.hasDate) {
            LocalDate.ofEpochDay(task.startEpochDay).format(ShortDateFormatter)
        } else {
            "Sem data"
        }
    }
    val timeLabel = remember(task.scheduledMinutesOfDay) {
        task.scheduledMinutesOfDay?.let { formatMinutesPtBr(it) } ?: "Sem hora"
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = OledSurfaceElevated,
        border = BorderStroke(
            width = 1.dp,
            color = if (task.isCompleted) OledBorder else accentColor.copy(alpha = 0.32f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = TaskCompleted,
                    uncheckedColor = accentColor
                ),
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (task.isCompleted) TextSecondary else TextPrimary,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$dateLabel • $timeLabel",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (task.advanceReminders.isNotEmpty()) {
                        Text(
                            text = "• 🔔${task.advanceReminders.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = IndigoPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanDayDialog(
    unscheduledTasks: List<Task>,
    onDismiss: () -> Unit,
    onDelegateTask: (Task) -> Unit
) {
    val todayEpochDay = remember { LocalDate.now().toEpochDay() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(20.dp),
            color = OledSurface,
            border = BorderStroke(1.dp, OledBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(IndigoContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            NerdIcon(
                                glyph = NerdIcons.CalendarToday,
                                color = IndigoPrimary,
                                size = 18.sp
                            )
                        }
                        Column {
                            Text(
                                text = "Planejar o dia",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Limpe a Caixa de entrada (Não Importante e Não Urgente) delegando data e quadrante.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        NerdIcon(glyph = NerdIcons.Close, color = TextSecondary, size = 18.sp)
                    }
                }

                HorizontalDivider(color = OledBorder, thickness = 1.dp)

                if (unscheduledTasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NerdIcon(
                                glyph = NerdIcons.CheckCircle,
                                color = TaskCompleted,
                                size = 36.sp
                            )
                            Text(
                                text = "Caixa de entrada limpa!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Todas as tarefas pendentes já possuem data ou horário definido.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = unscheduledTasks,
                            key = { it.id }
                        ) { task ->
                            PlanDayTaskItemCard(
                                task = task,
                                todayEpochDay = todayEpochDay,
                                onDelegate = onDelegateTask
                            )
                        }
                    }
                }

                HorizontalDivider(color = OledBorder, thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Fechar", color = IndigoPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanDayTaskItemCard(
    task: Task,
    todayEpochDay: Long,
    onDelegate: (Task) -> Unit
) {
    var targetEpochDay by remember(task.id) { mutableLongStateOf(todayEpochDay) }
    var useTime by remember(task.id) { mutableStateOf(false) }
    var targetMinutes by remember(task.id) { mutableIntStateOf(9 * 60) }
    var targetUrgency by remember(task.id) { mutableStateOf(TaskUrgency.HIGH) }
    var showDatePicker by remember(task.id) { mutableStateOf(false) }
    var showTimePicker by remember(task.id) { mutableStateOf(false) }

    val formattedDate = remember(targetEpochDay) {
        LocalDate.ofEpochDay(targetEpochDay).format(ShortDateFormatter)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = OledSurfaceElevated),
        border = BorderStroke(1.dp, OledBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            if (!task.notes.isNullOrBlank()) {
                Text(
                    text = task.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Date selection chips
            Text(
                text = "1. Delegar para qual data?",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = targetEpochDay == todayEpochDay,
                    onClick = { targetEpochDay = todayEpochDay },
                    label = { Text("Hoje") }
                )
                FilterChip(
                    selected = targetEpochDay == todayEpochDay + 1,
                    onClick = { targetEpochDay = todayEpochDay + 1 },
                    label = { Text("Amanhã") }
                )
                FilterChip(
                    selected = targetEpochDay != todayEpochDay && targetEpochDay != todayEpochDay + 1,
                    onClick = { showDatePicker = true },
                    leadingIcon = { NerdIcon(glyph = NerdIcons.Calendar, size = 13.sp) },
                    label = { Text("Data: $formattedDate") }
                )
                FilterChip(
                    selected = useTime,
                    onClick = {
                        if (!useTime) {
                            useTime = true
                            showTimePicker = true
                        } else {
                            useTime = false
                        }
                    },
                    leadingIcon = { NerdIcon(glyph = NerdIcons.ClockOutline, size = 13.sp) },
                    label = {
                        Text(
                            if (useTime) "Horário: ${formatMinutesPtBr(targetMinutes)}"
                            else "Definir horário (opcional)"
                        )
                    }
                )
            }

            // Quadrant selection
            Text(
                text = "2. Delegar para qual quadrante?",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TaskUrgency.entries.sortedBy { it.sortOrder }.forEach { option ->
                    val optionColor = Color(option.colorArgb.toInt())
                    val isSelected = targetUrgency == option
                    Surface(
                        onClick = { targetUrgency = option },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) optionColor.copy(alpha = 0.18f) else OledSurface,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) optionColor else OledBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(optionColor)
                                )
                                Text(
                                    text = option.matrixTitlePtBr,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = option.shortLabelPtBr,
                                style = MaterialTheme.typography.labelSmall,
                                color = optionColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            val selectedColor = Color(targetUrgency.colorArgb.toInt())
            Button(
                onClick = {
                    onDelegate(
                        task.copy(
                            startEpochDay = targetEpochDay,
                            endEpochDay = targetEpochDay,
                            scheduledMinutesOfDay = if (useTime) targetMinutes else null,
                            urgency = targetUrgency
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = selectedColor,
                    contentColor = Color(0xFF0B0F17)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                NerdIcon(
                    glyph = NerdIcons.CheckCircle,
                    color = Color(0xFF0B0F17),
                    size = 16.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Agendar para $formattedDate • ${targetUrgency.shortLabelPtBr}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showDatePicker) {
        DatePickerModalDialog(
            initialEpochDay = targetEpochDay,
            onDismiss = { showDatePicker = false },
            onConfirm = { pickedDay ->
                targetEpochDay = pickedDay
                showDatePicker = false
            }
        )
    }

    if (showTimePicker) {
        TimePickerModalDialog(
            initialMinutesOfDay = targetMinutes,
            title = "Horário da Tarefa",
            onDismiss = { showTimePicker = false },
            onConfirm = { pickedMinutes ->
                targetMinutes = pickedMinutes
                useTime = true
                showTimePicker = false
            }
        )
    }
}
