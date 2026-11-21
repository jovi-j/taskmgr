package com.taskmgr.ui.fullscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.jovij.taskmgr.TaskMgrApplication
import com.jovij.taskmgr.ui.theme.IndigoContainer
import com.jovij.taskmgr.ui.theme.IndigoOnContainer
import com.jovij.taskmgr.ui.theme.IndigoPrimary
import com.jovij.taskmgr.ui.theme.OledBackground
import com.jovij.taskmgr.ui.theme.OledBorder
import com.jovij.taskmgr.ui.theme.OledSurface
import com.jovij.taskmgr.ui.theme.OledSurfaceElevated
import com.jovij.taskmgr.ui.theme.PriorityHigh
import com.jovij.taskmgr.ui.theme.TaskCompleted
import com.jovij.taskmgr.ui.theme.TaskmgrTheme
import com.jovij.taskmgr.ui.theme.TextPrimary
import com.jovij.taskmgr.ui.theme.TextSecondary
import com.jovij.taskmgr.ui.theme.UbuntuSansMonoNerdFontFamily
import com.taskmgr.data.local.toDomain
import com.taskmgr.domain.logic.HabitCompletionCalculator
import com.taskmgr.domain.model.FullScreenReminderPayload
import com.taskmgr.domain.model.ReminderItemType
import com.taskmgr.domain.model.SnoozeOption
import com.taskmgr.scheduler.FullScreenAlertDispatcherImpl
import com.taskmgr.ui.components.NerdIcon
import com.taskmgr.ui.components.NerdIcons
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class FullScreenReminderUiState(
    val itemType: ReminderItemType,
    val itemId: Long,
    val epochDay: Long,
    val title: String,
    val momentName: String?,
    val progressText: String?,
    val progressFraction: Float?,
    val primaryButtonLabel: String,
    val showSnoozeOptions: Boolean = true,
    val acknowledgeOnly: Boolean = false,
    val isFinished: Boolean = false
)

class FullScreenReminderActivity : ComponentActivity() {

    private var currentItemType: ReminderItemType = ReminderItemType.TASK
    private var currentItemId: Long = -1L
    private var payloadState by mutableStateOf<FullScreenReminderPayload?>(null)

    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val typeStr = intent.getStringExtra(FullScreenAlertDispatcherImpl.EXTRA_ITEM_TYPE) ?: return
            val id = intent.getLongExtra(FullScreenAlertDispatcherImpl.EXTRA_ITEM_ID, -1L)
            if (typeStr == currentItemType.name && id == currentItemId) {
                finishAndRemoveTaskSafely()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newPayload = extractPayload(intent)
        currentItemType = newPayload.itemType
        currentItemId = newPayload.itemId
        payloadState = newPayload
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureLockscreenFlags()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        val initialPayload = extractPayload(intent)
        currentItemType = initialPayload.itemType
        currentItemId = initialPayload.itemId
        payloadState = initialPayload

        ContextCompat.registerReceiver(
            this,
            dismissReceiver,
            IntentFilter(FullScreenAlertDispatcherImpl.ACTION_DISMISS_REMINDER),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val container = (application as TaskMgrApplication).container

        setContent {
            val activePayload = payloadState ?: initialPayload
            val scope = rememberCoroutineScope()
            val db = container.database

            val uiStateFlow = remember(
                activePayload.itemType,
                activePayload.itemId,
                activePayload.epochDay,
                activePayload.showSnoozeOptions,
                activePayload.acknowledgeOnly
            ) {
                when (activePayload.itemType) {
                    ReminderItemType.TASK -> {
                        combine(
                            db.taskDao().observeById(activePayload.itemId),
                            db.momentDao().observeAll()
                        ) { taskEntity, moments ->
                            if (taskEntity == null) {
                                FullScreenReminderUiState(
                                    itemType = ReminderItemType.TASK,
                                    itemId = activePayload.itemId,
                                    epochDay = activePayload.epochDay,
                                    title = activePayload.title,
                                    momentName = activePayload.momentName,
                                    progressText = activePayload.progressText,
                                    progressFraction = null,
                                    primaryButtonLabel = "Sim",
                                    showSnoozeOptions = false,
                                    acknowledgeOnly = activePayload.acknowledgeOnly,
                                    isFinished = true
                                )
                            } else {
                                val task = taskEntity.toDomain()
                                val momentName = task.momentId?.let { id ->
                                    moments.firstOrNull { it.id == id }?.name
                                } ?: activePayload.momentName
                                val refreshedPayload = HabitCompletionCalculator.buildTaskPayload(
                                    task = task,
                                    epochDay = activePayload.epochDay,
                                    momentName = momentName,
                                    isAdvanceReminder = activePayload.acknowledgeOnly
                                )
                                FullScreenReminderUiState(
                                    itemType = ReminderItemType.TASK,
                                    itemId = task.id,
                                    epochDay = activePayload.epochDay,
                                    title = task.title,
                                    momentName = momentName,
                                    progressText = refreshedPayload.progressText ?: activePayload.progressText,
                                    progressFraction = null,
                                    primaryButtonLabel = "Sim",
                                    showSnoozeOptions = false,
                                    acknowledgeOnly = activePayload.acknowledgeOnly,
                                    isFinished = task.isCompleted
                                )
                            }
                        }
                    }

                    ReminderItemType.HABIT -> {
                        combine(
                            db.habitDao().observeById(activePayload.itemId),
                            db.habitLogDao().observeLog(activePayload.itemId, activePayload.epochDay),
                            db.momentDao().observeAll()
                        ) { habitEntity, logEntity, moments ->
                            if (habitEntity == null) {
                                FullScreenReminderUiState(
                                    itemType = ReminderItemType.HABIT,
                                    itemId = activePayload.itemId,
                                    epochDay = activePayload.epochDay,
                                    title = activePayload.title,
                                    momentName = activePayload.momentName,
                                    progressText = activePayload.progressText,
                                    progressFraction = null,
                                    primaryButtonLabel = activePayload.incrementButtonLabel ?: "Sim (Concluir)",
                                    showSnoozeOptions = true,
                                    acknowledgeOnly = false,
                                    isFinished = true
                                )
                            } else {
                                val habit = habitEntity.toDomain()
                                val log = logEntity?.toDomain()
                                val accumulated = log?.accumulatedAmount ?: 0.0
                                val fraction = (accumulated / habit.targetAmount.coerceAtLeast(1e-6))
                                    .toFloat()
                                    .coerceIn(0f, 1f)
                                val momentName = habit.momentId?.let { id ->
                                    moments.firstOrNull { it.id == id }?.name
                                } ?: activePayload.momentName

                                FullScreenReminderUiState(
                                    itemType = ReminderItemType.HABIT,
                                    itemId = habit.id,
                                    epochDay = activePayload.epochDay,
                                    title = habit.title,
                                    momentName = momentName,
                                    progressText = HabitCompletionCalculator.formatProgressText(habit, accumulated),
                                    progressFraction = fraction,
                                    primaryButtonLabel = HabitCompletionCalculator.formatIncrementButtonLabel(habit),
                                    showSnoozeOptions = true,
                                    acknowledgeOnly = false,
                                    isFinished = (log?.isCompleted == true) || (log?.dismissedForDay == true)
                                )
                            }
                        }
                    }
                }
            }

            val fallbackState = FullScreenReminderUiState(
                itemType = activePayload.itemType,
                itemId = activePayload.itemId,
                epochDay = activePayload.epochDay,
                title = activePayload.title,
                momentName = activePayload.momentName,
                progressText = activePayload.progressText,
                progressFraction = null,
                primaryButtonLabel = activePayload.incrementButtonLabel
                    ?: if (activePayload.itemType == ReminderItemType.TASK) "Sim" else "Sim (Concluir)",
                showSnoozeOptions = activePayload.showSnoozeOptions,
                acknowledgeOnly = activePayload.acknowledgeOnly
            )

            val uiState by uiStateFlow.collectAsState(initial = fallbackState)

            LaunchedEffect(uiState.isFinished) {
                if (uiState.isFinished) {
                    container.vibrationController.cancel()
                    finishAndRemoveTaskSafely()
                }
            }

            TaskmgrTheme(darkTheme = true, dynamicColor = false) {
                FullScreenReminderScreen(
                    uiState = uiState,
                    onPrimaryAction = {
                        container.vibrationController.cancel()
                        container.alertDispatcher.cancelStatusBarNotification(uiState.itemType, uiState.itemId)
                        scope.launch {
                            val now = System.currentTimeMillis()
                            when (uiState.itemType) {
                                ReminderItemType.TASK -> {
                                    if (uiState.acknowledgeOnly) {
                                        container.reminderScheduler.scheduleNextForTask(uiState.itemId)
                                        finishAndRemoveTaskSafely()
                                    } else {
                                        container.taskRepository.setCompleted(uiState.itemId, true, now)
                                    }
                                }
                                ReminderItemType.HABIT -> {
                                    container.habitRepository.incrementHabit(
                                        habitId = uiState.itemId,
                                        epochDay = uiState.epochDay,
                                        nowEpochMillis = now
                                    )
                                }
                            }
                        }
                    },
                    onSnooze = { option ->
                        container.vibrationController.cancel()
                        container.alertDispatcher.cancelStatusBarNotification(uiState.itemType, uiState.itemId)
                        scope.launch {
                            val now = System.currentTimeMillis()
                            when (uiState.itemType) {
                                ReminderItemType.TASK -> {
                                    container.taskRepository.snooze(uiState.itemId, option, now)
                                }
                                ReminderItemType.HABIT -> {
                                    container.habitRepository.snoozeHabit(
                                        habitId = uiState.itemId,
                                        epochDay = uiState.epochDay,
                                        option = option,
                                        nowEpochMillis = now
                                    )
                                }
                            }
                            finishAndRemoveTaskSafely()
                        }
                    },
                    onDismissScreen = {
                        container.vibrationController.cancel()
                        container.alertDispatcher.cancelStatusBarNotification(uiState.itemType, uiState.itemId)
                        finishAndRemoveTaskSafely()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        runCatching {
            val container = (application as TaskMgrApplication).container
            container.vibrationController.cancel()
            container.alertDispatcher.cancelStatusBarNotification(currentItemType, currentItemId)
        }
        runCatching { unregisterReceiver(dismissReceiver) }
        super.onDestroy()
    }

    private fun configureLockscreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun finishAndRemoveTaskSafely() {
        if (!isFinishing) {
            finish()
        }
    }

    private fun extractPayload(intent: Intent): FullScreenReminderPayload {
        val typeName = intent.getStringExtra(EXTRA_ITEM_TYPE) ?: ReminderItemType.TASK.name
        val itemType = runCatching { ReminderItemType.valueOf(typeName) }.getOrDefault(ReminderItemType.TASK)
        return FullScreenReminderPayload(
            itemType = itemType,
            itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L),
            epochDay = intent.getLongExtra(EXTRA_EPOCH_DAY, LocalDate.now().toEpochDay()),
            title = intent.getStringExtra(EXTRA_TITLE) ?: "Lembrete",
            momentName = intent.getStringExtra(EXTRA_MOMENT_NAME),
            progressText = intent.getStringExtra(EXTRA_PROGRESS_TEXT),
            incrementButtonLabel = intent.getStringExtra(EXTRA_INCREMENT_LABEL),
            vibrationEnabled = intent.getBooleanExtra(EXTRA_VIBRATION_ENABLED, true),
            showSnoozeOptions = intent.getBooleanExtra(
                EXTRA_SHOW_SNOOZE_OPTIONS,
                itemType == ReminderItemType.HABIT
            ),
            acknowledgeOnly = intent.getBooleanExtra(EXTRA_ACKNOWLEDGE_ONLY, false)
        )
    }

    companion object {
        const val EXTRA_ITEM_TYPE = "extra_item_type"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_EPOCH_DAY = "extra_epoch_day"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MOMENT_NAME = "extra_moment_name"
        const val EXTRA_PROGRESS_TEXT = "extra_progress_text"
        const val EXTRA_INCREMENT_LABEL = "extra_increment_label"
        const val EXTRA_VIBRATION_ENABLED = "extra_vibration_enabled"
        const val EXTRA_SHOW_SNOOZE_OPTIONS = "extra_show_snooze_options"
        const val EXTRA_ACKNOWLEDGE_ONLY = "extra_acknowledge_only"

        fun createIntent(context: Context, payload: FullScreenReminderPayload): Intent {
            return Intent(context, FullScreenReminderActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                putExtra(EXTRA_ITEM_TYPE, payload.itemType.name)
                putExtra(EXTRA_ITEM_ID, payload.itemId)
                putExtra(EXTRA_EPOCH_DAY, payload.epochDay)
                putExtra(EXTRA_TITLE, payload.title)
                putExtra(EXTRA_MOMENT_NAME, payload.momentName)
                putExtra(EXTRA_PROGRESS_TEXT, payload.progressText)
                putExtra(EXTRA_INCREMENT_LABEL, payload.incrementButtonLabel)
                putExtra(EXTRA_VIBRATION_ENABLED, payload.vibrationEnabled)
                putExtra(EXTRA_SHOW_SNOOZE_OPTIONS, payload.showSnoozeOptions)
                putExtra(EXTRA_ACKNOWLEDGE_ONLY, payload.acknowledgeOnly)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FullScreenReminderScreen(
    uiState: FullScreenReminderUiState,
    onPrimaryAction: () -> Unit,
    onSnooze: (SnoozeOption) -> Unit,
    onDismissScreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentTimeText = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
    val typeLabel = if (uiState.itemType == ReminderItemType.HABIT) "Hábito" else "Tarefa"
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentVisible = true }
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progressFraction ?: 0f,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "fullscreen_reminder_progress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        OledBackground,
                        OledSurface,
                        OledBackground
                    )
                )
            )
            .safeDrawingPadding()
    ) {
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(280)) + slideInVertically(
                animationSpec = tween(320, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 14 }
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = IndigoContainer,
                    border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NerdIcon(
                            glyph = if (uiState.itemType == ReminderItemType.HABIT) NerdIcons.Fire else NerdIcons.Alarm,
                            color = IndigoPrimary,
                            size = 18.sp
                        )
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = IndigoOnContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = currentTimeText,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = UbuntuSansMonoNerdFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary
                )

                if (!uiState.momentName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NerdIcon(
                            glyph = NerdIcons.ClockOutline,
                            color = TextSecondary,
                            size = 16.sp
                        )
                        Text(
                            text = "Momento: ${uiState.momentName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = uiState.title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
            }

            if (uiState.progressText != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = OledSurface),
                    border = BorderStroke(1.dp, OledBorder),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            NerdIcon(
                                glyph = if (uiState.itemType == ReminderItemType.HABIT) NerdIcons.Trophy else NerdIcons.CalendarToday,
                                color = TextSecondary,
                                size = 16.sp
                            )
                            Text(
                                text = if (uiState.itemType == ReminderItemType.HABIT) "Progresso hoje" else "Data e horário agendado",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.progressText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary
                        )
                        if (uiState.progressFraction != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = IndigoPrimary,
                                trackColor = OledSurfaceElevated
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onPrimaryAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TaskCompleted,
                        contentColor = Color(0xFF041F0D)
                    )
                ) {
                    NerdIcon(
                        glyph = NerdIcons.CheckCircle,
                        color = Color(0xFF041F0D),
                        size = 24.sp
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = uiState.primaryButtonLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (uiState.showSnoozeOptions) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NerdIcon(
                            glyph = NerdIcons.TimerOutline,
                            color = TextSecondary,
                            size = 18.sp
                        )
                        Text(
                            text = "Soneca",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SnoozeOption.entries.forEach { option ->
                            if (option == SnoozeOption.UNTIL_TOMORROW) {
                                OutlinedButton(
                                    onClick = { onSnooze(option) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, OledBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = OledSurfaceElevated,
                                        contentColor = PriorityHigh
                                    )
                                ) {
                                    NerdIcon(
                                        glyph = NerdIcons.BellOffOutline,
                                        color = PriorityHigh,
                                        size = 15.sp
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(text = option.labelPtBr())
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = { onSnooze(option) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = OledSurfaceElevated,
                                        contentColor = TextPrimary
                                    )
                                ) {
                                    NerdIcon(glyph = NerdIcons.ClockOutline, color = IndigoPrimary, size = 14.sp)
                                    Spacer(modifier = Modifier.size(5.dp))
                                    Text(text = option.labelPtBr())
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = onDismissScreen,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = TextSecondary
                        )
                    ) {
                        NerdIcon(glyph = NerdIcons.Close, color = TextSecondary, size = 15.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Fechar")
                    }
                }
            }
        }
        }
    }
}

fun SnoozeOption.labelPtBr(): String = when (this) {
    SnoozeOption.MINUTES_5 -> "5 min"
    SnoozeOption.MINUTES_10 -> "10 min"
    SnoozeOption.MINUTES_30 -> "30 min"
    SnoozeOption.HOURS_1 -> "1 hora"
    SnoozeOption.HOURS_3 -> "3 horas"
    SnoozeOption.UNTIL_TOMORROW -> "Até amanhã"
}
