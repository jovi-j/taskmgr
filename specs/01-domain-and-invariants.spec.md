# specs/01-domain-and-invariants.spec.md
version: 1.0.0
status: draft
system: taskmgr (Android)

## 1. Domínio e Regras de Negócio

### 1.1 Momento (`Moment`)
- Representa uma janela temporal recorrente em dias da semana (`daysOfWeekMask` com bitmask `1..127` para `Segunda..Domingo`).
- `startMinutesOfDay` e `endMinutesOfDay` são minutos desde `00:00` (`0..1439`).
- Se `endMinutesOfDay < startMinutesOfDay`, a janela cruza meia-noite (`[startMinutesOfDay..1439]` no dia `D` e `[0..endMinutesOfDay]` no dia `D+1`).
- `Moment` é opcional para `Task` e `Habit`. Quando associado, fornece janela padrão e política de lembretes, a menos que sobrescrito pelo item.

### 1.2 Tarefa (`Task`)
- Possui `startEpochDay` e `endEpochDay`, com invariante `endEpochDay >= startEpochDay`.
- Pode ter `scheduledMinutesOfDay` (`0..1439`), `momentId` (`Long?`) e/ou `repeatIntervalMinutes` (`Int? > 0`).
- Concluída quando `isCompleted == true`; ao concluir, nenhum alarme futuro do ciclo atual deve disparar.

### 1.3 Hábito (`Habit`)
- Especialização recorrente contínua iniciando em `startEpochDay`, sem data obrigatória de término.
- `activeDaysMask` define os dias da semana ativos do hábito (`1..127`).
- Modos de conclusão (`CompletionType`):
    1. `BOOLEAN`: `incrementAmount = 1.0`, `targetAmount = 1.0`, `incrementUnitLabel = "Sim"`, `targetUnitLabel = "Sim"`.
    2. `COUNT_PER_DAY`: `incrementAmount = 1.0`, `incrementUnitLabel = "Sim"`, `targetAmount >= 1.0` (inteiro positivo), `targetUnitLabel = "Sim"`.
    3. `CUSTOM_UNIT`: `incrementAmount > 0.0`, `targetAmount > 0.0`, `incrementUnitLabel` não vazio (ex.: `"250 ml"` ou `"1 capítulo"`), `targetUnitLabel` não vazio (ex.: `"2000 ml"` ou `"10 capítulos"`).
- `HabitLog` registra o estado por `(habitId, epochDay)`.
- `isCompleted` em `HabitLog` é verdadeiro se e somente se `accumulatedAmount >= targetAmount` (com tolerância numérica `1e-6`).
- `dismissedForDay == true` indica que o usuário acionou soneca `"ATÉ_AMANHA"` para o `epochDay` corrente; o hábito não deve gerar novos alarmes agressivos neste `epochDay`.

### 1.4 Soneca (`SnoozeOption`)
Valores permitidos exclusivamente no enum `SnoozeDuration`:
- `MINUTES_5` (`5m`)
- `MINUTES_10` (`10m`)
- `MINUTES_30` (`30m`)
- `HOURS_1` (`1h`)
- `HOURS_3` (`3h`)
- `UNTIL_TOMORROW` (`dismissedForDay = true` no dia atual; próximo disparo agendado para o próximo dia ativo).

## 2. Invariantes do Sistema
- INV-01: Um item já concluído (`Task.isCompleted == true` ou `HabitLog.isCompleted == true`) não dispara notificações de tela cheia para o ciclo concluído.
- INV-02: Se `HabitLog.dismissedForDay == true` no dia `D`, nenhum alarme de `Habit` para o dia `D` pode ser agendado ou disparado.
- INV-03: Edições em `HabitLog` para qualquer `epochDay` (passado, presente ou futuro permitido) recalculam `isCompleted` de forma determinística (`accumulatedAmount >= targetAmount`).
- INV-04: Em modo `FULL_SCREEN_ALARM` com `vibrationEnabled == true`, o disparo deve acionar `VibrationEffect` e `fullScreenIntent` para `FullScreenReminderActivity` com `showWhenLocked = true` e `turnScreenOn = true`.
- INV-05: Qualquer alteração de horário do sistema, fuso horário, boot do dispositivo ou alteração de `Moment`/`Task`/`Habit` dispara reconciliação idempotente dos alarmes pendentes.