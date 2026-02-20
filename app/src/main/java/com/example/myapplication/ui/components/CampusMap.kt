package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.myapplication.data.Campus
import com.example.myapplication.ui.theme.ConcordiaMaroon
import com.example.myapplication.ui.theme.concordiaGold
import com.google.maps.android.compose.*

@Composable
fun CampusMap(
    currentCampus: Campus?,
    highlightedBuildingName: String?, // NEW PARAMETER
    cameraPositionState: CameraPositionState,
    hasLocationPermission: Boolean
) {
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = hasLocationPermission)
    ) {
        currentCampus?.buildings?.forEach { building ->
            val points = building.getGoogleOutline()
            val isHighlighted = building.name == highlightedBuildingName

            if (points.isNotEmpty()) {
                Polygon(
                    points = points,
                    // Use Gold if highlighted, otherwise Maroon
                    fillColor = if (isHighlighted) concordiaGold.copy(alpha = 0.5f)
                    else ConcordiaMaroon.copy(alpha = 0.3f),
                    strokeColor = if (isHighlighted) concordiaGold else ConcordiaMaroon,
                    strokeWidth = if (isHighlighted) 10f else 5f
                )
            }
        }
    }
}