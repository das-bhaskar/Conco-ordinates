package com.example.myapplication.logic

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.example.myapplication.telemetry.CrashReporter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng

/**
 * Stateless location utility functions.
 *
 * These functions are context-agnostic — they accept only primitive/interface
 * parameters and do not cast Context to Activity (PR review: unsafe cast removed).
 * The caller is responsible for computing [shouldShowRationale] using
 * ActivityCompat.shouldShowRequestPermissionRationale() before calling here.
 */

/**
 * Handles a "recenter" tap:
 * - If permission is missing and [shouldShowRationale] → calls [onShowSettings]
 * - If permission is missing and no rationale needed → launches permission request
 * - If permission is granted → fetches last known location and calls [onLocationFound]
 *
 * @param shouldShowRationale Pre-computed by the caller via
 *   ActivityCompat.shouldShowRequestPermissionRationale(). Passing this as a
 *   parameter avoids unsafe Context → ComponentActivity casting inside a utility.
 */
fun handleRecenter(
    client: FusedLocationProviderClient,
    hasPermission: Boolean,
    shouldShowRationale: Boolean,
    launcher: ActivityResultLauncher<String>,
    context: Context,
    onShowSettings: () -> Unit,
    onLocationFound: (LatLng) -> Unit
) {
    if (!hasPermission) {
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
