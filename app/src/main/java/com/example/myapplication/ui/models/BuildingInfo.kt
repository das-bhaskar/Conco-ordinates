package com.example.myapplication.ui.models

import com.example.myapplication.data.Building
import com.example.myapplication.data.ShuttleAvailability
import com.example.myapplication.data.ShuttleStop

enum class MapUIMode {
    PREVIEW,          // Just looking at a building
    DIRECTIONS,       // Choosing start/end and travel mode
    ACTIVE_NAVIGATION
}

data class BuildingUiState(
    val isVisible: Boolean = false,
    val mode: MapUIMode = MapUIMode.PREVIEW,
    val building: Building? = null,
    val address: String? = null,
    val imageUrl: String? = null,
    val startLocationName: String = "Your position",
    val destinationName: String = "",
    val startPoint: com.google.android.gms.maps.model.LatLng? = null,
    val endPoint: com.google.android.gms.maps.model.LatLng? = null,
    val routePoints: List<com.google.android.gms.maps.model.LatLng> = emptyList(),
    val selectedTransportMode: String = "walk",
    val isSearchExpanded: Boolean = false,
    val isStartCurrentLocation: Boolean = false,
    val routeBounds: com.google.android.gms.maps.model.LatLngBounds? = null,
    val routeDuration: String = "-- min",
    val routeDistance: String = "-- m",
    val routeErrorMessage: String? = null,
    val navState: NavigationState = NavigationState(),
    // ── Shuttle (US-2.6 / US-2.7 / US-2.8) ───────────────────────────────────
    val shuttleAvailability: ShuttleAvailability = ShuttleAvailability.ScheduleUnavailable,
    val shuttleStatusMessage: String = "",
    val nearestShuttleStopName: String = "",
    val nearestShuttleStopCampus: String = "",
    val shuttleStops: List<ShuttleStop> = emptyList(),
    // ── Indoor map ────────────────────────────────────────────────────────────
    val hasIndoorMap: Boolean = false
)
