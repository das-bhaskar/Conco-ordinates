package com.example.myapplication.logic

import android.graphics.Color
import com.example.myapplication.data.Building
import com.example.myapplication.data.Campus
import com.example.myapplication.data.JsonLatLng
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class MapManagerTest {

    private lateinit var mockMap: GoogleMap
    private lateinit var mapManager: MapManager

    // Sample data for testing
    private val testBuilding = Building(
        name = "Hall Building",
        code = "H",
        wayID = 123L,
        address = "1455 De Maisonneuve",
        outline = listOf(
            JsonLatLng(45.497, -73.579),
            JsonLatLng(45.498, -73.579),
            JsonLatLng(45.498, -73.578)
        )
    )

    private val testCampus = Campus(
        name = "SGW",
        center = JsonLatLng(45.497, -73.579),
        buildings = listOf(testBuilding),
        outline = emptyList()
    )

    @Before
    fun setup() {
        mockMap = mock(GoogleMap::class.java)
        mapManager = MapManager(mockMap)
        com.example.myapplication.telemetry.CrashReporter.isTesting = true
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 1. COMPANION OBJECT LOGIC (Ray-casting & Distance)
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    fun `findBuildingAtLocation returns building name when inside`() {
        // Hall Building outline covers roughly 45.497, -73.579
        val insideLocation = LatLng(45.4975, -73.5785)
        val result = MapManager.findBuildingAtLocation(insideLocation, testCampus)
        assertEquals("Hall Building", result)
    }

    @Test
    fun `findBuildingAtLocation returns closest building if very close to edge`() {
        // 45.49695 is only ~5.5 meters away from 45.49700
        // This is within the 10.0 threshold
        val nearbyLocation = LatLng(45.49695, -73.579)
        val result = MapManager.findBuildingAtLocation(nearbyLocation, testCampus)
        assertEquals("Hall Building", result)
    }

    @Test
    fun `distanceFromPoly returns a valid distance`() {
        val point = LatLng(45.0, -73.0)
        val poly = listOf(LatLng(45.1, -73.1), LatLng(45.1, -72.9))
        val dist = MapManager.distanceFromPoly(point, poly)
        // Ensure it calculated a positive distance
        assert(dist > 0)
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 2. STATE & DRAWING LOGIC (The "if" and "forEach" branches)
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    fun `updateHighlightsOnly skips redrawing if building name is unchanged`() {
        // First draw
        mapManager.updateHighlightsOnly(testCampus, "Hall Building")
        verify(mockMap, times(1)).clear()

        // Second draw with SAME name should return early because of: if (highlightedBuildingName == lastBuilding) return
        mapManager.updateHighlightsOnly(testCampus, "Hall Building")
        verify(mockMap, times(1)).clear()
    }

    @Test
    fun `updateHighlightsOnly redraws when building selection changes`() {
        mapManager.updateHighlightsOnly(testCampus, "Hall Building")
        mapManager.updateHighlightsOnly(testCampus, "Other Building")

        // Should have cleared twice (once for each unique name)
        verify(mockMap, times(2)).clear()
    }

    @Test
    fun `drawBuildings adds polygons with different styles based on selection`() {
        // This exercises the 'isCurrentBuilding' ternary logic for colors/stroke
        mapManager.updateHighlightsOnly(testCampus, "Hall Building")

        // Verify polygon was actually added to the map
        verify(mockMap, atLeastOnce()).addPolygon(any())
    }

    @Test
    fun `drawBuildings handles campus with no buildings gracefully`() {
        val emptyCampus = Campus(
            name = "Empty",
            center = JsonLatLng(0.0, 0.0),
            buildings = emptyList(),
            outline = emptyList()
        )

        mapManager.updateHighlightsOnly(emptyCampus, "None")

        // Should clear the map but not add any polygons
        verify(mockMap, atLeastOnce()).clear()
        verify(mockMap, never()).addPolygon(any())
    }
}