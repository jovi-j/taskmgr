package com.taskmgr.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jovij.taskmgr.ui.theme.IndigoPrimary
import com.jovij.taskmgr.ui.theme.OledBackground
import com.jovij.taskmgr.ui.theme.OledBorder
import com.jovij.taskmgr.ui.theme.OledSurface
import com.jovij.taskmgr.ui.theme.OledSurfaceElevated
import com.jovij.taskmgr.ui.theme.PriorityHigh
import com.jovij.taskmgr.ui.theme.TextPrimary
import com.jovij.taskmgr.ui.theme.TextSecondary
import com.taskmgr.domain.logic.DaysOfWeekMask
import com.taskmgr.domain.model.CompletionType
import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.HabitGroup
import com.taskmgr.domain.model.Moment
import com.taskmgr.domain.model.Task
import com.taskmgr.domain.model.TaskUrgency
import com.taskmgr.ui.components.DatePickerModalDialog
import com.taskmgr.ui.components.DateSelectorField
import com.taskmgr.ui.components.DaysOfWeekSelector
import com.taskmgr.ui.components.HabitBadgeIcon
import com.taskmgr.ui.components.HabitGroupEditorDialog
import com.taskmgr.ui.components.HabitIconPickerDialog
import com.taskmgr.ui.components.NerdIcon
import com.taskmgr.ui.components.NerdIcons
import com.taskmgr.ui.components.RepeatIntervalHourMinuteFields
import com.taskmgr.ui.components.TimePickerModalDialog
import com.taskmgr.ui.components.TimeSelectorField
import com.taskmgr.ui.components.momentNerdGlyph
import com.taskmgr.ui.components.oledTextFieldColors
import java.time.LocalDate

@Composable
private fun EditorSectionCard(
    title: String,
    glyph: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OledSurface),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NerdIcon(glyph = glyph, color = IndigoPrimary, size = 18.sp)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            content()
        }
    }
}

@Composable
private fun EditorSwitchRow(
    glyph: String,
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            NerdIcon(
                glyph = glyph,
                color = if (checked) IndigoPrimary else TextSecondary,
                size = 20.sp
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun EditorErrorBanner(errorMessage: String?) {
    AnimatedVisibility(
        visible = errorMessage != null,
        enter = expandVertically(tween(200, easing = FastOutSlowInEasing)) + fadeIn(tween(160)),
        exit = shrinkVertically(tween(160)) + fadeOut(tween(120))
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NerdIcon(glyph = NerdIcons.AlertCircle, color = PriorityHigh, size = 18.sp)
                Text(
                    text = errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun TaskDetailsSection(
    title: String,
    notes: String,
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit
) {
    val textFieldColors = oledTextFieldColors()
    EditorSectionCard(title = "Detalhes da Tarefa", glyph = NerdIcons.Pencil) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Título da tarefa") },
            leadingIcon = { NerdIcon(glyph = NerdIcons.Pencil, size = 18.sp) },
            singleLine = true,
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Notas (opcional)") },
            leadingIcon = { NerdIcon(glyph = NerdIcons.FormatListChecks, size = 18.sp) },
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TaskUrgencySection(
    selectedUrgency: TaskUrgency,
    isUnscheduled: Boolean,
    onSelectUrgency: (TaskUrgency) -> Unit
) {
    val options = remember { TaskUrgency.entries.sortedBy { it.sortOrder } }
    EditorSectionCard(title = "Matriz de Eisenhower (Urgência)", glyph = NerdIcons.Layers) {
        Text(
            text = if (isUnscheduled && selectedUrgency == TaskUrgency.LOW) {
                "Tarefas sem data e sem horário ficam em Urgência Baixa (Caixa de entrada) até serem planejadas."
            } else {
                "Toda tarefa inicia em Urgência Baixa (Não Importante e Não Urgente) por padrão, exceto quando criada diretamente em um quadrante."
            },
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val isSelected = selectedUrgency == option
                val optionColor = Color(option.colorArgb.toInt())
                Surface(
                    onClick = { onSelectUrgency(option) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) optionColor.copy(alpha = 0.16f) else OledSurfaceElevated,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) optionColor else OledBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(optionColor),
                            contentAlignment = Alignment.Center
                        ) {
                            NerdIcon(
                                glyph = urgencyGlyph(option),
                                color = Color(0xFF0B0F17),
                                size = 16.sp
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.matrixTitlePtBr,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${option.titlePtBr} • ${option.colorNamePtBr}",
                                style = MaterialTheme.typography.bodySmall,
                                color = optionColor
                            )
                        }
                        if (isSelected) {
                            NerdIcon(
                                glyph = NerdIcons.CheckCircle,
                                color = optionColor,
                                size = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskDateAndDurationSection(
    useDate: Boolean,
    startEpochDay: Long,
    todayEpochDay: Long,
    durationDaysText: String,
    onUseDateChange: (Boolean) -> Unit,
    onOpenDatePicker: () -> Unit,
    onSelectStartDay: (Long) -> Unit,
    onDurationDaysChange: (String) -> Unit
) {
    EditorSectionCard(title = "Data e Duração", glyph = NerdIcons.Calendar) {
        EditorSwitchRow(
            glyph = NerdIcons.Calendar,
            label = "Definir data",
            subtitle = if (useDate) "Agendada para uma data específica" else "Sem data (vai para a Caixa de entrada / Urgência Baixa)",
            checked = useDate,
            onCheckedChange = onUseDateChange
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = !useDate,
                onClick = { onUseDateChange(false) },
                label = { Text("Sem data") }
            )
            FilterChip(
                selected = useDate && startEpochDay == todayEpochDay,
                onClick = {
                    onUseDateChange(true)
                    onSelectStartDay(todayEpochDay)
                },
                label = { Text("Hoje") }
            )
            FilterChip(
                selected = useDate && startEpochDay == todayEpochDay + 1,
                onClick = {
                    onUseDateChange(true)
                    onSelectStartDay(todayEpochDay + 1)
                },
                label = { Text("Amanhã") }
            )
        }

        AnimatedVisibility(
            visible = useDate,
            enter = expandVertically(tween(200, easing = FastOutSlowInEasing)) + fadeIn(tween(160)),
            exit = shrinkVertically(tween(160)) + fadeOut(tween(120))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DateSelectorField(
                    label = "Data de início",
                    epochDay = startEpochDay,
                    onClick = onOpenDatePicker
                )

                OutlinedTextField(
                    value = durationDaysText,
                    onValueChange = { onDurationDaysChange(it.filter { c -> c.isDigit() }) },
                    label = { Text("Duração em dias (1..N)") },
                    leadingIcon = { NerdIcon(glyph = NerdIcons.CalendarToday, size = 18.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = oledTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskTimeAndMomentSection(
    useExactTime: Boolean,
    scheduledMinutes: Int,
    moments: List<Moment>,
    selectedMomentId: Long?,
    repeatHoursText: String,
    repeatMinutesText: String,
    advanceReminders: List<com.taskmgr.domain.model.TaskAdvanceReminder>,
    onUseExactTimeChange: (Boolean) -> Unit,
    onOpenTimePicker: () -> Unit,
    onSelectMomentId: (Long?) -> Unit,
    onRepeatHoursChange: (String) -> Unit,
    onRepeatMinutesChange: (String) -> Unit,
    onAddAdvanceReminder: (com.taskmgr.domain.model.TaskAdvanceReminder) -> Unit,
    onRemoveAdvanceReminder: (Int) -> Unit
) {
    val textFieldColors = oledTextFieldColors()
    var advDaysText by remember { mutableStateOf("") }
    var advHoursText by remember { mutableStateOf("") }
    var advMinutesText by remember { mutableStateOf("") }
    var advRepeatHoursText by remember { mutableStateOf("") }
    var advRepeatMinutesText by remember { mutableStateOf("") }

    EditorSectionCard(title = "Horário & Momento", glyph = NerdIcons.ClockOutline) {
        EditorSwitchRow(
            glyph = NerdIcons.ClockOutline,
            label = "Horário exato",
            subtitle = "Dispara no minuto configurado (opcional)",
            checked = useExactTime,
            onCheckedChange = onUseExactTimeChange
        )

        AnimatedVisibility(
            visible = useExactTime,
            enter = expandVertically(tween(200, easing = FastOutSlowInEasing)) + fadeIn(tween(160)),
            exit = shrinkVertically(tween(160)) + fadeOut(tween(120))
        ) {
            TimeSelectorField(
                label = "Horário do lembrete",
                minutesOfDay = scheduledMinutes,
                onClick = onOpenTimePicker
            )
        }

        if (moments.isNotEmpty()) {
            Text(
                text = "Momento associado (opcional)",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = selectedMomentId == null,
                    onClick = { onSelectMomentId(null) },
                    label = { Text("Nenhum") }
                )
                moments.forEach { moment ->
                    FilterChip(
                        selected = selectedMomentId == moment.id,
                        onClick = { onSelectMomentId(moment.id) },
                        leadingIcon = {
                            NerdIcon(
                                glyph = momentNerdGlyph(moment.startMinutesOfDay),
                                size = 14.sp
                            )
                        },
                        label = { Text(moment.name) }
                    )
                }
            }
        }

        RepeatIntervalHourMinuteFields(
            hoursText = repeatHoursText,
            minutesText = repeatMinutesText,
            onHoursChange = onRepeatHoursChange,
            onMinutesChange = onRepeatMinutesChange,
            isOptional = true
        )

        HorizontalDivider(color = OledBorder, thickness = 1.dp)

        // Advance reminders section ("Lembrar X tempo antes do horário especificado")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NerdIcon(glyph = NerdIcons.BellRing, color = IndigoPrimary, size = 18.sp)
            Column {
                Text(
                    text = "Lembrar antes do horário especificado",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Adicione quantos lembretes antecipados quiser (dias, horas, minutos antes e frequência).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Quick preset chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = false,
                onClick = {
                    onAddAdvanceReminder(
                        com.taskmgr.domain.model.TaskAdvanceReminder(daysBefore = 1)
                    )
                },
                leadingIcon = { NerdIcon(glyph = NerdIcons.Plus, color = IndigoPrimary, size = 13.sp) },
                label = { Text("1 dia antes") }
            )
            FilterChip(
                selected = false,
                onClick = {
                    onAddAdvanceReminder(
                        com.taskmgr.domain.model.TaskAdvanceReminder(hoursBefore = 2)
                    )
                },
                leadingIcon = { NerdIcon(glyph = NerdIcons.Plus, color = IndigoPrimary, size = 13.sp) },
                label = { Text("2 horas antes") }
            )
            FilterChip(
                selected = false,
                onClick = {
                    onAddAdvanceReminder(
                        com.taskmgr.domain.model.TaskAdvanceReminder(minutesBefore = 30)
                    )
                },
                leadingIcon = { NerdIcon(glyph = NerdIcons.Plus, color = IndigoPrimary, size = 13.sp) },
                label = { Text("30 min antes") }
            )
        }

        if (advanceReminders.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                advanceReminders.forEachIndexed { index, item ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = OledSurfaceElevated,
                        border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.4f)),
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
                                NerdIcon(glyph = NerdIcons.Alarm, color = IndigoPrimary, size = 16.sp)
                                Text(
                                    text = item.formatLabelPtBr(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                            IconButton(
                                onClick = { onRemoveAdvanceReminder(index) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                NerdIcon(glyph = NerdIcons.Close, color = PriorityHigh, size = 15.sp)
                            }
                        }
                    }
                }
            }
        }

        // Custom Advance Reminder Builder
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = OledSurfaceElevated,
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
                    text = "Novo lembrete antecipado customizado",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = advDaysText,
                        onValueChange = { advDaysText = it.filter { c -> c.isDigit() } },
                        label = { Text("Dias") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = advHoursText,
                        onValueChange = { advHoursText = it.filter { c -> c.isDigit() } },
                        label = { Text("Horas") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = advMinutesText,
                        onValueChange = { advMinutesText = it.filter { c -> c.isDigit() } },
                        label = { Text("Minutos") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Frequência de repetição deste lembrete (opcional)",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = advRepeatHoursText,
                        onValueChange = { advRepeatHoursText = it.filter { c -> c.isDigit() } },
                        label = { Text("Hora(s)") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = advRepeatMinutesText,
                        onValueChange = { advRepeatMinutesText = it.filter { c -> c.isDigit() } },
                        label = { Text("Minuto(s)") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                val parsedDays = advDaysText.toIntOrNull() ?: 0
                val parsedHours = advHoursText.toIntOrNull() ?: 0
                val parsedMinutes = advMinutesText.toIntOrNull() ?: 0
                val parsedRepMin = ((advRepeatHoursText.toIntOrNull() ?: 0) * 60 +
                    (advRepeatMinutesText.toIntOrNull() ?: 0)).takeIf { it > 0 }
                val canAddCustom = (parsedDays + parsedHours + parsedMinutes) > 0

                Button(
                    onClick = {
                        if (canAddCustom) {
                            onAddAdvanceReminder(
                                com.taskmgr.domain.model.TaskAdvanceReminder(
                                    daysBefore = parsedDays,
                                    hoursBefore = parsedHours,
                                    minutesBefore = parsedMinutes,
                                    repeatIntervalMinutes = parsedRepMin
                                )
                            )
                            advDaysText = ""
                            advHoursText = ""
                            advMinutesText = ""
                            advRepeatHoursText = ""
                            advRepeatMinutesText = ""
                        }
                    },
                    enabled = canAddCustom,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NerdIcon(glyph = NerdIcons.PlusCircle, size = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Adicionar lembrete antecipado")
                }
            }
        }
    }
}

@Composable
private fun TaskAlertsSection(
    aggressiveFullScreen: Boolean,
    vibrationEnabled: Boolean,
    onAggressiveFullScreenChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit
) {
    EditorSectionCard(title = "Alertas", glyph = NerdIcons.BellAlert) {
        EditorSwitchRow(
            glyph = NerdIcons.BellAlert,
            label = "Alarme em tela cheia",
            subtitle = "Abre sobre a tela bloqueada ou app atual (apenas com a opção 'Sim')",
            checked = aggressiveFullScreen,
            onCheckedChange = onAggressiveFullScreenChange
        )
        EditorSwitchRow(
            glyph = NerdIcons.Vibrate,
            label = "Vibração",
            checked = vibrationEnabled,
            onCheckedChange = onVibrationEnabledChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    initialTask: Task?,
    defaultEpochDay: Long,
    defaultUrgency: TaskUrgency = TaskUrgency.LOW,
    moments: List<Moment>,
    onBack: () -> Unit,
    onSave: (Task) -> Unit
) {
    BackHandler(onBack = onBack)

    val todayEpochDay = remember { LocalDate.now().toEpochDay() }
    var title by remember(initialTask?.id) { mutableStateOf(initialTask?.title ?: "") }
    var notes by remember(initialTask?.id) { mutableStateOf(initialTask?.notes ?: "") }
    var selectedUrgency by remember(initialTask?.id, defaultUrgency) {
        mutableStateOf(initialTask?.urgency ?: defaultUrgency)
    }
    var useDate by remember(initialTask?.id) {
        mutableStateOf(
            if (initialTask != null) initialTask.hasDate else false
        )
    }
    var startEpochDay by remember(initialTask?.id) {
        mutableLongStateOf(
            if (initialTask != null && initialTask.hasDate) initialTask.startEpochDay
            else maxOf(defaultEpochDay, todayEpochDay)
        )
    }
    var durationDaysText by remember(initialTask?.id) {
        mutableStateOf(
            if (initialTask != null && initialTask.hasDate) {
                ((initialTask.endEpochDay - initialTask.startEpochDay) + 1).coerceAtLeast(1).toString()
            } else "1"
        )
    }
    var useExactTime by remember(initialTask?.id) { mutableStateOf(initialTask?.scheduledMinutesOfDay != null) }
    var scheduledMinutes by remember(initialTask?.id) { mutableIntStateOf(initialTask?.scheduledMinutesOfDay ?: 9 * 60) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedMomentId by remember(initialTask?.id) { mutableStateOf(initialTask?.momentId) }
    val initialRepeat = initialTask?.repeatIntervalMinutes
    var repeatHoursText by remember(initialTask?.id) {
        mutableStateOf(initialRepeat?.let { (it / 60).takeIf { h -> h > 0 }?.toString() } ?: "")
    }
    var repeatMinutesText by remember(initialTask?.id) {
        mutableStateOf(initialRepeat?.let { (it % 60).takeIf { m -> m > 0 }?.toString() } ?: "")
    }
    var advanceReminders by remember(initialTask?.id) {
        mutableStateOf(initialTask?.advanceReminders ?: emptyList())
    }
    var aggressiveFullScreen by remember(initialTask?.id) { mutableStateOf(initialTask?.aggressiveFullScreen ?: true) }
    var vibrationEnabled by remember(initialTask?.id) { mutableStateOf(initialTask?.vibrationEnabled ?: true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isUnscheduled = !useDate && !useExactTime

    fun submit() {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            errorMessage = "Informe o título da tarefa."
            return
        }
        val parsedMinutes = if (useExactTime) scheduledMinutes else null
        val durationDays = (durationDaysText.toLongOrNull() ?: 1L).coerceAtLeast(1L)
        val totalRepeatMin = (repeatHoursText.toIntOrNull() ?: 0) * 60 + (repeatMinutesText.toIntOrNull() ?: 0)
        val repeatInterval = totalRepeatMin.takeIf { it > 0 }
        val finalStartDay = if (useDate) startEpochDay else Task.UNSCHEDULED_EPOCH_DAY
        val finalEndDay = if (useDate) (startEpochDay + durationDays - 1L) else Task.UNSCHEDULED_EPOCH_DAY

        onSave(
            Task(
                id = initialTask?.id ?: 0L,
                title = cleanTitle,
                notes = notes.trim().ifBlank { null },
                startEpochDay = finalStartDay,
                endEpochDay = finalEndDay,
                scheduledMinutesOfDay = parsedMinutes,
                momentId = selectedMomentId,
                repeatIntervalMinutes = repeatInterval,
                aggressiveFullScreen = aggressiveFullScreen,
                vibrationEnabled = vibrationEnabled,
                isCompleted = initialTask?.isCompleted ?: false,
                snoozedUntilEpochMillis = initialTask?.snoozedUntilEpochMillis,
                urgency = selectedUrgency,
                advanceReminders = advanceReminders
            )
        )
    }

    Scaffold(
        containerColor = OledBackground,
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = OledBackground,
                        titleContentColor = TextPrimary
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = TextPrimary
                            )
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NerdIcon(
                                glyph = NerdIcons.CheckboxMarkedCircle,
                                color = IndigoPrimary,
                                size = 22.sp
                            )
                            Text(
                                text = if (initialTask == null) "Nova Tarefa" else "Editar Tarefa",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = { submit() }) {
                            Text("Salvar", color = IndigoPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                HorizontalDivider(color = OledBorder, thickness = 1.dp)
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(OledSurface)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                HorizontalDivider(color = OledBorder, thickness = 1.dp)
                Button(
                    onClick = { submit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    NerdIcon(glyph = NerdIcons.CheckCircle, size = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (initialTask == null) "Criar Tarefa" else "Salvar Alterações",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (errorMessage != null) {
                item(key = "task_error_banner", contentType = "error_banner") {
                    EditorErrorBanner(errorMessage = errorMessage)
                }
            }

            item(key = "task_details_section", contentType = "task_details") {
                TaskDetailsSection(
                    title = title,
                    notes = notes,
                    onTitleChange = {
                        title = it
                        if (errorMessage != null) errorMessage = null
                    },
                    onNotesChange = { notes = it }
                )
            }

            item(key = "task_date_section", contentType = "task_date") {
                TaskDateAndDurationSection(
                    useDate = useDate,
                    startEpochDay = startEpochDay,
                    todayEpochDay = todayEpochDay,
                    durationDaysText = durationDaysText,
                    onUseDateChange = { enabled ->
                        useDate = enabled
                        if (!enabled && !useExactTime) {
                            selectedUrgency = TaskUrgency.LOW
                        }
                    },
                    onOpenDatePicker = {
                        useDate = true
                        showDatePicker = true
                    },
                    onSelectStartDay = { startEpochDay = it },
                    onDurationDaysChange = { durationDaysText = it }
                )
            }

            item(key = "task_time_section", contentType = "task_time") {
                TaskTimeAndMomentSection(
                    useExactTime = useExactTime,
                    scheduledMinutes = scheduledMinutes,
                    moments = moments,
                    selectedMomentId = selectedMomentId,
                    repeatHoursText = repeatHoursText,
                    repeatMinutesText = repeatMinutesText,
                    advanceReminders = advanceReminders,
                    onUseExactTimeChange = { enabled ->
                        useExactTime = enabled
                        if (!useDate && !enabled) {
                            selectedUrgency = TaskUrgency.LOW
                        }
                    },
                    onOpenTimePicker = { showTimePicker = true },
                    onSelectMomentId = { id ->
                        selectedMomentId = id
                        if (id != null && initialTask == null) useExactTime = false
                    },
                    onRepeatHoursChange = { repeatHoursText = it },
                    onRepeatMinutesChange = { repeatMinutesText = it },
                    onAddAdvanceReminder = { reminder ->
                        if (reminder.totalMinutesBefore > 0L && !advanceReminders.contains(reminder)) {
                            advanceReminders = (advanceReminders + reminder)
                                .sortedByDescending { it.totalMinutesBefore }
                        }
                    },
                    onRemoveAdvanceReminder = { index ->
                        advanceReminders = advanceReminders.filterIndexed { i, _ -> i != index }
                    }
                )
            }

            item(key = "task_urgency_section", contentType = "task_urgency") {
                TaskUrgencySection(
                    selectedUrgency = selectedUrgency,
                    isUnscheduled = isUnscheduled,
                    onSelectUrgency = { selectedUrgency = it }
                )
            }

            item(key = "task_alerts_section", contentType = "task_alerts") {
                TaskAlertsSection(
                    aggressiveFullScreen = aggressiveFullScreen,
                    vibrationEnabled = vibrationEnabled,
                    onAggressiveFullScreenChange = { aggressiveFullScreen = it },
                    onVibrationEnabledChange = { vibrationEnabled = it }
                )
            }

            item(key = "task_bottom_spacer", contentType = "spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showTimePicker) {
        TimePickerModalDialog(
            initialMinutesOfDay = scheduledMinutes,
            title = "Horário da Tarefa",
            onDismiss = { showTimePicker = false },
            onConfirm = { picked ->
                scheduledMinutes = picked
                showTimePicker = false
            }
        )
    }

    if (showDatePicker) {
        DatePickerModalDialog(
            initialEpochDay = startEpochDay,
            onDismiss = { showDatePicker = false },
            onConfirm = { pickedDay ->
                startEpochDay = pickedDay
                showDatePicker = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitIdentitySection(
    title: String,
    notes: String,
    selectedIconGlyph: String,
    selectedGroup: HabitGroup?,
    habitGroups: List<HabitGroup>,
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onOpenIconPicker: () -> Unit,
    onSelectGroup: (HabitGroup?) -> Unit,
    onOpenCreateGroup: () -> Unit
) {
    val previewColor = selectedGroup?.let { Color(it.colorArgb.toInt()) } ?: IndigoPrimary
    val textFieldColors = oledTextFieldColors()

    EditorSectionCard(title = "Identidade, Grupo & Ícone", glyph = NerdIcons.Tag) {
        Surface(
            onClick = onOpenIconPicker,
            shape = RoundedCornerShape(12.dp),
            color = OledSurfaceElevated,
            border = BorderStroke(1.dp, OledBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HabitBadgeIcon(
                    iconGlyph = selectedIconGlyph,
                    groupColorArgb = selectedGroup?.colorArgb,
                    size = 48.dp,
                    iconSize = 23.sp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedGroup?.name ?: "Sem grupo",
                        style = MaterialTheme.typography.labelMedium,
                        color = previewColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Toque para escolher o ícone",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
                NerdIcon(glyph = NerdIcons.Tune, color = TextSecondary, size = 16.sp)
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Nome do hábito (ex.: Beber água, Arrumar quarto)") },
            leadingIcon = { NerdIcon(glyph = NerdIcons.Pencil, size = 18.sp) },
            singleLine = true,
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Notas (opcional)") },
            leadingIcon = { NerdIcon(glyph = NerdIcons.FormatListChecks, size = 18.sp) },
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Grupo do hábito (define a cor circular do ícone)",
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = selectedGroup == null,
                onClick = { onSelectGroup(null) },
                label = { Text("Nenhum") }
            )
            habitGroups.forEach { group ->
                FilterChip(
                    selected = selectedGroup?.id == group.id,
                    onClick = { onSelectGroup(group) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(group.colorArgb.toInt()))
                        )
                    },
                    label = { Text(group.name) }
                )
            }
            FilterChip(
                selected = false,
                onClick = onOpenCreateGroup,
                leadingIcon = { NerdIcon(glyph = NerdIcons.PlusCircle, color = IndigoPrimary, size = 15.sp) },
                label = { Text("Novo grupo") }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitScheduleAndFrequencySection(
    startEpochDay: Long,
    todayEpochDay: Long,
    activeDaysMask: Int,
    onOpenDatePicker: () -> Unit,
    onSelectStartDay: (Long) -> Unit,
    onActiveDaysMaskChange: (Int) -> Unit
) {
    EditorSectionCard(title = "Início Retroativo & Frequência", glyph = NerdIcons.History) {
        DateSelectorField(
            label = "Data de início (pode ser anterior a hoje)",
            epochDay = startEpochDay,
            onClick = onOpenDatePicker
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = startEpochDay == todayEpochDay,
                onClick = { onSelectStartDay(todayEpochDay) },
                label = { Text("Hoje") }
            )
            FilterChip(
                selected = startEpochDay == todayEpochDay - 1,
                onClick = { onSelectStartDay(todayEpochDay - 1) },
                label = { Text("Ontem") }
            )
            FilterChip(
                selected = startEpochDay == todayEpochDay - 7,
                onClick = { onSelectStartDay(todayEpochDay - 7) },
                label = { Text("7 dias atrás") }
            )
            FilterChip(
                selected = startEpochDay == todayEpochDay - 30,
                onClick = { onSelectStartDay(todayEpochDay - 30) },
                label = { Text("30 dias atrás") }
            )
        }

        Text(
            text = "Dias ativos na semana",
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary
        )
        DaysOfWeekSelector(mask = activeDaysMask, onMaskChange = onActiveDaysMaskChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitCompletionModeSection(
    completionType: CompletionType,
    countPerDayTargetText: String,
    customIncrementValueText: String,
    customIncrementUnitText: String,
    customTargetValueText: String,
    customTargetUnitText: String,
    onCompletionTypeChange: (CompletionType) -> Unit,
    onCountPerDayTargetChange: (String) -> Unit,
    onCustomIncrementValueChange: (String) -> Unit,
    onCustomIncrementUnitChange: (String) -> Unit,
    onCustomTargetValueChange: (String) -> Unit,
    onCustomTargetUnitChange: (String) -> Unit
) {
    val textFieldColors = oledTextFieldColors()

    EditorSectionCard(title = "Modo de Conclusão", glyph = NerdIcons.Target) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = completionType == CompletionType.BOOLEAN,
                onClick = { onCompletionTypeChange(CompletionType.BOOLEAN) },
                leadingIcon = { NerdIcon(glyph = NerdIcons.CheckCircle, size = 14.sp) },
                label = { Text("Sim e Não") }
            )
            FilterChip(
                selected = completionType == CompletionType.COUNT_PER_DAY,
                onClick = { onCompletionTypeChange(CompletionType.COUNT_PER_DAY) },
                leadingIcon = { NerdIcon(glyph = NerdIcons.Counter, size = 14.sp) },
                label = { Text("Por quantidade") }
            )
            FilterChip(
                selected = completionType == CompletionType.CUSTOM_UNIT,
                onClick = { onCompletionTypeChange(CompletionType.CUSTOM_UNIT) },
                leadingIcon = { NerdIcon(glyph = NerdIcons.CupWater, size = 14.sp) },
                label = { Text("Customizado") }
            )
        }

        AnimatedContent(
            targetState = completionType,
            transitionSpec = {
                fadeIn(tween(180)) togetherWith fadeOut(tween(140))
            },
            label = "completion_type_transition"
        ) { currentType ->
            when (currentType) {
                CompletionType.BOOLEAN -> {
                    Text(
                        text = "1 confirmação 'Sim' conclui o hábito no dia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                CompletionType.COUNT_PER_DAY -> {
                    OutlinedTextField(
                        value = countPerDayTargetText,
                        onValueChange = { onCountPerDayTargetChange(it.filter { c -> c.isDigit() }) },
                        label = { Text("Quantidade de 'Sim' por dia") },
                        leadingIcon = { NerdIcon(glyph = NerdIcons.Counter, size = 18.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                CompletionType.CUSTOM_UNIT -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customIncrementValueText,
                                onValueChange = onCustomIncrementValueChange,
                                label = { Text("Por 'Sim' (ex.: 250)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                colors = textFieldColors,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = customIncrementUnitText,
                                onValueChange = onCustomIncrementUnitChange,
                                label = { Text("Unid. (ex.: ml)") },
                                singleLine = true,
                                colors = textFieldColors,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customTargetValueText,
                                onValueChange = onCustomTargetValueChange,
                                label = { Text("Meta (ex.: 2000)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                colors = textFieldColors,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = customTargetUnitText,
                                onValueChange = onCustomTargetUnitChange,
                                label = { Text("Unid. meta (ex.: ml)") },
                                singleLine = true,
                                colors = textFieldColors,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitTimeMomentAndAlertsSection(
    useExactTime: Boolean,
    scheduledMinutes: Int,
    moments: List<Moment>,
    selectedMomentId: Long?,
    repeatHoursText: String,
    repeatMinutesText: String,
    aggressiveFullScreen: Boolean,
    vibrationEnabled: Boolean,
    onUseExactTimeChange: (Boolean) -> Unit,
    onOpenTimePicker: () -> Unit,
    onSelectMomentId: (Long?) -> Unit,
    onRepeatHoursChange: (String) -> Unit,
    onRepeatMinutesChange: (String) -> Unit,
    onAggressiveFullScreenChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit
) {
    EditorSectionCard(title = "Horário, Momento & Alertas", glyph = NerdIcons.BellAlert) {
        EditorSwitchRow(
            glyph = NerdIcons.ClockOutline,
            label = "Horário exato",
            checked = useExactTime,
            onCheckedChange = onUseExactTimeChange
        )

        AnimatedVisibility(
            visible = useExactTime,
            enter = expandVertically(tween(200, easing = FastOutSlowInEasing)) + fadeIn(tween(160)),
            exit = shrinkVertically(tween(160)) + fadeOut(tween(120))
        ) {
            TimeSelectorField(
                label = "Horário do lembrete",
                minutesOfDay = scheduledMinutes,
                onClick = onOpenTimePicker
            )
        }

        if (moments.isNotEmpty()) {
            Text(
                text = "Momento associado (opcional)",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = selectedMomentId == null,
                    onClick = { onSelectMomentId(null) },
                    label = { Text("Nenhum") }
                )
                moments.forEach { moment ->
                    FilterChip(
                        selected = selectedMomentId == moment.id,
                        onClick = { onSelectMomentId(moment.id) },
                        leadingIcon = {
                            NerdIcon(
                                glyph = momentNerdGlyph(moment.startMinutesOfDay),
                                size = 14.sp
                            )
                        },
                        label = { Text(moment.name) }
                    )
                }
            }
        }

        RepeatIntervalHourMinuteFields(
            hoursText = repeatHoursText,
            minutesText = repeatMinutesText,
            onHoursChange = onRepeatHoursChange,
            onMinutesChange = onRepeatMinutesChange,
            isOptional = true
        )

        EditorSwitchRow(
            glyph = NerdIcons.BellAlert,
            label = "Alarme em tela cheia",
            checked = aggressiveFullScreen,
            onCheckedChange = onAggressiveFullScreenChange
        )

        EditorSwitchRow(
            glyph = NerdIcons.Vibrate,
            label = "Vibração",
            checked = vibrationEnabled,
            onCheckedChange = onVibrationEnabledChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditorScreen(
    initialHabit: Habit?,
    defaultStartEpochDay: Long,
    defaultGroupId: Long? = null,
    moments: List<Moment>,
    habitGroups: List<HabitGroup>,
    onBack: () -> Unit,
    onCreateGroup: (HabitGroup, (Long) -> Unit) -> Unit,
    onSave: (Habit) -> Unit
) {
    BackHandler(onBack = onBack)

    val todayEpochDay = remember { LocalDate.now().toEpochDay() }
    var title by remember { mutableStateOf(initialHabit?.title ?: "") }
    var notes by remember { mutableStateOf(initialHabit?.notes ?: "") }
    var startEpochDay by remember { mutableLongStateOf(initialHabit?.startEpochDay ?: defaultStartEpochDay) }
    var selectedGroupId by remember { mutableStateOf(initialHabit?.groupId ?: defaultGroupId) }
    val initialGroupIcon = remember(defaultGroupId, habitGroups) {
        defaultGroupId?.let { id -> habitGroups.firstOrNull { it.id == id }?.iconGlyph }
    }
    var selectedIconGlyph by remember {
        mutableStateOf(initialHabit?.iconGlyph ?: initialGroupIcon ?: NerdIcons.Fire)
    }
    var activeDaysMask by remember { mutableIntStateOf(initialHabit?.activeDaysMask ?: DaysOfWeekMask.ALL_DAYS) }
    var completionType by remember { mutableStateOf(initialHabit?.completionType ?: CompletionType.BOOLEAN) }
    var countPerDayTargetText by remember {
        mutableStateOf(
            if (initialHabit?.completionType == CompletionType.COUNT_PER_DAY) {
                initialHabit.targetAmount.toInt().toString()
            } else "3"
        )
    }
    var customIncrementValueText by remember {
        mutableStateOf(
            if (initialHabit?.completionType == CompletionType.CUSTOM_UNIT) {
                initialHabit.incrementAmount.toString().removeSuffix(".0")
            } else "250"
        )
    }
    var customIncrementUnitText by remember {
        mutableStateOf(
            if (initialHabit?.completionType == CompletionType.CUSTOM_UNIT) {
                initialHabit.incrementUnitLabel
            } else "ml"
        )
    }
    var customTargetValueText by remember {
        mutableStateOf(
            if (initialHabit?.completionType == CompletionType.CUSTOM_UNIT) {
                initialHabit.targetAmount.toString().removeSuffix(".0")
            } else "2000"
        )
    }
    var customTargetUnitText by remember {
        mutableStateOf(
            if (initialHabit?.completionType == CompletionType.CUSTOM_UNIT) {
                initialHabit.targetUnitLabel
            } else "ml"
        )
    }

    var useExactTime by remember { mutableStateOf(initialHabit?.scheduledMinutesOfDay != null || initialHabit == null) }
    var scheduledMinutes by remember { mutableIntStateOf(initialHabit?.scheduledMinutesOfDay ?: 8 * 60) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var selectedMomentId by remember { mutableStateOf(initialHabit?.momentId) }
    val initialHabitRepeat = initialHabit?.repeatIntervalMinutes
    var repeatHoursText by remember {
        mutableStateOf(initialHabitRepeat?.let { (it / 60).takeIf { h -> h > 0 }?.toString() } ?: "")
    }
    var repeatMinutesText by remember {
        mutableStateOf(initialHabitRepeat?.let { (it % 60).takeIf { m -> m > 0 }?.toString() } ?: "")
    }
    var aggressiveFullScreen by remember { mutableStateOf(initialHabit?.aggressiveFullScreen ?: true) }
    var vibrationEnabled by remember { mutableStateOf(initialHabit?.vibrationEnabled ?: true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val selectedGroup = remember(habitGroups, selectedGroupId) {
        habitGroups.firstOrNull { it.id == selectedGroupId }
    }
    val previewColor = selectedGroup?.let { Color(it.colorArgb.toInt()) } ?: IndigoPrimary

    fun submit() {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            errorMessage = "Informe o nome do hábito."
            return
        }
        if ((activeDaysMask and DaysOfWeekMask.ALL_DAYS) == 0) {
            errorMessage = "Selecione ao menos um dia da semana."
            return
        }

        val parsedMinutes = if (useExactTime) scheduledMinutes else null

        val incAmount: Double
        val incLabel: String
        val targetAmount: Double
        val targetLabel: String

        when (completionType) {
            CompletionType.BOOLEAN -> {
                incAmount = 1.0
                incLabel = "Sim"
                targetAmount = 1.0
                targetLabel = "Sim"
            }
            CompletionType.COUNT_PER_DAY -> {
                val targetInt = countPerDayTargetText.toIntOrNull()?.takeIf { it >= 1 }
                if (targetInt == null) {
                    errorMessage = "Informe uma quantidade inteira >= 1."
                    return
                }
                incAmount = 1.0
                incLabel = "Sim"
                targetAmount = targetInt.toDouble()
                targetLabel = "Sim"
            }
            CompletionType.CUSTOM_UNIT -> {
                val incVal = customIncrementValueText.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 }
                val tarVal = customTargetValueText.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 }
                val incUnit = customIncrementUnitText.trim()
                val tarUnit = customTargetUnitText.trim()
                if (incVal == null || tarVal == null || incUnit.isEmpty() || tarUnit.isEmpty()) {
                    errorMessage = "Preencha valores (> 0) e unidades válidas."
                    return
                }
                incAmount = incVal
                incLabel = incUnit
                targetAmount = tarVal
                targetLabel = tarUnit
            }
        }

        onSave(
            Habit(
                id = initialHabit?.id ?: 0L,
                title = cleanTitle,
                notes = notes.trim().ifBlank { null },
                startEpochDay = startEpochDay,
                activeDaysMask = activeDaysMask,
                momentId = selectedMomentId,
                groupId = selectedGroupId,
                iconGlyph = selectedIconGlyph,
                scheduledMinutesOfDay = parsedMinutes,
                repeatIntervalMinutes = ((repeatHoursText.toIntOrNull() ?: 0) * 60 +
                    (repeatMinutesText.toIntOrNull() ?: 0)).takeIf { it > 0 },
                aggressiveFullScreen = aggressiveFullScreen,
                vibrationEnabled = vibrationEnabled,
                completionType = completionType,
                incrementAmount = incAmount,
                incrementUnitLabel = incLabel,
                targetAmount = targetAmount,
                targetUnitLabel = targetLabel,
                isActive = initialHabit?.isActive ?: true
            )
        )
    }

    Scaffold(
        containerColor = OledBackground,
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = OledBackground,
                        titleContentColor = TextPrimary
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = TextPrimary
                            )
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HabitBadgeIcon(
                                iconGlyph = selectedIconGlyph,
                                groupColorArgb = selectedGroup?.colorArgb,
                                size = 30.dp,
                                iconSize = 15.sp
                            )
                            Text(
                                text = if (initialHabit == null) "Novo Hábito" else "Editar Hábito",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = { submit() }) {
                            Text("Salvar", color = IndigoPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                HorizontalDivider(color = OledBorder, thickness = 1.dp)
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(OledSurface)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                HorizontalDivider(color = OledBorder, thickness = 1.dp)
                Button(
                    onClick = { submit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    NerdIcon(glyph = NerdIcons.CheckCircle, size = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (initialHabit == null) "Criar Hábito" else "Salvar Alterações",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (errorMessage != null) {
                item(key = "habit_error_banner", contentType = "error_banner") {
                    EditorErrorBanner(errorMessage = errorMessage)
                }
            }

            item(key = "habit_identity_section", contentType = "habit_identity") {
                HabitIdentitySection(
                    title = title,
                    notes = notes,
                    selectedIconGlyph = selectedIconGlyph,
                    selectedGroup = selectedGroup,
                    habitGroups = habitGroups,
                    onTitleChange = {
                        title = it
                        if (errorMessage != null) errorMessage = null
                    },
                    onNotesChange = { notes = it },
                    onOpenIconPicker = { showIconPicker = true },
                    onSelectGroup = { group ->
                        selectedGroupId = group?.id
                        if (group != null && initialHabit == null && selectedIconGlyph == NerdIcons.Fire) {
                            selectedIconGlyph = group.iconGlyph
                        }
                    },
                    onOpenCreateGroup = { showCreateGroupDialog = true }
                )
            }

            item(key = "habit_schedule_section", contentType = "habit_schedule") {
                HabitScheduleAndFrequencySection(
                    startEpochDay = startEpochDay,
                    todayEpochDay = todayEpochDay,
                    activeDaysMask = activeDaysMask,
                    onOpenDatePicker = { showDatePicker = true },
                    onSelectStartDay = { startEpochDay = it },
                    onActiveDaysMaskChange = { activeDaysMask = it }
                )
            }

            item(key = "habit_completion_section", contentType = "habit_completion") {
                HabitCompletionModeSection(
                    completionType = completionType,
                    countPerDayTargetText = countPerDayTargetText,
                    customIncrementValueText = customIncrementValueText,
                    customIncrementUnitText = customIncrementUnitText,
                    customTargetValueText = customTargetValueText,
                    customTargetUnitText = customTargetUnitText,
                    onCompletionTypeChange = { completionType = it },
                    onCountPerDayTargetChange = { countPerDayTargetText = it },
                    onCustomIncrementValueChange = { customIncrementValueText = it },
                    onCustomIncrementUnitChange = { customIncrementUnitText = it },
                    onCustomTargetValueChange = { customTargetValueText = it },
                    onCustomTargetUnitChange = { customTargetUnitText = it }
                )
            }

            item(key = "habit_alerts_section", contentType = "habit_alerts") {
                HabitTimeMomentAndAlertsSection(
                    useExactTime = useExactTime,
                    scheduledMinutes = scheduledMinutes,
                    moments = moments,
                    selectedMomentId = selectedMomentId,
                    repeatHoursText = repeatHoursText,
                    repeatMinutesText = repeatMinutesText,
                    aggressiveFullScreen = aggressiveFullScreen,
                    vibrationEnabled = vibrationEnabled,
                    onUseExactTimeChange = { useExactTime = it },
                    onOpenTimePicker = { showTimePicker = true },
                    onSelectMomentId = { id ->
                        selectedMomentId = id
                        if (id != null && initialHabit == null) useExactTime = false
                    },
                    onRepeatHoursChange = { repeatHoursText = it },
                    onRepeatMinutesChange = { repeatMinutesText = it },
                    onAggressiveFullScreenChange = { aggressiveFullScreen = it },
                    onVibrationEnabledChange = { vibrationEnabled = it }
                )
            }

            item(key = "habit_bottom_spacer", contentType = "spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showTimePicker) {
        TimePickerModalDialog(
            initialMinutesOfDay = scheduledMinutes,
            title = "Horário do Hábito",
            onDismiss = { showTimePicker = false },
            onConfirm = { picked ->
                scheduledMinutes = picked
                showTimePicker = false
            }
        )
    }

    if (showDatePicker) {
        DatePickerModalDialog(
            initialEpochDay = startEpochDay,
            onDismiss = { showDatePicker = false },
            onConfirm = { pickedDay ->
                startEpochDay = pickedDay
                showDatePicker = false
            }
        )
    }

    if (showIconPicker) {
        HabitIconPickerDialog(
            selectedGlyph = selectedIconGlyph,
            accentColor = previewColor,
            onDismiss = { showIconPicker = false },
            onSelect = { picked ->
                selectedIconGlyph = picked
                showIconPicker = false
            }
        )
    }

    if (showCreateGroupDialog) {
        HabitGroupEditorDialog(
            initialGroup = null,
            onDismiss = { showCreateGroupDialog = false },
            onSave = { newGroup ->
                showCreateGroupDialog = false
                onCreateGroup(newGroup) { createdId ->
                    selectedGroupId = createdId
                    if (selectedIconGlyph == NerdIcons.Fire) {
                        selectedIconGlyph = newGroup.iconGlyph
                    }
                }
            }
        )
    }
}
