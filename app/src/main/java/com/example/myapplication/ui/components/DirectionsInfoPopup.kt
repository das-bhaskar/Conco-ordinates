package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.ConcordiaMaroon
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.example.myapplication.ui.models.BuildingUiState
import com.example.myapplication.ui.viewmodel.ShuttleViewModel

@Composable
fun DirectionsInfoPopup(
    uiState: BuildingUiState,
    shuttleViewModel: ShuttleViewModel,
    onModeChange: (String) -> Unit,
    onStartClick: () -> Unit,
    onSwapClick: () -> Unit,
    onDestinationClick: () -> Unit,
    onClose: () -> Unit,
    onStartNavigation: () -> Unit,
    bottomNavHeight: androidx.compose.ui.unit.Dp = 80.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = bottomNavHeight)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = Color.White.copy(alpha = 0.92f),
            shadowElevation = 10.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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

                TransportModeToggle(uiState.selectedTransportMode, onModeChange)

                Spacer(modifier = Modifier.height(16.dp))

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

                // Conditional content based on transport mode
                if (uiState.selectedTransportMode == "shuttle") {
                    ShuttleInfoSection(shuttleViewModel, onStartNavigation)
                } else {
                    RouteInfoSection(uiState, onStartNavigation)
                }
            }
        }
    }
}

@Composable
private fun RouteInfoSection(uiState: BuildingUiState, onStartNavigation: () -> Unit) {
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
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
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
                        else      -> Icons.Default.DirectionsBus // Covers transit
                    },
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = Color(0xFF5F6368)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Button(
        onClick = onStartNavigation,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)), // Google Blue
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = if (uiState.startLocationName == "Your position") "START" else "PREVIEW",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
    }
}

@Composable
private fun ShuttleInfoSection(viewModel: ShuttleViewModel, onStartNavigation: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (viewModel.isShuttleEnabled) ConcordiaMaroon.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (viewModel.isLoadingRoute) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), color = ConcordiaMaroon)
                    Text(
                        text = "Finding nearest stop...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ConcordiaMaroon
                    )
                } else {
                    Text(
                        text = viewModel.shuttleStatusText,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (viewModel.isShuttleEnabled) ConcordiaMaroon else Color.DarkGray
                    )
                    Text(
                        // ALWAYS show the direction, so the user knows which schedule they are looking at
                        text = viewModel.currentDirection.displayName,
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.DirectionsBus,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (viewModel.isShuttleEnabled) ConcordiaMaroon else Color.Gray
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Button(
        onClick = onStartNavigation,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ConcordiaMaroon),
        shape = RoundedCornerShape(16.dp),
        enabled = viewModel.shuttleRoute != null
    ) {
        Text(
            text = "SHOW ROUTE",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
    }
}

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
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onTrailingIconClick) {
            Icon(trailingIcon, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(22.dp))
        }
    }
}

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
                Triple("shuttle", Icons.Default.DirectionsBus,              "Shuttle")
            )

            modes.forEach { (mode, icon, label) ->
                val isSelected = selectedMode == mode
                Surface(
                    modifier = Modifier
                        .height(40.dp)
                        .weight(1f)
                        .clickable { onModeChange(mode) },
                    color = when {
                        isSelected && mode == "shuttle" -> ConcordiaMaroon
                        isSelected                     -> Color(0xFF1A73E8)
                        else                           -> Color.Transparent
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (mode == "shuttle") {
                            Text("🚌", style = MaterialTheme.typography.labelSmall)
                        } else {
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
}
