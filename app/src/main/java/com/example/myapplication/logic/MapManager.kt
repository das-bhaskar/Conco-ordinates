package com.example.myapplication.logic

import android.graphics.Color
import androidx.core.graphics.toColorInt
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.maps.android.PolyUtil
import com.example.myapplication.data.Campus
import com.example.myapplication.telemetry.CrashReporter

class MapManager(private val googleMap: GoogleMap) {

    // 1. Android-dependent logic (Hard to unit test, stays in class)
    fun getUserLocation(fusedLocationClient: FusedLocationProviderClient, callback: (LatLng?) -> Unit) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    callback(LatLng(location.latitude, location.longitude))
                } else {
                    callback(null)
                }
            }
                .addOnFailureListener {
                    callback(null)
                }
        } catch (e: SecurityException) {
            CrashReporter.setKey("map_manager_operation", "get_user_location")
            CrashReporter.recordNonFatal(e, "location_permission_missing_in_map_manager")
            callback(null)
        }
    }

    fun focusOnCampus(campus: Campus, highlightedBuildingName: String? = null) {
        drawBuildings(campus, highlightedBuildingName)
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(campus.getGoogleCenter(), campus.defaultZoom))
    }

    fun updateHighlightsOnly(campus: Campus, highlightedBuildingName: String? = null) {
        drawBuildings(campus, highlightedBuildingName)
    }

    private var lastBuilding: String? = "FORCE_INITIAL_DRAW"

    private fun drawBuildings(campus: Campus, highlightedBuildingName: String?) {
        if (highlightedBuildingName == lastBuilding) return

        googleMap.clear()
        lastBuilding = highlightedBuildingName

        campus.buildings.forEach { building ->
            val isCurrentBuilding = building.name == highlightedBuildingName
            val polygon = PolygonOptions()
                .addAll(building.getGoogleOutline())
                .strokeWidth(if (isCurrentBuilding) 8f else 4f)
                .strokeColor(if (isCurrentBuilding) "#FFD700".toColorInt() else "#912338".toColorInt())
                .fillColor(if (isCurrentBuilding) Color.argb(80, 255, 204, 0) else Color.argb(80, 145, 35, 56))

            googleMap.addPolygon(polygon)
        }
    }

    // 2. PURE LOGIC (Easy to unit test, moved to companion object)
    // We use this in the app, and we use this in the tests.
    companion object {
        fun findBuildingAtLocation(userLocation: LatLng, campus: Campus): String? {
            val buildingInside = campus.buildings.firstOrNull { building ->
                PolyUtil.containsLocation(userLocation, building.getGoogleOutline(), false)
            }
            if (buildingInside != null) return buildingInside.name

            var closestBuildingName: String? = null
            var shortestDistance = 10.0

            campus.buildings.forEach { building ->
                val distToPoly = distanceFromPoly(userLocation, building.getGoogleOutline())
                if (distToPoly < shortestDistance) {
                    shortestDistance = distToPoly
                    closestBuildingName = building.name
                }
            }
            return closestBuildingName
        }

        fun distanceFromPoly(point: LatLng, poly: List<LatLng>): Double {
            var minDistance = Double.MAX_VALUE
            for (i in poly.indices) {
                val segmentStart = poly[i]
                val segmentEnd = poly[(i + 1) % poly.size]
                val distance = PolyUtil.distanceToLine(point, segmentStart, segmentEnd)
                if (distance < minDistance) minDistance = distance
            }
            return minDistance
        }
    }
}
