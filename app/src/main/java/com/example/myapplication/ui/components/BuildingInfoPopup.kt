package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.Building
import com.example.myapplication.ui.models.BuildingUiState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
fun BuildingInfoPopup(
    building: Building,
    uiState: BuildingUiState,
    onDismiss: () -> Unit,
    onDirectionsClick: () -> Unit // Added this
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column {
                AsyncImage(
                    model = uiState.imageUrl ?: "https://your-placeholder-url.com/campus.jpg",
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentScale = ContentScale.Crop
                )

                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = building.name, style = MaterialTheme.typography.headlineSmall)

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Wired the click here
                        ActionButton(Icons.Default.Directions, "Directions", onDirectionsClick)
                        ActionButton(Icons.Default.Save, "Save") {}
                        ActionButton(Icons.Default.PinDrop, "PIN") {}
                        ActionButton(Icons.Default.Share, "Share") {}
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp, color = Color.LightGray)
                    Row(modifier = Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Blue)
                        Spacer(Modifier.width(8.dp))
                        Text(text = uiState.address ?: "Address loading...", style = MaterialTheme.typography.bodyMedium)
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
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = concordiaBlue.copy(alpha = 0.1f),
                contentColor = concordiaBlue
            )
        ) {
            Icon(icon, contentDescription = label)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = concordiaBlue)
    }
}