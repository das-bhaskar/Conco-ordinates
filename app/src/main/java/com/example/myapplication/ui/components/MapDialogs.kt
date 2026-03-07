package com.example.myapplication.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.myapplication.ui.theme.ConcordiaMaroon

/**
 * Location permission rationale dialog.
 *
 * Shown when the user taps Recenter but has previously denied location permission,
 * guiding them to the system settings to re-enable it.
 *
 * Extracted from MapsActivity — has no Activity or Context dependency.
 */
@Composable
fun LocationPermissionDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Location Required") },
        text    = { Text("To see which building you are in, please enable location permissions in the app settings.") },
        confirmButton = {
            Button(
                onClick = onOpenSettings,
                colors  = ButtonDefaults.buttonColors(containerColor = ConcordiaMaroon)
            ) { Text("OPEN SETTINGS") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.Gray) }
        },
        icon = {
            Icon(Icons.Default.MyLocation, contentDescription = null, tint = ConcordiaMaroon)
        }
    )
}
