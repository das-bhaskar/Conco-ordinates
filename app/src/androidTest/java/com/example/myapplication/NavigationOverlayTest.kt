package com.example.myapplication

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.myapplication.ui.components.NavigationOverlay
import com.example.myapplication.ui.models.NavigationState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NavigationOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun overlayDisplaysInstructionAndButtons() {
        composeTestRule.setContent {
            NavigationOverlay(
                navState = NavigationState(currentInstruction = "Walk straight"),
                onRecenterClick = {},
                onExit = {},
                destinationName = { "Hall Building" }
            )
        }

        composeTestRule.onNodeWithText("Walk straight").assertIsDisplayed()
        composeTestRule.onNodeWithText("RECENTER").assertIsDisplayed()
        composeTestRule.onNodeWithText("EXIT").assertIsDisplayed()
    }

    @Test
    fun overlayButtonsInvokeCallbacks() {
        var recenterClicks = 0
        var exitClicks = 0

        composeTestRule.setContent {
            NavigationOverlay(
                navState = NavigationState(),
                onRecenterClick = { recenterClicks++ },
                onExit = { exitClicks++ },
                destinationName = { "Hall Building" }
            )
        }

        composeTestRule.onNodeWithText("RECENTER").performClick()
        composeTestRule.onNodeWithText("EXIT").performClick()

        assertEquals(1, recenterClicks)
        assertEquals(1, exitClicks)
    }

    @Test
    fun arrivalDialogAppearsWhenUserHasArrived() {
        composeTestRule.setContent {
            NavigationOverlay(
                navState = NavigationState(hasArrived = true),
                onRecenterClick = {},
                onExit = {},
                destinationName = { "Hall Building" }
            )
        }

        composeTestRule.onNodeWithText("Destination Reached").assertIsDisplayed()
        composeTestRule.onNodeWithText("You have arrived at Hall Building").assertIsDisplayed()
        composeTestRule.onNodeWithText("END TRIP").assertIsDisplayed()
    }
}
