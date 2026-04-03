package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.myapplication.data.Campus
import com.example.myapplication.data.poi.POI
import com.example.myapplication.ui.theme.ConcordiaMaroon
import com.example.myapplication.ui.theme.concordiaGold
import com.example.myapplication.ui.viewmodel.MapViewModel
import com.google.maps.android.compose.*
import com.example.myapplication.logic.MapInteractionHandler
import com.example.myapplication.ui.theme.ConcordiaGreen
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.models.POIUiState
import com.example.myapplication.ui.models.MapUIMode

@Composable
fun CampusMap(
    cameraPositionState: CameraPositionState,
    state: CampusMapState,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel,
    poiBindings: CampusMapPoiBindings
) {
    val context = LocalContext.current

    GoogleMap(
        modifier = modifier.fillMaxSize().testTag("google_map"),
        cameraPositionState = cameraPositionState,
        contentPadding = state.contentPadding,
        properties = MapProperties(isMyLocationEnabled = state.hasLocationPermission),
        uiSettings = MapUiSettings(
            myLocationButtonEnabled = false,
            zoomControlsEnabled = false
        ),
        onMapClick = { latLng -> MapInteractionHandler.processClick(latLng, viewModel, context) }
    ) {
        NavigationCameraHandler(
            cameraPositionState = cameraPositionState,
            isNavigating = viewModel.uiBuildingState.mode == MapUIMode.ACTIVE_NAVIGATION,
            onManualMove = { viewModel.toggleAutoCenter(false) }
        )

        // 1. THE ROUTE LINE
        val segments = viewModel.uiBuildingState.routeSegments

        if (segments.isNotEmpty()) {
            segments.forEach { segment ->
                Polyline(
                    points = segment.points,
                    color = if (segment.mode == "shuttle") ConcordiaMaroon else com.example.myapplication.ui.theme.ConcordiaBlue,
                    width = 12f,
                    jointType = com.google.android.gms.maps.model.JointType.ROUND,
                    pattern = if (segment.mode == "walk") {
                        listOf(com.google.android.gms.maps.model.Dash(20f), com.google.android.gms.maps.model.Gap(20f))
                    } else null
                )
            }
        } else if (viewModel.uiBuildingState.routePoints.isNotEmpty()) {
            val transportMode = viewModel.uiBuildingState.selectedTransportMode
            Polyline(
                points = viewModel.uiBuildingState.routePoints,
                color = com.example.myapplication.ui.theme.ConcordiaBlue,
                width = 12f,
                jointType = com.google.android.gms.maps.model.JointType.ROUND,
                pattern = if (transportMode == "walk") {
                    listOf(com.google.android.gms.maps.model.Dash(20f), com.google.android.gms.maps.model.Gap(20f))
                } else null
            )
        }

        // 2. THE BUILDING POLYGONS
        com.example.myapplication.data.CampusRepo.getAllCampuses().forEach { campus ->
            campus.buildings.forEach { building ->
                val points = building.getGoogleOutline()

                val isDestinationBuilding = viewModel.uiBuildingState.mode == com.example.myapplication.ui.models.MapUIMode.DIRECTIONS
                        && building.name == viewModel.uiBuildingState.destinationName
                val isSelected    = viewModel.uiBuildingState.building?.name == building.name
                val isHighlighted = building.name == state.highlightedBuildingName

                if (points.isNotEmpty()) {
                    Polygon(
                        points = points,
                        fillColor = when {
                            isDestinationBuilding -> ConcordiaGreen.copy(alpha = 0.3f)
                            isSelected            -> com.example.myapplication.ui.theme.ConcordiaBlue.copy(alpha = 0.5f)
                            isHighlighted         -> concordiaGold.copy(alpha = 0.5f)
                            else                  -> ConcordiaMaroon.copy(alpha = 0.3f)
                        },
                        strokeColor = when {
                            isDestinationBuilding -> ConcordiaGreen.copy(alpha = 0.5f)
                            isSelected            -> com.example.myapplication.ui.theme.ConcordiaBlue
                            isHighlighted         -> concordiaGold
                            else                  -> ConcordiaMaroon
                        },
                        strokeWidth = if (isSelected || isHighlighted || isDestinationBuilding) 10f else 5f
                    )
                }
            }
        }

        // 3. POI MARKERS
        POIMarkers(
            uiState       = poiBindings.uiState,
            onMarkerClick = poiBindings.onPoiSelected
        )

        // 4. NAVIGATION MARKERS
        if (viewModel.uiBuildingState.mode == MapUIMode.DIRECTIONS) {
            viewModel.uiBuildingState.startPoint?.let { startPos ->
                Marker(
                    state = MarkerState(position = startPos),
                    title = "Start: ${viewModel.uiBuildingState.startLocationName}",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
            }
            viewModel.uiBuildingState.endPoint?.let { endPos ->
                Marker(
                    state = MarkerState(position = endPos),
                    title = "Destination: ${viewModel.uiBuildingState.destinationName}",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )
            }
        }
    }
}

data class CampusMapState(
    val highlightedBuildingName: String?,
    val hasLocationPermission: Boolean,
    val contentPadding: PaddingValues = PaddingValues(0.dp)
)

data class CampusMapPoiBindings(
    val uiState: POIUiState,
    val onPoiSelected: (POI) -> Unit
)
