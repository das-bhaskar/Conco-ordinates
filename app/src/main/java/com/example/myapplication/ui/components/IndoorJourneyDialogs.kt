package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.Building
import com.example.myapplication.data.indoor.BuildingEntrance
import com.example.myapplication.data.indoor.BuildingEntrances
import com.example.myapplication.ui.models.IndoorJourneyPhase

private val Maroon = Color(0xFF912338)

/**
 * Hosts all dialogs for the indoor journey state machine.
 *
 * The composable is intentionally free of business logic and coroutines:
 * - Room search is delegated to [onSearchRoom] which the ViewModel handles.
 * - Entrance selection is delegated to [onEntranceSelected].
 * This satisfies SRP and makes every dialog preview/test without a real repository.
 */
@Composable
fun IndoorJourneyDialogs(
    phase:              IndoorJourneyPhase,
    /**
     * Called when the user submits a room query.
     * The ViewModel runs [IndoorRoomResolver.resolve] and updates
     * [IndoorJourneyPhase] accordingly, then calls back via [onRoomResolved]
     * or surfaces an error through the UiState.
     */
    onSearchRoom:       (query: String, buildingCode: String) -> Unit,
    isSearching:        Boolean,
    searchError:        String?,
    onRoomResolved:     (nodeId: String, label: String, buildingCode: String, floor: Int) -> Unit,
    onEntranceSelected: (BuildingEntrance) -> Unit,
    onDismiss:          () -> Unit
) {
    when (phase) {

        // ── Detecting location ────────────────────────────────────────────
        IndoorJourneyPhase.DetectingLocation -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Finding your location…") },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Maroon, strokeWidth = 2.dp
                        )
                        Text("Checking if you're inside a building")
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // ── Ask current room ──────────────────────────────────────────────
        is IndoorJourneyPhase.AskCurrentRoom -> {
            AskCurrentRoomDialog(
                phase        = phase,
                onSearchRoom = onSearchRoom,
                isSearching  = isSearching,
                searchError  = searchError,
                onResolved   = onRoomResolved,
                onDismiss    = onDismiss
            )
        }

        // ── Ask entry point ───────────────────────────────────────────────
        is IndoorJourneyPhase.AskEntryPoint -> {
            AskEntryPointDialog(
                building   = phase.building,
                entrances  = phase.entrances,
                onSelected = onEntranceSelected,
                onDismiss  = onDismiss
            )
        }

        // ── Arrived ───────────────────────────────────────────────────────
        IndoorJourneyPhase.Arrived -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("You have arrived! 🎉", fontWeight = FontWeight.Bold) },
                text  = { Text("You have reached your destination.") },
                confirmButton = {
                    Button(onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Maroon)
                    ) { Text("Done", color = Color.White) }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = Color.White
            )
        }

        else -> { /* Idle, Outdoor, IndoorToExit, IndoorToDestination — no dialog */ }
    }
}

// ── Ask current room dialog ───────────────────────────────────────────────────

/**
 * Room search dialog — the UI owns query state only.
 * Business logic (coroutine, resolve, error) lives in the ViewModel;
 * results flow back via [onSearchRoom] → ViewModel → [isSearching]/[searchError]/[onResolved].
 */
@Composable
private fun AskCurrentRoomDialog(
    phase:       IndoorJourneyPhase.AskCurrentRoom,
    onSearchRoom:(query: String, buildingCode: String) -> Unit,
    isSearching: Boolean,
    searchError: String?,
    onResolved:  (nodeId: String, label: String, buildingCode: String, floor: Int) -> Unit,
    onDismiss:   () -> Unit
) {
    var query        by remember { mutableStateOf("") }
    val buildingCode = phase.currentBuilding.code

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("You're in ${phase.currentBuilding.name}",
                    fontWeight = FontWeight.Bold)
                Text(
                    "Navigating to ${phase.destination.label}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(.55f)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Which room are you currently in?",
                    color = MaterialTheme.colorScheme.onSurface.copy(.7f))
                OutlinedTextField(
                    value         = query,
                    onValueChange = { query = it },
                    placeholder   = { Text("e.g. ${buildingCode}-119") },
                    singleLine    = true,
                    isError       = searchError != null,
                    supportingText = searchError?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text("Or start from:", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(.5f))
                val entrances = BuildingEntrances.forBuilding(buildingCode)
                entrances.forEach { entrance ->
                    Surface(
                        onClick = {
                            onResolved(entrance.nodeId, entrance.label, buildingCode, entrance.floor)
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = Maroon.copy(.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "🚪  ${entrance.label}",
                            modifier = Modifier.padding(12.dp),
                            color = Maroon,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = query.isNotBlank() && !isSearching,
                colors  = ButtonDefaults.buttonColors(containerColor = Maroon),
                // UI simply notifies the ViewModel — no coroutine here
                onClick = { onSearchRoom(query, buildingCode) }
            ) {
                if (isSearching) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Start Navigation", color = Color.White)
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = RoundedCornerShape(16.dp)
    )
}

// ── Ask entry point dialog ────────────────────────────────────────────────────

@Composable
private fun AskEntryPointDialog(
    building:   Building,
    entrances:  List<BuildingEntrance>,
    onSelected: (BuildingEntrance) -> Unit,
    onDismiss:  () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Entering ${building.name}", fontWeight = FontWeight.Bold) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Which entrance are you using?",
                    color = MaterialTheme.colorScheme.onSurface.copy(.7f))
                Spacer(Modifier.height(4.dp))
                entrances.forEach { entrance ->
                    Surface(
                        onClick  = { onSelected(entrance) },
                        shape    = RoundedCornerShape(8.dp),
                        color    = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🚪  ${entrance.label}", fontWeight = FontWeight.Medium)
                            Text("Floor ${entrance.floor}", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(.5f))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = RoundedCornerShape(16.dp)
    )
}
