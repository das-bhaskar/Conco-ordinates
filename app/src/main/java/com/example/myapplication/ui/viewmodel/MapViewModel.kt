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
import com.example.myapplication.logic.ShuttleRouteProvider
import com.example.myapplication.logic.ShuttleService
import com.example.myapplication.logic.SimpleMockRouteProvider
import kotlinx.coroutines.launch
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.PolyUtil
import com.example.myapplication.ui.models.BuildingUiState
import com.example.myapplication.ui.models.MapUIMode

/**
 * [shuttleService] has no default value so callers must inject a concrete
 * implementation.  This makes the ViewModel truly modular – swap in a
 * [MockShuttleService] for tests without touching this class.         [#7]
 *
 * [ShuttleRouteProvider] is constructed internally but receives its two
 * dependencies via constructor injection, keeping it testable as well.  [#1][#2]
 */
class MapViewModel(
    private val locationProvider: com.example.myapplication.logic.LocationProvider? = null,
    private val routeProvider: com.example.myapplication.logic.RouteProvider? = null,
    private val shuttleService: ShuttleService                // no default – must be injected
) : ViewModel() {

    private val shuttleRouteProvider = ShuttleRouteProvider(
        shuttleService     = shuttleService,
        googleRouteProvider = routeProvider ?: SimpleMockRouteProvider()
    )

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

            searchResults = emptyList()

            // Emit one atomic state update for shuttle + route.           [#8]
            calculateRouteWithState()
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
        calculateRouteWithState()
    }

    /**
     * Calculates the route and, if shuttle mode is active, refreshes the
     * shuttle status in the **same coroutine** so both are committed in a
     * single [uiBuildingState] assignment – no risk of one update
     * overwriting the other.                                            [#8]
     */
    fun calculateRouteWithState() {
        val start = uiBuildingState.startPoint ?: lastProcessedLocation ?: return
        val end   = uiBuildingState.endPoint   ?: uiBuildingState.building?.getCenter() ?: return
        val isShuttle = uiBuildingState.selectedTransportMode == "shuttle"
        val provider  = if (isShuttle) shuttleRouteProvider else routeProvider

        viewModelScope.launch {
            // Resolve shuttle status inside the coroutine so both
            // shuttle fields and route fields are written atomically.   [#8][#9]
            val shuttleSnapshot = if (isShuttle) {
                val locationToUse = uiBuildingState.startPoint ?: lastProcessedLocation
                val nearestStop   = shuttleService.resolveNearestStop(locationToUse)
                val fromCampus    = nearestStop?.campus ?: "SGW"
                ShuttleSnapshot(
                    availability  = shuttleService.checkAvailability(fromCampus),
                    statusMessage = shuttleService.statusMessage(fromCampus),
                    stopName      = nearestStop?.name   ?: "",
                    stopCampus    = nearestStop?.campus ?: "",
                    stops         = shuttleService.getAllStops()
                )
            } else null

            val routeData = provider?.getRoute(start, end, uiBuildingState.selectedTransportMode)

            // One atomic copy – shuttle + route fields together.        [#8]
            uiBuildingState = if (routeData != null) {
                val builder = LatLngBounds.Builder()
                routeData.points.forEach { builder.include(it) }
                uiBuildingState.copy(
                    routePoints   = routeData.points,
                    routeDuration = routeData.duration,
                    routeDistance = routeData.distance,
                    routeBounds   = builder.build(),
                    routeErrorMessage = null
                )
            } else {
              val mode = uiBuildingState.selectedTransportMode.replaceFirstChar { it.uppercase() }
                uiBuildingState.copy(
                    routePoints   = emptyList(),
                    routeDuration = "-- min",
                    routeDistance = "-- m",
                    routeBounds   = null,
                    routeErrorMessage = "$mode route unavailable between these points."
                )
            }.let { state ->
                if (shuttleSnapshot != null) state.copy(
                    shuttleAvailability      = shuttleSnapshot.availability,
                    shuttleStatusMessage     = shuttleSnapshot.statusMessage,
                    nearestShuttleStopName   = shuttleSnapshot.stopName,
                    nearestShuttleStopCampus = shuttleSnapshot.stopCampus,
                    shuttleStops             = shuttleSnapshot.stops
                ) else state
            }
        }
    }

    // Keep the old name as an alias so existing call-sites still compile.
    fun calculateRoute() = calculateRouteWithState()

    /**
     * Refreshes only the shuttle status fields (no route recalculation).
     * Called from [processLocationUpdate] when the user moves while in
     * shuttle mode – the route itself doesn't need to change.
     */
    private fun refreshShuttleStatus(fromLocation: LatLng?) {
        val locationToUse = uiBuildingState.startPoint ?: fromLocation
        val nearestStop   = shuttleService.resolveNearestStop(locationToUse)  // [#9]
        val fromCampus    = nearestStop?.campus ?: "SGW"

        // Single atomic copy – all shuttle fields together.             [#8]
        uiBuildingState = uiBuildingState.copy(
            shuttleAvailability      = shuttleService.checkAvailability(fromCampus),
            shuttleStatusMessage     = shuttleService.statusMessage(fromCampus),
            nearestShuttleStopName   = nearestStop?.name   ?: "",
            nearestShuttleStopCampus = nearestStop?.campus ?: "",
            shuttleStops             = shuttleService.getAllStops()         // [#6]
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

        uiBuildingState = uiBuildingState.copy(
            startLocationName = uiBuildingState.destinationName,
            destinationName   = uiBuildingState.startLocationName,
            startPoint        = currentDestLatLng,
            endPoint          = currentStartLatLng,
            building          = if (!uiBuildingState.isStartCurrentLocation) null else currentDestBuilding
        )

        uiBuildingState.endPoint?.let { setMapEventWithOffset(it) }
        highlightedBuildingName = uiBuildingState.building?.name

        // Refresh shuttle + route in one atomic update after swap.      [#8]
        calculateRouteWithState()
    }

    fun setMapEventWithOffset(target: LatLng) {
        mapEvent = LatLng(target.latitude - 0.005, target.longitude)
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /** Transient carrier for shuttle status computed inside the coroutine. */
    private data class ShuttleSnapshot(
        val availability:  com.example.myapplication.data.ShuttleAvailability,
        val statusMessage: String,
        val stopName:      String,
        val stopCampus:    String,
        val stops:         List<com.example.myapplication.data.ShuttleStop>
    )
}
