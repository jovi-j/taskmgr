package com.taskmgr.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.jovij.taskmgr.MainActivity
import com.jovij.taskmgr.TaskMgrApplication
import com.jovij.taskmgr.ui.theme.IndigoOnPrimary
import com.jovij.taskmgr.ui.theme.IndigoPrimary
import com.jovij.taskmgr.ui.theme.OledBackground
import com.jovij.taskmgr.ui.theme.OledBorder
import com.jovij.taskmgr.ui.theme.OledSurface
import com.jovij.taskmgr.ui.theme.TextPrimary
import com.jovij.taskmgr.ui.theme.TextSecondary
import com.jovij.taskmgr.ui.theme.TaskmgrTheme
import com.taskmgr.data.local.toDomain
import com.taskmgr.domain.logic.HabitCompletionCalculator
import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.HabitGroup
import com.taskmgr.ui.components.HabitBadgeIcon
import com.taskmgr.ui.components.NerdIcon
import com.taskmgr.ui.components.NerdIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private data class HabitPickerItem(
    val habit: Habit,
    val group: HabitGroup?,
    val remainingText: String,
    val progressText: String
)

class HabitWidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Default result is CANCELED so pressing Back cancels adding the widget.
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        val app = application as TaskMgrApplication

        setContent {
            TaskmgrTheme {
                HabitWidgetConfigureScreen(
                    app = app,
                    appWidgetId = appWidgetId,
                    onCancel = { finish() },
                    onOpenApp = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onHabitSelected = {
                        val resultIntent = Intent().putExtra(
                            AppWidgetManager.EXTRA_APPWIDGET_ID,
                            appWidgetId
                        )
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitWidgetConfigureScreen(
    app: TaskMgrApplication,
    appWidgetId: Int,
    onCancel: () -> Unit,
    onOpenApp: () -> Unit,
    onHabitSelected: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf<List<HabitPickerItem>>(emptyList()) }
    var currentlyBoundHabitId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(appWidgetId) {
        val loaded = withContext(Dispatchers.IO) {
            val todayEpochDay = LocalDate.now().toEpochDay()
            val db = app.container.database
            val habits = db.habitDao().getAll().map { it.toDomain() }
            val groupsById = db.habitGroupDao().getAll().map { it.toDomain() }.associateBy { it.id }

            val boundId = runCatching {
                val glanceId = GlanceAppWidgetManager(app).getGlanceIdBy(appWidgetId)
                getAppWidgetState(app, PreferencesGlanceStateDefinition, glanceId)[HabitWidgetPrefKeys.HABIT_ID]
            }.getOrNull()

            val pickerItems = habits.map { habit ->
                val log = db.habitLogDao().getLog(habit.id, todayEpochDay)?.toDomain()
                val accumulated = log?.accumulatedAmount ?: 0.0
                HabitPickerItem(
                    habit = habit,
                    group = habit.groupId?.let { groupsById[it] },
                    remainingText = HabitCompletionCalculator.formatWidgetProgressText(habit, accumulated),
                    progressText = HabitCompletionCalculator.formatWidgetProgressText(habit, accumulated)
                )
            }
            pickerItems to boundId
        }
        items = loaded.first
        currentlyBoundHabitId = loaded.second
        isLoading = false
    }

    Scaffold(
        containerColor = OledBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OledBackground,
                    titleContentColor = TextPrimary
                ),
                title = {
                    Column {
                        Text(
                            text = "Widget de Hábito (2x2)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Escolha qual hábito exibir na tela inicial",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        NerdIcon(
                            glyph = NerdIcons.Close,
                            color = TextSecondary,
                            size = 20.sp
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            isLoading || isSaving -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = IndigoPrimary)
                }
            }

            items.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NerdIcon(
                        glyph = NerdIcons.Fire,
                        color = IndigoPrimary,
                        size = 44.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nenhum hábito cadastrado ainda",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Crie pelo menos um hábito no taskmgr para poder adicioná-lo ao widget.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onOpenApp,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IndigoPrimary,
                            contentColor = IndigoOnPrimary
                        )
                    ) {
                        Text("Abrir taskmgr")
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(key = "hint") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = OledSurface),
                            border = BorderStroke(1.dp, OledBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                NerdIcon(
                                    glyph = NerdIcons.CheckCircle,
                                    color = IndigoPrimary,
                                    size = 18.sp
                                )
                                Text(
                                    text = "No widget: toque no ícone central para marcar \"Sim\" no hábito, ou fora do ícone para abrir o app.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    items(items = items, key = { it.habit.id }) { item ->
                        val isSelected = currentlyBoundHabitId == item.habit.id
                        val accentColor = item.group?.colorArgb?.let { Color(it.toInt()) } ?: IndigoPrimary
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isSaving = true
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            HabitWidgetUpdater.bindHabitToWidget(
                                                context = app,
                                                appWidgetId = appWidgetId,
                                                habitId = item.habit.id
                                            )
                                        }
                                        onHabitSelected(item.habit.id)
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = OledSurface),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) accentColor else OledBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HabitBadgeIcon(
                                    iconGlyph = item.habit.iconGlyph ?: item.group?.iconGlyph,
                                    groupColorArgb = item.group?.colorArgb,
                                    size = 44.dp,
                                    iconSize = 22.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.habit.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = buildString {
                                            append(item.group?.name ?: "Sem grupo")
                                            append(" • ")
                                            append(item.remainingText)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    NerdIcon(
                                        glyph = NerdIcons.CheckCircle,
                                        color = accentColor,
                                        size = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
