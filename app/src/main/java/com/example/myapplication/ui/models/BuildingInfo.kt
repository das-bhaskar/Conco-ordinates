package com.example.myapplication.ui.models

import com.example.myapplication.data.Building

enum class MapUIMode {
    PREVIEW,    // Just looking at a building
    DIRECTIONS  // Choosing start/end and travel mode
}

data class BuildingUiState(
    val isVisible: Boolean = false,
    val mode: MapUIMode = MapUIMode.PREVIEW,
    val building: Building? = null,
    val fullAddress: String? = null,
    val imageUrl: String? = null,
    val startLocationName: String = "Your position", // Default start
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
)