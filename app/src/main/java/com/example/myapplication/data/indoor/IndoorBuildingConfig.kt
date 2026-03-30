package com.example.myapplication.data.indoor

/**
 * Single source of truth for which buildings have indoor map data
 * and which floors are available for each.
 *
 * Previously this map was duplicated in three places:
 *   - SearchProvider.floorsFor()
 *   - IndoorRoomResolver.floorsFor()
 *   - MapsActivity.floorsFor()
 *
 * Adding a new building or floor now requires a change in exactly one place.
 * OCP: open for extension (add a building code), closed for modification of callers.
 */
object IndoorBuildingConfig {

    private val floorMap: Map<String, List<Int>> = mapOf(
        "CC" to listOf(1),
        "H"  to listOf(1, 2, 8, 9),
        "MB" to listOf(1, -2),
        "VE" to listOf(1, 2),
        "VL" to listOf(1, 2)
    )

    /**
     * Approximate building footprint dimensions in metres (width × height).
     * Used by [IndoorPathfinder] and [CrossFloorNavigator] to convert
     * normalized 0–1 coordinates into real-world distances so that path
     * costs are comparable across buildings with different aspect ratios.
     *
     * Values are rough estimates from satellite imagery; sufficient for
     * relative path ranking (which route is shorter), not turn-by-turn
     * distance display.
     */
    data class BuildingDims(val widthM: Float, val heightM: Float)

    private val dimsMap: Map<String, BuildingDims> = mapOf(
        "H"  to BuildingDims(70f, 120f),
        "CC" to BuildingDims(80f,  90f),
        "MB" to BuildingDims(60f,  80f),
        "VE" to BuildingDims(40f,  60f),
        "VL" to BuildingDims(40f,  60f)
    )

    /** Default dims used when the building code is unknown. */
    private val defaultDims = BuildingDims(60f, 80f)

    /** Returns the approximate footprint for [buildingCode]. */
    fun dimsFor(buildingCode: String): BuildingDims =
        dimsMap[buildingCode.uppercase()] ?: defaultDims

    /** Returns the available floor numbers for [buildingCode], or empty list if unknown. */
    fun floorsFor(buildingCode: String): List<Int> =
        floorMap[buildingCode.uppercase()] ?: emptyList()

    /** Returns true if we have indoor map data for [buildingCode]. */
    fun hasIndoorMap(buildingCode: String): Boolean =
        floorMap.containsKey(buildingCode.uppercase())

    /** All building codes that have indoor map data. */
    val allCodes: Set<String> get() = floorMap.keys
}
