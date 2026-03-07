package com.example.myapplication.logic

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import com.example.myapplication.telemetry.CrashReporter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng

/**
 * Stateless location utility functions.
 *
 * Extracted from MapsActivity — neither function accesses Activity members
 * or Composable state, making them safe to call from anywhere.
 */

/**
 * Handles a "recenter" tap:
 * - If permission is missing and rationale should be shown → calls [onShowSettings]
 * - If permission is missing and no rationale needed → launches permission request
 * - If permission is granted → fetches last known location and calls [onLocationFound]
 */
fun handleRecenter(
    client: FusedLocationProviderClient,
    hasPermission: Boolean,
    launcher: ActivityResultLauncher<String>,
    context: Context,
    onShowSettings: () -> Unit,
    onLocationFound: (LatLng) -> Unit
) {
    if (!hasPermission) {
        val activity = context as? ComponentActivity
        val shouldShowRationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(
                it, Manifest.permission.ACCESS_FINE_LOCATION
            )
        } ?: false
        if (shouldShowRationale) onShowSettings()
        else launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        return
    }
    try {
        client.lastLocation.addOnSuccessListener { loc ->
            loc?.let { onLocationFound(LatLng(it.latitude, it.longitude)) }
        }
    } catch (e: SecurityException) {
        CrashReporter.setKey("recenter_has_permission", hasPermission)
        CrashReporter.recordNonFatal(e, "recenter_location_lookup_failed")
    }
}

/**
 * Opens the system app settings page for this app so the user can
 * manually re-enable a previously denied permission.
 */
fun openAppSettings(context: Context) {
    val intent = Intent(
        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        android.net.Uri.fromParts("package", context.packageName, null)
    )
    context.startActivity(intent)
}
