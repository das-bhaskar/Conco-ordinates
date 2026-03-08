package com.example.myapplication.ui.viewmodel

import android.content.Context
import com.example.myapplication.data.Building
import com.example.myapplication.data.JsonLatLng
import com.example.myapplication.logic.*
import com.example.myapplication.ui.models.MapUIMode
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val mockLocationProvider: LocationProvider = mock()
    private val mockRouteProvider: RouteProvider = mock()
    private val mockShuttleService: ShuttleService = mock()
    private val mockContext: Context = mock()

    private lateinit var viewModel: MapViewModel

    // Helper to create valid Building objects with all required fields
    private fun createTestBuilding(
        name: String = "Test Building",
        address: String = "123 Test St"
    ) = Building(
        name = name,
        code = "TEST",
        wayID = 1L,
        address = address,
        outline = listOf(JsonLatLng(45.0, -73.0))
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MapViewModel(
            locationProvider = mockLocationProvider,
            routeProvider = mockRouteProvider,
            shuttleService = mockShuttleService
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `handleMapTap updates state correctly`() {
        val testBuilding = createTestBuilding(name = "Hall Building", address = "1455 De Maisonneuve")

        viewModel.handleMapTap(testBuilding)

        val state = viewModel.uiBuildingState
        assertTrue(state.isVisible)
        assertEquals("Hall Building", state.building?.name)
        assertEquals("1455 De Maisonneuve", state.address)
    }

    @Test
    fun `onDirectionsRequested sets mode to DIRECTIONS`() {
        viewModel.onDirectionsRequested()
        assertEquals(MapUIMode.DIRECTIONS, viewModel.uiBuildingState.mode)
    }

    @Test
    fun `calculateRouteWithState updates UI state with route data`() = runTest {
        // Arrange
        val start = LatLng(45.497, -73.579)
        val end = LatLng(45.498, -73.580)
        val fakeRoute = RouteData(
            points = listOf(start, end),
            duration = "10 mins",
            distance = "1.2 km"
        )

        // Mock the route provider response
        whenever(mockRouteProvider.getRoute(any(), any(), any())).thenReturn(fakeRoute)

        // Set up the state: Directions mode + Destination selected
        val building = createTestBuilding(name = "Dest Building")
        val result = SearchResult.BuildingResult(building)

        viewModel.onDirectionsRequested()
        // We need to set a start point so the route calculation doesn't return early
        viewModel.processLocationUpdate(start, isForce = true)
        viewModel.handleSearchResult(result, mockContext)

        // Act
        advanceUntilIdle() // Process coroutines launched in viewModelScope

        // Assert
        val state = viewModel.uiBuildingState
        assertEquals("10 mins", state.routeDuration)
        assertEquals("1.2 km", state.routeDistance)
        assertEquals(2, state.routePoints.size)
    }

    @Test
    fun `swapLocations interchanges start and destination names`() = runTest {
        // Arrange
        viewModel.onDirectionsRequested()

        // Use search results to populate names
        val b1 = createTestBuilding(name = "Building A")
        val b2 = createTestBuilding(name = "Building B")

        viewModel.onSearchQueryChanged("", "start")
        viewModel.handleSearchResult(SearchResult.BuildingResult(b1), mockContext)

        viewModel.onSearchQueryChanged("", "dest")
        viewModel.handleSearchResult(SearchResult.BuildingResult(b2), mockContext)

        // Act
        viewModel.swapLocations()

        // Assert
        assertEquals("Building B", viewModel.uiBuildingState.startLocationName)
        assertEquals("Building A", viewModel.uiBuildingState.destinationName)
    }


    @Test
    fun `onTransportModeChanged triggers new route calculation`() = runTest {
        // Arrange
        val start = LatLng(45.497, -73.579)
        viewModel.processLocationUpdate(start, isForce = true)
        viewModel.onDirectionsRequested()
        viewModel.handleSearchResult(SearchResult.BuildingResult(createTestBuilding()), mockContext)

        // Act
        viewModel.onTransportModeChanged("walking")
        advanceUntilIdle()

        // Assert
        verify(mockRouteProvider, atLeastOnce()).getRoute(any(), any(), eq("walking"))
    }

    @Test
    fun `calculateRouteWithState returns early if start point is missing`() = runTest {
        // Ensure lastProcessedLocation and startPoint are null
        // (They are null by default in a new ViewModel)

        viewModel.calculateRouteWithState()

        // Assert: No route provider calls should be made
        verifyNoInteractions(mockRouteProvider)
    }

    @Test
    fun `handleSearchResult with CurrentLocation triggers location provider`() {
        // Arrange
        val fakeLocation = LatLng(45.0, -73.0)
        whenever(mockLocationProvider.getUserLocation(any())).thenAnswer {
            val callback = it.arguments[0] as (LatLng?) -> Unit
            callback(fakeLocation)
        }

        // Act
        viewModel.handleSearchResult(SearchResult.CurrentLocation, mockContext)

        // Assert
        assertEquals(fakeLocation, viewModel.mapEvent)
        verify(mockLocationProvider).getUserLocation(any())
    }

    @Test
    fun `toggleSearchExpansion updates field and expansion state`() {
        viewModel.toggleSearchExpansion(true, "start")

        assertTrue(viewModel.uiBuildingState.isSearchExpanded)
        // Note: your VM stores activeSearchField separately from the state
        // Verify private field indirectly or via effect on handleSearchResult
    }

    @Test
    fun `refreshShuttleStatus is NOT called if start point is manually set`() {
        viewModel.onTransportModeChanged("shuttle")

        // Set a manual start point name (not "Your position")
        viewModel.onStartQueryChanged("Hall Building")

        viewModel.processLocationUpdate(LatLng(45.497, -73.579))

        // Verify it was never called because startLocationName != "Your position"
        verify(mockShuttleService, never()).resolveNearestStop(any())
    }

    @Test
    fun `calculateRouteWithState handles null route gracefully`() = runTest {
        // Arrange: Mock provider to return null
        whenever(mockRouteProvider.getRoute(any(), any(), any())).thenReturn(null)

        viewModel.onDirectionsRequested()
        viewModel.processLocationUpdate(LatLng(45.0, -73.0), isForce = true)

        // Act
        viewModel.calculateRouteWithState()
        advanceUntilIdle()

        // Assert: Verify UI shows fallback values ("-- min")
        assertEquals("-- min", viewModel.uiBuildingState.routeDuration)
        assertTrue(viewModel.uiBuildingState.routePoints.isEmpty())
    }

    @Test
    fun `onCampusSelected handles invalid campus name gracefully`() {
        // Check if it's null initially due to uninitialized Repo
        val initialCampus = viewModel.currentCampus

        // Act
        viewModel.onCampusSelected("NonExistentCampus")

        // Assert: The state should not have changed from whatever it was
        assertEquals(initialCampus, viewModel.currentCampus)
    }

  
}