# specs/02-state-machines.spec.md
version: 1.0.0
status: draft
system: taskmgr (Android)

## 1. Máquina de Estados — `Task` (por ciclo/instância)
Estados:
- `PENDING` (inicial)
- `SNOOZED`
- `COMPLETED` (terminal para o ciclo atual)
- `EXPIRED_WINDOW` (quando janela/prazo encerra sem conclusão)

Transições:
- `PENDING` --[onAlarmTriggered]--> `PENDING` (exibe `FullScreenReminderActivity` / notificação; reagenda próximo `repeatIntervalMinutes` se `isCompleted == false`)
- `PENDING` --[onComplete]--> `COMPLETED` (cancela alarmes pendentes da tarefa; remove notificação ativa)
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

Transições:
- `IN_PROGRESS` --[onIncrement(delta = habit.incrementAmount)]-->
    - se novo `accumulatedAmount >= targetAmount`: `COMPLETED` (cancela alarmes do dia para o hábito)
    - senão: `IN_PROGRESS` (persiste novo `accumulatedAmount`)
- `IN_PROGRESS` --[onSnooze(MINUTES_5 | MINUTES_10 | MINUTES_30 | HOURS_1 | HOURS_3)]--> `SNOOZED_SHORT` (define `snoozedUntilEpochMillis`; agenda alarme exato)
- `IN_PROGRESS` --[onSnooze(UNTIL_TOMORROW)]--> `DISMISSED_FOR_DAY` (define `dismissedForDay = true`; cancela alarmes restantes do dia; agenda próximo dia ativo)
- `SNOOZED_SHORT` --[onSnoozeTimerFired]--> `IN_PROGRESS` (limpa `snoozedUntilEpochMillis`; dispara alerta se ainda não concluído)
- `SNOOZED_SHORT` --[onIncrement(delta)]--> `COMPLETED` se `accumulatedAmount >= targetAmount` senão `IN_PROGRESS`
- `COMPLETED` --[onEditCalendarDecrement / onUncomplete]--> `IN_PROGRESS` (recalcula `isCompleted = false` e pode reagendar conforme regra do dia)
- `DISMISSED_FOR_DAY` --[onEditCalendarAdjust]--> `IN_PROGRESS` ou `COMPLETED` (limpa `dismissedForDay = false` e recalcula `isCompleted`)

## 3. Máquina de Estados — Disparo Full-Screen & Permissões
Estados da permissão `USE_FULL_SCREEN_INTENT`:
- `GRANTED`
- `DENIED_OR_REVOKED`

Transições:
- `DENIED_OR_REVOKED` --[onRequestPermission / onReturnFromSettings]--> `GRANTED` se concedida, senão `DENIED_OR_REVOKED` (exibe aviso explícito de que tela cheia sobre lockscreen requer permissão ativa nas configurações do sistema).
- Qualquer disparo com `aggressiveFullScreen == true` e `GRANTED` deve iniciar notificação de canal `IMPORTANCE_HIGH` com `fullScreenIntent` e lançar `FullScreenReminderActivity`.