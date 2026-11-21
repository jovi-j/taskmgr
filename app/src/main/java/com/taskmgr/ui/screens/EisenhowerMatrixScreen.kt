package com.taskmgr.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskmgr.ui.theme.IndigoOnPrimary
import com.example.taskmgr.ui.theme.IndigoPrimary
import com.example.taskmgr.ui.theme.OledBorder
import com.example.taskmgr.ui.theme.OledSurface
import com.example.taskmgr.ui.theme.OledSurfaceElevated
import com.example.taskmgr.ui.theme.TextDisabled
import com.example.taskmgr.ui.theme.TextPrimary
import com.example.taskmgr.ui.theme.TextSecondary
import com.example.taskmgr.ui.theme.UbuntuSansMonoNerdFontFamily
import com.taskmgr.domain.model.Task
import com.taskmgr.domain.model.TaskUrgency
import com.taskmgr.ui.components.NerdIcon
import com.taskmgr.ui.components.NerdIcons
import com.taskmgr.ui.components.formatMinutes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MATRIX_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yy", Locale.forLanguageTag("pt-BR"))

@Composable
fun EisenhowerMatrixScreen(
    tasks: List<Task>,
    onCreateTaskInQuadrant: (TaskUrgency) -> Unit,
    onToggleTask: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    @Suppress("UNUSED_PARAMETER") onDeleteTask: (Long) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onTestTaskAlarm: (Task) -> Unit = {},
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Cabeçalho compacto da Matriz
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
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
                    size = 20.sp
                )
                Column {
                    Text(
                        text = "Matriz de Eisenhower",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    val pendingCount = tasks.count { !it.isCompleted }
                    Text(
                        text = "${tasks.size} tarefa(s) • $pendingCount pendente(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            FilledTonalButton(
                onClick = { onCreateTaskInQuadrant(TaskUrgency.LOW) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                NerdIcon(glyph = NerdIcons.PlusCircle, size = 15.sp)
                Spacer(modifier = Modifier.width(5.dp))
                Text("Nova Tarefa", style = MaterialTheme.typography.labelMedium)
            }
        }

        // Linha Superior da Matriz 2x2:
        // Esquerdo Superior: Importante e Urgente (VERY_HIGH - Vermelho)
        // Direito Superior: Importante e Não Urgente (MEDIUM - Verde)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EisenhowerQuadrantCell(
                quadrantTitle = "Importante e Urgente",
                urgency = TaskUrgency.VERY_HIGH,
                tasks = tasksByUrgency[TaskUrgency.VERY_HIGH].orEmpty(),
                onAddTask = { onCreateTaskInQuadrant(TaskUrgency.VERY_HIGH) },
                onToggleTask = onToggleTask,
                onEditTask = onEditTask,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            EisenhowerQuadrantCell(
                quadrantTitle = "Importante e Não Urgente",
                urgency = TaskUrgency.MEDIUM,
                tasks = tasksByUrgency[TaskUrgency.MEDIUM].orEmpty(),
                onAddTask = { onCreateTaskInQuadrant(TaskUrgency.MEDIUM) },
                onToggleTask = onToggleTask,
                onEditTask = onEditTask,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        // Linha Inferior da Matriz 2x2:
        // Esquerdo Inferior: Não importante e Urgente (HIGH - Amarelo)
        // Direito Inferior: Não Urgente e Não importante (LOW - Azul)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EisenhowerQuadrantCell(
                quadrantTitle = "Não importante e Urgente",
                urgency = TaskUrgency.HIGH,
                tasks = tasksByUrgency[TaskUrgency.HIGH].orEmpty(),
                onAddTask = { onCreateTaskInQuadrant(TaskUrgency.HIGH) },
                onToggleTask = onToggleTask,
                onEditTask = onEditTask,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            EisenhowerQuadrantCell(
                quadrantTitle = "Não Urgente e Não importante",
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
}

@Composable
private fun EisenhowerQuadrantCell(
    quadrantTitle: String,
    urgency: TaskUrgency,
    tasks: List<Task>,
    onAddTask: () -> Unit,
    onToggleTask: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val quadrantColor = Color(urgency.colorArgb.toInt())
    val glyph = remember(urgency) { urgencyGlyph(urgency) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = OledSurface),
        border = BorderStroke(1.5.dp, quadrantColor.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Cabeçalho do Quadrante
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(quadrantColor.copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(quadrantColor),
                    contentAlignment = Alignment.Center
                ) {
                    NerdIcon(
                        glyph = glyph,
                        color = Color(0xFF0B0F17),
                        size = 12.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quadrantTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )
                    Text(
                        text = "${urgency.titlePtBr} (${tasks.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = quadrantColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp
                    )
                }

                Surface(
                    onClick = onAddTask,
                    shape = CircleShape,
                    color = quadrantColor.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, quadrantColor.copy(alpha = 0.6f)),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        NerdIcon(
                            glyph = NerdIcons.Plus,
                            color = quadrantColor,
                            size = 12.sp
                        )
                    }
                }
            }

            HorizontalDivider(color = quadrantColor.copy(alpha = 0.3f), thickness = 1.dp)

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sem tarefas",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = tasks,
                        key = { "q_${urgency.name}_task_${it.id}" },
                        contentType = { "matrix_task_cell_item" }
                    ) { task ->
                        MatrixQuadrantTaskItem(
                            task = task,
                            quadrantColor = quadrantColor,
                            onToggleCompleted = { onToggleTask(task) },
                            onEdit = { onEditTask(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixQuadrantTaskItem(
    task: Task,
    quadrantColor: Color,
    onToggleCompleted: () -> Unit,
    onEdit: () -> Unit
) {
    val titleColor by animateColorAsState(
        targetValue = if (task.isCompleted) TextDisabled else TextPrimary,
        animationSpec = tween(200),
        label = "quadrant_task_title_color"
    )

    val dateLabel = remember(task.startEpochDay, task.endEpochDay) {
        val startStr = LocalDate.ofEpochDay(task.startEpochDay).format(MATRIX_DATE_FORMATTER)
        if (task.endEpochDay > task.startEpochDay) {
            val endStr = LocalDate.ofEpochDay(task.endEpochDay).format(MATRIX_DATE_FORMATTER)
            "$startStr..$endStr"
        } else {
            startStr
        }
    }

    val timeLabel = remember(task.scheduledMinutesOfDay) {
        task.scheduledMinutesOfDay?.let { formatMinutes(it) } ?: "Dia todo"
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = OledSurfaceElevated,
        border = BorderStroke(1.dp, OledBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onEdit)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleCompleted() },
                colors = CheckboxDefaults.colors(
                    checkedColor = quadrantColor,
                    checkmarkColor = IndigoOnPrimary,
                    uncheckedColor = TextSecondary
                ),
                modifier = Modifier
                    .size(28.dp)
                    .scale(0.82f)
            )

            Spacer(modifier = Modifier.width(3.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        NerdIcon(
                            glyph = NerdIcons.CalendarToday,
                            color = TextSecondary,
                            size = 10.sp
                        )
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = UbuntuSansMonoNerdFontFamily,
                                fontSize = 10.sp
                            ),
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        NerdIcon(
                            glyph = NerdIcons.ClockOutline,
                            color = quadrantColor,
                            size = 10.sp
                        )
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = UbuntuSansMonoNerdFontFamily,
                                fontSize = 10.sp
                            ),
                            color = quadrantColor,
                            maxLines = 1
                        )
                    }
                }
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
