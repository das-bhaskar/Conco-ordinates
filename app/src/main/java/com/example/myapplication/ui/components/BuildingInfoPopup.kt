package com.example.myapplication.ui.components

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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.myapplication.data.Building
import com.example.myapplication.ui.models.BuildingUiState
import com.example.myapplication.ui.theme.ConcordiaMaroon
import com.example.myapplication.ui.theme.concordiaGold
import com.example.myapplication.ui.theme.faintMaroon

@Composable
fun BuildingInfoPopup(
    building:          Building,
    uiState:           BuildingUiState,
    onDismiss:         () -> Unit,
    onDirectionsClick: () -> Unit,
    onIndoorMapClick:  () -> Unit = {}
) {
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        Dialog(onDismissRequest = { showInfoDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
            ) {
                // ── Maroon gradient header ────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ConcordiaMaroon)
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Building code badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(concordiaGold.copy(alpha = 0.25f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text       = building.code,
                                color      = concordiaGold,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 11.sp,
                                letterSpacing = 1.5.sp
                            )
                        }
                        Text(
                            text       = building.name,
                            color      = Color.White,
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 26.sp
                        )
                        // Gold accent divider
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(concordiaGold)
                        )
                    }
                }

                // ── Scrollable body ───────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Description section
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text       = "About",
                            color      = ConcordiaMaroon,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text  = building.description ?: "No description available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2C2C2C),
                            lineHeight = 22.sp
                        )
                    }

                    HorizontalDivider(color = faintMaroon, thickness = 1.dp)

                    // Address row
                    Row(
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier          = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(faintMaroon),
                            contentAlignment  = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint               = ConcordiaMaroon,
                                modifier           = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text  = building.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF555555)
                        )
                    }

                    // Close button
                    Button(
                        onClick  = { showInfoDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = ConcordiaMaroon,
                            contentColor   = Color.White
                        )
                    ) {
                        Text("Close", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
            }
        }
    }

    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier  = Modifier.fillMaxWidth().padding(12.dp),
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column {
                AsyncImage(
                    model              = uiState.imageUrl,
                    contentDescription = null,
                    modifier           = Modifier.fillMaxWidth().height(180.dp),
                    contentScale       = ContentScale.Crop
                )
                Column(
                    modifier            = Modifier.padding(16.dp),
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
                        ActionButton(Icons.Default.Info,    "Info")  { showInfoDialog = true }
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
