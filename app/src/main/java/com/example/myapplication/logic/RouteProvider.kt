package com.example.myapplication.logic

import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import java.io.IOException
import okhttp3.*
import org.json.JSONObject

interface RouteProvider {
    fun getRoute(start: LatLng,
                 end: LatLng,
                 travelMode: TravelMode,
                 callback: (List<LatLng>) -> Unit
    )
}

class GoogleRouteProvider(private val context: Context,
                          private val client: OkHttpClient = OkHttpClient()) : RouteProvider {

    // Send to callback function a path between two points using the google routes api.
    override fun getRoute(start: LatLng,
                          end: LatLng,
                          travelMode: TravelMode,
                          callback: (List<LatLng>) -> Unit
    ) {
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

        val request = Request.Builder().url(url).build()

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
}

// Returns parsed path given a valid json string and exception otherwise.
fun decodeRoute(json: String): List<LatLng> {
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

class SimpleMockRouteProvider(private val points: List<LatLng> = emptyList()) : RouteProvider {

    override fun getRoute(start: LatLng,
                          end: LatLng,
                          travelMode: TravelMode,
                          callback: (List<LatLng>) -> Unit
    ) {
        callback(points)
    }
}

class InterpolatingMockRouteProvider(
    private var steps: UInt
) : RouteProvider {

    override fun getRoute(start: LatLng,
                          end: LatLng,
                          travelMode: TravelMode,
                          callback: (List<LatLng>) -> Unit
    ) {
        val latStep = (end.latitude - start.latitude) / steps.toDouble()
        val lngStep = (end.longitude - start.longitude) / steps.toDouble()
        val points = (0..steps.toInt()).map { i ->
            LatLng(start.latitude + latStep * i, start.longitude + lngStep * i)
        }
        callback(points)
    }
}