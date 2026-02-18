package com.example.myapplication.logic

import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import java.io.IOException
import okhttp3.*
import org.json.JSONObject

enum class TravelMode {
    PUB_TRANSIT,
    MOTORIZED,
    WALK,
}

enum class RouteUpdateStatus {
    SUCCESS,
    MISSING_TRAVEL_MODE,
    MISSING_START,
    MISSING_END,
}

class Route(private val context: Context) {
    private var route: List<LatLng>? = null
    private var travelMode: TravelMode? = null
    private var start: LatLng? = null
    private var end: LatLng? = null
    private var callback: ((List<LatLng>) -> Unit) = {}

    public fun route(): List<LatLng>? {
        return route
    }
    public fun setTravelMode(input: TravelMode) {
        travelMode = input
        updateRoute()
    }
    public fun setStart(input: LatLng) {
        start = input
        updateRoute()
    }
    public fun setEnd(input: LatLng) {
        end = input
        updateRoute()
    }
    public fun setCallback(input: ((List<LatLng>) -> Unit)) {
        callback = input
    }
    public fun setParams(startIn: LatLng, endIn: LatLng, mode: TravelMode) {
        start = startIn
        end = endIn
        travelMode = mode
        updateRoute()
    }

    // Calls reqPath if all parameters are set
    public fun updateRoute(): RouteUpdateStatus {
        val startIm: LatLng = start ?: return RouteUpdateStatus.MISSING_START
        val endIm: LatLng = end ?: return RouteUpdateStatus.MISSING_END
        val travelModeIm: TravelMode = travelMode ?: return RouteUpdateStatus.MISSING_TRAVEL_MODE
        val callbackIm = callback
        reqPath(startIm, endIm, travelModeIm, callbackIm)

        return RouteUpdateStatus.SUCCESS
    }

    // Send to callback function a path between two points
    private fun reqPath(start: LatLng, end: LatLng, travelMode: TravelMode, callback: (List<LatLng>) -> Unit) {
        val mode = when (travelMode) {
            TravelMode.PUB_TRANSIT -> "transit"
            TravelMode.MOTORIZED -> "driving"
            TravelMode.WALK -> "walking"
        }

        val appInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        val apiKey = appInfo.metaData.getString("com.google.android.geo.API_KEY")

        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${start.latitude},${start.longitude}" +
                "&destination=${end.latitude},${end.longitude}" +
                "&mode=$mode" +
                "&key=${apiKey}"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        // Requests route from google api
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.body.string().let { json ->
                    val points = decodeRoute(json)
                    callback(points)
                }
            }
        })
    }

    // Returns parsed path given a valid json string and exception otherwise.
    private fun decodeRoute(json: String): List<LatLng> {
        val points = mutableListOf<LatLng>()
        try {
            val jsonObject = JSONObject(json)
            val routes = jsonObject.getJSONArray("routes")
            if (routes.length() > 0) {
                val lineBytes = routes.getJSONObject(0)
                    .getJSONObject("overview_polyline")
                    .getString("points")
                // Google api returns string of characters representing bytes
                // describing the points in a sequence. These need to be parsed
                // with a known algorithm before usable.
                points.addAll(PolyUtil.decode(lineBytes))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return points
    }
}