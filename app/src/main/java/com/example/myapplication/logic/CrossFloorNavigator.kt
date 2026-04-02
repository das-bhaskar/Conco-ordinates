package com.example.myapplication.logic

import com.example.myapplication.data.indoor.IndoorEdge
import com.example.myapplication.data.indoor.IndoorNode
import com.example.myapplication.data.indoor.IIndoorRepository

/**
 * Finds a path that may cross multiple floors within one building.
 *
 * Converted from object to class so [IndoorRepository] can be injected via
 * the constructor (DIP). The companion [navigate] preserves the existing
 * call-sites that pass repo as a parameter.
 *
 * Transfer method is controlled by [TransferPreference]:
 *   - ANY           → pick whichever gives the shortest total path
 *   - ELEVATOR_ONLY → accessible mode, only elevators considered
 *   - ESCALATOR     → prefer escalator, fall back to elevator
 *   - STAIRS        → prefer staircase, fall back to elevator
 *
 * Nodes are matched across floors using [IndoorNode.elevatorGroupId].
 */
class CrossFloorNavigator {

    companion object {
        const val VIA_ELEVATOR  = "elevator"
        const val VIA_ESCALATOR = "escalator"
        const val VIA_STAIRCASE = "staircase"

        /** Convenience entry-point — preserves the old object API for all callers. */
        suspend fun navigate(
            repo:         IIndoorRepository,
            building:     String,
            startFloor:   Int,
            startNodeId:  String,
            targetFloor:  Int,
            targetNodeId: String,
            preference:   TransferPreference = TransferPreference.ANY
        ): List<NavStep> = CrossFloorNavigator().navigate(
            repo, building, startFloor, startNodeId, targetFloor, targetNodeId, preference
        )
    }

    data class FloorSegment(
        val floor:    Int,
        val path:     List<IndoorNode>,
        val building: String
    )

    sealed class NavStep {
        data class Walk(val segment: FloorSegment) : NavStep()
        data class ChangeFloor(
            val fromFloor:    Int,
            val toFloor:      Int,
            val via:          String,
            val building:     String,
            val targetNodeId: String
        ) : NavStep()
    }

    /**
     * Build a cross-floor route inside one building.
     */
    suspend fun navigate(
        repo:         IIndoorRepository,
        building:     String,
        startFloor:   Int,
        startNodeId:  String,
        targetFloor:  Int,
        targetNodeId: String,
        preference:   TransferPreference = TransferPreference.ANY
    ): List<NavStep> {

        if (startFloor == targetFloor) {
            val floor = repo.getFloor(building, startFloor) ?: return emptyList()
            val path  = IndoorPathfinder.findPath(
                floor.nodes, floor.edges, startNodeId, targetNodeId,
                accessibleOnly = preference == TransferPreference.ELEVATOR_ONLY
            )
            return if (path.isEmpty()) emptyList()
            else listOf(NavStep.Walk(FloorSegment(startFloor, path, building)))
        }

        val startFloorData  = repo.getFloor(building, startFloor)  ?: return emptyList()
        val targetFloorData = repo.getFloor(building, targetFloor) ?: return emptyList()

        val targetTransferNodes = targetFloorData.nodes.filter {
            it.type in listOf("ELEVATOR", "ESCALATOR", "STAIRCASE")
        }

        val pairs = buildTransferPairs(startFloorData.nodes, targetTransferNodes, preference)
        if (pairs.isEmpty()) return emptyList()

        return findBestRoute(
            pairs  = pairs,
            params = RouteSearchParams(
                startNodes    = startFloorData.nodes,
                startEdges    = startFloorData.edges,
                targetNodes   = targetFloorData.nodes,
                targetEdges   = targetFloorData.edges,
                startNodeId   = startNodeId,
                targetNodeId  = targetNodeId,
                startFloor    = startFloor,
                targetFloor   = targetFloor,
                building      = building,
                accessibleOnly = preference == TransferPreference.ELEVATOR_ONLY
            )
        )
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private data class TransferPair(
        val startNode:  IndoorNode,
        val targetNode: IndoorNode,
        val via:        String
    )

    /**
     * Builds all valid transfer pairs, trying primary types first then fallback.
     * Extracted to reduce cognitive complexity of [navigate].
     */
    private fun buildTransferPairs(
        startNodes:          List<IndoorNode>,
        targetTransferNodes: List<IndoorNode>,
        preference:          TransferPreference
    ): List<TransferPair> {
        val primary = pairsForTypes(startNodes, targetTransferNodes, preference.primary)
        if (primary.isNotEmpty()) return primary
        return if (preference.fallback.isNotEmpty())
            pairsForTypes(startNodes, targetTransferNodes, preference.fallback)
        else emptyList()
    }

    private fun pairsForTypes(
        startNodes:          List<IndoorNode>,
        targetTransferNodes: List<IndoorNode>,
        types:               List<String>
    ): List<TransferPair> {
        val pairs = mutableListOf<TransferPair>()
        for (type in types) {
            startNodes.filter { it.type == type }.forEach { sn ->
                val groupId = sn.elevatorGroupId ?: return@forEach
                val tn = targetTransferNodes.firstOrNull {
                    it.type == type && it.elevatorGroupId == groupId
                } ?: return@forEach
                val via = when (type) {
                    "ELEVATOR"  -> VIA_ELEVATOR
                    "ESCALATOR" -> VIA_ESCALATOR
                    else        -> VIA_STAIRCASE
                }
                pairs.add(TransferPair(sn, tn, via))
            }
        }
        return pairs
    }

    /** Parameters for [findBestRoute] — groups related values to stay within param limit. */
    private data class RouteSearchParams(
        val startNodes:    List<IndoorNode>,
        val startEdges:    List<IndoorEdge>,
        val targetNodes:   List<IndoorNode>,
        val targetEdges:   List<IndoorEdge>,
        val startNodeId:   String,
        val targetNodeId:  String,
        val startFloor:    Int,
        val targetFloor:   Int,
        val building:      String,
        val accessibleOnly: Boolean
    )

    /**
     * Picks the transfer pair with the lowest total A* path cost.
     * Extracted to reduce cognitive complexity of [navigate].
     */
    private fun findBestRoute(
        pairs:  List<TransferPair>,
        params: RouteSearchParams
    ): List<NavStep> {
        var bestSteps: List<NavStep> = emptyList()
        var bestCost  = Float.MAX_VALUE

        for (pair in pairs) {
            val seg1 = IndoorPathfinder.findPath(
                params.startNodes, params.startEdges,
                params.startNodeId, pair.startNode.id, params.accessibleOnly
            )
            if (seg1.isEmpty()) continue

            val seg2 = IndoorPathfinder.findPath(
                params.targetNodes, params.targetEdges,
                pair.targetNode.id, params.targetNodeId, params.accessibleOnly
            )
            if (seg2.isEmpty()) continue

            val cost = pathCost(seg1, params.building) + pathCost(seg2, params.building)
            if (cost < bestCost) {
                bestCost  = cost
                bestSteps = listOf(
                    NavStep.Walk(FloorSegment(params.startFloor, seg1, params.building)),
                    NavStep.ChangeFloor(
                        fromFloor    = params.startFloor,
                        toFloor      = params.targetFloor,
                        via          = pair.via,
                        building     = params.building,
                        targetNodeId = pair.targetNode.id
                    ),
                    NavStep.Walk(FloorSegment(params.targetFloor, seg2, params.building))
                )
            }
        }
        return bestSteps
    }

    /**
     * Approximates the real-world path length in metres by scaling normalized
     * 0–1 coordinates by the building's footprint dimensions.
     * This avoids skewed costs when buildings have non-square aspect ratios.
     */
    private fun pathCost(path: List<IndoorNode>, building: String): Float {
        val dims = com.example.myapplication.data.indoor.IndoorBuildingConfig.dimsFor(building)
        var cost = 0f
        for (i in 0 until path.size - 1) {
            val a = path[i]; val b = path[i + 1]
            val dx = (a.x - b.x) * dims.widthM
            val dy = (a.y - b.y) * dims.heightM
            cost += kotlin.math.sqrt(dx * dx + dy * dy)
        }
        return cost
    }
}
