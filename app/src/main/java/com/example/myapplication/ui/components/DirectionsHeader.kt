package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.models.BuildingUiState

@Composable
fun DirectionsHeader(
    uiState: BuildingUiState,
    onBackClick: () -> Unit,
    onStartQueryChange: (String) -> Unit,
    onDestQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("dir_header"),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick, modifier = Modifier.testTag("dir_header_back")) { Icon(Icons.Default.ArrowBack, null) }
                Text("Directions", style = MaterialTheme.typography.titleMedium)
            }

            // Start Field
            SearchInput(
                label = "Starting point",
                value = uiState.startLocationName,
                onValueChange = { onStartQueryChange(it) },
                icon = Icons.Default.MyLocation,
                iconColor = Color.Blue,
                testTag = "dir_start"
            )

            Spacer(Modifier.height(8.dp))

            // Destination Field
            SearchInput(
                label = "Destination",
                value = uiState.destinationName,
                onValueChange = { onDestQueryChange(it) },
                icon = Icons.Default.LocationOn,
                iconColor = Color.Red,
                testTag = "dir_dest"
            )
        }
    }
}
@Composable
private fun SearchInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    testTag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).testTag(testTag),
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = iconColor) },
        shape = RoundedCornerShape(8.dp),
        singleLine = true,
        colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFFF5F5F5))
    )
}