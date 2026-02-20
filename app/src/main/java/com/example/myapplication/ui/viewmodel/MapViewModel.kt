package com.example.myapplication.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.myapplication.data.Campus
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.logic.LocationProvider // Added this
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil

// We add the provider to the constructor here
class MapViewModel(private val locationProvider: LocationProvider? = null) : ViewModel() {

    var currentCampus by mutableStateOf<Campus?>(CampusRepo.getCampusByName("SGW"))
        private set
    private var lastProcessedLocation: LatLng? = null

    var highlightedBuildingName by mutableStateOf<String?>(null)
        private set

    // NEW: This allows the UI to tell the ViewModel "Hey, get the location now"
    fun refreshLocation() {
        locationProvider?.getUserLocation { location ->
            location?.let { processLocationUpdate(it) }
        }
    }

    fun onCampusSelected(name: String) {
        val found = CampusRepo.getCampusByName(name)
        if (found != null) {
            currentCampus = found
            highlightedBuildingName = null
        }
    }

    private fun isTooClose(p1: LatLng, p2: LatLng): Boolean {
        val deltaLat = Math.abs(p1.latitude - p2.latitude)
        val deltaLng = Math.abs(p1.longitude - p2.longitude)
        return deltaLat < 0.00002 && deltaLng < 0.00002
    }

    fun processLocationUpdate(userLocation: LatLng) {
        android.util.Log.d("MAP_DEBUG", "1. New Location: ${userLocation.latitude}, ${userLocation.longitude}")

        // KEPT YOUR OPTIMIZATION EXACTLY AS IT WAS
        /*
        lastProcessedLocation?.let { last ->
            if (isTooClose(userLocation, last)) return
        }
        */
        lastProcessedLocation = userLocation

        val detected = CampusRepo.getCampus(userLocation)
        android.util.Log.d("MAP_DEBUG", "2. Detected Campus: ${detected?.name ?: "NONE"}")

        if (detected != null) {
            if (currentCampus?.name != detected.name) {
                android.util.Log.d("MAP_DEBUG", "3. SWITCHING Campus from ${currentCampus?.name} to ${detected.name}")
                currentCampus = detected
            }

            // KEPT YOUR EXACT BUILDING DETECTION LOGIC
            val buildingAtPos = detected.buildings.firstOrNull { building ->
                val outline = building.getGoogleOutline()
                val isInside = PolyUtil.containsLocation(userLocation, outline, false)
                val isNear = PolyUtil.isLocationOnPath(userLocation, outline, true, 15.0)
                isInside || isNear
            }

            android.util.Log.d("MAP_DEBUG", "4. Highlighted Building: ${buildingAtPos?.name ?: "NONE"}")
            highlightedBuildingName = buildingAtPos?.name
        }
    }
}