package com.example.myapplication.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.myapplication.data.Campus
import com.example.myapplication.data.CampusRepo
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil

class MapViewModel : ViewModel() {
    var currentCampus by mutableStateOf<Campus?>(CampusRepo.getCampusByName("SGW"))
        private set
    private var lastProcessedLocation: LatLng? = null

    var highlightedBuildingName by mutableStateOf<String?>(null)
        private set

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

        // OPTIMIZATION: temporarily comment this out to see if it's blocking updates
        /*
        lastProcessedLocation?.let { last ->
            val diffLat = Math.abs(userLocation.latitude - last.latitude)
            val diffLng = Math.abs(userLocation.longitude - last.longitude)
            if (diffLat < 0.00002 && diffLng < 0.00002) return
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