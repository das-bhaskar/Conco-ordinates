package com.example.myapplication.logic

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteProviderAdditionalTest {

    @Test
    fun `InterpolatingMockRouteProvider keeps start and end points in generated route`() = runBlocking {
        val start = LatLng(45.497, -73.579)
        val end = LatLng(45.520, -73.610)

        val route = InterpolatingMockRouteProvider(4u).getRoute(start, end, "walk")!!

        assertEquals(start, route.points.first())
        assertEquals(end, route.points.last())
    }

    @Test
    fun `InterpolatingMockRouteProvider returns default metadata and no instructions`() = runBlocking {
        val route = InterpolatingMockRouteProvider(1u).getRoute(
            LatLng(0.0, 0.0),
            LatLng(1.0, 1.0),
            "walk"
        )!!

        assertEquals("0 mins", route.duration)
        assertEquals("0 km", route.distance)
        assertEquals(0L, route.durationSeconds)
        assertTrue(route.instructions.isEmpty())
        assertTrue(route.segments.isEmpty())
    }
}
