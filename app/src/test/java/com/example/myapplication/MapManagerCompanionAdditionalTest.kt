package com.example.myapplication.logic

import com.example.myapplication.data.Building
import com.example.myapplication.data.Campus
import com.example.myapplication.data.JsonLatLng
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapManagerCompanionAdditionalTest {

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

    @Test
    fun `findBuildingAtLocation returns null when campus has no buildings`() {
        val campus = Campus("Empty", JsonLatLng(45.497, -73.579), emptyList(), outline = emptyList())

        val result = MapManager.findBuildingAtLocation(LatLng(45.497, -73.579), campus)

        assertNull(result)
    }

    @Test
    fun `findBuildingAtLocation returns null when user is not near any building`() {
        val campus = Campus("SGW", JsonLatLng(45.497, -73.579), listOf(hall), outline = emptyList())

        val result = MapManager.findBuildingAtLocation(LatLng(45.600, -73.700), campus)

        assertNull(result)
    }

    @Test
    fun `distanceFromPoly returns zero when point lies on polygon vertex`() {
        val poly = hall.getGoogleOutline()
        val point = poly.first()

        val distance = MapManager.distanceFromPoly(point, poly)

        assertEquals(0.0, distance, 0.0001)
    }

    @Test
    fun `distanceFromPoly returns positive distance for far point`() {
        val distance = MapManager.distanceFromPoly(LatLng(45.600, -73.700), hall.getGoogleOutline())

        assertTrue(distance > 0.0)
    }
}
