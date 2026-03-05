package com.example.myapplication.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.myapplication.data.Building
import com.example.myapplication.data.Campus
import com.example.myapplication.data.CampusRepo
import androidx.lifecycle.viewModelScope
import com.example.myapplication.logic.CalendarProvider
import com.example.myapplication.logic.SearchResult
import com.example.myapplication.logic.HybridSearchProvider
import com.example.myapplication.logic.ShuttleRouteProvider
import com.example.myapplication.logic.ShuttleService
import com.example.myapplication.logic.SimpleMockRouteProvider
import com.example.myapplication.ui.models.CalendarState
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
 * [calendarProvider] follows the same pattern: nullable with null default so
 * existing call-sites and tests that don't need calendar keep compiling.
 */
class MapViewModel(
    private val locationProvider: com.example.myapplication.logic.LocationProvider? = null,
    private val routeProvider: com.example.myapplication.logic.RouteProvider? = null,
    private val shuttleService: ShuttleService,
    private val calendarProvider: CalendarProvider? = null   // ← NEW
) : ViewModel() {

    private val shuttleRouteProvider = ShuttleRouteProvider(
        shuttleService      = shuttleService,
        googleRouteProvider = routeProvider ?: SimpleMockRouteProvider()
    )

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

    // ── Calendar state ─────────────────────────────────────────────────────────

    var calendarState by mutableStateOf<CalendarState>(CalendarState.Idle)
        private set

    /** Remembers which calendar the user picked so we can re-fetch on demand. */

    // ── Map / Location functions ───────────────────────────────────────────────

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

            uiBuildingState = if (activeSearchField == "start") {
                uiBuildingState.copy(startLocationName = resultName, startPoint = resultCoords)
            } else {
                uiBuildingState.copy(destinationName = resultName, building = selectedBuilding, endPoint = resultCoords)
            }

            resultCoords?.let { setMapEventWithOffset(it) }
            uiBuildingState = uiBuildingState.copy(isSearchExpanded = false)
            searchResults = emptyList()
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
     * shuttle status in the same coroutine so both are committed in a
     * single [uiBuildingState] assignment.                              [#8]
     */
    fun calculateRouteWithState() {
        val start = uiBuildingState.startPoint ?: lastProcessedLocation ?: return
        val end   = uiBuildingState.endPoint   ?: uiBuildingState.building?.getCenter() ?: return
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
                null // Treat network/API crashes as no route found
            }
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
                val modeName = uiBuildingState.selectedTransportMode.replaceFirstChar { it.uppercase() }
                uiBuildingState.copy(
                    routePoints   = emptyList(),
                    routeDuration = "-- min",
                    routeDistance = "-- m",
                    routeBounds   = null,
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

    fun calculateRoute() = calculateRouteWithState()

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
        calculateRouteWithState()
    }

    fun setMapEventWithOffset(target: LatLng) {
        mapEvent = LatLng(target.latitude - 0.005, target.longitude)
    }

    // ── Calendar functions ─────────────────────────────────────────────────────

    /**
     * Step 1 — User taps "Connect Google Calendar".
     * Fetches the list of calendars and transitions to [CalendarState.SelectingCalendar].
     * Must be called from a UI action; never auto-triggered (privacy).
     */
    /**
     * Loads calendars and automatically selects the primary one (first in list).
     * Called after Google Sign-In succeeds — no manual picker needed.
     */
    /**
     * After sign-in, loads all calendars and shows the picker so the user
     * can choose which calendar contains their courses.
     */
    fun loadCalendarsAndAutoSelect() {
        val provider = calendarProvider ?: run {
            calendarState = CalendarState.Error("Calendar not configured")
            return
        }
        viewModelScope.launch {
            calendarState = CalendarState.Loading
            val calendars = provider.getCalendars()
            calendarState = if (calendars.isEmpty()) {
                CalendarState.Error("No calendars found. Make sure you are signed in.")
            } else {
                CalendarState.SelectingCalendar(calendars)
            }
        }
    }

    fun loadCalendars() {
        val provider = calendarProvider ?: run {
            calendarState = CalendarState.Error("Calendar not configured")
            return
        }
        viewModelScope.launch {
            calendarState = CalendarState.Loading
            val calendars = provider.getCalendars()
            calendarState = if (calendars.isEmpty()) {
                CalendarState.Error("No calendars found. Make sure you are signed in.")
            } else {
                CalendarState.SelectingCalendar(calendars)
            }
        }
    }

    /**
     * Step 2 — User selects a calendar from the picker.
     * Fetches upcoming events and finds the next one with a location.
     */
    fun onCalendarSelected(calendarId: String, calendarName: String) {
        selectedCalendarId   = calendarId
        selectedCalendarName = calendarName
        val provider = calendarProvider ?: return

        viewModelScope.launch {
            calendarState = CalendarState.Loading
            // Load week events for the calendar view
            loadWeekEvents(calendarId)
            // Also find next class for the pill
            val event = provider.getNextEventWithLocation(calendarId)
            calendarState = if (event != null) {
                CalendarState.NextClassReady(event, calendarName)
            } else {
                CalendarState.NoUpcomingClass(calendarName)
            }
        }
    }

    /**
     * Step 3 — User taps "Get Directions" on the [NextClassCard].
     * Extracts the location string from the event and routes through the
     * existing search → directions pipeline.
     */
    /** Navigate to a specific event's location (called from week view).
     *  Resolves building code → Building from CampusRepo and auto-selects it,
     *  skipping the search results confirmation step entirely.
     */
    fun navigateToEvent(location: String) {
        // Look up the building directly from CampusRepo by code
        val building = com.example.myapplication.data.CampusRepo
            .getAllCampuses()
            .flatMap { it.buildings }
            .firstOrNull { it.code.equals(location, ignoreCase = true) }

        if (building != null) {
            // Directly set the building as destination — no search results step needed
            uiBuildingState = uiBuildingState.copy(
                mode            = MapUIMode.DIRECTIONS,
                destinationName = building.name,
                building        = building,
                endPoint        = building.getCenter()
            )
            calculateRouteWithState()
        } else {
            // Fallback: use search if code not found in local data
            uiBuildingState = uiBuildingState.copy(
                mode            = MapUIMode.DIRECTIONS,
                destinationName = location
            )
            onSearchQueryChanged(location, field = "dest")
        }
    }

    fun navigateToNextClass(context: android.content.Context) {
        val state    = calendarState as? CalendarState.NextClassReady ?: return
        val location = state.event.location ?: return

        // Put the app into directions mode with the classroom as destination
        uiBuildingState = uiBuildingState.copy(
            mode            = MapUIMode.DIRECTIONS,
            destinationName = location
        )

        // Trigger search so HybridSearchProvider can resolve the location
        // string to a building or Google Places result
        onSearchQueryChanged(location, field = "dest")
    }

    /**
     * Re-fetches the next class for the already-selected calendar.
     * No-op if no calendar has been selected yet.
     */
    // ── Week view state ───────────────────────────────────────────────────────

    var selectedCalendarId by mutableStateOf<String?>(null)
        private set

    var selectedCalendarName by mutableStateOf<String?>(null)
        private set

    var weekEvents by mutableStateOf<List<com.example.myapplication.data.CalendarEvent>>(emptyList())
        private set

    /**
     * The next upcoming CalendarEvent that has a location — shown in NextClassPill on the map.
     * Derived from weekEvents; null if none found or calendar not connected.
     */
    val nextUpcomingEvent: com.example.myapplication.data.CalendarEvent?
        get() {
            val now = System.currentTimeMillis()
            return weekEvents
                .filter { it.startTimeMs >= now && !it.location.isNullOrBlank() }
                .minByOrNull { it.startTimeMs }
        }

    var weekViewLoading by mutableStateOf(false)
        private set

    var currentWeekStartMs by mutableStateOf(com.example.myapplication.logic.currentWeekMonday())
        private set

    /**
     * Loads all events for the week starting at [weekStartMs].
     * Called when the user opens the Calendar tab or navigates weeks.
     */
    fun loadWeekEvents(calendarId: String, weekStartMs: Long = currentWeekStartMs) {
        val provider = calendarProvider ?: return
        currentWeekStartMs = weekStartMs
        viewModelScope.launch {
            weekViewLoading = true
            weekEvents = provider.getWeekEvents(calendarId, weekStartMs)
            weekViewLoading = false
        }
    }

    fun goToPreviousWeek(calendarId: String) {
        loadWeekEvents(calendarId, currentWeekStartMs - 7L * 24 * 60 * 60 * 1000)
    }

    fun goToNextWeek(calendarId: String) {
        loadWeekEvents(calendarId, currentWeekStartMs + 7L * 24 * 60 * 60 * 1000)
    }

    fun refreshNextClass() {
        val id   = selectedCalendarId   ?: return
        val name = selectedCalendarName ?: return
        onCalendarSelected(id, name)
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private data class ShuttleSnapshot(
        val availability:  com.example.myapplication.data.ShuttleAvailability,
        val statusMessage: String,
        val stopName:      String,
        val stopCampus:    String,
        val stops:         List<com.example.myapplication.data.ShuttleStop>
    )
}
