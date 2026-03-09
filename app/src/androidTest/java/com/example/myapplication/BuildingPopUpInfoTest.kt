package com.example.myapplication

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.data.ShuttleRepo
import com.example.myapplication.logic.InterpolatingMockRouteProvider
import com.example.myapplication.logic.MockLocationProvider
import com.example.myapplication.ui.components.CampusToggle
import com.example.myapplication.ui.viewmodel.MapViewModel
import com.google.android.gms.maps.model.LatLng
import org.junit.Before
import org.junit.Rule

class BuildingPopUpInfoTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockLocation: LatLng
    private lateinit var viewModel: MapViewModel
    private lateinit var mockLocationProvider: MockLocationProvider
    private lateinit var mockShuttleService: MockShuttleService

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
                CampusToggle(
                    selectedCampusName = viewModel.currentCampus?.name,
                    onCampusClick = {viewModel.onCampusSelected(it)},
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
        }
        composeTestRule.waitForIdle()
    }
}