# Visão Geral & Arquitetura — taskmgr

**taskmgr** (`v0.3.1`, pacote `com.jovij.taskmgr`) é um aplicativo nativo Android (Kotlin + Jetpack Compose + Material 3 + Jetpack Glance) projetado para gerenciamento de tarefas, hábitos com unidades customizadas, janelas temporais ("Momentos"), detector inteligente de linguagem natural em títulos, widgets interativos na tela inicial, lembretes persistentes em tela cheia sobre a tela bloqueada e interoperabilidade via GitHub Flavored Markdown e Google/Android Agenda.

---

## 1. Estrutura de Pacotes

- **`com.jovij.taskmgr`** (`app/src/main/java/com/jovij/taskmgr`):
    - `TaskMgrApplication.kt`: Inicializa o container de dependências (`AppContainer`) e canais de notificação.
    - `MainActivity.kt`: Ponto de entrada Compose com suporte a tema escuro OLED e deep links vindos dos widgets (`EXTRA_OPEN_TASK_ID`, `EXTRA_OPEN_HABIT_ID`).
    - `ui.theme`: Paleta OLED (`Color.kt`), tipografia com fontes Nerd Font (`Type.kt`) e esquema Material 3 (`Theme.kt`).
- **`com.taskmgr.domain`** (`app/src/main/java/com/taskmgr/domain`):
    - `model/DomainModels.kt`: Modelos imutáveis (`Moment`, `DefaultMoments`, `HabitGroup`, `Task`, `TaskAdvanceReminder`, `TaskUrgency`, `Habit`, `HabitLog`, `SnoozeOption`, `FullScreenReminderPayload`).
    - `contract/Contracts.kt`: Interfaces de repositório e agendamento.
    - `logic/`: Regras puras em Kotlin (`DaysOfWeekMask`, `HabitCompletionCalculator`, `HabitStreakCalculator`, `MomentWindowEvaluator`, `NextAlarmCalculator`, `ReminderStateMachine`, `SmartTitleParser`).
    - `export/MarkdownTransferEngine.kt`: Motor puro de exportação e importação para `.taskmgr.md` (backup completo lossless) e `.md` (GitHub Flavored Markdown para Notion e Obsidian).
- **`com.taskmgr.data`** (`app/src/main/java/com/taskmgr/data`):
    - `local/`: Banco de dados Room v6 (`TaskMgrDatabase`, `Entities.kt`, `Daos.kt`, `Mappers.kt`) com migrações `MIGRATION_1_2` a `MIGRATION_5_6` e seed automático dos 4 momentos padrões (`DefaultMoments`).
    - `repository/RepositoriesImpl.kt`: Implementações reativas (`Flow`) dos repositórios integradas ao `ReminderScheduler`.
    - `sync/AndroidCalendarSyncManager.kt`: Sincronização bidirecional de tarefas (`Task`) com o provedor nativo de agenda do Android (`CalendarContract.Events`).
- **`com.taskmgr.scheduler`** (`app/src/main/java/com/taskmgr/scheduler`):
    - `ReminderSchedulerImpl.kt`: Cálculo e registro de alarmes exatos via `AlarmManager` (incluindo lembretes antecipados `TaskAdvanceReminder`).
    - `FullScreenAlertDispatcherImpl.kt`: Disparo de notificações `IMPORTANCE_HIGH` com `fullScreenIntent`.
    - `VibrationController.kt`: Controle de padrões de vibração (`STANDARD`, `PULSE`, `URGENT`).
    - `PermissionHelper.kt`: Diagnóstico e intents para permissões críticas do Android 13/14+ e fabricantes (MIUI/HyperOS).
- **`com.taskmgr.ui`** (`app/src/main/java/com/taskmgr/ui`):
    - `MainScreen.kt`: `Scaffold` principal com 5 abas na barra inferior (`Hoje`, `Calendário`, `Matriz`, `Grupos`, `Momentos`), botão de `Config` na `TopAppBar` e transições animadas (`AnimatedContent`).
    - `viewmodel/TaskMgrViewModel.kt`: Gerenciamento de estado reativo (`StateFlow`) e sincronização com widgets Glance.
    - `screens/`: Telas do aplicativo (`TodayScreen`, `CalendarScreen`, `EisenhowerMatrixScreen`, `HabitGroupsScreen`, `MomentsScreen`, `MomentsAndSettingsScreen`, `ItemEditorScreens`).
    - `components/`: Componentes reutilizáveis (`NerdFontIcons`, `HabitGroupComponents`, `TimePickerComponents`, `ItemEditorDialogs`, `OnboardingPermissionsDialog`).
    - `fullscreen/FullScreenReminderActivity.kt`: Activity que acende a tela sobre a lockscreen para conclusão rápida, reconhecimento de lembrete antecipado ou soneca.
- **`com.taskmgr.widget`** (`app/src/main/java/com/taskmgr/widget`):
    - `HabitGlanceWidget.kt`, `HabitWidgetConfigureActivity.kt`, `HabitWidgetState.kt`, `HabitWidgetIconRenderer.kt`: Widget 2x2 de Hábito com incremento direto e calendário mensal do hábito.
    - `CalendarTasksGlanceWidget.kt`, `CalendarWidgetCalculator.kt`, `CalendarWidgetState.kt`: Widget 4x2 de Calendário de Tarefas com navegação mensal, seleção de dia, indicadores de tarefas futuras, conclusão via checkbox e abertura direta da tarefa no app.

---

## 2. Design System OLED Dark

O aplicativo utiliza uma paleta **Focus OLED Dark** para máxima economia de bateria em telas AMOLED e alto contraste visual:

| Papel na UI | Código Hex | Constante (`Color.kt`) |
| :--- | :---: | :--- |
| Fundo Principal (Background) | `#000000` | `OledBackground` |
| Superfície 1 (Cards / Tarefas) | `#0E1117` | `OledSurface` |
| Superfície 2 (Menus / Modais) | `#161B26` | `OledSurfaceElevated` |
| Bordas e Divisores (`1.dp`) | `#1F2937` | `OledBorder` |
| Primária (Destaque Índigo) | `#818CF8` | `IndigoPrimary` |
| Primária Container | `#1E1B4B` | `IndigoContainer` |
| Texto Principal | `#F8FAFC` | `TextPrimary` |
| Texto Secundário | `#94A3B8` | `TextSecondary` |
| Texto Desabilitado / Concluído | `#475569` | `TextDisabled` |
| Tarefa / Hábito Concluído | `#34D399` | `TaskCompleted` |
