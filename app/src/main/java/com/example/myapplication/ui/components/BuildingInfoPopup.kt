package com.example.myapplication.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplication.data.Building
import com.example.myapplication.ui.models.BuildingUiState
import com.example.myapplication.ui.theme.ConcordiaGreen
import com.example.myapplication.ui.theme.ConcordiaMaroon
import com.example.myapplication.ui.theme.concordiaGold
import com.example.myapplication.ui.theme.faintMaroon

@Composable
fun BuildingInfoPopup(
    building:          Building,
    uiState:           BuildingUiState,
    onDismiss:         () -> Unit,
    onDirectionsClick: () -> Unit,
    onIndoorMapClick:  () -> Unit = {},
    onInfoClick:       () -> Unit = {}
) {

    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier  = Modifier.fillMaxWidth().padding(12.dp).animateContentSize(),
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column {
                Box {
                    AsyncImage(
                        model              = uiState.imageUrl,
                        contentDescription = null,
                        modifier           = Modifier.fillMaxWidth().height(180.dp),
                        contentScale       = ContentScale.Crop
                    )
                    IconButton(
                        onClick  = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Close,
                            contentDescription = "Close",
                            tint               = Color.White,
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                }
                Column(
                    modifier            = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = building.name, style = MaterialTheme.typography.headlineSmall)

                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ActionButton(Icons.Default.Directions, "Directions", onDirectionsClick)
                        if (uiState.hasIndoorMap) {
                            ActionButton(Icons.Default.Map, "Indoor", onIndoorMapClick)
                        }
                        ActionButton(
                            icon    = if (uiState.isInfoExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.Info,
                            label   = "Info",
                            onClick = onInfoClick
                        )
                        ActionButton(Icons.Default.PinDrop, "PIN")   {}
                        ActionButton(Icons.Default.Share,   "Share") {}
                    }

                    HorizontalDivider(
                        modifier  = Modifier.padding(vertical = 8.dp),
                        thickness = 1.dp,
                        color     = Color.LightGray
                    )
                    Row(
                        modifier          = Modifier.padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Blue)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text  = uiState.address ?: "Address loading...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // ── Inline info panel ─────────────────────────────────────────
                    AnimatedVisibility(visible = uiState.isInfoExpanded) {
                        Column(
                            modifier            = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            HorizontalDivider(color = faintMaroon, thickness = 1.dp)

                            // About
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text          = "About",
                                    color         = ConcordiaMaroon,
                                    fontWeight    = FontWeight.SemiBold,
                                    fontSize      = 11.sp,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text       = building.description ?: "No description available.",
                                    style      = MaterialTheme.typography.bodyMedium,
                                    color      = Color(0xFF2C2C2C),
                                    lineHeight = 22.sp
                                )
                            }

                            HorizontalDivider(color = faintMaroon, thickness = 1.dp)

                            // Opening hours
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text          = "Opening Hours",
                                    color         = ConcordiaMaroon,
                                    fontWeight    = FontWeight.SemiBold,
                                    fontSize      = 11.sp,
                                    letterSpacing = 1.sp
                                )
                                Row(
                                    verticalAlignment     = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier         = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(faintMaroon),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector        = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint               = ConcordiaMaroon,
                                            modifier           = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text       = building.openingHours ?: "Hours not available",
                                        style      = MaterialTheme.typography.bodySmall,
                                        color      = Color(0xFF555555),
                                        lineHeight = 20.sp
                                    )
                                }
                            }

                            HorizontalDivider(color = faintMaroon, thickness = 1.dp)

                            // Access
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text          = "Access",
                                    color         = ConcordiaMaroon,
                                    fontWeight    = FontWeight.SemiBold,
                                    fontSize      = 11.sp,
                                    letterSpacing = 1.sp
                                )
                                val wcColor = if (building.isWheelchairAccessible) ConcordiaGreen else Color(0xFF9E9E9E)
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier         = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(wcColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector        = Icons.Default.Accessible,
                                            contentDescription = null,
                                            tint               = wcColor,
                                            modifier           = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text       = if (building.isWheelchairAccessible) "Wheelchair Accessible" else "Limited Accessibility",
                                        style      = MaterialTheme.typography.bodySmall,
                                        color      = wcColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                val tnColor = if (building.hasTunnelAccess) ConcordiaMaroon else Color(0xFF9E9E9E)
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier         = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(tnColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector        = Icons.Default.Subway,
                                            contentDescription = null,
                                            tint               = tnColor,
                                            modifier           = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text       = if (building.hasTunnelAccess) "Underground Tunnel Connected" else "No Underground Tunnel",
                                        style      = MaterialTheme.typography.bodySmall,
                                        color      = tnColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    val concordiaBlue = Color(0xFF1652f0)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(
            onClick = onClick,
            colors  = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = concordiaBlue.copy(alpha = 0.1f),
                contentColor   = concordiaBlue
            )
        ) {
            Icon(icon, contentDescription = label)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = concordiaBlue)
    }
}
