package com.example.myapplication.logic

import com.example.myapplication.data.Building
import com.example.myapplication.data.Campus
import com.example.myapplication.data.JsonLatLng
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Test

class MapManagerTest {

    private val testBuilding = Building(
        name = "Hall Building",
        code = "H",
        wayID = 123L,
        address = "1455 De Maisonneuve",
        outline = listOf(
            JsonLatLng(45.497, -73.579),
            JsonLatLng(45.498, -73.579),
            JsonLatLng(45.498, -73.578),
            JsonLatLng(45.497, -73.578)
        )
    )

    private val testCampus = Campus(
        name = "SGW",
        center = JsonLatLng(45.497, -73.579),
        buildings = listOf(testBuilding),
        outline = emptyList()
    )

    @Test
    fun `findBuildingAtLocation returns building name when inside`() {
        val userLocation = LatLng(45.4975, -73.5785)
        val result = MapManager.findBuildingAtLocation(userLocation, testCampus)
        assertEquals("Hall Building", result)
    }

    @Test
    fun `findBuildingAtLocation returns null if point is very far away`() {
        // Point far outside the 10.0 threshold
        val farLocation = LatLng(40.0, -70.0)
        val result = MapManager.findBuildingAtLocation(farLocation, testCampus)
        assertEquals(null, result)
    }

    @Test
    fun `findBuildingAtLocation returns closest building when just outside`() {
        val closeOutside = LatLng(45.4970001, -73.5780001)
        val result = MapManager.findBuildingAtLocation(closeOutside, testCampus)
        assertEquals("Hall Building", result)
    }

    @Test
    fun `distanceFromPoly hits all segments of the polygon`() {
        val point = LatLng(45.496, -73.579)
        val poly = testBuilding.getGoogleOutline()
        val distance = MapManager.distanceFromPoly(point, poly)
        assert(distance > 0)
    }
}