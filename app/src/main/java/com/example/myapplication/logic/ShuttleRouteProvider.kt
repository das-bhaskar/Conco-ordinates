package com.example.myapplication.logic

import com.google.android.gms.maps.model.LatLng

/**
 * ShuttleRouteProvider – implements [RouteProvider] for the Concordia shuttle.
 *
 * US-2.6  Shuttle route rendering
 *
 * Resolves the two nearest shuttle stops for the given start/end points,
 * then **delegates the road-polyline fetch to [googleRouteProvider]** instead
 * of duplicating the HTTP + JSON-parsing logic that already lives in
 * [GoogleRouteProvider].                                               [#1][#2]
 *
 * The shuttle's fixed duration/distance override the values returned by the
 * delegate so the UI always shows the actual shuttle ride time.
 */
class ShuttleRouteProvider(
    private val shuttleService: ShuttleService,
    private val googleRouteProvider: RouteProvider
) : RouteProvider {

    override suspend fun getRoute(start: LatLng, end: LatLng, mode: String): RouteData? {
        val boardingStop  = shuttleService.resolveNearestStop(start) ?: return null
        val alightingStop = shuttleService.resolveNearestStop(end)   ?: return null

        // Leg 1: Walk to Shuttle
        val walkToStop = googleRouteProvider.getRoute(start, boardingStop.location, "walk")

        // Leg 2: The Shuttle (Drive)
        val shuttleRide = googleRouteProvider.getRoute(boardingStop.location, alightingStop.location, "drive")

        // Leg 3: Walk to Destination
        val walkToDest = googleRouteProvider.getRoute(alightingStop.location, end, "walk")

        // Stitch them together
        val allPoints = mutableListOf<LatLng>()
        val segments = mutableListOf<RouteSegment>()

        walkToStop?.let {
            allPoints.addAll(it.points)
            segments.add(RouteSegment(it.points, "walk"))
        }
        shuttleRide?.let {
            allPoints.addAll(it.points)
            segments.add(RouteSegment(it.points, "shuttle"))
        }
        walkToDest?.let {
            allPoints.addAll(it.points)
            segments.add(RouteSegment(it.points, "walk"))
        }

        return RouteData(
            points = allPoints,
            duration = "Approx 30 min", // You can sum up the durations here if needed
            distance = "Multi-leg",
            segments = segments
        )
    }
}