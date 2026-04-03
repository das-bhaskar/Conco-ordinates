package com.example.myapplication.data.poi

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacesPOIRepositoryTest {

    @Test
    fun `getNearbyPOIs returns empty list for ZERO_RESULTS`() = runBlocking {
        val repository = PlacesPOIRepository(
            apiKey = "test-key",
            responseFetcher = { """{"status":"ZERO_RESULTS"}""" }
        )

        val results = repository.getNearbyPOIs(TEST_ORIGIN, 500, POICategory.CAFE)

        assertTrue(results.isEmpty())
    }

    @Test(expected = POIException::class)
    fun `getNearbyPOIs throws wrapped exception when fetcher fails`() {
        val repository = PlacesPOIRepository(
            apiKey = "test-key",
            responseFetcher = { throw IllegalStateException("boom") }
        )

        runBlocking {
            repository.getNearbyPOIs(TEST_ORIGIN, 500, POICategory.CAFE)
        }
    }

    @Test
    fun `getNearbyPOIs uses single category in request URL`() = runBlocking {
        var capturedUrl = ""
        val repository = PlacesPOIRepository(
            apiKey = "test-key",
            responseFetcher = { url ->
                capturedUrl = url
                """{"status":"ZERO_RESULTS"}"""
            }
        )

        repository.getNearbyPOIs(TEST_ORIGIN, 750, POICategory.CAFE)

        assertTrue(capturedUrl.contains("type=cafe"))
        assertTrue(capturedUrl.contains("radius=750"))
    }

    @Test
    fun `getNearbyPOIs fans out requests for ALL category`() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val repository = PlacesPOIRepository(
            apiKey = "test-key",
            responseFetcher = { url ->
                requestedUrls += url
                """{"status":"ZERO_RESULTS"}"""
            }
        )

        val results = repository.getNearbyPOIs(TEST_ORIGIN, 500, POICategory.ALL)

        assertTrue(results.isEmpty())
        assertEquals(POICategory.entries.count { it != POICategory.ALL }, requestedUrls.size)
        assertTrue(requestedUrls.none { it.contains("type=") && it.endsWith("type=&key=test-key") })
    }

    companion object {
        private val TEST_ORIGIN = LatLng(45.497, -73.579)
    }
}
