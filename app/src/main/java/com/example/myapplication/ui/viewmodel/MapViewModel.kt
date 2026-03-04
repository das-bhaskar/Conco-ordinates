package com.example.myapplication.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.myapplication.data.Building
import com.example.myapplication.data.Campus
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.data.NearestStopResult
import androidx.lifecycle.viewModelScope
import com.example.myapplication.logic.SearchResult
import com.example.myapplication.logic.HybridSearchProvider
import com.example.myapplication.logic.DefaultShuttleService
import com.example.myapplication.logic.ShuttleRouteProvider
import com.example.myapplication.logic.ShuttleService
import kotlinx.coroutines.launch
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.example.myapplication.ui.models.BuildingUiState
import com.example.myapplication.ui.models.MapUIMode

class MapViewModel(
    private val locationProvider: com.example.myapplication.logic.LocationProvider? = null,
    private val routeProvider: com.example.myapplication.logic.RouteProvider? = null,
    private val shuttleService: ShuttleService = DefaultShuttleService()
) : ViewModel() {

    private val shuttleRouteProvider = ShuttleRouteProvider(shuttleService)

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
            building  = building,
            address   = building?.address,
            imageUrl  = imageUrl
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
            isManualCampusSelection = true
            currentCampus = found
            highlightedBuildingName = null
        }
    }

    fun processLocationUpdate(userLocation: LatLng, isForce: Boolean = false) {
        if (isForce) isManualCampusSelection = false

        lastProcessedLocation = userLocation
        val detected = CampusRepo.getCampus(userLocation)

        if (detected != null) {
            if (!isManualCampusSelection) {
                if (currentCampus?.name != detected.name) currentCampus = detected
            } else {
                if (detected.name == currentCampus?.name) isManualCampusSelection = false
            }

            val buildingAtPos = detected.buildings.firstOrNull { building ->
                val outline = building.getGoogleOutline()
                PolyUtil.containsLocation(userLocation, outline, false) ||
                        PolyUtil.isLocationOnPath(userLocation, outline, true, 15.0)
            }
            highlightedBuildingName = buildingAtPos?.name

            // US-2.8: refresh shuttle status on location update,
            // but only if start point hasn't been manually set
            if (uiBuildingState.selectedTransportMode == "shuttle" &&
                uiBuildingState.startLocationName == "Your position") {
                refreshShuttleStatus(userLocation)
            }
        } else {
            isManualCampusSelection = false
        }
    }

    var mapEvent by mutableStateOf<LatLng?>(null)
        private set

    fun clearMapEvent() { mapEvent = null }

    fun handleSearchResult(result: SearchResult, context: android.content.Context) {
        val resultName = when (result) {
            is SearchResult.BuildingResult  -> result.building.name
            is SearchResult.CampusResult    -> result.campus.name
            is SearchResult.GoogleResult    -> result.title
            is SearchResult.CurrentLocation -> "Your position"
            is SearchResult.Home            -> "Home"
        }

        val resultCoords = when (result) {
            is SearchResult.BuildingResult  -> result.building.getCenter()
            is SearchResult.CampusResult    -> result.campus.buildings.firstOrNull()?.getCenter()
            is SearchResult.CurrentLocation -> lastProcessedLocation
            is SearchResult.Home            -> LatLng(45.51723868665001, -73.627297124046)
            is SearchResult.GoogleResult    -> null
        }

        if (uiBuildingState.mode == MapUIMode.DIRECTIONS) {
            val selectedBuilding = if (result is SearchResult.BuildingResult) result.building else null

            uiBuildingState = if (activeSearchField == "start") {
                uiBuildingState.copy(startLocationName = resultName, startPoint = resultCoords)
            } else {
                uiBuildingState.copy(destinationName = resultName, building = selectedBuilding, endPoint = resultCoords)
            }

            resultCoords?.let { setMapEventWithOffset(it) }
            uiBuildingState = uiBuildingState.copy(isSearchExpanded = false)

            // After search selection in directions mode, refresh shuttle with new startPoint
            if (uiBuildingState.selectedTransportMode == "shuttle") {
                refreshShuttleStatus(uiBuildingState.startPoint)
            }

            calculateRoute()
            searchResults = emptyList()
            return
        }

        when (result) {
            is SearchResult.CampusResult -> {
                onCampusSelected(result.campus.name)
                resultCoords?.let { setMapEventWithOffset(it) }
                uiBuildingState = uiBuildingState.copy(isVisible = false, building = null)
            }
            is SearchResult.BuildingResult -> {
                val b = result.building
                highlightedBuildingName = b.name
                uiBuildingState = uiBuildingState.copy(isVisible = true, building = b, endPoint = b.getCenter())
                com.example.myapplication.logic.MapInteractionHandler.handleSearchSelection(b, this, context)
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
        when (field) {
            "main"  -> searchQuery = newQuery
            "start" -> uiBuildingState = uiBuildingState.copy(startLocationName = newQuery)
            "dest"  -> uiBuildingState = uiBuildingState.copy(destinationName = newQuery)
        }
        viewModelScope.launch {
            searchProvider?.let { searchResults = it.search(newQuery) }
        }
    }

    fun onDirectionsRequested() {
        uiBuildingState = uiBuildingState.copy(
            mode            = MapUIMode.DIRECTIONS,
            destinationName = uiBuildingState.building?.name ?: ""
        )
    }

    fun onBackToPreview() {
        uiBuildingState = uiBuildingState.copy(mode = MapUIMode.PREVIEW)
    }

    fun onStartQueryChanged(newQuery: String) {
        uiBuildingState = uiBuildingState.copy(startLocationName = newQuery)
    }

    fun onDestinationQueryChanged(newQuery: String) {
        uiBuildingState = uiBuildingState.copy(destinationName = newQuery)
    }

    fun onTransportModeChanged(mode: String) {
        uiBuildingState = uiBuildingState.copy(selectedTransportMode = mode)
        if (mode == "shuttle") {
            // Use startPoint if manually set, otherwise fall back to current location
            val fromLocation = uiBuildingState.startPoint ?: lastProcessedLocation
            refreshShuttleStatus(fromLocation)
        }
        calculateRoute()
    }

    fun calculateRoute() {
        val start = uiBuildingState.startPoint ?: lastProcessedLocation ?: return
        val end   = uiBuildingState.endPoint   ?: uiBuildingState.building?.getCenter() ?: return

        val provider = if (uiBuildingState.selectedTransportMode == "shuttle") {
            shuttleRouteProvider
        } else {
            routeProvider
        }

        viewModelScope.launch {
            val routeData = provider?.getRoute(start, end, uiBuildingState.selectedTransportMode)
            if (routeData != null) {
                val builder = com.google.android.gms.maps.model.LatLngBounds.Builder()
                routeData.points.forEach { builder.include(it) }
                uiBuildingState = uiBuildingState.copy(
                    routePoints   = routeData.points,
                    routeDuration = routeData.duration,
                    routeDistance = routeData.distance,
                    routeBounds   = builder.build()
                )
            } else {
                uiBuildingState = uiBuildingState.copy(
                    routePoints   = emptyList(),
                    routeDuration = "-- min",
                    routeDistance = "-- m",
                    routeBounds   = null
                )
            }
        }
    }

    /**
     * Refreshes shuttle status based on the from-location (start point).
     * Uses startPoint from uiBuildingState if available, falls back to [fromLocation].
     * This ensures swap operations correctly reflect the new direction.
     */
    private fun refreshShuttleStatus(fromLocation: LatLng?) {
        // Always prefer the current startPoint in state (already updated before this call)
        val locationToUse = uiBuildingState.startPoint ?: fromLocation

        val nearestResult = shuttleService.nearestStop(locationToUse)
        val nearestStop = when (nearestResult) {
            is NearestStopResult.Found     -> nearestResult.stop
            is NearestStopResult.Ambiguous -> nearestResult.candidates.firstOrNull()
            else                           -> null
        }

        val fromCampus   = nearestStop?.campus ?: "SGW"
        val availability = shuttleService.checkAvailability(fromCampus)
        val statusMsg    = shuttleService.statusMessage(fromCampus)

        uiBuildingState = uiBuildingState.copy(
            shuttleAvailability      = availability,
            shuttleStatusMessage     = statusMsg,
            nearestShuttleStopName   = nearestStop?.name   ?: "",
            nearestShuttleStopCampus = nearestStop?.campus ?: ""
        )
    }

    fun toggleSearchExpansion(expanded: Boolean, field: String = "main") {
        activeSearchField = field
        uiBuildingState = uiBuildingState.copy(isSearchExpanded = expanded)
    }

    fun swapLocations() {
        val currentStartLatLng  = uiBuildingState.startPoint ?: lastProcessedLocation
        val currentDestLatLng   = uiBuildingState.endPoint   ?: uiBuildingState.building?.getCenter()
        val currentDestBuilding = uiBuildingState.building

        // Swap names, points and building first
        uiBuildingState = uiBuildingState.copy(
            startLocationName = uiBuildingState.destinationName,
            destinationName   = uiBuildingState.startLocationName,
            startPoint        = currentDestLatLng,
            endPoint          = currentStartLatLng,
            building          = if (!uiBuildingState.isStartCurrentLocation) null else currentDestBuilding
        )

        uiBuildingState.endPoint?.let { setMapEventWithOffset(it) }
        highlightedBuildingName = uiBuildingState.building?.name

        // Refresh shuttle AFTER swap so startPoint already reflects new direction
        if (uiBuildingState.selectedTransportMode == "shuttle") {
            refreshShuttleStatus(uiBuildingState.startPoint)
        }

        calculateRoute()
    }

    fun setMapEventWithOffset(target: LatLng) {
        mapEvent = LatLng(target.latitude - 0.005, target.longitude)
    }
}
