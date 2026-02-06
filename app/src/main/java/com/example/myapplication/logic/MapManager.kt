package com.example.myapplication.logic

import android.graphics.Color
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.PolygonOptions
import com.example.myapplication.data.Campus
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.maps.android.PolyUtil

class MapManager(private val googleMap: GoogleMap) {

    // Task 1.5.1: Retrieve user's current location
    fun getUserLocation(fusedLocationClient: FusedLocationProviderClient, callback: (LatLng) -> Unit) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    callback(userLatLng)
                }
            }
        } catch (e: SecurityException) {
            // Permission missing
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

    // Use this for Manual Button clicks (SGW/Loyola/Recenter)
    fun focusOnCampus(campus: Campus, highlightedBuildingName: String? = null) {
        drawBuildings(campus, highlightedBuildingName)
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(campus.center, campus.defaultZoom))
    }

    // Task 1.5.4: Use this for Background GPS updates (No camera yanking)
    fun updateHighlightsOnly(campus: Campus, highlightedBuildingName: String? = null) {
        drawBuildings(campus, highlightedBuildingName)
    }

    private fun drawBuildings(campus: Campus, highlightedBuildingName: String?) {
        googleMap.clear()
        campus.buildings.forEach { building ->
            val isCurrentBuilding = building.name == highlightedBuildingName

            val polygon = PolygonOptions()
                .addAll(building.outline)
                .strokeWidth(if (isCurrentBuilding) 8f else 4f)
                .strokeColor(Color.parseColor("#912338"))
                .fillColor(if (isCurrentBuilding)
                    Color.argb(180, 145, 35, 56) else
                    Color.argb(80, 145, 35, 56))

            googleMap.addPolygon(polygon)
        }
    }
}