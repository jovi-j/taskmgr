# Tarefas, Hábitos, Grupos e Momentos

Este documento descreve o funcionamento das funcionalidades principais de produtividade do **taskmgr**.

---

## 1. Tarefas (`Task`)

As tarefas representam demandas pontuais ou com duração em dias específicos (`startEpochDay` até `endEpochDay`):
- **Criação em Tela Inteira (`TaskEditorScreen`):** Acessada pelo botão flutuante (`+`) na aba **Hoje**.
- **Agendamento:**
    - Podem ter um **horário exato** (`HH:mm`).
    - Podem ser associadas a um **Momento** (herdando a janela temporal do momento).
    - Podem ter repetição configurada no campo **"Repetir a cada:"** com caixas separadas para horas (`H:`) e minutos (`M:`).
- **Conclusão:** Ao marcar uma tarefa como concluída na aba **Hoje** ou no **Calendário**, todos os alarmes pendentes para ela são imediatamente cancelados.

---

## 2. Hábitos (`Habit`) e Retenção (`HabitStreakCalculator`)

Os hábitos são atividades recorrentes contínuas que ajudam a construir consistência ao longo do tempo:
- **Três Modos de Meta Diária (`CompletionType`):**
    1. **Sim / Não (`BOOLEAN`):** Concluído com um único toque (`1 / 1 Sim`).
    2. **Contagem por Dia (`COUNT_PER_DAY`):** Exemplo: `3 vezes` por dia (`+1` a cada toque).
    3. **Unidade Customizada (`CUSTOM_UNIT`):** Exemplo: `Beber água` com incremento de `+250 ml` e meta diária de `2000 ml`, ou `Ler livro` com `+1 capítulo` e meta de `10 capítulos`.
- **Hábitos Retroativos e Edição no Calendário:**
    - Na criação do hábito, é possível escolher uma **data de início retroativa** (anterior a hoje).
    - Na aba **Calendário**, ao tocar em qualquer dia passado, o usuário visualiza todas as tarefas e hábitos daquele dia e pode informar **exatamente quanto do hábito foi feito** (ou antecipar a data de início de um hábito criado posteriormente para incluí-lo naquele dia).
- **Indicador de Retenção e Sequências:**
    - O app calcula automaticamente para cada hábito:
        - **Sequência Atual (`Seq: Xd`):** Quantos dias ativos consecutivos o usuário manteve o hábito até o dia de referência.
        - **Recorde (`Rec: Xd`):** Maior sequência histórica alcançada.
        - **Total de Dias Concluídos:** Quantidade total de dias em que a meta foi batida.
- **Indicador Visual Compacto:** Quando a meta diária de um hábito é atingida, um ícone verde de conclusão (`CheckCircle`) é exibido ao lado do título, evitando quebra de linha.

---

## 3. Grupos de Hábitos (`HabitGroup`) e Seletor de Ícones Nerd Font

Para facilitar a organização visual, os hábitos podem ser agrupados em categorias personalizadas:
- **Aba Dedicada "Grupos" (`HabitGroupsScreen`):**
    - Permite criar, editar e excluir grupos (ex.: `"Casa"` com cor vermelha, `"Saúde"` com cor ciano/verde, `"Estudos"` com cor índigo).
    - Lista todos os hábitos organizados dentro de cada grupo, exibindo o progresso do dia, as métricas de sequência e um botão rápido **"Adicionar hábito em [Grupo]"**.
- **Fundo Circular Colorido (`HabitBadgeIcon`):**
    - Todos os hábitos pertencentes a um grupo exibem seu ícone dentro de um círculo preenchido com a cor daquele grupo.
- **Seletor de Ícones Nerd Font (`HabitIconPickerDialog`):**
    - Catálogo com **mais de 180 ícones** da fonte Ubuntu Sans Nerd Font.
    - Os ícones são exibidos de forma limpa (apenas os glifos circulares, sem rótulos de texto poluindo a grade), mas possuem títulos, categorias e palavras-chave internas em português para pesquisa instantânea na barra de busca.

---

## 4. Momentos (`Moment`) e Alarmes em Tela Cheia

Os **Momentos** (acessíveis na aba **Momentos**) definem janelas do dia (ex.: *Manhã 06:00–12:00*, *Tarde 13:00–18:00*, *Todo o Dia 06:00–00:00*):
- **Regra de Meia-Noite do Mesmo Dia:** Quando o horário de fim é selecionado como `00:00`, o sistema considera **meia-noite do mesmo dia (`23:59`)**, e não do dia seguinte.
- **Validação de Janela:** O início nunca pode ser posterior ao fim (ex.: `22:00` até `04:00` não é permitido).
- **Repetição `H:` e `M:`:** No modo de repetição por intervalo, o configurador de momentos também utiliza os campos **"Repetir a cada:"** (`H:` e `M:`) para definir de quanto em quanto tempo o usuário pode ser lembrado dentro daquele momento.
- **Alarme em Tela Cheia (`FullScreenReminderActivity`):** Quando chega o horário de uma tarefa ou hábito, o app pode acender a tela e abrir sobre a lockscreen com vibração configurável, permitindo concluir/incrementar o item ou escolher uma das 6 opções de soneca (`5m`, `10m`, `30m`, `1h`, `3h`, `Até amanhã`).
