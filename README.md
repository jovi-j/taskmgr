# taskmgr

Aplicativo Android nativo para gerenciamento de **Tarefas**, **Hábitos com unidades customizadas**, **Grupos de Hábitos**, **Momentos (janelas do dia)**, **Widgets Interativos (Glance)** e **Alarmes em Tela Cheia**, construído com **Kotlin**, **Jetpack Compose (Material 3 OLED Dark)** e **Room**.

- **Versão:** `0.3.3` (`com.jovij.taskmgr`)
- **Criador:** [Jovi](https://github.com/jovi-j/)

---

## ✨ Principais Funcionalidades

- **Interface OLED Dark (`#000000`):** Paleta de alto contraste com pixels pretos reais para telas AMOLED, superfícies em camadas (`#0E1117` / `#161B26`), destaques em índigo (`#818CF8`), transições animadas suaves e tipografia **Ubuntu Sans Nerd Font**.
- **Detector de Texto Inteligente em Títulos (`SmartTitleParser`):**
    - Reconhecimento em tempo real (case-insensitive e sem acentuação) enquanto o usuário digita o título de uma Tarefa ou Hábito:
        - **Data de início:** `21/11/2026`, `23/11`, `hoje`, `amanhã`, `depois de amanhã` (assume hoje por padrão).
        - **Data fim (Tarefas):** `até [data]` (ex.: `até 23/12/2026`, `até amanhã`).
        - **Horário exato:** `às 15:00`, `ás 15:00`, `15:00`, `às 15h`, `às 15h30`.
        - **Período (Hábitos):** `pela manhã`, `pela tarde`, `pela noite` (ou `todo o dia`), selecionando o momento correspondente (ou assumindo **Todo o dia** caso nada seja digitado).
        - **Frequência (Tarefas):** `toda` seguido de dias da semana (ex.: `toda segunda e terça`, `toda quarta, quinta e sexta`).
        - **Frequência (Hábitos):** `[N]x ao dia`, `[N] vezes ao dia`, ou unidades customizadas `[inc] [unid] de [meta] [unid] ao dia` (ex.: `2x ao dia`, `10 capitulos de 100 capítulos ao dia`, `250 ml de 2500 ml ao dia`).
        - **Urgência (Tarefas):** `urgencia muito alta`, `urgencia alta`, `urgencia média`, `urgencia baixa`.
        - **Grupo (Hábitos):** `grupo [nome]` (ex.: `grupo casa`, `grupo saude`).
    - Os trechos detectados ficam destacados em azul como um link com fundo mais escuro; pressionar **Delete/Backspace** logo à frente do trecho mantém o texto literal no título e reverte a alteração automática.
- **Editores Enxutos por Modais (`ItemEditorScreens`):**
    - Cartão único e *seamless* com **Título** e **Detalhes**, acompanhado por uma barra horizontal de ícones de seção (cinza quando no padrão e coloridos por seção/grupo/urgência quando preenchidos) que abrem modais (`ModalBottomSheet`) sob demanda.
- **5 Abas na Barra Inferior + Configurações no Topo:**
    1. **Hoje:** Progresso diário, modal **"Planejar o dia"** para delegar tarefas da Caixa de entrada, tarefas e hábitos agrupados por Momento do dia (toque simples abre a edição; toque longo abre **Testar alarme** e **Excluir** com confirmação).
    2. **Calendário:** Visão mensal com indicadores visuais, resumo de retenção de hábitos e **edição retroativa** (clique em qualquer dia anterior para ajustar exatamente quanto de cada hábito foi feito ou incluir hábitos retroativamente).
    3. **Matriz:** Visão em grade 2x2 com os **4 quadrantes da Matriz de Eisenhower** para tarefas (exibindo tarefas pendentes e tarefas concluídas hoje):
        - **Importante e Urgente (`Urgência Muito Alta`) — Vermelho (`#EF4444`)**
        - **Importante e Não Urgente (`Urgência Alta`) — Amarelo (`#FBBF24`)**
        - **Não Importante e Urgente (`Urgência Média`) — Verde (`#22C55E`)**
        - **Não Importante e Não Urgente (`Urgência Baixa`) — Azul (`#3B82F6`)** *(quadrante padrão ao criar uma nova tarefa)*
    4. **Grupos:** Criação de grupos de hábitos com paleta de cores suaves (muted) e ícone circular (`HabitBadgeIcon`) do catálogo com mais de 180 ícones Nerd Font pesquisáveis, listando todos os hábitos dentro de cada grupo.
    5. **Momentos:** 4 momentos padrões já pré-configurados (**Manhã** `06:00–12:00`, **Tarde** `12:00–18:00`, **Noite** `18:00–22:00` e **Todo o dia** `06:00–22:00`, repetindo a cada 1h), além de criação/edição de janelas customizadas.
    - **Config (no canto superior direito da TopAppBar):** Menu organizado em 4 subtelas (**Exportação, Sincronização e Importação**, **Permissões**, **Vibração** e **Sobre**).
- **Widgets de Tela Inicial (Jetpack Glance):**
    1. **Widget de Hábito (2x2):** Exibe ícone, progresso diário, botão rápido de incremento (`+1 Sim` ou `+unidade`) e calendário mensal de conclusões do hábito.
    2. **Widget de Calendário de Tarefas (4x2):** Exibe o calendário mensal à esquerda (com destaque para o dia de hoje e pontinhos nos dias futuros com tarefas) e a lista de tarefas do dia selecionado à direita (com conclusão via checkbox e abertura direta da tarefa no app ao tocar no título).
- **Hábitos Flexíveis & Indicador de Sequência (Streak):**
    - Suporte a hábitos booleanos (`Sim/Não`), contagem diária (`N vezes`) ou unidades customizadas (ex.: `+250 ml / 2000 ml`, `+1 capítulo / 10 capítulos`).
    - Cálculo automático de **Sequência Atual**, **Recorde** e **Total de Dias Concluídos**.
- **Alarmes em Tela Cheia, Lembretes Antecipados & Soneca Inteligente:**
    - Acende a tela e abre sobre a lockscreen (`FullScreenReminderActivity`) com padrões de vibração configuráveis, lembretes antecipados customizáveis (`TaskAdvanceReminder`) e 6 opções de soneca (`5m`, `10m`, `30m`, `1h`, `3h`, `Até amanhã`).
- **Exportação, Sincronização e Importação (5 Modos):**
    1. **Sincronizar calendário:** Sincroniza as tarefas com o Google Agenda / Calendário do Android via `CalendarContract`.
    2. **Importar arquivo `.taskmgr.md`:** Restaura backup completo (grupos, momentos, hábitos, histórico e tarefas) de outra instância do app.
    3. **Importar arquivo `.md` (Notion/Obsidian):** Importa checklists GitHub Flavored Markdown (`- [ ]` e `- [x]`) como tarefas.
    4. **Exportar arquivo `.taskmgr.md`:** Backup completo em GFM com metadados para migração entre dispositivos.
    5. **Exportar arquivo `.md` (Notion/Obsidian):** Exporta documento GFM limpo com checklists e tabelas de retenção.

---

## 📚 Documentação e Especificações (Specs para Agentes)

- **Documentação Funcional (`docs/`):**
    - [Visão Geral e Arquitetura](docs/overview-and-architecture.md)
    - [Tarefas, Hábitos, Grupos e Momentos](docs/tasks-habits-and-groups.md)
    - [Exportação, Sincronização e Importação](docs/export-sync-and-import.md)
- **Especificações Técnicas (`specs/`):**
    - [01 — Domínio e Invariantes](specs/01-domain-and-invariants.spec.md)
    - [02 — Máquinas de Estado](specs/02-state-machines.spec.md)
    - [03 — Dados, Room v6 e Contratos](specs/03-data-and-contracts.spec.md)
    - [04 — Critérios de Aceite e Matriz de Testes](specs/04-acceptance-and-testing.spec.md)

---

## 🛠️ Build e Testes

```bash
# Executar testes unitários (JUnit 4)
./gradlew :app:testDebugUnitTest

# Compilar APK de debug
./gradlew :app:assembleDebug

# Executar testes instrumentados (Room + Compose UI) em dispositivo/emulador conectado
./gradlew :app:connectedDebugAndroidTest
```
