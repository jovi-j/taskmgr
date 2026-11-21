# taskmgr

Aplicativo Android nativo para gerenciamento de **Tarefas**, **Hábitos com unidades customizadas**, **Grupos de Hábitos**, **Momentos (janelas do dia)** e **Alarmes em Tela Cheia**, construído com **Kotlin**, **Jetpack Compose (Material 3 OLED Dark)** e **Room**.

- **Versão:** `0.0.2`
- **Criador:** [Jovi](https://github.com/jovi-j/)

---

## ✨ Principais Funcionalidades

- **Interface OLED Dark (`#000000`):** Paleta de alto contraste com pixels pretos reais para telas AMOLED, superfícies em camadas (`#0E1117` / `#161B26`), destaques em índigo (`#818CF8`), transições animadas suaves e tipografia **Ubuntu Sans Nerd Font**.
- **5 Abas Principais na Barra Inferior:**
    1. **Hoje:** Progresso diário, tarefas e hábitos agrupados por Momento do dia.
    2. **Calendário:** Visão mensal com indicadores visuais, resumo de retenção de hábitos e **edição retroativa** (clique em qualquer dia anterior para ajustar exatamente quanto de cada hábito foi feito ou incluir hábitos retroativamente).
    3. **Grupos:** Criação de grupos de hábitos com cor e ícone circular (`HabitBadgeIcon`) do catálogo com mais de 180 ícones Nerd Font pesquisáveis, listando todos os hábitos dentro de cada grupo.
    4. **Momentos:** Janelas temporais do dia (ex.: `06:00 - 12:00`, ou `06:00 - 00:00` tratando meia-noite como fim do mesmo dia) com repetição configurável em horas (`H:`) e minutos (`M:`).
    5. **Config:** Menu organizado em 4 subtelas (**Exportação, Sincronização e Importação**, **Permissões**, **Vibração** e **Sobre**).
- **Hábitos Flexíveis & Indicador de Sequência (Streak):**
    - Suporte a hábitos booleanos (`Sim/Não`), contagem diária (`N vezes`) ou unidades customizadas (ex.: `+250 ml / 2000 ml`, `+1 capítulo / 10 capítulos`).
    - Cálculo automático de **Sequência Atual**, **Recorde** e **Total de Dias Concluídos**.
- **Alarmes em Tela Cheia & Soneca Inteligente:**
    - Acende a tela e abre sobre a lockscreen (`FullScreenReminderActivity`) com padrões de vibração configuráveis e 6 opções de soneca (`5m`, `10m`, `30m`, `1h`, `3h`, `Até amanhã`).
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
    - [03 — Dados, Room v3 e Contratos](specs/03-data-and-contracts.spec.md)
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
