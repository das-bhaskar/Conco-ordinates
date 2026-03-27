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

    /** Returns the available floor numbers for [buildingCode], or empty list if unknown. */
    fun floorsFor(buildingCode: String): List<Int> =
        floorMap[buildingCode.uppercase()] ?: emptyList()

    /** Returns true if we have indoor map data for [buildingCode]. */
    fun hasIndoorMap(buildingCode: String): Boolean =
        floorMap.containsKey(buildingCode.uppercase())

    /** All building codes that have indoor map data. */
    val allCodes: Set<String> get() = floorMap.keys
}
