package com.example.myapplication.data.indoor

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.google.android.gms.maps.model.LatLng
import org.json.JSONObject

data class BuildingEntrance(
    val nodeId: String,
    val label:  String,
    val gps:    LatLng,
    val floor:  Int = 1
)

/**
 * Provides building entrance data loaded from `building_entrances.json`.
 *
 * Converted from `object` to `class` so it can be injected via the constructor
 * (Dependency Inversion Principle). Callers that previously used the global
 * singleton can still use [BuildingEntrances.default], which is initialised
 * once in `MapsActivity.onCreate` via [BuildingEntrances.initialize].
 *
 * For production code, prefer injecting a [BuildingEntrances] instance
 * rather than referencing the companion singleton directly.
 */
class BuildingEntrances(initialData: Map<String, List<BuildingEntrance>> = emptyMap()) {

    private var data: Map<String, List<BuildingEntrance>> = initialData

    /** All entrances for a building code (e.g. "CC", "H"). Case-insensitive. */
    fun forBuilding(code: String): List<BuildingEntrance> =
        data[code.uppercase()] ?: emptyList()

    /** Entrance closest to [gps] — uses Haversine via SphericalUtil for accuracy. */
    fun nearest(code: String, gps: LatLng): BuildingEntrance? =
        forBuilding(code).minByOrNull { e ->
            com.google.maps.android.SphericalUtil.computeDistanceBetween(e.gps, gps)
        }

    /** Load from raw JSON asset — used by the companion singleton path. */
    internal fun loadFrom(context: Context) {
        if (data.isNotEmpty()) return  // already loaded
        data = loadFromRaw(context)
    }

    // ── JSON parsing ──────────────────────────────────────────────────────────

    private fun loadFromRaw(context: Context): Map<String, List<BuildingEntrance>> {
        return try {
            val resId = context.resources.getIdentifier(
                "building_entrances", "raw", context.packageName
            )
            if (resId == 0) return emptyMap()

            val json = context.resources.openRawResource(resId)
                .bufferedReader()
                .use { it.readText() }

            parseJson(json)
        } catch (e: Exception) {
            android.util.Log.e("BuildingEntrances", "Failed to load: ${e.message}")
            emptyMap()
        }
    }

    @VisibleForTesting
    internal fun parseJson(json: String): Map<String, List<BuildingEntrance>> {
        val root   = JSONObject(json)
        val result = mutableMapOf<String, List<BuildingEntrance>>()
        try {
            val names = root.names() ?: return result
            for (i in 0 until names.length()) {
                val buildingCode = names.optString(i) ?: continue
                if (buildingCode.isBlank()) continue
                val arr = root.optJSONArray(buildingCode) ?: continue
                val entrances = (0 until arr.length()).mapNotNull { j ->
                    val obj = arr.optJSONObject(j) ?: return@mapNotNull null
                    BuildingEntrance(
                        nodeId = obj.optString("nodeId", ""),
                        label  = obj.optString("label", ""),
                        gps    = LatLng(obj.optDouble("lat", 0.0), obj.optDouble("lng", 0.0)),
                        floor  = obj.optInt("floor", 1)
                    )
                }
                result[buildingCode.uppercase()] = entrances
            }
        } catch (e: Exception) {
            // Return whatever was collected before the failure
        }
        return result
    }

    // ── Companion singleton (backward-compat call sites) ──────────────────────

    companion object {
        /**
         * Singleton instance used by legacy call sites.
         * Prefer constructor injection for new code.
         */
        val default: BuildingEntrances = BuildingEntrances()

        /**
         * Initialises the singleton from a Context.
         * Call once in `MapsActivity.onCreate` before any navigation.
         */
        fun initialize(context: Context) = default.loadFrom(context)

        /** Delegates to [default.forBuilding] — preserves existing call sites. */
        fun forBuilding(code: String): List<BuildingEntrance> = default.forBuilding(code)

        /** Delegates to [default.nearest] — preserves existing call sites. */
        fun nearest(code: String, gps: LatLng): BuildingEntrance? = default.nearest(code, gps)
    }
}
