package com.example.myapplication

import com.example.myapplication.logic.*
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.*
import org.junit.Test

class RouteStateTest {
    private val pointA = LatLng(0.0, 0.0)
    private val pointB = LatLng(1.0, 1.0)
    private val mockPoints = listOf(pointA, LatLng(0.5, 0.5), pointB)

    @Test
    fun missingStartEndMode() {
        val route = Route(SimpleMockRouteProvider(mockPoints))
        val status = route.updateRoute()
        assertEquals(
            setOf(
                RouteUpdateStatus.MISSING_START,
                RouteUpdateStatus.MISSING_END,
                RouteUpdateStatus.MISSING_TRAVEL_MODE
            ),
            status
        )
    }

    @Test
    fun missingEndMode() {
        val route = Route(SimpleMockRouteProvider(mockPoints))
        route.setStart(pointA)
        val status = route.updateRoute()
        assertEquals(
            setOf(
                RouteUpdateStatus.MISSING_END,
                RouteUpdateStatus.MISSING_TRAVEL_MODE
            ),
            status
        )
    }

    @Test
    fun missingStartMode() {
        val route = Route(SimpleMockRouteProvider(mockPoints))
        route.setEnd(pointA)
        val status = route.updateRoute()
        assertEquals(
            setOf(
                RouteUpdateStatus.MISSING_START,
                RouteUpdateStatus.MISSING_TRAVEL_MODE
            ),
            status
        )
    }

    @Test
    fun missingStartEnd() {
        val route = Route(SimpleMockRouteProvider(mockPoints))
        route.setTravelMode(TravelMode.WALK)
        val status = route.updateRoute()
        assertEquals(
            setOf(
                RouteUpdateStatus.MISSING_START,
                RouteUpdateStatus.MISSING_END
            ),
            status
        )
    }

    @Test
    fun missingStart() {
        val route = Route(SimpleMockRouteProvider(mockPoints))
        route.setEnd(pointB)
        route.setTravelMode(TravelMode.WALK)
        val status = route.updateRoute()
        assertEquals(
            setOf(
                RouteUpdateStatus.MISSING_START,
            ),
            status
        )
    }

    @Test
    fun missingEnd() {
        val route = Route(SimpleMockRouteProvider(mockPoints))
        route.setStart(pointA)
        route.setTravelMode(TravelMode.WALK)
        val status = route.updateRoute()
        assertEquals(
            setOf(
                RouteUpdateStatus.MISSING_END,
            ),
            status
        )
    }

    @Test
    fun missingMode() {
        val route = Route(SimpleMockRouteProvider(mockPoints))
        route.setStart(pointA)
        route.setEnd(pointB)
        val status = route.updateRoute()
        assertEquals(
            setOf(
                RouteUpdateStatus.MISSING_TRAVEL_MODE,
            ),
            status
        )
    }

    @Test
    fun missingNone() {
        val route = Route(SimpleMockRouteProvider(mockPoints))
        route.setStart(pointA)
        route.setEnd(pointB)
        route.setTravelMode(TravelMode.WALK)
        val status = route.updateRoute()
        assertEquals(
            setOf(
                RouteUpdateStatus.SUCCESS,
            ),
            status
        )
    }

    @Test
    fun callbackGivesMockPoints() {
        val route = Route(SimpleMockRouteProvider(mockPoints))
        var received: List<LatLng>? = null
        route.setCallback { received = it.getOrNull() }
        route.setParams(pointA, pointB, TravelMode.WALK)

        assertEquals(mockPoints, received)
    }

    @Test
    fun autoUpdateStart() {
        val route = Route(InterpolatingMockRouteProvider(
            steps = 2u))
        var received: List<LatLng>? = null
        route.setCallback { received = it.getOrNull() }
        route.setParams(pointA, pointB, TravelMode.WALK)

        assertEquals(mockPoints, received)

        route.setStart(LatLng(-1.0, -1.0))

        val mockPointsUpdated = listOf(
            LatLng(-1.0, -1.0),
            LatLng(0.0, 0.0),
            pointB)

        assertEquals(mockPointsUpdated, received)
    }
}