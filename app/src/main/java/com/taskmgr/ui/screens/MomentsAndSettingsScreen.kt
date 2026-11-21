package com.taskmgr.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskmgr.ui.theme.IndigoContainer
import com.example.taskmgr.ui.theme.IndigoPrimary
import com.example.taskmgr.ui.theme.OledBorder
import com.example.taskmgr.ui.theme.OledSurface
import com.example.taskmgr.ui.theme.PriorityHigh
import com.example.taskmgr.ui.theme.TaskCompleted
import com.example.taskmgr.ui.theme.TextSecondary
import com.taskmgr.scheduler.AppPermissionStatus
import com.taskmgr.scheduler.VibrationPattern
import com.taskmgr.ui.components.NerdIcon
import com.taskmgr.ui.components.NerdIcons
import com.taskmgr.ui.viewmodel.ExportSyncUiState

@Composable
fun SettingsScreen(
    permissionStatus: AppPermissionStatus,
    vibrationPattern: VibrationPattern,
    exportSyncUiState: ExportSyncUiState,
    onSyncCalendarNow: () -> Unit,
    onRequestCalendarPermissions: () -> Unit,
    onSelectCalendarForSync: (Long) -> Unit,
    onToggleAutoSyncTasks: (Boolean) -> Unit,
    onImportTaskMgrMarkdown: () -> Unit,
    onImportNotionObsidianMarkdown: () -> Unit,
    onExportTaskMgrMarkdown: () -> Unit,
    onExportNotionObsidianMarkdown: () -> Unit,
    onShareTaskMgrMarkdown: () -> Unit,
    onShareNotionObsidianMarkdown: () -> Unit,
    onRequestFullScreenPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onRequestPostNotificationPermission: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onOpenManufacturerPermissions: () -> Unit,
    isXiaomiDevice: Boolean,
    onSelectVibrationPattern: (VibrationPattern) -> Unit,
    onTestVibration: () -> Unit,
    onStopVibration: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NerdIcon(
                    glyph = NerdIcons.Cloud,
                    color = MaterialTheme.colorScheme.primary,
                    size = 22.sp
                )
                Text(
                    text = "Exportação, Sincronização e Importação",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            ExportAndSyncCard(
                uiState = exportSyncUiState,
                onSyncCalendarNow = onSyncCalendarNow,
                onRequestCalendarPermissions = onRequestCalendarPermissions,
                onSelectCalendarForSync = onSelectCalendarForSync,
                onToggleAutoSyncTasks = onToggleAutoSyncTasks,
                onImportTaskMgrMarkdown = onImportTaskMgrMarkdown,
                onImportNotionObsidianMarkdown = onImportNotionObsidianMarkdown,
                onExportTaskMgrMarkdown = onExportTaskMgrMarkdown,
                onExportNotionObsidianMarkdown = onExportNotionObsidianMarkdown,
                onShareTaskMgrMarkdown = onShareTaskMgrMarkdown,
                onShareNotionObsidianMarkdown = onShareNotionObsidianMarkdown
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NerdIcon(
                    glyph = NerdIcons.ShieldCheck,
                    color = MaterialTheme.colorScheme.primary,
                    size = 22.sp
                )
                Text(
                    text = "Permissões Críticas (Alarmes TDAH)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            PermissionRowCard(
                iconGlyph = NerdIcons.Alarm,
                title = "Notificações em Tela Cheia",
                description = "Obrigatório no Android 14+ para abrir o alarme sobre a tela de bloqueio.",
                granted = permissionStatus.canUseFullScreenIntent,
                actionLabel = "Abrir Configurações",
                onAction = onRequestFullScreenPermission
            )
        }
        item {
            PermissionRowCard(
                iconGlyph = NerdIcons.Layers,
                title = "Exibir sobre Outros Apps",
                description = "Permite que o alarme apareça em tela cheia na frente do que você estiver usando.",
                granted = permissionStatus.canDrawOverlays,
                actionLabel = "Permitir Sobreposição",
                onAction = onRequestOverlayPermission
            )
        }
        item {
            PermissionRowCard(
                iconGlyph = NerdIcons.ClockCheck,
                title = "Alarmes e Lembretes Exatos",
                description = "Garante disparo pontual mesmo quando o sistema entra em Doze Mode.",
                granted = permissionStatus.canScheduleExactAlarms,
                actionLabel = "Autorizar",
                onAction = onRequestExactAlarmPermission
            )
        }
        item {
            PermissionRowCard(
                iconGlyph = NerdIcons.BellRing,
                title = "Permissão de Notificações",
                description = "Necessária no Android 13+ para exibir alertas e canais de alta prioridade.",
                granted = permissionStatus.hasPostNotifications,
                actionLabel = "Conceder",
                onAction = onRequestPostNotificationPermission
            )
        }
        item {
            PermissionRowCard(
                iconGlyph = NerdIcons.BatteryHigh,
                title = "Bateria Sem Restrições",
                description = "Evita que fabricantes (MIUI/HyperOS/Samsung) matem alarmes com tela apagada.",
                granted = permissionStatus.isIgnoringBatteryOptimizations,
                actionLabel = "Desativar Otimização",
                onAction = onRequestBatteryOptimization
            )
        }

        if (isXiaomiDevice) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = OledSurface),
                    border = BorderStroke(1.dp, OledBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NerdIcon(
                                glyph = NerdIcons.Cellphone,
                                color = MaterialTheme.colorScheme.primary,
                                size = 18.sp
                            )
                            Text(
                                text = "Permissões Extras MIUI / HyperOS (Xiaomi)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Em aparelhos Xiaomi/Redmi/POCO, ative também: 'Mostrar na tela de bloqueio', 'Exibir janelas pop-up em segundo plano' e 'Início automático'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FilledTonalButton(onClick = onOpenManufacturerPermissions) {
                            NerdIcon(glyph = NerdIcons.Cog, size = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Abrir Permissões Xiaomi")
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NerdIcon(
                    glyph = NerdIcons.Vibrate,
                    color = MaterialTheme.colorScheme.primary,
                    size = 22.sp
                )
                Text(
                    text = "Padrão de Vibração",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = OledSurface),
                border = BorderStroke(1.dp, OledBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        VibrationPattern.entries.forEach { pattern ->
                            FilterChip(
                                selected = vibrationPattern == pattern,
                                onClick = { onSelectVibrationPattern(pattern) },
                                leadingIcon = if (vibrationPattern == pattern) {
                                    { NerdIcon(glyph = NerdIcons.Flash, size = 14.sp) }
                                } else null,
                                label = { Text(pattern.displayName) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onTestVibration) {
                            NerdIcon(glyph = NerdIcons.Vibrate, size = 17.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Testar")
                        }
                        OutlinedButton(onClick = onStopVibration) {
                            NerdIcon(glyph = NerdIcons.Close, size = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Parar")
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExportAndSyncCard(
    uiState: ExportSyncUiState,
    onSyncCalendarNow: () -> Unit,
    onRequestCalendarPermissions: () -> Unit,
    onSelectCalendarForSync: (Long) -> Unit,
    onToggleAutoSyncTasks: (Boolean) -> Unit,
    onImportTaskMgrMarkdown: () -> Unit,
    onImportNotionObsidianMarkdown: () -> Unit,
    onExportTaskMgrMarkdown: () -> Unit,
    onExportNotionObsidianMarkdown: () -> Unit,
    onShareTaskMgrMarkdown: () -> Unit,
    onShareNotionObsidianMarkdown: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OledSurface),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(visible = !uiState.statusMessage.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.isStatusError) {
                            PriorityHigh.copy(alpha = 0.14f)
                        } else {
                            IndigoContainer
                        }
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (uiState.isStatusError) PriorityHigh else IndigoPrimary
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NerdIcon(
                            glyph = if (uiState.isStatusError) NerdIcons.AlertCircle else NerdIcons.CheckCircle,
                            color = if (uiState.isStatusError) PriorityHigh else TaskCompleted,
                            size = 18.sp
                        )
                        Text(
                            text = uiState.statusMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // 1. Sincronizar calendário (apenas tarefas)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        NerdIcon(
                            glyph = NerdIcons.CalendarCheck,
                            color = IndigoPrimary,
                            size = 19.sp
                        )
                        Column {
                            Text(
                                text = "1. Sincronizar calendário",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Sincroniza apenas as tarefas com o Google Agenda / Calendário do celular.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                if (!uiState.hasCalendarPermissions) {
                    FilledTonalButton(
                        onClick = onRequestCalendarPermissions,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        NerdIcon(glyph = NerdIcons.CalendarCheck, size = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Permitir Acesso ao Calendário e Sincronizar")
                    }
                } else {
                    if (uiState.availableCalendars.isNotEmpty()) {
                        Text(
                            text = "Conta de agenda destino:",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            uiState.availableCalendars.forEach { cal ->
                                val selected = cal.id == uiState.selectedCalendarId
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectCalendarForSync(cal.id) },
                                    leadingIcon = if (selected) {
                                        { NerdIcon(glyph = NerdIcons.CheckCircle, size = 14.sp) }
                                    } else null,
                                    label = {
                                        Text("${cal.displayName} (${cal.accountName})")
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sincronizar tarefas automaticamente",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Atualiza a agenda sempre que criar, concluir ou editar uma tarefa.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = uiState.autoSyncTasksEnabled,
                            onCheckedChange = onToggleAutoSyncTasks
                        )
                    }

                    Button(
                        onClick = onSyncCalendarNow,
                        enabled = !uiState.isSyncingCalendar,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        NerdIcon(glyph = NerdIcons.Repeat, size = 17.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (uiState.isSyncingCalendar) {
                                "Sincronizando tarefas..."
                            } else {
                                "Sincronizar calendário"
                            }
                        )
                    }

                    if (!uiState.lastSyncSummary.isNullOrBlank()) {
                        Text(
                            text = "Última sincronização: ${uiState.lastSyncSummary}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            HorizontalDivider(color = OledBorder, thickness = 1.dp)

            // 2. Importar arquivo .taskmgr.md
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "2. Importar arquivo .taskmgr.md",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Restaura backup completo de outra instância do app (grupos, momentos, hábitos, histórico e tarefas).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                FilledTonalButton(
                    onClick = onImportTaskMgrMarkdown,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NerdIcon(glyph = NerdIcons.Folder, size = 17.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Importar arquivo .taskmgr.md")
                }
            }

            HorizontalDivider(color = OledBorder, thickness = 1.dp)

            // 3. Importar arquivo .md (Notion/Obsidian)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "3. Importar arquivo .md (Notion/Obsidian)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Importa checklists GitHub Flavored Markdown (- [ ] e - [x]) como tarefas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                FilledTonalButton(
                    onClick = onImportNotionObsidianMarkdown,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NerdIcon(glyph = NerdIcons.FormatListChecks, size = 17.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Importar arquivo .md (Notion/Obsidian)")
                }
            }

            HorizontalDivider(color = OledBorder, thickness = 1.dp)

            // 4. Exportar arquivo .taskmgr.md
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "4. Exportar arquivo .taskmgr.md",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Gera um Markdown GFM com metadados completos para transferir todos os grupos, momentos, hábitos e tarefas para outra instância do app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportTaskMgrMarkdown,
                        modifier = Modifier.weight(1f)
                    ) {
                        NerdIcon(glyph = NerdIcons.FileDocument, size = 17.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exportar arquivo .taskmgr.md")
                    }
                    OutlinedButton(onClick = onShareTaskMgrMarkdown) {
                        NerdIcon(glyph = NerdIcons.Paperclip, size = 16.sp)
                    }
                }
            }

            HorizontalDivider(color = OledBorder, thickness = 1.dp)

            // 5. Exportar arquivo .md (Notion/Obsidian)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "5. Exportar arquivo .md (Notion/Obsidian)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Gera um documento GitHub Flavored Markdown limpo com checklists, tabelas de retenção e momentos para abrir no Notion ou Obsidian.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportNotionObsidianMarkdown,
                        modifier = Modifier.weight(1f)
                    ) {
                        NerdIcon(glyph = NerdIcons.Notebook, size = 17.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exportar arquivo .md (Notion/Obsidian)")
                    }
                    OutlinedButton(onClick = onShareNotionObsidianMarkdown) {
                        NerdIcon(glyph = NerdIcons.Paperclip, size = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRowCard(
    iconGlyph: String,
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OledSurface),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    NerdIcon(
                        glyph = iconGlyph,
                        color = MaterialTheme.colorScheme.primary,
                        size = 18.sp
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                NerdIcon(
                    glyph = if (granted) NerdIcons.CheckCircle else NerdIcons.AlertCircle,
                    color = if (granted) TaskCompleted else PriorityHigh,
                    size = 20.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!granted) {
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(onClick = onAction) {
                    NerdIcon(glyph = NerdIcons.Cog, size = 15.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(actionLabel)
                }
            }
        }
    }
}

