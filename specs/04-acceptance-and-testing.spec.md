# specs/04-acceptance-and-testing.spec.md
version: 1.4.0
status: implemented
system: taskmgr (Android v0.3.3)

## 1. Cenários de Aceite (Given / When / Then)

### AC-01: Hábito com unidade customizável (ex.: água 250 ml / 2500 ml)
- **Given** um `Habit` `"Beber água"` com `completionType = CUSTOM_UNIT`, `incrementAmount = 250.0`, `incrementUnitLabel = "+250 ml"`, `targetAmount = 2500.0`, `targetUnitLabel = "2500 ml"`, e `HabitLog` inicial com `accumulatedAmount = 500.0`, `isCompleted = false`.
- **When** o usuário toca no botão de incremento (no app ou no widget 2x2).
- **Then** `HabitLog.accumulatedAmount` passa a `750.0`, `isCompleted` permanece `false`, a UI exibe `"750 / 2500 ml"`, e o botão de incremento exibe `"+250 ml"`.

### AC-02: Conclusão automática ao atingir o objetivo do hábito e badge verde compacto
- **Given** um `Habit` com `targetAmount = 2000.0` e `HabitLog.accumulatedAmount = 1750.0`.
- **When** o usuário executa incremento de `250.0`.
- **Then** `HabitLog.accumulatedAmount` torna-se `2000.0`, `HabitLog.isCompleted` torna-se `true`, `cancelHabitRemindersForDay` é invocado, e o cartão do hábito exibe o ícone verde `NerdIcons.CheckCircle` ao lado do título sem quebrar linha.

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

### AC-06: Edição retroativa no calendário e cálculo de sequência (`HabitStreakCalculator`)
- **Given** o usuário abre um dia anterior `D - 2` no calendário onde um hábito ficou incompleto (`accumulatedAmount = 0.0`, `isCompleted = false`).
- **When** o usuário edita o registro desse dia para `accumulatedAmount = 2000.0` (ou antecipa `startEpochDay` para incluir o hábito em um dia anterior).
- **Then** `HabitLog` de `(habitId, D - 2)` é atualizado com `isCompleted = true` e as métricas de sequência atual (`currentStreakDays`), recorde (`longestStreakDays`) e total de dias (`totalCompletedDays`) são recalculadas imediatamente.

### AC-07: Fim de janela às `00:00` no mesmo dia e bloqueio de início posterior ao fim
- **Given** um `Moment` configurado com `startMinutesOfDay = 360` (`06:00`) e `endMinutesOfDay = 0` (`00:00`).
- **When** `MomentWindowEvaluator` avalia a janela.
- **Then** `00:00` é tratado como `23:59` (`1439`) do mesmo dia, e o editor impede salvar janelas em que `startMinutesOfDay > effectiveEndMinutes(endMinutesOfDay)` (ex.: `22:00` até `04:00`).

### AC-08: Exportação e Importação `.taskmgr.md` (Round-trip Lossless) e `.md` (Notion/Obsidian)
- **Given** um snapshot com `Moment`, `HabitGroup`, `Task`, `Habit` e `HabitLog`.
- **When** exportado via `MarkdownTransferEngine.exportTaskMgrMarkdown` e reimportado via `parseTaskMgrMarkdown`.
- **Then** todos os momentos, grupos, tarefas, hábitos e logs são restaurados sem perda de metadados; e quando exportado via `exportNotionObsidianMarkdown`, gera GFM puro sem comentários internos, cujas checklists `- [ ]` / `- [x]` podem ser reimportadas como tarefas via `parseGfmMarkdownChecklists`.

### AC-09: Sincronização de Tarefas com Agenda do Dispositivo (`AndroidCalendarSyncManager`)
- **Given** permissões `READ_CALENDAR` e `WRITE_CALENDAR` concedidas e uma conta de calendário selecionada.
- **When** o usuário aciona `"Sincronizar calendário"` (ou edita uma tarefa com auto-sync ligado).
- **Then** apenas as `Task` são inseridas/atualizadas/removidas em `CalendarContract.Events` e vinculadas em `calendar_sync_events`.

### AC-10: Detector de Texto Inteligente em Títulos e Reversão com Backspace (`SmartTitleParser`)
- **Given** o usuário digita `"Limpar a caixa dos gatos 2x ao dia pela noite grupo casa"` em Novo Hábito, ou `"Visitar meu tio 23/11/2026 toda segunda e terça ás 15:00 até 23/12/2026"` em Nova Tarefa, ou `"Beber água 250 ml de 2500 ml ao dia grupo saude"` em Novo Hábito.
- **When** o motor `SmartTitleParser` avalia o título.
- **Then** os tokens especiais são destacados em azul com fundo escuro, os campos de data início, data fim, horário, frequência, momento, urgência e grupo são preenchidos automaticamente, o título salvo fica limpo (`"Limpar a caixa dos gatos"`, `"Visitar meu tio"`, `"Beber água"`), e pressionar **Backspace** logo após um token detectado preserva o texto literal e reverte a alteração automática.

### AC-11: Widget de Calendário de Tarefas 4x2 (`CalendarTasksGlanceWidget`)
- **Given** o widget 4x2 adicionado à tela inicial com tarefas agendadas hoje e em dias futuros.
- **When** o usuário toca em um dia com pontinho indicador no calendário, ou marca o checkbox de uma tarefa, ou toca no título da tarefa.
- **Then** a lista lateral passa a exibir as tarefas do dia clicado (mantendo o dia de hoje destacado em cor diferenciada), o checkbox conclui a tarefa diretamente pelo widget e o toque no título abre a tarefa dentro do aplicativo.

## 2. Matriz de Testes Implementada
- **Testes Unitários (`app/src/test/java/com/taskmgr/domain` — 37 testes):**
    - `MomentWindowEvaluatorTest.kt`: janelas de `Moment`, tratamento de `00:00` como fim do mesmo dia (`23:59`) e dias ativos.
    - `HabitCompletionAndFormattingTest.kt`: cálculo de `isCompleted` e formatação de progresso/botão em `BOOLEAN`, `COUNT_PER_DAY` e `CUSTOM_UNIT`.
    - `HabitStreakCalculatorTest.kt`: cálculo de `currentStreakDays`, `longestStreakDays` e `totalCompletedDays` com edições retroativas e máscaras de dias da semana.
    - `ReminderStateMachineAndAlarmTest.kt`: transições de `Task` e `HabitLog`, lembretes antecipados (`TaskAdvanceReminder`), filtragem da Matriz de Eisenhower (`shouldShowInEisenhowerMatrix`) e cálculo do próximo alarme (`NextAlarmCalculator`).
    - `MarkdownTransferAndCalendarSyncTest.kt`: round-trip completo de `.taskmgr.md`, exportação/importação GFM `.md` (Notion/Obsidian) e mapeamento de eventos do `TaskCalendarEventMapper`.
    - `CalendarWidgetCalculatorTest.kt`: geração da grade mensal do widget 4x2, indicadores de tarefas por dia e filtragem de tarefas do dia selecionado.
    - `SmartTitleParserTest.kt`: detecção case/accent-insensitive de datas, horários, períodos, frequências, unidades customizadas, urgências, grupos, momento padrão `"Todo o dia"`, tarefas recorrentes com/sem data fim e reversão com Backspace.
- **Testes Instrumentados (`app/src/androidTest/java/com/taskmgr`):**
    - `data/TaskMgrDatabaseTest.kt`: persistência Room, relacionamentos `Moment`, `HabitGroup`, `Task`, `Habit` e `HabitLog`, cascata e edição retroativa.
    - `ui/FullScreenReminderUiTest.kt`: renderização Compose de `FullScreenReminderActivity` com progresso e opções de soneca.
