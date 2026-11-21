# specs/02-state-machines.spec.md
version: 1.1.2
status: implemented
system: taskmgr (Android v0.0.3)

## 1. Máquina de Estados — `Task` (por ciclo/instância)
Estados:
- `PENDING` (inicial)
- `SNOOZED`
- `COMPLETED` (terminal para o ciclo atual)
- `EXPIRED_WINDOW` (quando janela/prazo encerra sem conclusão)

Transições (`ReminderStateMachine`):
- `PENDING` --[onAlarmTriggered]--> `PENDING` (exibe `FullScreenReminderActivity` / notificação; reagenda próximo `repeatIntervalMinutes` se `isCompleted == false`)
- `PENDING` --[onComplete]--> `COMPLETED` (cancela alarmes pendentes da tarefa; remove notificação ativa; dispara auto-sync com calendário se habilitado)
- `PENDING` --[onSnooze(duration != UNTIL_TOMORROW)]--> `SNOOZED` (agenda alarme exato em `now + duration`; remove notificação ativa)
- `PENDING` --[onSnooze(UNTIL_TOMORROW)]--> `SNOOZED` (agenda para próximo dia válido; suprime disparos restantes no dia atual)
- `SNOOZED` --[onSnoozeTimerFired]--> `PENDING` (dispara alerta novamente)
- `SNOOZED` --[onComplete]--> `COMPLETED` (cancela timer de soneca)

## 2. Máquina de Estados — `HabitLog` (por `habitId` e `epochDay`)
Estados:
- `IN_PROGRESS` (`0.0 <= accumulatedAmount < targetAmount`, `dismissedForDay == false`, `isCompleted == false`)
- `SNOOZED_SHORT` (`accumulatedAmount < targetAmount`, `dismissedForDay == false`, `snoozedUntilEpochMillis != null`)
- `DISMISSED_FOR_DAY` (`dismissedForDay == true`, `isCompleted == false`)
- `COMPLETED` (`accumulatedAmount >= targetAmount`, `isCompleted == true`)

Transições (`ReminderStateMachine`):
- `IN_PROGRESS` --[onIncrement(delta = habit.incrementAmount)]-->
    - se novo `accumulatedAmount >= targetAmount`: `COMPLETED` (cancela alarmes do dia para o hábito)
    - senão: `IN_PROGRESS` (persiste novo `accumulatedAmount`)
- `IN_PROGRESS` --[onSnooze(MINUTES_5 | MINUTES_10 | MINUTES_30 | HOURS_1 | HOURS_3)]--> `SNOOZED_SHORT` (define `snoozedUntilEpochMillis`; agenda alarme exato)
- `IN_PROGRESS` --[onSnooze(UNTIL_TOMORROW)]--> `DISMISSED_FOR_DAY` (define `dismissedForDay = true`; cancela alarmes restantes do dia; agenda próximo dia ativo)
- `SNOOZED_SHORT` --[onSnoozeTimerFired]--> `IN_PROGRESS` (limpa `snoozedUntilEpochMillis`; dispara alerta se ainda não concluído)
- `SNOOZED_SHORT` --[onIncrement(delta)]--> `COMPLETED` se `accumulatedAmount >= targetAmount` senão `IN_PROGRESS`
- `COMPLETED` --[onEditCalendarDecrement / onUncomplete]--> `IN_PROGRESS` (recalcula `isCompleted = false` via `reduceHabitLogOnManualEdit`)
- `DISMISSED_FOR_DAY` --[onEditCalendarAdjust]--> `IN_PROGRESS` ou `COMPLETED` (permite edição retroativa exata da quantidade ou flag de dispensado no calendário)

## 3. Máquina de Estados — Navegação Principal & Editores Full-Screen (`MainScreen`)
Abas da barra inferior (`MainTab`):
1. `TODAY` ("Hoje")
2. `CALENDAR` ("Calendário")
3. `GROUPS` ("Grupos")
4. `MOMENTS` ("Momentos")
5. `SETTINGS` ("Config")

Editores de tela inteira (`ActiveEditor`):
- `null` (exibe `Scaffold` principal com `NavigationBar` e `AnimatedContent` entre as 5 abas)
- `ActiveEditor.TaskEditor(task: Task?)` (abre `TaskEditorScreen` em tela cheia com animação vertical/fade)
- `ActiveEditor.HabitEditor(habit: Habit?, defaultGroupId: Long?)` (abre `HabitEditorScreen` em tela cheia, pré-selecionando grupo/ícone quando iniciado a partir da aba `GROUPS`)

Subtelas de Configurações (`SettingsSection` em `MomentsAndSettingsScreen.kt`):
- `null` (menu principal com 4 cartões de navegação)
- `EXPORT_SYNC` ("Exportação, Sincronização e Importação")
- `PERMISSIONS` ("Permissões")
- `VIBRATION` ("Vibração")
- `ABOUT` ("Sobre" — Versão `0.0.3`, Criador `Jovi` `https://github.com/jovi-j/`)

## 4. Máquina de Estados — Sincronização de Calendário (`AndroidCalendarSyncManager`)
- **Precondição:** `READ_CALENDAR` e `WRITE_CALENDAR` concedidas + calendário gravável selecionado (`CALENDAR_ACCESS_LEVEL >= CAL_ACCESS_CONTRIBUTOR`).
- **Ciclo `syncTasksNow()`:**
    1. **Limpeza de órfãos:** Para cada registro em `calendar_sync_events` (`itemType = "TASK"`) cujo `itemId` não existe mais em `tasks`, remove o evento do `CalendarContract.Events` e apaga o vínculo.
    2. **Pull Bidirecional:** Se o hash local da `Task` (`contentHash`) é igual a `lastSyncedHash`, lê o evento remoto. Se o evento remoto foi editado no Google/Android Agenda (título, descrição, data ou horário), atualiza a `Task` no Room e recalcula `lastSyncedHash`.
    3. **Push Local:** Para tarefas novas (ou cujo evento remoto foi apagado/trocou de calendário), insere em `CalendarContract.Events` e salva `CalendarSyncEventEntity`. Para tarefas cujo `contentHash != lastSyncedHash`, atualiza o evento remoto existente.
