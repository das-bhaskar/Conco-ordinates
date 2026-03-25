package com.example.myapplication

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.myapplication.data.Building
import com.example.myapplication.data.JsonLatLng
import com.example.myapplication.ui.components.BuildingInfoPopup
import com.example.myapplication.ui.models.BuildingUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BuildingInfoPopupTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val hallBuilding = Building(
        name = "Henry F. Hall Building",
        code = "H",
        wayID = 1L,
        address = "1455 De Maisonneuve Blvd. W.",
        outline = listOf(JsonLatLng(45.497, -73.579))
    )

    @Test
    fun popupDisplaysBuildingNameAndAddress() {
        composeTestRule.setContent {
            BuildingInfoPopup(
                building = hallBuilding,
                uiState = BuildingUiState(address = hallBuilding.address),
                onDismiss = {},
                onDirectionsClick = {}
            )
        }

        composeTestRule.onNodeWithText("Henry F. Hall Building").assertIsDisplayed()
        composeTestRule.onNodeWithText("1455 De Maisonneuve Blvd. W.").assertIsDisplayed()
    }

    @Test
    fun popupShowsAddressLoadingWhenAddressMissing() {
        composeTestRule.setContent {
            BuildingInfoPopup(
                building = hallBuilding,
                uiState = BuildingUiState(address = null),
                onDismiss = {},
                onDirectionsClick = {}
            )
        }

        composeTestRule.onNodeWithText("Address loading...").assertIsDisplayed()
    }

    @Test
    fun directionsButtonInvokesCallback() {
        var clicks = 0

        composeTestRule.setContent {
            BuildingInfoPopup(
                building = hallBuilding,
                uiState = BuildingUiState(address = hallBuilding.address),
                onDismiss = {},
                onDirectionsClick = { clicks++ }
            )
        }

        composeTestRule.onNodeWithText("Directions").performClick()
        assertEquals(1, clicks)
    }
}
