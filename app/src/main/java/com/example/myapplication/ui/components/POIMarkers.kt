package com.example.myapplication.ui.components

import androidx.compose.runtime.Composable
import com.example.myapplication.data.poi.POI
import com.example.myapplication.logic.formatDistance
import com.example.myapplication.logic.poiMarkerHue
import com.example.myapplication.ui.models.POIUiState
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.MarkerInfoWindowContent
import com.google.maps.android.compose.rememberMarkerState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Renders POI markers on the [GoogleMap] composable.
 *
 * Must be called **inside** the [GoogleMap] content lambda — exactly like
 * existing [Marker] calls in [CampusMap].  Zero changes to CampusMap required.
 *
 * Design Pattern — Composite:
 *   Composes multiple [MarkerInfoWindowContent] calls transparently.
 *   The parent GoogleMap doesn't need to know how many markers are drawn.
 *
 * SOLID — Open/Closed:
 *   New POI categories get their own hue automatically via [categoryHue];
 *   no branching needed.
 */
@Composable
fun POIMarkers(
    uiState: POIUiState,
    onMarkerClick: (POI) -> Unit
) {
    val pois = uiState.poisOrEmpty()
    if (pois.isEmpty()) return
    val selectedPlaceId = uiState.selectedPlaceId()

    pois.forEach { poi ->
        val markerState = rememberMarkerState(
            key      = poi.placeId,
            position = poi.latLng
        )
        val isSelected = poi.placeId == selectedPlaceId

        MarkerInfoWindowContent(
            state   = markerState,
            title   = poi.name,
            snippet = poi.address,
            icon    = BitmapDescriptorFactory.defaultMarker(poiMarkerHue(poi.category)),
            alpha   = if (isSelected) 1.0f else 0.85f,
            onClick = {
                onMarkerClick(poi)
                false  // return false = do NOT consume; lets the default info window show
            }
        ) {
            // Custom info window content
            Card(elevation = CardDefaults.cardElevation(4.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text       = "${poi.category.emoji} ${poi.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 13.sp
                    )
                    Text(
                        text     = formatDistance(poi.distanceMeters),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private fun POIUiState.poisOrEmpty(): List<POI> = when (this) {
    is POIUiState.Browse -> pois
    is POIUiState.Selection -> pois
    else -> emptyList()
}

private fun POIUiState.selectedPlaceId(): String? = when (this) {
    is POIUiState.Selection -> selectedPOI.placeId
    else -> null
}
