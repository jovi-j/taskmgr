package com.taskmgr.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskmgr.ui.theme.IndigoPrimary
import com.example.taskmgr.ui.theme.OledSurface
import com.example.taskmgr.ui.theme.OledSurfaceElevated
import com.example.taskmgr.ui.theme.UbuntuSansMonoNerdFontFamily
import com.taskmgr.domain.logic.DaysOfWeekMask
import com.taskmgr.domain.logic.MomentWindowEvaluator
import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.Moment
import com.taskmgr.domain.model.ReminderMode
import java.time.DayOfWeek
import kotlin.math.roundToInt

fun formatMinutes(minutesOfDay: Int): String {
    val clamped = minutesOfDay.coerceIn(0, 1439)
    return "%02d:%02d".format(clamped / 60, clamped % 60)
}

fun isMinuteInsideWindow(minute: Int, startMinute: Int, endMinute: Int): Boolean {
    val effectiveEnd = MomentWindowEvaluator.effectiveEndMinutes(endMinute)
    return if (effectiveEnd >= startMinute) {
        minute in startMinute..effectiveEnd
    } else {
        minute >= startMinute || minute <= effectiveEnd
    }
}

fun DayOfWeek.shortPtBr(): String = when (this) {
    DayOfWeek.MONDAY -> "Seg"
    DayOfWeek.TUESDAY -> "Ter"
    DayOfWeek.WEDNESDAY -> "Qua"
    DayOfWeek.THURSDAY -> "Qui"
    DayOfWeek.FRIDAY -> "Sex"
    DayOfWeek.SATURDAY -> "Sáb"
    DayOfWeek.SUNDAY -> "Dom"
}

@Composable
private fun DialogSectionLabel(
    glyph: String,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        NerdIcon(
            glyph = glyph,
            color = MaterialTheme.colorScheme.primary,
            size = 16.sp
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DialogSwitchRow(
    glyph: String,
    label: String,
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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            NerdIcon(
                glyph = glyph,
                color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                size = 18.sp
            )
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DaysOfWeekSelector(
    mask: Int,
    onMaskChange: (Int) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DayOfWeek.entries.forEach { day ->
            val selected = DaysOfWeekMask.contains(mask, day)
            FilterChip(
                selected = selected,
                onClick = { onMaskChange(DaysOfWeekMask.toggle(mask, day)) },
                label = { Text(day.shortPtBr()) }
            )
        }
    }
}

private enum class MomentPickerTarget {
    START,
    END,
    ADD_SPECIFIC
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MomentEditorDialog(
    initialMoment: Moment?,
    onDismiss: () -> Unit,
    onSave: (Moment) -> Unit
) {
    var name by remember { mutableStateOf(initialMoment?.name ?: "") }
    var startMinutes by remember { mutableIntStateOf(initialMoment?.startMinutesOfDay ?: 8 * 60) }
    var endMinutes by remember { mutableIntStateOf(initialMoment?.endMinutesOfDay ?: 12 * 60) }
    var daysMask by remember { mutableIntStateOf(initialMoment?.daysOfWeekMask ?: DaysOfWeekMask.ALL_DAYS) }
    var reminderMode by remember { mutableStateOf(initialMoment?.reminderMode ?: ReminderMode.ONCE_AT_START) }
    val initialInterval = initialMoment?.intervalMinutes ?: 30
    var intervalHoursText by remember {
        mutableStateOf((initialInterval / 60).takeIf { it > 0 }?.toString() ?: "")
    }
    var intervalMinutesText by remember {
        mutableStateOf((initialInterval % 60).takeIf { it > 0 }?.toString() ?: "")
    }
    var specificTimes by remember {
        mutableStateOf(
            initialMoment?.specificTimesMinutesOfDay?.takeIf { it.isNotEmpty() }
                ?: listOf(8 * 60 + 30, 10 * 60 + 30)
        )
    }
    var aggressiveFullScreen by remember { mutableStateOf(initialMoment?.aggressiveFullScreen ?: true) }
    var vibrationEnabled by remember { mutableStateOf(initialMoment?.vibrationEnabled ?: true) }
    var isEnabled by remember { mutableStateOf(initialMoment?.isEnabled ?: true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pickerTarget by remember { mutableStateOf<MomentPickerTarget?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OledSurfaceElevated,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NerdIcon(
                    glyph = momentNerdGlyph(startMinutes),
                    color = MaterialTheme.colorScheme.primary,
                    size = 24.sp
                )
                Text(if (initialMoment == null) "Novo Momento" else "Editar Momento")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Nome (ex.: Manhã, Antes de dormir)") },
                    leadingIcon = { NerdIcon(glyph = NerdIcons.Pencil, size = 18.sp) },
                    singleLine = true,
                    colors = oledTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                DialogSectionLabel(glyph = NerdIcons.ClockOutline, text = "Janela de horário")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeSelectorField(
                        label = "Início",
                        minutesOfDay = startMinutes,
                        onClick = { pickerTarget = MomentPickerTarget.START; errorMessage = null },
                        modifier = Modifier.weight(1f)
                    )
                    TimeSelectorField(
                        label = "Fim",
                        minutesOfDay = endMinutes,
                        onClick = { pickerTarget = MomentPickerTarget.END; errorMessage = null },
                        modifier = Modifier.weight(1f)
                    )
                }

                val effectiveEnd = MomentWindowEvaluator.effectiveEndMinutes(endMinutes)
                if (startMinutes > effectiveEnd) {
                    Text(
                        text = "O horário de início (${formatMinutes(startMinutes)}) não pode ser posterior ao fim (${formatMinutes(endMinutes)}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (endMinutes == 0) {
                    Text(
                        text = "Fim às 00:00 considera até o final do mesmo dia (23:59).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                DialogSectionLabel(glyph = NerdIcons.Calendar, text = "Dias da semana")
                DaysOfWeekSelector(mask = daysMask, onMaskChange = { daysMask = it })

                DialogSectionLabel(glyph = NerdIcons.BellRing, text = "Política de disparo")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = reminderMode == ReminderMode.ONCE_AT_START,
                        onClick = { reminderMode = ReminderMode.ONCE_AT_START },
                        label = { Text("No início") }
                    )
                    FilterChip(
                        selected = reminderMode == ReminderMode.INTERVAL_MINUTES,
                        onClick = { reminderMode = ReminderMode.INTERVAL_MINUTES },
                        label = { Text("Repetir a cada") }
                    )
                    FilterChip(
                        selected = reminderMode == ReminderMode.SPECIFIC_TIMES,
                        onClick = { reminderMode = ReminderMode.SPECIFIC_TIMES },
                        label = { Text("Horários fixos") }
                    )
                }

                if (reminderMode == ReminderMode.INTERVAL_MINUTES) {
                    RepeatIntervalHourMinuteFields(
                        hoursText = intervalHoursText,
                        minutesText = intervalMinutesText,
                        onHoursChange = { intervalHoursText = it; errorMessage = null },
                        onMinutesChange = { intervalMinutesText = it; errorMessage = null },
                        isOptional = false
                    )
                } else if (reminderMode == ReminderMode.SPECIFIC_TIMES) {
                    DialogSectionLabel(glyph = NerdIcons.ClockCheck, text = "Horários selecionados")
                    if (specificTimes.isEmpty()) {
                        Text(
                            text = "Nenhum horário adicionado.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            specificTimes.forEach { minute ->
                                InputChip(
                                    selected = true,
                                    onClick = {
                                        specificTimes = specificTimes.filterNot { it == minute }
                                        errorMessage = null
                                    },
                                    leadingIcon = {
                                        NerdIcon(glyph = NerdIcons.ClockOutline, size = 14.sp)
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remover horário",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = formatMinutes(minute),
                                            fontFamily = UbuntuSansMonoNerdFontFamily,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                )
                            }
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            errorMessage = null
                            pickerTarget = MomentPickerTarget.ADD_SPECIFIC
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        NerdIcon(glyph = NerdIcons.PlusCircle, size = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Adicionar horário")
                    }

                    Text(
                        text = "Todos os horários fixos devem estar entre Início (${formatMinutes(startMinutes)}) e Fim (${formatMinutes(endMinutes)}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DialogSwitchRow(
                    glyph = NerdIcons.BellAlert,
                    label = "Alarme em tela cheia",
                    checked = aggressiveFullScreen,
                    onCheckedChange = { aggressiveFullScreen = it }
                )

                DialogSwitchRow(
                    glyph = NerdIcons.Vibrate,
                    label = "Vibração",
                    checked = vibrationEnabled,
                    onCheckedChange = { vibrationEnabled = it }
                )

                DialogSwitchRow(
                    glyph = NerdIcons.CheckCircle,
                    label = "Ativo",
                    checked = isEnabled,
                    onCheckedChange = { isEnabled = it }
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    if (cleanName.isEmpty()) {
                        errorMessage = "Informe o nome do momento."
                        return@Button
                    }
                    if ((daysMask and DaysOfWeekMask.ALL_DAYS) == 0) {
                        errorMessage = "Selecione ao menos um dia."
                        return@Button
                    }
                    val effectiveEnd = MomentWindowEvaluator.effectiveEndMinutes(endMinutes)
                    if (startMinutes > effectiveEnd) {
                        errorMessage = "O horário de início não pode ser posterior ao horário de fim."
                        return@Button
                    }
                    val intervalMin = if (reminderMode == ReminderMode.INTERVAL_MINUTES) {
                        val total = (intervalHoursText.toIntOrNull() ?: 0) * 60 +
                            (intervalMinutesText.toIntOrNull() ?: 0)
                        if (total <= 0) {
                            errorMessage = "Informe horas ou minutos > 0."
                            return@Button
                        }
                        total
                    } else null
                    val specTimes = if (reminderMode == ReminderMode.SPECIFIC_TIMES) {
                        if (specificTimes.isEmpty()) {
                            errorMessage = "Adicione ao menos um horário na janela."
                            return@Button
                        }
                        val outOfWindow = specificTimes.firstOrNull { min ->
                            !isMinuteInsideWindow(min, startMinutes, endMinutes)
                        }
                        if (outOfWindow != null) {
                            errorMessage = "O horário ${formatMinutes(outOfWindow)} está fora da janela (${formatMinutes(startMinutes)}–${formatMinutes(endMinutes)}). Remova ou ajuste Início/Fim."
                            return@Button
                        }
                        specificTimes.distinct().sorted()
                    } else emptyList()

                    onSave(
                        Moment(
                            id = initialMoment?.id ?: 0L,
                            name = cleanName,
                            startMinutesOfDay = startMinutes,
                            endMinutesOfDay = endMinutes,
                            daysOfWeekMask = daysMask,
                            reminderMode = reminderMode,
                            intervalMinutes = intervalMin,
                            specificTimesMinutesOfDay = specTimes,
                            aggressiveFullScreen = aggressiveFullScreen,
                            vibrationEnabled = vibrationEnabled,
                            isEnabled = isEnabled
                        )
                    )
                }
            ) {
                NerdIcon(glyph = NerdIcons.CheckCircle, size = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )

    when (pickerTarget) {
        MomentPickerTarget.START -> {
            TimePickerModalDialog(
                initialMinutesOfDay = startMinutes,
                title = "Horário de Início",
                onDismiss = { pickerTarget = null },
                onConfirm = { picked ->
                    startMinutes = picked
                    pickerTarget = null
                }
            )
        }
        MomentPickerTarget.END -> {
            TimePickerModalDialog(
                initialMinutesOfDay = endMinutes,
                title = "Horário de Fim",
                onDismiss = { pickerTarget = null },
                onConfirm = { picked ->
                    endMinutes = picked
                    pickerTarget = null
                }
            )
        }
        MomentPickerTarget.ADD_SPECIFIC -> {
            val defaultPick = specificTimes.lastOrNull() ?: startMinutes
            TimePickerModalDialog(
                initialMinutesOfDay = defaultPick,
                title = "Adicionar Horário Fixo",
                onDismiss = { pickerTarget = null },
                onConfirm = { picked ->
                    if (!isMinuteInsideWindow(picked, startMinutes, endMinutes)) {
                        errorMessage = "O horário ${formatMinutes(picked)} está fora da janela (${formatMinutes(startMinutes)}–${formatMinutes(endMinutes)})."
                    } else {
                        specificTimes = (specificTimes + picked).distinct().sorted()
                        errorMessage = null
                    }
                    pickerTarget = null
                }
            )
        }
        null -> Unit
    }
}

private fun formatCleanNumber(value: Double): String {
    return if (value % 1.0 == 0.0) value.toLong().toString()
    else "%.1f".format(value)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualHabitAmountDialog(
    habit: Habit,
    currentAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var text by remember {
        mutableStateOf(formatCleanNumber(currentAmount))
    }
    var error by remember { mutableStateOf<String?>(null) }

    val numericValue = text.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val maxSlider = habit.targetAmount.coerceAtLeast(1.0).toFloat()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OledSurfaceElevated,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NerdIcon(
                    glyph = habit.iconGlyph ?: NerdIcons.Pencil,
                    color = IndigoPrimary,
                    size = 20.sp
                )
                Text("Quanto foi feito — ${habit.title}")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NerdIcon(
                        glyph = NerdIcons.Target,
                        color = IndigoPrimary,
                        size = 16.sp
                    )
                    Text(
                        text = "Meta: ${formatCleanNumber(habit.targetAmount)} ${habit.targetUnitLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Slider(
                    value = numericValue.toFloat().coerceIn(0f, maxSlider),
                    onValueChange = { raw ->
                        val snapped = if (habit.incrementAmount > 0.0) {
                            ((raw / habit.incrementAmount).roundToInt() * habit.incrementAmount)
                                .coerceIn(0.0, habit.targetAmount)
                        } else {
                            raw.toDouble()
                        }
                        text = formatCleanNumber(snapped)
                        error = null
                    },
                    valueRange = 0f..maxSlider,
                    colors = SliderDefaults.colors(
                        thumbColor = IndigoPrimary,
                        activeTrackColor = IndigoPrimary,
                        inactiveTrackColor = OledSurface
                    )
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(0.0, 0.25, 0.5, 0.75, 1.0).forEach { ratio ->
                        val preset = habit.targetAmount * ratio
                        val label = "${(ratio * 100).toInt()}%"
                        FilterChip(
                            selected = kotlin.math.abs(numericValue - preset) < 0.01,
                            onClick = {
                                text = formatCleanNumber(preset)
                                error = null
                            },
                            label = { Text("$label (${formatCleanNumber(preset)})") }
                        )
                    }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; error = null },
                    label = { Text("Quantidade exata (${habit.targetUnitLabel})") },
                    leadingIcon = { NerdIcon(glyph = NerdIcons.Counter, size = 18.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = oledTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { text = "0" },
                        modifier = Modifier.weight(1f)
                    ) {
                        NerdIcon(glyph = NerdIcons.Close, size = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Zerar")
                    }
                    OutlinedButton(
                        onClick = { text = formatCleanNumber(habit.targetAmount) },
                        modifier = Modifier.weight(1f)
                    ) {
                        NerdIcon(glyph = NerdIcons.CheckCircle, size = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("100%")
                    }
                }
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = text.replace(',', '.').toDoubleOrNull()
                    if (value == null || value < 0.0) {
                        error = "Informe um número >= 0."
                        return@Button
                    }
                    onConfirm(value)
                }
            ) {
                NerdIcon(glyph = NerdIcons.CheckCircle, size = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Aplicar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
