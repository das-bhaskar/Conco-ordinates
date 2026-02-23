package com.example.myapplication.logic

import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import java.io.IOException
import okhttp3.*
import org.json.JSONObject

enum class RouteUpdateStatus {
    SUCCESS,
    MISSING_TRAVEL_MODE,
    MISSING_START,
    MISSING_END,
}

class Route(private var routeProvider: RouteProvider) {
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
        routeProvider.getRoute(
            startIm,
            endIm,
            travelModeIm,
            callbackIm
        )

        return RouteUpdateStatus.SUCCESS
    }
}