package com.example.myapplication.logic

import android.util.Log
import com.example.myapplication.BuildConfig
import com.example.myapplication.data.ShuttleDirection
import com.example.myapplication.data.ShuttleRoute
import com.example.myapplication.data.ShuttleRouteResult
import com.example.myapplication.data.ShuttleStop
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

interface ShuttleDirectionsRepository {
    suspend fun getRoute(
        boarding:  ShuttleStop,
        alighting: ShuttleStop,
        direction: ShuttleDirection
    ): ShuttleRouteResult
}

class ShuttleDirectionsRepositoryImpl : ShuttleDirectionsRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override suspend fun getRoute(
        boarding:  ShuttleStop,
        alighting: ShuttleStop,
        direction: ShuttleDirection
    ): ShuttleRouteResult = withContext(Dispatchers.IO) {

        val apiKey = BuildConfig.MAPS_API_KEY
        if (apiKey.isBlank()) return@withContext ShuttleRouteResult.ApiKeyMissing

        val origin = "${boarding.location.latitude},${boarding.location.longitude}"
        val dest   = "${alighting.location.latitude},${alighting.location.longitude}"
        val url    = "https://maps.googleapis.com/maps/api/directions/json" +
                     "?origin=$origin&destination=$dest&mode=driving&key=$apiKey"

        try {
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            val body     = response.body?.string() ?: return@withContext ShuttleRouteResult.NetworkError

            val json   = JSONObject(body)
            val status = json.optString("status")

            if (status != "OK") {
                Log.w("ShuttleDirections", "API status: $status")
                return@withContext ShuttleRouteResult.NoRouteFound
            }

            val route = json.getJSONArray("routes").getJSONObject(0)
            val leg   = route.getJSONArray("legs").getJSONObject(0)

            val polyline = route.getJSONObject("overview_polyline").getString("points")
            val points   = PolyUtil.decode(polyline)

            val duration = leg.getJSONObject("duration").getString("text")
            val distance = leg.getJSONObject("distance").getString("text")

            ShuttleRouteResult.Success(
                ShuttleRoute(
                    direction      = direction,
                    boardingStop   = boarding,
                    alightingStop  = alighting,
                    polylinePoints = points,
                    durationText   = duration,
                    distanceText   = distance
                )
            )
        } catch (e: Exception) {
            Log.e("ShuttleDirections", "Error: ${e.message}")
            ShuttleRouteResult.NetworkError
        }
    }
}
