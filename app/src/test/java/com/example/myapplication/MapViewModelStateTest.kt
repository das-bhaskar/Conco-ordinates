package com.example.myapplication.ui.viewmodel

import com.example.myapplication.data.Building
import com.example.myapplication.data.Campus
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.data.JsonLatLng
import com.example.myapplication.data.NearestStopResult
import com.example.myapplication.data.ShuttleAvailability
import com.example.myapplication.data.ShuttleStop
import com.example.myapplication.logic.HybridSearchProvider
import com.example.myapplication.logic.RouteData
import com.example.myapplication.logic.RouteProvider
import com.example.myapplication.logic.SearchResult
import com.example.myapplication.logic.ShuttleService
import com.example.myapplication.data.indoor.IIndoorRepository
import com.example.myapplication.data.indoor.IndoorFloor
import com.example.myapplication.data.indoor.IndoorNode
import com.example.myapplication.data.indoor.IndoorRoom
import com.example.myapplication.data.indoor.BuildingEntrance
import com.google.android.libraries.places.api.net.PlacesClient
import com.example.myapplication.ui.models.IndoorJourneyPhase
import com.example.myapplication.ui.models.IndoorJourneyState
import com.example.myapplication.ui.models.MapUIMode
import com.google.android.gms.maps.model.LatLng
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelStateTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private val hall = Building(
        name = "Hall Building",
        code = "H",
        wayID = 1L,
        address = "1455 De Maisonneuve",
        outline = listOf(
            JsonLatLng(45.496, -73.580),
            JsonLatLng(45.498, -73.580),
            JsonLatLng(45.498, -73.578),
            JsonLatLng(45.496, -73.578)
        )
    )

    private class FakeRouteProvider : RouteProvider {
        override suspend fun getRoute(start: LatLng, end: LatLng, mode: String): RouteData =
            RouteData(
                points = listOf(start, end),
                duration = "10 min",
                distance = "1 km",
                instructions = listOf("Start walking"),
                durationSeconds = 600L
            )
    }

    private class FakeShuttleService : ShuttleService {
        override fun checkAvailability(fromCampus: String, calendar: java.util.Calendar): ShuttleAvailability =
            ShuttleAvailability.ScheduleUnavailable

        override fun nearestStop(userLocation: LatLng?) = NearestStopResult.NoStopsAvailable

        override fun resolveNearestStop(userLocation: LatLng?): ShuttleStop? = null

        override fun getAllStops(): List<ShuttleStop> = emptyList()

        override fun statusMessage(fromCampus: String, calendar: java.util.Calendar): String = ""
    }

    private class RichShuttleService(
        private val stop: ShuttleStop
    ) : ShuttleService {
        override fun checkAvailability(fromCampus: String, calendar: java.util.Calendar): ShuttleAvailability =
            ShuttleAvailability.Active(4)

        override fun nearestStop(userLocation: LatLng?) = NearestStopResult.Found(stop)

        override fun resolveNearestStop(userLocation: LatLng?): ShuttleStop = stop

        override fun getAllStops(): List<ShuttleStop> = listOf(stop)

        override fun statusMessage(fromCampus: String, calendar: java.util.Calendar): String = "Next shuttle soon"
    }

    private class FakeIndoorRepo(
        private val floors: Map<Pair<String, Int>, IndoorFloor>
    ) : IIndoorRepository {
        override suspend fun getFloor(building: String, floor: Int): IndoorFloor? =
            floors[building.uppercase() to floor]

        override fun clearCache() = Unit
    }

    private fun indoorRoom(id: String, label: String) = IndoorRoom(
        id = id,
        type = "classroom",
        label = label,
        polygon = listOf(Offset(0.5f, 0.5f))
    )

    private fun indoorNode(id: String, roomId: String? = null) =
        IndoorNode(id = id, x = 0.5f, y = 0.5f, type = "ROOM", roomId = roomId)

    private lateinit var viewModel: MapViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        CampusRepo.setTestCampuses(
            listOf(
                Campus("SGW", JsonLatLng(45.497, -73.579), listOf(hall), outline = null)
            )
        )
        viewModel = MapViewModel(
            routeProvider = FakeRouteProvider(),
            shuttleService = FakeShuttleService()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initSearch stores provider and seeds current location result`() {
        val searchProvider = HybridSearchProvider(
            placesClient = mock<PlacesClient>(),
            indoorRepo = mock<IIndoorRepository>()
        )
        viewModel.initSearch(searchProvider)
        assertEquals(1, viewModel.searchResults.size)
        assertTrue(viewModel.searchResults.first() is SearchResult.CurrentLocation)
    }

    @Test
    fun `onSearchQueryChanged updates main query and blank search results`() = runTest {
        val searchProvider = HybridSearchProvider(
            placesClient = mock<PlacesClient>(),
            indoorRepo = mock<IIndoorRepository>()
        )
        viewModel.initSearch(searchProvider)

        viewModel.onSearchQueryChanged("")
        advanceUntilIdle()

        assertEquals("", viewModel.searchQuery)
        assertEquals(2, viewModel.searchResults.size)
        assertTrue(viewModel.searchResults.any { it is SearchResult.CurrentLocation })
        assertTrue(viewModel.searchResults.any { it is SearchResult.Home })
    }

    @Test
    fun `clearJourney resets indoor journey to idle`() {
        viewModel.setJourneyPhase(IndoorJourneyPhase.Arrived)
        viewModel.clearJourney()
        assertEquals(IndoorJourneyState(), viewModel.indoorJourneyState)
    }

    @Test
    fun `setJourneyPhase outdoor starts outdoor directions state`() = runTest {
        val destination = SearchResult.IndoorRoomResult("H", 8, "H-8-829", "node", "H-829")
        viewModel.setJourneyPhase(
            IndoorJourneyPhase.Outdoor(
                origin = LatLng(45.497, -73.579),
                destination = LatLng(45.498, -73.580),
                destRoom = destination
            )
        )
        advanceUntilIdle()

        assertEquals(MapUIMode.DIRECTIONS, viewModel.uiBuildingState.mode)
        assertEquals("H-829", viewModel.uiBuildingState.destinationName)
        assertEquals("10 min", viewModel.uiBuildingState.routeDuration)
    }

    @Test
    fun `handleMapTap sets visible building data and indoor map flag in preview mode`() {
        viewModel.handleMapTap(hall)

        assertTrue(viewModel.uiBuildingState.isVisible)
        assertEquals(hall, viewModel.uiBuildingState.building)
        assertEquals("1455 De Maisonneuve", viewModel.uiBuildingState.address)
        assertTrue(viewModel.uiBuildingState.hasIndoorMap)
    }

    @Test
    fun `handleMapTap is ignored while already in directions mode`() {
        viewModel.onDirectionsRequested()

        viewModel.handleMapTap(hall)

        assertNull(viewModel.uiBuildingState.building)
        assertEquals(MapUIMode.DIRECTIONS, viewModel.uiBuildingState.mode)
    }

    @Test
    fun `onCurrentRoomSelected is ignored when phase is not AskCurrentRoom`() {
        viewModel.clearJourney()
        viewModel.onCurrentRoomSelected("node", "H-110")
        assertEquals(IndoorJourneyPhase.Idle, viewModel.indoorJourneyState.phase)
    }

    @Test
    fun `searchCurrentRoom resolves room and transitions to indoor destination`() = runTest {
        val destination = SearchResult.IndoorRoomResult("H", 8, "H-8-829", "dest-node", "H-829")
        val repo = FakeIndoorRepo(
            mapOf(
                ("H" to 8) to IndoorFloor(
                    building = "H",
                    floor = 8,
                    rooms = listOf(indoorRoom("H-8-829", "H-829")),
                    nodes = listOf(indoorNode("node-829", roomId = "H-8-829"))
                )
            )
        )
        viewModel.initSearch(HybridSearchProvider(mock<PlacesClient>(), repo))
        viewModel.setJourneyPhase(IndoorJourneyPhase.AskCurrentRoom(hall, destination))

        viewModel.searchCurrentRoom("829", "H")
        advanceUntilIdle()

        assertFalse(viewModel.indoorRoomSearching)
        assertNull(viewModel.indoorRoomSearchError)
        val phase = viewModel.indoorJourneyState.phase as IndoorJourneyPhase.IndoorToDestination
        assertEquals("H", phase.buildingCode)
        assertEquals("node-829", phase.startNodeId)
        assertEquals(8, phase.startFloor)
    }

    @Test
    fun `searchCurrentRoom stores error when room cannot be found`() = runTest {
        val destination = SearchResult.IndoorRoomResult("H", 8, "missing", null, "H-999")
        viewModel.initSearch(HybridSearchProvider(mock<PlacesClient>(), FakeIndoorRepo(emptyMap())))
        viewModel.setJourneyPhase(IndoorJourneyPhase.AskCurrentRoom(hall, destination))

        viewModel.searchCurrentRoom("999", "H")
        advanceUntilIdle()

        assertFalse(viewModel.indoorRoomSearching)
        assertEquals("Room \"999\" not found in H", viewModel.indoorRoomSearchError)
        assertTrue(viewModel.indoorJourneyState.phase is IndoorJourneyPhase.AskCurrentRoom)
    }

    @Test
    fun `onEntranceSelected is ignored when phase is not AskEntryPoint`() {
        viewModel.onEntranceSelected(BuildingEntrance("n1", "Entry", LatLng(0.0, 0.0), 1))
        assertEquals(IndoorJourneyPhase.Idle, viewModel.indoorJourneyState.phase)
    }

    @Test
    fun `onEntranceSelected advances ask entry point to indoor destination`() {
        val destination = SearchResult.IndoorRoomResult("H", 8, "H-8-829", "dest-node", "H-829")
        val entrance = BuildingEntrance("entry-1", "Main Entrance", LatLng(45.497, -73.579), 1)
        viewModel.setJourneyPhase(
            IndoorJourneyPhase.AskEntryPoint(
                building = hall,
                entrances = listOf(entrance),
                destination = destination
            )
        )

        viewModel.onEntranceSelected(entrance)

        val phase = viewModel.indoorJourneyState.phase as IndoorJourneyPhase.IndoorToDestination
        assertEquals("H", phase.buildingCode)
        assertEquals(1, phase.startFloor)
        assertEquals("entry-1", phase.startNodeId)
    }

    @Test
    fun `onUserExited is ignored when no indoor exit phase is active`() {
        viewModel.onUserExited()
        assertEquals(IndoorJourneyPhase.Idle, viewModel.indoorJourneyState.phase)
    }

    @Test
    fun `onUserExited falls back to idle when destination entrance data is unavailable`() {
        val destination = SearchResult.IndoorRoomResult("ZZZ", 1, "ZZZ-101", null, "ZZZ-101")
        viewModel.processLocationUpdate(LatLng(45.497, -73.579), isForce = true)
        viewModel.setJourneyPhase(
            IndoorJourneyPhase.IndoorToExit(
                buildingCode = "H",
                startFloor = 1,
                exitFloor = 1,
                startNodeId = "room-node",
                exitNodeId = "exit-node",
                destination = destination
            )
        )

        viewModel.onUserExited()

        assertEquals(IndoorJourneyPhase.Idle, viewModel.indoorJourneyState.phase)
    }

    @Test
    fun `navigateToPOI sets directions state and destination point`() = runTest {
        val poi = LatLng(45.501, -73.582)
        viewModel.processLocationUpdate(LatLng(45.497, -73.579), isForce = true)

        viewModel.navigateToPOI("Coffee Shop", poi)
        advanceUntilIdle()

        assertEquals(MapUIMode.DIRECTIONS, viewModel.uiBuildingState.mode)
        assertEquals("Coffee Shop", viewModel.uiBuildingState.destinationName)
        assertEquals(poi, viewModel.uiBuildingState.endPoint)
    }

    @Test
    fun `toggleAutoCenter updates nav state`() {
        viewModel.toggleAutoCenter(false)
        assertFalse(viewModel.uiBuildingState.navState.isAutoCenterEnabled)
    }

    @Test
    fun `forceRecenter re-enables auto center and emits last location`() {
        val current = LatLng(45.497, -73.579)
        viewModel.processLocationUpdate(current, isForce = true)
        viewModel.toggleAutoCenter(false)

        viewModel.forceRecenter()

        assertTrue(viewModel.uiBuildingState.navState.isAutoCenterEnabled)
        assertEquals(current, viewModel.mapEvent)
    }

    @Test
    fun `setMapEventWithOffset stores latitude offset target`() {
        val target = LatLng(45.500, -73.600)
        viewModel.setMapEventWithOffset(target)

        assertNotNull(viewModel.mapEvent)
        assertEquals(45.495, viewModel.mapEvent!!.latitude, 0.000001)
        assertEquals(-73.600, viewModel.mapEvent!!.longitude, 0.000001)
    }

    @Test
    fun `startNavigation does nothing when there is no destination`() {
        viewModel.startNavigation()
        assertEquals(MapUIMode.PREVIEW, viewModel.uiBuildingState.mode)
    }

    @Test
    fun `startNavigation enters active navigation when destination exists`() = runTest {
        viewModel.processLocationUpdate(LatLng(45.497, -73.579), isForce = true)
        viewModel.navigateToPOI("Coffee Shop", LatLng(45.501, -73.582))
        advanceUntilIdle()

        viewModel.startNavigation()
        advanceUntilIdle()

        assertEquals(MapUIMode.ACTIVE_NAVIGATION, viewModel.uiBuildingState.mode)
        assertFalse(viewModel.uiBuildingState.navState.hasArrived)
        assertTrue(viewModel.uiBuildingState.navState.isAutoCenterEnabled)
        assertNotNull(viewModel.uiBuildingState.navState.currentInstruction)
    }

    @Test
    fun `onCampusSelected updates current campus for known campus`() {
        viewModel.onCampusSelected("SGW")

        assertEquals("SGW", viewModel.currentCampus?.name)
        assertNull(viewModel.highlightedBuildingName)
    }

    @Test
    fun `onCampusSelected ignores unknown campus`() {
        viewModel.onCampusSelected("UNKNOWN")

        assertNull(viewModel.currentCampus)
    }

    @Test
    fun `processLocationUpdate detects campus and highlights building inside polygon`() {
        viewModel.processLocationUpdate(LatLng(45.497, -73.579), isForce = true)

        assertEquals("SGW", viewModel.currentCampus?.name)
        assertEquals("Hall Building", viewModel.highlightedBuildingName)
    }

    @Test
    fun `processLocationUpdate clears current campus when user is far away`() {
        viewModel.onCampusSelected("SGW")

        viewModel.processLocationUpdate(LatLng(46.0, -74.0), isForce = true)

        assertNull(viewModel.currentCampus)
        assertNull(viewModel.highlightedBuildingName)
    }

    @Test
    fun `processLocationUpdate near destination building advances outdoor journey`() {
        val destination = SearchResult.IndoorRoomResult("H", 8, "H-8-829", "dest-node", "H-829")
        viewModel.setJourneyPhase(
            IndoorJourneyPhase.Outdoor(
                origin = LatLng(45.490, -73.570),
                destination = hall.getCenter(),
                destRoom = destination
            )
        )

        viewModel.processLocationUpdate(hall.getCenter(), isForce = true)

        assertTrue(viewModel.indoorJourneyState.phase is IndoorJourneyPhase.AskEntryPoint)
    }

    @Test
    fun `handleSearchResult home in preview mode sets start point and map event`() {
        viewModel.handleSearchResult(SearchResult.Home, ContextWrapper(null))

        assertEquals(LatLng(45.51723868665001, -73.627297124046), viewModel.uiBuildingState.startPoint)
        assertEquals(LatLng(45.51723868665001, -73.627297124046), viewModel.mapEvent)
        assertTrue(viewModel.searchResults.isEmpty())
    }

    @Test
    fun `navigateToBuildingCode for unknown code opens directions and leaves building null`() {
        viewModel.navigateToBuildingCode("UNKNOWN")

        assertEquals(MapUIMode.DIRECTIONS, viewModel.uiBuildingState.mode)
        assertEquals("UNKNOWN", viewModel.uiBuildingState.destinationName)
        assertNull(viewModel.uiBuildingState.building)
        assertNull(viewModel.uiBuildingState.endPoint)
    }

    @Test
    fun `onBackToPreview resets mode and navigation state`() = runTest {
        viewModel.processLocationUpdate(LatLng(45.497, -73.579), isForce = true)
        viewModel.navigateToPOI("Coffee Shop", LatLng(45.501, -73.582))
        advanceUntilIdle()
        viewModel.startNavigation()
        advanceUntilIdle()

        viewModel.onBackToPreview()

        assertEquals(MapUIMode.PREVIEW, viewModel.uiBuildingState.mode)
        assertFalse(viewModel.uiBuildingState.navState.hasArrived)
        assertFalse(viewModel.uiBuildingState.navState.isAutoCenterEnabled)
        assertEquals(0f, viewModel.uiBuildingState.navState.currentBearing, 0.0f)
    }

    @Test
    fun `onTransportModeChanged updates selected mode`() {
        viewModel.onTransportModeChanged("drive")

        assertEquals("drive", viewModel.uiBuildingState.selectedTransportMode)
    }

    @Test
    fun `onSearchQueryChanged updates start and destination fields`() {
        viewModel.onSearchQueryChanged("Current Hall", field = "start")
        viewModel.onSearchQueryChanged("MB", field = "dest")

        assertEquals("Current Hall", viewModel.uiBuildingState.startLocationName)
        assertEquals("MB", viewModel.uiBuildingState.destinationName)
    }

    @Test
    fun `toggleSearchExpansion updates expansion state and active field`() {
        viewModel.toggleSearchExpansion(true, field = "dest")

        assertTrue(viewModel.uiBuildingState.isSearchExpanded)
        assertEquals("dest", viewModel.activeSearchField)
    }

    @Test
    fun `onStartQueryChanged stores start location name`() {
        viewModel.onStartQueryChanged("Hall Building")

        assertEquals("Hall Building", viewModel.uiBuildingState.startLocationName)
    }

    @Test
    fun `handleSearchResult in directions mode updates destination and collapses search`() {
        viewModel.onDirectionsRequested()
        viewModel.toggleSearchExpansion(true, field = "dest")

        viewModel.handleSearchResult(SearchResult.Home, ContextWrapper(null))

        assertEquals("Home", viewModel.uiBuildingState.destinationName)
        assertEquals(LatLng(45.51723868665001, -73.627297124046), viewModel.uiBuildingState.endPoint)
        assertFalse(viewModel.uiBuildingState.isSearchExpanded)
        assertTrue(viewModel.searchResults.isEmpty())
    }

    @Test
    fun `handleSearchResult in directions mode updates start field when active field is start`() {
        viewModel.onDirectionsRequested()
        viewModel.toggleSearchExpansion(true, field = "start")

        viewModel.handleSearchResult(SearchResult.Home, ContextWrapper(null))

        assertEquals("Home", viewModel.uiBuildingState.startLocationName)
        assertEquals(LatLng(45.51723868665001, -73.627297124046), viewModel.uiBuildingState.startPoint)
        assertFalse(viewModel.uiBuildingState.isSearchExpanded)
    }

    @Test
    fun `swapLocations swaps names and coordinates`() {
        val start = LatLng(45.497, -73.579)
        val end = LatLng(45.501, -73.582)
        viewModel.processLocationUpdate(start, isForce = true)
        viewModel.onDirectionsRequested()
        viewModel.toggleSearchExpansion(true, field = "start")
        viewModel.handleSearchResult(SearchResult.Home, ContextWrapper(null))
        viewModel.navigateToPOI("Destination", end)

        viewModel.swapLocations()

        assertEquals("Destination", viewModel.uiBuildingState.startLocationName)
        assertEquals("Home", viewModel.uiBuildingState.destinationName)
        assertEquals(end, viewModel.uiBuildingState.startPoint)
        assertEquals(LatLng(45.51723868665001, -73.627297124046), viewModel.uiBuildingState.endPoint)
    }

    @Test
    fun `onDirectionsRequested enters directions mode and preserves selected building name`() {
        viewModel.handleMapTap(hall)

        viewModel.onDirectionsRequested()

        assertEquals(MapUIMode.DIRECTIONS, viewModel.uiBuildingState.mode)
        assertEquals("Hall Building", viewModel.uiBuildingState.destinationName)
    }

    @Test
    fun `handleSearchResult campus in preview updates current campus and hides popup`() {
        val campus = Campus("SGW", JsonLatLng(45.497, -73.579), listOf(hall), outline = emptyList())
        viewModel.handleMapTap(hall)

        viewModel.handleSearchResult(SearchResult.CampusResult(campus), ContextWrapper(null))

        assertEquals("SGW", viewModel.currentCampus?.name)
        assertFalse(viewModel.uiBuildingState.isVisible)
        assertNull(viewModel.uiBuildingState.building)
        assertNotNull(viewModel.mapEvent)
    }

    @Test
    fun `startOutdoorLeg sets walk directions and clears previous route errors`() = runTest {
        viewModel.processLocationUpdate(LatLng(45.497, -73.579), isForce = true)
        viewModel.navigateToBuildingCode("UNKNOWN")
        viewModel.startOutdoorLeg(
            origin = LatLng(45.497, -73.579),
            destination = LatLng(45.500, -73.582),
            destLabel = "H-829"
        )
        advanceUntilIdle()

        assertEquals(MapUIMode.DIRECTIONS, viewModel.uiBuildingState.mode)
        assertEquals("walk", viewModel.uiBuildingState.selectedTransportMode)
        assertEquals("H-829", viewModel.uiBuildingState.destinationName)
        assertEquals("10 min", viewModel.uiBuildingState.routeDuration)
        assertNull(viewModel.uiBuildingState.routeErrorMessage)
    }

    @Test
    fun `navigateToPOI with no route provider stores unavailable route message`() = runTest {
        val noRouteViewModel = MapViewModel(shuttleService = FakeShuttleService())
        noRouteViewModel.processLocationUpdate(LatLng(45.497, -73.579), isForce = true)

        noRouteViewModel.navigateToPOI("Coffee Shop", LatLng(45.501, -73.582))
        advanceUntilIdle()

        assertEquals("-- min", noRouteViewModel.uiBuildingState.routeDuration)
        assertEquals("Walk route unavailable between these points.", noRouteViewModel.uiBuildingState.routeErrorMessage)
    }

    @Test
    fun `calculateRouteWithState in shuttle mode stores shuttle snapshot`() = runTest {
        val stop = ShuttleStop("SGW-1", "SGW Stop", "SGW", LatLng(45.497, -73.579))
        val shuttleViewModel = MapViewModel(
            routeProvider = FakeRouteProvider(),
            shuttleService = RichShuttleService(stop)
        )
        shuttleViewModel.processLocationUpdate(LatLng(45.497, -73.579), isForce = true)
        shuttleViewModel.navigateToPOI("Coffee Shop", LatLng(45.501, -73.582))
        advanceUntilIdle()

        shuttleViewModel.onTransportModeChanged("shuttle")
        advanceUntilIdle()

        assertTrue(shuttleViewModel.uiBuildingState.shuttleAvailability is ShuttleAvailability.Active)
        assertEquals("Next shuttle soon", shuttleViewModel.uiBuildingState.shuttleStatusMessage)
        assertEquals("SGW Stop", shuttleViewModel.uiBuildingState.nearestShuttleStopName)
        assertEquals("SGW", shuttleViewModel.uiBuildingState.nearestShuttleStopCampus)
        assertEquals(listOf(stop), shuttleViewModel.uiBuildingState.shuttleStops)
    }
}
