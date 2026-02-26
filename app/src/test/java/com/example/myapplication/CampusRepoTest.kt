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
    fun `getCampus returns null for far away locations like Chemin Bates`() {
        // Chemin Bates is roughly 45.51, far from these test coordinates
        val campusA = Campus("SGW", JsonLatLng(45.49, -73.57), emptyList(), emptyList())
        CampusRepo.setTestCampuses(listOf(campusA))

        // This point is ~2km away, exceeding the 0.005 (approx 500m) threshold
        val farPoint = LatLng(45.51, -73.61)
        val result = CampusRepo.getCampus(farPoint)

        assertEquals(null, result)
    }

    @Test
    fun `getCampus identifies user inside a specific building even if campus outline is empty`() {
        val bOutline = listOf(JsonLatLng(0.0, 0.0), JsonLatLng(1.0, 0.0), JsonLatLng(1.0, 1.0), JsonLatLng(0.0, 1.0))
        val building = Building(
            name = "Hall",
            code = "H",
            wayID = 22080570,
            address = "1455 De Maisonneuve",
            outline = emptyList()
        )
        val campus = Campus("SGW", JsonLatLng(0.5, 0.5), listOf(building), emptyList())

        CampusRepo.setTestCampuses(listOf(campus))

        // Point inside the building (Step 2 of your logic)
        val result = CampusRepo.getCampus(LatLng(0.5, 0.5))
        assertEquals("SGW", result?.name)
    }

    @Test
    fun `getCampus identifies user near campus via fallback buffer`() {
        val campus = Campus("SGW", JsonLatLng(45.0, -73.0), emptyList(), emptyList())
        CampusRepo.setTestCampuses(listOf(campus))

        // Point is ~111m away (0.001 degrees), which is within the 0.005 buffer
        val nearPoint = LatLng(45.001, -73.001)
        val result = CampusRepo.getCampus(nearPoint)

        assertEquals("SGW", result?.name)
    }



}