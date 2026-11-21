package com.taskmgr.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jovij.taskmgr.ui.theme.IndigoPrimary
import com.jovij.taskmgr.ui.theme.OledBorder
import com.jovij.taskmgr.ui.theme.OledSurface
import com.jovij.taskmgr.ui.theme.TextPrimary
import com.jovij.taskmgr.ui.theme.TextSecondary
import com.jovij.taskmgr.ui.theme.UbuntuSansMonoNerdFontFamily
import com.taskmgr.domain.logic.DaysOfWeekMask
import com.taskmgr.domain.logic.MomentWindowEvaluator
import com.taskmgr.domain.model.Moment
import com.taskmgr.domain.model.ReminderMode
import com.taskmgr.ui.components.NerdIcon
import com.taskmgr.ui.components.NerdIcons
import com.taskmgr.ui.components.formatIntervalSummary
import com.taskmgr.ui.components.formatMinutes
import com.taskmgr.ui.components.momentNerdGlyph
import com.taskmgr.ui.components.shortPtBr

@Composable
fun MomentsScreen(
    moments: List<Moment>,
    onAddMoment: () -> Unit,
    onEditMoment: (Moment) -> Unit,
    onToggleMomentEnabled: (Moment) -> Unit,
    onDeleteMoment: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "moments_header", contentType = "moments_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NerdIcon(
                        glyph = NerdIcons.TimerOutline,
                        color = IndigoPrimary,
                        size = 22.sp
                    )
                    Text(
                        text = "Momentos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                FilledTonalButton(onClick = onAddMoment) {
                    NerdIcon(glyph = NerdIcons.PlusCircle, size = 17.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Novo Momento")
                }
            }
        }

        if (moments.isEmpty()) {
            item(key = "moments_empty", contentType = "moments_empty") {
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NerdIcon(
                            glyph = NerdIcons.TimerOutline,
                            color = IndigoPrimary,
                            size = 34.sp
                        )
                        Text(
                            text = "Nenhum momento configurado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Crie janelas como Manhã (07:00–11:00) ou Antes de dormir (22:00–01:00) para agrupar e repetir lembretes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(
                items = moments,
                key = { "moment_${it.id}" },
                contentType = { "moment_card" }
            ) { moment ->
                MomentCard(
                    moment = moment,
                    onEdit = { onEditMoment(moment) },
                    onToggleEnabled = { onToggleMomentEnabled(moment) },
                    onDelete = { onDeleteMoment(moment.id) }
                )
            }
        }

        item(key = "moments_bottom_spacer", contentType = "spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MomentCard(
    moment: Moment,
    onEdit: () -> Unit,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val daysLabel = DaysOfWeekMask.toDays(moment.daysOfWeekMask)
        .joinToString(", ") { it.shortPtBr() }
    val modeLabel = when (moment.reminderMode) {
        ReminderMode.ONCE_AT_START -> "No início"
        ReminderMode.INTERVAL_MINUTES -> "A cada ${formatIntervalSummary(moment.intervalMinutes ?: 30)}"
        ReminderMode.SPECIFIC_TIMES -> "${moment.specificTimesMinutesOfDay.size} horários"
    }
    val overnight = if (MomentWindowEvaluator.crossesMidnight(moment)) " (cruza meia-noite)" else ""

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OledSurface),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    NerdIcon(
                        glyph = momentNerdGlyph(moment.startMinutesOfDay),
                        color = MaterialTheme.colorScheme.primary,
                        size = 18.sp
                    )
                    Text(
                        text = moment.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    NerdIcon(
                        glyph = NerdIcons.ClockOutline,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 14.sp
                    )
                    Text(
                        text = "${formatMinutes(moment.startMinutesOfDay)} – ${formatMinutes(moment.endMinutesOfDay)}$overnight",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = UbuntuSansMonoNerdFontFamily
                        )
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    NerdIcon(
                        glyph = NerdIcons.Repeat,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 13.sp
                    )
                    Text(
                        text = "$modeLabel • $daysLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(checked = moment.isEnabled, onCheckedChange = { onToggleEnabled() })
            IconButton(onClick = onEdit) {
                NerdIcon(
                    glyph = NerdIcons.Pencil,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 19.sp
                )
            }
            IconButton(onClick = onDelete) {
                NerdIcon(
                    glyph = NerdIcons.DeleteOutline,
                    color = MaterialTheme.colorScheme.error,
                    size = 19.sp
                )
            }
        }
    }
}
