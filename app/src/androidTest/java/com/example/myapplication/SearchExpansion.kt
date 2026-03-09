package com.example.myapplication

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.data.ShuttleRepo
import com.example.myapplication.logic.InterpolatingMockRouteProvider
import com.example.myapplication.logic.MockLocationProvider
import com.example.myapplication.ui.components.BuildingInfoPopup
import com.example.myapplication.ui.components.CampusSearchBar
import com.example.myapplication.ui.components.CampusToggle
import com.example.myapplication.ui.components.DirectionsHeader
import com.example.myapplication.ui.components.DirectionsInfoPopup
import com.example.myapplication.ui.models.MapUIMode
import com.example.myapplication.ui.viewmodel.MapViewModel
import org.junit.Before
import org.junit.Rule

class SearchExpansion {

    @get:Rule
    val composeTestRule = createComposeRule()

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
                if (viewModel.uiBuildingState.mode != MapUIMode.DIRECTIONS) {
                    CampusToggle(
                        selectedCampusName = viewModel.currentCampus?.name,
                        onCampusClick = {viewModel.onCampusSelected(it)},
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                    CampusSearchBar(
                        query = viewModel.searchQuery,
                        results = viewModel.searchResults,
                        onQueryChange = {viewModel.onSearchQueryChanged(it)},
                        onResultClick = {}
                    )
                    ExtendedFloatingActionButton(
                        onClick = {},
                        modifier = Modifier.align(Alignment.BottomEnd),
                        icon = {Icon(Icons.Default.MyLocation, null)},
                        text = {Text("RECENTER")}
                    )
                }

                if (viewModel.uiBuildingState.isVisible && viewModel.uiBuildingState.mode == MapUIMode.PREVIEW) {
                    viewModel.uiBuildingState.building?.let {
                        building -> BuildingInfoPopup(
                            building = building,
                            uiState = viewModel.uiBuildingState,
                            onDismiss = {viewModel.handleMapTap(null)},
                            onDirectionsClick = {viewModel.onDirectionsRequested()}
                        )
                    }
                }

                if (viewModel.uiBuildingState.mode == MapUIMode.DIRECTIONS) {
                    if (!viewModel.uiBuildingState.isSearchExpanded) {
                        DirectionsInfoPopup(
                            uiState            = viewModel.uiBuildingState,
                            onModeChange       = { viewModel.onTransportModeChanged(it) },
                            onStartClick       = { viewModel.toggleSearchExpansion(true, "start") },
                            onDestinationClick = { viewModel.toggleSearchExpansion(true, "dest") },
                            onSwapClick        = { viewModel.swapLocations() },
                            onClose            = { viewModel.onBackToPreview() },
                            onStartNavigation  = {},
                            modifier           = Modifier.align(Alignment.BottomCenter)
                        )
                    } else {
                        DirectionsHeader(
                            uiState            = viewModel.uiBuildingState,
                            onBackClick        = { viewModel.toggleSearchExpansion(false) },
                            onStartQueryChange = { viewModel.onSearchQueryChanged(it, "start") },
                            onDestQueryChange  = { viewModel.onSearchQueryChanged(it, "dest") },
                            modifier           = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
    }

}