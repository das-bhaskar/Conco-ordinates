package com.example.myapplication.logic

import com.example.myapplication.data.CampusRepo
import com.example.myapplication.data.ShuttleAvailability
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
        val startCampus = CampusRepo.getCampus(start)
        val endCampus = CampusRepo.getCampus(end)

        // 1. Same-campus check (Returns actual walk time)
        if (startCampus != null && startCampus == endCampus) {
            val walkOnly = googleRouteProvider.getRoute(start, end, "walk")
            return walkOnly?.copy(segments = listOf(RouteSegment(walkOnly.points, "walk")))
        }

        val boardingStop = shuttleService.resolveNearestStop(start) ?: return null
        val alightingStop = shuttleService.resolveNearestStop(end) ?: return null

        // 2. Fetch the 3 real legs from Google
        val walkToStop = googleRouteProvider.getRoute(start, boardingStop.location, "walk")
        val shuttleRide =
            googleRouteProvider.getRoute(boardingStop.location, alightingStop.location, "drive")
        val walkToDest = googleRouteProvider.getRoute(alightingStop.location, end, "walk")

        val allPoints = mutableListOf<LatLng>()
        val segments = mutableListOf<RouteSegment>()

        // Helper to extract "5" from "5 mins" or "1 hour 2 mins"
        fun parseMinutes(duration: String?): Int {
            if (duration == null) return 0
            return duration.split(" ").firstOrNull { it.all { char -> char.isDigit() } }
                ?.toIntOrNull() ?: 0
        }

        // 3. Sum up the travel durations
        val travelTime = parseMinutes(walkToStop?.duration) +
                parseMinutes(shuttleRide?.duration) +
                parseMinutes(walkToDest?.duration)

        // 4. Get the real-time shuttle wait
        val availability = shuttleService.checkAvailability(startCampus?.name ?: "")
        val waitTime =
            if (availability is ShuttleAvailability.Active) availability.nextDepartureMinutes else 0

        val totalTime = travelTime //+ waitTime

        // Stitch points and segments
        listOf(
            walkToStop to "walk",
            shuttleRide to "shuttle",
            walkToDest to "walk"
        ).forEach { (data, type) ->
            data?.let {
                allPoints.addAll(it.points)
                segments.add(RouteSegment(it.points, type))
            }
        }
        val totalSeconds = (walkToStop?.durationSeconds ?: 0L) +
                (shuttleRide?.durationSeconds ?: 0L) +
                (walkToDest?.durationSeconds ?: 0L)
        return RouteData(
            points = allPoints,
            duration = "$totalTime min", // Now dynamic based on Google + Shuttle wait
            distance = "Multi-leg journey",
            durationSeconds = totalSeconds,
            segments = segments
        )
    }
}