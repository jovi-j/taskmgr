# specs/02-state-machines.spec.md
version: 1.4.0
status: implemented
system: taskmgr (Android v0.3.2)

## 1. Máquina de Estados — `Task` (por ciclo/instância)
Estados:
- `UNSCHEDULED_INBOX` (`startEpochDay == -1L`, sem data definida)
- `PENDING` (agendada para `startEpochDay..endEpochDay` nos dias de `activeDaysMask`, ou sem data fim com `hasEndDate == false`)
- `SNOOZED`
- `COMPLETED` (terminal para a tarefa; registra `completedAtEpochMillis`)
- `EXPIRED_WINDOW` (quando `hasEndDate == true` e a janela/prazo encerra sem conclusão)

Transições (`ReminderStateMachine`):
- `UNSCHEDULED_INBOX` --[onPlanDayAssign(todayEpochDay)]--> `PENDING` (delega a tarefa para o dia de hoje com urgência escolhida)
- `PENDING` --[onAdvanceAlarmTriggered]--> `PENDING` (exibe `FullScreenReminderActivity` em modo `acknowledgeOnly = true` / `showSnoozeOptions = false`; agenda próximo lembrete antecipado ou o horário principal)
- `PENDING` --[onAlarmTriggered]--> `PENDING` (exibe `FullScreenReminderActivity` / notificação; reagenda próximo `repeatIntervalMinutes` ou próximo dia ativo em `activeDaysMask` se `isCompleted == false`)
- `PENDING` --[onComplete (App / Notificação / Widget 4x2)]--> `COMPLETED` (cancela alarmes pendentes da tarefa; remove notificação ativa; atualiza widget Glance; dispara auto-sync com calendário se habilitado)
- `PENDING` --[onSnooze(duration != UNTIL_TOMORROW)]--> `SNOOZED` (agenda alarme exato em `now + duration`; remove notificação ativa)
- `PENDING` --[onSnooze(UNTIL_TOMORROW)]--> `SNOOZED` (agenda para próximo dia válido em `activeDaysMask`; suprime disparos restantes no dia atual)
- `SNOOZED` --[onSnoozeTimerFired]--> `PENDING` (dispara alerta novamente)
- `SNOOZED` --[onComplete]--> `COMPLETED` (cancela timer de soneca)

## 2. Máquina de Estados — `HabitLog` (por `habitId` e `epochDay`)
Estados:
- `IN_PROGRESS` (`0.0 <= accumulatedAmount < targetAmount`, `dismissedForDay == false`, `isCompleted == false`)
- `SNOOZED_SHORT` (`accumulatedAmount < targetAmount`, `dismissedForDay == false`, `snoozedUntilEpochMillis != null`)
- `DISMISSED_FOR_DAY` (`dismissedForDay == true`, `isCompleted == false`)
- `COMPLETED` (`accumulatedAmount >= targetAmount`, `isCompleted == true`)

Transições (`ReminderStateMachine`):
- `IN_PROGRESS` --[onIncrement(delta = habit.incrementAmount) via App / Alarme / Widget 2x2]-->
    - se novo `accumulatedAmount >= targetAmount`: `COMPLETED` (cancela alarmes do dia para o hábito e atualiza widget Glance)
    - senão: `IN_PROGRESS` (persiste novo `accumulatedAmount` e atualiza widget Glance)
- `IN_PROGRESS` --[onSnooze(MINUTES_5 | MINUTES_10 | MINUTES_30 | HOURS_1 | HOURS_3)]--> `SNOOZED_SHORT` (define `snoozedUntilEpochMillis`; agenda alarme exato)
- `IN_PROGRESS` --[onSnooze(UNTIL_TOMORROW)]--> `DISMISSED_FOR_DAY` (define `dismissedForDay = true`; cancela alarmes restantes do dia; agenda próximo dia ativo)
- `SNOOZED_SHORT` --[onSnoozeTimerFired]--> `IN_PROGRESS` (limpa `snoozedUntilEpochMillis`; dispara alerta se ainda não concluído)
- `SNOOZED_SHORT` --[onIncrement(delta)]--> `COMPLETED` se `accumulatedAmount >= targetAmount` senão `IN_PROGRESS`
- `COMPLETED` --[onEditCalendarDecrement / onUncomplete]--> `IN_PROGRESS` (recalcula `isCompleted = false` via `reduceHabitLogOnManualEdit`)
- `DISMISSED_FOR_DAY` --[onEditCalendarAdjust]--> `IN_PROGRESS` ou `COMPLETED` (permite edição retroativa exata da quantidade ou flag de dispensado no calendário)

## 3. Máquina de Estados — Navegação Principal & Editores Full-Screen (`MainScreen`)
Abas da barra inferior (`MainTab`):
1. `CALENDAR` ("Calendário")
2. `MATRIX` ("Matriz" — Matriz de Eisenhower 2x2)
3. `TODAY` ("Hoje" — Botão circular central com cores invertidas e ícone `nf-weather-day_sunny`)
4. `GROUPS` ("Grupos")
5. `MOMENTS` ("Momentos")

Menu de Configurações (`TopAppBar` -> `showSettingsScreen = true`):
- `null` (menu principal com 4 cartões de navegação)
- `EXPORT_SYNC` ("Exportação, Sincronização e Importação")
- `PERMISSIONS` ("Permissões")
- `VIBRATION` ("Vibração")
- `ABOUT` ("Sobre" — Versão dinâmica via `PackageInfo.versionName`, Criador `Jovi` `https://github.com/jovi-j/`)

Editores de tela inteira (`ActiveEditor` em `MainScreen.kt` e modais em `ItemEditorScreens.kt`):
- `null` (exibe `Scaffold` principal com `NavigationBar` e `AnimatedContent` entre as 5 abas)
- `ActiveEditor.TaskEditor(task: Task?, defaultUrgency: TaskUrgency?)` (abre `TaskEditorScreen` com cartão *seamless* Título/Detalhes, detector `SmartTitleParser` e barra horizontal de ícones que abrem `TaskEditorModalSection`)
- `ActiveEditor.HabitEditor(habit: Habit?, defaultGroupId: Long?)` (abre `HabitEditorScreen` com cartão *seamless* Título/Detalhes, detector `SmartTitleParser`, pré-seleção do momento `"Todo o dia"` e barra horizontal de ícones que abrem `HabitEditorModalSection`)

## 4. Máquina de Estados — Sincronização de Calendário (`AndroidCalendarSyncManager`)
- **Precondição:** `READ_CALENDAR` e `WRITE_CALENDAR` concedidas + calendário gravável selecionado (`CALENDAR_ACCESS_LEVEL >= CAL_ACCESS_CONTRIBUTOR`).
- **Ciclo `syncTasksNow()`:**
    1. **Limpeza de órfãos:** Para cada registro em `calendar_sync_events` (`itemType = "TASK"`) cujo `itemId` não existe mais em `tasks` (ou deixou de ter data), remove o evento do `CalendarContract.Events` e apaga o vínculo.
    2. **Pull Bidirecional:** Se o hash local da `Task` (`contentHash`) é igual a `lastSyncedHash`, lê o evento remoto. Se o evento remoto foi editado no Google/Android Agenda (título, descrição, data ou horário), atualiza a `Task` no Room e recalcula `lastSyncedHash`.
    3. **Push Local:** Para tarefas novas com data definida (ou cujo evento remoto foi apagado/trocou de calendário), insere em `CalendarContract.Events` e salva `CalendarSyncEventEntity`. Para tarefas cujo `contentHash != lastSyncedHash`, atualiza o evento remoto existente.
