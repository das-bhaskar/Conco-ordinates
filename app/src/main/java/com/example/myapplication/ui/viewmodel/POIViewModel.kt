package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.poi.POI
import com.example.myapplication.data.poi.POICategory
import com.example.myapplication.data.poi.POIException
import com.example.myapplication.data.poi.POIRepository
import com.example.myapplication.data.poi.PlacesPOIRepository
import com.example.myapplication.ui.models.POIUiState
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the outdoor POI feature (Epic 5).
 *
 * SOLID — Single Responsibility:
 *   Owns only POI state. Routing state stays in [MapViewModel].
 *
 * SOLID — Dependency Inversion:
 *   Depends on [POIRepository] interface, never on [PlacesPOIRepository]
 *   directly. The factory injects the concrete impl in production.
 *
 * Design Pattern — Observer (via StateFlow):
 *   The composable layer collects [uiState] reactively; no polling needed.
 */
class POIViewModel(
    private val repository: POIRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<POIUiState>(POIUiState.Hidden)
    val uiState: StateFlow<POIUiState> = _uiState.asStateFlow()

    private var lastOrigin: LatLng? = null

    // ── Public API ─────────────────────────────────────────────────────────

    /** Called by the Explore FAB. Triggers first fetch if we have a location. */
    fun openPOIPanel() {
        if (_uiState.value is POIUiState.Hidden) {
            val origin = lastOrigin
            if (origin != null) fetchPOIs(origin, POICategory.ALL)
            else _uiState.value = POIUiState.Loading   // wait for location
        }
    }

    /** Collapses the POI panel back to Hidden (e.g. user taps ✕). */
    fun closePOIPanel() {
        _uiState.value = POIUiState.Hidden
    }

    /**
     * Called whenever the user's location changes — piggybacked on the
     * existing [MapScreen] location callback, no new permission needed.
     */
    fun onLocationUpdated(origin: LatLng) {
        lastOrigin = origin
        if (_uiState.value is POIUiState.Loading) {
            fetchPOIs(origin, POICategory.ALL)
        }
    }

    /** Switches category filter chip and re-fetches. */
    fun onCategorySelected(category: POICategory) {
        val origin = lastOrigin ?: return
        val radius = currentRadius()
        fetchPOIs(origin, category, radiusMeters = radius)
    }

    /** Tap on a POI row — updates selectedPOI inside Success state. */
    fun onPOISelected(poi: POI) {
        val current = _uiState.value as? POIUiState.Success ?: return
        _uiState.value = current.copy(selectedPOI = poi)
    }

    /** Dismisses the selected POI action panel, returns to list. */
    fun onPOIDismissed() {
        val current = _uiState.value as? POIUiState.Success ?: return
        _uiState.value = current.copy(selectedPOI = null)
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private fun fetchPOIs(
        origin:       LatLng,
        category:     POICategory,
        radiusMeters: Int = POIRepository.DEFAULT_RADIUS
    ) {
        _uiState.value = POIUiState.Loading

        viewModelScope.launch {
            try {
                val results = repository.getNearbyPOIs(origin, radiusMeters, category)
                _uiState.value = if (results.isEmpty()) {
                    POIUiState.Empty
                } else {
                    POIUiState.Success(
                        pois               = results,
                        selectedCategory   = category,
                        searchRadiusMeters = radiusMeters
                    )
                }
            } catch (e: POIException) {
                _uiState.value = POIUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun currentRadius(): Int =
        (_uiState.value as? POIUiState.Success)?.searchRadiusMeters
            ?: POIRepository.DEFAULT_RADIUS

    // ── Factory ────────────────────────────────────────────────────────────

    /**
     * Design Pattern — Factory Method:
     *   Takes [apiKey] (your MAPS_API_KEY) instead of PlacesClient,
     *   since the REST implementation no longer needs the SDK client.
     *
     * Usage in your Activity/NavHost:
     *   val poiViewModel: POIViewModel by viewModels {
     *       POIViewModel.Factory(apiKey = BuildConfig.MAPS_API_KEY)
     *   }
     */
    class Factory(private val apiKey: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            POIViewModel(PlacesPOIRepository(apiKey)) as T
    }
}
