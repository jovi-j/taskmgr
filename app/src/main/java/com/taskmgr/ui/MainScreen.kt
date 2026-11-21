package com.taskmgr.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskmgr.R
import com.example.taskmgr.ui.theme.IndigoContainer
import com.example.taskmgr.ui.theme.IndigoOnPrimary
import com.example.taskmgr.ui.theme.IndigoPrimary
import com.example.taskmgr.ui.theme.OledBackground
import com.example.taskmgr.ui.theme.OledBorder
import com.example.taskmgr.ui.theme.OledSurface
import com.example.taskmgr.ui.theme.OledSurfaceElevated
import com.example.taskmgr.ui.theme.PriorityHigh
import com.example.taskmgr.ui.theme.TextPrimary
import com.example.taskmgr.ui.theme.TextSecondary
import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.HabitGroup
import com.taskmgr.domain.model.Moment
import com.taskmgr.domain.model.Task
import com.taskmgr.scheduler.PermissionHelper
import com.taskmgr.ui.components.HabitGroupEditorDialog
import com.taskmgr.ui.components.MomentEditorDialog
import com.taskmgr.ui.components.NerdIcon
import com.taskmgr.ui.components.NerdIcons
import com.taskmgr.ui.components.OnboardingPermissionsDialog
import com.taskmgr.ui.screens.CalendarScreen
import com.taskmgr.ui.screens.HabitEditorScreen
import com.taskmgr.ui.screens.HabitGroupsScreen
import com.taskmgr.ui.screens.MomentsScreen
import com.taskmgr.ui.screens.SettingsScreen
import com.taskmgr.ui.screens.TaskEditorScreen
import com.taskmgr.ui.screens.TodayScreen
import com.taskmgr.ui.viewmodel.TaskMgrViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

enum class MainTab(val label: String) {
    TODAY("Hoje"),
    CALENDAR("Calendário"),
    GROUPS("Grupos"),
    MOMENTS("Momentos"),
    SETTINGS("Config")
}

private sealed interface ActiveEditor {
    data class TaskEditor(val task: Task?) : ActiveEditor
    data class HabitEditor(val habit: Habit?, val defaultGroupId: Long? = null) : ActiveEditor
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: TaskMgrViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(MainTab.TODAY) }
    var permissionStatus by remember { mutableStateOf(PermissionHelper.getStatus(context)) }

    var showFabMenu by remember { mutableStateOf(false) }
    var activeEditor by remember { mutableStateOf<ActiveEditor?>(null) }
    var editingMoment by remember { mutableStateOf<Moment?>(null) }
    var showMomentDialog by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<HabitGroup?>(null) }
    var showGroupDialog by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        permissionStatus = PermissionHelper.getStatus(context)
    }

    val coroutineScope = rememberCoroutineScope()

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        viewModel.refreshCalendarAccounts()
        val granted = grants[Manifest.permission.READ_CALENDAR] == true &&
            grants[Manifest.permission.WRITE_CALENDAR] == true
        if (granted) {
            viewModel.syncCalendarNow()
        } else {
            viewModel.setExportSyncStatusMessage(
                message = "Permissão de calendário negada.",
                isError = true
            )
        }
    }

    val importTaskMgrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val content = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            stream.bufferedReader(Charsets.UTF_8).readText()
                        }
                    }.getOrNull()
                }
                if (!content.isNullOrBlank()) {
                    viewModel.importTaskMgrMarkdownContent(content)
                } else {
                    viewModel.setExportSyncStatusMessage(
                        message = "Não foi possível ler o arquivo .taskmgr.md selecionado.",
                        isError = true
                    )
                }
            }
        }
    }

    val importGfmMarkdownLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val content = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            stream.bufferedReader(Charsets.UTF_8).readText()
                        }
                    }.getOrNull()
                }
                if (!content.isNullOrBlank()) {
                    viewModel.importGfmMarkdownContent(content)
                } else {
                    viewModel.setExportSyncStatusMessage(
                        message = "Não foi possível ler o arquivo .md selecionado.",
                        isError = true
                    )
                }
            }
        }
    }

    val exportTaskMgrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val markdown = viewModel.buildTaskMgrMarkdownExport()
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(markdown.toByteArray(Charsets.UTF_8))
                        } ?: error("Null output stream")
                    }.isSuccess
                }
                if (ok) {
                    viewModel.setExportSyncStatusMessage(
                        message = "Arquivo .taskmgr.md exportado com sucesso!",
                        isError = false
                    )
                } else {
                    viewModel.setExportSyncStatusMessage(
                        message = "Erro ao salvar o arquivo .taskmgr.md.",
                        isError = true
                    )
                }
            }
        }
    }

    val exportNotionObsidianLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val markdown = viewModel.buildNotionObsidianMarkdownExport()
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(markdown.toByteArray(Charsets.UTF_8))
                        } ?: error("Null output stream")
                    }.isSuccess
                }
                if (ok) {
                    viewModel.setExportSyncStatusMessage(
                        message = "Arquivo .md (Notion/Obsidian) exportado com sucesso!",
                        isError = false
                    )
                } else {
                    viewModel.setExportSyncStatusMessage(
                        message = "Erro ao salvar o arquivo .md (Notion/Obsidian).",
                        isError = true
                    )
                }
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionStatus = PermissionHelper.getStatus(context)
        viewModel.refreshCalendarAccounts()
    }

    val todayState by viewModel.todayUiState.collectAsStateWithLifecycle()
    val calendarState by viewModel.calendarUiState.collectAsStateWithLifecycle()
    val dayDetailState by viewModel.dayDetailUiState.collectAsStateWithLifecycle()
    val habitRetentionSummaries by viewModel.habitRetentionSummaries.collectAsStateWithLifecycle()
    val allHabitsWithStats by viewModel.allHabitsWithStats.collectAsStateWithLifecycle()
    val moments by viewModel.moments.collectAsStateWithLifecycle()
    val habitGroups by viewModel.habitGroups.collectAsStateWithLifecycle()
    val vibrationPattern by viewModel.vibrationPattern.collectAsStateWithLifecycle()
    val showOnboarding by viewModel.showOnboarding.collectAsStateWithLifecycle()
    val exportSyncUiState by viewModel.exportSyncUiState.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = activeEditor,
        transitionSpec = {
            if (targetState != null) {
                (slideInVertically(
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    initialOffsetY = { it / 10 }
                ) + fadeIn(tween(180))) togetherWith fadeOut(tween(120))
            } else {
                fadeIn(tween(180)) togetherWith (slideOutVertically(
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    targetOffsetY = { it / 10 }
                ) + fadeOut(tween(140)))
            }
        },
        label = "editor_screen_transition"
    ) { targetEditor ->
        when (targetEditor) {
            is ActiveEditor.TaskEditor -> {
                TaskEditorScreen(
                    initialTask = targetEditor.task,
                    defaultEpochDay = todayState.selectedEpochDay,
                    moments = moments,
                    onBack = { activeEditor = null },
                    onSave = { task ->
                        viewModel.upsertTask(task)
                        activeEditor = null
                    }
                )
            }

            is ActiveEditor.HabitEditor -> {
                HabitEditorScreen(
                    initialHabit = targetEditor.habit,
                    defaultStartEpochDay = todayState.selectedEpochDay,
                    defaultGroupId = targetEditor.defaultGroupId,
                    moments = moments,
                    habitGroups = habitGroups,
                    onBack = { activeEditor = null },
                    onCreateGroup = { group, onCreated ->
                        viewModel.upsertHabitGroup(group, onCreated)
                    },
                    onSave = { habit ->
                        viewModel.upsertHabit(habit)
                        activeEditor = null
                    }
                )
            }

            null -> {
                Scaffold(
                    containerColor = OledBackground,
                    topBar = {
                        Column {
                            TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = OledBackground,
                                    titleContentColor = TextPrimary
                                ),
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(R.drawable.ic_taskmgr_icon),
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Icon(
                                            painter = painterResource(R.drawable.ic_taskmgr_logo),
                                            contentDescription = "taskmgr",
                                            tint = TextPrimary,
                                            modifier = Modifier.height(26.dp)
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { currentTab = MainTab.SETTINGS }) {
                                        NerdIcon(
                                            glyph = if (permissionStatus.allCriticalGranted) NerdIcons.ShieldCheck else NerdIcons.BellAlert,
                                            color = if (permissionStatus.allCriticalGranted) {
                                                IndigoPrimary
                                            } else {
                                                PriorityHigh
                                            },
                                            size = 22.sp
                                        )
                                    }
                                }
                            )
                            HorizontalDivider(color = OledBorder, thickness = 1.dp)
                        }
                    },
                    bottomBar = {
                        val navColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IndigoPrimary,
                            selectedTextColor = IndigoPrimary,
                            indicatorColor = IndigoContainer,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                        Column {
                            HorizontalDivider(color = OledBorder, thickness = 1.dp)
                            NavigationBar(containerColor = OledSurface) {
                                NavigationBarItem(
                                    selected = currentTab == MainTab.TODAY,
                                    onClick = { currentTab = MainTab.TODAY },
                                    icon = { NerdIcon(glyph = NerdIcons.CalendarToday, size = 20.sp) },
                                    label = { Text(MainTab.TODAY.label, maxLines = 1) },
                                    colors = navColors
                                )
                                NavigationBarItem(
                                    selected = currentTab == MainTab.CALENDAR,
                                    onClick = { currentTab = MainTab.CALENDAR },
                                    icon = { NerdIcon(glyph = NerdIcons.Calendar, size = 20.sp) },
                                    label = { Text(MainTab.CALENDAR.label, maxLines = 1) },
                                    colors = navColors
                                )
                                NavigationBarItem(
                                    selected = currentTab == MainTab.GROUPS,
                                    onClick = { currentTab = MainTab.GROUPS },
                                    icon = { NerdIcon(glyph = NerdIcons.Tag, size = 20.sp) },
                                    label = { Text(MainTab.GROUPS.label, maxLines = 1) },
                                    colors = navColors
                                )
                                NavigationBarItem(
                                    selected = currentTab == MainTab.MOMENTS,
                                    onClick = { currentTab = MainTab.MOMENTS },
                                    icon = { NerdIcon(glyph = NerdIcons.TimerOutline, size = 20.sp) },
                                    label = { Text(MainTab.MOMENTS.label, maxLines = 1) },
                                    colors = navColors
                                )
                                NavigationBarItem(
                                    selected = currentTab == MainTab.SETTINGS,
                                    onClick = { currentTab = MainTab.SETTINGS },
                                    icon = { NerdIcon(glyph = NerdIcons.Cog, size = 20.sp) },
                                    label = { Text(MainTab.SETTINGS.label, maxLines = 1) },
                                    colors = navColors
                                )
                            }
                        }
                    },
                    floatingActionButton = {
                        AnimatedVisibility(
                            visible = currentTab == MainTab.TODAY,
                            enter = scaleIn(tween(220, easing = FastOutSlowInEasing)) + fadeIn(tween(180)),
                            exit = scaleOut(tween(180)) + fadeOut(tween(140))
                        ) {
                            Box {
                                FloatingActionButton(
                                    onClick = { showFabMenu = true },
                                    containerColor = IndigoPrimary,
                                    contentColor = IndigoOnPrimary
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Criar item")
                                }
                                DropdownMenu(
                                    expanded = showFabMenu,
                                    onDismissRequest = { showFabMenu = false },
                                    containerColor = OledSurfaceElevated
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Nova Tarefa", color = TextPrimary) },
                                        leadingIcon = {
                                            NerdIcon(
                                                glyph = NerdIcons.CheckboxMarkedCircle,
                                                color = IndigoPrimary,
                                                size = 20.sp
                                            )
                                        },
                                        onClick = {
                                            showFabMenu = false
                                            activeEditor = ActiveEditor.TaskEditor(null)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Novo Hábito", color = TextPrimary) },
                                        leadingIcon = {
                                            NerdIcon(
                                                glyph = NerdIcons.Fire,
                                                color = IndigoPrimary,
                                                size = 20.sp
                                            )
                                        },
                                        onClick = {
                                            showFabMenu = false
                                            activeEditor = ActiveEditor.HabitEditor(null)
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    AnimatedContent(
                        targetState = currentTab,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        transitionSpec = {
                            val forward = targetState.ordinal >= initialState.ordinal
                            val slideOffset: (Int) -> Int = { width ->
                                if (forward) width / 8 else -width / 8
                            }
                            (slideInHorizontally(
                                animationSpec = tween(280, easing = FastOutSlowInEasing),
                                initialOffsetX = slideOffset
                            ) + fadeIn(tween(260))) togetherWith (slideOutHorizontally(
                                animationSpec = tween(240, easing = FastOutSlowInEasing),
                                targetOffsetX = { width -> -slideOffset(width) }
                            ) + fadeOut(tween(180)))
                        },
                        label = "main_tab_transition"
                    ) { tab ->
                        when (tab) {
                            MainTab.TODAY -> {
                                TodayScreen(
                                    uiState = todayState,
                                    permissionStatus = permissionStatus,
                                    onSelectDay = viewModel::selectDay,
                                    onToggleTask = viewModel::toggleTaskCompleted,
                                    onEditTask = { task ->
                                        activeEditor = ActiveEditor.TaskEditor(task)
                                    },
                                    onDeleteTask = viewModel::deleteTask,
                                    onTestTaskAlarm = viewModel::triggerTestReminder,
                                    onIncrementHabit = viewModel::incrementHabit,
                                    onEditHabit = { habit ->
                                        activeEditor = ActiveEditor.HabitEditor(habit)
                                    },
                                    onDeleteHabit = viewModel::deleteHabit,
                                    onTestHabitAlarm = viewModel::triggerTestReminder,
                                    onOpenPermissionsSettings = { currentTab = MainTab.SETTINGS }
                                )
                            }

                            MainTab.CALENDAR -> {
                                CalendarScreen(
                                    calendarState = calendarState,
                                    dayDetailState = dayDetailState,
                                    habitRetentionSummaries = habitRetentionSummaries,
                                    onSelectMonth = viewModel::selectMonth,
                                    onDayClick = viewModel::openDayDetail,
                                    onDismissDayDetail = viewModel::closeDayDetail,
                                    onToggleTask = viewModel::toggleTaskCompleted,
                                    onUpdateHabitLogManual = viewModel::updateHabitLogManual,
                                    onIncludeHabitFromPastDay = viewModel::includeHabitFromPastDay
                                )
                            }

                            MainTab.GROUPS -> {
                                HabitGroupsScreen(
                                    habitGroups = habitGroups,
                                    habitsWithStats = allHabitsWithStats,
                                    onAddHabitGroup = {
                                        editingGroup = null
                                        showGroupDialog = true
                                    },
                                    onEditHabitGroup = { group ->
                                        editingGroup = group
                                        showGroupDialog = true
                                    },
                                    onDeleteHabitGroup = viewModel::deleteHabitGroup,
                                    onCreateHabitInGroup = { groupId ->
                                        activeEditor = ActiveEditor.HabitEditor(
                                            habit = null,
                                            defaultGroupId = groupId
                                        )
                                    },
                                    onEditHabit = { habit ->
                                        activeEditor = ActiveEditor.HabitEditor(habit)
                                    },
                                    onDeleteHabit = viewModel::deleteHabit
                                )
                            }

                            MainTab.MOMENTS -> {
                                MomentsScreen(
                                    moments = moments,
                                    onAddMoment = {
                                        editingMoment = null
                                        showMomentDialog = true
                                    },
                                    onEditMoment = { moment ->
                                        editingMoment = moment
                                        showMomentDialog = true
                                    },
                                    onToggleMomentEnabled = { moment ->
                                        viewModel.upsertMoment(moment.copy(isEnabled = !moment.isEnabled))
                                    },
                                    onDeleteMoment = viewModel::deleteMoment
                                )
                            }

                            MainTab.SETTINGS -> {
                                val todayIso = LocalDate.now().toString()
                                SettingsScreen(
                                    permissionStatus = permissionStatus,
                                    vibrationPattern = vibrationPattern,
                                    exportSyncUiState = exportSyncUiState,
                                    onSyncCalendarNow = viewModel::syncCalendarNow,
                                    onRequestCalendarPermissions = {
                                        calendarPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.READ_CALENDAR,
                                                Manifest.permission.WRITE_CALENDAR
                                            )
                                        )
                                    },
                                    onSelectCalendarForSync = viewModel::selectCalendarForSync,
                                    onToggleAutoSyncTasks = viewModel::setAutoSyncTasksEnabled,
                                    onImportTaskMgrMarkdown = {
                                        importTaskMgrLauncher.launch(arrayOf("text/*", "application/octet-stream", "*/*"))
                                    },
                                    onImportNotionObsidianMarkdown = {
                                        importGfmMarkdownLauncher.launch(arrayOf("text/*", "application/octet-stream", "*/*"))
                                    },
                                    onExportTaskMgrMarkdown = {
                                        exportTaskMgrLauncher.launch("backup-$todayIso.taskmgr.md")
                                    },
                                    onExportNotionObsidianMarkdown = {
                                        exportNotionObsidianLauncher.launch("taskmgr-$todayIso.md")
                                    },
                                    onShareTaskMgrMarkdown = {
                                        coroutineScope.launch {
                                            val md = viewModel.buildTaskMgrMarkdownExport()
                                            shareMarkdownFile(
                                                context = context,
                                                fileName = "backup-$todayIso.taskmgr.md",
                                                markdown = md
                                            )
                                        }
                                    },
                                    onShareNotionObsidianMarkdown = {
                                        coroutineScope.launch {
                                            val md = viewModel.buildNotionObsidianMarkdownExport()
                                            shareMarkdownFile(
                                                context = context,
                                                fileName = "taskmgr-$todayIso.md",
                                                markdown = md
                                            )
                                        }
                                    },
                                    onRequestFullScreenPermission = {
                                        context.startActivity(PermissionHelper.buildFullScreenIntentSettingsIntent(context))
                                    },
                                    onRequestOverlayPermission = {
                                        context.startActivity(PermissionHelper.buildOverlayPermissionIntent(context))
                                    },
                                    onRequestExactAlarmPermission = {
                                        context.startActivity(PermissionHelper.buildExactAlarmSettingsIntent(context))
                                    },
                                    onRequestPostNotificationPermission = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            context.startActivity(PermissionHelper.buildAppNotificationSettingsIntent(context))
                                        }
                                    },
                                    onRequestBatteryOptimization = {
                                        context.startActivity(PermissionHelper.buildIgnoreBatteryOptimizationIntent(context))
                                    },
                                    onOpenManufacturerPermissions = {
                                        PermissionHelper.openManufacturerPermissions(context)
                                    },
                                    isXiaomiDevice = PermissionHelper.isXiaomiFamilyDevice(),
                                    onSelectVibrationPattern = viewModel::setVibrationPattern,
                                    onTestVibration = viewModel::testVibration,
                                    onStopVibration = viewModel::stopVibration
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOnboarding && !permissionStatus.allCriticalGranted) {
        OnboardingPermissionsDialog(
            permissionStatus = permissionStatus,
            isXiaomiDevice = PermissionHelper.isXiaomiFamilyDevice(),
            onRequestFullScreen = {
                context.startActivity(PermissionHelper.buildFullScreenIntentSettingsIntent(context))
            },
            onRequestOverlay = {
                context.startActivity(PermissionHelper.buildOverlayPermissionIntent(context))
            },
            onRequestExactAlarm = {
                context.startActivity(PermissionHelper.buildExactAlarmSettingsIntent(context))
            },
            onRequestNotifications = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    context.startActivity(PermissionHelper.buildAppNotificationSettingsIntent(context))
                }
            },
            onRequestBatteryUnrestricted = {
                context.startActivity(PermissionHelper.buildIgnoreBatteryOptimizationIntent(context))
            },
            onOpenManufacturerPermissions = {
                PermissionHelper.openManufacturerPermissions(context)
            },
            onFinish = viewModel::dismissOnboarding
        )
    }

    if (showMomentDialog) {
        MomentEditorDialog(
            initialMoment = editingMoment,
            onDismiss = { showMomentDialog = false },
            onSave = { moment ->
                viewModel.upsertMoment(moment)
                showMomentDialog = false
            }
        )
    }

    if (showGroupDialog) {
        HabitGroupEditorDialog(
            initialGroup = editingGroup,
            onDismiss = { showGroupDialog = false },
            onSave = { group ->
                viewModel.upsertHabitGroup(group)
                showGroupDialog = false
            }
        )
    }
}

private suspend fun shareMarkdownFile(
    context: android.content.Context,
    fileName: String,
    markdown: String
) {
    val file = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        File(dir, fileName).apply {
            writeText(markdown, Charsets.UTF_8)
        }
    }
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/markdown"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, markdown)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Compartilhar Markdown"))
}

