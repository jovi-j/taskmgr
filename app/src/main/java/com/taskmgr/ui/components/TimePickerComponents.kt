package com.taskmgr.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskmgr.ui.theme.IndigoContainer
import com.example.taskmgr.ui.theme.IndigoOnPrimary
import com.example.taskmgr.ui.theme.IndigoPrimary
import com.example.taskmgr.ui.theme.OledBorder
import com.example.taskmgr.ui.theme.OledSurface
import com.example.taskmgr.ui.theme.OledSurfaceElevated
import com.example.taskmgr.ui.theme.TextPrimary
import com.example.taskmgr.ui.theme.TextSecondary
import com.example.taskmgr.ui.theme.UbuntuSansMonoNerdFontFamily

@Composable
fun TimeSelectorField(
    label: String,
    minutesOfDay: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = OledSurface,
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                NerdIcon(
                    glyph = NerdIcons.ClockOutline,
                    color = MaterialTheme.colorScheme.primary,
                    size = 20.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatMinutes(minutesOfDay),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = UbuntuSansMonoNerdFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            NerdIcon(
                glyph = NerdIcons.Tune,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 16.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerModalDialog(
    initialMinutesOfDay: Int,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val clamped = initialMinutesOfDay.coerceIn(0, 1439)
    val timePickerState = rememberTimePickerState(
        initialHour = clamped / 60,
        initialMinute = clamped % 60,
        is24Hour = true
    )
    var useKeyboardInput by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OledSurfaceElevated,
        title = {
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
                        glyph = NerdIcons.ClockOutline,
                        color = MaterialTheme.colorScheme.primary,
                        size = 22.sp
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = { useKeyboardInput = !useKeyboardInput }) {
                    Icon(
                        imageVector = if (useKeyboardInput) Icons.Default.Schedule else Icons.Default.Keyboard,
                        contentDescription = if (useKeyboardInput) "Usar relógio" else "Digitar hora"
                    )
                }
            }
        },
        text = {
            val pickerColors = TimePickerDefaults.colors(
                clockDialColor = OledSurface,
                clockDialSelectedContentColor = IndigoOnPrimary,
                clockDialUnselectedContentColor = TextPrimary,
                selectorColor = IndigoPrimary,
                containerColor = OledSurfaceElevated,
                periodSelectorBorderColor = OledBorder,
                periodSelectorSelectedContainerColor = IndigoContainer,
                periodSelectorUnselectedContainerColor = OledSurface,
                periodSelectorSelectedContentColor = TextPrimary,
                periodSelectorUnselectedContentColor = TextSecondary,
                timeSelectorSelectedContainerColor = IndigoContainer,
                timeSelectorUnselectedContainerColor = OledSurface,
                timeSelectorSelectedContentColor = TextPrimary,
                timeSelectorUnselectedContentColor = TextSecondary
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (useKeyboardInput) {
                    TimeInput(state = timePickerState, colors = pickerColors)
                } else {
                    TimePicker(state = timePickerState, colors = pickerColors)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minutes = (timePickerState.hour * 60 + timePickerState.minute).coerceIn(0, 1439)
                    onConfirm(minutes)
                }
            ) {
                NerdIcon(glyph = NerdIcons.CheckCircle, size = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

fun formatIntervalSummary(totalMinutes: Int): String {
    val safe = totalMinutes.coerceAtLeast(1)
    val h = safe / 60
    val m = safe % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}min"
        h > 0 -> "${h}h"
        else -> "$m min"
    }
}

@Composable
fun RepeatIntervalHourMinuteFields(
    hoursText: String,
    minutesText: String,
    onHoursChange: (String) -> Unit,
    onMinutesChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isOptional: Boolean = true
) {
    val hours = hoursText.toIntOrNull() ?: 0
    val minutes = minutesText.toIntOrNull() ?: 0
    val totalMinutes = hours * 60 + minutes

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NerdIcon(
                glyph = NerdIcons.Repeat,
                color = IndigoPrimary,
                size = 16.sp
            )
            Text(
                text = "Repetir a cada:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = hoursText,
                onValueChange = { raw ->
                    onHoursChange(raw.filter { it.isDigit() }.take(2))
                },
                label = { Text("H:") },
                placeholder = { Text("0") },
                leadingIcon = { NerdIcon(glyph = NerdIcons.ClockOutline, size = 17.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = oledTextFieldColors(),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = minutesText,
                onValueChange = { raw ->
                    onMinutesChange(raw.filter { it.isDigit() }.take(3))
                },
                label = { Text("M:") },
                placeholder = { Text("0") },
                leadingIcon = { NerdIcon(glyph = NerdIcons.TimerOutline, size = 17.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = oledTextFieldColors(),
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = when {
                totalMinutes > 0 -> "Lembrar a cada ${formatIntervalSummary(totalMinutes)} ($totalMinutes min)"
                isOptional -> "Opcional: preencha horas e/ou minutos para repetir"
                else -> "Informe ao menos 1 hora ou 1 minuto"
            },
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}
