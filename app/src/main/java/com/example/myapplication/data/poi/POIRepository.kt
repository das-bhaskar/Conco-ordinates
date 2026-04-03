package com.example.myapplication.data.poi

import com.google.android.gms.maps.model.LatLng

/**
 * Contract for fetching nearby Points of Interest.
 *
 * SOLID — Dependency Inversion Principle:
 *   - [POIViewModel] depends on this interface, never on [PlacesPOIRepository].
 *   - Tests inject [FakePOIRepository]; production injects [PlacesPOIRepository].
 *
 * Design Pattern — Repository:
 *   Abstracts the data source (Places API, local cache, mock) behind a
 *   single suspending call.  The ViewModel never knows where data comes from.
 */
interface POIRepository {
    /**
     * Returns POIs near [origin] within [radiusMeters], optionally filtered
     * by [category].  Results are sorted by ascending distance.
     *
     * Throws [POIException] on unrecoverable errors so callers can react
     * uniformly regardless of the underlying source.
     */
    suspend fun getNearbyPOIs(
        origin:       LatLng,
        radiusMeters: Int        = DEFAULT_RADIUS,
        category:     POICategory = POICategory.ALL
    ): List<POI>

    companion object {
        const val DEFAULT_RADIUS = 500   // metres — matches AC acceptance criteria
    }
}

/** Typed error wrapper so the ViewModel doesn't catch raw IOException. */
class POIException(message: String, cause: Throwable? = null) : Exception(message, cause)
