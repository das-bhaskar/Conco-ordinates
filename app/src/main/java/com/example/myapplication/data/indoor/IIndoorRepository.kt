package com.example.myapplication.data.indoor

/**
 * Abstraction over indoor floor-plan data loading.
 *
 * Decouples logic-layer classes ([CrossFloorNavigator], [IndoorOutdoorRouter],
 * [IndoorRoomResolver]) and ViewModels from the Android-specific
 * [IndoorRepository] implementation, enabling pure-JVM unit tests via a
 * simple fake/mock that implements this interface directly.
 */
interface IIndoorRepository {
    /**
     * Returns the [IndoorFloor] for [building] and [floor], or null if
     * no map data is available (e.g. missing raw JSON resource).
     */
    suspend fun getFloor(building: String, floor: Int): IndoorFloor?

    /** Clears the in-memory cache so the next [getFloor] call re-loads from disk. */
    fun clearCache()
}
