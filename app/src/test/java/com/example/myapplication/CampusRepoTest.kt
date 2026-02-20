package com.example.myapplication.data

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CampusRepoTest {

    @Test
    fun `test rayCastIntersect and isInsidePolygon logic`() {
        // 1. Create a simple square campus
        val squareOutline = listOf(
            JsonLatLng(10.0, 10.0),
            JsonLatLng(20.0, 10.0),
            JsonLatLng(20.0, 20.0),
            JsonLatLng(10.0, 20.0)
        )
        val testCampus = Campus("Test", JsonLatLng(15.0, 15.0), emptyList(), squareOutline)

        // 2. Inject it
        CampusRepo.setTestCampuses(listOf(testCampus))

        // 3. Test a point INSIDE (This hits the rayCastIntersect lines)
        val insidePoint = LatLng(15.0, 15.0)
        val resultInside = CampusRepo.getCampus(insidePoint)
        assertNotNull(resultInside)
        assertEquals("Test", resultInside?.name)

        // 4. Test getCampusByName (Hits another method)
        val namedCampus = CampusRepo.getCampusByName("Test")
        assertEquals("Test", namedCampus?.name)
    }

    @Test
    fun `getCampus returns nearest campus when user is far away`() {
        val campusA = Campus("FarNorth", JsonLatLng(80.0, 0.0), emptyList(), emptyList())
        val campusB = Campus("FarSouth", JsonLatLng(-80.0, 0.0), emptyList(), emptyList())
        CampusRepo.setTestCampuses(listOf(campusA, campusB))

        // Point near the equator, but closer to South
        val userLoc = LatLng(-10.0, 0.0)
        val result = CampusRepo.getCampus(userLoc)

        assertEquals("FarSouth", result?.name)
    }
}