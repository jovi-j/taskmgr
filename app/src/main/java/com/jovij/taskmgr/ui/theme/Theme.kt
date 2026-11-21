package com.jovij.taskmgr.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

private val AppColorScheme = FocusOledDarkColorScheme.copy(
    secondary = IndigoPrimary,
    onSecondary = IndigoOnPrimary,
    secondaryContainer = IndigoContainer,
    onSecondaryContainer = IndigoOnContainer,
    surfaceContainerLowest = OledBackground,
    surfaceContainerLow = OledSurface,
    surfaceContainer = OledSurface,
    surfaceContainerHigh = OledSurfaceElevated,
    surfaceContainerHighest = OledSurfaceElevated
)

@Suppress("UNUSED_PARAMETER")
@Composable
fun TaskmgrTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
