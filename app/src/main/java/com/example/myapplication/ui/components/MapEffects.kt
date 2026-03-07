package com.example.myapplication.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.example.myapplication.map.TrueCameraController
import com.example.myapplication.telemetry.CrashReporter
import com.example.myapplication.ui.viewmodel.MapViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay

/**
 * Map-related side-effect composables.
 *
 * Extracted from MapsActivity — none of these functions depend on Activity
 * lifecycle or state. Grouping them here keeps MapScreen lean and makes
 * each effect easy to find and reason about independently.
 */

/**
 * Requests periodic location updates from [fusedLocationClient] while
 * [hasLocationPermission] is true, forwarding results to the ViewModel.
 */
@Composable
fun ObserveLocationUpdates(
    hasLocationPermission: Boolean,
    fusedLocationClient: FusedLocationProviderClient,
    viewModel: MapViewModel
) {
    LaunchedEffect(hasLocationPermission) {
        CrashReporter.setKey("location_permission_granted", hasLocationPermission)
        if (!hasLocationPermission) return@LaunchedEffect

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000
        ).build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    viewModel.processLocationUpdate(LatLng(loc.latitude, loc.longitude))
                }
            }
        }
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, android.os.Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            CrashReporter.recordNonFatal(e, "request_location_updates_failed")
        }
    }
}

/**
 * Creates and remembers a [CameraPositionState] initialised to the SGW campus centre.
 */
@Composable
fun rememberMapCamera(): CameraPositionState {
    return rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(45.497, -73.579), 16f)
    }
}

/**
 * Observes ViewModel state changes that require camera animations:
 * - Route bounds → fit camera to route
 * - Campus change → animate to campus centre
 * - Map mode change → log to crash reporter
 */
@Composable
fun ObserveCameraEffects(
    cameraPositionState: CameraPositionState,
    cameraController: TrueCameraController,
    viewModel: MapViewModel
) {
    LaunchedEffect(viewModel.uiBuildingState.routeBounds) {
        val bounds = viewModel.uiBuildingState.routeBounds
        if (bounds != null) {
            delay(500)
            cameraPositionState.animate(
                update     = CameraUpdateFactory.newLatLngBounds(bounds, 400),
                durationMs = 1000
            )
        }
    }
    LaunchedEffect(viewModel.currentCampus) {
        viewModel.currentCampus?.let { campus ->
            CrashReporter.setKey("selected_campus", campus.name)
            cameraController.animateTo(campus.getGoogleCenter(), campus.defaultZoom)
        }
    }
    LaunchedEffect(viewModel.uiBuildingState.mode) {
        CrashReporter.setKey("map_mode", viewModel.uiBuildingState.mode.name)
    }
}
