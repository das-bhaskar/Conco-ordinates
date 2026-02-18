package com.example.myapplication.logic

import android.graphics.Color
import androidx.core.graphics.toColorInt
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.PolygonOptions
import com.example.myapplication.data.Campus
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.maps.android.PolyUtil

class MapManager(private val googleMap: GoogleMap) {

    fun getUserLocation(fusedLocationClient: FusedLocationProviderClient, callback: (LatLng?) -> Unit) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                // If location is null, we pass null to the callback so the Activity can handle the fallback
                if (location != null) {
                    callback(LatLng(location.latitude, location.longitude))
                } else {
                    callback(null)
                }
            }
        } catch (e: SecurityException) {
            callback(null)
        }
    }

    fun findBuildingAtLocation(userLocation: LatLng, campus: Campus): String? {
        var closestBuildingName: String? = null
        // 10 meters is ideal for downtown street density
        var shortestDistance = 10.0

        campus.buildings.forEach { building ->
            // 1. Check if user is strictly inside the polygon
            if (PolyUtil.containsLocation(userLocation, building.outline, false)) {
                return building.name
            }

            // 2. Check distance to the nearest edge of the building
            val distToPoly = distanceFromPoly(userLocation, building.outline)

            // 3. Update if this building is closer than the current best match
            if (distToPoly < shortestDistance) {
                shortestDistance = distToPoly
                closestBuildingName = building.name
            }
        }
        return closestBuildingName
    }

    // Helper to find the distance (in meters) from a point to the nearest polygon edge
    private fun distanceFromPoly(point: LatLng, poly: List<LatLng>): Double {
        var minDistance = Double.MAX_VALUE
        for (i in poly.indices) {
            val segmentStart = poly[i]
            val segmentEnd = poly[(i + 1) % poly.size]

            // Calculates distance from point to the line segment
            val distance = PolyUtil.distanceToLine(point, segmentStart, segmentEnd)

            if (distance < minDistance) {
                minDistance = distance
            }
        }
        return minDistance
    }

    fun focusOnCampus(campus: Campus, highlightedBuildingName: String? = null) {
        drawBuildings(campus, highlightedBuildingName)
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(campus.center, campus.defaultZoom))
    }

    fun updateHighlightsOnly(campus: Campus, highlightedBuildingName: String? = null) {
        drawBuildings(campus, highlightedBuildingName)
    }

    private var lastBuilding: String? = "NONE"

    private fun drawBuildings(campus: Campus, highlightedBuildingName: String?) {
        if (highlightedBuildingName == lastBuilding) {
            return
        }
        lastBuilding = highlightedBuildingName

        googleMap.clear()
        campus.buildings.forEach { building ->
            val isCurrentBuilding = building.name == highlightedBuildingName

            val polygon = PolygonOptions()
                .addAll(building.outline)
                .strokeWidth(if (isCurrentBuilding) 8f else 4f)
                .strokeColor(if (isCurrentBuilding)
                    "#FFD700".toColorInt() else //gold for when the user is near the building
                    "#912338".toColorInt())
                .fillColor(if (isCurrentBuilding)
                    Color.argb(80, 255, 204, 0) else
                    Color.argb(80, 145, 35, 56))

            googleMap.addPolygon(polygon)
        }
    }
}