package com.example.myapplication

import android.Manifest
import com.example.myapplication.ui.components.BuildingInfoPopup
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.logic.TrueLocationProvider
import com.example.myapplication.map.TrueCameraController
import com.example.myapplication.ui.components.CampusMap
import com.example.myapplication.ui.components.CampusToggle
import com.example.myapplication.ui.theme.ConcordiaMaroon
import com.example.myapplication.ui.viewmodel.MapViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

class MapsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (!com.google.android.libraries.places.api.Places.isInitialized()) {
            com.google.android.libraries.places.api.Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }
        super.onCreate(savedInstanceState)
        CampusRepo.initialize(this)

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationProvider = TrueLocationProvider(fusedLocationClient)

        val masterViewModel = MapViewModel(locationProvider)

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var showSettingsDialog by remember { mutableStateOf(false) }

            val viewModel = masterViewModel

            var hasLocationPermission by remember {
                mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            }

            LaunchedEffect(hasLocationPermission) {
                if (hasLocationPermission) {
                    val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                        5000
                    ).build()

                    val locationCallback = object : com.google.android.gms.location.LocationCallback() {
                        override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                            result.lastLocation?.let { loc ->
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

            val cameraController = remember(cameraPositionState) {
                TrueCameraController(cameraPositionState)
            }

            LaunchedEffect(viewModel.currentCampus) {
                viewModel.currentCampus?.let { campus ->
                    cameraController.animateTo(campus.getGoogleCenter(), campus.defaultZoom)
                }
            }

            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                hasLocationPermission = isGranted
            }

            Box(modifier = Modifier.fillMaxSize()) {
                CampusMap(
                    currentCampus = viewModel.currentCampus,
                    highlightedBuildingName = viewModel.highlightedBuildingName,
                    cameraPositionState = cameraPositionState,
                    hasLocationPermission = hasLocationPermission,
                    viewModel = viewModel,
                    modifier = Modifier.testTag("campus_map")
                )

                if (showSettingsDialog && !hasLocationPermission) {
                    AlertDialog(
                        onDismissRequest = { showSettingsDialog = false },
                        title = { Text("Location Required") },
                        text = { Text("To see which building you are in, please enable location permissions in the app settings.") },
                        confirmButton = {
                            Button(
                                onClick = { openAppSettings(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = ConcordiaMaroon)
                            ) {
                                Text("OPEN SETTINGS")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSettingsDialog = false }) {
                                Text("CANCEL", color = Color.Gray)
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = ConcordiaMaroon
                            )
                        }


                    )
                }

                if (viewModel.uiBuildingState.isVisible) {
                    viewModel.uiBuildingState.building?.let { building ->
                        com.example.myapplication.ui.components.BuildingInfoPopup(
                            building = building,
                            uiState = viewModel.uiBuildingState,
                            onDismiss = { viewModel.handleMapTap(null) } // Reset state to hide it
                        )
                    }
                }
                CampusToggle(
                    selectedCampusName = viewModel.currentCampus?.name,
                    onCampusClick = { name ->
                        viewModel.onCampusSelected(name)
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 160.dp)
                )

                ExtendedFloatingActionButton(
                    onClick = {
                        handleRecenter(
                            client = fusedLocationClient,
                            hasPermission = hasLocationPermission,
                            launcher = launcher,
                            context = context,
                            onShowSettings = { showSettingsDialog = true }
                        ) { userLocation ->
                            scope.launch {
                                cameraController.animateTo(userLocation, 18.5f)
                            }
                            viewModel.processLocationUpdate(userLocation, isForce = true)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp),
                    containerColor = ConcordiaMaroon,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.MyLocation, contentDescription = null) },
                    text = { Text(text = "RECENTER") }
                )

                if (viewModel.uiBuildingState.isVisible) {
                    viewModel.uiBuildingState.building?.let { building ->
                        BuildingInfoPopup(
                            building = building,
                            uiState = viewModel.uiBuildingState,
                            onDismiss = { viewModel.handleMapTap(null) }
                        )
                    }
                }
            }
            }
        }


    private fun handleRecenter(
        client: FusedLocationProviderClient,
        hasPermission: Boolean,
        launcher: androidx.activity.result.ActivityResultLauncher<String>,
        context: android.content.Context,
        onShowSettings: () -> Unit,
        onLocationFound: (LatLng) -> Unit
    ) {
        if (!hasPermission) {
            val activity = context as? androidx.activity.ComponentActivity
            val shouldShowRationale = activity?.let {
                androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION)
            } ?: false

            if (shouldShowRationale) {
                // They've denied it before, show the "Go to Settings" box
                onShowSettings()
            } else {
                // First time or system can still show the popup
                launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            return
        }
        try {
            client.lastLocation.addOnSuccessListener { loc ->
                loc?.let { onLocationFound(LatLng(it.latitude, it.longitude)) }
            }
        } catch (e: SecurityException) { /* log error */ }
    }

    private fun openAppSettings(context: android.content.Context) {
        val intent = android.content.Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts("package", context.packageName, null)
        )
        context.startActivity(intent)
    }

}