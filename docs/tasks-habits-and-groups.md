# Tarefas, Hábitos, Grupos, Momentos e Widgets

Este documento descreve o funcionamento das funcionalidades principais de produtividade do **taskmgr**.

---

## 1. Tarefas (`Task`) e Matriz de Eisenhower (`TaskUrgency`)

As tarefas representam demandas pontuais, na Caixa de entrada (sem data) ou recorrentes em dias da semana específicos (`startEpochDay`, `activeDaysMask`, `hasEndDate`, `endEpochDay`):
- **Matriz de Eisenhower (4 Quadrantes / Níveis de Urgência):**
    1. **Importante e Urgente (`Urgência Muito Alta` / `VERY_HIGH`) — Vermelho (`#EF4444`)**
    2. **Importante e Não Urgente (`Urgência Alta` / `HIGH`) — Amarelo (`#FBBF24`)**
    3. **Não Importante e Urgente (`Urgência Média` / `MEDIUM`) — Verde (`#22C55E`)**
    4. **Não Importante e Não Urgente (`Urgência Baixa` / `LOW`) — Azul (`#3B82F6`)**
- **Quadrante Padrão:** Toda tarefa ao ser criada inicia automaticamente em **Urgência Baixa (Não Importante e Não Urgente — Azul)**, a menos que o usuário selecione outro quadrante, digite `"urgencia [nível]"` no título ou crie a tarefa diretamente dentro de um quadrante na aba **Matriz** (`EisenhowerMatrixScreen`).
- **Aba "Matriz" (`EisenhowerMatrixScreen`):** Exibe os 4 quadrantes contendo apenas tarefas ainda não concluídas e tarefas concluídas hoje (`shouldShowInEisenhowerMatrix`). Tarefas concluídas em dias anteriores permanecem consultáveis na aba **Calendário**.
- **Data, Frequência Semanal e Data Fim Opcional:**
    - Podem ser criadas sem data (ficando na **Caixa de entrada** para delegação via modal **"Planejar o dia"** na aba **Hoje**).
    - Podem ter **Frequência Semanal** (`activeDaysMask`, ex.: toda segunda e terça) e **Data Fim opcional** (`hasEndDate`). Quando uma tarefa recorrente não possui data fim, ela permanece ativa nos dias selecionados até que o usuário a marque como concluída na notificação/alarme ou checkbox.
- **Agendamento & Lembretes Antecipados (`TaskAdvanceReminder`):**
    - Podem ter um **horário exato** (`HH:mm`), ser associadas a um **Momento** e/ou repetir a cada `Hora(s)` e `Minuto(s)`.
    - Suportam múltiplos **lembretes antecipados** (`dias`, `horas`, `minutos` antes do horário, com repetição opcional), que disparam tela cheia informando quanto tempo falta para a tarefa.
- **Interação na Tela Inicial (`TodayScreen`):**
    - Toque simples em uma tarefa ou hábito abre a tela de edição.
    - Toque longo abre o menu com **Testar alarme** e **Excluir** (com diálogo de confirmação).

---

## 2. Detector de Texto Inteligente (`SmartTitleParser`) e Editores por Modais

Nas telas de criação/edição de Tarefa e Hábito (`ItemEditorScreens.kt`):
- **Layout Seamless com Barra Horizontal de Modais:**
    - O topo exibe apenas um cartão contínuo com **Título** e **Detalhes** separados por um divisor sutil.
    - Logo abaixo, uma linha horizontal de ícones representa cada seção de configuração. Os ícones ficam cinza quando estão com valor padrão e ganham cor própria quando preenchidos (no caso de **Grupo**, o ícone de Tag assume a cor do grupo selecionado; no caso da **Matriz de Eisenhower**, o ícone de Bandeira assume a cor da urgência selecionada).
- **Detecção em Tempo Real no Título (`SmartTitleParser`):**
    - Case-insensitive e insensível a acentuação:
        - **Data início:** `21/11/2026`, `23/11`, `hoje`, `amanhã`, `depois de amanhã`.
        - **Data fim (Tarefas):** `até 22/12/2026`, `até amanhã`.
        - **Horário:** `às 15:00`, `ás 15:00`, `15:00`, `às 15h`.
        - **Período (Hábitos):** `pela manhã`, `pela tarde`, `pela noite` (se nada for digitado em um novo hábito, assume o momento **Todo o dia** caso exista).
        - **Frequência (Tarefas):** `toda segunda, terça, quarta, quinta`, `toda terça e quinta`.
        - **Frequência (Hábitos):** `2x ao dia`, `3 vezes ao dia`, `10 capitulos de 100 capítulos ao dia`, `250 ml de 2500 ml ao dia`.
        - **Urgência (Tarefas):** `urgencia muito alta`, `urgencia alta`, `urgencia média`, `urgencia baixa`.
        - **Grupo (Hábitos):** `grupo casa`, `grupo saude`.
    - **Destaque e Reversão com Backspace:** Trechos reconhecidos ficam destacados em azul (`#60A5FA`, sublinhado) com fundo mais escuro (`#06090F`). Pressionar **Delete/Backspace** logo à frente de um trecho detectado mantém o texto literal no título, remove a marcação inteligente e reverte imediatamente o campo correspondente na tarefa/hábito.

---

## 3. Hábitos (`Habit`) e Retenção (`HabitStreakCalculator`)

Os hábitos são atividades recorrentes contínuas que ajudam a construir consistência ao longo do tempo:
- **Três Modos de Meta Diária (`CompletionType`):**
    1. **Sim / Não (`BOOLEAN`):** Concluído com um único toque (`1 / 1 Sim`).
    2. **Contagem por Dia (`COUNT_PER_DAY`):** Exemplo: `3 vezes` por dia (`+1` a cada toque).
    3. **Unidade Customizada (`CUSTOM_UNIT`):** Exemplo: `Beber água` com incremento de `+250 ml` e meta diária de `2500 ml`, ou `Ler livro` com `+10 capítulos` e meta de `100 capítulos`.
- **Hábitos Retroativos e Edição no Calendário:**
    - Na criação do hábito, é possível escolher uma **data de início retroativa** (anterior a hoje).
    - Na aba **Calendário**, ao tocar em qualquer dia passado, o usuário visualiza todas as tarefas e hábitos daquele dia e pode informar **exatamente quanto do hábito foi feito** (ou antecipar a data de início de um hábito criado posteriormente para incluí-lo naquele dia).
- **Indicador de Retenção e Sequências:**
    - O app calcula automaticamente **Sequência Atual (`Seq: Xd`)**, **Recorde (`Rec: Xd`)** e **Total de Dias Concluídos**.

---

## 4. Grupos de Hábitos (`HabitGroup`) e Seletor de Ícones Nerd Font

- **Aba Dedicada "Grupos" (`HabitGroupsScreen`):**
    - Permite criar, editar e excluir grupos com uma paleta de 12 cores suaves (muted/pastel escuro) para conforto visual em telas OLED.
    - Lista todos os hábitos organizados dentro de cada grupo, exibindo o progresso do dia, as métricas de sequência e um botão rápido **"Adicionar hábito em [Grupo]"**.
- **Fundo Circular Colorido (`HabitBadgeIcon`) e Catálogo Nerd Font (`HabitIconPickerDialog`):**
    - Catálogo com **mais de 180 ícones** da fonte Ubuntu Sans Nerd Font pesquisáveis em português.

---

## 5. Momentos (`Moment`) e 4 Momentos Padrões (`DefaultMoments`)

Os **Momentos** (acessíveis na aba **Momentos**) definem janelas do dia:
- **4 Momentos Padrões Pré-Configurados:**
    1. **Manhã:** das `06:00` às `12:00`, repetindo a cada `1h` (`60 min`).
    2. **Tarde:** das `12:00` às `18:00`, repetindo a cada `1h` (`60 min`).
    3. **Noite:** das `18:00` às `22:00`, repetindo a cada `1h` (`60 min`).
    4. **Todo o dia:** das `06:00` às `22:00`, repetindo a cada `1h` (`60 min`).
- **Regra de Meia-Noite do Mesmo Dia:** Quando o horário de fim é selecionado como `00:00`, o sistema considera **meia-noite do mesmo dia (`23:59`)**, e não do dia seguinte.
- **Alarme em Tela Cheia (`FullScreenReminderActivity`):** Acende a tela e abre sobre a lockscreen com vibração configurável, permitindo concluir/incrementar o item ou escolher uma das 6 opções de soneca (`5m`, `10m`, `30m`, `1h`, `3h`, `Até amanhã`).

---

## 6. Widgets de Tela Inicial (Jetpack Glance)

- **Widget de Hábito 2x2 (`HabitGlanceWidget`):** Configurável por instância (`HabitWidgetConfigureActivity`), mostra o ícone colorido do hábito, progresso do dia, botão `+` para incrementar diretamente da tela inicial e mini-calendário mensal com os dias concluídos.
- **Widget de Calendário de Tarefas 4x2 (`CalendarTasksGlanceWidget`):** Exibe o calendário mensal à esquerda (com destaque para o dia de hoje, seleção de qualquer dia ao tocar e pontinhos indicadores nos dias posteriores que possuem tarefas) e a lista de tarefas do dia selecionado à direita (permitindo concluir a tarefa clicando no checkbox ou abrir a tarefa no app clicando no título).
