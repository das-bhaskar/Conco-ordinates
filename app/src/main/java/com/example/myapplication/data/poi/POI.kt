package com.example.myapplication.data.poi

import com.google.android.gms.maps.model.LatLng

/**
 * Immutable value object representing a single outdoor Point of Interest.
 *
 * Kept as a pure data class (no Android dependencies) so it's trivially
 * testable and serialisable without a Robolectric environment.
 *
 * SOLID: Single Responsibility — holds POI data only, zero behaviour.
 */
data class POI(
    val placeId:  String,
    val name:     String,
    val address:  String,
    val category: POICategory,
    val latLng:   LatLng,
    val distanceMeters: Int          // pre-computed by repository, always >= 0
)

/**
 * Exhaustive set of POI categories the app supports.
 *
 * Each entry carries:
 *  - [placesType]   the Google Places API "type" filter string
 *  - [label]        human-readable chip label
 *  - [emoji]        icon shown on map markers and list rows
 *
 * Open/Closed Principle: adding a new category only requires a new enum
 * entry — no when-chains need updating in business logic.
 */
enum class POICategory(
    val placesType: String,
    val label:      String,
    val emoji:      String
) {
    ALL(         placesType = "",           label = "All",       emoji = "📍"),
    CAFE(        placesType = "cafe",       label = "Cafe",      emoji = "☕"),
    RESTAURANT(  placesType = "restaurant", label = "Restaurant",emoji = "🍽"),
    PHARMACY(    placesType = "pharmacy",   label = "Pharmacy",  emoji = "💊"),
    GROCERY(     placesType = "grocery_or_supermarket",
                                            label = "Grocery",   emoji = "🛒"),
    GYM(         placesType = "gym",        label = "Gym",       emoji = "🏋️"),
    ATM(         placesType = "atm",        label = "ATM",       emoji = "🏧")
}
