package com.taskmgr.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskmgr.ui.theme.IndigoOnPrimary
import com.example.taskmgr.ui.theme.IndigoPrimary
import com.example.taskmgr.ui.theme.OledBorder
import com.example.taskmgr.ui.theme.OledSurface
import com.example.taskmgr.ui.theme.OledSurfaceElevated
import com.example.taskmgr.ui.theme.PriorityHigh
import com.example.taskmgr.ui.theme.TextDisabled
import com.example.taskmgr.ui.theme.TextPrimary
import com.example.taskmgr.ui.theme.TextSecondary
import com.taskmgr.domain.model.Task
import com.taskmgr.domain.model.TaskUrgency
import com.taskmgr.ui.components.NerdIcon
import com.taskmgr.ui.components.NerdIcons
import com.taskmgr.ui.components.formatMinutes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MATRIX_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM", Locale.forLanguageTag("pt-BR"))

@Composable
fun EisenhowerMatrixScreen(
    tasks: List<Task>,
    onCreateTaskInQuadrant: (TaskUrgency) -> Unit,
    onToggleTask: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onTestTaskAlarm: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val tasksByUrgency = remember(tasks) {
        tasks
            .sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenBy { it.startEpochDay }
                    .thenBy { it.scheduledMinutesOfDay ?: Int.MAX_VALUE }
                    .thenBy { it.id }
            )
            .groupBy { it.urgency }
    }

    val quadrants = remember { TaskUrgency.entries.sortedBy { it.sortOrder } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "matrix_header", contentType = "matrix_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    NerdIcon(
                        glyph = NerdIcons.Layers,
                        color = IndigoPrimary,
                        size = 22.sp
                    )
                    Column {
                        Text(
                            text = "Matriz de Eisenhower",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        val pendingCount = tasks.count { !it.isCompleted }
                        Text(
                            text = "${tasks.size} tarefa(s) • $pendingCount pendente(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                FilledTonalButton(onClick = { onCreateTaskInQuadrant(TaskUrgency.LOW) }) {
                    NerdIcon(glyph = NerdIcons.PlusCircle, size = 17.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nova Tarefa")
                }
            }
        }

        items(
            items = quadrants,
            key = { "quadrant_${it.name}" },
            contentType = { "quadrant_card" }
        ) { urgency ->
            val quadrantTasks = tasksByUrgency[urgency].orEmpty()
            EisenhowerQuadrantCard(
                urgency = urgency,
                tasks = quadrantTasks,
                onAddTask = { onCreateTaskInQuadrant(urgency) },
                onToggleTask = onToggleTask,
                onEditTask = onEditTask,
                onDeleteTask = onDeleteTask,
                onTestTaskAlarm = onTestTaskAlarm
            )
        }

        item(key = "matrix_bottom_spacer", contentType = "spacer") {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun EisenhowerQuadrantCard(
    urgency: TaskUrgency,
    tasks: List<Task>,
    onAddTask: () -> Unit,
    onToggleTask: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onTestTaskAlarm: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val quadrantColor = Color(urgency.colorArgb.toInt())
    val completedCount = tasks.count { it.isCompleted }
    val glyph = remember(urgency) { urgencyGlyph(urgency) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OledSurface),
        border = BorderStroke(1.dp, quadrantColor.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(quadrantColor),
                    contentAlignment = Alignment.Center
                ) {
                    NerdIcon(
                        glyph = glyph,
                        color = Color(0xFF0B0F17),
                        size = 20.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = urgency.matrixTitlePtBr,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = quadrantColor.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, quadrantColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "${urgency.titlePtBr} • ${urgency.colorNamePtBr}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = quadrantColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = if (tasks.isEmpty()) {
                                "0 tarefas"
                            } else {
                                "${tasks.size} tarefa(s) • $completedCount concluída(s)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            HorizontalDivider(color = OledBorder, thickness = 1.dp)

            if (tasks.isEmpty()) {
                Text(
                    text = "Nenhuma tarefa classificada em '${urgency.matrixTitlePtBr}'.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tasks.forEach { task ->
                        MatrixTaskRow(
                            task = task,
                            quadrantColor = quadrantColor,
                            onToggleCompleted = { onToggleTask(task) },
                            onEdit = { onEditTask(task) },
                            onDelete = { onDeleteTask(task.id) },
                            onTestAlarm = { onTestTaskAlarm(task) }
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onAddTask,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, quadrantColor.copy(alpha = 0.5f))
            ) {
                NerdIcon(glyph = NerdIcons.Plus, size = 15.sp, color = quadrantColor)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Adicionar tarefa em ${urgency.titlePtBr}",
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun MatrixTaskRow(
    task: Task,
    quadrantColor: Color,
    onToggleCompleted: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTestAlarm: () -> Unit
) {
    val titleColor by animateColorAsState(
        targetValue = if (task.isCompleted) TextDisabled else TextPrimary,
        animationSpec = tween(220),
        label = "matrix_task_title_color"
    )

    val dateSummary = remember(task.startEpochDay, task.endEpochDay) {
        val startStr = LocalDate.ofEpochDay(task.startEpochDay).format(MATRIX_DATE_FORMATTER)
        if (task.endEpochDay > task.startEpochDay) {
            val endStr = LocalDate.ofEpochDay(task.endEpochDay).format(MATRIX_DATE_FORMATTER)
            "$startStr..$endStr"
        } else {
            startStr
        }
    }
    val timeSummary = task.scheduledMinutesOfDay?.let { " • ${formatMinutes(it)}" }.orEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = OledSurfaceElevated),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleCompleted() },
                colors = CheckboxDefaults.colors(
                    checkedColor = quadrantColor,
                    checkmarkColor = IndigoOnPrimary,
                    uncheckedColor = TextSecondary
                )
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = "Data: $dateSummary$timeSummary",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (!task.notes.isNullOrBlank()) {
                    Text(
                        text = task.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (task.isCompleted) TextDisabled else TextSecondary,
                        maxLines = 2
                    )
                }
            }

            IconButton(onClick = onTestAlarm) {
                NerdIcon(
                    glyph = NerdIcons.BellRing,
                    color = IndigoPrimary,
                    size = 17.sp
                )
            }
            IconButton(onClick = onEdit) {
                NerdIcon(
                    glyph = NerdIcons.Pencil,
                    color = TextSecondary,
                    size = 17.sp
                )
            }
            IconButton(onClick = onDelete) {
                NerdIcon(
                    glyph = NerdIcons.DeleteOutline,
                    color = PriorityHigh,
                    size = 17.sp
                )
            }
        }
    }
}

fun urgencyGlyph(urgency: TaskUrgency): String = when (urgency) {
    TaskUrgency.VERY_HIGH -> NerdIcons.Fire
    TaskUrgency.HIGH -> NerdIcons.Flash
    TaskUrgency.MEDIUM -> NerdIcons.Target
    TaskUrgency.LOW -> NerdIcons.ClockOutline
}
