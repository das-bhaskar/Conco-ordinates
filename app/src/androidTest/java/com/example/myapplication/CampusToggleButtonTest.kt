package com.example.myapplication

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.example.myapplication.data.Building
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.data.ShuttleRepo
import com.example.myapplication.logic.InterpolatingMockRouteProvider
import com.example.myapplication.logic.MockLocationProvider
import com.example.myapplication.map.CameraController
import com.example.myapplication.ui.components.CampusToggle
import com.example.myapplication.ui.models.MapUIMode
import com.example.myapplication.ui.viewmodel.MapViewModel
import com.google.android.gms.maps.model.LatLng
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CampusToggleButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockLocation: LatLng
    private lateinit var viewModel: MapViewModel
    private lateinit var mockLocationProvider: MockLocationProvider
    private lateinit var mockShuttleService: MockShuttleService

    private val testCameraController: CameraControllerTest = CameraControllerTest()


    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        CampusRepo.initialize(context)
        ShuttleRepo.initialize(context)
        mockLocationProvider = MockLocationProvider()
        mockShuttleService = MockShuttleService()
        viewModel = MapViewModel(
            locationProvider = mockLocationProvider,
            routeProvider = InterpolatingMockRouteProvider(4u),
            shuttleService = mockShuttleService
            )
    }

    fun setContent() {
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                if (viewModel.uiBuildingState.mode != MapUIMode.DIRECTIONS) {
                    CampusToggle(
                        selectedCampusName = viewModel.currentCampus?.name,
                        onCampusClick = {viewModel.onCampusSelected(it)},
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun defaultNoCampusToggleOptionSelected() {
        setContent()

        composeTestRule.onNodeWithTag("campus_toggle").assertIsDisplayed()
        composeTestRule.onNodeWithTag("SGW_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("LOY_button").assertIsDisplayed()

        composeTestRule.onNodeWithTag("SGW_button").assertIsNotSelected()
        composeTestRule.onNodeWithTag("LOY_button").assertIsNotSelected()

    }

    @Test
    fun loyolaCampusSelectsAndUnselectsOnClick() {
        setContent()

        assertEquals(null, viewModel.currentCampus?.name)

        composeTestRule.onNodeWithTag("LOY_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("LOY_button").assertIsSelected()
        composeTestRule.onNodeWithTag("SGW_button").assertIsNotSelected()
        assertEquals("Loyola", viewModel.currentCampus?.name)

        composeTestRule.onNodeWithTag("LOY_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("LOY_button").assertIsNotSelected()
        composeTestRule.onNodeWithTag("SGW_button").assertIsNotSelected()
        assertEquals(null, viewModel.currentCampus?.name)

    }

    @Test
    fun sgwCampusSelectsAndUnselectsOnClick() {
        setContent()

        assertEquals(null, viewModel.currentCampus?.name)

        composeTestRule.onNodeWithTag("SGW_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("SGW_button").assertIsSelected()
        composeTestRule.onNodeWithTag("LOY_button").assertIsNotSelected()
        assertEquals("SGW", viewModel.currentCampus?.name)

        composeTestRule.onNodeWithTag("SGW_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("SGW_button").assertIsNotSelected()
        composeTestRule.onNodeWithTag("LOY_button").assertIsNotSelected()
        assertEquals(null, viewModel.currentCampus?.name)

    }

    @Test
    fun sgwCampusSelectsFromLoyolaAndUnselectsLoyola() {
        setContent()

        assertEquals(null, viewModel.currentCampus?.name)

        composeTestRule.onNodeWithTag("LOY_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("LOY_button").assertIsSelected()
        composeTestRule.onNodeWithTag("SGW_button").assertIsNotSelected()
        assertEquals("Loyola", viewModel.currentCampus?.name)

        composeTestRule.onNodeWithTag("SGW_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("SGW_button").assertIsSelected()
        composeTestRule.onNodeWithTag("LOY_button").assertIsNotSelected()
        assertEquals("SGW", viewModel.currentCampus?.name)

    }

    @Test
    fun loyolaCampusSelectsFromSGWAndUnselectsSGW() {
        setContent()

        assertEquals(null, viewModel.currentCampus?.name)

        composeTestRule.onNodeWithTag("SGW_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("SGW_button").assertIsSelected()
        composeTestRule.onNodeWithTag("LOY_button").assertIsNotSelected()
        assertEquals("SGW", viewModel.currentCampus?.name)

        composeTestRule.onNodeWithTag("LOY_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("LOY_button").assertIsSelected()
        composeTestRule.onNodeWithTag("SGW_button").assertIsNotSelected()
        assertEquals("Loyola", viewModel.currentCampus?.name)
    }

    @Test
    fun toggleNotVisibleInDirectionsMode() {
        setContent()
        val hbuilding = CampusRepo.getBuildingByName("Henry F. Hall Building")
        composeTestRule.runOnUiThread {
            viewModel.handleMapTap(hbuilding)
            viewModel.onDirectionsRequested()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("campus_toggle").assertDoesNotExist()
    }

    @Test
    fun cameraMovementOnToggleOptionSelection() = runTest{

        mockLocation = LatLng(45.4973, -73.579)
        mockLocationProvider.mockedLocation = mockLocation

        composeTestRule.setContent {

            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                mockLocationProvider.getUserLocation { location ->
                    location?.let {
                        scope.launch {
                            testCameraController.animateTo(it, 15f)
                        }
                    }
                }
            }

            LaunchedEffect(viewModel.currentCampus) {
                if (viewModel.currentCampus != null) {
                    testCameraController.animateTo(viewModel.currentCampus!!.getGoogleCenter(), 17f)
                }
                else {
                    mockLocationProvider.getUserLocation { location ->
                        location?.let {
                            scope.launch {
                               testCameraController.animateTo(it, 15f)
                            }
                        }
                    }
                }
            }

            CampusToggle(
                selectedCampusName = viewModel.currentCampus?.name,
                onCampusClick = { name -> viewModel.onCampusSelected(name)}
            )
        }

        // Makes sure the camera starts at user location
        composeTestRule.waitForIdle()
        assertEquals(mockLocation, testCameraController.lastTarget)

        // Perform click on the SGW option (Select)
        composeTestRule.onNodeWithTag("SGW_button").performClick()
        composeTestRule.waitForIdle()

        val sgwCampus = CampusRepo.getCampusByName("SGW")
        assertNotNull(sgwCampus)
        assertEquals(sgwCampus?.getGoogleCenter(), testCameraController.lastTarget)
        composeTestRule.onNodeWithTag("SGW_button").assertIsSelected()

        // Perform click on the SGW option (Deselect)
        composeTestRule.onNodeWithTag("SGW_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(mockLocation, testCameraController.lastTarget)
        composeTestRule.onNodeWithTag("SGW_button").assertIsNotSelected()


        // Perform click on the LOY option (Select)
        composeTestRule.onNodeWithTag("LOY_button").performClick()
        composeTestRule.waitForIdle()

        val loyCampus = CampusRepo.getCampusByName("Loyola")
        assertNotNull(loyCampus)
        assertEquals(loyCampus?.getGoogleCenter(), testCameraController.lastTarget)
        composeTestRule.onNodeWithTag("LOY_button").assertIsSelected()

        // Perform click on the LOY option (Deselect)
        composeTestRule.onNodeWithTag("LOY_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(mockLocation, testCameraController.lastTarget)
        composeTestRule.onNodeWithTag("LOY_button").assertIsNotSelected()
    }
}


