package com.example.myapplication.data

import android.content.Context
import android.content.res.Resources
import com.example.myapplication.R
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.io.ByteArrayInputStream

class ShuttleRepoTest {

    private val mockContext: Context = mock()
    private val mockResources: Resources = mock()

    @Before
    fun setup() {
        resetShuttleRepo() // Ensure a clean slate for every test
    }

    /**
     * Uses reflection to reset the private 'initialized' flag and lists.
     * This allows us to test the 'initialize' logic multiple times.
     */
    private fun resetShuttleRepo() {
        val field = ShuttleRepo::class.java.getDeclaredField("initialized")
        field.isAccessible = true
        field.set(ShuttleRepo, false)

        val stopsField = ShuttleRepo::class.java.getDeclaredField("stops")
        stopsField.isAccessible = true
        stopsField.set(ShuttleRepo, emptyList<ShuttleStop>())
    }

    @Test
    fun `initialize parses valid JSON correctly`() {
        // 1. Arrange: Prepare a valid JSON string
        val fakeJson = """
            {
              "stops": [
                { "id": "S1", "name": "SGW Stop", "campus": "SGW", "latitude": 45.497, "longitude": -73.579 }
              ],
              "schedules": [
                { "fromCampus": "SGW", "departures": ["10:00", "10:30"] }
              ]
            }
        """.trimIndent()
        val inputStream = ByteArrayInputStream(fakeJson.toByteArray())

        whenever(mockContext.resources).thenReturn(mockResources)
        whenever(mockResources.openRawResource(R.raw.shuttle_schedule)).thenReturn(inputStream)

        // 2. Act
        ShuttleRepo.initialize(mockContext)

        // 3. Assert
        val stops = ShuttleRepo.getAllStops()
        assertEquals(1, stops.size)
        assertEquals("SGW Stop", stops[0].name)
        assertEquals(45.497, stops[0].location.latitude, 0.001)

        val departures = ShuttleRepo.getDepartures("SGW")
        assertEquals(listOf("10:00", "10:30"), departures)
    }

    @Test
    fun `getDepartures returns empty list for unknown campus`() {
        // Arrange
        ShuttleRepo.setTestData(emptyList(), mapOf("LOY" to listOf("12:00")))

        // Act
        val result = ShuttleRepo.getDepartures("SGW")

        // Assert
        assertTrue(result.isEmpty())
    }

}