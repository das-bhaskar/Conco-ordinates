package com.example.myapplication.logic

import com.example.myapplication.data.indoor.IIndoorRepository
import com.example.myapplication.data.indoor.IndoorEdge
import com.example.myapplication.data.indoor.IndoorFloor
import com.example.myapplication.data.indoor.IndoorNode
import kotlin.math.sqrt

/**
 * Finds a path that may cross multiple floors within one building.
 *
 * Elevators and staircases are matched across floors using [IndoorNode.elevatorGroupId].
 * Escalators can additionally define directed cross-floor links using
 * [IndoorNode.transferFloor] + [IndoorNode.transferNodeId].
 */
class CrossFloorNavigator {

    companion object {
        const val VIA_ELEVATOR  = "elevator"
        const val VIA_ESCALATOR = "escalator"
        const val VIA_STAIRCASE = "staircase"

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

    private data class State(
        val floor: Int,
        val nodeId: String
    )

    private sealed class GraphAction {
        data class Walk(
            val building: String,
            val floor: Int,
            val path: List<IndoorNode>
        ) : GraphAction()

        data class Transfer(
            val building: String,
            val fromFloor: Int,
            val toFloor: Int,
            val via: String,
            val targetNodeId: String
        ) : GraphAction()
    }

    private data class GraphEdge(
        val to: State,
        val cost: Float,
        val action: GraphAction
    )

    private data class FloorRoute(
        val from: State,
        val to: State,
        val path: List<IndoorNode>,
        val cost: Float
    )

    private data class DijkstraNode(
        val state: State,
        val cost: Float
    )

    suspend fun navigate(
        repo:         IIndoorRepository,
        building:     String,
        startFloor:   Int,
        startNodeId:  String,
        targetFloor:  Int,
        targetNodeId: String,
        preference:   TransferPreference = TransferPreference.ANY
    ): List<NavStep> {
        val floorMap = loadFloors(repo, building, startFloor, targetFloor)
        if (floorMap[startFloor] == null || floorMap[targetFloor] == null) return emptyList()
        if (startFloor == targetFloor && startNodeId == targetNodeId) {
            val node = floorMap[startFloor]
                ?.nodes
                ?.firstOrNull { it.id == startNodeId }
                ?: return emptyList()
            return listOf(
                NavStep.Walk(
                    FloorSegment(
                        floor = startFloor,
                        path = listOf(node),
                        building = building
                    )
                )
            )
        }

        val primary = navigateWithTypes(
            floorMap = floorMap,
            building = building,
            start = State(startFloor, startNodeId),
            destination = State(targetFloor, targetNodeId),
            allowedTypes = preference.primary,
            accessibleOnly = preference == TransferPreference.ELEVATOR_ONLY
        )
        if (primary.isNotEmpty()) return primary

        return if (preference.fallback.isNotEmpty()) {
            navigateWithTypes(
                floorMap = floorMap,
                building = building,
                start = State(startFloor, startNodeId),
                destination = State(targetFloor, targetNodeId),
                allowedTypes = preference.fallback,
                accessibleOnly = preference == TransferPreference.ELEVATOR_ONLY
            )
        } else emptyList()
    }

    private suspend fun loadFloors(
        repo: IIndoorRepository,
        building: String,
        startFloor: Int,
        targetFloor: Int
    ): Map<Int, IndoorFloor> {
        val floors = (
            com.example.myapplication.data.indoor.IndoorBuildingConfig.floorsFor(building) +
                listOf(startFloor, targetFloor)
            ).distinct()

        return buildMap {
            floors.forEach { floor ->
                repo.getFloor(building, floor)?.let { put(floor, it) }
            }
        }
    }

    private fun navigateWithTypes(
        floorMap: Map<Int, IndoorFloor>,
        building: String,
        start: State,
        destination: State,
        allowedTypes: List<String>,
        accessibleOnly: Boolean
    ): List<NavStep> {
        val graph = buildGraph(floorMap, building, start, destination, allowedTypes, accessibleOnly)
        val actions = shortestActions(graph, start, destination)
        if (actions.isEmpty()) return emptyList()
        return toNavSteps(actions)
    }

    private fun buildGraph(
        floorMap: Map<Int, IndoorFloor>,
        building: String,
        start: State,
        destination: State,
        allowedTypes: List<String>,
        accessibleOnly: Boolean
    ): Map<State, List<GraphEdge>> {
        val graph = mutableMapOf<State, MutableList<GraphEdge>>()
        val anchorsByFloor = mutableMapOf<Int, MutableSet<State>>()

        floorMap.forEach { (floorNumber, floorData) ->
            val anchors = anchorsByFloor.getOrPut(floorNumber) { linkedSetOf() }
            floorData.nodes
                .filter { it.type in allowedTypes }
                .forEach { anchors += State(floorNumber, it.id) }
        }

        anchorsByFloor.getOrPut(start.floor) { linkedSetOf() } += start
        anchorsByFloor.getOrPut(destination.floor) { linkedSetOf() } += destination

        anchorsByFloor.forEach { (floorNumber, anchors) ->
            val floorData = floorMap[floorNumber] ?: return@forEach
            val routes = floorRoutesFor(floorData, building, anchors.toList(), accessibleOnly)
            routes.forEach { route ->
                graph.getOrPut(route.from) { mutableListOf() }.add(
                    GraphEdge(
                        to = route.to,
                        cost = route.cost,
                        action = GraphAction.Walk(building, floorNumber, route.path)
                    )
                )
            }
        }

        floorMap.forEach { (floorNumber, floorData) ->
            floorData.nodes
                .filter { it.type in allowedTypes }
                .forEach { node ->
                    val from = State(floorNumber, node.id)
                    transferEdgesFor(node, floorMap, building, floorNumber, allowedTypes).forEach { edge ->
                        graph.getOrPut(from) { mutableListOf() }.add(edge)
                    }
                }
        }

        return graph
    }

    private fun floorRoutesFor(
        floor: IndoorFloor,
        building: String,
        anchors: List<State>,
        accessibleOnly: Boolean
    ): List<FloorRoute> {
        if (anchors.size < 2) return emptyList()

        val nodeMap = floor.nodes.associateBy { it.id }
        val routes = mutableListOf<FloorRoute>()

        for (from in anchors) {
            for (to in anchors) {
                if (from == to) continue
                if (nodeMap[from.nodeId] == null || nodeMap[to.nodeId] == null) continue

                val path = IndoorPathfinder.findPath(
                    nodes = floor.nodes,
                    edges = floor.edges,
                    startId = from.nodeId,
                    endId = to.nodeId,
                    accessibleOnly = accessibleOnly,
                    building = building
                )
                if (path.isEmpty()) continue

                routes += FloorRoute(
                    from = from,
                    to = to,
                    path = path,
                    cost = pathCost(path, building)
                )
            }
        }
        return routes
    }

    private fun transferEdgesFor(
        node: IndoorNode,
        floorMap: Map<Int, IndoorFloor>,
        building: String,
        floorNumber: Int,
        allowedTypes: List<String>
    ): List<GraphEdge> {
        if (node.type == "ESCALATOR") {
            val targetFloor = node.transferFloor
            val targetNodeId = node.transferNodeId
            if (targetFloor != null && targetNodeId != null) {
                val targetFloorData = floorMap[targetFloor] ?: return emptyList()
                val targetNode = targetFloorData.nodes.firstOrNull { it.id == targetNodeId } ?: return emptyList()
                return listOf(
                    GraphEdge(
                        to = State(targetFloor, targetNode.id),
                        cost = 0f,
                        action = GraphAction.Transfer(
                            building = building,
                            fromFloor = floorNumber,
                            toFloor = targetFloor,
                            via = VIA_ESCALATOR,
                            targetNodeId = targetNode.id
                        )
                    )
                )
            }
        }

        val groupId = node.elevatorGroupId ?: return emptyList()
        if (node.type == "ESCALATOR") {
            val groupHasDirectionalEscalator = floorMap.values.any { floor ->
                floor.nodes.any { candidate ->
                    candidate.type == "ESCALATOR" &&
                        candidate.elevatorGroupId == groupId &&
                        candidate.transferFloor != null &&
                        candidate.transferNodeId != null
                }
            }
            if (groupHasDirectionalEscalator) return emptyList()
        }

        return floorMap
            .filterKeys { it != floorNumber }
            .flatMap { (targetFloor, targetFloorData) ->
                targetFloorData.nodes
                    .filter { it.type == node.type && it.elevatorGroupId == groupId && it.type in allowedTypes }
                    .map { targetNode ->
                        GraphEdge(
                            to = State(targetFloor, targetNode.id),
                            cost = 0f,
                            action = GraphAction.Transfer(
                                building = building,
                                fromFloor = floorNumber,
                                toFloor = targetFloor,
                                via = when (node.type) {
                                    "ELEVATOR" -> VIA_ELEVATOR
                                    "ESCALATOR" -> VIA_ESCALATOR
                                    else -> VIA_STAIRCASE
                                },
                                targetNodeId = targetNode.id
                            )
                        )
                    }
            }
    }

    private fun shortestActions(
        graph: Map<State, List<GraphEdge>>,
        start: State,
        destination: State
    ): List<GraphAction> {
        val costs = mutableMapOf(start to 0f)
        val previousState = mutableMapOf<State, State>()
        val previousAction = mutableMapOf<State, GraphAction>()
        val queue = java.util.PriorityQueue<DijkstraNode>(compareBy { it.cost })
        queue += DijkstraNode(start, 0f)

        while (queue.isNotEmpty()) {
            val current = queue.poll()
            if (current.cost > (costs[current.state] ?: Float.MAX_VALUE)) continue
            if (current.state == destination) break

            graph[current.state].orEmpty().forEach { edge ->
                val nextCost = current.cost + edge.cost
                if (nextCost < (costs[edge.to] ?: Float.MAX_VALUE)) {
                    costs[edge.to] = nextCost
                    previousState[edge.to] = current.state
                    previousAction[edge.to] = edge.action
                    queue += DijkstraNode(edge.to, nextCost)
                }
            }
        }

        if (destination != start && destination !in previousAction) return emptyList()

        val actions = mutableListOf<GraphAction>()
        var cursor = destination
        while (cursor != start) {
            val action = previousAction[cursor] ?: return emptyList()
            actions += action
            cursor = previousState[cursor] ?: return emptyList()
        }
        return actions.reversed()
    }

    private fun toNavSteps(actions: List<GraphAction>): List<NavStep> {
        val steps = mutableListOf<NavStep>()
        var pendingWalk: FloorSegment? = null

        fun flushWalk() {
            pendingWalk?.let { steps += NavStep.Walk(it) }
            pendingWalk = null
        }

        actions.forEach { action ->
            when (action) {
                is GraphAction.Walk -> {
                    if (action.path.isEmpty()) return@forEach
                    val current = pendingWalk
                    pendingWalk = if (
                        current != null &&
                        current.floor == action.floor &&
                        current.building == action.building &&
                        current.path.lastOrNull()?.id == action.path.firstOrNull()?.id
                    ) {
                        current.copy(path = current.path + action.path.drop(1))
                    } else {
                        flushWalk()
                        FloorSegment(action.floor, action.path, action.building)
                    }
                }
                is GraphAction.Transfer -> {
                    flushWalk()
                    steps += NavStep.ChangeFloor(
                        fromFloor = action.fromFloor,
                        toFloor = action.toFloor,
                        via = action.via,
                        building = action.building,
                        targetNodeId = action.targetNodeId
                    )
                }
            }
        }

        flushWalk()
        return steps
    }

    private fun pathCost(path: List<IndoorNode>, building: String): Float {
        val dims = com.example.myapplication.data.indoor.IndoorBuildingConfig.dimsFor(building)
        var cost = 0f
        for (i in 0 until path.size - 1) {
            val a = path[i]
            val b = path[i + 1]
            val dx = (a.x - b.x) * dims.widthM
            val dy = (a.y - b.y) * dims.heightM
            cost += sqrt(dx * dx + dy * dy)
        }
        return cost
    }
}
