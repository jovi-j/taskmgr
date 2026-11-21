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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
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
import com.taskmgr.domain.logic.DismissedSmartToken
import com.taskmgr.domain.logic.SmartHabitFrequencySpec
import com.taskmgr.domain.logic.SmartTitleParser
import com.taskmgr.domain.logic.SmartToken
import com.taskmgr.domain.model.CompletionType
import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.HabitGroup
import com.taskmgr.domain.model.Moment
import com.taskmgr.domain.model.Task
import com.taskmgr.domain.model.TaskAdvanceReminder
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
import com.taskmgr.ui.components.formatMinutes
import com.taskmgr.ui.components.momentNerdGlyph
import com.taskmgr.ui.components.oledTextFieldColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private object EditorSectionColors {
    val Date = Color(0xFF6B93C4)       // Azul aço suave
    val Time = Color(0xFFC9A66B)       // Âmbar suave
    val Advance = Color(0xFF9E84B8)    // Lavanda suave
    val Alerts = Color(0xFFC47A76)     // Coral terracota suave
    val HabitIcon = Color(0xFF68A69B)  // Verde-água suave
    val HabitGoal = Color(0xFF72A37E)  // Verde sálvia suave
    val SmartLinkText = Color(0xFF60A5FA)
    val SmartLinkBg = Color(0xFF06090F)
}

private enum class TaskEditorModal {
    DATE,
    TIME,
    ADVANCE_REMINDERS,
    URGENCY,
    ALERTS
}

private enum class HabitEditorModal {
    GROUP,
    SCHEDULE,
    COMPLETION,
    TIME,
    ALERTS
}

private val CompactDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")

private fun formatCompactEpochDay(epochDay: Long, todayEpochDay: Long): String = when (epochDay) {
    todayEpochDay -> "Hoje"
    todayEpochDay + 1L -> "Amanhã"
    todayEpochDay - 1L -> "Ontem"
    else -> LocalDate.ofEpochDay(epochDay).format(CompactDateFormatter)
}

private fun formatRepeatShort(hoursText: String, minutesText: String): String? {
    val h = hoursText.toIntOrNull() ?: 0
    val m = minutesText.toIntOrNull() ?: 0
    if (h <= 0 && m <= 0) return null
    return buildString {
        if (h > 0) append("${h}h")
        if (h > 0 && m > 0) append(" ")
        if (m > 0) append("${m}m")
    }
}

private fun formatDaysMaskCompact(mask: Int): String {
    val clean = mask and DaysOfWeekMask.ALL_DAYS
    if (clean == DaysOfWeekMask.ALL_DAYS) return "Todos os dias"
    val weekdaysMask = DaysOfWeekMask.fromDays(
        setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )
    )
    val weekendMask = DaysOfWeekMask.fromDays(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
    if (clean == weekdaysMask) return "Seg–Sex"
    if (clean == weekendMask) return "Fim de semana"
    val labels = listOf(
        DayOfWeek.MONDAY to "Seg",
        DayOfWeek.TUESDAY to "Ter",
        DayOfWeek.WEDNESDAY to "Qua",
        DayOfWeek.THURSDAY to "Qui",
        DayOfWeek.FRIDAY to "Sex",
        DayOfWeek.SATURDAY to "Sáb",
        DayOfWeek.SUNDAY to "Dom"
    )
    return labels.filter { (day, _) -> DaysOfWeekMask.contains(clean, day) }.joinToString(", ") { it.second }
}

@Composable
private fun EditorPrimaryNotesCard(
    titleValue: TextFieldValue,
    smartTokens: List<SmartToken>,
    notes: String,
    onTitleValueChange: (TextFieldValue) -> Unit,
    onNotesChange: (String) -> Unit
) {
    val titleStyle = MaterialTheme.typography.titleLarge.copy(
        color = TextPrimary,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    )
    val notesStyle = MaterialTheme.typography.bodyLarge.copy(
        color = TextPrimary,
        fontSize = 15.sp,
        lineHeight = 22.sp
    )

    val smartVisualTransformation = remember(smartTokens) {
        VisualTransformation { rawAnnotated ->
            val raw = rawAnnotated.text
            if (smartTokens.isEmpty() || raw.isEmpty()) {
                TransformedText(rawAnnotated, OffsetMapping.Identity)
            } else {
                val styled = buildAnnotatedString {
                    append(raw)
                    for (token in smartTokens) {
                        val s = token.startIndex.coerceIn(0, raw.length)
                        val e = token.endIndex.coerceIn(s, raw.length)
                        if (s < e) {
                            addStyle(
                                style = SpanStyle(
                                    color = EditorSectionColors.SmartLinkText,
                                    background = EditorSectionColors.SmartLinkBg,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline
                                ),
                                start = s,
                                end = e
                            )
                        }
                    }
                }
                TransformedText(styled, OffsetMapping.Identity)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OledSurface),
        border = BorderStroke(1.dp, OledBorder.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = titleValue,
                onValueChange = onTitleValueChange,
                textStyle = titleStyle,
                maxLines = 3,
                visualTransformation = smartVisualTransformation,
                cursorBrush = SolidColor(IndigoPrimary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        if (titleValue.text.isEmpty()) {
                            Text(
                                text = "Título",
                                style = titleStyle,
                                color = TextSecondary.copy(alpha = 0.55f)
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = OledBorder.copy(alpha = 0.55f)
            )

            BasicTextField(
                value = notes,
                onValueChange = onNotesChange,
                textStyle = notesStyle,
                maxLines = 8,
                cursorBrush = SolidColor(IndigoPrimary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 112.dp)
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        if (notes.isEmpty()) {
                            Text(
                                text = "Detalhes",
                                style = notesStyle,
                                color = TextSecondary.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EditorSectionIconButton(
    glyph: String,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (isActive) activeColor else TextSecondary
    val containerColor = if (isActive) activeColor.copy(alpha = 0.16f) else OledSurface
    val borderColor = if (isActive) activeColor.copy(alpha = 0.72f) else OledBorder

    Column(
        modifier = modifier.semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(14.dp),
            color = containerColor,
            border = BorderStroke(if (isActive) 1.5.dp else 1.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                NerdIcon(
                    glyph = glyph,
                    color = tint,
                    size = 20.sp
                )
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 6.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(activeColor)
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EditorSummaryChip(
    glyph: String,
    text: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = accentColor.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NerdIcon(glyph = glyph, color = accentColor, size = 13.sp)
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorSectionModalSheet(
    title: String,
    glyph: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OledSurface,
        contentColor = TextPrimary,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = OledBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
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
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        NerdIcon(glyph = glyph, color = accentColor, size = 17.sp)
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Concluir",
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            HorizontalDivider(
                color = OledBorder,
                thickness = 1.dp,
                modifier = Modifier.padding(top = 6.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
            Spacer(modifier = Modifier.height(8.dp))
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
private fun TaskUrgencyModalContent(
    selectedUrgency: TaskUrgency,
    isUrgencySelected: Boolean,
    isUnscheduled: Boolean,
    onSelectUrgency: (TaskUrgency) -> Unit
) {
    val options = remember { TaskUrgency.entries.sortedBy { it.sortOrder } }
    Text(
        text = if (isUnscheduled && selectedUrgency == TaskUrgency.LOW) {
            "Tarefas sem data e sem horário ficam em Urgência Baixa (Caixa de entrada) por padrão, ou escolha um quadrante abaixo."
        } else {
            "Selecione o quadrante na Matriz de Eisenhower para esta tarefa."
        },
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val isSelected = selectedUrgency == option && (isUrgencySelected || option != TaskUrgency.LOW)
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
                            glyph = NerdIcons.Flag,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskDateAndFrequencyModalContent(
    useDate: Boolean,
    startEpochDay: Long,
    hasEndDate: Boolean,
    endEpochDay: Long,
    activeDaysMask: Int,
    todayEpochDay: Long,
    onUseDateChange: (Boolean) -> Unit,
    onOpenStartDatePicker: () -> Unit,
    onSelectStartDay: (Long) -> Unit,
    onHasEndDateChange: (Boolean) -> Unit,
    onOpenEndDatePicker: () -> Unit,
    onActiveDaysMaskChange: (Int) -> Unit
) {
    EditorSwitchRow(
        glyph = NerdIcons.Calendar,
        label = "Definir data",
        subtitle = if (useDate) "Agendada a partir da data de início" else "Sem data (vai para a Caixa de entrada)",
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
            selected = useDate && startEpochDay == todayEpochDay + 1L,
            onClick = {
                onUseDateChange(true)
                onSelectStartDay(todayEpochDay + 1L)
            },
            label = { Text("Amanhã") }
        )
        FilterChip(
            selected = useDate && startEpochDay == todayEpochDay + 2L,
            onClick = {
                onUseDateChange(true)
                onSelectStartDay(todayEpochDay + 2L)
            },
            label = { Text("Depois de amanhã") }
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
                onClick = onOpenStartDatePicker
            )

            Text(
                text = "Dias da semana em que a tarefa ocorre",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            DaysOfWeekSelector(
                mask = activeDaysMask,
                onMaskChange = onActiveDaysMaskChange
            )

            HorizontalDivider(color = OledBorder, thickness = 1.dp)

            EditorSwitchRow(
                glyph = NerdIcons.CalendarCheck,
                label = "Definir data fim",
                subtitle = if (hasEndDate) {
                    "Termina após a data selecionada"
                } else {
                    "Sem data fim: termina ao concluir a tarefa na notificação"
                },
                checked = hasEndDate,
                onCheckedChange = onHasEndDateChange
            )

            AnimatedVisibility(
                visible = hasEndDate,
                enter = expandVertically(tween(200, easing = FastOutSlowInEasing)) + fadeIn(tween(160)),
                exit = shrinkVertically(tween(160)) + fadeOut(tween(120))
            ) {
                DateSelectorField(
                    label = "Data fim (até)",
                    epochDay = maxOf(startEpochDay, endEpochDay),
                    onClick = onOpenEndDatePicker
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeAndMomentModalContent(
    useExactTime: Boolean,
    scheduledMinutes: Int,
    moments: List<Moment>,
    selectedMomentId: Long?,
    repeatHoursText: String,
    repeatMinutesText: String,
    exactTimeSubtitle: String? = null,
    onUseExactTimeChange: (Boolean) -> Unit,
    onOpenTimePicker: () -> Unit,
    onSelectMomentId: (Long?) -> Unit,
    onRepeatHoursChange: (String) -> Unit,
    onRepeatMinutesChange: (String) -> Unit
) {
    EditorSwitchRow(
        glyph = NerdIcons.ClockOutline,
        label = "Horário exato",
        subtitle = exactTimeSubtitle,
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskAdvanceRemindersModalContent(
    advanceReminders: List<TaskAdvanceReminder>,
    onAddAdvanceReminder: (TaskAdvanceReminder) -> Unit,
    onRemoveAdvanceReminder: (Int) -> Unit
) {
    val textFieldColors = oledTextFieldColors()
    var advDaysText by remember { mutableStateOf("") }
    var advHoursText by remember { mutableStateOf("") }
    var advMinutesText by remember { mutableStateOf("") }
    var advRepeatHoursText by remember { mutableStateOf("") }
    var advRepeatMinutesText by remember { mutableStateOf("") }

    Text(
        text = "Adicione quantos lembretes antecipados quiser (dias, horas, minutos antes e frequência).",
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterChip(
            selected = false,
            onClick = { onAddAdvanceReminder(TaskAdvanceReminder(daysBefore = 1)) },
            leadingIcon = { NerdIcon(glyph = NerdIcons.Plus, color = EditorSectionColors.Advance, size = 13.sp) },
            label = { Text("1 dia antes") }
        )
        FilterChip(
            selected = false,
            onClick = { onAddAdvanceReminder(TaskAdvanceReminder(hoursBefore = 2)) },
            leadingIcon = { NerdIcon(glyph = NerdIcons.Plus, color = EditorSectionColors.Advance, size = 13.sp) },
            label = { Text("2 horas antes") }
        )
        FilterChip(
            selected = false,
            onClick = { onAddAdvanceReminder(TaskAdvanceReminder(minutesBefore = 30)) },
            leadingIcon = { NerdIcon(glyph = NerdIcons.Plus, color = EditorSectionColors.Advance, size = 13.sp) },
            label = { Text("30 min antes") }
        )
    }

    if (advanceReminders.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            advanceReminders.forEachIndexed { index, item ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = OledSurfaceElevated,
                    border = BorderStroke(1.dp, EditorSectionColors.Advance.copy(alpha = 0.45f)),
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
                            NerdIcon(glyph = NerdIcons.Alarm, color = EditorSectionColors.Advance, size = 16.sp)
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
                            TaskAdvanceReminder(
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

@Composable
private fun AlertsModalContent(
    aggressiveFullScreen: Boolean,
    vibrationEnabled: Boolean,
    fullScreenSubtitle: String? = null,
    onAggressiveFullScreenChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit
) {
    EditorSwitchRow(
        glyph = NerdIcons.BellAlert,
        label = "Alarme em tela cheia",
        subtitle = fullScreenSubtitle,
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val defaultStartDay = remember(defaultEpochDay, todayEpochDay) {
        maxOf(defaultEpochDay, todayEpochDay)
    }

    var titleFieldValue by remember(initialTask?.id) {
        val initText = initialTask?.title ?: ""
        mutableStateOf(TextFieldValue(text = initText, selection = TextRange(initText.length)))
    }
    var dismissedTokens by remember(initialTask?.id) {
        mutableStateOf<List<DismissedSmartToken>>(emptyList())
    }
    var notes by remember(initialTask?.id) { mutableStateOf(initialTask?.notes ?: "") }

    var manualUrgency by remember(initialTask?.id, defaultUrgency) {
        mutableStateOf(initialTask?.urgency ?: defaultUrgency)
    }
    var manualUrgencyExplicitlySelected by remember(initialTask?.id, defaultUrgency) {
        mutableStateOf(
            defaultUrgency != TaskUrgency.LOW ||
                (initialTask != null && initialTask.urgency != TaskUrgency.LOW)
        )
    }
    var manualUseDate by remember(initialTask?.id) {
        mutableStateOf(initialTask?.hasDate ?: true)
    }
    var manualStartEpochDay by remember(initialTask?.id) {
        mutableLongStateOf(
            if (initialTask != null && initialTask.hasDate) initialTask.startEpochDay
            else defaultStartDay
        )
    }
    var manualHasEndDate by remember(initialTask?.id) {
        mutableStateOf(
            initialTask != null &&
                initialTask.hasDate &&
                (initialTask.hasEndDate ||
                    (initialTask.endEpochDay > initialTask.startEpochDay &&
                        initialTask.endEpochDay < Task.OPEN_ENDED_EPOCH_DAY))
        )
    }
    var manualEndEpochDay by remember(initialTask?.id) {
        mutableLongStateOf(
            if (initialTask != null &&
                initialTask.hasDate &&
                initialTask.endEpochDay in initialTask.startEpochDay until Task.OPEN_ENDED_EPOCH_DAY
            ) {
                initialTask.endEpochDay
            } else {
                defaultStartDay
            }
        )
    }
    var manualActiveDaysMask by remember(initialTask?.id) {
        mutableIntStateOf(initialTask?.activeDaysMask ?: DaysOfWeekMask.ALL_DAYS)
    }
    var manualUseExactTime by remember(initialTask?.id) {
        mutableStateOf(initialTask?.scheduledMinutesOfDay != null)
    }
    var manualScheduledMinutes by remember(initialTask?.id) {
        mutableIntStateOf(initialTask?.scheduledMinutesOfDay ?: 9 * 60)
    }

    var showTimePicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var activeModal by remember { mutableStateOf<TaskEditorModal?>(null) }
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

    val smartParseResult = remember(titleFieldValue.text, todayEpochDay, dismissedTokens) {
        SmartTitleParser.parseTaskTitle(
            text = titleFieldValue.text,
            todayEpochDay = todayEpochDay,
            dismissedTokens = dismissedTokens
        )
    }

    val useDate = if (
        smartParseResult.startEpochDay != null ||
        smartParseResult.endEpochDay != null ||
        smartParseResult.weekdaysMask != null
    ) {
        true
    } else {
        manualUseDate
    }
    val startEpochDay = smartParseResult.startEpochDay ?: manualStartEpochDay
    val hasEndDate = if (smartParseResult.endEpochDay != null) true else manualHasEndDate
    val endEpochDay = maxOf(startEpochDay, smartParseResult.endEpochDay ?: manualEndEpochDay)
    val activeDaysMask = smartParseResult.weekdaysMask ?: manualActiveDaysMask
    val useExactTime = if (smartParseResult.scheduledMinutesOfDay != null) true else manualUseExactTime
    val scheduledMinutes = smartParseResult.scheduledMinutesOfDay ?: manualScheduledMinutes
    val selectedUrgency = smartParseResult.urgency ?: manualUrgency
    val urgencyExplicitlySelected = if (smartParseResult.urgency != null) true else manualUrgencyExplicitlySelected

    val isUnscheduled = !useDate && !useExactTime
    val selectedMoment = remember(moments, selectedMomentId) {
        moments.firstOrNull { it.id == selectedMomentId }
    }
    val repeatShort = remember(repeatHoursText, repeatMinutesText) {
        formatRepeatShort(repeatHoursText, repeatMinutesText)
    }

    val isDateActive = !useDate ||
        startEpochDay != defaultStartDay ||
        hasEndDate ||
        activeDaysMask != DaysOfWeekMask.ALL_DAYS ||
        smartParseResult.startEpochDay != null
    val isTimeActive = useExactTime || selectedMomentId != null || repeatShort != null
    val isAdvanceActive = advanceReminders.isNotEmpty()
    val isUrgencyActive = urgencyExplicitlySelected || selectedUrgency != TaskUrgency.LOW
    val isAlertsActive = !aggressiveFullScreen || !vibrationEnabled
    val urgencyColor = Color(selectedUrgency.colorArgb.toInt())

    fun submit() {
        val cleanTitle = smartParseResult.cleanTitle.ifEmpty { titleFieldValue.text.trim() }
        if (cleanTitle.isEmpty()) {
            errorMessage = "Informe o título da tarefa."
            return
        }
        if (useDate && (activeDaysMask and DaysOfWeekMask.ALL_DAYS) == 0) {
            errorMessage = "Selecione ao menos um dia da semana."
            activeModal = TaskEditorModal.DATE
            return
        }
        val parsedMinutes = if (useExactTime) scheduledMinutes else null
        val totalRepeatMin = (repeatHoursText.toIntOrNull() ?: 0) * 60 + (repeatMinutesText.toIntOrNull() ?: 0)
        val repeatInterval = totalRepeatMin.takeIf { it > 0 }
        val finalStartDay = if (useDate) startEpochDay else Task.UNSCHEDULED_EPOCH_DAY
        val finalEndDay = when {
            !useDate -> Task.UNSCHEDULED_EPOCH_DAY
            hasEndDate -> maxOf(startEpochDay, endEpochDay)
            activeDaysMask != DaysOfWeekMask.ALL_DAYS -> Task.OPEN_ENDED_EPOCH_DAY
            else -> startEpochDay
        }

        onSave(
            Task(
                id = initialTask?.id ?: 0L,
                title = cleanTitle,
                notes = notes.trim().ifBlank { null },
                startEpochDay = finalStartDay,
                endEpochDay = finalEndDay,
                activeDaysMask = if (useDate) activeDaysMask else DaysOfWeekMask.ALL_DAYS,
                hasEndDate = useDate && hasEndDate,
                scheduledMinutesOfDay = parsedMinutes,
                momentId = selectedMomentId,
                repeatIntervalMinutes = repeatInterval,
                aggressiveFullScreen = aggressiveFullScreen,
                vibrationEnabled = vibrationEnabled,
                isCompleted = initialTask?.isCompleted ?: false,
                completedAtEpochMillis = initialTask?.completedAtEpochMillis,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (errorMessage != null) {
                EditorErrorBanner(errorMessage = errorMessage)
            }

            EditorPrimaryNotesCard(
                titleValue = titleFieldValue,
                smartTokens = smartParseResult.tokens,
                notes = notes,
                onTitleValueChange = { incoming ->
                    val outcome = SmartTitleParser.processTextEdit(
                        oldText = titleFieldValue.text,
                        newText = incoming.text,
                        activeTokens = smartParseResult.tokens,
                        dismissedTokens = dismissedTokens
                    )
                    dismissedTokens = outcome.dismissedTokens
                    titleFieldValue = if (outcome.wasTokenDismissed && outcome.cursorOffset != null) {
                        TextFieldValue(
                            text = outcome.text,
                            selection = TextRange(outcome.cursorOffset)
                        )
                    } else {
                        incoming
                    }
                    if (errorMessage != null) errorMessage = null
                },
                onNotesChange = { notes = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EditorSectionIconButton(
                    glyph = NerdIcons.Calendar,
                    label = "Data",
                    isActive = isDateActive,
                    activeColor = EditorSectionColors.Date,
                    onClick = { activeModal = TaskEditorModal.DATE },
                    modifier = Modifier.weight(1f)
                )
                EditorSectionIconButton(
                    glyph = NerdIcons.ClockOutline,
                    label = "Horário",
                    isActive = isTimeActive,
                    activeColor = EditorSectionColors.Time,
                    onClick = { activeModal = TaskEditorModal.TIME },
                    modifier = Modifier.weight(1f)
                )
                EditorSectionIconButton(
                    glyph = NerdIcons.Alarm,
                    label = "Antes",
                    isActive = isAdvanceActive,
                    activeColor = EditorSectionColors.Advance,
                    onClick = { activeModal = TaskEditorModal.ADVANCE_REMINDERS },
                    modifier = Modifier.weight(1f)
                )
                EditorSectionIconButton(
                    glyph = NerdIcons.Flag,
                    label = "Matriz",
                    isActive = isUrgencyActive,
                    activeColor = urgencyColor,
                    onClick = { activeModal = TaskEditorModal.URGENCY },
                    modifier = Modifier.weight(1f)
                )
                EditorSectionIconButton(
                    glyph = NerdIcons.BellAlert,
                    label = "Alertas",
                    isActive = isAlertsActive,
                    activeColor = EditorSectionColors.Alerts,
                    onClick = { activeModal = TaskEditorModal.ALERTS },
                    modifier = Modifier.weight(1f)
                )
            }

            val hasAnySummary = isDateActive || isTimeActive || isAdvanceActive || isUrgencyActive || isAlertsActive
            if (hasAnySummary) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isDateActive) {
                        val dateSummary = if (!useDate) {
                            "Sem data"
                        } else {
                            buildList {
                                add(formatCompactEpochDay(startEpochDay, todayEpochDay))
                                if (hasEndDate) {
                                    add("até ${formatCompactEpochDay(endEpochDay, todayEpochDay)}")
                                }
                                if (activeDaysMask != DaysOfWeekMask.ALL_DAYS) {
                                    add(formatDaysMaskCompact(activeDaysMask))
                                }
                            }.joinToString(" • ")
                        }
                        EditorSummaryChip(
                            glyph = NerdIcons.Calendar,
                            text = dateSummary,
                            accentColor = EditorSectionColors.Date,
                            onClick = { activeModal = TaskEditorModal.DATE }
                        )
                    }
                    if (isTimeActive) {
                        val parts = buildList {
                            if (useExactTime) add(formatMinutes(scheduledMinutes))
                            selectedMoment?.let { add(it.name) }
                            repeatShort?.let { add("↻ $it") }
                        }
                        EditorSummaryChip(
                            glyph = NerdIcons.ClockOutline,
                            text = parts.joinToString(" • "),
                            accentColor = EditorSectionColors.Time,
                            onClick = { activeModal = TaskEditorModal.TIME }
                        )
                    }
                    if (isAdvanceActive) {
                        val count = advanceReminders.size
                        EditorSummaryChip(
                            glyph = NerdIcons.Alarm,
                            text = if (count == 1) advanceReminders.first().formatLabelPtBr() else "$count lembretes antes",
                            accentColor = EditorSectionColors.Advance,
                            onClick = { activeModal = TaskEditorModal.ADVANCE_REMINDERS }
                        )
                    }
                    if (isUrgencyActive) {
                        EditorSummaryChip(
                            glyph = NerdIcons.Flag,
                            text = selectedUrgency.matrixTitlePtBr,
                            accentColor = urgencyColor,
                            onClick = { activeModal = TaskEditorModal.URGENCY }
                        )
                    }
                    if (isAlertsActive) {
                        val alertLabel = when {
                            !aggressiveFullScreen && !vibrationEnabled -> "Sem tela cheia • Sem vibração"
                            !aggressiveFullScreen -> "Sem tela cheia"
                            else -> "Sem vibração"
                        }
                        EditorSummaryChip(
                            glyph = NerdIcons.BellAlert,
                            text = alertLabel,
                            accentColor = EditorSectionColors.Alerts,
                            onClick = { activeModal = TaskEditorModal.ALERTS }
                        )
                    }
                }
            }
        }
    }

    when (activeModal) {
        TaskEditorModal.DATE -> {
            EditorSectionModalSheet(
                title = "Data e Frequência",
                glyph = NerdIcons.Calendar,
                accentColor = EditorSectionColors.Date,
                onDismiss = { activeModal = null }
            ) {
                TaskDateAndFrequencyModalContent(
                    useDate = useDate,
                    startEpochDay = startEpochDay,
                    hasEndDate = hasEndDate,
                    endEpochDay = endEpochDay,
                    activeDaysMask = activeDaysMask,
                    todayEpochDay = todayEpochDay,
                    onUseDateChange = { enabled ->
                        manualUseDate = enabled
                        if (!enabled && !useExactTime && !urgencyExplicitlySelected) {
                            manualUrgency = TaskUrgency.LOW
                        }
                    },
                    onOpenStartDatePicker = {
                        manualUseDate = true
                        showStartDatePicker = true
                    },
                    onSelectStartDay = { picked ->
                        manualStartEpochDay = picked
                        if (manualEndEpochDay < picked) {
                            manualEndEpochDay = picked
                        }
                    },
                    onHasEndDateChange = { enabled ->
                        manualHasEndDate = enabled
                        if (enabled && manualEndEpochDay < startEpochDay) {
                            manualEndEpochDay = startEpochDay
                        }
                    },
                    onOpenEndDatePicker = {
                        manualUseDate = true
                        manualHasEndDate = true
                        showEndDatePicker = true
                    },
                    onActiveDaysMaskChange = { mask ->
                        manualActiveDaysMask = mask
                        if (errorMessage != null) errorMessage = null
                    }
                )
            }
        }

        TaskEditorModal.TIME -> {
            EditorSectionModalSheet(
                title = "Horário & Momento",
                glyph = NerdIcons.ClockOutline,
                accentColor = EditorSectionColors.Time,
                onDismiss = { activeModal = null }
            ) {
                TimeAndMomentModalContent(
                    useExactTime = useExactTime,
                    scheduledMinutes = scheduledMinutes,
                    moments = moments,
                    selectedMomentId = selectedMomentId,
                    repeatHoursText = repeatHoursText,
                    repeatMinutesText = repeatMinutesText,
                    exactTimeSubtitle = "Dispara no minuto configurado (opcional)",
                    onUseExactTimeChange = { enabled ->
                        manualUseExactTime = enabled
                        if (!useDate && !enabled && !urgencyExplicitlySelected) {
                            manualUrgency = TaskUrgency.LOW
                        }
                    },
                    onOpenTimePicker = { showTimePicker = true },
                    onSelectMomentId = { id ->
                        selectedMomentId = id
                        if (id != null && initialTask == null && smartParseResult.scheduledMinutesOfDay == null) {
                            manualUseExactTime = false
                        }
                    },
                    onRepeatHoursChange = { repeatHoursText = it },
                    onRepeatMinutesChange = { repeatMinutesText = it }
                )
            }
        }

        TaskEditorModal.ADVANCE_REMINDERS -> {
            EditorSectionModalSheet(
                title = "Lembretes Antecipados",
                glyph = NerdIcons.Alarm,
                accentColor = EditorSectionColors.Advance,
                onDismiss = { activeModal = null }
            ) {
                TaskAdvanceRemindersModalContent(
                    advanceReminders = advanceReminders,
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
        }

        TaskEditorModal.URGENCY -> {
            EditorSectionModalSheet(
                title = "Matriz de Eisenhower",
                glyph = NerdIcons.Flag,
                accentColor = if (isUrgencyActive) urgencyColor else IndigoPrimary,
                onDismiss = { activeModal = null }
            ) {
                TaskUrgencyModalContent(
                    selectedUrgency = selectedUrgency,
                    isUrgencySelected = isUrgencyActive,
                    isUnscheduled = isUnscheduled,
                    onSelectUrgency = { picked ->
                        manualUrgency = picked
                        manualUrgencyExplicitlySelected = true
                        activeModal = null
                    }
                )
            }
        }

        TaskEditorModal.ALERTS -> {
            EditorSectionModalSheet(
                title = "Alertas",
                glyph = NerdIcons.BellAlert,
                accentColor = EditorSectionColors.Alerts,
                onDismiss = { activeModal = null }
            ) {
                AlertsModalContent(
                    aggressiveFullScreen = aggressiveFullScreen,
                    vibrationEnabled = vibrationEnabled,
                    fullScreenSubtitle = "Abre sobre a tela bloqueada ou app atual (apenas com a opção 'Sim')",
                    onAggressiveFullScreenChange = { aggressiveFullScreen = it },
                    onVibrationEnabledChange = { vibrationEnabled = it }
                )
            }
        }

        null -> Unit
    }

    if (showTimePicker) {
        TimePickerModalDialog(
            initialMinutesOfDay = scheduledMinutes,
            title = "Horário da Tarefa",
            onDismiss = { showTimePicker = false },
            onConfirm = { picked ->
                manualUseExactTime = true
                manualScheduledMinutes = picked
                showTimePicker = false
            }
        )
    }

    if (showStartDatePicker) {
        DatePickerModalDialog(
            initialEpochDay = startEpochDay,
            onDismiss = { showStartDatePicker = false },
            onConfirm = { pickedDay ->
                manualStartEpochDay = pickedDay
                if (manualEndEpochDay < pickedDay) {
                    manualEndEpochDay = pickedDay
                }
                showStartDatePicker = false
            }
        )
    }

    if (showEndDatePicker) {
        DatePickerModalDialog(
            initialEpochDay = maxOf(startEpochDay, endEpochDay),
            onDismiss = { showEndDatePicker = false },
            onConfirm = { pickedDay ->
                manualHasEndDate = true
                manualEndEpochDay = maxOf(startEpochDay, pickedDay)
                showEndDatePicker = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitGroupModalContent(
    selectedGroup: HabitGroup?,
    habitGroups: List<HabitGroup>,
    onSelectGroup: (HabitGroup?) -> Unit,
    onOpenCreateGroup: () -> Unit
) {
    Text(
        text = "Escolha um grupo para organizar o hábito e definir a cor do ícone.",
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitScheduleAndFrequencyModalContent(
    startEpochDay: Long,
    todayEpochDay: Long,
    activeDaysMask: Int,
    onOpenDatePicker: () -> Unit,
    onSelectStartDay: (Long) -> Unit,
    onActiveDaysMaskChange: (Int) -> Unit
) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitCompletionModeModalContent(
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val defaultTodoODiaMomentId = remember(moments) {
        SmartTitleParser.findDefaultHabitMomentId(moments)
    }

    var titleFieldValue by remember(initialHabit?.id) {
        val initText = initialHabit?.title ?: ""
        mutableStateOf(TextFieldValue(text = initText, selection = TextRange(initText.length)))
    }
    var dismissedTokens by remember(initialHabit?.id) {
        mutableStateOf<List<DismissedSmartToken>>(emptyList())
    }
    var notes by remember(initialHabit?.id) { mutableStateOf(initialHabit?.notes ?: "") }
    var manualStartEpochDay by remember(initialHabit?.id) {
        mutableLongStateOf(initialHabit?.startEpochDay ?: defaultStartEpochDay)
    }
    var manualGroupId by remember(initialHabit?.id, defaultGroupId) {
        mutableStateOf(initialHabit?.groupId ?: defaultGroupId)
    }
    val initialGroupIcon = remember(defaultGroupId, habitGroups) {
        defaultGroupId?.let { id -> habitGroups.firstOrNull { it.id == id }?.iconGlyph }
    }
    var selectedIconGlyph by remember(initialHabit?.id) {
        mutableStateOf(initialHabit?.iconGlyph ?: initialGroupIcon ?: NerdIcons.Fire)
    }
    var iconExplicitlySelected by remember(initialHabit?.id) {
        mutableStateOf(
            (initialHabit != null && initialHabit.iconGlyph != NerdIcons.Fire) ||
                (initialGroupIcon != null && initialGroupIcon != NerdIcons.Fire)
        )
    }
    var activeDaysMask by remember(initialHabit?.id) {
        mutableIntStateOf(initialHabit?.activeDaysMask ?: DaysOfWeekMask.ALL_DAYS)
    }
    var manualCompletionType by remember(initialHabit?.id) {
        mutableStateOf(initialHabit?.completionType ?: CompletionType.BOOLEAN)
    }
    var manualCountPerDayTargetText by remember(initialHabit?.id) {
        mutableStateOf(
            if (initialHabit?.completionType == CompletionType.COUNT_PER_DAY) {
                initialHabit.targetAmount.toInt().toString()
            } else "3"
        )
    }
    var manualCustomIncrementValueText by remember(initialHabit?.id) {
        mutableStateOf(
            if (initialHabit?.completionType == CompletionType.CUSTOM_UNIT) {
                initialHabit.incrementAmount.toString().removeSuffix(".0")
            } else "250"
        )
    }
    var manualCustomIncrementUnitText by remember(initialHabit?.id) {
        mutableStateOf(
            if (initialHabit?.completionType == CompletionType.CUSTOM_UNIT) {
                initialHabit.incrementUnitLabel
            } else "ml"
        )
    }
    var manualCustomTargetValueText by remember(initialHabit?.id) {
        mutableStateOf(
            if (initialHabit?.completionType == CompletionType.CUSTOM_UNIT) {
                initialHabit.targetAmount.toString().removeSuffix(".0")
            } else "2000"
        )
    }
    var manualCustomTargetUnitText by remember(initialHabit?.id) {
        mutableStateOf(
            if (initialHabit?.completionType == CompletionType.CUSTOM_UNIT) {
                initialHabit.targetUnitLabel
            } else "ml"
        )
    }

    var manualUseExactTime by remember(initialHabit?.id) {
        mutableStateOf(initialHabit?.scheduledMinutesOfDay != null)
    }
    var manualScheduledMinutes by remember(initialHabit?.id) {
        mutableIntStateOf(initialHabit?.scheduledMinutesOfDay ?: 8 * 60)
    }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var activeModal by remember { mutableStateOf<HabitEditorModal?>(null) }
    var manualMomentId by remember(initialHabit?.id) { mutableStateOf(initialHabit?.momentId) }
    var manualMomentExplicitlyChanged by remember(initialHabit?.id) {
        mutableStateOf(initialHabit != null)
    }
    val initialHabitRepeat = initialHabit?.repeatIntervalMinutes
    var repeatHoursText by remember(initialHabit?.id) {
        mutableStateOf(initialHabitRepeat?.let { (it / 60).takeIf { h -> h > 0 }?.toString() } ?: "")
    }
    var repeatMinutesText by remember(initialHabit?.id) {
        mutableStateOf(initialHabitRepeat?.let { (it % 60).takeIf { m -> m > 0 }?.toString() } ?: "")
    }
    var aggressiveFullScreen by remember(initialHabit?.id) { mutableStateOf(initialHabit?.aggressiveFullScreen ?: true) }
    var vibrationEnabled by remember(initialHabit?.id) { mutableStateOf(initialHabit?.vibrationEnabled ?: true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val smartParseResult = remember(
        titleFieldValue.text,
        todayEpochDay,
        moments,
        habitGroups,
        dismissedTokens
    ) {
        SmartTitleParser.parseHabitTitle(
            text = titleFieldValue.text,
            todayEpochDay = todayEpochDay,
            moments = moments,
            habitGroups = habitGroups,
            dismissedTokens = dismissedTokens
        )
    }

    val startEpochDay = smartParseResult.startEpochDay ?: manualStartEpochDay
    val selectedGroupId = smartParseResult.groupId ?: manualGroupId
    val selectedMomentId = smartParseResult.momentId ?: if (manualMomentExplicitlyChanged) {
        manualMomentId
    } else {
        manualMomentId ?: defaultTodoODiaMomentId
    }
    val useExactTime = if (smartParseResult.scheduledMinutesOfDay != null) true else manualUseExactTime
    val scheduledMinutes = smartParseResult.scheduledMinutesOfDay ?: manualScheduledMinutes

    val completionType = when (smartParseResult.frequencySpec) {
        is SmartHabitFrequencySpec.CountPerDay -> CompletionType.COUNT_PER_DAY
        is SmartHabitFrequencySpec.CustomUnit -> CompletionType.CUSTOM_UNIT
        null -> manualCompletionType
    }
    val countPerDayTargetText = (smartParseResult.frequencySpec as? SmartHabitFrequencySpec.CountPerDay)
        ?.count
        ?.toString()
        ?: manualCountPerDayTargetText
    val customIncrementValueText = (smartParseResult.frequencySpec as? SmartHabitFrequencySpec.CustomUnit)
        ?.incrementValueText
        ?: manualCustomIncrementValueText
    val customIncrementUnitText = (smartParseResult.frequencySpec as? SmartHabitFrequencySpec.CustomUnit)
        ?.incrementUnitText
        ?: manualCustomIncrementUnitText
    val customTargetValueText = (smartParseResult.frequencySpec as? SmartHabitFrequencySpec.CustomUnit)
        ?.targetValueText
        ?: manualCustomTargetValueText
    val customTargetUnitText = (smartParseResult.frequencySpec as? SmartHabitFrequencySpec.CustomUnit)
        ?.targetUnitText
        ?: manualCustomTargetUnitText

    val selectedGroup = remember(habitGroups, selectedGroupId) {
        habitGroups.firstOrNull { it.id == selectedGroupId }
    }
    val effectiveIconGlyph = if (!iconExplicitlySelected && initialHabit == null && selectedGroup != null) {
        selectedGroup.iconGlyph
    } else {
        selectedIconGlyph
    }
    val selectedMoment = remember(moments, selectedMomentId) {
        moments.firstOrNull { it.id == selectedMomentId }
    }
    val repeatShort = remember(repeatHoursText, repeatMinutesText) {
        formatRepeatShort(repeatHoursText, repeatMinutesText)
    }
    val previewColor = selectedGroup?.let { Color(it.colorArgb.toInt()) } ?: EditorSectionColors.HabitIcon
    val groupColor = selectedGroup?.let { Color(it.colorArgb.toInt()) } ?: TextSecondary

    val isIconActive = iconExplicitlySelected || effectiveIconGlyph != NerdIcons.Fire
    val isGroupActive = selectedGroup != null
    val isScheduleActive = startEpochDay != defaultStartEpochDay ||
        activeDaysMask != DaysOfWeekMask.ALL_DAYS ||
        smartParseResult.startEpochDay != null
    val isCompletionActive = completionType != CompletionType.BOOLEAN
    val isTimeActive = if (initialHabit == null) {
        useExactTime ||
            selectedMomentId != defaultTodoODiaMomentId ||
            smartParseResult.momentId != null ||
            repeatShort != null
    } else {
        useExactTime || selectedMomentId != null || repeatShort != null
    }
    val isAlertsActive = !aggressiveFullScreen || !vibrationEnabled

    fun submit() {
        val cleanTitle = smartParseResult.cleanTitle.ifEmpty { titleFieldValue.text.trim() }
        if (cleanTitle.isEmpty()) {
            errorMessage = "Informe o nome do hábito."
            return
        }
        if ((activeDaysMask and DaysOfWeekMask.ALL_DAYS) == 0) {
            errorMessage = "Selecione ao menos um dia da semana."
            activeModal = HabitEditorModal.SCHEDULE
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
                    activeModal = HabitEditorModal.COMPLETION
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
                    activeModal = HabitEditorModal.COMPLETION
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
                iconGlyph = effectiveIconGlyph,
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
                                iconGlyph = effectiveIconGlyph,
                                groupColorArgb = selectedGroup?.colorArgb,
                                size = 30.dp,
                                iconSize = 15.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { showIconPicker = true }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (errorMessage != null) {
                EditorErrorBanner(errorMessage = errorMessage)
            }

            EditorPrimaryNotesCard(
                titleValue = titleFieldValue,
                smartTokens = smartParseResult.tokens,
                notes = notes,
                onTitleValueChange = { incoming ->
                    val outcome = SmartTitleParser.processTextEdit(
                        oldText = titleFieldValue.text,
                        newText = incoming.text,
                        activeTokens = smartParseResult.tokens,
                        dismissedTokens = dismissedTokens
                    )
                    dismissedTokens = outcome.dismissedTokens
                    titleFieldValue = if (outcome.wasTokenDismissed && outcome.cursorOffset != null) {
                        TextFieldValue(
                            text = outcome.text,
                            selection = TextRange(outcome.cursorOffset)
                        )
                    } else {
                        incoming
                    }
                    if (errorMessage != null) errorMessage = null
                },
                onNotesChange = { notes = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EditorSectionIconButton(
                    glyph = effectiveIconGlyph,
                    label = "Ícone",
                    isActive = isIconActive,
                    activeColor = EditorSectionColors.HabitIcon,
                    onClick = { showIconPicker = true },
                    modifier = Modifier.weight(1f)
                )
                EditorSectionIconButton(
                    glyph = NerdIcons.Tag,
                    label = "Grupo",
                    isActive = isGroupActive,
                    activeColor = groupColor,
                    onClick = { activeModal = HabitEditorModal.GROUP },
                    modifier = Modifier.weight(1f)
                )
                EditorSectionIconButton(
                    glyph = NerdIcons.Calendar,
                    label = "Dias",
                    isActive = isScheduleActive,
                    activeColor = EditorSectionColors.Date,
                    onClick = { activeModal = HabitEditorModal.SCHEDULE },
                    modifier = Modifier.weight(1f)
                )
                EditorSectionIconButton(
                    glyph = NerdIcons.Target,
                    label = "Meta",
                    isActive = isCompletionActive,
                    activeColor = EditorSectionColors.HabitGoal,
                    onClick = { activeModal = HabitEditorModal.COMPLETION },
                    modifier = Modifier.weight(1f)
                )
                EditorSectionIconButton(
                    glyph = NerdIcons.ClockOutline,
                    label = "Horário",
                    isActive = isTimeActive,
                    activeColor = EditorSectionColors.Time,
                    onClick = { activeModal = HabitEditorModal.TIME },
                    modifier = Modifier.weight(1f)
                )
                EditorSectionIconButton(
                    glyph = NerdIcons.BellAlert,
                    label = "Alertas",
                    isActive = isAlertsActive,
                    activeColor = EditorSectionColors.Alerts,
                    onClick = { activeModal = HabitEditorModal.ALERTS },
                    modifier = Modifier.weight(1f)
                )
            }

            val hasAnySummary = isGroupActive || isScheduleActive || isCompletionActive || isTimeActive || isAlertsActive
            if (hasAnySummary) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedGroup != null) {
                        EditorSummaryChip(
                            glyph = NerdIcons.Tag,
                            text = selectedGroup.name,
                            accentColor = groupColor,
                            onClick = { activeModal = HabitEditorModal.GROUP }
                        )
                    }
                    if (isScheduleActive) {
                        val parts = buildList {
                            if (startEpochDay != defaultStartEpochDay || smartParseResult.startEpochDay != null) {
                                add("Início ${formatCompactEpochDay(startEpochDay, todayEpochDay)}")
                            }
                            if (activeDaysMask != DaysOfWeekMask.ALL_DAYS) {
                                add(formatDaysMaskCompact(activeDaysMask))
                            }
                        }
                        EditorSummaryChip(
                            glyph = NerdIcons.Calendar,
                            text = parts.joinToString(" • "),
                            accentColor = EditorSectionColors.Date,
                            onClick = { activeModal = HabitEditorModal.SCHEDULE }
                        )
                    }
                    if (isCompletionActive) {
                        val goalText = when (completionType) {
                            CompletionType.COUNT_PER_DAY -> "${countPerDayTargetText.ifBlank { "1" }}x/dia"
                            CompletionType.CUSTOM_UNIT -> {
                                val inc = customIncrementValueText.ifBlank { "?" }
                                val incUnit = customIncrementUnitText.trim()
                                val tar = customTargetValueText.ifBlank { "?" }
                                val tarUnit = customTargetUnitText.trim()
                                "+$inc $incUnit / $tar $tarUnit".trim()
                            }
                            CompletionType.BOOLEAN -> "Sim e Não"
                        }
                        EditorSummaryChip(
                            glyph = NerdIcons.Target,
                            text = goalText,
                            accentColor = EditorSectionColors.HabitGoal,
                            onClick = { activeModal = HabitEditorModal.COMPLETION }
                        )
                    }
                    if (isTimeActive) {
                        val parts = buildList {
                            if (useExactTime) {
                                add(formatMinutes(scheduledMinutes))
                            }
                            selectedMoment?.let { add(it.name) }
                            repeatShort?.let { add("↻ $it") }
                            if (isEmpty()) add("Sem horário")
                        }
                        EditorSummaryChip(
                            glyph = NerdIcons.ClockOutline,
                            text = parts.joinToString(" • "),
                            accentColor = EditorSectionColors.Time,
                            onClick = { activeModal = HabitEditorModal.TIME }
                        )
                    }
                    if (isAlertsActive) {
                        val alertLabel = when {
                            !aggressiveFullScreen && !vibrationEnabled -> "Sem tela cheia • Sem vibração"
                            !aggressiveFullScreen -> "Sem tela cheia"
                            else -> "Sem vibração"
                        }
                        EditorSummaryChip(
                            glyph = NerdIcons.BellAlert,
                            text = alertLabel,
                            accentColor = EditorSectionColors.Alerts,
                            onClick = { activeModal = HabitEditorModal.ALERTS }
                        )
                    }
                }
            }
        }
    }

    when (activeModal) {
        HabitEditorModal.GROUP -> {
            EditorSectionModalSheet(
                title = "Grupo do Hábito",
                glyph = NerdIcons.Tag,
                accentColor = if (isGroupActive) groupColor else IndigoPrimary,
                onDismiss = { activeModal = null }
            ) {
                HabitGroupModalContent(
                    selectedGroup = selectedGroup,
                    habitGroups = habitGroups,
                    onSelectGroup = { group ->
                        manualGroupId = group?.id
                        if (group != null && initialHabit == null && !iconExplicitlySelected) {
                            selectedIconGlyph = group.iconGlyph
                        }
                        activeModal = null
                    },
                    onOpenCreateGroup = { showCreateGroupDialog = true }
                )
            }
        }

        HabitEditorModal.SCHEDULE -> {
            EditorSectionModalSheet(
                title = "Início & Frequência",
                glyph = NerdIcons.Calendar,
                accentColor = EditorSectionColors.Date,
                onDismiss = { activeModal = null }
            ) {
                HabitScheduleAndFrequencyModalContent(
                    startEpochDay = startEpochDay,
                    todayEpochDay = todayEpochDay,
                    activeDaysMask = activeDaysMask,
                    onOpenDatePicker = { showDatePicker = true },
                    onSelectStartDay = { manualStartEpochDay = it },
                    onActiveDaysMaskChange = {
                        activeDaysMask = it
                        if (errorMessage != null) errorMessage = null
                    }
                )
            }
        }

        HabitEditorModal.COMPLETION -> {
            EditorSectionModalSheet(
                title = "Modo de Conclusão",
                glyph = NerdIcons.Target,
                accentColor = EditorSectionColors.HabitGoal,
                onDismiss = { activeModal = null }
            ) {
                HabitCompletionModeModalContent(
                    completionType = completionType,
                    countPerDayTargetText = countPerDayTargetText,
                    customIncrementValueText = customIncrementValueText,
                    customIncrementUnitText = customIncrementUnitText,
                    customTargetValueText = customTargetValueText,
                    customTargetUnitText = customTargetUnitText,
                    onCompletionTypeChange = {
                        manualCompletionType = it
                        if (errorMessage != null) errorMessage = null
                    },
                    onCountPerDayTargetChange = { manualCountPerDayTargetText = it },
                    onCustomIncrementValueChange = { manualCustomIncrementValueText = it },
                    onCustomIncrementUnitChange = { manualCustomIncrementUnitText = it },
                    onCustomTargetValueChange = { manualCustomTargetValueText = it },
                    onCustomTargetUnitChange = { manualCustomTargetUnitText = it }
                )
            }
        }

        HabitEditorModal.TIME -> {
            EditorSectionModalSheet(
                title = "Horário & Momento",
                glyph = NerdIcons.ClockOutline,
                accentColor = EditorSectionColors.Time,
                onDismiss = { activeModal = null }
            ) {
                TimeAndMomentModalContent(
                    useExactTime = useExactTime,
                    scheduledMinutes = scheduledMinutes,
                    moments = moments,
                    selectedMomentId = selectedMomentId,
                    repeatHoursText = repeatHoursText,
                    repeatMinutesText = repeatMinutesText,
                    onUseExactTimeChange = { manualUseExactTime = it },
                    onOpenTimePicker = { showTimePicker = true },
                    onSelectMomentId = { id ->
                        manualMomentId = id
                        manualMomentExplicitlyChanged = true
                        if (id != null && initialHabit == null && smartParseResult.scheduledMinutesOfDay == null) {
                            manualUseExactTime = false
                        }
                    },
                    onRepeatHoursChange = { repeatHoursText = it },
                    onRepeatMinutesChange = { repeatMinutesText = it }
                )
            }
        }

        HabitEditorModal.ALERTS -> {
            EditorSectionModalSheet(
                title = "Alertas",
                glyph = NerdIcons.BellAlert,
                accentColor = EditorSectionColors.Alerts,
                onDismiss = { activeModal = null }
            ) {
                AlertsModalContent(
                    aggressiveFullScreen = aggressiveFullScreen,
                    vibrationEnabled = vibrationEnabled,
                    onAggressiveFullScreenChange = { aggressiveFullScreen = it },
                    onVibrationEnabledChange = { vibrationEnabled = it }
                )
            }
        }

        null -> Unit
    }

    if (showTimePicker) {
        TimePickerModalDialog(
            initialMinutesOfDay = scheduledMinutes,
            title = "Horário do Hábito",
            onDismiss = { showTimePicker = false },
            onConfirm = { picked ->
                manualUseExactTime = true
                manualScheduledMinutes = picked
                showTimePicker = false
            }
        )
    }

    if (showDatePicker) {
        DatePickerModalDialog(
            initialEpochDay = startEpochDay,
            onDismiss = { showDatePicker = false },
            onConfirm = { pickedDay ->
                manualStartEpochDay = pickedDay
                showDatePicker = false
            }
        )
    }

    if (showIconPicker) {
        HabitIconPickerDialog(
            selectedGlyph = effectiveIconGlyph,
            accentColor = previewColor,
            onDismiss = { showIconPicker = false },
            onSelect = { picked ->
                selectedIconGlyph = picked
                iconExplicitlySelected = true
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
                activeModal = null
                onCreateGroup(newGroup) { createdId ->
                    manualGroupId = createdId
                    if (!iconExplicitlySelected && selectedIconGlyph == NerdIcons.Fire) {
                        selectedIconGlyph = newGroup.iconGlyph
                    }
                }
            }
        )
    }
}
