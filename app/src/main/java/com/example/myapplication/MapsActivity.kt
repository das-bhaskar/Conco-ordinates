package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.ui.components.CampusMap
import com.example.myapplication.ui.components.CampusToggle
import com.example.myapplication.ui.theme.ConcordiaMaroon
import com.example.myapplication.ui.viewmodel.MapViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

class MapsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CampusRepo.initialize(this)
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val viewModel: MapViewModel = viewModel()


            // Fix: Explicitly check permission and store as Boolean
            var hasLocationPermission by remember {
                mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            }

            LaunchedEffect(hasLocationPermission) {
                if (hasLocationPermission) {
                    val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                        5000 // Check every 5 seconds
                    ).build()

                    val locationCallback = object : com.google.android.gms.location.LocationCallback() {
                        override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                            result.lastLocation?.let { loc ->
                                // AUTOMATICALLY update the ViewModel as the user walks
                                android.util.Log.d("MAP_DEBUG", "ACTIVITY: Sending location to ViewModel")
                                viewModel.processLocationUpdate(LatLng(loc.latitude, loc.longitude))
                            }
                        }
                    }

                    try {
                        fusedLocationClient.requestLocationUpdates(
                            locationRequest,
                            locationCallback,
                            android.os.Looper.getMainLooper()
                        )
                    } catch (e: SecurityException) { /* Handle error */ }
                }
            }

            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(LatLng(45.497, -73.579), 16f)
            }

            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                hasLocationPermission = isGranted // Fix: Launcher result is a Boolean
            }

            Box(modifier = Modifier.fillMaxSize()) {
                CampusMap(
                    currentCampus = viewModel.currentCampus,
                    highlightedBuildingName = viewModel.highlightedBuildingName,
                    cameraPositionState = cameraPositionState,
                    hasLocationPermission = hasLocationPermission
                )

                CampusToggle(
                    selectedCampusName = viewModel.currentCampus?.name,
                    onCampusClick = { name ->
                        viewModel.onCampusSelected(name)
                        viewModel.currentCampus?.let { campus ->
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(campus.getGoogleCenter(), campus.defaultZoom),
                                    1000
                                )
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 160.dp)
                )

                FloatingActionButton(
                    onClick = {
                        // Fix: Call the local helper function correctly
                        handleRecenter(fusedLocationClient, hasLocationPermission, launcher) { userLocation ->
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(userLocation, 18.5f),
                                    1200
                                )
                            }
                            viewModel.processLocationUpdate(userLocation)
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                    containerColor = ConcordiaMaroon,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null)
                }
            }
        }
    }

    // Fix: Ensure these helpers are INSIDE the class or properly defined globally
    private fun handleRecenter(
        client: FusedLocationProviderClient,
        hasPermission: Boolean,
        launcher: androidx.activity.result.ActivityResultLauncher<String>,
        onLocationFound: (LatLng) -> Unit
    ) {
        if (!hasPermission) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        try {
            client.lastLocation.addOnSuccessListener { loc ->
                loc?.let { onLocationFound(LatLng(it.latitude, it.longitude)) }
            }
        } catch (e: SecurityException) { /* log error */ }
    }
}