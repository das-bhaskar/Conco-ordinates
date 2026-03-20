package com.example.myapplication.logic

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.example.myapplication.telemetry.CrashReporter
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleRouteProvider(private val apiKey: String) : RouteProvider {
    private val client = OkHttpClient()

    override suspend fun getRoute(start: LatLng, end: LatLng, mode: String): RouteData? = withContext(Dispatchers.IO) {
        val transportMode = when(mode) {
            "transit" -> "transit"
            "drive" -> "driving"
            else -> "walking"
        }

        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${start.latitude},${start.longitude}" +
                "&destination=${end.latitude},${end.longitude}" +
                "&mode=$transportMode" +
                "&key=$apiKey"

        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            android.util.Log.d("ROUTE_DEBUG", "Response: $responseBody")

            val json = JSONObject(responseBody)

            if (json.getString("status") != "OK") {
                android.util.Log.e("ROUTE_DEBUG", "Error Status: ${json.getString("status")}")
                // Return null instead of a list to match the RouteData? type
                return@withContext null
            }

            val routes = json.getJSONArray("routes")

            if (routes.length() > 0) {
                val route = routes.getJSONObject(0)
                val leg = route.getJSONArray("legs").getJSONObject(0)
                val durationObject = leg.getJSONObject("duration")
                val steps = leg.getJSONArray("steps")

                val firstInstruction = if (steps.length() > 0) {
                    steps.getJSONObject(0).getString("html_instructions")
                        .replace(Regex("<[^>]*>"), "") // Remove HTML tags like <b>
                } else "Follow the path"

                val durationText = leg.getJSONObject("duration").getString("text")
                val durationValue = durationObject.getLong("value")
                val distanceText = leg.getJSONObject("distance").getString("text")
                val overviewPolyline = route.getJSONObject("overview_polyline").getString("points")
                val decodedPoints = PolyUtil.decode(overviewPolyline)

                // Updated RouteData to include the instruction
                return@withContext RouteData(
                    points = decodedPoints,
                    duration = durationText,
                    distance = distanceText,
                    durationSeconds = durationValue,
                    instructions = listOf(firstInstruction)
                )            }
        } catch (e: Exception) {
            android.util.Log.e("ROUTE_DEBUG", "Exception: ${e.message}")
            CrashReporter.setKey("route_mode", transportMode)
            CrashReporter.setKey("route_start", "${start.latitude},${start.longitude}")
            CrashReporter.setKey("route_end", "${end.latitude},${end.longitude}")
            CrashReporter.recordNonFatal(e, "google_directions_request_failed")
        }

        // Final fallback: return null if everything fails
        return@withContext null
    }
}
