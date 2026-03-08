package com.example.myapplication.logic

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RouteProviderTest {

    @Test
    fun `SimpleMockRouteProvider returns provided mock data`() = runBlocking {
        // Arrange
        val expectedData = RouteData(
            points = listOf(LatLng(45.0, -73.0)),
            duration = "10 mins",
            distance = "1 km"
        )
        val provider = SimpleMockRouteProvider(expectedData)

        // Act
        val result = provider.getRoute(LatLng(0.0, 0.0), LatLng(1.0, 1.0), "walking")

        // Assert
        assertEquals(expectedData, result)
    }

    @Test
    fun `SimpleMockRouteProvider returns null when no data provided`() = runBlocking {
        val provider = SimpleMockRouteProvider(null)
        val result = provider.getRoute(LatLng(0.0, 0.0), LatLng(1.0, 1.0), "walking")
        assertNull(result)
    }

    @Test
    fun `InterpolatingMockRouteProvider generates correct number of points`() = runBlocking {
        // Arrange
        val steps: UInt = 10u
        val provider = InterpolatingMockRouteProvider(steps)
        val start = LatLng(45.0, -73.0)
        val end = LatLng(45.1, -73.1)

        // Act
        val result = provider.getRoute(start, end, "driving")

        // Assert
        assertNotNull(result)
        // steps + 1 because (0..steps) is inclusive
        assertEquals(11, result?.points?.size)
    }

    @Test
    fun `InterpolatingMockRouteProvider interpolates points linearly`() = runBlocking {
        // Arrange
        val steps: UInt = 2u
        val provider = InterpolatingMockRouteProvider(steps)
        val start = LatLng(10.0, 10.0)
        val end = LatLng(20.0, 20.0)

        // Act
        val result = provider.getRoute(start, end, "walking")
        val points = result!!.points

        // Assert
        assertEquals(start.latitude, points[0].latitude, 0.001)
        // Midpoint should be exactly in the middle
        assertEquals(15.0, points[1].latitude, 0.001)
        assertEquals(15.0, points[1].longitude, 0.001)
        assertEquals(end.latitude, points[2].latitude, 0.001)
    }
}