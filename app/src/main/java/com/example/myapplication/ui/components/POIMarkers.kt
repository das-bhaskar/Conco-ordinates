package com.example.myapplication.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.myapplication.data.poi.POI
import com.example.myapplication.ui.models.POIUiState
import com.example.myapplication.ui.viewmodel.POIViewModel
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
    poiViewModel: POIViewModel,
    onMarkerClick: (POI) -> Unit
) {
    val uiState by poiViewModel.uiState.collectAsState()
    val pois    = (uiState as? POIUiState.Success)?.pois ?: return
    val selectedPOI = (uiState as? POIUiState.Success)?.selectedPOI

    pois.forEach { poi ->
        val markerState = rememberMarkerState(
            key      = poi.placeId,
            position = poi.latLng
        )
        val isSelected = poi.placeId == selectedPOI?.placeId

        MarkerInfoWindowContent(
            state   = markerState,
            title   = poi.name,
            snippet = poi.address,
            icon    = BitmapDescriptorFactory.defaultMarker(poi.category.categoryHue()),
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

// Maps each POICategory to a distinct Google Maps marker hue
private fun com.example.myapplication.data.poi.POICategory.categoryHue(): Float =
    when (this) {
        com.example.myapplication.data.poi.POICategory.ALL        -> BitmapDescriptorFactory.HUE_RED
        com.example.myapplication.data.poi.POICategory.CAFE       -> BitmapDescriptorFactory.HUE_ORANGE
        com.example.myapplication.data.poi.POICategory.RESTAURANT -> BitmapDescriptorFactory.HUE_ROSE
        com.example.myapplication.data.poi.POICategory.PHARMACY   -> BitmapDescriptorFactory.HUE_GREEN
        com.example.myapplication.data.poi.POICategory.GROCERY    -> BitmapDescriptorFactory.HUE_YELLOW
        com.example.myapplication.data.poi.POICategory.GYM        -> BitmapDescriptorFactory.HUE_VIOLET
        com.example.myapplication.data.poi.POICategory.ATM        -> BitmapDescriptorFactory.HUE_AZURE
    }

private fun formatDistance(meters: Int): String = when {
    meters < 1000 -> "$meters m"
    else          -> "${"%.1f".format(meters / 1000.0)} km"
}
