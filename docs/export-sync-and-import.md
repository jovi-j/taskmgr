# Exportação, Sincronização e Importação

Na aba **Config** → **Exportação, Sincronização e Importação**, o **taskmgr** oferece 5 ferramentas de interoperabilidade e backup:

---

## 1. Sincronizar calendário (Google Agenda / Calendário Android)

- **Escopo:** Sincroniza **apenas as tarefas (`Task`)** com o aplicativo de calendário do dispositivo utilizando a API nativa `CalendarContract.Events` do Android.
- **Seleção de Conta:** Lista todas as agendas graváveis do aparelho (como contas do Google Agenda sincronizadas no Android) para o usuário escolher onde os eventos serão criados.
- **Sincronização Automática e Manual:**
    - Possui chave **"Sincronizar tarefas automaticamente"**, que atualiza a agenda sempre que uma tarefa é criada, editada, concluída ou excluída.
    - Possui botão **"Sincronizar calendário"** para disparo imediato.
- **Sincronização Bidirecional de Tarefas Mapeadas:**
    - A tabela Room `calendar_sync_events` mantém o vínculo `(taskId <-> eventId)` e o `lastSyncedHash`.
    - Se o usuário alterar o título, notas, data ou horário de uma tarefa diretamente no aplicativo do Google Agenda, a próxima sincronização traz essas alterações de volta para a tarefa no **taskmgr**.
    - Tarefas com horário agendado viram eventos com hora marcada; tarefas sem horário específico viram eventos de dia inteiro (`ALL_DAY` em UTC).

---

## 2. Importar arquivo `.taskmgr.md`

- Restaura um backup completo gerado por outra instância do **taskmgr**.
- Reconstrói integralmente:
    - **Momentos** (janelas, dias ativos, intervalos de repetição).
    - **Grupos de Hábitos** (nomes, cores em ARGB e ícones Nerd Font).
    - **Hábitos** (unidades customizadas, metas, dias ativos, vínculo com grupos e momentos).
    - **Histórico de Hábitos (`HabitLog`)** (progresso acumulado por dia para preservar sequências e recordes).
    - **Tarefas** (datas, horários, notas, momentos e status de conclusão).

---

## 3. Importar arquivo `.md` (Notion / Obsidian)

- Lê qualquer arquivo GitHub Flavored Markdown (`.md`) exportado do **Notion**, **Obsidian** ou editores Markdown.
- Converte todas as linhas de checklist GFM (`- [ ] Tarefa pendente` e `- [x] Tarefa concluída`) em tarefas do **taskmgr**.
- Reconhece automaticamente:
    - Datas no formato ISO (`YYYY-MM-DD` ou intervalos `YYYY-MM-DD..YYYY-MM-DD`).
    - Horários no formato `HH:mm`.
    - Notas adicionais indentadas com `>` logo abaixo do item da checklist.

---

## 4. Exportar arquivo `.taskmgr.md`

- Gera um arquivo `.taskmgr.md` legível em qualquer leitor de Markdown (100% compatível com GFM) e enriquecido com blocos de metadados (`<!-- TASKMGR_* -->` codificados em Base64 URL-safe).
- Ideal para migrar todos os dados para outro celular ou manter um backup completo sem perder grupos, momentos, hábitos ou histórico.
- Permite salvar diretamente no armazenamento do dispositivo ou compartilhar via `FileProvider`.

---

## 5. Exportar arquivo `.md` (Notion / Obsidian)

- Gera um documento **GitHub Flavored Markdown (GFM)** limpo (sem comentários internos de metadados), contendo:
    - **Frontmatter YAML** (`title`, `exported_at`, `reference_date`, `tags`).
    - **Seção de Tarefas:** Checklists GFM (`- [ ]` e `- [x]`) com badges de data, horário, momento e notas em blockquote.
    - **Tabela GFM de Hábitos e Retenção:** Hábito, grupo, meta diária, momento/horário, sequência atual, recorde e total de dias concluídos.
    - **Tabela GFM de Grupos de Hábitos e Momentos.**
    - **Tabela GFM de Histórico de Execução de Hábitos.**
