# specs/01-domain-and-invariants.spec.md
version: 1.1.0
status: implemented
system: taskmgr (Android v0.0.1)

## 1. Domínio e Regras de Negócio

### 1.1 Momento (`Moment`)
- Representa uma janela temporal recorrente em dias da semana (`daysOfWeekMask` com bitmask `1..127` para `Segunda..Domingo`, gerenciado por `DaysOfWeekMask`).
- `startMinutesOfDay` e `endMinutesOfDay` são minutos desde `00:00` (`0..1439`).
- **Regra de Meia-Noite do Mesmo Dia:** Quando `endMinutesOfDay == 0` (`00:00`), o avaliador `MomentWindowEvaluator.effectiveEndMinutes` interpreta o fim como `1439` (`23:59` do mesmo dia). Exemplo: `06:00` até `00:00` cobre `06:00..23:59` do mesmo dia.
- **Restrição de Janela:** Não é permitido criar/editar um `Moment` onde `startMinutesOfDay > effectiveEndMinutes(endMinutesOfDay)` (ex.: início `22:00` e fim `04:00` é bloqueado na validação de `MomentEditorDialog`).
- Modos de lembrete (`ReminderMode`):
    1. `ONCE_AT_START`: dispara uma única vez em `startMinutesOfDay`.
    2. `INTERVAL_MINUTES`: repete a cada `intervalMinutes` (configurado na UI via campos `H:` e `M:`) dentro da janela.
    3. `SPECIFIC_TIMES`: dispara nos horários listados em `specificTimesMinutesOfDay`.
- `Moment` é opcional para `Task` e `Habit`. Quando associado, fornece janela padrão e política de lembretes, a menos que sobrescrito pelo item.

### 1.2 Grupo de Hábitos (`HabitGroup`)
- Agrupa hábitos visual e semanticamente (`id`, `name`, `colorArgb`, `iconGlyph`).
- Todo hábito associado a um `HabitGroup` renderiza seu ícone (`Habit.iconGlyph ?: HabitGroup.iconGlyph`) dentro de um fundo circular colorido com a cor `HabitGroup.colorArgb` (`HabitBadgeIcon`).
- A exclusão de um `HabitGroup` mantém os hábitos existentes, definindo `groupId = NULL` (`ForeignKey.SET_NULL`).

### 1.3 Tarefa (`Task`)
- Possui `startEpochDay` e `endEpochDay`, com invariante `endEpochDay >= startEpochDay`.
- Pode ter `scheduledMinutesOfDay` (`0..1439`), `momentId` (`Long?`) e/ou `repeatIntervalMinutes` (`Int? > 0`, configurado na UI com caixas `H:` e `M:`).
- Concluída quando `isCompleted == true`; ao concluir, nenhum alarme futuro do ciclo atual deve disparar.
- **Sincronização com Calendário:** Apenas `Task` é sincronizada com o `CalendarContract.Events` do Android/Google Agenda.

### 1.4 Hábito (`Habit`) & Retenção (`HabitStats`)
- Especialização recorrente contínua iniciando em `startEpochDay` (que pode ser retroativo ou antecipado via Calendário), sem data obrigatória de término.
- `activeDaysMask` define os dias da semana ativos do hábito (`1..127`).
- Possui `groupId: Long?` e `iconGlyph: String?` (selecionado via `HabitIconPickerDialog` com catálogo de 180+ ícones Nerd Font pesquisáveis por palavras-chave internas, sem exibir rótulos abaixo dos ícones).
- Modos de conclusão (`CompletionType`):
    1. `BOOLEAN`: `incrementAmount = 1.0`, `targetAmount = 1.0`, `incrementUnitLabel = "Sim"`, `targetUnitLabel = "Sim"`.
    2. `COUNT_PER_DAY`: `incrementAmount = 1.0`, `incrementUnitLabel = "Sim"`, `targetAmount >= 1.0` (inteiro positivo), `targetUnitLabel = "vezes"`.
    3. `CUSTOM_UNIT`: `incrementAmount > 0.0`, `targetAmount > 0.0`, `incrementUnitLabel` não vazio (ex.: `"+250 ml"`), `targetUnitLabel` não vazio (ex.: `"2000 ml"`).
- `HabitLog` registra o estado por `(habitId, epochDay)` (inclusive para dias passados editados retroativamente no calendário).
- `isCompleted` em `HabitLog` é verdadeiro se e somente se `accumulatedAmount + 1e-6 >= targetAmount`.
- `HabitStreakCalculator` calcula `HabitStats(currentStreakDays, longestStreakDays, totalCompletedDays)` avaliando apenas os dias a partir de `habit.startEpochDay` que pertencem a `habit.activeDaysMask`. Dias inativos não quebram a sequência.

### 1.5 Soneca (`SnoozeOption`)
Valores permitidos no enum `SnoozeOption`:
- `MINUTES_5` (`5m`)
- `MINUTES_10` (`10m`)
- `MINUTES_30` (`30m`)
- `HOURS_1` (`1h`)
- `HOURS_3` (`3h`)
- `UNTIL_TOMORROW` (`dismissedForDay = true` no dia atual; próximo disparo agendado para o próximo dia ativo).

### 1.6 Exportação, Sincronização e Importação
1. **Sincronizar calendário (`AndroidCalendarSyncManager`):** Sincroniza exclusivamente `Task` com `CalendarContract.Events` (bidirecional para tarefas já mapeadas em `calendar_sync_events` e push/delete automático quando ativo).
2. **Exportar/Importar `.taskmgr.md` (`MarkdownTransferEngine`):** Documento GitHub Flavored Markdown contendo frontmatter YAML (`format: taskmgr-v1`), checklists e tabelas GFM acompanhadas de blocos HTML `<!-- TASKMGR_* -->` codificados em Base64 URL-safe para restauração lossless de `Moment`, `HabitGroup`, `Task`, `Habit` e `HabitLog`.
3. **Exportar/Importar `.md` Notion/Obsidian (`MarkdownTransferEngine`):** Exporta documento GFM limpo (sem comentários HTML internos) e importa qualquer checklist GFM (`- [ ]` / `- [x]`) como `Task`.

## 2. Invariantes do Sistema
- INV-01: Um item já concluído (`Task.isCompleted == true` ou `HabitLog.isCompleted == true`) não dispara notificações de tela cheia para o ciclo concluído.
- INV-02: Se `HabitLog.dismissedForDay == true` no dia `D`, nenhum alarme de `Habit` para o dia `D` pode ser agendado ou disparado.
- INV-03: Edições em `HabitLog` para qualquer `epochDay` (inclusive dias retroativos no calendário) recalculam `isCompleted` e as métricas de sequência (`HabitStreakCalculator`) de forma determinística.
- INV-04: Em modo `aggressiveFullScreen == true` com `vibrationEnabled == true`, o disparo deve acionar `VibrationEffect` e `fullScreenIntent` para `FullScreenReminderActivity` com `showWhenLocked = true` e `turnScreenOn = true`.
- INV-05: Qualquer alteração de horário do sistema, fuso horário, boot do dispositivo ou alteração de `Moment`/`Task`/`Habit` dispara reconciliação idempotente dos alarmes pendentes.
- INV-06: Na UI (`HabitCard` e `RetroactiveHabitRow`), quando um hábito está concluído, exibe-se apenas o ícone verde `NerdIcons.CheckCircle` ao lado do título (sem pílula de texto que quebre linha).
