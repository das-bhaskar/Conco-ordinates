package com.example.myapplication.logic

import com.example.myapplication.data.indoor.IndoorBuildingConfig
import com.example.myapplication.data.indoor.IndoorEdge
import com.example.myapplication.data.indoor.IndoorNode
import java.util.PriorityQueue
import kotlin.math.sqrt

object IndoorPathfinder {

    /**
     * A* pathfinding on a normalized indoor nav graph.
     *
     * @param nodes          all nodes for the floor
     * @param edges          all edges for the floor (treated as bidirectional)
     * @param startId        node id of the starting point
     * @param endId          node id of the destination
     * @param accessibleOnly if true, skip edges where accessible=false
     * @param building       building code used to scale normalized coords to real metres.
     *                       Defaults to "" which falls back to [IndoorBuildingConfig.defaultDims].
     * @return ordered list of nodes from start to end, empty if no path found
     */
    fun findPath(
        nodes:          List<IndoorNode>,
        edges:          List<IndoorEdge>,
        startId:        String,
        endId:          String,
        accessibleOnly: Boolean = false,
        building:       String  = ""
    ): List<IndoorNode> {
        if (startId == endId) return listOf(nodes.first { it.id == startId })

        val nodeMap = nodes.associateBy { it.id }
        val goal    = nodeMap[endId] ?: return emptyList()
        if (nodeMap[startId] == null) return emptyList()

        val dims = IndoorBuildingConfig.dimsFor(building)
        val adj  = buildAdjacencyList(edges, accessibleOnly)

        val gScore   = HashMap<String, Float>().apply { put(startId, 0f) }
        val fScore   = HashMap<String, Float>().apply { put(startId, h(nodeMap[startId]!!, goal, dims)) }
        val cameFrom = HashMap<String, String>()
        val closed   = HashSet<String>()
        val openSet  = PriorityQueue<String>(compareBy { fScore[it] ?: Float.MAX_VALUE })
        openSet.add(startId)

        while (openSet.isNotEmpty()) {
            val current = openSet.poll()!!
            if (current == endId) return reconstruct(cameFrom, nodeMap, endId)
            closed.add(current)

            adj[current]?.forEach { (neighbor, weight) ->
                if (neighbor in closed) return@forEach
                val tentativeG = (gScore[current] ?: Float.MAX_VALUE) + weight
                if (tentativeG < (gScore[neighbor] ?: Float.MAX_VALUE)) {
                    cameFrom[neighbor] = current
                    gScore[neighbor]   = tentativeG
                    fScore[neighbor]   = tentativeG + h(nodeMap[neighbor] ?: return@forEach, goal, dims)
                    if (!openSet.contains(neighbor)) openSet.add(neighbor)
                }
            }
        }
        return emptyList()
    }

    /**
     * Builds a bidirectional adjacency list from [edges].
     * Extracted to reduce cognitive complexity of [findPath].
     */
    private fun buildAdjacencyList(
        edges:          List<IndoorEdge>,
        accessibleOnly: Boolean
    ): HashMap<String, MutableList<Pair<String, Float>>> {
        val adj = HashMap<String, MutableList<Pair<String, Float>>>()
        edges.forEach { edge ->
            if (accessibleOnly && !edge.accessible) return@forEach
            adj.getOrPut(edge.from) { mutableListOf() }.add(edge.to   to edge.weight)
            adj.getOrPut(edge.to)   { mutableListOf() }.add(edge.from to edge.weight)
        }
        return adj
    }

    /**
     * Euclidean distance heuristic scaled by real building dimensions.
     * Multiplying normalized 0–1 coords by the building's width/height (metres)
     * gives a physically meaningful distance estimate, preventing skewed paths
     * in buildings with non-square floor plans (e.g. Hall Building is ~70m × 120m).
     */
    private fun h(
        a:    IndoorNode,
        b:    IndoorNode,
        dims: IndoorBuildingConfig.BuildingDims
    ): Float {
        val dx = (a.x - b.x) * dims.widthM
        val dy = (a.y - b.y) * dims.heightM
        return sqrt(dx * dx + dy * dy)
    }

    private fun reconstruct(
        cameFrom: Map<String, String>,
        nodeMap:  Map<String, IndoorNode>,
        endId:    String
    ): List<IndoorNode> {
        val path = mutableListOf<IndoorNode>()
        var cur: String? = endId
        while (cur != null) {
            nodeMap[cur]?.let { path.add(it) }
            cur = cameFrom[cur]
        }
        return path.reversed()
    }
}
