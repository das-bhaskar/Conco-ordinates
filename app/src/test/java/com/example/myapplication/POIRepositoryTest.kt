package com.example.myapplication.data.poi

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class POIRepositoryTest {

    @Test
    fun `DEFAULT_RADIUS should be 500`() {
        assertEquals(500, POIRepository.DEFAULT_RADIUS)
    }

    @Test
    fun `POIException should keep message and cause`() {
        val cause = RuntimeException("network failure")

        val exception = POIException("Failed to fetch POIs", cause)

        assertEquals("Failed to fetch POIs", exception.message)
        assertSame(cause, exception.cause)
    }

    @Test
    fun `getNearbyPOIs should use default radius and category when omitted`() = runTest {
        val fakeRepo = CapturingPOIRepository()
        val origin = LatLng(45.5017, -73.5673)

        fakeRepo.getNearbyPOIs(origin)

        assertEquals(origin, fakeRepo.capturedOrigin)
        assertEquals(POIRepository.DEFAULT_RADIUS, fakeRepo.capturedRadius)
        assertEquals(POICategory.ALL, fakeRepo.capturedCategory)
    }

    @Test
    fun `getNearbyPOIs should use provided radius and category`() = runTest {
        val fakeRepo = CapturingPOIRepository()
        val origin = LatLng(45.5017, -73.5673)
        val radius = 1000
        val category = POICategory.ALL   // replace with another category if your enum has one

        fakeRepo.getNearbyPOIs(
            origin = origin,
            radiusMeters = radius,
            category = category
        )

        assertEquals(origin, fakeRepo.capturedOrigin)
        assertEquals(radius, fakeRepo.capturedRadius)
        assertEquals(category, fakeRepo.capturedCategory)
    }

    @Test
    fun `getNearbyPOIs should return repository result`() = runTest {
        val fakeRepo = CapturingPOIRepository()

        val result = fakeRepo.getNearbyPOIs(
            origin = LatLng(45.5017, -73.5673)
        )

        assertTrue(result.isEmpty())
    }

    /**
     * Simple fake repository used to verify parameter passing.
     * No assumptions about POI structure are needed.
     */
    private class CapturingPOIRepository : POIRepository {
        lateinit var capturedOrigin: LatLng
        var capturedRadius: Int = -1
        lateinit var capturedCategory: POICategory

        override suspend fun getNearbyPOIs(
            origin: LatLng,
            radiusMeters: Int,
            category: POICategory
        ): List<POI> {
            capturedOrigin = origin
            capturedRadius = radiusMeters
            capturedCategory = category
            return emptyList()
        }
    }
}