package com.taskmgr.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskmgr.R
import com.example.taskmgr.ui.theme.IndigoContainer
import com.example.taskmgr.ui.theme.IndigoPrimary
import com.example.taskmgr.ui.theme.OledBorder
import com.example.taskmgr.ui.theme.OledSurface
import com.example.taskmgr.ui.theme.PriorityHigh
import com.example.taskmgr.ui.theme.TaskCompleted
import com.example.taskmgr.ui.theme.TextPrimary
import com.example.taskmgr.ui.theme.TextSecondary
import com.taskmgr.scheduler.AppPermissionStatus
import com.taskmgr.scheduler.VibrationPattern
import com.taskmgr.ui.components.NerdIcon
import com.taskmgr.ui.components.NerdIcons
import com.taskmgr.ui.viewmodel.ExportSyncUiState

private enum class SettingsSection(val title: String) {
    EXPORT_SYNC("Exportação, Sincronização e Importação"),
    PERMISSIONS("Permissões"),
    VIBRATION("Vibração"),
    ABOUT("Sobre")
}

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
    var activeSectionName by rememberSaveable { mutableStateOf<String?>(null) }
    val activeSection = activeSectionName?.let { name ->
        runCatching { SettingsSection.valueOf(name) }.getOrNull()
    }

    BackHandler(enabled = activeSection != null) {
        activeSectionName = null
    }

    AnimatedContent(
        targetState = activeSection,
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally(
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    initialOffsetX = { it / 4 }
                ) + fadeIn(tween(220))) togetherWith (slideOutHorizontally(
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    targetOffsetX = { -it / 6 }
                ) + fadeOut(tween(160)))
            } else {
                (slideInHorizontally(
                    animationSpec = tween(240, easing = FastOutSlowInEasing),
                    initialOffsetX = { -it / 6 }
                ) + fadeIn(tween(220))) togetherWith (slideOutHorizontally(
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    targetOffsetX = { it / 4 }
                ) + fadeOut(tween(160)))
            }
        },
        label = "settings_section_transition"
    ) { section ->
        when (section) {
            null -> {
                SettingsMenuScreen(
                    permissionStatus = permissionStatus,
                    vibrationPattern = vibrationPattern,
                    onSelectSection = { selected -> activeSectionName = selected.name }
                )
            }

            SettingsSection.EXPORT_SYNC -> {
                SettingsSubScreenScaffold(
                    title = SettingsSection.EXPORT_SYNC.title,
                    iconGlyph = NerdIcons.Cloud,
                    onBack = { activeSectionName = null }
                ) {
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
                }
            }

            SettingsSection.PERMISSIONS -> {
                SettingsSubScreenScaffold(
                    title = SettingsSection.PERMISSIONS.title,
                    iconGlyph = NerdIcons.ShieldCheck,
                    onBack = { activeSectionName = null }
                ) {
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
                            description = "Evita que fabricantes (MIUI/HyperOS/Samsung) encerrem alarmes com tela apagada.",
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
                }
            }

            SettingsSection.VIBRATION -> {
                SettingsSubScreenScaffold(
                    title = SettingsSection.VIBRATION.title,
                    iconGlyph = NerdIcons.Vibrate,
                    onBack = { activeSectionName = null }
                ) {
                    item {
                        VibrationSettingsCard(
                            vibrationPattern = vibrationPattern,
                            onSelectVibrationPattern = onSelectVibrationPattern,
                            onTestVibration = onTestVibration,
                            onStopVibration = onStopVibration
                        )
                    }
                }
            }

            SettingsSection.ABOUT -> {
                SettingsSubScreenScaffold(
                    title = SettingsSection.ABOUT.title,
                    iconGlyph = NerdIcons.CodeTags,
                    onBack = { activeSectionName = null }
                ) {
                    item {
                        AboutSettingsCard()
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsMenuScreen(
    permissionStatus: AppPermissionStatus,
    vibrationPattern: VibrationPattern,
    onSelectSection: (SettingsSection) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NerdIcon(
                    glyph = NerdIcons.Cog,
                    color = IndigoPrimary,
                    size = 22.sp
                )
                Text(
                    text = "Configurações",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            SettingsMenuButtonCard(
                iconGlyph = NerdIcons.Cloud,
                title = SettingsSection.EXPORT_SYNC.title,
                subtitle = "Google Agenda, backup .taskmgr.md e Markdown para Notion/Obsidian",
                onClick = { onSelectSection(SettingsSection.EXPORT_SYNC) }
            )
        }

        item {
            val statusSubtitle = if (permissionStatus.allCriticalGranted) {
                "Todas as permissões concedidas"
            } else {
                "Permissões pendentes para alarmes em tela cheia"
            }
            SettingsMenuButtonCard(
                iconGlyph = NerdIcons.ShieldCheck,
                title = SettingsSection.PERMISSIONS.title,
                subtitle = statusSubtitle,
                trailingStatusGlyph = if (permissionStatus.allCriticalGranted) {
                    NerdIcons.CheckCircle
                } else {
                    NerdIcons.AlertCircle
                },
                trailingStatusSuccess = permissionStatus.allCriticalGranted,
                onClick = { onSelectSection(SettingsSection.PERMISSIONS) }
            )
        }

        item {
            SettingsMenuButtonCard(
                iconGlyph = NerdIcons.Vibrate,
                title = SettingsSection.VIBRATION.title,
                subtitle = "Padrão atual: ${vibrationPattern.displayName}",
                onClick = { onSelectSection(SettingsSection.VIBRATION) }
            )
        }

        item {
            SettingsMenuButtonCard(
                iconGlyph = NerdIcons.CodeTags,
                title = SettingsSection.ABOUT.title,
                subtitle = "Versão 0.0.3 • Criado por Jovi",
                onClick = { onSelectSection(SettingsSection.ABOUT) }
            )
        }
    }
}

@Composable
private fun SettingsMenuButtonCard(
    iconGlyph: String,
    title: String,
    subtitle: String,
    trailingStatusGlyph: String? = null,
    trailingStatusSuccess: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OledSurface),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            NerdIcon(
                glyph = iconGlyph,
                color = IndigoPrimary,
                size = 24.sp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            if (trailingStatusGlyph != null) {
                NerdIcon(
                    glyph = trailingStatusGlyph,
                    color = if (trailingStatusSuccess) TaskCompleted else PriorityHigh,
                    size = 18.sp
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun SettingsSubScreenScaffold(
    title: String,
    iconGlyph: String,
    onBack: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = TextPrimary
                    )
                }
                NerdIcon(
                    glyph = iconGlyph,
                    color = IndigoPrimary,
                    size = 22.sp
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
        content()
        item { Spacer(modifier = Modifier.height(28.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VibrationSettingsCard(
    vibrationPattern: VibrationPattern,
    onSelectVibrationPattern: (VibrationPattern) -> Unit,
    onTestVibration: () -> Unit,
    onStopVibration: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OledSurface),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Escolha a intensidade e o ritmo de vibração dos lembretes:",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
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
            Spacer(modifier = Modifier.height(12.dp))
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

@Composable
private fun AboutSettingsCard() {
    val context = LocalContext.current
    val githubUrl = "https://github.com/jovi-j/"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OledSurface),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_taskmgr_icon),
                    contentDescription = "taskmgr",
                    modifier = Modifier.size(48.dp)
                )
                Column {
                    Text(
                        text = "taskmgr",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Versão 0.1.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IndigoPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalDivider(color = OledBorder, thickness = 1.dp)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Criador",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Text(
                    text = "Jovi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = githubUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = IndigoPrimary
                )
            }

            FilledTonalButton(
                onClick = {
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                NerdIcon(glyph = NerdIcons.CodeTags, size = 17.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Abrir GitHub (github.com/jovi-j)")
            }
        }
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
