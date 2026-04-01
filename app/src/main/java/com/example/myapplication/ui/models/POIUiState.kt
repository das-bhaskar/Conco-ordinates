package com.example.myapplication.ui.models

import com.example.myapplication.data.poi.POI
import com.example.myapplication.data.poi.POICategory

/**
 * Sealed hierarchy representing every possible state of the POI panel.
 *
 * Design Pattern — State:
 *   The UI composable switches on this sealed class instead of managing
 *   multiple boolean flags (isLoading, isError, isEmpty…).  Each state
 *   carries exactly the data it needs — no nullable landmines.
 *
 * SOLID — Open/Closed: new UI states (e.g. Offline) are new subclasses,
 *   not mutations of existing ones.
 */
sealed class POIUiState {

    /** Panel is hidden — user hasn't opened POI mode yet. */
    data object Hidden : POIUiState()

    /** Waiting for Places API response. */
    data object Loading : POIUiState()

    /** Places API returned results successfully. */
    data class Success(
        val pois:             List<POI>,
        val selectedCategory: POICategory = POICategory.ALL,
        val selectedPOI:      POI?        = null,
        val searchRadiusMeters: Int       = 500
    ) : POIUiState()

    /** Places API call failed. */
    data class Error(val message: String) : POIUiState()

    /** API returned no results for the current filter + location. */
    data object Empty : POIUiState()
}
