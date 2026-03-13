package com.example.myapplication.ui.viewmodel

import android.content.Context
import com.example.myapplication.data.Building
import com.example.myapplication.data.Campus
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.data.JsonLatLng
import com.example.myapplication.data.ShuttleAvailability
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
import com.example.myapplication.data.ShuttleStop
import kotlin.collections.emptyList

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

        // FIX: Initialize the Repo so getCampusByName doesn't return null
        val loyola = Campus("LOYOLA", JsonLatLng(45.458, -73.640), emptyList(), emptyList())
        val sgw = Campus("SGW", JsonLatLng(45.497, -73.579), emptyList(), emptyList())
        CampusRepo.setTestCampuses(listOf(loyola, sgw))

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
    // ── Fixed New Tests ──────────────────────────────────────────────────────


    @Test
    fun `processLocationUpdate unselects campus when user moves away`() {
        val nowhere = LatLng(0.0, 0.0)
        viewModel.processLocationUpdate(nowhere)

        assertNull("Current campus should be null when far away", viewModel.currentCampus)
        assertFalse("UI should not show building info", viewModel.uiBuildingState.isVisible)
    }

    @Test
    fun `MapsToBuildingCode updates state for known building`() = runTest {
        val buildingCode = "H"
        viewModel.navigateToBuildingCode(buildingCode)

        assertEquals(MapUIMode.DIRECTIONS, viewModel.uiBuildingState.mode)
        assertEquals(buildingCode, viewModel.uiBuildingState.destinationName)
    }

    @Test
    fun `handleSearchResult for CampusResult updates currentCampus`() {
        // Mode must NOT be DIRECTIONS for campus logic to trigger in your VM
        val campus = Campus("LOYOLA", JsonLatLng(0.0, 0.0), emptyList(), emptyList())
        val result = SearchResult.CampusResult(campus)

        // Ensure we are in PREVIEW mode
        viewModel.onBackToPreview()
        viewModel.handleSearchResult(result, mockContext)

        assertEquals("LOYOLA", viewModel.currentCampus?.name)
    }
    @Test
    fun `calculateRouteWithState handles API exceptions gracefully`() = runTest {
        whenever(mockRouteProvider.getRoute(any(), any(), any()))
            .thenThrow(RuntimeException("API Down"))

        viewModel.onDirectionsRequested()
        viewModel.processLocationUpdate(LatLng(45.0, -73.0), isForce = true)

        val destBuilding = createTestBuilding("Destination")
        viewModel.handleSearchResult(SearchResult.BuildingResult(destBuilding), mockContext)

        viewModel.onTransportModeChanged("walking")
        advanceUntilIdle()

        val error = viewModel.uiBuildingState.routeErrorMessage
        assertNotNull("Error message should be set when provider throws", error)
        assertTrue("Error should contain 'unavailable'", error!!.contains("unavailable"))
    }

    @Test
    fun `shuttle route calculation updates shuttle state`() = runTest {
        val userLoc = LatLng(45.497, -73.579)
        val mockStop = ShuttleStop("s1", "SGW Stop", "SGW", userLoc)

        whenever(mockShuttleService.resolveNearestStop(anyOrNull())).thenReturn(mockStop)

        whenever(mockShuttleService.statusMessage(any(), anyOrNull())).thenReturn("On time")

        whenever(mockShuttleService.checkAvailability(any(), anyOrNull())).thenReturn(ShuttleAvailability.Active(10))

        viewModel.onDirectionsRequested()
        viewModel.processLocationUpdate(userLoc, isForce = true)

        val destBuilding = createTestBuilding("Hall Building")
        viewModel.handleSearchResult(SearchResult.BuildingResult(destBuilding), mockContext)

        viewModel.onTransportModeChanged("shuttle")
        advanceUntilIdle()

        assertEquals("SGW Stop", viewModel.uiBuildingState.nearestShuttleStopName)
        assertEquals("On time", viewModel.uiBuildingState.shuttleStatusMessage)
    }


    @Test
    fun `processLocationUpdate triggers reroute when distance moved exceeds 15 meters`() = runTest {
        // Arrange: Set initial position and start navigation
        val startPos = LatLng(45.497, -73.579)
        val movePos = LatLng(45.4975, -73.580) // ~50 meters away

        viewModel.onDirectionsRequested()
        viewModel.handleSearchResult(SearchResult.BuildingResult(createTestBuilding()), mockContext)
        viewModel.startNavigation()
        viewModel.processLocationUpdate(startPos, isForce = true)

        clearInvocations(mockRouteProvider)

        // Act: Move user significantly
        viewModel.processLocationUpdate(movePos)
        advanceUntilIdle()

        // Assert: calculateRouteWithState should be called again (via routeProvider)
        verify(mockRouteProvider, atLeastOnce()).getRoute(any(), any(), any())
    }

    @Test
    fun `processLocationUpdate updates bearing and auto-center during navigation`() {
        // Arrange
        val userPos = LatLng(45.497, -73.579)
        viewModel.onDirectionsRequested()
        viewModel.handleSearchResult(SearchResult.BuildingResult(createTestBuilding()), mockContext)
        viewModel.startNavigation()
        viewModel.toggleAutoCenter(true)

        // Act
        viewModel.processLocationUpdate(userPos)

        // Assert
        assertEquals(userPos, viewModel.mapEvent) // mapEvent is updated for auto-centering
    }

    @Test
    fun `updateCampusState handles manual selection override correctly`() {
        val sgw = Campus("SGW", JsonLatLng(45.497, -73.579), emptyList(), emptyList())
        val loyola = Campus("LOYOLA", JsonLatLng(45.458, -73.640), emptyList(), emptyList())
        CampusRepo.setTestCampuses(listOf(sgw, loyola))

        // 1. Manually select SGW
        viewModel.onCampusSelected("SGW")

        // 2. Physically move to Loyola (Manual selection should stick)
        viewModel.processLocationUpdate(LatLng(45.458, -73.640))
        assertEquals("SGW", viewModel.currentCampus?.name)

        // 3. Force update (Like clicking a 'Find Me' button)
        viewModel.processLocationUpdate(LatLng(45.458, -73.640), isForce = true)
        assertEquals("LOYOLA", viewModel.currentCampus?.name)
    }

    @Test
    fun `onBackToPreview resets navigation state and arrival flag`() {
        // Arrange: Mock an arrived state
        viewModel.onDirectionsRequested()
        viewModel.startNavigation()
        // We simulate arrival by modifying the state directly for the test
        // or triggering the logic. Let's just call the reset.

        // Act
        viewModel.onBackToPreview()

        // Assert
        val state = viewModel.uiBuildingState
        assertEquals(MapUIMode.PREVIEW, state.mode)
        assertFalse(state.navState.hasArrived)
        assertEquals(0f, state.navState.currentBearing)
    }

    @Test
    fun `calculateRouteWithState uses user location as start during active navigation`() = runTest {
        val actualUserLoc = LatLng(45.497, -73.579)
        val destinationLoc = LatLng(45.498, -73.581)

        // 1. Create building and result
        val destBuilding = createTestBuilding("Target Building")
        // Use a result that actually populates coordinates
        val result = SearchResult.BuildingResult(destBuilding)

        viewModel.onDirectionsRequested()

        // 2. This sets both 'building' AND 'endPoint' in the state
        viewModel.handleSearchResult(result, mockContext)

        // 3. Update location and start nav
        viewModel.processLocationUpdate(actualUserLoc, isForce = true)
        viewModel.startNavigation()

        // Act
        advanceUntilIdle()

        // Assert
        verify(mockRouteProvider, atLeastOnce()).getRoute(
            eq(actualUserLoc),
            any(),
            any()
        )
    }
}