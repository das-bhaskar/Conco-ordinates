package com.example.myapplication

import android.Manifest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.example.myapplication.data.CampusRepo
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.lifecycle.Lifecycle

@RunWith(AndroidJUnit4::class)
class MapsActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MapsActivity>()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    @Before
    fun setup() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            // This ensure Maps is initialized on the UI thread before Activity start
            // otherwise, CameraUpdateFactory errors
            MapsInitializer.initialize(context)
        }
    }

    @Test
    fun testUIElementsDisplayed() {
        composeTestRule.waitForIdle()

        // Use useUnmergedTree = true to find the GoogleMap node
        composeTestRule.onNodeWithTag("google_map", useUnmergedTree = true).assertExists()

        // Check toggle buttons
        composeTestRule.onNodeWithText("SGW").assertExists()
        composeTestRule.onNodeWithText("LOY").assertExists()
    }

    @Test
    fun testToggleCampusSelection() {
        composeTestRule.waitForIdle()

        // Switch to LOY
        composeTestRule.onNodeWithText("LOY").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("LOY_button").assertIsSelected()

        // Switch back to SGW
        composeTestRule.onNodeWithText("SGW").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("SGW_button").assertIsSelected()
        composeTestRule.onNodeWithTag("LOY_button").assertIsNotSelected()
    }

    @Test
    fun testMapInteraction() {
        composeTestRule.waitForIdle()
        // Click on the map to work with logic
        composeTestRule.onNodeWithTag("google_map").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun testActivityLifecycleAndRecreation() {
        composeTestRule.waitForIdle()
        // This covers state restoration
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        
        // This covers transitions
        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeTestRule.waitForIdle()
    }

    @Test
    fun testCampusRepoIntegration() {
        val sgwLatLng = LatLng(45.4973, -73.5790)
        assertNotNull(CampusRepo.getCampus(sgwLatLng))
    }
}
