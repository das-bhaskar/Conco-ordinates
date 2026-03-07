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
 *
 */
class MapViewModel(
    private val locationProvider: com.example.myapplication.logic.LocationProvider? = null,
    private val routeProvider: com.example.myapplication.logic.RouteProvider? = null,
    private val shuttleService: ShuttleService
) : ViewModel() {

    private val shuttleRouteProvider = ShuttleRouteProvider(
        shuttleService      = shuttleService,
        googleRouteProvider = routeProvider ?: SimpleMockRouteProvider()
    )

    // Pre-indexed for O(1) building code lookup — avoids flatMap on every navigation call
    private val buildingIndex: Map<String, com.example.myapplication.data.Building> by lazy {
        CampusRepo.getAllCampuses()
            .flatMap { it.buildings }
            .associateBy { it.code.lowercase() }
    }

    // ── Search ─────────────────────────────────────────────────────────────────

    var searchQuery by mutableStateOf("")
        private set

    var searchResults by mutableStateOf<List<SearchResult>>(emptyList())
        private set

    private var searchProvider: HybridSearchProvider? = null
    private var isManualCampusSelection = false

    // ── Map UI state ───────────────────────────────────────────────────────────

    var uiBuildingState by mutableStateOf(BuildingUiState())
        private set

    var currentCampus by mutableStateOf<Campus?>(CampusRepo.getCampusByName("SGW"))
        private set

    private var lastProcessedLocation: LatLng? = null

    var highlightedBuildingName by mutableStateOf<String?>(null)
        private set

    var mapEvent by mutableStateOf<LatLng?>(null)
        private set

    var activeSearchField by mutableStateOf("main")
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
            if (uiBuildingState.selectedTransportMode == "shuttle" &&
                uiBuildingState.startLocationName == "Your position") {
                refreshShuttleStatus(userLocation)
            }
        } else {
            isManualCampusSelection = false
        }
    }

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
            // Prepare all new values first, then commit in one atomic copy()
            val updatedState = if (activeSearchField == "start") {
                uiBuildingState.copy(startLocationName = resultName, startPoint = resultCoords)
            } else {
                uiBuildingState.copy(destinationName = resultName, building = selectedBuilding, endPoint = resultCoords)
            }
            uiBuildingState = updatedState.copy(isSearchExpanded = false)
            searchResults = emptyList()
            resultCoords?.let { setMapEventWithOffset(it) }
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
                val homePos = HOME_POSITION
                mapEvent = homePos
                uiBuildingState = uiBuildingState.copy(startPoint = homePos)
            }
            is SearchResult.GoogleResult -> { /* Future implementation */ }
        }
        searchResults = emptyList()
    }

    fun initSearch(client: com.google.android.libraries.places.api.net.PlacesClient) {
        searchProvider = HybridSearchProvider(client)
        searchResults  = listOf(SearchResult.CurrentLocation)
    }

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

    fun calculateRouteWithState() {
        val start     = uiBuildingState.startPoint ?: lastProcessedLocation ?: return
        val end       = uiBuildingState.endPoint   ?: uiBuildingState.building?.getCenter() ?: return
        val isShuttle = uiBuildingState.selectedTransportMode == "shuttle"
        val provider  = if (isShuttle) shuttleRouteProvider else routeProvider
        viewModelScope.launch {
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
            val routeData = try {
                provider?.getRoute(start, end, uiBuildingState.selectedTransportMode)
            } catch (e: Exception) {
                null // Network / API crash — handled below as unavailable
            }
            val modeName = uiBuildingState.selectedTransportMode
                .replaceFirstChar { it.uppercase() }
            uiBuildingState = if (routeData != null) {
                val builder = LatLngBounds.Builder()
                routeData.points.forEach { builder.include(it) }
                uiBuildingState.copy(
                    routePoints       = routeData.points,
                    routeDuration     = routeData.duration,
                    routeDistance     = routeData.distance,
                    routeBounds       = builder.build(),
                    routeErrorMessage = null
                )
            } else {
                uiBuildingState.copy(
                    routePoints       = emptyList(),
                    routeDuration     = "-- min",
                    routeDistance     = "-- m",
                    routeBounds       = null,
                    routeErrorMessage = "$modeName route unavailable between these points."
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

    /**
     * Navigate to a building by code — Map domain only, no Calendar awareness.
     *
     * Accepts a generic building code string so this method works for any
     * feature that needs map navigation (Calendar, search, deep links, etc.).
     * The caller is responsible for extracting the code from their domain object.
     *
     * Uses [buildingIndex] for O(1) lookup instead of flatMap O(n).
     * State is committed in a single atomic copy() to prevent UI flickering.
     */
    fun navigateToBuildingCode(buildingCode: String) {
        // Single O(1) lookup — no flatMap iteration
        val building = buildingIndex[buildingCode.lowercase()]

        // ONE atomic copy() — destination fields + route reset in the same write.
        // Without the reset, the UI briefly renders DIRECTIONS mode with stale
        // route data from the previous navigation (partial-update jank).
        // calculateRouteWithState() is async (coroutine + network) and does the
        // second write only after the route arrives, so the user always sees a
        // clean blank-route state before the new polyline appears.
        uiBuildingState = uiBuildingState.copy(
            mode              = MapUIMode.DIRECTIONS,
            destinationName   = building?.name ?: buildingCode,
            building          = building,
            endPoint          = building?.getCenter(),
            // Reset stale route fields atomically — no partial state visible to UI
            routePoints       = emptyList(),
            routeDuration     = "-- min",
            routeDistance     = "-- m",
            routeBounds       = null,
            routeErrorMessage = null
        )

        if (building != null) {
            calculateRouteWithState()
        } else {
            // Building not in local data — fall back to search
            onSearchQueryChanged(buildingCode, field = "dest")
        }
    }

    fun toggleSearchExpansion(expanded: Boolean, field: String = "main") {
        activeSearchField = field
        uiBuildingState   = uiBuildingState.copy(isSearchExpanded = expanded)
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
        calculateRouteWithState()
    }

    fun setMapEventWithOffset(target: LatLng) {
        mapEvent = LatLng(target.latitude - 0.005, target.longitude)
    }

    private fun refreshShuttleStatus(fromLocation: LatLng?) {
        val locationToUse = uiBuildingState.startPoint ?: fromLocation
        val nearestStop   = shuttleService.resolveNearestStop(locationToUse)
        val fromCampus    = nearestStop?.campus ?: "SGW"
        uiBuildingState = uiBuildingState.copy(
            shuttleAvailability      = shuttleService.checkAvailability(fromCampus),
            shuttleStatusMessage     = shuttleService.statusMessage(fromCampus),
            nearestShuttleStopName   = nearestStop?.name   ?: "",
            nearestShuttleStopCampus = nearestStop?.campus ?: "",
            shuttleStops             = shuttleService.getAllStops()
        )
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    companion object {
        /** Coordinates used for the "Home" search result quick-action. */
        val HOME_POSITION = LatLng(45.51723868665001, -73.627297124046)
    }

    private data class ShuttleSnapshot(
        val availability:  com.example.myapplication.data.ShuttleAvailability,
        val statusMessage: String,
        val stopName:      String,
        val stopCampus:    String,
        val stops:         List<com.example.myapplication.data.ShuttleStop>
    )
}
