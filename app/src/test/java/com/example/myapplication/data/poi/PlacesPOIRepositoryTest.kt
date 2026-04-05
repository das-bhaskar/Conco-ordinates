package com.example.myapplication.data.poi

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlacesPOIRepositoryTest {

    @Test
    fun `single category returns parsed POIs sorted by distance`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val requestedUrls = mutableListOf<String>()
        val category = supportedCategories().first()

        val repository = PlacesPOIRepository(
            apiKey = "test-key",
            ioDispatcher = dispatcher,
            responseFetcher = { url ->
                requestedUrls += url
                """
                {
                  "status": "OK",
                  "results": [
                    {
                      "place_id": "far-place",
                      "name": "Far Place",
                      "vicinity": "Far Address",
                      "geometry": {
                        "location": {
                          "lat": 45.5100,
                          "lng": -73.6000
                        }
                      }
                    },
                    {
                      "place_id": "near-place",
                      "name": "Near Place",
                      "vicinity": "Near Address",
                      "geometry": {
                        "location": {
                          "lat": 45.5001,
                          "lng": -73.5701
                        }
                      }
                    }
                  ]
                }
                """.trimIndent()
            }
        )

        val origin = LatLng(45.5000, -73.5700)
        val result = repository.getNearbyPOIs(origin, 500, category)

        assertEquals(1, requestedUrls.size)
        assertTrue(requestedUrls.first().contains("location=45.5,-73.57"))
        assertTrue(requestedUrls.first().contains("radius=500"))
        assertTrue(requestedUrls.first().contains("type=${category.placesType}"))
        assertTrue(requestedUrls.first().contains("key=test-key"))

        assertEquals(2, result.size)
        assertEquals("near-place", result[0].placeId)
        assertEquals("Near Place", result[0].name)
        assertEquals("Near Address", result[0].address)
        assertEquals(category, result[0].category)

        assertEquals("far-place", result[1].placeId)
        assertTrue(result[0].distanceMeters <= result[1].distanceMeters)
    }

    @Test
    fun `single category returns empty list on ZERO_RESULTS`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val category = supportedCategories().first()

        val repository = PlacesPOIRepository(
            apiKey = "test-key",
            ioDispatcher = dispatcher,
            responseFetcher = {
                """
                {
                  "status": "ZERO_RESULTS",
                  "results": []
                }
                """.trimIndent()
            }
        )

        val result = repository.getNearbyPOIs(
            origin = LatLng(45.5, -73.57),
            radiusMeters = 500,
            category = category
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `single category returns empty list when status blank and results empty`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val category = supportedCategories().first()

        val repository = PlacesPOIRepository(
            apiKey = "test-key",
            ioDispatcher = dispatcher,
            responseFetcher = {
                """
                {
                  "results": []
                }
                """.trimIndent()
            }
        )

        val result = repository.getNearbyPOIs(
            origin = LatLng(45.5, -73.57),
            radiusMeters = 500,
            category = category
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `single category throws POIException when Places API returns non OK status`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val category = supportedCategories().first()

        val repository = PlacesPOIRepository(
            apiKey = "test-key",
            ioDispatcher = dispatcher,
            responseFetcher = {
                """
                {
                  "status": "REQUEST_DENIED",
                  "results": []
                }
                """.trimIndent()
            }
        )

        try {
            repository.getNearbyPOIs(
                origin = LatLng(45.5, -73.57),
                radiusMeters = 500,
                category = category
            )
            fail("Expected POIException")
        } catch (e: POIException) {
            assertTrue(e.message!!.contains("Nearby Search failed"))
            assertTrue(e.message!!.contains("REQUEST_DENIED"))
        }
    }



    @Test
    fun `single category skips invalid results`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val category = supportedCategories().first()

        val repository = PlacesPOIRepository(
            apiKey = "test-key",
            ioDispatcher = dispatcher,
            responseFetcher = {
                """
                {
                  "status": "OK",
                  "results": [
                    {
                      "place_id": "",
                      "name": "Missing PlaceId",
                      "geometry": { "location": { "lat": 45.5, "lng": -73.57 } }
                    },
                    {
                      "place_id": "missing-name",
                      "name": "",
                      "geometry": { "location": { "lat": 45.5, "lng": -73.57 } }
                    },
                    {
                      "place_id": "missing-geometry",
                      "name": "Missing Geometry"
                    },
                    {
                      "place_id": "valid-place",
                      "name": "Valid Place",
                      "vicinity": "123 Test St",
                      "geometry": { "location": { "lat": 45.5002, "lng": -73.5702 } }
                    }
                  ]
                }
                """.trimIndent()
            }
        )

        val result = repository.getNearbyPOIs(
            origin = LatLng(45.5, -73.57),
            radiusMeters = 500,
            category = category
        )

        assertEquals(1, result.size)
        assertEquals("valid-place", result.first().placeId)
        assertEquals("Valid Place", result.first().name)
    }

    @Test
    fun `ALL category merges deduplicates sorts and limits to 20`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val supported = supportedCategories()
        val firstCategory = supported[0]
        val secondCategory = supported[1]

        val repository = PlacesPOIRepository(
            apiKey = "test-key",
            ioDispatcher = dispatcher,
            responseFetcher = { url ->
                when {
                    url.contains("type=${firstCategory.placesType}") -> {
                        """
                        {
                          "status": "OK",
                          "results": [
                            {
                              "place_id": "duplicate",
                              "name": "Duplicate Place",
                              "vicinity": "A",
                              "geometry": { "location": { "lat": 45.5001, "lng": -73.5701 } }
                            },
                            {
                              "place_id": "cat1-only",
                              "name": "Category One",
                              "vicinity": "B",
                              "geometry": { "location": { "lat": 45.5002, "lng": -73.5702 } }
                            }
                          ]
                        }
                        """.trimIndent()
                    }
                    url.contains("type=${secondCategory.placesType}") -> {
                        val extraResults = (1..25).joinToString(",") { index ->
                            """
                            {
                              "place_id": "p$index",
                              "name": "Place $index",
                              "vicinity": "Addr $index",
                              "geometry": { "location": { "lat": ${45.5000 + index * 0.001}, "lng": -73.57 } }
                            }
                            """.trimIndent()
                        }

                        """
                        {
                          "status": "OK",
                          "results": [
                            {
                              "place_id": "duplicate",
                              "name": "Duplicate Place Again",
                              "vicinity": "C",
                              "geometry": { "location": { "lat": 45.5001, "lng": -73.5701 } }
                            },
                            $extraResults
                          ]
                        }
                        """.trimIndent()
                    }
                    else -> """{ "status": "ZERO_RESULTS", "results": [] }"""
                }
            }
        )

        val result = repository.getNearbyPOIs(
            origin = LatLng(45.5, -73.57),
            radiusMeters = 500,
            category = POICategory.ALL
        )

        assertTrue(result.isNotEmpty())
        assertTrue(result.size <= 20)
        assertEquals(result.map { it.placeId }.distinct().size, result.size)
        assertTrue(result.any { it.placeId == "duplicate" })
        assertTrue(result.any { it.placeId == "cat1-only" })

        val distances = result.map { it.distanceMeters }
        assertEquals(distances.sorted(), distances)
    }

    @Test
    fun `ALL category ignores one failing request and still returns remaining results`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val supported = supportedCategories()
        val failingCategory = supported.first()
        val succeedingCategory = supported.first { it != failingCategory }

        val repository = PlacesPOIRepository(
            apiKey = "test-key",
            ioDispatcher = dispatcher,
            responseFetcher = { url ->
                when {
                    url.contains("type=${failingCategory.placesType}") -> {
                        throw RuntimeException("boom")
                    }
                    url.contains("type=${succeedingCategory.placesType}") -> {
                        """
                        {
                          "status": "OK",
                          "results": [
                            {
                              "place_id": "survivor",
                              "name": "Survivor Place",
                              "vicinity": "Still here",
                              "geometry": { "location": { "lat": 45.5003, "lng": -73.5703 } }
                            }
                          ]
                        }
                        """.trimIndent()
                    }
                    else -> """{ "status": "ZERO_RESULTS", "results": [] }"""
                }
            }
        )

        val result = repository.getNearbyPOIs(
            origin = LatLng(45.5, -73.57),
            radiusMeters = 500,
            category = POICategory.ALL
        )

        assertTrue(result.any { it.placeId == "survivor" })
    }

    private fun supportedCategories(): List<POICategory> =
        POICategory.entries.filter { it != POICategory.ALL }
}