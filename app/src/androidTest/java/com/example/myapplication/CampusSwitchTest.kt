package com.example.myapplication

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CampusSwitchTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MapsActivity>()

    @Test
    fun testInitialState_DisplaysSGWCampus() {
        // 1. Wait for the map (Handling that 6.6s lag)
        composeTestRule.waitUntil(15000) {
            composeTestRule.onAllNodesWithTag("google_map").fetchSemanticsNodes().isNotEmpty()
        }

        // 2. Find and interact with the Search Bar
        // We use a broader matcher for the placeholder text
        val searchMatcher = hasText("Search", substring = true, ignoreCase = true)
        composeTestRule.onNode(searchMatcher).performTextInput("Hall")

        // 3. Verify the building appears using a SUBSTRING
        // This will match "Concordia University Henry F." even if it's truncated
        composeTestRule.onNodeWithText("Concordia University Henry F.", substring = true, ignoreCase = true)
            .assertIsDisplayed()
            .performClick()

        // 4. Final verification that the node exists in the UI tree
        composeTestRule.onNodeWithText("Concordia University Henry F.", substring = true, ignoreCase = true)
            .assertExists()
    }
}