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

    /** Places API returned results successfully and the user is browsing them. */
    data class Browse(
        val pois:             List<POI>,
        val selectedCategory: POICategory = POICategory.ALL
    ) : POIUiState()

    /** A POI is selected and the action card is visible. */
    data class Selection(
        val pois:             List<POI>,
        val selectedCategory: POICategory = POICategory.ALL,
        val selectedPOI:      POI
    ) : POIUiState()

    /** Places API call failed. */
    data class Error(val message: String) : POIUiState()

    /** API returned no results for the current filter + location. */
    data object Empty : POIUiState()
}
