package com.example.myapplication.logic

import com.example.myapplication.data.indoor.IndoorRepository
import com.example.myapplication.data.indoor.IndoorNode

/**
 * Resolves a user-typed room label (e.g. "CC-119") to a nav node ID
 * by searching the indoor JSON for that building/floor.
 *
 * Returns null if no matching room or node is found.
 */
object IndoorRoomResolver {

    data class ResolvedRoom(
        val buildingCode: String,
        val floor:        Int,
        val roomId:       String,
        val nodeId:       String,
        val label:        String
    )

    /**
     * Try to resolve [query] (e.g. "CC-119", "119", "H-829") inside
     * [buildingCode] by scanning all known floors.
     */
    suspend fun resolve(
        repo:         IndoorRepository,
        buildingCode: String,
        query:        String,
        floors:       List<Int> = floorsFor(buildingCode)
    ): ResolvedRoom? {
        val q = query.trim().uppercase()

        for (floor in floors) {
            val floorData = repo.getFloor(buildingCode, floor) ?: continue

            // Match by room label or room id suffix
            val room = floorData.rooms.firstOrNull { room ->
                val label = room.label.uppercase()
                val id    = room.id.uppercase()
                label == q ||
                label.endsWith("-$q") ||
                label.endsWith(q) ||
                id.endsWith("-$q") ||
                id.endsWith(q)
            } ?: continue

            // Find the nav node linked to this room
            val node = floorData.nodes.firstOrNull { it.roomId == room.id }
                ?: floorData.nodes.minByOrNull { n ->
                    // fallback: node closest to room polygon centroid
                    val cx = room.polygon.map { it.x }.average().toFloat()
                    val cy = room.polygon.map { it.y }.average().toFloat()
                    val dx = n.x - cx; val dy = n.y - cy
                    dx * dx + dy * dy
                } ?: continue

            return ResolvedRoom(
                buildingCode = buildingCode,
                floor        = floor,
                roomId       = room.id,
                nodeId       = node.id,
                label        = room.label
            )
        }
        return null
    }

    /** Resolve the entrance node for a building (used when user picks "Building Entrance"). */
    fun resolveEntrance(
        repo:         IndoorRepository,
        buildingCode: String,
        floor:        Int = 1
    ): String? = null  // filled in at runtime from IndoorRepository

    private fun floorsFor(code: String): List<Int> =
        com.example.myapplication.data.indoor.IndoorBuildingConfig.floorsFor(code)
}
