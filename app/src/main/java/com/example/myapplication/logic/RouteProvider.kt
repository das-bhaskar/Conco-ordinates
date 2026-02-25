package com.example.myapplication.logic

import com.google.android.gms.maps.model.LatLng

data class RouteData(
    val points: List<LatLng>,
    val duration: String,
    val distance: String
)

interface RouteProvider {
    suspend fun getRoute(
        start: LatLng,
        end: LatLng,
        mode: String
    ): RouteData?
}

class SimpleMockRouteProvider(private val mockData: RouteData? = null) : RouteProvider {
    override suspend fun getRoute(start: LatLng, end: LatLng, mode: String): RouteData? = mockData
}

class InterpolatingMockRouteProvider(private var steps: UInt) : RouteProvider {
    override suspend fun getRoute(start: LatLng, end: LatLng, mode: String): RouteData? {
        val latStep = (end.latitude - start.latitude) / steps.toDouble()
        val lngStep = (end.longitude - start.longitude) / steps.toDouble()
        val points = (0..steps.toInt()).map { i ->
            LatLng(start.latitude + latStep * i, start.longitude + lngStep * i)
        }
        return RouteData(points, "0 mins", "0 km")
    }
}