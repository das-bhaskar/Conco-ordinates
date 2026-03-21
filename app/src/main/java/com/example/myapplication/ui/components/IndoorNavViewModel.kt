package com.example.myapplication.ui.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.indoor.IndoorRepository
import com.example.myapplication.data.indoor.IndoorFloor
import com.example.myapplication.data.indoor.IndoorNode
import com.example.myapplication.logic.IndoorOutdoorRouter
import com.example.myapplication.logic.IndoorOutdoorRouter.Segment
import com.example.myapplication.logic.TransferPreference
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── UI state ──────────────────────────────────────────────────────────────────

data class IndoorNavUiState(
    // current floor display
    val floor:              IndoorFloor?    = null,
    val currentFloorNumber: Int             = 1,
    val currentBuilding:    String          = "",
    val availableFloors:    List<Int>       = emptyList(),

    // room selection
    val selectedRoom:       com.example.myapplication.data.indoor.IndoorRoom? = null,
    val highlightRoomId:    String?         = null,

    // active route
    val fullRoute:              IndoorOutdoorRouter.FullRoute? = null,
    val currentSegmentIdx:      Int             = 0,
    val pathNodeIds:            Set<String>     = emptySet(),
    val pathEdgeIds:            List<Pair<String, String>> = emptyList(),

    // current segment instruction shown to user
    val instruction:            String          = "",

    // floor-change overlay (user must confirm they changed floors)
    val pendingFloorChange:     Segment.FloorChange? = null,

    // mid-route advance card: shown when a Walk segment ends but route continues
    val pendingSegmentAdvance:  String?         = null,

    // exit confirmation: set true when the exit-leg path finishes drawing.
    val pendingExitConfirm:     Boolean         = false,

    // outdoor handoff
    val outdoorSegment:         Segment.OutdoorWalk? = null,

    // ── Accessibility & transfer preference ───────────────────────────────────
    // Accessible mode (♿): forces ELEVATOR_ONLY regardless of transferPreference.
    val accessibleMode:         Boolean         = false,
    // User's preferred floor-change method (⚙ settings menu).
    val transferPreference:     TransferPreference = TransferPreference.ANY,

    // misc
    val showNavGraph:           Boolean         = false,
    val isLoading:              Boolean         = false,
    val error:                  String?         = null,
    val hasArrived:             Boolean         = false,
    val isExitLeg:              Boolean         = false
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class IndoorNavViewModel(app: Application) : AndroidViewModel(app) {

    private val repo   = IndoorRepository(app)
    private val _state = MutableStateFlow(IndoorNavUiState())
    val state: StateFlow<IndoorNavUiState> = _state.asStateFlow()

    // ── loading ───────────────────────────────────────────────────────────────

    /**
     * Fully resets navigation state and loads the building/floor.
     * Called every time IndoorNavScreen opens to prevent stale route data
     * from a previous session being shown on re-entry.
     */
    fun resetForNewSession(building: String, floor: Int, floors: List<Int>, isExitLeg: Boolean = false) {
        _state.update {
            IndoorNavUiState(
                currentBuilding    = building,
                availableFloors    = floors,
                currentFloorNumber = floor,
                isExitLeg          = isExitLeg
            )
        }
        loadFloor(building, floor)
    }

    fun loadBuilding(building: String, floor: Int, floors: List<Int>) {
        _state.update { it.copy(currentBuilding = building, availableFloors = floors) }
        loadFloor(building, floor)
    }

    fun switchFloor(building: String, floor: Int) {
        _state.update {
            it.copy(
                currentFloorNumber = floor,
                selectedRoom       = null,
                pathNodeIds        = emptySet(),
                pathEdgeIds        = emptyList()
            )
        }
        loadFloor(building, floor)
    }

    private fun loadFloor(building: String, floor: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result       = repo.getFloor(building, floor)
            val defaultStart = result?.nodes?.firstOrNull { it.type == "ENTRANCE" }?.id
            _state.update {
                it.copy(
                    isLoading          = false,
                    floor              = result,
                    currentFloorNumber = floor,
                    currentBuilding    = building,
                    error = if (result == null) "No map data for $building floor $floor" else null
                )
            }
        }
    }

    // ── room interaction ──────────────────────────────────────────────────────

    fun onRoomTap(room: com.example.myapplication.data.indoor.IndoorRoom) =
        _state.update { it.copy(selectedRoom = room) }

    fun dismissRoom() = _state.update { it.copy(selectedRoom = null) }
    fun toggleNavGraph() = _state.update { it.copy(showNavGraph = !it.showNavGraph) }

    /** Toggle accessible mode (♿). When on, forces ELEVATOR_ONLY. */
    fun toggleAccessibleMode() {
        _state.update { it.copy(accessibleMode = !it.accessibleMode) }
    }

    /** Set the user's preferred floor-change method from the ⚙ settings menu. */
    fun setTransferPreference(pref: TransferPreference) {
        _state.update { it.copy(transferPreference = pref) }
    }

    /** Effective preference: accessible mode overrides the settings menu choice. */
    private fun effectivePreference(): TransferPreference =
        if (_state.value.accessibleMode) TransferPreference.ELEVATOR_ONLY
        else _state.value.transferPreference

    // ── FULL ROUTE NAVIGATION ─────────────────────────────────────────────────

    /**
     * Build a complete route from the current position to [destination].
     * May span buildings, floors, and outdoor segments.
     *
     * [startFloor] overrides the currently displayed floor — needed when the
     * user's start node is on a different floor than the one loaded in the map
     * (e.g. same-building cross-floor: user is on floor 1, destination is floor 8).
     */
    fun navigateTo(
        destination: IndoorOutdoorRouter.IndoorDestination,
        startNodeId: String,
        building:    String? = null,
        startFloor:  Int?    = null,
        userGps:     LatLng? = null
    ) {
        val state = _state.value
        val resolvedBuilding   = building ?: state.currentBuilding
        val resolvedStartFloor = startFloor ?: state.currentFloorNumber
        val preference         = effectivePreference()
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // If nodeId is blank, resolve it from the destination floor JSON
            val resolvedDestNodeId = if (destination.nodeId.isBlank()) {
                val destFloorData = repo.getFloor(destination.building, destination.floor)
                val room = destFloorData?.rooms?.firstOrNull { room ->
                    room.label.endsWith(destination.label.substringAfterLast('-'), ignoreCase = true) ||
                    room.id.endsWith(destination.label.substringAfterLast('-'), ignoreCase = true)
                }
                val node = if (room != null) {
                    destFloorData?.nodes?.firstOrNull { it.roomId == room.id }
                } else {
                    destFloorData?.nodes?.firstOrNull { n ->
                        n.roomId?.endsWith(destination.label.substringAfterLast('-'), ignoreCase = true) == true
                    }
                }
                if (node == null) {
                    android.util.Log.e("IndoorNav",
                        "Cannot resolve nodeId for ${destination.label} on floor ${destination.floor}")
                    _state.update { it.copy(isLoading = false,
                        error = "Cannot find room ${destination.label} on floor ${destination.floor}") }
                    return@launch
                }
                android.util.Log.d("IndoorNav",
                    "Resolved nodeId for ${destination.label} → ${node.id}")
                node.id
            } else {
                destination.nodeId
            }

            val resolvedDestination = destination.copy(nodeId = resolvedDestNodeId)

            val route = IndoorOutdoorRouter.buildRoute(
                repo          = repo,
                startBuilding = resolvedBuilding,
                startFloor    = resolvedStartFloor,
                startNodeId   = startNodeId,
                destination   = resolvedDestination,
                userGps       = userGps,
                preference    = preference
            )

            android.util.Log.d("IndoorNav",
                "Route: ${route.segments.size} segments, " +
                "start=$startNodeId(F$resolvedStartFloor, $resolvedBuilding) " +
                "→ ${resolvedDestNodeId}(F${destination.floor})")

            if (route.segments.isEmpty()) {
                _state.update { it.copy(isLoading = false,
                    error = "No path found to ${destination.label}") }
                return@launch
            }

            _state.update { it.copy(isLoading = false, fullRoute = route, currentSegmentIdx = 0) }
            applySegment(0)
        }
    }

    /**
     * User signals they have completed the current segment
     * (arrived at exit, boarded elevator, etc.).
     */
    fun advanceToNextSegment() {
        val st = _state.value
        val route = st.fullRoute ?: return
        val nextIdx = st.currentSegmentIdx + 1

        if (nextIdx >= route.segments.size) {
            _state.update { it.copy(hasArrived = true, instruction = "You have arrived!") }
            return
        }

        // If next segment is a floor change, show overlay first
        val nextSeg = route.segments[nextIdx]
        if (nextSeg is Segment.FloorChange) {
            _state.update {
                it.copy(
                    currentSegmentIdx = nextIdx,
                    pendingFloorChange = nextSeg,
                    instruction = nextSeg.instruction
                )
            }
            return
        }

        // If next is outdoor, surface the outdoor segment
        if (nextSeg is Segment.OutdoorWalk) {
            _state.update {
                it.copy(
                    currentSegmentIdx = nextIdx,
                    outdoorSegment    = nextSeg,
                    instruction       = nextSeg.instruction
                )
            }
            return
        }

        _state.update { it.copy(currentSegmentIdx = nextIdx) }
        applySegment(nextIdx)
    }

    /** Call after user has physically changed floors. */
    fun confirmFloorChange() {
        val st = _state.value
        val fc = st.pendingFloorChange ?: return
        _state.update { it.copy(pendingFloorChange = null) }
        // Load the new floor then advance
        loadFloor(fc.building, fc.toFloor)
        advanceToNextSegment()
    }

    /** Call after outdoor navigation hands back to indoor. */
    fun confirmOutdoorArrival() {
        _state.update { it.copy(outdoorSegment = null) }
        advanceToNextSegment()
    }

    fun clearRoute() = _state.update {
        it.copy(
            fullRoute        = null,
            currentSegmentIdx = 0,
            pathNodeIds      = emptySet(),
            pathEdgeIds      = emptyList(),
            highlightRoomId  = null,
            pendingFloorChange = null,
            outdoorSegment   = null,
            instruction      = "",
            hasArrived       = false
        )
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun applySegment(idx: Int) {
        val route = _state.value.fullRoute ?: return
        val seg   = route.segments.getOrNull(idx) ?: return

        when (seg) {
            is Segment.IndoorWalk -> {
                val path    = seg.path
                val nodeIds = path.map { it.id }.toSet()
                val edgeIds = path.zipWithNext { a, b -> a.id to b.id }

                // Load the correct floor map if this segment is on a different floor
                if (seg.floor != _state.value.currentFloorNumber) {
                    loadFloor(seg.building, seg.floor)
                }

                val isLastSegment = idx == route.segments.size - 1
                val nextSeg       = route.segments.getOrNull(idx + 1)

                // If path is empty or user is already at the exit node (start==end),
                // skip showing a path and go straight to exit/arrival confirmation.
                if (path.isEmpty() || (path.size == 1 && isLastSegment)) {
                    _state.update {
                        it.copy(
                            pathNodeIds           = nodeIds,
                            pathEdgeIds           = emptyList(),
                            instruction           = seg.instruction,
                            pendingSegmentAdvance = null,
                            hasArrived            = true
                        )
                    }
                    return
                }

                // Determine confirmation prompt. Order matters:
                // OutdoorWalk next → null (exit card via onConfirmExit handles it)
                // FloorChange next → "Have you reached the elevator?"
                // Last segment, exit leg → "Have you reached the exit?" (user walks first, then confirms)
                // Last segment, normal  → "Have you arrived at your destination?"
                val advancePrompt: String? = when {
                    nextSeg is Segment.OutdoorWalk -> null
                    nextSeg is Segment.FloorChange ->
                        "Have you reached the ${nextSeg.via}?"
                    isLastSegment && _state.value.isExitLeg ->
                        "Have you reached the exit?"
                    isLastSegment ->
                        "Have you arrived at your destination?"
                    else -> null
                }

                _state.update {
                    it.copy(
                        pathNodeIds           = nodeIds,
                        pathEdgeIds           = edgeIds,
                        instruction           = seg.instruction,
                        highlightRoomId       = path.lastOrNull()?.roomId,
                        pendingSegmentAdvance = advancePrompt,
                        // Never auto-set hasArrived here — user must confirm via the card
                        hasArrived            = false
                    )
                }
            }
            is Segment.FloorChange -> {
                _state.update {
                    it.copy(
                        pendingFloorChange    = seg,
                        pendingSegmentAdvance = null,
                        instruction           = seg.instruction
                    )
                }
            }
            is Segment.OutdoorWalk -> {
                _state.update {
                    it.copy(
                        outdoorSegment        = seg,
                        pendingSegmentAdvance = null,
                        instruction           = seg.instruction
                    )
                }
            }
        }
    }

    /** Called when user taps the advance/confirmation button.
     *  - Mid-route: user has physically reached the transfer point → advance to FloorChange
     *  - Final segment: user has arrived at destination → set hasArrived */
    fun confirmSegmentAdvance() {
        val st    = _state.value
        val route = st.fullRoute ?: return
        val isLast = st.currentSegmentIdx == route.segments.size - 1

        when {
            isLast && st.isExitLeg -> {
                // User confirmed they reached the exit — show ExitConfirmationCard
                _state.update { it.copy(pendingSegmentAdvance = null, hasArrived = true) }
            }
            isLast -> {
                // User confirmed they arrived at final destination
                _state.update { it.copy(pendingSegmentAdvance = null, hasArrived = true) }
            }
            else -> {
                // Mid-route: advance to next segment (FloorChange)
                _state.update { it.copy(pendingSegmentAdvance = null) }
                advanceToNextSegment()
            }
        }
    }
}
