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

```

```markdown
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

```

```markdown
# specs/03-data-and-contracts.spec.md
version: 1.0.0
status: draft
system: taskmgr (Android)

## 1. Modelos de Domínio e Contratos Kotlin

```kotlin
package com.taskmgr.domain.model

enum class ReminderMode {
    ONCE_AT_START,
    INTERVAL_MINUTES,
    SPECIFIC_TIMES
}

enum class CompletionType {
    BOOLEAN,
    COUNT_PER_DAY,
    CUSTOM_UNIT
}

enum class SnoozeOption(val minutes: Long?, val untilTomorrow: Boolean) {
    MINUTES_5(5L, false),
    MINUTES_10(10L, false),
    MINUTES_30(30L, false),
    HOURS_1(60L, false),
    HOURS_3(180L, false),
    UNTIL_TOMORROW(null, true)
}

data class Moment(
    val id: Long = 0L,
    val name: String,
    val startMinutesOfDay: Int,
    val endMinutesOfDay: Int,
    val daysOfWeekMask: Int,
    val reminderMode: ReminderMode,
    val intervalMinutes: Int? = null,
    val specificTimesMinutesOfDay: List<Int> = emptyList(),
    val aggressiveFullScreen: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val isEnabled: Boolean = true
)

data class Task(
    val id: Long = 0L,
    val title: String,
    val notes: String? = null,
    val startEpochDay: Long,
    val endEpochDay: Long,
    val scheduledMinutesOfDay: Int? = null,
    val momentId: Long? = null,
    val repeatIntervalMinutes: Int? = null,
    val aggressiveFullScreen: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val isCompleted: Boolean = false,
    val snoozedUntilEpochMillis: Long? = null
)

data class Habit(
    val id: Long = 0L,
    val title: String,
    val notes: String? = null,
    val startEpochDay: Long,
    val activeDaysMask: Int,
    val momentId: Long? = null,
    val scheduledMinutesOfDay: Int? = null,
    val repeatIntervalMinutes: Int? = null,
    val aggressiveFullScreen: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val completionType: CompletionType,
    val incrementAmount: Double,
    val incrementUnitLabel: String,
    val targetAmount: Double,
    val targetUnitLabel: String,
    val isActive: Boolean = true
)

data class HabitLog(
    val habitId: Long,
    val epochDay: Long,
    val accumulatedAmount: Double,
    val isCompleted: Boolean,
    val dismissedForDay: Boolean,
    val snoozedUntilEpochMillis: Long? = null,
    val updatedAtEpochMillis: Long
)

data class FullScreenReminderPayload(
    val itemType: ReminderItemType,
    val itemId: Long,
    val epochDay: Long,
    val title: String,
    val momentName: String?,
    val progressText: String?,
    val incrementButtonLabel: String?,
    val vibrationEnabled: Boolean
)

enum class ReminderItemType {
    TASK,
    HABIT
}

```

## 2. Contratos de Repositório e Agendamento

```kotlin
package com.taskmgr.domain.contract

import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.HabitLog
import com.taskmgr.domain.model.Moment
import com.taskmgr.domain.model.SnoozeOption
import com.taskmgr.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface MomentRepository {
    fun observeMoments(): Flow<List<Moment>>
    suspend fun upsert(moment: Moment): Long
    suspend fun delete(momentId: Long)
}

interface TaskRepository {
    fun observeTasksForDay(epochDay: Long): Flow<List<Task>>
    suspend fun getById(taskId: Long): Task?
    suspend fun upsert(task: Task): Long
    suspend fun setCompleted(taskId: Long, completed: Boolean, completedAtEpochMillis: Long?)
    suspend fun snooze(taskId: Long, option: SnoozeOption, nowEpochMillis: Long)
}

interface HabitRepository {
    fun observeHabits(): Flow<List<Habit>>
    fun observeHabitLogsForDay(epochDay: Long): Flow<List<HabitLog>>
    fun observeHabitLogsForRange(startEpochDay: Long, endEpochDay: Long): Flow<List<HabitLog>>
    suspend fun getHabitById(habitId: Long): Habit?
    suspend fun getLog(habitId: Long, epochDay: Long): HabitLog?
    suspend fun upsertHabit(habit: Habit): Long
    suspend fun incrementHabit(habitId: Long, epochDay: Long, nowEpochMillis: Long): HabitLog
    suspend fun updateHabitLogManual(
        habitId: Long,
        epochDay: Long,
        accumulatedAmount: Double,
        dismissedForDay: Boolean,
        nowEpochMillis: Long
    ): HabitLog
    suspend fun snoozeHabit(habitId: Long, epochDay: Long, option: SnoozeOption, nowEpochMillis: Long): HabitLog
}

interface ReminderScheduler {
    suspend fun scheduleNextForTask(taskId: Long)
    suspend fun scheduleNextForHabit(habitId: Long, epochDay: Long)
    suspend fun cancelTaskReminders(taskId: Long)
    suspend fun cancelHabitRemindersForDay(habitId: Long, epochDay: Long)
    suspend fun reconcileAll(nowEpochMillis: Long)
}

interface FullScreenAlertDispatcher {
    fun canUseFullScreenIntent(): Boolean
    fun triggerFullScreenReminder(payload: FullScreenReminderPayload)
    fun dismissReminder(itemType: ReminderItemType, itemId: Long)
}

```

```

```markdown
# specs/04-acceptance-and-testing.spec.md
version: 1.0.0
status: draft
system: taskmgr (Android)

## 1. Cenários de Aceite (Given / When / Then)

### AC-01: Hábito com unidade customizável (ex.: água 250 ml / 2000 ml)
- **Given** um `Habit` `"Beber água"` com `completionType = CUSTOM_UNIT`, `incrementAmount = 250.0`, `incrementUnitLabel = "+250 ml"`, `targetAmount = 2000.0`, `targetUnitLabel = "2000 ml"`, e `HabitLog` inicial com `accumulatedAmount = 500.0`, `isCompleted = false`.
- **When** o usuário toca no botão de incremento na tela cheia de lembrete.
- **Then** `HabitLog.accumulatedAmount` passa a `750.0`, `isCompleted` permanece `false`, a tela cheia exibe `"750.0 / 2000.0 ml"`, e o botão de incremento continua exibindo `"+250 ml"`.

### AC-02: Conclusão automática ao atingir o objetivo do hábito
- **Given** um `Habit` com `targetAmount = 2000.0` e `HabitLog.accumulatedAmount = 1750.0`.
- **When** o usuário executa incremento de `250.0`.
- **Then** `HabitLog.accumulatedAmount` torna-se `2000.0`, `HabitLog.isCompleted` torna-se `true`, `cancelHabitRemindersForDay` é invocado, e a notificação/tela cheia é encerrada.

### AC-03: Soneca até o dia seguinte (`UNTIL_TOMORROW`)
- **Given** um `Habit` ativo hoje (`epochDay = D`) com `aggressiveFullScreen = true` e disparos recorrentes configurados.
- **When** o usuário seleciona a soneca `"Até amanhã"` na tela cheia.
- **Then** `HabitLog.dismissedForDay` no dia `D` torna-se `true`, `HabitLog.isCompleted` permanece `false`, alarmes restantes do dia `D` são cancelados, e o próximo alarme é agendado para o próximo dia ativo do hábito.

### AC-04: Sonecas de curta/média duração (`5m`, `10m`, `30m`, `1h`, `3h`)
- **Given** um lembrete disparado em `t0`.
- **When** o usuário seleciona `MINUTES_30`.
- **Then** um alarme exato é agendado para `t0 + 30 min`, mantendo o item não concluído.

### AC-05: Tela bloqueada, vibração e tela cheia
- **Given** dispositivo bloqueado/com tela desligada, `aggressiveFullScreen = true`, `vibrationEnabled = true` e `canUseFullScreenIntent() == true`.
- **When** o horário agendado ou momento ativo dispara o lembrete.
- **Then** o sistema dispara `VibrationEffect` e exibe `FullScreenReminderActivity` sobre a tela bloqueada com `turnScreenOn` ativo.

### AC-06: Edição retroativa no calendário
- **Given** o usuário abre um dia anterior `D - 2` no calendário onde um hábito ficou incompleto (`accumulatedAmount = 0.0`, `isCompleted = false`).
- **When** o usuário edita o registro desse dia para `accumulatedAmount = 2000.0` (ou marca como concluído).
- **Then** `HabitLog` de `(habitId, D - 2)` é atualizado com `isCompleted = true` e `updatedAtEpochMillis` atualizado, refletindo imediatamente na visão do calendário.

## 2. Matriz de Testes Obrigatória
- **Testes Unitários (`src/test`):**
  - Avaliação de janelas de `Moment` normais e cruzando meia-noite.
  - Cálculo de `isCompleted` em `BOOLEAN`, `COUNT_PER_DAY` e `CUSTOM_UNIT` (incluindo unidades distintas como `1 capítulo` até `10 capítulos`).
  - Transições da máquina de estados de `Task` e `HabitLog` para todas as opções de `SnoozeOption`.
- **Testes de Integração Room (`src/androidTest`):**
  - Persistência de `Moment`, `Task`, `Habit` e `HabitLog`, chaves compostas `(habitId, epochDay)`, `ON DELETE SET NULL` em `momentId` e `CASCADE` em `habit_logs`.
  - Consulta de intervalos de datas para renderização do calendário e edição retroativa.
- **Testes de UI / Instrumentados (`src/androidTest`):**
  - Renderização de `FullScreenReminderActivity` exibindo progresso do hábito, botões de incremento/conclusão e todas as 6 opções de soneca (`5m`, `10m`, `30m`, `1h`, `3h`, `Até amanhã`).