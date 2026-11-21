# specs/01-domain-and-invariants.spec.md
version: 1.4.0
status: implemented
system: taskmgr (Android v0.3.0)

## 1. Domínio e Regras de Negócio

### 1.1 Momento (`Moment`) & Momentos Padrões (`DefaultMoments`)
- Representa uma janela temporal recorrente em dias da semana (`daysOfWeekMask` com bitmask `1..127` para `Segunda..Domingo`, gerenciado por `DaysOfWeekMask`).
- `startMinutesOfDay` e `endMinutesOfDay` são minutos desde `00:00` (`0..1439`).
- **4 Momentos Padrões (`DefaultMoments.ALL`):** Semeados automaticamente no banco (`TaskMgrDatabase`) na criação e na migração `MIGRATION_5_6`, todos com `daysOfWeekMask = 0b1111111`, `reminderMode = INTERVAL_MINUTES`, `intervalMinutes = 60` (`1h`), `aggressiveFullScreen = true`, `vibrationEnabled = true`:
    1. **Manhã:** `06:00` (`360`) às `12:00` (`720`)
    2. **Tarde:** `12:00` (`720`) às `18:00` (`1080`)
    3. **Noite:** `18:00` (`1080`) às `22:00` (`1320`)
    4. **Todo o dia:** `06:00` (`360`) às `22:00` (`1320`)
- **Regra de Meia-Noite do Mesmo Dia:** Quando `endMinutesOfDay == 0` (`00:00`), o avaliador `MomentWindowEvaluator.effectiveEndMinutes` interpreta o fim como `1439` (`23:59` do mesmo dia). Exemplo: `06:00` até `00:00` cobre `06:00..23:59` do mesmo dia.
- **Restrição de Janela:** Não é permitido criar/editar um `Moment` onde `startMinutesOfDay > effectiveEndMinutes(endMinutesOfDay)` (ex.: início `22:00` e fim `04:00` é bloqueado na validação de `MomentEditorDialog`).
- Modos de lembrete (`ReminderMode`):
    1. `ONCE_AT_START`: dispara uma única vez em `startMinutesOfDay`.
    2. `INTERVAL_MINUTES`: repete a cada `intervalMinutes` (configurado na UI via campos `Hora(s)` e `Minuto(s)`) dentro da janela.
    3. `SPECIFIC_TIMES`: dispara nos horários listados em `specificTimesMinutesOfDay`.
- `Moment` é opcional para `Task` e `Habit`. Ao criar um novo `Habit` sem período explícito, o editor assume o momento **"Todo o dia"** caso ele exista (`SmartTitleParser.findDefaultHabitMomentId`).

### 1.2 Grupo de Hábitos (`HabitGroup`)
- Agrupa hábitos visual e semanticamente (`id`, `name`, `colorArgb`, `iconGlyph`) com paleta de cores suaves (muted) para telas OLED.
- Todo hábito associado a um `HabitGroup` renderiza seu ícone (`Habit.iconGlyph ?: HabitGroup.iconGlyph`) dentro de um fundo circular colorido com a cor `HabitGroup.colorArgb` (`HabitBadgeIcon`).
- A exclusão de um `HabitGroup` mantém os hábitos existentes, definindo `groupId = NULL` (`ForeignKey.SET_NULL`).

### 1.3 Tarefa (`Task`), Recorrência, Lembretes Antecipados & Matriz de Eisenhower (`TaskUrgency`)
- Possui `startEpochDay` e `endEpochDay`:
    - `startEpochDay == Task.UNSCHEDULED_EPOCH_DAY` (`-1L`) indica uma tarefa sem data (na **Caixa de entrada**, elegível ao modal **"Planejar o dia"**).
    - `activeDaysMask: Int` (`1..127`, padrão `0b1111111`) define em quais dias da semana a tarefa ocorre.
    - `hasEndDate: Boolean`: quando `false`, `endEpochDay` recebe `Task.OPEN_ENDED_EPOCH_DAY` (`365_242L`) para tarefas com frequência semanal recorrente sem data fim, permanecendo ativa nos dias de `activeDaysMask` até que o usuário conclua a tarefa (`isCompleted = true`). Quando `hasEndDate == true`, a tarefa encerra em `endEpochDay`.
- Pode ter `scheduledMinutesOfDay` (`0..1439`), `momentId` (`Long?`), `repeatIntervalMinutes` (`Int? > 0`) e `advanceReminders: List<TaskAdvanceReminder>` (lembretes antecipados com `daysBefore`, `hoursBefore`, `minutesBefore` e `repeatIntervalMinutes`).
- Pertence a um dos 4 quadrantes da **Matriz de Eisenhower** (`TaskUrgency`):
    1. `VERY_HIGH`: **Importante e Urgente (Urgência Muito Alta) — Vermelho (`0xFFEF4444L`)**
    2. `HIGH`: **Importante e Não Urgente (Urgência Alta) — Amarelo (`0xFFFBBF24L`)**
    3. `MEDIUM`: **Não Importante e Urgente (Urgência Média) — Verde (`0xFF22C55EL`)**
    4. `LOW`: **Não Importante e Não Urgente (Urgência Baixa) — Azul (`0xFF3B82F6L`)** *(padrão ao criar uma nova tarefa)*
- Na aba **Matriz**, `shouldShowInEisenhowerMatrix(todayEpochDay)` exibe apenas tarefas não concluídas (`!isCompleted`) ou tarefas concluídas no dia de hoje (`isCompletedOnDay(todayEpochDay)`).
- Concluída quando `isCompleted == true` (registrando `completedAtEpochMillis`); ao concluir, nenhum alarme futuro do ciclo atual deve disparar.
- **Sincronização com Calendário:** Apenas `Task` (com data definida) é sincronizada com o `CalendarContract.Events` do Android/Google Agenda.

### 1.4 Hábito (`Habit`) & Retenção (`HabitStats`)
- Especialização recorrente contínua iniciando em `startEpochDay` (que pode ser retroativo ou antecipado via Calendário), sem data obrigatória de término.
- `activeDaysMask` define os dias da semana ativos do hábito (`1..127`).
- Possui `groupId: Long?` e `iconGlyph: String?` (selecionado via `HabitIconPickerDialog` com catálogo de 180+ ícones Nerd Font pesquisáveis por palavras-chave internas).
- Modos de conclusão (`CompletionType`):
    1. `BOOLEAN`: `incrementAmount = 1.0`, `targetAmount = 1.0`, `incrementUnitLabel = "Sim"`, `targetUnitLabel = "Sim"`.
    2. `COUNT_PER_DAY`: `incrementAmount = 1.0`, `incrementUnitLabel = "Sim"`, `targetAmount >= 1.0` (inteiro positivo), `targetUnitLabel = "vezes"`.
    3. `CUSTOM_UNIT`: `incrementAmount > 0.0`, `targetAmount > 0.0`, `incrementUnitLabel` não vazio (ex.: `"+250 ml"`), `targetUnitLabel` não vazio (ex.: `"2500 ml"`).
- `HabitLog` registra o estado por `(habitId, epochDay)` (inclusive para dias passados editados retroativamente no calendário).
- `isCompleted` em `HabitLog` é verdadeiro se e somente se `accumulatedAmount + 1e-6 >= targetAmount`.
- `HabitStreakCalculator` calcula `HabitStats(currentStreakDays, longestStreakDays, totalCompletedDays)` avaliando apenas os dias a partir de `habit.startEpochDay` que pertencem a `habit.activeDaysMask`. Dias inativos não quebram a sequência.

### 1.5 Detector de Texto Inteligente em Títulos (`SmartTitleParser`)
- Opera com normalização 1-para-1 preservando índices (`normalizePreservingLength`, case-insensitive e sem acentos):
    - `START_DATE`: `"DD/MM/YYYY"`, `"DD/MM"`, `"hoje"`, `"amanhã"`, `"depois de amanhã"`, `"ontem"`.
    - `END_DATE` (Tarefas): `"até [data]"`.
    - `EXACT_TIME`: `"às HH:mm"`, `"ás HH:mm"`, `"HH:mm"`, `"às HHh[mm]"`.
    - `PERIOD` (Hábitos): `"pela manhã"`, `"pela tarde"`, `"pela noite"`, `"todo o dia"`.
    - `TASK_WEEKDAYS` (Tarefas): `"toda [segunda|terça|quarta|quinta|sexta|sábado|domingo, ...]"`.
    - `HABIT_FREQUENCY` (Hábitos): `"[N]x ao dia"`, `"[N] vezes ao dia"`, ou `"[inc] [unid] de [meta] [unid] ao dia"`.
    - `URGENCY` (Tarefas): `"urgencia [muito alta|alta|média|baixa]"`.
    - `GROUP` (Hábitos): `"grupo [nome do grupo]"`.
- Quando o usuário pressiona **Backspace/Delete** exatamente no fim (`endIndex`) de um `SmartToken` ativo (`processTextEdit`), o texto literal é restaurado integralmente no campo, o token passa para `dismissedTokens`, o destaque azul é removido e o atributo correspondente da tarefa/hábito reverte ao valor manual/padrão.

### 1.6 Soneca (`SnoozeOption`)
Valores permitidos no enum `SnoozeOption`:
- `MINUTES_5` (`5m`), `MINUTES_10` (`10m`), `MINUTES_30` (`30m`), `HOURS_1` (`1h`), `HOURS_3` (`3h`), `UNTIL_TOMORROW` (`dismissedForDay = true` no dia atual; próximo disparo agendado para o próximo dia ativo).

### 1.7 Exportação, Sincronização, Importação e Widgets Glance
1. **Sincronizar calendário (`AndroidCalendarSyncManager`):** Sincroniza exclusivamente `Task` com `CalendarContract.Events` (bidirecional para tarefas já mapeadas em `calendar_sync_events` e push/delete automático quando ativo).
2. **Exportar/Importar `.taskmgr.md` (`MarkdownTransferEngine`):** Documento GitHub Flavored Markdown contendo frontmatter YAML (`format: taskmgr-v1`), checklists e tabelas GFM acompanhadas de blocos HTML `<!-- TASKMGR_* -->` codificados em Base64 URL-safe para restauração lossless de `Moment`, `HabitGroup`, `Task`, `Habit` e `HabitLog`.
3. **Exportar/Importar `.md` Notion/Obsidian (`MarkdownTransferEngine`):** Exporta documento GFM limpo e importa qualquer checklist GFM (`- [ ]` / `- [x]`) como `Task`.
4. **Widgets Glance (`HabitGlanceWidget` 2x2 e `CalendarTasksGlanceWidget` 4x2):** Refletem e alteram diretamente o estado do Room via `AppContainer`, mantendo sincronia imediata com os alarmes e com a UI do aplicativo.

## 2. Invariantes do Sistema
- INV-01: Um item já concluído (`Task.isCompleted == true` ou `HabitLog.isCompleted == true`) ou sem data (`!Task.hasDate`) não dispara notificações de tela cheia para o ciclo concluído.
- INV-02: Se `HabitLog.dismissedForDay == true` no dia `D`, nenhum alarme de `Habit` para o dia `D` pode ser agendado ou disparado.
- INV-03: Edições em `HabitLog` para qualquer `epochDay` (inclusive dias retroativos no calendário ou via widget) recalculam `isCompleted` e as métricas de sequência (`HabitStreakCalculator`) de forma determinística.
- INV-04: Em modo `aggressiveFullScreen == true` com `vibrationEnabled == true`, o disparo deve acionar `VibrationEffect` e `fullScreenIntent` para `FullScreenReminderActivity` com `showWhenLocked = true` e `turnScreenOn = true`.
- INV-05: Qualquer alteração de horário do sistema, fuso horário, boot do dispositivo ou alteração de `Moment`/`Task`/`Habit` dispara reconciliação idempotente dos alarmes pendentes e atualização dos widgets Glance.
- INV-06: Na UI (`HabitCard` e `RetroactiveHabitRow`), quando um hábito está concluído, exibe-se apenas o ícone verde `NerdIcons.CheckCircle` ao lado do título (sem pílula de texto que quebre linha).
- INV-07: Em `SmartTitleParser.normalizePreservingLength(text)`, a string normalizada possui exatamente o mesmo comprimento (`length`) que `text`, garantindo mapeamento 1-para-1 dos intervalos (`startIndex..endIndex`) entre o texto com acentos e os tokens detectados.
