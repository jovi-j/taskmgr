package com.taskmgr.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.taskmgr.ui.theme.TaskmgrTheme
import com.taskmgr.domain.model.ReminderItemType
import com.taskmgr.domain.model.SnoozeOption
import com.taskmgr.ui.fullscreen.FullScreenReminderScreen
import com.taskmgr.ui.fullscreen.FullScreenReminderUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FullScreenReminderUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun habitFullScreen_displaysProgressIncrementAndAllSixSnoozeOptions() {
        var primaryClicked = false
        var selectedSnooze: SnoozeOption? = null

        val state = FullScreenReminderUiState(
            itemType = ReminderItemType.HABIT,
            itemId = 1L,
            epochDay = 20_000L,
            title = "Beber água",
            momentName = "Manhã",
            progressText = "750.0 / 2000.0 ml",
            progressFraction = 0.375f,
            primaryButtonLabel = "+250 ml"
        )

        composeRule.setContent {
            TaskmgrTheme(darkTheme = true, dynamicColor = false) {
                FullScreenReminderScreen(
                    uiState = state,
                    onPrimaryAction = { primaryClicked = true },
                    onSnooze = { selectedSnooze = it }
                )
            }
        }

        composeRule.onNodeWithText("Hábito").assertIsDisplayed()
        composeRule.onNodeWithText("Beber água").assertIsDisplayed()
        composeRule.onNodeWithText("Momento: Manhã").assertIsDisplayed()
        composeRule.onNodeWithText("750.0 / 2000.0 ml").assertIsDisplayed()
        composeRule.onNodeWithText("+250 ml").assertIsDisplayed().performClick()
        assertTrue(primaryClicked)

        val expectedLabels = listOf("5 min", "10 min", "30 min", "1 hora", "3 horas", "Até amanhã")
        expectedLabels.forEach { label ->
            composeRule.onNodeWithText(label).assertIsDisplayed()
        }

        composeRule.onNodeWithText("30 min").performClick()
        assertEquals(SnoozeOption.MINUTES_30, selectedSnooze)

        composeRule.onNodeWithText("Até amanhã").performClick()
        assertEquals(SnoozeOption.UNTIL_TOMORROW, selectedSnooze)
    }
}
