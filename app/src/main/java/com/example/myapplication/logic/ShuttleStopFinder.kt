package com.example.myapplication.logic

import com.example.myapplication.data.NearestStopResult
import com.example.myapplication.data.ShuttleDirection
import com.example.myapplication.data.ShuttleStop
import com.example.myapplication.data.ShuttleStopData
import com.google.android.gms.maps.model.LatLng
import kotlin.math.*

interface ShuttleStopFinder {
    fun findNearest(userLocation: LatLng, direction: ShuttleDirection): NearestStopResult
}

class ShuttleStopFinderImpl : ShuttleStopFinder {

    override fun findNearest(userLocation: LatLng, direction: ShuttleDirection): NearestStopResult {
        // Only boarding stops for the given direction
        val campus = if (direction == ShuttleDirection.SGW_TO_LOYOLA) "SGW" else "Loyola"
        val stops  = ShuttleStopData.ALL_STOPS.filter { it.campus == campus }

        if (stops.isEmpty()) return NearestStopResult.NoStopsAvailable

        // Calculate distance to each stop
        val withDist = stops.map { stop ->
            stop to haversineMeters(userLocation, stop.location)
        }.sortedBy { it.second }

        if (withDist.size == 1) return NearestStopResult.Found(withDist.first().first)

        val nearest     = withDist.first()
        val secondNearest = withDist[1]

        // Equidistant threshold: within 20m of each other
        return if (abs(nearest.second - secondNearest.second) < 20.0) {
            NearestStopResult.Ambiguous(withDist.map { it.first })
        } else {
            NearestStopResult.Found(nearest.first)
        }
    }

    private fun haversineMeters(a: LatLng, b: LatLng): Double {
        val R   = 6371000.0
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)
        return 2 * R * asin(sqrt(h))
    }
}
