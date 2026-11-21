package com.taskmgr.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskmgr.ui.theme.IndigoContainer
import com.example.taskmgr.ui.theme.IndigoPrimary
import com.example.taskmgr.ui.theme.OledBorder
import com.example.taskmgr.ui.theme.OledSurface
import com.example.taskmgr.ui.theme.OledSurfaceElevated
import com.example.taskmgr.ui.theme.TextDisabled
import com.example.taskmgr.ui.theme.TextPrimary
import com.example.taskmgr.ui.theme.TextSecondary
import com.example.taskmgr.ui.theme.UbuntuSansMonoNerdFontFamily
import com.taskmgr.domain.model.HabitGroup
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class GroupColorOption(
    val label: String,
    val argb: Long
)

object HabitGroupColors {
    val palette: List<GroupColorOption> = listOf(
        GroupColorOption("Vermelho", 0xFFEF4444),
        GroupColorOption("Laranja", 0xFFF97316),
        GroupColorOption("Âmbar", 0xFFFBBF24),
        GroupColorOption("Verde", 0xFF22C55E),
        GroupColorOption("Esmeralda", 0xFF34D399),
        GroupColorOption("Ciano", 0xFF22D3EE),
        GroupColorOption("Azul", 0xFF3B82F6),
        GroupColorOption("Índigo", 0xFF818CF8),
        GroupColorOption("Roxo", 0xFFA855F7),
        GroupColorOption("Rosa", 0xFFEC4899)
    )
}

@Composable
fun oledTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = OledSurface,
    unfocusedContainerColor = OledSurface,
    focusedBorderColor = IndigoPrimary,
    unfocusedBorderColor = OledBorder,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = IndigoPrimary,
    unfocusedLabelColor = TextSecondary,
    focusedPlaceholderColor = TextDisabled,
    unfocusedPlaceholderColor = TextDisabled,
    focusedLeadingIconColor = IndigoPrimary,
    unfocusedLeadingIconColor = TextSecondary,
    cursorColor = IndigoPrimary
)

fun contrastContentColor(backgroundColor: Color): Color {
    return if (backgroundColor.luminance() > 0.45f) Color(0xFF0F172A) else Color.White
}

@Composable
fun HabitBadgeIcon(
    iconGlyph: String?,
    groupColorArgb: Long?,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: TextUnit = 20.sp
) {
    val bgColor = if (groupColorArgb != null) Color(groupColorArgb.toInt()) else IndigoContainer
    val fgColor = if (groupColorArgb != null) contrastContentColor(bgColor) else IndigoPrimary
    val borderColor = if (groupColorArgb != null) bgColor.copy(alpha = 0.65f) else IndigoPrimary.copy(alpha = 0.45f)
    val resolvedGlyph = iconGlyph?.takeIf { it.isNotBlank() } ?: NerdIcons.Fire

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        NerdIcon(
            glyph = resolvedGlyph,
            color = fgColor,
            size = iconSize
        )
    }
}

@Composable
fun DateSelectorField(
    label: String,
    epochDay: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatted = LocalDate.ofEpochDay(epochDay)
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
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
                    glyph = NerdIcons.CalendarToday,
                    color = IndigoPrimary,
                    size = 20.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = formatted,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = UbuntuSansMonoNerdFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary
                    )
                }
            }
            NerdIcon(
                glyph = NerdIcons.Calendar,
                color = TextSecondary,
                size = 16.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModalDialog(
    initialEpochDay: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val initialMillis = LocalDate.ofEpochDay(initialEpochDay)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = DatePickerDefaults.colors(containerColor = OledSurfaceElevated),
        confirmButton = {
            Button(
                onClick = {
                    val millis = state.selectedDateMillis ?: initialMillis
                    val pickedDay = Instant.ofEpochMilli(millis)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                        .toEpochDay()
                    onConfirm(pickedDay)
                }
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    ) {
        DatePicker(
            state = state,
            colors = DatePickerDefaults.colors(
                containerColor = OledSurfaceElevated,
                titleContentColor = TextPrimary,
                headlineContentColor = TextPrimary,
                weekdayContentColor = TextSecondary,
                subheadContentColor = TextSecondary,
                dayContentColor = TextPrimary,
                selectedDayContainerColor = IndigoPrimary,
                selectedDayContentColor = Color(0xFF0F172A),
                todayDateBorderColor = IndigoPrimary,
                todayContentColor = IndigoPrimary
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitIconPickerDialog(
    selectedGlyph: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val filtered = remember(search, selectedCategory) {
        val query = search.trim()
        NerdIconCatalog.all.filter { item ->
            val matchesCat = selectedCategory == null || item.category == selectedCategory
            val matchesSearch = query.isEmpty() ||
                item.label.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true) ||
                item.keywords.any { it.contains(query, ignoreCase = true) }
            matchesCat && matchesSearch
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OledSurfaceElevated,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NerdIcon(glyph = selectedGlyph, color = accentColor, size = 22.sp)
                Text("Selecionar Ícone", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Pesquisar ícone (ex.: casa, água, treino)") },
                    leadingIcon = { NerdIcon(glyph = NerdIcons.Tag, size = 16.sp) },
                    singleLine = true,
                    colors = oledTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("Todos") }
                    )
                    NerdIconCatalog.categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filtered.isEmpty()) {
                        Text(
                            text = "Nenhum ícone encontrado para \"$search\".",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            filtered.forEach { option ->
                                val isSelected = option.glyph == selectedGlyph
                                Surface(
                                    onClick = { onSelect(option.glyph) },
                                    shape = CircleShape,
                                    color = if (isSelected) accentColor else OledSurface,
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) TextPrimary else OledBorder
                                    ),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        NerdIcon(
                                            glyph = option.glyph,
                                            color = if (isSelected) contrastContentColor(accentColor) else TextPrimary,
                                            size = 21.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitGroupEditorDialog(
    initialGroup: HabitGroup?,
    onDismiss: () -> Unit,
    onSave: (HabitGroup) -> Unit
) {
    var name by remember { mutableStateOf(initialGroup?.name ?: "") }
    var colorArgb by remember { mutableLongStateOf(initialGroup?.colorArgb ?: HabitGroupColors.palette.first().argb) }
    var iconGlyph by remember { mutableStateOf(initialGroup?.iconGlyph ?: NerdIcons.Home) }
    var showIconPicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val selectedColor = Color(colorArgb.toInt())

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OledSurfaceElevated,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HabitBadgeIcon(
                    iconGlyph = iconGlyph,
                    groupColorArgb = colorArgb,
                    size = 34.dp,
                    iconSize = 17.sp
                )
                Text(
                    text = if (initialGroup == null) "Novo Grupo de Hábitos" else "Editar Grupo",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Nome do grupo (ex.: Casa, Saúde)") },
                    leadingIcon = { NerdIcon(glyph = NerdIcons.Tag, size = 18.sp) },
                    singleLine = true,
                    colors = oledTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Cor do grupo",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HabitGroupColors.palette.forEach { option ->
                        val isSelected = option.argb == colorArgb
                        val c = Color(option.argb.toInt())
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) TextPrimary else OledBorder,
                                    shape = CircleShape
                                )
                                .clickable { colorArgb = option.argb },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                NerdIcon(
                                    glyph = NerdIcons.CheckCircle,
                                    color = contrastContentColor(c),
                                    size = 16.sp
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Ícone do grupo",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Surface(
                    onClick = { showIconPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    color = OledSurface,
                    border = BorderStroke(1.dp, OledBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HabitBadgeIcon(
                                iconGlyph = iconGlyph,
                                groupColorArgb = colorArgb,
                                size = 36.dp,
                                iconSize = 18.sp
                            )
                            Text(
                                text = "Alterar ícone do grupo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                        NerdIcon(glyph = NerdIcons.Tune, color = TextSecondary, size = 16.sp)
                    }
                }

                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    if (cleanName.isEmpty()) {
                        error = "Informe o nome do grupo."
                        return@Button
                    }
                    onSave(
                        HabitGroup(
                            id = initialGroup?.id ?: 0L,
                            name = cleanName,
                            colorArgb = colorArgb,
                            iconGlyph = iconGlyph
                        )
                    )
                }
            ) {
                NerdIcon(glyph = NerdIcons.CheckCircle, size = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )

    if (showIconPicker) {
        HabitIconPickerDialog(
            selectedGlyph = iconGlyph,
            accentColor = selectedColor,
            onDismiss = { showIconPicker = false },
            onSelect = { picked ->
                iconGlyph = picked
                showIconPicker = false
            }
        )
    }
}
