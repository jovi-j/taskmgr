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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskmgr.ui.theme.IndigoPrimary
import com.example.taskmgr.ui.theme.OledBorder
import com.example.taskmgr.ui.theme.OledSurface
import com.example.taskmgr.ui.theme.OledSurfaceElevated
import com.example.taskmgr.ui.theme.PriorityHigh
import com.example.taskmgr.ui.theme.PriorityMedium
import com.example.taskmgr.ui.theme.TaskCompleted
import com.example.taskmgr.ui.theme.TextPrimary
import com.example.taskmgr.ui.theme.TextSecondary
import com.taskmgr.domain.logic.DaysOfWeekMask
import com.taskmgr.domain.logic.HabitCompletionCalculator
import com.taskmgr.domain.model.CompletionType
import com.taskmgr.domain.model.Habit
import com.taskmgr.domain.model.HabitGroup
import com.taskmgr.ui.components.HabitBadgeIcon
import com.taskmgr.ui.components.NerdIcon
import com.taskmgr.ui.components.NerdIcons
import com.taskmgr.ui.components.formatMinutes
import com.taskmgr.ui.components.shortPtBr
import com.taskmgr.ui.viewmodel.HabitWithLog

@Composable
fun HabitGroupsScreen(
    habitGroups: List<HabitGroup>,
    habitsWithStats: List<HabitWithLog>,
    onAddHabitGroup: () -> Unit,
    onEditHabitGroup: (HabitGroup) -> Unit,
    onDeleteHabitGroup: (Long) -> Unit,
    onCreateHabitInGroup: (Long?) -> Unit,
    onEditHabit: (Habit) -> Unit,
    onDeleteHabit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val habitsByGroupId = remember(habitsWithStats) {
        habitsWithStats.groupBy { it.habit.groupId }
    }
    val ungroupedHabits = habitsByGroupId[null].orEmpty()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "groups_header", contentType = "groups_header") {
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
                        glyph = NerdIcons.Tag,
                        color = IndigoPrimary,
                        size = 22.sp
                    )
                    Text(
                        text = "Grupos de Hábitos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                FilledTonalButton(onClick = onAddHabitGroup) {
                    NerdIcon(glyph = NerdIcons.PlusCircle, size = 17.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Novo Grupo")
                }
            }
        }

        if (habitGroups.isEmpty() && ungroupedHabits.isEmpty()) {
            item(key = "groups_empty", contentType = "groups_empty") {
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
                            glyph = NerdIcons.Tag,
                            color = IndigoPrimary,
                            size = 34.sp
                        )
                        Text(
                            text = "Nenhum grupo de hábitos criado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Crie grupos como 'Casa' (vermelho) ou 'Saúde' (verde) para organizar seus hábitos com cor e ícone circular e acompanhar cada grupo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(
                items = habitGroups,
                key = { "group_section_${it.id}" },
                contentType = { "group_section" }
            ) { group ->
                val groupHabits = habitsByGroupId[group.id].orEmpty()
                HabitGroupSectionCard(
                    group = group,
                    habits = groupHabits,
                    onEditGroup = { onEditHabitGroup(group) },
                    onDeleteGroup = { onDeleteHabitGroup(group.id) },
                    onAddHabitToGroup = { onCreateHabitInGroup(group.id) },
                    onEditHabit = onEditHabit,
                    onDeleteHabit = onDeleteHabit
                )
            }

            if (ungroupedHabits.isNotEmpty()) {
                item(key = "ungrouped_section", contentType = "ungrouped_section") {
                    UngroupedHabitsSectionCard(
                        habits = ungroupedHabits,
                        onAddHabit = { onCreateHabitInGroup(null) },
                        onEditHabit = onEditHabit,
                        onDeleteHabit = onDeleteHabit
                    )
                }
            }
        }

        item(key = "groups_bottom_spacer", contentType = "spacer") {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun HabitGroupSectionCard(
    group: HabitGroup,
    habits: List<HabitWithLog>,
    onEditGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    onAddHabitToGroup: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    onDeleteHabit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val completedToday = habits.count { it.isCompleted }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OledSurface),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HabitBadgeIcon(
                    iconGlyph = group.iconGlyph,
                    groupColorArgb = group.colorArgb,
                    size = 42.dp,
                    iconSize = 20.sp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    val subtitle = if (habits.isEmpty()) {
                        "Nenhum hábito neste grupo"
                    } else {
                        "${habits.size} hábito(s) • $completedToday concluído(s) hoje"
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onEditGroup) {
                    NerdIcon(glyph = NerdIcons.Pencil, color = TextSecondary, size = 18.sp)
                }
                IconButton(onClick = onDeleteGroup) {
                    NerdIcon(glyph = NerdIcons.DeleteOutline, color = PriorityHigh, size = 18.sp)
                }
            }

            HorizontalDivider(color = OledBorder, thickness = 1.dp)

            if (habits.isEmpty()) {
                Text(
                    text = "Adicione hábitos a este grupo para que herdem o fundo circular da cor de '${group.name}'.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    habits.forEach { habitWithLog ->
                        GroupHabitRow(
                            habitWithLog = habitWithLog,
                            groupColorArgb = group.colorArgb,
                            defaultIconGlyph = group.iconGlyph,
                            onEdit = { onEditHabit(habitWithLog.habit) },
                            onDelete = { onDeleteHabit(habitWithLog.habit.id) }
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onAddHabitToGroup,
                modifier = Modifier.fillMaxWidth()
            ) {
                NerdIcon(glyph = NerdIcons.Plus, size = 15.sp, color = Color(group.colorArgb))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Adicionar hábito em ${group.name}")
            }
        }
    }
}

@Composable
private fun UngroupedHabitsSectionCard(
    habits: List<HabitWithLog>,
    onAddHabit: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    onDeleteHabit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OledSurface),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HabitBadgeIcon(
                    iconGlyph = NerdIcons.Fire,
                    groupColorArgb = 0xFF818CF8L,
                    size = 42.dp,
                    iconSize = 20.sp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sem Grupo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${habits.size} hábito(s) sem grupo associado",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            HorizontalDivider(color = OledBorder, thickness = 1.dp)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                habits.forEach { habitWithLog ->
                    GroupHabitRow(
                        habitWithLog = habitWithLog,
                        groupColorArgb = 0xFF818CF8L,
                        defaultIconGlyph = NerdIcons.Fire,
                        onEdit = { onEditHabit(habitWithLog.habit) },
                        onDelete = { onDeleteHabit(habitWithLog.habit.id) }
                    )
                }
            }

            OutlinedButton(
                onClick = onAddHabit,
                modifier = Modifier.fillMaxWidth()
            ) {
                NerdIcon(glyph = NerdIcons.Plus, size = 15.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Novo hábito sem grupo")
            }
        }
    }
}

@Composable
private fun GroupHabitRow(
    habitWithLog: HabitWithLog,
    groupColorArgb: Long,
    defaultIconGlyph: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val habit = habitWithLog.habit
    val targetLabel = when (habit.completionType) {
        CompletionType.BOOLEAN -> "Meta: 1x (${habit.targetUnitLabel.ifBlank { "Sim" }})"
        CompletionType.COUNT_PER_DAY, CompletionType.CUSTOM_UNIT ->
            "Meta: ${HabitCompletionCalculator.formatCleanNumber(habit.targetAmount)} ${habit.targetUnitLabel}"
    }
    val daysSummary = if ((habit.activeDaysMask and DaysOfWeekMask.ALL_DAYS) == DaysOfWeekMask.ALL_DAYS) {
        "Todos os dias"
    } else {
        DaysOfWeekMask.toDays(habit.activeDaysMask).joinToString(", ") { it.shortPtBr() }
    }
    val timePart = habit.scheduledMinutesOfDay?.let { " • ${formatMinutes(it)}" }.orEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = OledSurfaceElevated),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HabitBadgeIcon(
                iconGlyph = habit.iconGlyph ?: defaultIconGlyph,
                groupColorArgb = groupColorArgb,
                size = 36.dp,
                iconSize = 17.sp
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    if (habitWithLog.isCompleted) {
                        NerdIcon(
                            glyph = NerdIcons.CheckCircle,
                            color = TaskCompleted,
                            size = 15.sp
                        )
                    }
                    if (!habit.isActive) {
                        Text(
                            text = "(Inativo)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                Text(
                    text = "$targetLabel • $daysSummary$timePart",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        NerdIcon(
                            glyph = NerdIcons.Fire,
                            color = PriorityMedium,
                            size = 13.sp
                        )
                        Text(
                            text = "Seq: ${habitWithLog.stats.currentStreakDays}d",
                            style = MaterialTheme.typography.labelSmall,
                            color = PriorityMedium
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        NerdIcon(
                            glyph = NerdIcons.Trophy,
                            color = TextSecondary,
                            size = 13.sp
                        )
                        Text(
                            text = "Rec: ${habitWithLog.stats.longestStreakDays}d • ${habitWithLog.stats.totalCompletedDays}d",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            IconButton(onClick = onEdit) {
                NerdIcon(glyph = NerdIcons.Pencil, color = TextSecondary, size = 17.sp)
            }
            IconButton(onClick = onDelete) {
                NerdIcon(glyph = NerdIcons.DeleteOutline, color = PriorityHigh, size = 17.sp)
            }
        }
    }
}
