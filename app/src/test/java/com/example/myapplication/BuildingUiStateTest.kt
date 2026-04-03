package com.example.myapplication.ui.models

import com.example.myapplication.data.Building
import com.example.myapplication.data.JsonLatLng
import com.example.myapplication.data.ShuttleAvailability
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildingUiStateTest {

    private val building = Building(
        name = "Henry F. Hall Building",
        code = "H",
        wayID = 1L,
        address = "1455 De Maisonneuve Blvd W",
        outline = listOf(
            JsonLatLng(45.496, -73.580),
            JsonLatLng(45.498, -73.580),
            JsonLatLng(45.498, -73.578)
        )
    )

    @Test
    fun `MapUIMode contains the expected modes`() {
        assertEquals(listOf(MapUIMode.PREVIEW, MapUIMode.DIRECTIONS, MapUIMode.ACTIVE_NAVIGATION), MapUIMode.entries)
    }

    @Test
    fun `BuildingUiState defaults match preview map state`() {
        val state = BuildingUiState()
        assertFalse(state.isVisible)
        assertEquals(MapUIMode.PREVIEW, state.mode)
        assertNull(state.building)
        assertEquals("Your position", state.startLocationName)
        assertEquals("", state.destinationName)
        assertEquals("walk", state.selectedTransportMode)
        assertEquals("-- min", state.routeDuration)
        assertEquals("-- m", state.routeDistance)
        assertFalse(state.hasIndoorMap)
        assertEquals(ShuttleAvailability.ScheduleUnavailable, state.shuttleAvailability)
        assertTrue(state.routePoints.isEmpty())
        assertTrue(state.routeSegments.isEmpty())
        assertTrue(state.shuttleStops.isEmpty())
    }

    @Test
    fun `BuildingUiState copy updates route and keeps unrelated values`() {
        val start = BuildingUiState(building = building, isVisible = true)
        val updated = start.copy(
            mode = MapUIMode.DIRECTIONS,
            destinationName = "CC-101",
            startPoint = LatLng(45.497, -73.579),
            endPoint = LatLng(45.458, -73.640),
            routeDurationSeconds = 600L,
            routeDuration = "10 min",
            routeDistance = "900 m",
            hasIndoorMap = true
        )

        assertTrue(updated.isVisible)
        assertEquals(building, updated.building)
        assertEquals(MapUIMode.DIRECTIONS, updated.mode)
        assertEquals("CC-101", updated.destinationName)
        assertEquals(600L, updated.routeDurationSeconds)
        assertEquals("10 min", updated.routeDuration)
        assertEquals("900 m", updated.routeDistance)
        assertTrue(updated.hasIndoorMap)
    }

    @Test
    fun `BuildingUiState navState defaults are preserved`() {
        val state = BuildingUiState()
        assertEquals(NavigationState(), state.navState)
    }
}
