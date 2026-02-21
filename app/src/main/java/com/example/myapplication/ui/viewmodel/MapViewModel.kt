package com.example.myapplication.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.myapplication.data.Building
import com.example.myapplication.data.Campus
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.logic.LocationProvider // Added this
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.example.myapplication.ui.models.BuildingUiState

class MapViewModel(private val locationProvider: LocationProvider? = null) : ViewModel() {

    private var isManualCampusSelection = false
    var uiBuildingState by mutableStateOf(BuildingUiState())
        private set

    fun handleMapTap(building: Building?, address: String? = null, imageUrl: String? = null) {
        uiBuildingState = BuildingUiState(
            isVisible = building != null,
            building = building,
            fullAddress = address,
            imageUrl = imageUrl
        )
    }

    var currentCampus by mutableStateOf<Campus?>(CampusRepo.getCampusByName("SGW"))
        private set
    private var lastProcessedLocation: LatLng? = null

    var highlightedBuildingName by mutableStateOf<String?>(null)
        private set

    fun refreshLocation() {
        locationProvider?.getUserLocation { location ->
            location?.let { processLocationUpdate(it) }
        }
    }

    fun onCampusSelected(name: String) {
        val found = CampusRepo.getCampusByName(name)
        if (found != null) {
            isManualCampusSelection = true // Lock the toggle to user choice
            currentCampus = found
            highlightedBuildingName = null
        }
    }

    private fun isTooClose(p1: LatLng, p2: LatLng): Boolean {
        val deltaLat = Math.abs(p1.latitude - p2.latitude)
        val deltaLng = Math.abs(p1.longitude - p2.longitude)
        return deltaLat < 0.00002 && deltaLng < 0.00002
    }

    fun processLocationUpdate(userLocation: LatLng, isForce: Boolean = false) {
        if (isForce) {
            isManualCampusSelection = false
        }

        lastProcessedLocation = userLocation
        val detected = CampusRepo.getCampus(userLocation)

        if (detected != null) {
            // ONLY auto-switch if the user hasn't manually locked a campus choice
            if (!isManualCampusSelection) {
                if (currentCampus?.name != detected.name) {
                    currentCampus = detected
                }
            } else {
                // If they manually picked a campus and finally physically walk INTO it,
                // we release the manual lock so auto-switching works again.
                if (detected.name == currentCampus?.name) {
                    isManualCampusSelection = false
                }
            }
            val buildingAtPos = detected.buildings.firstOrNull { building ->
                val outline = building.getGoogleOutline()
                val isInside = PolyUtil.containsLocation(userLocation, outline, false)
                val isNear = PolyUtil.isLocationOnPath(userLocation, outline, true, 15.0)
                isInside || isNear
            }

            highlightedBuildingName = buildingAtPos?.name
        } else {
            // User walked out of all known campuses; reset the lock
            isManualCampusSelection = false
        }
    }
}