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
    private val googleRouteProvider: RouteProvider   // injected – no OkHttpClient here
) : RouteProvider {

    companion object {
        private const val SHUTTLE_RIDE_MINUTES = 20
        private const val SHUTTLE_DISTANCE     = "11.0 km"
    }

    override suspend fun getRoute(start: LatLng, end: LatLng, mode: String): RouteData? {
        val boardingStop  = shuttleService.resolveNearestStop(start) ?: return null
        val alightingStop = shuttleService.resolveNearestStop(end)   ?: return null

        // Delegate to the shared GoogleRouteProvider – single source of truth
        // for all Google Directions API calls.
        val road = googleRouteProvider.getRoute(
            boardingStop.location,
            alightingStop.location,
            "drive"
        ) ?: RouteData(
            // Fallback: straight line between stops if network call fails
            points   = listOf(boardingStop.location, alightingStop.location),
            duration = "$SHUTTLE_RIDE_MINUTES min",
            distance = SHUTTLE_DISTANCE
        )

        // Override duration/distance with the known shuttle timetable values
        return road.copy(
            duration = "$SHUTTLE_RIDE_MINUTES min",
            distance = SHUTTLE_DISTANCE
        )
    }
}
