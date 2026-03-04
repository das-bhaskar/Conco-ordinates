package com.example.myapplication.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.example.myapplication.data.ShuttleAvailability
import com.example.myapplication.shuttle.ShuttleStatusCard
import com.example.myapplication.ui.models.BuildingUiState

@Composable
fun DirectionsInfoPopup(
    uiState: BuildingUiState,
    onModeChange: (String) -> Unit,
    onStartClick: () -> Unit,
    onSwapClick: () -> Unit,
    onDestinationClick: () -> Unit,
    onClose: () -> Unit,
    onStartNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 20.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = Color.White.copy(alpha = 0.92f),
            shadowElevation = 10.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // ── Header ────────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Directions",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Black)
                    }
                }

                // ── Transport mode toggle (now 4 options) ─────────────────────
                TransportModeToggle(uiState.selectedTransportMode, onModeChange)

                Spacer(modifier = Modifier.height(16.dp))

                // ── Location rows (unchanged) ─────────────────────────────────
                LocationRow(
                    label = uiState.startLocationName,
                    icon = Icons.Default.Circle,
                    iconColor = Color(0xFF4285F4),
                    trailingIcon = Icons.Default.ImportExport,
                    onTrailingIconClick = onSwapClick,
                    onClick = onStartClick
                )

                Divider(
                    modifier = Modifier.padding(start = 40.dp),
                    thickness = 0.5.dp,
                    color = Color.LightGray
                )

                LocationRow(
                    label = uiState.destinationName,
                    icon = Icons.Default.LocationOn,
                    iconColor = Color(0xFFEA4335),
                    trailingIcon = Icons.Default.Menu,
                    onClick = onDestinationClick
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── Shuttle status card (US-2.7 + US-2.8) ────────────────────
                // Fades in when shuttle mode is selected, fades out otherwise.
                AnimatedVisibility(
                    visible = uiState.selectedTransportMode == "shuttle",
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column {
                        ShuttleStatusCard(
                            availability      = uiState.shuttleAvailability,
                            statusMessage     = uiState.shuttleStatusMessage,
                            nearestStopName   = uiState.nearestShuttleStopName,
                            nearestStopCampus = uiState.nearestShuttleStopCampus
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // ── ETA card (hidden when shuttle mode is active) ─────────────
                AnimatedVisibility(
                    visible = uiState.selectedTransportMode != "shuttle",
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column {
                        // --- ADDED FALLBACK LOGIC START ---
                        if (uiState.routeErrorMessage != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFFCE8E6) // Light Red Error background
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Error",
                                        tint = Color(0xFFC5221F) // Dark Red
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = uiState.routeErrorMessage!!,
                                        color = Color(0xFFC5221F),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                            }
                        } else {
                            // --- YOUR EXISTING ETA CARD START ---
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFF1F3F4)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = uiState.routeDuration,
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = "Fastest route · ${uiState.routeDistance}",
                                            color = Color.Gray,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = when (uiState.selectedTransportMode) {
                                                "drive"   -> Icons.Default.DirectionsCar
                                                "walk"    -> Icons.AutoMirrored.Filled.DirectionsWalk
                                                else      -> Icons.Default.DirectionsBus
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.padding(10.dp),
                                            tint = Color(0xFF5F6368)
                                        )
                                    }
                                }
                            }
                            // --- YOUR EXISTING ETA CARD END ---
                        }
                        // --- ADDED FALLBACK LOGIC END ---

                        Spacer(Modifier.height(16.dp))
                    }
                }

                // ── CTA button ────────────────────────────────────────────────
                // Disabled when shuttle is selected but currently out of service.
                val isCurrentModeInvalid = when (uiState.selectedTransportMode) {
                    "shuttle" -> uiState.shuttleAvailability !is ShuttleAvailability.Active
                    else -> uiState.routeErrorMessage != null || uiState.routePoints.isEmpty()
                }
                Button(
                    onClick = onStartNavigation,
                    enabled = !isCurrentModeInvalid,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF912338), // Concordia Maroon
                        disabledContainerColor = Color(0xFFBDBDBD)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(

                        text = when {
                            uiState.selectedTransportMode == "shuttle" && isCurrentModeInvalid ->
                                "SHUTTLE OUT OF SERVICE"

                            isCurrentModeInvalid ->
                                "UNAVAILABLE"
                            uiState.startLocationName == "Your position"  -> "START"
                            else                                           -> "PREVIEW"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ── LocationRow ───────────────────────────────────────────────────────────────

@Composable
fun LocationRow(
    label: String,
    icon: ImageVector,
    iconColor: Color,
    trailingIcon: ImageVector = Icons.Default.Menu,
    onTrailingIconClick: () -> Unit = {},
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onTrailingIconClick) {
            Icon(trailingIcon, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(22.dp))
        }
    }
}

// ── TransportModeToggle ───────────────────────────────────────────────────────

@Composable
fun TransportModeToggle(selectedMode: String, onModeChange: (String) -> Unit) {
    Surface(
        color = Color(0xFFF1F3F4),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val modes = listOf(
                Triple("walk",    Icons.AutoMirrored.Filled.DirectionsWalk, "Walk"),
                Triple("drive",   Icons.Default.DirectionsCar,              "Drive"),
                Triple("transit", Icons.Default.DirectionsBus,              "Transit"),
                Triple("shuttle", Icons.Default.AirportShuttle,                     "Shuttle")  // ← NEW (US-2.6)
            )

            modes.forEach { (mode, icon, label) ->
                val isSelected = selectedMode == mode
                Surface(
                    modifier = Modifier
                        .height(40.dp)
                        .weight(1f)
                        .clickable { onModeChange(mode) },
                    color = if (isSelected) Color(0xFF1A73E8) else Color.Transparent,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) Color.White else Color(0xFF5F6368),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
