package com.example.myapplication.data.poi

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import kotlin.math.*

/**
 * Production implementation of [POIRepository] using the Google Places
 * Nearby Search REST API (v1).
 *
 * WHY REST instead of Places SDK:
 *   [SearchNearbyRequest] is only available in Places SDK 3.5+.
 *   Using the REST endpoint works with any SDK version and requires
 *   only the existing MAPS_API_KEY — no additional dependency changes.
 *
 * SOLID — Single Responsibility: translates HTTP JSON → [POI] domain objects.
 * SOLID — Open/Closed: new [POICategory] entries are handled via the enum,
 *   no branching needed here.
 * Design Pattern — Adapter: maps raw JSON fields into typed [POI] objects.
 */
class PlacesPOIRepository(
    private val apiKey: String
) : POIRepository {

    override suspend fun getNearbyPOIs(
        origin:       LatLng,
        radiusMeters: Int,
        category:     POICategory
    ): List<POI> = withContext(Dispatchers.IO) {
        val types = if (category == POICategory.ALL) {
            POICategory.entries
                .filter { it != POICategory.ALL }
                .joinToString("|") { it.placesType }
        } else {
            category.placesType
        }

        val url = buildUrl(origin, radiusMeters, types)

        try {
            val response = URL(url).readText()
            parseResponse(response, origin, category)
        } catch (e: Exception) {
            throw POIException("Nearby Search failed: ${e.message}", e)
        }
    }

    // ── URL builder ────────────────────────────────────────────────────────

    private fun buildUrl(origin: LatLng, radius: Int, types: String): String {
        val base = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
        return "$base" +
            "?location=${origin.latitude},${origin.longitude}" +
            "&radius=$radius" +
            "&type=$types" +
            "&key=$apiKey"
    }

    // ── JSON → POI adapter ─────────────────────────────────────────────────

    private fun parseResponse(
        json:             String,
        origin:           LatLng,
        requestedCategory: POICategory
    ): List<POI> {
        val root    = JSONObject(json)
        val status  = root.optString("status")

        if (status == "ZERO_RESULTS") return emptyList()
        if (status != "OK") throw POIException("Places API error: $status")

        val results = root.optJSONArray("results") ?: return emptyList()

        return (0 until results.length())
            .mapNotNull { i -> results.getJSONObject(i).toPOI(origin, requestedCategory) }
            .sortedBy { it.distanceMeters }
            .take(20)
    }

    private fun JSONObject.toPOI(origin: LatLng, requestedCategory: POICategory): POI? {
        val placeId = optString("place_id").takeIf { it.isNotBlank() } ?: return null
        val name    = optString("name").takeIf    { it.isNotBlank() } ?: return null
        val address = optString("vicinity", "")

        val locationObj = optJSONObject("geometry")?.optJSONObject("location") ?: return null
        val lat    = locationObj.optDouble("lat", Double.NaN)
        val lng    = locationObj.optDouble("lng", Double.NaN)
        if (lat.isNaN() || lng.isNaN()) return null

        val latLng   = LatLng(lat, lng)
        val distance = haversineMeters(origin, latLng).toInt()

        // Infer category from the place's "types" array if ALL was requested
        val category = if (requestedCategory != POICategory.ALL) {
            requestedCategory
        } else {
            val typesArray = optJSONArray("types")
            val typesList  = (0 until (typesArray?.length() ?: 0))
                .map { typesArray!!.getString(it) }
            POICategory.entries.firstOrNull { cat ->
                cat != POICategory.ALL && typesList.contains(cat.placesType)
            } ?: POICategory.RESTAURANT
        }

        return POI(
            placeId        = placeId,
            name           = name,
            address        = address,
            category       = category,
            latLng         = latLng,
            distanceMeters = distance
        )
    }

    // ── Haversine distance ─────────────────────────────────────────────────

    private fun haversineMeters(a: LatLng, b: LatLng): Double {
        val r  = 6_371_000.0
        val φ1 = Math.toRadians(a.latitude)
        val φ2 = Math.toRadians(b.latitude)
        val Δφ = Math.toRadians(b.latitude  - a.latitude)
        val Δλ = Math.toRadians(b.longitude - a.longitude)
        val h  = sin(Δφ / 2).pow(2) + cos(φ1) * cos(φ2) * sin(Δλ / 2).pow(2)
        return 2 * r * asin(sqrt(h))
    }
}
