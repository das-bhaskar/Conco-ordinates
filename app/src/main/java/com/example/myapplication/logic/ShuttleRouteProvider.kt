package com.example.myapplication.logic

import com.example.myapplication.data.NearestStopResult
import com.example.myapplication.data.ShuttleStop
import com.example.myapplication.logic.RouteData
import com.example.myapplication.logic.RouteProvider
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.example.myapplication.telemetry.CrashReporter
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ShuttleRouteProvider – implements [RouteProvider] for the Concordia shuttle.
 *
 * US-2.6  Shuttle route rendering
 *
 * Only draws the route between the two fixed shuttle stops (SGW ↔ Loyola).
 * The user's position and destination are NOT connected to the stops —
 * the caller (CampusMap) is responsible for rendering stop markers instead.
 *
 * Uses Google Directions API (driving mode) to get the real road polyline
 * between the two stops.
 */
class ShuttleRouteProvider(
    private val shuttleService: ShuttleService = DefaultShuttleService(),
    private val apiKey: String = com.example.myapplication.BuildConfig.MAPS_API_KEY
) : RouteProvider {

    private val client = OkHttpClient()

    companion object {
        private const val SHUTTLE_RIDE_MINUTES = 20
        private const val SHUTTLE_DISTANCE     = "11.0 km"
    }

    override suspend fun getRoute(start: LatLng, end: LatLng, mode: String): RouteData? =
        withContext(Dispatchers.IO) {

            // Resolve nearest stops for start and end
            val boardingStop  = shuttleService.nearestStop(start).stopOrNull()  ?: return@withContext null
            val alightingStop = shuttleService.nearestStop(end).stopOrNull()    ?: return@withContext null

            // Fetch real road polyline between the two shuttle stops only
            val roadPoints = fetchRoadPolyline(boardingStop.location, alightingStop.location)
                ?: listOf(boardingStop.location, alightingStop.location) // fallback to straight line

            RouteData(
                points   = roadPoints,
                duration = "$SHUTTLE_RIDE_MINUTES min",
                distance = SHUTTLE_DISTANCE
            )
        }

    private fun fetchRoadPolyline(from: LatLng, to: LatLng): List<LatLng>? {
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${from.latitude},${from.longitude}" +
                "&destination=${to.latitude},${to.longitude}" +
                "&mode=driving" +
                "&key=$apiKey"

        return try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: return null)

            if (json.getString("status") != "OK") return null

            val overviewPolyline = json
                .getJSONArray("routes")
                .getJSONObject(0)
                .getJSONObject("overview_polyline")
                .getString("points")

            PolyUtil.decode(overviewPolyline)
        } catch (e: Exception) {
            CrashReporter.recordNonFatal(e, "shuttle_directions_request_failed")
            null
        }
    }

    private fun NearestStopResult.stopOrNull(): ShuttleStop? = when (this) {
        is NearestStopResult.Found     -> stop
        is NearestStopResult.Ambiguous -> candidates.firstOrNull()
        else                           -> null
    }
}
