package com.example.myapplication.shuttle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.ShuttleAvailability

/**
 * ShuttleStatusCard – displays shuttle availability (US-2.7) and nearest
 * stop information (US-2.8) inside [DirectionsInfoPopup].
 *
 * Stateless Composable: all data comes in as parameters, making it fully
 * unit/screenshot-testable without an Android emulator.
 *
 * @param availability  result of [ShuttleService.checkAvailability]
 * @param statusMessage human-readable string from [ShuttleService.statusMessage]
 * @param nearestStopName    display name of the closest shuttle stop
 * @param nearestStopCampus  "SGW" or "Loyola" – used for the route badge
 */
@Composable
fun ShuttleStatusCard(
    availability: ShuttleAvailability,
    statusMessage: String,
    nearestStopName: String,
    nearestStopCampus: String,
    routeDuration: String,
    modifier: Modifier = Modifier
) {
    val isActive   = availability is ShuttleAvailability.Active
    val cardColor  = if (isActive) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
    val accentColor = if (isActive) Color(0xFF2E7D32) else Color(0xFFE65100)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardColor
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Row 1: service status icon + message ──────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isActive) Icons.Default.DirectionsBus else Icons.Default.Info,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                )
            }

            // ── Rows 2-3: only shown when shuttle is active ───────────────────
            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn() + expandVertically()
            ) {
                Column {
                    Spacer(Modifier.height(10.dp))

                    // Nearest stop row (US-2.8)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF5F6368),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Nearest stop: $nearestStopName",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF3C4043)
                            )
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Route badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accentColor.copy(alpha = 0.12f)
                    ) {
                        // Calculate total time: Wait Time + Travel Time
                        val waitTime = if (availability is ShuttleAvailability.Active) availability.nextDepartureMinutes else 0
                        val direction = if (nearestStopCampus == "SGW") "SGW → Loyola" else "Loyola → SGW"

                        Text(
                            text = "$direction · $routeDuration (+$waitTime min wait)",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
