package com.example.myapplication.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.myapplication.data.Building
import com.example.myapplication.data.Campus
import com.example.myapplication.data.CampusRepo
import androidx.lifecycle.viewModelScope
import com.example.myapplication.logic.SearchResult
import com.example.myapplication.logic.HybridSearchProvider
import kotlinx.coroutines.launch
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.example.myapplication.ui.models.BuildingUiState
import com.example.myapplication.ui.models.MapUIMode

class MapViewModel(
    private val locationProvider: com.example.myapplication.logic.LocationProvider? = null,
    private val routeProvider: com.example.myapplication.logic.RouteProvider? = null
) : ViewModel() {



    var searchQuery by mutableStateOf("")
        private set

    var searchResults by mutableStateOf<List<SearchResult>>(emptyList())
        private set

    private var searchProvider: HybridSearchProvider? = null
    private var isManualCampusSelection = false
    var uiBuildingState by mutableStateOf(BuildingUiState())
        private set

    fun handleMapTap(building: Building?, imageUrl: String? = null) {
        if (uiBuildingState.mode == MapUIMode.DIRECTIONS) return
        uiBuildingState = BuildingUiState(
            isVisible = building != null,
            building = building,
            address = building?.address,
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

    var mapEvent by mutableStateOf<LatLng?>(null)
        private set

    fun clearMapEvent() { mapEvent = null }

    fun handleSearchResult(result: SearchResult, context: android.content.Context) {
        val resultName = when (result) {
            is SearchResult.BuildingResult -> result.building.name
            is SearchResult.CampusResult -> result.campus.name
            is SearchResult.GoogleResult -> result.title
            is SearchResult.CurrentLocation -> "Your position"
            is SearchResult.Home -> "Home"
        }

        val resultCoords = when (result) {
            is SearchResult.BuildingResult -> result.building.getCenter()
            is SearchResult.CampusResult -> result.campus.buildings.firstOrNull()?.getCenter()
            is SearchResult.CurrentLocation -> lastProcessedLocation
            is SearchResult.Home -> LatLng(45.51723868665001, -73.627297124046)
            is SearchResult.GoogleResult -> null
        }

        if (uiBuildingState.mode == MapUIMode.DIRECTIONS) {
            val selectedBuilding = if (result is SearchResult.BuildingResult) result.building else null

            uiBuildingState = if (activeSearchField == "start") {
                uiBuildingState.copy(
                    startLocationName = resultName,
                    startPoint = resultCoords // <--- Saved for route
                )
            } else {
                uiBuildingState.copy(
                    destinationName = resultName,
                    building = selectedBuilding,
                    endPoint = resultCoords // <--- Saved for route
                )
            }

            // Camera move for directions selection
            resultCoords?.let { setMapEventWithOffset(it) }

            uiBuildingState = uiBuildingState.copy(isSearchExpanded = false)
            // RECALCULATE ROUTE AUTOMATICALLY
            calculateRoute()

            searchResults = emptyList()
            return
        }

        when (result) {

            is SearchResult.CampusResult -> {
                onCampusSelected(result.campus.name)

                resultCoords?.let { setMapEventWithOffset(it) }

                uiBuildingState = uiBuildingState.copy(
                    isVisible = false,
                    building = null
                )
            }

            is SearchResult.BuildingResult -> {
                val b = result.building
                highlightedBuildingName = b.name

                // Set endPoint in case they switch to directions later
                uiBuildingState = uiBuildingState.copy(
                    isVisible = true,
                    building = b,
                    endPoint = b.getCenter()
                )

                com.example.myapplication.logic.MapInteractionHandler.handleSearchSelection(
                    b, this, context
                )

                CampusRepo.getAllCampuses().find { it.buildings.contains(b) }?.let {
                    currentCampus = it
                    isManualCampusSelection = true
                }

                b.getGoogleOutline().firstOrNull()?.let { mapEvent = it }
            }
            is SearchResult.CurrentLocation -> {
                locationProvider?.getUserLocation { location ->
                    location?.let {
                        mapEvent = it
                        processLocationUpdate(it, isForce = true)
                        uiBuildingState = uiBuildingState.copy(startPoint = it)
                    }
                }
            }
            is SearchResult.Home -> {
                val homePos = LatLng(45.51723868665001, -73.627297124046)
                mapEvent = homePos
                uiBuildingState = uiBuildingState.copy(startPoint = homePos)
            }
            is SearchResult.GoogleResult -> { /* Future implementation */ }
        }
        searchResults = emptyList()
    }
    fun initSearch(client: com.google.android.libraries.places.api.net.PlacesClient) {
        searchProvider = HybridSearchProvider(client)
        searchResults = listOf(SearchResult.CurrentLocation)
    }

    var activeSearchField by mutableStateOf("main")
        private set

    fun onSearchQueryChanged(newQuery: String, field: String = "main") {
        activeSearchField = field

        if (field == "main") {
            searchQuery = newQuery
        } else if (field == "start") {
            uiBuildingState = uiBuildingState.copy(startLocationName = newQuery)
        } else if (field == "dest") {
            uiBuildingState = uiBuildingState.copy(destinationName = newQuery)
        }

        viewModelScope.launch {
            searchProvider?.let { provider ->
                searchResults = provider.search(newQuery)
            }
        }
    }
    fun onDirectionsRequested() {
        uiBuildingState = uiBuildingState.copy(
            mode = com.example.myapplication.ui.models.MapUIMode.DIRECTIONS,
            destinationName = uiBuildingState.building?.name ?: ""
        )
    }

    fun onBackToPreview() {
        uiBuildingState = uiBuildingState.copy(
            mode = com.example.myapplication.ui.models.MapUIMode.PREVIEW
        )
    }
    fun onStartQueryChanged(newQuery: String) {
        uiBuildingState = uiBuildingState.copy(startLocationName = newQuery)
    }

    fun onDestinationQueryChanged(newQuery: String) {
        uiBuildingState = uiBuildingState.copy(destinationName = newQuery)
    }

    fun onTransportModeChanged(mode: String) {
        uiBuildingState = uiBuildingState.copy(selectedTransportMode = mode)
        calculateRoute()
    }

    fun calculateRoute() {

        val start = uiBuildingState.startPoint ?: lastProcessedLocation ?: return

        val end = uiBuildingState.endPoint ?: uiBuildingState.building?.getCenter() ?: return

        viewModelScope.launch {
            val routeData = routeProvider?.getRoute(start, end, uiBuildingState.selectedTransportMode)

            if (routeData != null) {
                val builder = com.google.android.gms.maps.model.LatLngBounds.Builder()
                routeData.points.forEach { builder.include(it) }
                val bounds = builder.build()

                uiBuildingState = uiBuildingState.copy(
                    routePoints = routeData.points,
                    routeDuration = routeData.duration,
                    routeDistance = routeData.distance,
                    routeBounds = bounds
                )
            } else {
                uiBuildingState = uiBuildingState.copy(
                    routePoints = emptyList(),
                    routeDuration = "-- min",
                    routeDistance = "-- m",
                    routeBounds = null
                )
            }
        }
    }

    fun toggleSearchExpansion(expanded: Boolean, field: String = "main") {
        activeSearchField = field
        uiBuildingState = uiBuildingState.copy(isSearchExpanded = expanded)
    }
    fun swapLocations() {
        val currentStartLatLng = uiBuildingState.startPoint ?: lastProcessedLocation
        val currentDestLatLng = uiBuildingState.endPoint ?: uiBuildingState.building?.getCenter()

        val currentDestBuilding = uiBuildingState.building
        uiBuildingState = uiBuildingState.copy(
            startLocationName = uiBuildingState.destinationName,
            destinationName = uiBuildingState.startLocationName,
            startPoint = currentDestLatLng,
            endPoint = currentStartLatLng,
            building = if (!uiBuildingState.isStartCurrentLocation) null else currentDestBuilding        )

        uiBuildingState.endPoint?.let { setMapEventWithOffset(it) }

        // Update the building highlight name for the renderer
        highlightedBuildingName = uiBuildingState.building?.name

        calculateRoute()
    }
    fun setMapEventWithOffset(target: LatLng) {
        val offsetTarget = LatLng(target.latitude - 0.005, target.longitude)
        mapEvent = offsetTarget
    }
}