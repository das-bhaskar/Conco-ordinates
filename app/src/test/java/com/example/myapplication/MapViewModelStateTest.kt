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
import com.google.android.libraries.places.api.net.PlacesClient
import com.example.myapplication.ui.models.IndoorJourneyPhase
import com.example.myapplication.ui.models.IndoorJourneyState
import com.example.myapplication.ui.models.MapUIMode
import com.google.android.gms.maps.model.LatLng
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
    fun `onCurrentRoomSelected is ignored when phase is not AskCurrentRoom`() {
        viewModel.clearJourney()
        viewModel.onCurrentRoomSelected("node", "H-110")
        assertEquals(IndoorJourneyPhase.Idle, viewModel.indoorJourneyState.phase)
    }

    @Test
    fun `onEntranceSelected is ignored when phase is not AskEntryPoint`() {
        viewModel.onEntranceSelected(com.example.myapplication.data.indoor.BuildingEntrance("n1", "Entry", LatLng(0.0, 0.0), 1))
        assertEquals(IndoorJourneyPhase.Idle, viewModel.indoorJourneyState.phase)
    }

    @Test
    fun `onUserExited is ignored when no indoor exit phase is active`() {
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
    fun `navigateToBuildingCode for unknown code opens directions and leaves building null`() {
        viewModel.navigateToBuildingCode("UNKNOWN")

        assertEquals(MapUIMode.DIRECTIONS, viewModel.uiBuildingState.mode)
        assertEquals("UNKNOWN", viewModel.uiBuildingState.destinationName)
        assertNull(viewModel.uiBuildingState.building)
        assertNull(viewModel.uiBuildingState.endPoint)
    }
}
