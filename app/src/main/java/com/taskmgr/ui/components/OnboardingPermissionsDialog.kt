package com.taskmgr.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jovij.taskmgr.R
import com.jovij.taskmgr.ui.theme.IndigoContainer
import com.jovij.taskmgr.ui.theme.IndigoPrimary
import com.jovij.taskmgr.ui.theme.OledBorder
import com.jovij.taskmgr.ui.theme.OledSurface
import com.jovij.taskmgr.ui.theme.OledSurfaceElevated
import com.jovij.taskmgr.ui.theme.PriorityHigh
import com.jovij.taskmgr.ui.theme.TaskCompleted
import com.taskmgr.scheduler.AppPermissionStatus

@Composable
fun OnboardingPermissionsDialog(
    permissionStatus: AppPermissionStatus,
    isXiaomiDevice: Boolean,
    onRequestFullScreen: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestBatteryUnrestricted: () -> Unit,
    onOpenManufacturerPermissions: () -> Unit,
    onFinish: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onFinish,
        containerColor = OledSurfaceElevated,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_taskmgr_icon),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp)
                )
                Text(
                    text = "Configuração de Permissões",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Para garantir que os alarmes do taskmgr interrompam a tela bloqueada e vibrem mesmo no modo Doze, ative as permissões abaixo:",
                    style = MaterialTheme.typography.bodyMedium
                )

                OnboardingStepCard(
                    iconGlyph = NerdIcons.Alarm,
                    title = "1. Notificações em Tela Cheia (Obrigatório)",
                    subtitle = "No Android 14+, sem isso o alarme não abre sobre a lockscreen.",
                    granted = permissionStatus.canUseFullScreenIntent,
                    buttonLabel = "Ativar Tela Cheia",
                    onClick = onRequestFullScreen
                )

                OnboardingStepCard(
                    iconGlyph = NerdIcons.Layers,
                    title = "2. Exibir sobre Outros Apps",
                    subtitle = "Faz o alarme abrir em tela cheia na frente do app atual quando o celular estiver em uso.",
                    granted = permissionStatus.canDrawOverlays,
                    buttonLabel = "Permitir Sobreposição",
                    onClick = onRequestOverlay
                )

                OnboardingStepCard(
                    iconGlyph = NerdIcons.ClockCheck,
                    title = "3. Alarmes Exatos",
                    subtitle = "Garante disparo pontual no minuto exato.",
                    granted = permissionStatus.canScheduleExactAlarms,
                    buttonLabel = "Autorizar Alarmes",
                    onClick = onRequestExactAlarm
                )

                OnboardingStepCard(
                    iconGlyph = NerdIcons.BellAlert,
                    title = "4. Notificações",
                    subtitle = "Exibe lembretes e canais de alarme de alta prioridade.",
                    granted = permissionStatus.hasPostNotifications,
                    buttonLabel = "Permitir Notificações",
                    onClick = onRequestNotifications
                )

                OnboardingStepCard(
                    iconGlyph = NerdIcons.BatteryHigh,
                    title = "5. Bateria Sem Restrições (MIUI / HyperOS)",
                    subtitle = "Evita que o sistema mate alarmes com a tela apagada.",
                    granted = permissionStatus.isIgnoringBatteryOptimizations,
                    buttonLabel = "Remover Restrição",
                    onClick = onRequestBatteryUnrestricted
                )

                if (isXiaomiDevice) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = OledSurface),
                        border = BorderStroke(1.dp, OledBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
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
                                    text = "6. Permissões Extras Xiaomi (Obrigatório)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ative 'Mostrar na tela de bloqueio', 'Janelas pop-up em segundo plano' e 'Início automático'.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FilledTonalButton(onClick = onOpenManufacturerPermissions) {
                                NerdIcon(glyph = NerdIcons.Cog, size = 15.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Abrir Permissões Xiaomi")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onFinish) {
                NerdIcon(
                    glyph = if (permissionStatus.allCriticalGranted) NerdIcons.RocketLaunch else NerdIcons.CheckCircle,
                    size = 16.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (permissionStatus.allCriticalGranted) "Começar" else "Continuar")
            }
        },
        dismissButton = {
            if (!permissionStatus.allCriticalGranted) {
                TextButton(onClick = onFinish) {
                    Text("Depois")
                }
            }
        }
    )
}

@Composable
private fun OnboardingStepCard(
    iconGlyph: String,
    title: String,
    subtitle: String,
    granted: Boolean,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (granted) IndigoContainer else OledSurface
        ),
        border = BorderStroke(1.dp, if (granted) IndigoPrimary.copy(alpha = 0.45f) else OledBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
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
                    size = 19.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
            if (!granted) {
                Spacer(modifier = Modifier.height(6.dp))
                FilledTonalButton(onClick = onClick) {
                    NerdIcon(glyph = NerdIcons.Cog, size = 15.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(buttonLabel)
                }
            }
        }
    }
}
