package com.example.taskmgr.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// --- Fundos e Superfícies (OLED) ---
val OledBackground = Color(0xFF000000)       // Fundo principal (pixels apagados)
val OledSurface = Color(0xFF0E1117)          // Cards de tarefas e inputs
val OledSurfaceElevated = Color(0xFF161B26)  // Bottom sheets, diálogos e menus
val OledBorder = Color(0xFF1F2937)           // Bordas finas (1.dp) e divisores

// --- Destaques (Índigo) ---
val IndigoPrimary = Color(0xFF818CF8)        // FAB (+), checkboxes ativos e abas
val IndigoOnPrimary = Color(0xFF0F172A)      // Ícones/textos sobre o botão primário
val IndigoContainer = Color(0xFF1E1B4B)      // Fundo de itens selecionados / tags
val IndigoOnContainer = Color(0xFFE0E7FF)    // Texto sobre o container primário

// --- Textos ---
val TextPrimary = Color(0xFFF8FAFC)          // Títulos das tarefas
val TextSecondary = Color(0xFF94A3B8)        // Datas, horários e subtarefas
val TextDisabled = Color(0xFF475569)         // Tarefas concluídas e placeholders

// --- Cores Semânticas (Prioridades e Status) ---
val PriorityHigh = Color(0xFFF87171)         // Alta prioridade / Atrasada
val PriorityMedium = Color(0xFFFBBF24)       // Média prioridade / Hoje
val PriorityLow = Color(0xFF60A5FA)          // Baixa prioridade / Agendada
val TaskCompleted = Color(0xFF34D399)        // Tarefa concluída

// --- Esquema de Cores Material 3 ---
val FocusOledDarkColorScheme = darkColorScheme(
    primary = IndigoPrimary,
    onPrimary = IndigoOnPrimary,
    primaryContainer = IndigoContainer,
    onPrimaryContainer = IndigoOnContainer,
    background = OledBackground,
    onBackground = TextPrimary,
    surface = OledSurface,
    onSurface = TextPrimary,
    surfaceVariant = OledSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = OledBorder,
    outlineVariant = OledBorder,
    error = PriorityHigh,
    onError = Color(0xFF450A0A)
)
