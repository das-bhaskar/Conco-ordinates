package com.example.myapplication.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.indoor.BuildingEntrances
import com.example.myapplication.data.indoor.IndoorRepository
import com.example.myapplication.data.indoor.IndoorRoom
import com.example.myapplication.logic.CrossFloorNavigator
import com.example.myapplication.logic.IndoorOutdoorRouter
import com.example.myapplication.logic.IndoorRoomResolver
import com.example.myapplication.logic.TransferPreference
import com.example.myapplication.ui.components.IndoorMapCanvas
import com.example.myapplication.ui.components.IndoorNavUiState
import com.example.myapplication.ui.components.IndoorNavViewModel
import com.example.myapplication.ui.components.IndoorNavViewModelFactory
import kotlinx.coroutines.launch


private val Maroon = Color(0xFF912338)

/**
 * Groups all call-site parameters for [IndoorNavScreen] so the composable
 * stays within SonarCloud's 7-parameter limit.
 */
data class IndoorNavParams(
    val building:         String,
    val availableFloors:  List<Int>                                        = listOf(1),
    val initialFloor:     Int                                              = availableFloors.first(),
    val destination:      IndoorOutdoorRouter.IndoorDestination?           = null,
    val startNodeId:      String?                                          = null,
    val startFloor:       Int?                                             = null,
    val sessionKey:       String                                           = "default",
    val onBack:           () -> Unit                                       = {},
    val onConfirmExit:    (() -> Unit)?                                    = null,
    val onOutdoorHandoff: (IndoorOutdoorRouter.Segment.OutdoorWalk) -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndoorNavScreen(
    params: IndoorNavParams,
    vm: IndoorNavViewModel = viewModel(
        key     = "indoor-nav-${params.sessionKey}",
        factory = IndoorNavViewModelFactory(
            androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val building         = params.building
    val availableFloors  = params.availableFloors
    val initialFloor     = params.initialFloor
    val destination      = params.destination
    val startNodeId      = params.startNodeId
    val startFloor       = params.startFloor
    val onBack           = params.onBack
    val onConfirmExit    = params.onConfirmExit
    val onOutdoorHandoff = params.onOutdoorHandoff
    val state by vm.state.collectAsState()

    // Initial navigation
    LaunchedEffect(building, destination, startNodeId) {
        vm.resetForNewSession(
            building   = building,
            floor      = initialFloor,
            floors     = availableFloors,
            isExitLeg  = onConfirmExit != null
        )
        if (destination != null && startNodeId != null) {
            vm.navigateTo(
                building    = building,
                destination = destination,
                startNodeId = startNodeId,
                startFloor  = startFloor
            )
        }
    }

    // Hand off outdoor segment to parent
    LaunchedEffect(state.outdoorSegment) {
        state.outdoorSegment?.let { onOutdoorHandoff(it) }
    }

    Scaffold(
        topBar = { IndoorNavTopBar(state, vm, onBack) }
    ) { padding ->
        IndoorNavContent(
            state        = state,
            vm           = vm,
            padding      = padding,
            onConfirmExit = onConfirmExit,
            onBack        = onBack
        )
    }
}

// ── top bar ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IndoorNavTopBar(
    state: IndoorNavUiState,
    vm:    IndoorNavViewModel,
    onBack: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    "${state.currentBuilding} · ${if (state.currentFloorNumber < 0) "B${-state.currentFloorNumber}" else "Floor ${state.currentFloorNumber}"}",
                    fontWeight = FontWeight.SemiBold
                )
                if (state.instruction.isNotEmpty()) {
                    Text(state.instruction, fontSize = 11.sp, color = Maroon)
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
        },
        actions = { IndoorNavTopBarActions(state, vm) }
    )
}

/** Actions row for [IndoorNavTopBar] — extracted to keep the top bar below complexity limit. */
@Composable
private fun IndoorNavTopBarActions(state: IndoorNavUiState, vm: IndoorNavViewModel) {
    IconButton(onClick = { vm.toggleAccessibleMode() }) {
        Text("♿", fontSize = 18.sp,
            color = if (state.accessibleMode) Maroon
                    else MaterialTheme.colorScheme.onSurface.copy(.35f))
    }
    TransferPreferenceMenu(state, vm)
    if (state.fullRoute != null) {
        TextButton(onClick = { vm.clearRoute() }) {
            Text("Clear", fontSize = 12.sp, color = Maroon)
        }
    }
    TextButton(onClick = { vm.toggleNavGraph() }) {
        Text(if (state.showNavGraph) "Hide Graph" else "Nav Graph", fontSize = 12.sp,
            color = if (state.showNavGraph) Maroon
                    else MaterialTheme.colorScheme.onSurface.copy(.45f))
    }
}

/** Transfer preference dropdown — extracted to keep [IndoorNavTopBarActions] readable. */
@Composable
private fun TransferPreferenceMenu(state: IndoorNavUiState, vm: IndoorNavViewModel) {
    var showPrefMenu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { showPrefMenu = true }) {
            Text(state.transferPreference.icon, fontSize = 18.sp,
                color = if (state.transferPreference != TransferPreference.ANY) Maroon
                        else MaterialTheme.colorScheme.onSurface.copy(.55f))
        }
        DropdownMenu(expanded = showPrefMenu, onDismissRequest = { showPrefMenu = false }) {
            Text("  Floor Change Method", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(.5f),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            TransferPreference.entries.forEach { pref ->
                val selected = state.transferPreference == pref
                DropdownMenuItem(
                    text = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(pref.icon, fontSize = 16.sp)
                            Text(pref.label,
                                color = if (selected) Maroon else MaterialTheme.colorScheme.onSurface)
                            if (selected) {
                                Spacer(Modifier.weight(1f))
                                Text("✓", color = Maroon, fontSize = 14.sp)
                            }
                        }
                    },
                    onClick = { vm.setTransferPreference(pref); showPrefMenu = false }
                )
            }
        }
    }
}

// ── screen content ────────────────────────────────────────────────────────────

@Composable
private fun IndoorNavContent(
    state:         IndoorNavUiState,
    vm:            IndoorNavViewModel,
    padding:       androidx.compose.foundation.layout.PaddingValues,
    onConfirmExit: (() -> Unit)?,
    onBack:        () -> Unit
) {
    Box(Modifier.fillMaxSize().padding(padding)) {

        if (state.isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Maroon)
        }

        state.floor?.let { floor ->
            IndoorMapCanvas(floor = floor, modifier = Modifier.fillMaxSize(),
                highlightRoomId = state.highlightRoomId, pathNodeIds = state.pathNodeIds,
                pathEdgeIds = state.pathEdgeIds, showNavGraph = state.showNavGraph,
                onRoomTap = { vm.onRoomTap(it) })
        }

        state.error?.let { error ->
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠️", fontSize = 20.sp)
                    Column {
                        Text(
                            "Route unavailable",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.92f)
                        )
                    }
                }
            }
        }

        if (state.availableFloors.size > 1) {
            FloorSelector(floors = state.availableFloors, current = state.currentFloorNumber,
                onSelect = { vm.switchFloor(state.currentBuilding, it) },
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp))
        }

        state.fullRoute?.let { route ->
            SegmentProgressBar(
                current  = state.stepOffset + state.currentStepIdx + 1,
                total    = state.totalStepCount.takeIf { it > 0 } ?: route.totalSteps,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp))
        }

        IndoorNavOverlays(state, vm, onConfirmExit, onBack)
    }
}

/** All animated bottom-sheet overlays — single source of truth via [ActiveOverlay] enum. */
@Composable
private fun BoxScope.IndoorNavOverlays(
    state:         IndoorNavUiState,
    vm:            IndoorNavViewModel,
    onConfirmExit: (() -> Unit)?,
    onBack:        () -> Unit
) {
    val bottomCenter = Modifier.align(Alignment.BottomCenter)

    val hasMoreSteps = state.currentSteps.isNotEmpty()
        && state.currentStepIdx < state.currentSteps.size - 1
        && state.pendingFloorChange == null && !state.hasArrived

    // Single source of truth — prevents multiple cards overlapping simultaneously.
    // Priority order matches expected user journey: floor change > next step >
    // segment advance > exit confirm > room info.
    val activeOverlay = when {
        state.pendingFloorChange != null                                              -> ActiveOverlay.FloorChange
        hasMoreSteps                                                                  -> ActiveOverlay.NextStep
        state.pendingSegmentAdvance != null                                           -> ActiveOverlay.SegmentAdvance
        state.hasArrived && onConfirmExit != null                                     -> ActiveOverlay.ExitConfirm
        state.hasArrived && onConfirmExit == null                                     -> ActiveOverlay.Arrival
        state.selectedRoom != null                                                    -> ActiveOverlay.RoomInfo
        else                                                                          -> ActiveOverlay.None
    }

    AnimatedContent(
        targetState = activeOverlay,
        modifier    = bottomCenter,
        transitionSpec = {
            (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { it } + fadeOut())
        },
        label = "indoor_overlay"
    ) { overlay ->
        when (overlay) {
            ActiveOverlay.FloorChange -> {
                state.pendingFloorChange?.let { fc ->
                    FloorChangeCard(fc.fromFloor, fc.toFloor, fc.via) { vm.confirmFloorChange() }
                }
            }
            ActiveOverlay.NextStep -> {
                NextStepCard(
                    nextInstruction = state.currentSteps.getOrNull(state.currentStepIdx + 1)?.instruction ?: "",
                    distanceLabel   = state.distanceLabel,
                    onConfirm       = { vm.advanceStep() }
                )
            }
            ActiveOverlay.SegmentAdvance -> {
                state.pendingSegmentAdvance?.let { prompt ->
                    SegmentAdvanceCard(prompt = prompt, onConfirm = { vm.confirmSegmentAdvance() })
                }
            }
            ActiveOverlay.ExitConfirm -> {
                if (onConfirmExit != null) ExitConfirmationCard(onConfirm = onConfirmExit)
            }
            ActiveOverlay.Arrival -> {
                AlertDialog(
                    onDismissRequest = {},
                    title   = { Text("Destination Reached", fontWeight = FontWeight.Bold) },
                    text    = { Text("You have arrived at your destination.") },
                    confirmButton = {
                        Button(onClick = { vm.clearRoute(); onBack() },
                            colors = ButtonDefaults.buttonColors(containerColor = Maroon)
                        ) { Text("Done", color = Color.White) }
                    },
                    shape = RoundedCornerShape(16.dp), containerColor = Color.White
                )
            }
            ActiveOverlay.RoomInfo -> {
                state.selectedRoom?.let { room ->
                    RoomCard(
                        room         = room,
                        buildingCode = state.currentBuilding,
                        currentFloor = state.currentFloorNumber,
                        onDismiss    = { vm.dismissRoom() },
                        onNavigate   = { startNodeId, startFloorNum ->
                            val destNodeId = state.floor?.nodes
                                ?.firstOrNull { it.roomId == room.id }?.id ?: return@RoomCard
                            vm.navigateTo(
                                destination = IndoorOutdoorRouter.IndoorDestination(
                                    building = state.currentBuilding,
                                    floor    = state.currentFloorNumber,
                                    nodeId   = destNodeId,
                                    label    = room.label),
                                startNodeId = startNodeId,
                                startFloor  = startFloorNum)
                            vm.dismissRoom()
                        }
                    )
                }
            }
            ActiveOverlay.None -> { /* nothing shown */ }
        }
    }
}

/** Represents which bottom overlay is currently active — enforces single source of truth. */
private enum class ActiveOverlay {
    FloorChange, NextStep, SegmentAdvance, ExitConfirm, Arrival, RoomInfo, None
}

// ── sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun SegmentProgressBar(current: Int, total: Int, modifier: Modifier = Modifier) {
    Surface(modifier.padding(horizontal = 48.dp, vertical = 2.dp),
        shape = RoundedCornerShape(8.dp), color = Color.White.copy(.85f)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Step $current / $total", fontSize = 11.sp,
                color = Maroon, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FloorChangeCard(
    fromFloor: Int, toFloor: Int, via: String, onConfirm: () -> Unit
) {
    // Icon and description for each transfer type
    val icon = when (via) {
        CrossFloorNavigator.VIA_ELEVATOR  -> "🛗"
        CrossFloorNavigator.VIA_ESCALATOR -> "↗"
        else                              -> "🪜"
    }
    val actionText = when (via) {
        CrossFloorNavigator.VIA_ELEVATOR  -> "Take the elevator"
        CrossFloorNavigator.VIA_ESCALATOR -> "Take the escalator"
        else                              -> "Use the staircase"
    }
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp), shadowElevation = 8.dp
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("$icon  Floor Change", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "$actionText from floor $fromFloor to floor $toFloor.",
                color = MaterialTheme.colorScheme.onSurface.copy(.7f)
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick  = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = Maroon)
            ) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("I'm on floor $toFloor", color = Color.White)
            }
        }
    }
}

@Composable
private fun FloorSelector(
    floors: List<Int>, current: Int,
    onSelect: (Int) -> Unit, modifier: Modifier = Modifier
) {
    Surface(modifier, shape = RoundedCornerShape(12.dp), shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            floors.sortedDescending().forEach { floor ->
                FloorButton(floor = floor, selected = floor == current, onSelect = onSelect)
            }
        }
    }
}

/** Single floor chip inside [FloorSelector]. Extracted to reduce [FloorSelector] complexity. */
@Composable
private fun FloorButton(floor: Int, selected: Boolean, onSelect: (Int) -> Unit) {
    val label = if (floor < 0) "B${-floor}" else floor.toString()
    Surface(
        onClick   = { onSelect(floor) },
        shape     = RoundedCornerShape(8.dp),
        color     = if (selected) Maroon else Color.Transparent,
        modifier  = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                fontSize   = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color      = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RoomCard(
    room:          IndoorRoom,
    buildingCode:  String,
    currentFloor:  Int,
    onDismiss:     () -> Unit,
    onNavigate:    (startNodeId: String, startFloor: Int) -> Unit
) {
    var startQuery  by remember { mutableStateOf("") }
    var errorMsg    by remember { mutableStateOf<String?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    val scope       = rememberCoroutineScope()
    val context     = androidx.compose.ui.platform.LocalContext.current
    val indoorRepo  = remember(context) {
        com.example.myapplication.data.indoor.IndoorRepository(context.applicationContext)
    }
    // Entrance node for quick shortcut
    val entranceNodeId = remember(buildingCode, currentFloor) {
        com.example.myapplication.data.indoor.BuildingEntrances
            .forBuilding(buildingCode)
            .firstOrNull()
    }

    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp), shadowElevation = 8.dp
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(room.label, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Text(room.type, fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(.5f))
                }
                IconButton(onClick = onDismiss) {
                    Text("✕", fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(.35f))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Where are you starting from?", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(.6f))
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value         = startQuery,
                onValueChange = { startQuery = it; errorMsg = null },
                placeholder   = { Text("Room number e.g. ${buildingCode}-110") },
                singleLine    = true,
                isError       = errorMsg != null,
                supportingText = errorMsg?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier      = Modifier.fillMaxWidth()
            )
            // Quick shortcut: building entrance
            if (entranceNodeId != null) {
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick        = { onNavigate(entranceNodeId.nodeId, entranceNodeId.floor) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("🚪  Start from ${entranceNodeId.label}", fontSize = 12.sp, color = Maroon)
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                enabled  = startQuery.isNotBlank() && !isSearching,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = Maroon),
                onClick  = {
                    errorMsg = null
                    scope.launch {
                        isSearching = true
                        val result = resolveRoom(indoorRepo, buildingCode, startQuery)
                        isSearching = false
                        if (result != null) onNavigate(result.nodeId, result.floor)
                        else errorMsg = "Room \"$startQuery\" not found in $buildingCode"
                    }
                }
            ) {
                if (isSearching) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Navigate Here", color = Color.White)
            }
        }
    }
}

/**
 * Mid-route and final confirmation card.
 * - Mid-route: "Have you reached the elevator?" → "Yes, Continue"
 * - Exit leg:  "Have you reached the exit?"     → "Yes, I'm at the Exit"
 * - Final:     "Have you arrived at your destination?" → "Yes, I've Arrived"
 */

@Composable
private fun NextStepCard(
    nextInstruction: String,
    distanceLabel:   String,   // pre-formatted in IndoorNavUiState — unit-testable without UI runner
    onConfirm:       () -> Unit
) {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 6.dp
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                if (distanceLabel.isNotEmpty()) {
                    Text(distanceLabel, fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(.5f))
                }
                Text(
                    "→  $nextInstruction",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
            }
            Button(
                onClick = onConfirm,
                colors  = ButtonDefaults.buttonColors(containerColor = Maroon),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("I'm here", color = Color.White, fontSize = 13.sp)
            }
        }
    }
}


@Composable
private fun SegmentAdvanceCard(prompt: String, onConfirm: () -> Unit) {
    val isExit  = prompt.startsWith("Have you reached the exit")
    val isFinal = prompt.startsWith("Have you arrived")
    val icon    = when { isExit -> "🚪"; isFinal -> "🎯"; else -> "📍" }
    val title   = when { isExit -> "Exit"; isFinal -> "Destination"; else -> "Next Step" }
    val btnText = when { isExit -> "Yes, I'm at the Exit"; isFinal -> "Yes, I've Arrived!"; else -> "Yes, Continue" }
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "$icon  $title",
                fontWeight = FontWeight.SemiBold, fontSize = 17.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(prompt, fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(.7f))
            Spacer(Modifier.height(14.dp))
            Button(
                onClick  = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = Maroon)
            ) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(btnText, color = Color.White)
            }
        }
    }
}

@Composable
private fun ExitConfirmationCard(onConfirm: () -> Unit) {    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("🚪  Are you outside the building?", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "Confirm when you have stepped outside to start outdoor navigation.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(.6f)
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick  = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = Maroon)
            ) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("I'm Outside — Start Outdoor Navigation", color = Color.White)
            }
        }
    }
}

/** Suspend helper extracted from [RoomCard] to keep its onClick lambda simple. */
private suspend fun resolveRoom(
    repo:         com.example.myapplication.data.indoor.IndoorRepository,
    buildingCode: String,
    query:        String
): com.example.myapplication.logic.IndoorRoomResolver.ResolvedRoom? =
    com.example.myapplication.logic.IndoorRoomResolver.resolve(
        repo = repo, buildingCode = buildingCode, query = query
    )
