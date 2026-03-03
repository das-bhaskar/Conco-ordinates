package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.data.Campus
import com.example.myapplication.data.ShuttleRoute
import com.example.myapplication.data.ShuttleStop
import com.example.myapplication.ui.theme.ConcordiaMaroon
import com.example.myapplication.ui.theme.concordiaGold
import com.example.myapplication.ui.viewmodel.MapViewModel
import com.google.maps.android.compose.*
import com.example.myapplication.logic.MapInteractionHandler
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng

@Composable
fun CampusMap(
    currentCampus:           Campus?,
    highlightedBuildingName: String?,
    cameraPositionState:     CameraPositionState,
    hasLocationPermission:   Boolean,
    viewModel:               MapViewModel,
    contentPadding:          PaddingValues = PaddingValues(),
    modifier:                Modifier = Modifier,
    shuttleRoute:            ShuttleRoute? = null,
    nearestStop:             ShuttleStop? = null,
    routePoints:             List<LatLng> = emptyList()
) {
    val context = LocalContext.current

    GoogleMap(
        modifier            = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties          = MapProperties(isMyLocationEnabled = hasLocationPermission),
        uiSettings          = MapUiSettings(
            myLocationButtonEnabled = false,
            zoomControlsEnabled     = false
        ),
        contentPadding = contentPadding,
        onMapClick     = { latLng -> MapInteractionHandler.processClick(latLng, viewModel, context) }
    ) {
        // ── Building polygons ─────────────────────────────────────────────
        currentCampus?.buildings?.forEach { building ->
            val points        = building.getGoogleOutline()
            val isHighlighted = building.name == highlightedBuildingName

            if (points.isNotEmpty()) {
                Polygon(
                    points      = points,
                    fillColor   = if (isHighlighted) concordiaGold.copy(alpha = 0.5f)
                                  else ConcordiaMaroon.copy(alpha = 0.3f),
                    strokeColor = if (isHighlighted) concordiaGold else ConcordiaMaroon,
                    strokeWidth = if (isHighlighted) 10f else 5f
                )
            }
        }

        // ── Directions route polyline (walk/drive/transit) ────────────────
        if (routePoints.isNotEmpty()) {
            Polyline(
                points   = routePoints,
                color    = Color(0xFF1A73E8),   // Google blue
                width    = 12f,
                geodesic = true
            )
        }

        // ── Task-2.8.3: Nearest Stop Marker ───────────────────────────────
        nearestStop?.let { stop ->
            Marker(
                state   = MarkerState(position = stop.location),
                title   = "Nearest Stop: ${stop.name}",
                icon    = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
            )
        }

        // ── Task-2.6.2: Render shuttle route ──────────────────────────────
        shuttleRoute?.let { route ->
            if (route.polylinePoints.isNotEmpty()) {
                Polyline(
                    points   = route.polylinePoints,
                    color    = Color(0xFF912338),   // Concordia Maroon
                    width    = 14f,
                    geodesic = true
                )
                Marker(
                    state   = MarkerState(position = route.boardingStop.location),
                    title   = "🚌 ${route.boardingStop.name}",
                    snippet = "Board here",
                    icon    = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
                Marker(
                    state   = MarkerState(position = route.alightingStop.location),
                    title   = "🚌 ${route.alightingStop.name}",
                    snippet = "Alight here",
                    icon    = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)
                )
            }
        }
    }
}
