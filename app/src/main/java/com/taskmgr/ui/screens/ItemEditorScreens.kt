package com.taskmgr.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.example.taskmgr.ui.theme.IndigoPrimary
import com.example.taskmgr.ui.theme.OledBackground
import com.example.taskmgr.ui.theme.OledBorder
import com.example.taskmgr.ui.theme.OledSurface
import com.example.taskmgr.ui.theme.OledSurfaceElevated
import com.example.taskmgr.ui.theme.PriorityHigh
import com.example.taskmgr.ui.theme.TextPrimary
import com.example.taskmgr.ui.theme.TextSecondary
import com.taskmgr.domain.logic.DaysOfWeekMask
import com.taskmgr.domain.model.CompletionType
import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.HabitGroup
import com.taskmgr.domain.model.Moment
import com.taskmgr.domain.model.Task
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
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(240, easing = FastOutSlowInEasing)),
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorScreen(
    initialTask: Task?,
    defaultEpochDay: Long,
    moments: List<Moment>,
    onBack: () -> Unit,
    onSave: (Task) -> Unit
) {
    BackHandler(onBack = onBack)

    val todayEpochDay = remember { LocalDate.now().toEpochDay() }
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var notes by remember { mutableStateOf(initialTask?.notes ?: "") }
    var startEpochDay by remember { mutableLongStateOf(initialTask?.startEpochDay ?: defaultEpochDay) }
    var durationDaysText by remember {
        mutableStateOf(
            if (initialTask != null) {
                ((initialTask.endEpochDay - initialTask.startEpochDay) + 1).coerceAtLeast(1).toString()
            } else "1"
        )
    }
    var useExactTime by remember { mutableStateOf(initialTask?.scheduledMinutesOfDay != null || initialTask == null) }
    var scheduledMinutes by remember { mutableIntStateOf(initialTask?.scheduledMinutesOfDay ?: 9 * 60) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedMomentId by remember { mutableStateOf(initialTask?.momentId) }
    val initialRepeat = initialTask?.repeatIntervalMinutes
    var repeatHoursText by remember {
        mutableStateOf(initialRepeat?.let { (it / 60).takeIf { h -> h > 0 }?.toString() } ?: "")
    }
    var repeatMinutesText by remember {
        mutableStateOf(initialRepeat?.let { (it % 60).takeIf { m -> m > 0 }?.toString() } ?: "")
    }
    var aggressiveFullScreen by remember { mutableStateOf(initialTask?.aggressiveFullScreen ?: true) }
    var vibrationEnabled by remember { mutableStateOf(initialTask?.vibrationEnabled ?: true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

        onSave(
            Task(
                id = initialTask?.id ?: 0L,
                title = cleanTitle,
                notes = notes.trim().ifBlank { null },
                startEpochDay = startEpochDay,
                endEpochDay = startEpochDay + durationDays - 1L,
                scheduledMinutesOfDay = parsedMinutes,
                momentId = selectedMomentId,
                repeatIntervalMinutes = repeatInterval,
                aggressiveFullScreen = aggressiveFullScreen,
                vibrationEnabled = vibrationEnabled,
                isCompleted = initialTask?.isCompleted ?: false,
                snoozedUntilEpochMillis = initialTask?.snoozedUntilEpochMillis
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = expandVertically(tween(220, easing = FastOutSlowInEasing)) + fadeIn(tween(180)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(140))
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

            EditorSectionCard(title = "Detalhes da Tarefa", glyph = NerdIcons.Pencil) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorMessage = null },
                    label = { Text("Título da tarefa") },
                    leadingIcon = { NerdIcon(glyph = NerdIcons.Pencil, size = 18.sp) },
                    singleLine = true,
                    colors = oledTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (opcional)") },
                    leadingIcon = { NerdIcon(glyph = NerdIcons.FormatListChecks, size = 18.sp) },
                    colors = oledTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            EditorSectionCard(title = "Data e Duração", glyph = NerdIcons.Calendar) {
                DateSelectorField(
                    label = "Data de início",
                    epochDay = startEpochDay,
                    onClick = { showDatePicker = true }
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = startEpochDay == todayEpochDay - 1,
                        onClick = { startEpochDay = todayEpochDay - 1 },
                        label = { Text("Ontem") }
                    )
                    FilterChip(
                        selected = startEpochDay == todayEpochDay,
                        onClick = { startEpochDay = todayEpochDay },
                        label = { Text("Hoje") }
                    )
                    FilterChip(
                        selected = startEpochDay == todayEpochDay + 1,
                        onClick = { startEpochDay = todayEpochDay + 1 },
                        label = { Text("Amanhã") }
                    )
                }

                OutlinedTextField(
                    value = durationDaysText,
                    onValueChange = { durationDaysText = it.filter { c -> c.isDigit() } },
                    label = { Text("Duração em dias (1..N)") },
                    leadingIcon = { NerdIcon(glyph = NerdIcons.CalendarToday, size = 18.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = oledTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            EditorSectionCard(title = "Horário & Momento", glyph = NerdIcons.ClockOutline) {
                EditorSwitchRow(
                    glyph = NerdIcons.ClockOutline,
                    label = "Horário exato",
                    subtitle = "Dispara no minuto configurado",
                    checked = useExactTime,
                    onCheckedChange = { useExactTime = it }
                )

                AnimatedVisibility(
                    visible = useExactTime,
                    enter = expandVertically(tween(240, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
                    exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
                ) {
                    TimeSelectorField(
                        label = "Horário do lembrete",
                        minutesOfDay = scheduledMinutes,
                        onClick = { showTimePicker = true }
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
                            onClick = { selectedMomentId = null },
                            label = { Text("Nenhum") }
                        )
                        moments.forEach { moment ->
                            FilterChip(
                                selected = selectedMomentId == moment.id,
                                onClick = {
                                    selectedMomentId = moment.id
                                    if (initialTask == null) useExactTime = false
                                },
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
                    onHoursChange = { repeatHoursText = it },
                    onMinutesChange = { repeatMinutesText = it },
                    isOptional = true
                )
            }

            EditorSectionCard(title = "Alertas", glyph = NerdIcons.BellAlert) {
                EditorSwitchRow(
                    glyph = NerdIcons.BellAlert,
                    label = "Alarme em tela cheia",
                    subtitle = "Abre sobre a tela bloqueada ou app atual",
                    checked = aggressiveFullScreen,
                    onCheckedChange = { aggressiveFullScreen = it }
                )
                EditorSwitchRow(
                    glyph = NerdIcons.Vibrate,
                    label = "Vibração",
                    checked = vibrationEnabled,
                    onCheckedChange = { vibrationEnabled = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
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

    val selectedGroup = habitGroups.firstOrNull { it.id == selectedGroupId }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = expandVertically(tween(220, easing = FastOutSlowInEasing)) + fadeIn(tween(180)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(140))
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

            EditorSectionCard(title = "Identidade, Grupo & Ícone", glyph = NerdIcons.Tag) {
                Surface(
                    onClick = { showIconPicker = true },
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
                    onValueChange = { title = it; errorMessage = null },
                    label = { Text("Nome do hábito (ex.: Beber água, Arrumar quarto)") },
                    leadingIcon = { NerdIcon(glyph = NerdIcons.Pencil, size = 18.sp) },
                    singleLine = true,
                    colors = oledTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (opcional)") },
                    leadingIcon = { NerdIcon(glyph = NerdIcons.FormatListChecks, size = 18.sp) },
                    colors = oledTextFieldColors(),
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
                        selected = selectedGroupId == null,
                        onClick = { selectedGroupId = null },
                        label = { Text("Nenhum") }
                    )
                    habitGroups.forEach { group ->
                        FilterChip(
                            selected = selectedGroupId == group.id,
                            onClick = {
                                selectedGroupId = group.id
                                if (initialHabit == null && selectedIconGlyph == NerdIcons.Fire) {
                                    selectedIconGlyph = group.iconGlyph
                                }
                            },
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
                        onClick = { showCreateGroupDialog = true },
                        leadingIcon = { NerdIcon(glyph = NerdIcons.PlusCircle, color = IndigoPrimary, size = 15.sp) },
                        label = { Text("Novo grupo") }
                    )
                }
            }

            EditorSectionCard(title = "Início Retroativo & Frequência", glyph = NerdIcons.History) {
                DateSelectorField(
                    label = "Data de início (pode ser anterior a hoje)",
                    epochDay = startEpochDay,
                    onClick = { showDatePicker = true }
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = startEpochDay == todayEpochDay,
                        onClick = { startEpochDay = todayEpochDay },
                        label = { Text("Hoje") }
                    )
                    FilterChip(
                        selected = startEpochDay == todayEpochDay - 1,
                        onClick = { startEpochDay = todayEpochDay - 1 },
                        label = { Text("Ontem") }
                    )
                    FilterChip(
                        selected = startEpochDay == todayEpochDay - 7,
                        onClick = { startEpochDay = todayEpochDay - 7 },
                        label = { Text("7 dias atrás") }
                    )
                    FilterChip(
                        selected = startEpochDay == todayEpochDay - 30,
                        onClick = { startEpochDay = todayEpochDay - 30 },
                        label = { Text("30 dias atrás") }
                    )
                }

                Text(
                    text = "Dias ativos na semana",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                DaysOfWeekSelector(mask = activeDaysMask, onMaskChange = { activeDaysMask = it })
            }

            EditorSectionCard(title = "Modo de Conclusão", glyph = NerdIcons.Target) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = completionType == CompletionType.BOOLEAN,
                        onClick = { completionType = CompletionType.BOOLEAN },
                        leadingIcon = { NerdIcon(glyph = NerdIcons.CheckCircle, size = 14.sp) },
                        label = { Text("Sim/Não") }
                    )
                    FilterChip(
                        selected = completionType == CompletionType.COUNT_PER_DAY,
                        onClick = { completionType = CompletionType.COUNT_PER_DAY },
                        leadingIcon = { NerdIcon(glyph = NerdIcons.Counter, size = 14.sp) },
                        label = { Text("N 'Sim' / dia") }
                    )
                    FilterChip(
                        selected = completionType == CompletionType.CUSTOM_UNIT,
                        onClick = { completionType = CompletionType.CUSTOM_UNIT },
                        leadingIcon = { NerdIcon(glyph = NerdIcons.CupWater, size = 14.sp) },
                        label = { Text("Unidade custom") }
                    )
                }

                AnimatedContent(
                    targetState = completionType,
                    transitionSpec = {
                        fadeIn(tween(220)) togetherWith fadeOut(tween(160))
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
                                onValueChange = { countPerDayTargetText = it.filter { c -> c.isDigit() } },
                                label = { Text("Quantidade de 'Sim' por dia") },
                                leadingIcon = { NerdIcon(glyph = NerdIcons.Counter, size = 18.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = oledTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        CompletionType.CUSTOM_UNIT -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = customIncrementValueText,
                                        onValueChange = { customIncrementValueText = it },
                                        label = { Text("Por 'Sim' (ex.: 250)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        colors = oledTextFieldColors(),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = customIncrementUnitText,
                                        onValueChange = { customIncrementUnitText = it },
                                        label = { Text("Unid. (ex.: ml)") },
                                        singleLine = true,
                                        colors = oledTextFieldColors(),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = customTargetValueText,
                                        onValueChange = { customTargetValueText = it },
                                        label = { Text("Meta (ex.: 2000)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        colors = oledTextFieldColors(),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = customTargetUnitText,
                                        onValueChange = { customTargetUnitText = it },
                                        label = { Text("Unid. meta (ex.: ml)") },
                                        singleLine = true,
                                        colors = oledTextFieldColors(),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            EditorSectionCard(title = "Horário, Momento & Alertas", glyph = NerdIcons.BellAlert) {
                EditorSwitchRow(
                    glyph = NerdIcons.ClockOutline,
                    label = "Horário exato",
                    checked = useExactTime,
                    onCheckedChange = { useExactTime = it }
                )

                AnimatedVisibility(
                    visible = useExactTime,
                    enter = expandVertically(tween(240, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
                    exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
                ) {
                    TimeSelectorField(
                        label = "Horário do lembrete",
                        minutesOfDay = scheduledMinutes,
                        onClick = { showTimePicker = true }
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
                            onClick = { selectedMomentId = null },
                            label = { Text("Nenhum") }
                        )
                        moments.forEach { moment ->
                            FilterChip(
                                selected = selectedMomentId == moment.id,
                                onClick = {
                                    selectedMomentId = moment.id
                                    if (initialHabit == null) useExactTime = false
                                },
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
                    onHoursChange = { repeatHoursText = it },
                    onMinutesChange = { repeatMinutesText = it },
                    isOptional = true
                )

                EditorSwitchRow(
                    glyph = NerdIcons.BellAlert,
                    label = "Alarme em tela cheia",
                    checked = aggressiveFullScreen,
                    onCheckedChange = { aggressiveFullScreen = it }
                )

                EditorSwitchRow(
                    glyph = NerdIcons.Vibrate,
                    label = "Vibração",
                    checked = vibrationEnabled,
                    onCheckedChange = { vibrationEnabled = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
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
