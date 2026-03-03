package com.example.myapplication.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.*
import com.example.myapplication.logic.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch

class ShuttleViewModel(
    private val availabilityRepo: ShuttleRepository         = ShuttleRepositoryImpl(),
    private val stopFinder: ShuttleStopFinder               = ShuttleStopFinderImpl(),
    private val directionsRepo: ShuttleDirectionsRepository = ShuttleDirectionsRepositoryImpl()
) : ViewModel() {

    var isShuttleEnabled by mutableStateOf(false)
        private set
    var shuttleStatusText by mutableStateOf("")
        private set
    var nearestStop by mutableStateOf<ShuttleStop?>(null)
        private set
    var ambiguousStops by mutableStateOf<List<ShuttleStop>>(emptyList())
        private set
    var selectedStop by mutableStateOf<ShuttleStop?>(null)
        private set
    var shuttleRoute by mutableStateOf<ShuttleRoute?>(null)
        private set
    var isLoadingRoute by mutableStateOf(false)
        private set
    var routeError by mutableStateOf<String?>(null)
        private set
    var currentDirection by mutableStateOf(ShuttleDirection.SGW_TO_LOYOLA)
        private set
    var isShuttleModeActive by mutableStateOf(false)
        private set

    fun enableShuttleMode(userLocation: LatLng?, startCampus: String?) {
        isShuttleModeActive = true
        currentDirection = if (startCampus?.contains("SGW", ignoreCase = true) == true)
            ShuttleDirection.SGW_TO_LOYOLA else ShuttleDirection.LOYOLA_TO_SGW

        val availability = availabilityRepo.getAvailability(currentDirection)
        applyAvailability(availability)

        val canProceed = availability is ShuttleAvailability.Active
                || availability is ShuttleAvailability.ScheduleUnavailable
        if (!canProceed) return

        if (userLocation != null) {
            findNearestStop(userLocation, currentDirection)
        } else {
            shuttleStatusText = "Location unavailable — please select a stop manually"
        }
    }

    fun swapDirection(userLocation: LatLng?) {
        if (!isShuttleModeActive) return

        currentDirection = if (currentDirection == ShuttleDirection.SGW_TO_LOYOLA)
            ShuttleDirection.LOYOLA_TO_SGW else ShuttleDirection.SGW_TO_LOYOLA

        val availability = availabilityRepo.getAvailability(currentDirection)
        applyAvailability(availability)

        val canProceed = availability is ShuttleAvailability.Active
                || availability is ShuttleAvailability.ScheduleUnavailable
        if (!canProceed) {
            shuttleRoute = null
            nearestStop = null
            return
        }

        if (userLocation != null) {
            findNearestStop(userLocation, currentDirection)
        } else {
            shuttleStatusText = "Location unavailable — please select a stop manually"
        }
    }

    private fun applyAvailability(availability: ShuttleAvailability) {
        when (availability) {
            is ShuttleAvailability.Active -> {
                isShuttleEnabled  = true
                shuttleStatusText = formatCountdown(availability.nextDepartureMinutes)
            }
            is ShuttleAvailability.OutOfService -> {
                isShuttleEnabled  = false
                shuttleStatusText = "Shuttle out of service"
            }
            is ShuttleAvailability.WeekendOrHoliday -> {
                isShuttleEnabled  = false
                shuttleStatusText = "Shuttle does not operate on weekends"
            }
            is ShuttleAvailability.ScheduleUnavailable -> {
                isShuttleEnabled  = true
                shuttleStatusText = "Schedule unavailable — check concordia.ca"
            }
        }
    }

    private fun formatCountdown(minutes: Int) = when {
        minutes <= 0 -> "Shuttle departing now"
        minutes == 1 -> "Next shuttle in 1 minute"
        minutes < 60 -> "Next shuttle in $minutes minutes"
        else         -> "Next shuttle in ${minutes / 60}h ${minutes % 60}min"
    }

    fun findNearestStop(userLocation: LatLng, direction: ShuttleDirection) {
        when (val result = stopFinder.findNearest(userLocation, direction)) {
            is NearestStopResult.Found -> {
                nearestStop    = result.stop
                selectedStop   = result.stop
                ambiguousStops = emptyList()
                fetchShuttleRoute(result.stop, direction)
            }
            is NearestStopResult.Ambiguous -> {
                nearestStop    = null
                ambiguousStops = result.candidates
            }
            is NearestStopResult.LocationUnavailable -> {
                nearestStop       = null
                shuttleStatusText = "Location unavailable — please select a stop manually"
            }
            is NearestStopResult.NoStopsAvailable -> {
                nearestStop       = null
                shuttleStatusText = "Shuttle stop data unavailable"
            }
        }
    }

    fun onUserSelectedStop(stop: ShuttleStop) {
        selectedStop   = stop
        nearestStop    = stop
        ambiguousStops = emptyList()
        fetchShuttleRoute(stop, currentDirection)
    }

    fun onUserLocationUpdated(newLocation: LatLng) {
        if (isShuttleModeActive && isShuttleEnabled) {
            findNearestStop(newLocation, currentDirection)
        }
    }

    private fun fetchShuttleRoute(boardingStop: ShuttleStop, direction: ShuttleDirection) {
        val alightingStop = when (direction) {
            ShuttleDirection.SGW_TO_LOYOLA ->
                ShuttleStopData.ALL_STOPS.first { it.campus == "Loyola" }
            ShuttleDirection.LOYOLA_TO_SGW ->
                ShuttleStopData.ALL_STOPS.first { it.campus == "SGW" }
        }

        isLoadingRoute = true
        routeError     = null

        viewModelScope.launch {
            when (val result = directionsRepo.getRoute(boardingStop, alightingStop, direction)) {
                is ShuttleRouteResult.Success -> {
                    shuttleRoute   = result.route.copy(
                        direction     = direction,
                        boardingStop  = boardingStop,
                        alightingStop = alightingStop
                    )
                    isLoadingRoute = false
                }
                is ShuttleRouteResult.NetworkError -> {
                    routeError     = "Network error — check your connection"
                    isLoadingRoute = false
                }
                is ShuttleRouteResult.NoRouteFound -> {
                    routeError     = "No shuttle route found"
                    isLoadingRoute = false
                }
                is ShuttleRouteResult.ApiKeyMissing -> {
                    routeError     = "Maps configuration error"
                    isLoadingRoute = false
                }
                is ShuttleRouteResult.InvalidStops -> {
                    routeError     = "Invalid stop selection"
                    isLoadingRoute = false
                }
            }
        }
    }

    fun disableShuttleMode() {
        isShuttleModeActive = false
        shuttleRoute        = null
        routeError          = null
        nearestStop         = null
        selectedStop        = null
        shuttleStatusText   = ""
        isShuttleEnabled    = false
    }
}
