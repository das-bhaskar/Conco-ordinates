package com.example.myapplication.logic

import android.content.ContextWrapper
import com.example.myapplication.data.Building
import com.example.myapplication.data.Campus
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.data.JsonLatLng
import com.example.myapplication.data.NearestStopResult
import com.example.myapplication.data.ShuttleAvailability
import com.example.myapplication.data.ShuttleStop
import com.example.myapplication.ui.viewmodel.MapViewModel
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MapInteractionHandlerAdditionalTest {

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
            RouteData(listOf(start, end), "10 min", "1 km", durationSeconds = 600L)
    }

    private class FakeShuttleService : ShuttleService {
        override fun checkAvailability(
            fromCampus: String,
            calendar: java.util.Calendar
        ): ShuttleAvailability =
            ShuttleAvailability.ScheduleUnavailable

        override fun nearestStop(userLocation: LatLng?) = NearestStopResult.NoStopsAvailable
        override fun resolveNearestStop(userLocation: LatLng?): ShuttleStop? = null
        override fun getAllStops(): List<ShuttleStop> = emptyList()
        override fun statusMessage(fromCampus: String, calendar: java.util.Calendar): String = ""
    }

    private lateinit var viewModel: MapViewModel

    @Before
    fun setup() {
        CampusRepo.setTestCampuses(
            listOf(Campus("SGW", JsonLatLng(45.497, -73.579), listOf(hall), outline = emptyList()))
        )
        viewModel = MapViewModel(
            routeProvider = FakeRouteProvider(),
            shuttleService = FakeShuttleService()
        )
    }

    @Test
    fun `processClick returns early when current campus is null`() {
        MapInteractionHandler.processClick(
            LatLng(45.497, -73.579),
            viewModel,
            ContextWrapper(null)
        )

        assertFalse(viewModel.uiBuildingState.isVisible)
        assertNull(viewModel.uiBuildingState.building)
    }

    @Test
    fun `processClick clears building popup when no building is found`() {
        viewModel.onCampusSelected("SGW")
        viewModel.handleMapTap(hall)

        MapInteractionHandler.processClick(
            LatLng(45.600, -73.700),
            viewModel,
            ContextWrapper(null)
        )

        assertFalse(viewModel.uiBuildingState.isVisible)
        assertNull(viewModel.uiBuildingState.building)
    }

    @Test
    fun `handleSearchSelection shows selected building`() {
        MapInteractionHandler.handleSearchSelection(
            hall,
            viewModel,
            ContextWrapper(null)
        )

        assertTrue(viewModel.uiBuildingState.isVisible)
        assertEquals("Hall Building", viewModel.uiBuildingState.building?.name)
        assertEquals("1455 De Maisonneuve", viewModel.uiBuildingState.address)
    }

}
