package com.example.myapplication

import android.Manifest
import com.example.myapplication.ui.components.BuildingInfoPopup
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
import android.content.Intent
import com.example.myapplication.ui.components.DirectionsInfoPopup
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.logic.SearchResult
import com.example.myapplication.logic.TrueLocationProvider
import com.example.myapplication.map.TrueCameraController
import com.example.myapplication.ui.components.CampusMap
import com.example.myapplication.ui.components.CampusToggle
import com.example.myapplication.ui.components.DirectionsHeader
import com.example.myapplication.ui.models.MapUIMode
import com.example.myapplication.telemetry.CrashReporter
import com.example.myapplication.ui.theme.ConcordiaMaroon
import com.example.myapplication.ui.viewmodel.MapViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import com.smartlook.android.core.api.Smartlook

class MapsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (!com.google.android.libraries.places.api.Places.isInitialized()) {
            com.google.android.libraries.places.api.Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }
        super.onCreate(savedInstanceState)

        if (BuildConfig.SMARTLOOK_PROJECT_KEY.isBlank()) {
            Log.w("Smartlook", "SMARTLOOK_PROJECT_KEY is empty, Smartlook is not started.")
        } else {
            val smartlook = Smartlook.instance
            smartlook.preferences.projectKey = BuildConfig.SMARTLOOK_PROJECT_KEY
            smartlook.start()
            if (BuildConfig.SMARTLOOK_TESTER_ID.isNotBlank()) {
                smartlook.user.identifier = BuildConfig.SMARTLOOK_TESTER_ID
            }
        }

        Smartlook.instance.trackNavigationEnter("MapsActivity")

        CrashReporter.setKey("screen", "MapsActivity")
        CrashReporter.setKey("app_version", BuildConfig.VERSION_NAME)
        CrashReporter.log("maps_activity_created")
        CampusRepo.initialize(this)

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationProvider = TrueLocationProvider(fusedLocationClient)
        val routeProvider = com.example.myapplication.logic.GoogleRouteProvider(BuildConfig.MAPS_API_KEY)

        val viewModel = MapViewModel(locationProvider, routeProvider)

        val placesClient = com.google.android.libraries.places.api.Places.createClient(this)
        viewModel.initSearch(placesClient)

        setContent {
            val mapPaddingBottom = if (viewModel.uiBuildingState.mode == MapUIMode.DIRECTIONS) 600 else 0
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var showSettingsDialog by remember { mutableStateOf(false) }
            var previousModeNavigation by remember { mutableStateOf<String?>(null) }

            var hasLocationPermission by remember {
                mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            }

            LaunchedEffect(hasLocationPermission) {
                CrashReporter.setKey("location_permission_granted", hasLocationPermission)
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
                    } catch (e: SecurityException) {
                        CrashReporter.recordNonFatal(e, "request_location_updates_failed")
                    }
                }
            }


            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(LatLng(45.497, -73.579), 16f)
            }

            val cameraController = remember(cameraPositionState) {
                TrueCameraController(cameraPositionState)
            }


            LaunchedEffect(viewModel.uiBuildingState.routeBounds) {
                val bounds = viewModel.uiBuildingState.routeBounds
                if (bounds != null) {
                    kotlinx.coroutines.delay(500)
                    cameraPositionState.animate(
                        update = com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, 400),
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
                val modeNavigation = "map_mode_${viewModel.uiBuildingState.mode.name.lowercase()}"
                previousModeNavigation
                    ?.takeIf { it != modeNavigation }
                    ?.let { Smartlook.instance.trackNavigationExit(it) }
                if (previousModeNavigation != modeNavigation) {
                    Smartlook.instance.trackNavigationEnter(modeNavigation)
                }
                previousModeNavigation = modeNavigation
            }

            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                hasLocationPermission = isGranted
            }

            LaunchedEffect(viewModel.uiBuildingState.routeBounds) {
                val bounds = viewModel.uiBuildingState.routeBounds
                if (bounds != null) {
                    kotlinx.coroutines.delay(500)
                    cameraPositionState.animate(
                        update = com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, 150),
                        durationMs = 1000
                    )
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                CampusMap(
                    currentCampus = viewModel.currentCampus,
                    highlightedBuildingName = viewModel.highlightedBuildingName,
                    cameraPositionState = cameraPositionState,
                    hasLocationPermission = hasLocationPermission,
                    viewModel = viewModel,
                    contentPadding = PaddingValues(bottom = mapPaddingBottom.dp),
                    modifier = Modifier.testTag("campus_map")
                )

                if (viewModel.uiBuildingState.mode == MapUIMode.PREVIEW) {
                    com.example.myapplication.ui.components.CampusSearchBar(
                        query = viewModel.searchQuery,
                        results = viewModel.searchResults,
                        onQueryChange = { viewModel.onSearchQueryChanged(it) },
                        onResultClick = { viewModel.handleSearchResult(it, context) },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                    )
                } else {
                    if (viewModel.uiBuildingState.isSearchExpanded) {
                        DirectionsHeader(
                            uiState = viewModel.uiBuildingState,
                            onBackClick = { viewModel.toggleSearchExpansion(false) },
                            onStartQueryChange = { viewModel.onSearchQueryChanged(it, field = "start") },
                            onDestQueryChange = { viewModel.onSearchQueryChanged(it, field = "dest") },
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }

                    if (viewModel.uiBuildingState.mode == MapUIMode.DIRECTIONS) {
                        DirectionsInfoPopup(
                            uiState = viewModel.uiBuildingState,
                            onModeChange = { viewModel.onTransportModeChanged(it) },
                            onStartClick = { viewModel.toggleSearchExpansion(true, "start") },
                            onDestinationClick = { viewModel.toggleSearchExpansion(true, "dest") },
                            onSwapClick = { viewModel.swapLocations() },
                            onClose = { viewModel.onBackToPreview() },
                            onStartNavigation = { /* ... */ },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }

                    val currentFieldText = when(viewModel.activeSearchField) {
                        "start" -> viewModel.uiBuildingState.startLocationName
                        "dest" -> viewModel.uiBuildingState.destinationName
                        else -> viewModel.searchQuery
                    }

                    if (viewModel.activeSearchField != "main" && currentFieldText.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .offset(y = 190.dp) // Adjusted slightly to sit below the DirectionsHeader
                                .zIndex(1f), // Ensure it sits on top of everything
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                                items(viewModel.searchResults) { result ->
                                    val title = when(result) {
                                        is com.example.myapplication.logic.SearchResult.BuildingResult -> result.building.name
                                        is SearchResult.CampusResult -> result.campus.name
                                        is com.example.myapplication.logic.SearchResult.GoogleResult -> result.title
                                        is com.example.myapplication.logic.SearchResult.CurrentLocation -> "Your position"
                                        is com.example.myapplication.logic.SearchResult.Home -> "Home"
                                    }

                                    ListItem(
                                        headlineContent = { Text(title) },
                                        modifier = Modifier.clickable {
                                            viewModel.handleSearchResult(result, context)
                                        }
                                    )
                                }
                            }
                        }
                    }

                }


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

                if (viewModel.uiBuildingState.mode != MapUIMode.DIRECTIONS) {
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

                }

                if (viewModel.uiBuildingState.isVisible) {
                    viewModel.uiBuildingState.building?.let { building ->
                        if (viewModel.uiBuildingState.mode == com.example.myapplication.ui.models.MapUIMode.PREVIEW) {
                            BuildingInfoPopup(
                                building = building,
                                uiState = viewModel.uiBuildingState,
                                onDismiss = { viewModel.handleMapTap(null) },
                                onDirectionsClick = { viewModel.onDirectionsRequested() }
                            )
                        }
                        else {
                            DirectionsInfoPopup(
                                uiState = viewModel.uiBuildingState,
                                onModeChange = { mode -> viewModel.onTransportModeChanged(mode) },
                                onStartClick = { viewModel.toggleSearchExpansion(true, "start") },
                                onDestinationClick = { viewModel.toggleSearchExpansion(true, "dest") },
                                onSwapClick = { viewModel.swapLocations() },
                                onClose = { viewModel.onBackToPreview() },
                                onStartNavigation = { /* Logic for navigation later */ },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }

                    }
                }
            }
            DisposableEffect(Unit) {
                onDispose {
                    previousModeNavigation?.let { Smartlook.instance.trackNavigationExit(it) }
                }
            }
            }
        }

    override fun onDestroy() {
        Smartlook.instance.trackNavigationExit("MapsActivity")
        super.onDestroy()
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
        } catch (e: SecurityException) {
            CrashReporter.setKey("recenter_has_permission", hasPermission)
            CrashReporter.recordNonFatal(e, "recenter_location_lookup_failed")
        }
    }

    private fun openAppSettings(context: android.content.Context) {
        val intent = android.content.Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts("package", context.packageName, null)
        )
        context.startActivity(intent)
    }

}
