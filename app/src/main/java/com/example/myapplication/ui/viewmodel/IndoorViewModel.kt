package com.example.myapplication.ui.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.indoor.IndoorFloor
import com.example.myapplication.data.indoor.IndoorNode
import com.example.myapplication.data.indoor.IndoorRepository
import com.example.myapplication.data.indoor.IndoorRoom
import com.example.myapplication.logic.IndoorPathfinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IndoorUiState(
    val floor:              IndoorFloor?    = null,
    val currentFloorNumber: Int             = 1,
    val availableFloors:    List<Int>       = emptyList(),
    val selectedRoom:       IndoorRoom?     = null,
    val highlightRoomId:    String?         = null,
    val pathNodeIds:        Set<String>     = emptySet(),   // nodes on the active route
    val pathEdgeIds:        List<Pair<String,String>> = emptyList(), // edges on the active route
    val startNodeId:        String?         = null,         // current origin node
    val showNavGraph:       Boolean         = false,
    val isLoading:          Boolean         = false,
    val error:              String?         = null
)

class IndoorViewModel(app: Application) : AndroidViewModel(app) {

    private val repo   = IndoorRepository(app)
    private val _state = MutableStateFlow(IndoorUiState())
    val state: StateFlow<IndoorUiState> = _state.asStateFlow()

    // ── loading ───────────────────────────────────────────────────────────────

    fun loadBuilding(building: String, startFloor: Int, availableFloors: List<Int>) {
        _state.update { it.copy(availableFloors = availableFloors, currentFloorNumber = startFloor) }
        loadFloor(building, startFloor)
    }

    fun switchFloor(building: String, floor: Int) {
        _state.update { it.copy(currentFloorNumber = floor, selectedRoom = null, pathNodeIds = emptySet(), pathEdgeIds = emptyList()) }
        loadFloor(building, floor)
    }

    private fun loadFloor(building: String, floor: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, floor = null) }
            val result = repo.getFloor(building, floor)
            // Default start = first ENTRANCE node on this floor
            val defaultStart = result?.nodes?.firstOrNull { it.type == "ENTRANCE" }?.id
            _state.update {
                it.copy(
                    isLoading   = false,
                    floor       = result,
                    startNodeId = defaultStart,
                    error       = if (result == null) "No map data for $building floor $floor" else null
                )
            }
        }
    }

    // ── room interaction ──────────────────────────────────────────────────────

    fun onRoomTap(room: IndoorRoom) = _state.update { it.copy(selectedRoom = room) }
    fun dismissRoom()               = _state.update { it.copy(selectedRoom = null) }
    fun highlight(roomId: String?)  = _state.update { it.copy(highlightRoomId = roomId) }
    fun toggleNavGraph()            = _state.update { it.copy(showNavGraph = !it.showNavGraph) }

    // ── navigation ────────────────────────────────────────────────────────────

    /**
     * Navigate from the current startNode to the node closest to [room].
     * Looks for a ROOM-type node whose roomId matches, then falls back to
     * the nearest node by Euclidean distance.
     */
    fun navigateTo(room: IndoorRoom) {
        val floor   = _state.value.floor ?: return
        val startId = _state.value.startNodeId ?: return

        // Find best destination node for this room
        val destNode = floor.nodes.firstOrNull { it.roomId == room.id }
            ?: floor.nodes.minByOrNull { n ->
                val cx = room.polygon.map { it.x }.average().toFloat()
                val cy = room.polygon.map { it.y }.average().toFloat()
                val dx = n.x - cx; val dy = n.y - cy
                dx * dx + dy * dy
            } ?: return

        val path = IndoorPathfinder.findPath(
            nodes   = floor.nodes,
            edges   = floor.edges,
            startId = startId,
            endId   = destNode.id
        )

        if (path.isEmpty()) {
            _state.update { it.copy(error = "No path found to ${room.label}") }
            return
        }

        // Build sets for the canvas to highlight
        val nodeIds = path.map { it.id }.toSet()
        val edgeIds = path.zipWithNext { a, b -> a.id to b.id }

        _state.update {
            it.copy(
                pathNodeIds  = nodeIds,
                pathEdgeIds  = edgeIds,
                highlightRoomId = room.id,
                selectedRoom = null,
                error        = null
            )
        }
    }

    /** Change the starting point (e.g. user picks a different entrance) */
    fun setStartNode(nodeId: String) = _state.update { it.copy(startNodeId = nodeId) }

    /** Clear the active route */
    fun clearPath() = _state.update {
        it.copy(pathNodeIds = emptySet(), pathEdgeIds = emptyList(), highlightRoomId = null)
    }
}
