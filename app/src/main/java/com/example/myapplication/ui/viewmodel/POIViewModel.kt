package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.poi.POI
import com.example.myapplication.data.poi.POICategory
import com.example.myapplication.data.poi.POIException
import com.example.myapplication.data.poi.POIRepository
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
    private var currentRadiusMeters: Int = POIRepository.DEFAULT_RADIUS

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
        fetchPOIs(origin, category, radiusMeters = currentRadiusMeters)
    }

    /** Tap on a POI row — switches from browse state to selected state. */
    fun onPOISelected(poi: POI) {
        when (val current = _uiState.value) {
            is POIUiState.Browse -> {
                _uiState.value = POIUiState.Selection(
                    pois = current.pois,
                    selectedCategory = current.selectedCategory,
                    selectedPOI = poi
                )
            }
            is POIUiState.Selection -> {
                _uiState.value = current.copy(selectedPOI = poi)
            }
            else -> Unit
        }
    }

    /** Dismisses the selected POI action panel, returns to list. */
    fun onPOIDismissed() {
        val current = _uiState.value as? POIUiState.Selection ?: return
        _uiState.value = POIUiState.Browse(
            pois = current.pois,
            selectedCategory = current.selectedCategory
        )
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private fun fetchPOIs(
        origin:       LatLng,
        category:     POICategory,
        radiusMeters: Int = POIRepository.DEFAULT_RADIUS
    ) {
        _uiState.value = POIUiState.Loading
        currentRadiusMeters = radiusMeters

        viewModelScope.launch {
            try {
                val results = repository.getNearbyPOIs(origin, radiusMeters, category)
                _uiState.value = if (results.isEmpty()) {
                    POIUiState.Empty
                } else {
                    POIUiState.Browse(
                        pois               = results,
                        selectedCategory   = category
                    )
                }
            } catch (e: POIException) {
                _uiState.value = POIUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // ── Factory ────────────────────────────────────────────────────────────

    /**
     * Design Pattern — Factory Method:
     *   Takes a [POIRepository] so tests can inject fakes without changing
     *   the ViewModel or relying on production networking code.
     *
     * Usage in your Activity/NavHost:
     *   val poiViewModel: POIViewModel by viewModels {
     *       POIViewModel.Factory(repository = PlacesPOIRepository(BuildConfig.MAPS_API_KEY))
     *   }
     */
    class Factory(private val repository: POIRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            POIViewModel(repository) as T
    }
}
