package com.example.myapplication.logic

import com.example.myapplication.data.Building
import com.example.myapplication.data.Campus
import com.example.myapplication.data.JsonLatLng
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchResultBehaviorTest {

    private val hall = Building(
        name = "Henry F. Hall Building",
        code = "H",
        wayID = 1L,
        address = "1455 De Maisonneuve Blvd W",
        outline = listOf(
            JsonLatLng(45.496, -73.580),
            JsonLatLng(45.498, -73.580),
            JsonLatLng(45.498, -73.578),
            JsonLatLng(45.496, -73.578)
        )
    )

    private val campus = Campus(
        name = "SGW Campus",
        center = JsonLatLng(45.497, -73.579),
        buildings = listOf(hall),
        outline = null
    )

    @Test
    fun `displayName returns building name for BuildingResult`() {
        assertEquals("Henry F. Hall Building", SearchResult.BuildingResult(hall).displayName)
    }

    @Test
    fun `displayName returns campus name for CampusResult`() {
        assertEquals("SGW Campus", SearchResult.CampusResult(campus).displayName)
    }

    @Test
    fun `displayName returns title for GoogleResult`() {
        assertEquals("Hall", SearchResult.GoogleResult("Hall", "Address", "place-id").displayName)
    }

    @Test
    fun `displayName returns label for IndoorRoomResult`() {
        val result = SearchResult.IndoorRoomResult("H", 8, "H-8-829", "node-829", "H-829 · H Floor 8")
        assertEquals("H-829 · H Floor 8", result.displayName)
    }

    @Test
    fun `displayName returns fixed labels for CurrentLocation and Home`() {
        assertEquals("Your position", SearchResult.CurrentLocation.displayName)
        assertEquals("Home", SearchResult.Home.displayName)
    }

    @Test
    fun `coordinates returns building center for BuildingResult`() {
        val expected = hall.getCenter()
        val actual = SearchResult.BuildingResult(hall).coordinates(null)
        assertEquals(expected, actual)
    }

    @Test
    fun `coordinates returns first building center for CampusResult`() {
        val actual = SearchResult.CampusResult(campus).coordinates(null)
        assertEquals(hall.getCenter(), actual)
    }

    @Test
    fun `coordinates returns null for CampusResult with no buildings`() {
        val emptyCampus = campus.copy(buildings = emptyList())
        val actual = SearchResult.CampusResult(emptyCampus).coordinates(null)
        assertNull(actual)
    }

    @Test
    fun `coordinates returns current location for CurrentLocation`() {
        val current = LatLng(45.5, -73.6)
        assertEquals(current, SearchResult.CurrentLocation.coordinates(current))
    }

    @Test
    fun `coordinates returns null for GoogleResult and IndoorRoomResult`() {
        assertNull(SearchResult.GoogleResult("Hall", "Address", "place-id").coordinates(null))
        assertNull(SearchResult.IndoorRoomResult("H", 8, "H-8-829", "node-829", "H-829").coordinates(null))
    }

    @Test
    fun `coordinates returns fixed home coordinates for Home`() {
        val home = SearchResult.Home.coordinates(null)
        assertEquals(LatLng(45.51723868665001, -73.627297124046), home)
    }
}
