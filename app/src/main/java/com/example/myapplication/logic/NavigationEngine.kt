package com.example.myapplication.logic

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.example.myapplication.data.Building
import com.example.myapplication.data.CampusRepo
import kotlin.math.roundToInt

interface NavigationEngine {
    fun calculateNextInstruction(userPos: LatLng, route: List<LatLng>): String
    fun checkArrival(userPos: LatLng, destination: LatLng): Boolean
    fun calculateBearing(userPos: LatLng, route: List<LatLng>): Float
}
class CampusNavigationEngine : NavigationEngine {

    // 1. ARRIVAL: Use the "Inside Building" logic you requested
    fun checkArrivalWithBuilding(userPos: LatLng, targetBuilding: Building?): Boolean {
        if (targetBuilding == null) return false

        val destination = targetBuilding.getCenter()
        val distance = com.google.maps.android.SphericalUtil.computeDistanceBetween(userPos, destination)

        // Just one simple check. 50 meters is roughly the width of a small building.
        return distance < 50.0
    }
    override fun checkArrival(userPos: LatLng, destination: LatLng): Boolean {
        // Fallback for generic points
        return SphericalUtil.computeDistanceBetween(userPos, destination) < 15.0
    }

    override fun calculateBearing(userPos: LatLng, route: List<LatLng>): Float {
        val target = route.firstOrNull { SphericalUtil.computeDistanceBetween(userPos, it) > 12.0 }
        return if (target != null) {
            SphericalUtil.computeHeading(userPos, target).toFloat()
        } else 0f
    }

    // 2. INSTRUCTIONS: We only use this if Google API fails
    override fun calculateNextInstruction(userPos: LatLng, route: List<LatLng>): String {
        return "Proceed toward destination"
    }
}