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
    private var callback: ((Result<List<LatLng>>) -> Unit) = {}

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
    public fun setCallback(input: ((Result<List<LatLng>>) -> Unit)) {
        callback = input
    }
    public fun setParams(startIn: LatLng, endIn: LatLng, mode: TravelMode) {
        start = startIn
        end = endIn
        travelMode = mode
        updateRoute()
    }

    // Calls reqPath if all parameters are set
    public fun updateRoute(): Set<RouteUpdateStatus> {
        val errors = errors()
        if (errors.isNotEmpty()) return errors
        routeProvider.getRoute(
            start!!,
            end!!,
            travelMode!!,
            { result ->
                if (result.isSuccess) {
                    route = result.getOrNull()
                }
                callback(result)
            }
        )

        return setOf(RouteUpdateStatus.SUCCESS)
    }

    public fun errors(): Set<RouteUpdateStatus> {
        val errors = mutableSetOf<RouteUpdateStatus>()
        if (start == null) {
            errors.add(RouteUpdateStatus.MISSING_START)
        }
        if (end == null) {
            errors.add(RouteUpdateStatus.MISSING_END)
        }
        if (travelMode == null) {
            errors.add(RouteUpdateStatus.MISSING_TRAVEL_MODE)
        }
        return errors
    }
}