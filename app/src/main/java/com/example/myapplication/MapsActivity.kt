package com.example.myapplication

import android.Manifest
import com.example.myapplication.ui.components.BuildingInfoPopup
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.data.ShuttleStop
import com.example.myapplication.logic.GoogleRouteProvider
import com.example.myapplication.logic.SearchResult
import com.example.myapplication.logic.TrueLocationProvider
import com.example.myapplication.map.TrueCameraController
import com.example.myapplication.ui.components.CampusMap
import com.example.myapplication.ui.components.CampusSearchBar
import com.example.myapplication.ui.components.CampusToggle
import com.example.myapplication.ui.components.DirectionsHeader
import com.example.myapplication.ui.components.DirectionsInfoPopup
import com.example.myapplication.ui.models.MapUIMode
import com.example.myapplication.ui.theme.ConcordiaMaroon
import com.example.myapplication.ui.viewmodel.MapViewModel
import com.example.myapplication.ui.viewmodel.ShuttleViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

class MapsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!com.google.android.libraries.places.api.Places.isInitialized()) {
            com.google.android.libraries.places.api.Places.initialize(
                applicationContext, BuildConfig.MAPS_API_KEY
            )
        }
        super.onCreate(savedInstanceState)
        CampusRepo.initialize(this)

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationProvider    = TrueLocationProvider(fusedLocationClient)
        val routeProvider       = GoogleRouteProvider(BuildConfig.MAPS_API_KEY)
        val mapViewModel        = MapViewModel(locationProvider, routeProvider)
        val shuttleViewModel    = ShuttleViewModel()

        val placesClient = com.google.android.libraries.places.api.Places.createClient(this)
        mapViewModel.initSearch(placesClient)

        setContent {
            MainScreen(
                fusedLocationClient = fusedLocationClient,
                mapViewModel        = mapViewModel,
                shuttleViewModel    = shuttleViewModel
            )
        }
    }
}

@Composable
fun MapsScreen(
    fusedLocationClient: FusedLocationProviderClient,
    mapViewModel:        MapViewModel,
    shuttleViewModel:    ShuttleViewModel,
    bottomPadding:       PaddingValues
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Map content padding accounts for both the bottom nav and the directions sheet height (if visible)
    val directionsPopupHeight = if (mapViewModel.uiBuildingState.mode == MapUIMode.DIRECTIONS) 600.dp else 0.dp
    val totalBottomPadding = bottomPadding.calculateBottomPadding() + directionsPopupHeight

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) return@LaunchedEffect

        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 5000
        ).build()

        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                result.lastLocation?.let { loc ->
                    val latLng = LatLng(loc.latitude, loc.longitude)
                    mapViewModel.processLocationUpdate(latLng)
                    if (shuttleViewModel.isShuttleModeActive) {
                        shuttleViewModel.onUserLocationUpdated(latLng)
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, android.os.Looper.getMainLooper()
            )
        } catch (e: SecurityException) { /* Ignored */ }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(45.497, -73.579), 16f)
    }
    val cameraController = remember(cameraPositionState) {
        TrueCameraController(cameraPositionState)
    }

    LaunchedEffect(mapViewModel.currentCampus) {
        mapViewModel.currentCampus?.let { campus ->
            cameraController.animateTo(campus.getGoogleCenter(), campus.defaultZoom)
        }
    }

    LaunchedEffect(mapViewModel.uiBuildingState.routeBounds) {
        val bounds = mapViewModel.uiBuildingState.routeBounds
        if (bounds != null) {
            kotlinx.coroutines.delay(500)
            cameraPositionState.animate(
                update     = com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, 150),
                durationMs = 1000
            )
        }
    }

    val ambiguousStops = shuttleViewModel.ambiguousStops
    if (ambiguousStops.isNotEmpty()) {
        ShuttleStopSelectionDialog(
            stops    = ambiguousStops,
            onSelect = { shuttleViewModel.onUserSelectedStop(it) }
        )
    }

    val routeError        = shuttleViewModel.routeError
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(routeError) {
        routeError?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
        }
    }

    // Main container should fill the WHOLE screen to avoid white bars
    Box(modifier = Modifier.fillMaxSize()) {
        CampusMap(
            currentCampus           = mapViewModel.currentCampus,
            highlightedBuildingName = mapViewModel.highlightedBuildingName,
            cameraPositionState     = cameraPositionState,
            hasLocationPermission   = hasLocationPermission,
            viewModel               = mapViewModel,
            // Map logo and camera shift should account for system bars and UI
            contentPadding          = PaddingValues(
                top    = bottomPadding.calculateTopPadding(),
                bottom = totalBottomPadding
            ),
            modifier                = Modifier.fillMaxSize().testTag("campus_map"),
            shuttleRoute            = shuttleViewModel.shuttleRoute,
            nearestStop             = shuttleViewModel.nearestStop,
            routePoints             = mapViewModel.uiBuildingState.routePoints
        )

        // UI Layer - Apply paddings to individual components
        Box(modifier = Modifier.fillMaxSize().padding(bottomPadding)) {
            
            if (mapViewModel.uiBuildingState.mode == MapUIMode.PREVIEW) {
                CampusSearchBar(
                    query         = mapViewModel.searchQuery,
                    results       = mapViewModel.searchResults,
                    onQueryChange = { mapViewModel.onSearchQueryChanged(it) },
                    onResultClick = { mapViewModel.handleSearchResult(it, context) },
                    modifier      = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                )
            } else { // DIRECTIONS mode
                if (mapViewModel.uiBuildingState.isSearchExpanded) {
                    DirectionsHeader(
                        uiState           = mapViewModel.uiBuildingState,
                        onBackClick       = { mapViewModel.toggleSearchExpansion(false) },
                        onStartQueryChange = { mapViewModel.onSearchQueryChanged(it, field = "start") },
                        onDestQueryChange = { mapViewModel.onSearchQueryChanged(it, field = "dest") },
                        modifier          = Modifier.align(Alignment.TopCenter)
                    )
                }

                DirectionsInfoPopup(
                    uiState            = mapViewModel.uiBuildingState,
                    shuttleViewModel   = shuttleViewModel,
                    onModeChange       = { mode ->
                        val wasShuttle = mapViewModel.uiBuildingState.selectedTransportMode == "shuttle"
                        mapViewModel.onTransportModeChanged(mode)

                        if (mode == "shuttle") {
                            // Smart direction detection: check destination campus
                            val destBuilding = mapViewModel.uiBuildingState.building
                            val destCampus = if (destBuilding != null) {
                                CampusRepo.getAllCampuses().find { it.buildings.contains(destBuilding) }?.name
                            } else null

                            // If we are going TO SGW, we board at Loyola. 
                            // If we are going TO Loyola, we board at SGW.
                            val startCampus = when (destCampus) {
                                "SGW" -> "Loyola"
                                "Loyola" -> "SGW"
                                else -> mapViewModel.currentCampus?.name
                            }

                            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                shuttleViewModel.enableShuttleMode(
                                    userLocation = loc?.let { LatLng(it.latitude, it.longitude) },
                                    startCampus  = startCampus
                                )
                            }
                        } else if (wasShuttle) {
                            shuttleViewModel.disableShuttleMode()
                        }
                    },
                    onStartClick       = { mapViewModel.toggleSearchExpansion(true, "start") },
                    onDestinationClick = { mapViewModel.toggleSearchExpansion(true, "dest") },
                    onSwapClick        = { 
                        mapViewModel.swapLocations()
                        if (mapViewModel.uiBuildingState.selectedTransportMode == "shuttle") {
                            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                shuttleViewModel.swapDirection(loc?.let { LatLng(it.latitude, it.longitude) })
                            }
                        }
                    },
                    onClose            = { mapViewModel.onBackToPreview() },
                    onStartNavigation  = { /* TODO */ },
                    // Popup is now inside a Box already padded by bottomPadding, so use 0.dp
                    bottomNavHeight    = 0.dp, 
                    modifier           = Modifier.align(Alignment.BottomCenter)
                )

                // Search Result Dropdown
                val currentFieldText = when (mapViewModel.activeSearchField) {
                    "start" -> mapViewModel.uiBuildingState.startLocationName
                    "dest"  -> mapViewModel.uiBuildingState.destinationName
                    else    -> mapViewModel.searchQuery
                }
                if (mapViewModel.activeSearchField != "main" && currentFieldText.isNotEmpty()) {
                    Card(
                        modifier  = Modifier.padding(horizontal = 24.dp).offset(y = 190.dp).zIndex(1f),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors    = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                            items(mapViewModel.searchResults) { result ->
                                val title = when (result) {
                                    is SearchResult.BuildingResult  -> result.building.name
                                    is SearchResult.CampusResult    -> result.campus.name
                                    is SearchResult.GoogleResult    -> result.title
                                    is SearchResult.CurrentLocation -> "Your position"
                                    is SearchResult.Home            -> "Home"
                                }
                                ListItem(
                                    headlineContent = { Text(title) },
                                    modifier        = Modifier.clickable { mapViewModel.handleSearchResult(result, context) }
                                )
                            }
                        }
                    }
                }
            }

            // Controls (Campus Toggle & Recenter FAB)
            if (mapViewModel.uiBuildingState.mode != MapUIMode.DIRECTIONS) {
                CampusToggle(
                    selectedCampusName = mapViewModel.currentCampus?.name,
                    onCampusClick      = { name -> mapViewModel.onCampusSelected(name) },
                    modifier           = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 10.dp)
                )

                FloatingActionButton(
                    onClick = {
                        handleRecenter(
                            client         = fusedLocationClient,
                            hasPermission  = hasLocationPermission,
                            launcher       = launcher,
                            context        = context,
                            onShowSettings = { showSettingsDialog = true }
                        ) { userLocation ->
                            scope.launch { cameraController.animateTo(userLocation, 18.5f) }
                            mapViewModel.processLocationUpdate(userLocation, isForce = true)
                        }
                    },
                    modifier       = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 130.dp),
                    containerColor = ConcordiaMaroon,
                    contentColor   = Color.White
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Recenter")
                }
            }

            // Info Popups
            if (mapViewModel.uiBuildingState.isVisible && mapViewModel.uiBuildingState.mode == MapUIMode.PREVIEW) {
                mapViewModel.uiBuildingState.building?.let { building ->
                    BuildingInfoPopup(
                        building          = building,
                        uiState           = mapViewModel.uiBuildingState,
                        onDismiss         = { mapViewModel.handleMapTap(null) },
                        onDirectionsClick = { mapViewModel.onDirectionsRequested() },
                        bottomNavHeight   = 0.dp // Managed by UI Box padding
                    )
                }
            }

            // Snackbar
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Location Permission Dialog
        if (showSettingsDialog && !hasLocationPermission) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title   = { Text("Location Required") },
                text    = { Text("To see which building you are in, please enable location permissions in the app settings.") },
                confirmButton = {
                    Button(
                        onClick = { openAppSettings(context) },
                        colors  = ButtonDefaults.buttonColors(containerColor = ConcordiaMaroon)
                    ) { Text("OPEN SETTINGS") }
                },
                dismissButton = { TextButton(onClick = { showSettingsDialog = false }) { Text("CANCEL", color = Color.Gray) } },
                icon = { Icon(Icons.Default.MyLocation, tint = ConcordiaMaroon, contentDescription = null) }
            )
        }
    }
}

@Composable
fun ShuttleStopSelectionDialog(
    stops:    List<ShuttleStop>,
    onSelect: (ShuttleStop) -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Non-dismissable */ },
        title = { Text("Multiple stops nearby") },
        text  = {
            Column {
                Text(
                    "Several shuttle stops are equidistant from your location. Please choose one:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                stops.forEach { stop ->
                    TextButton(
                        onClick  = { onSelect(stop) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stop.name, style = MaterialTheme.typography.bodyLarge) }
                }
            }
        },
        confirmButton = { }
    )
}

private fun handleRecenter(
    client:          FusedLocationProviderClient,
    hasPermission:   Boolean,
    launcher:        androidx.activity.result.ActivityResultLauncher<String>,
    context:         android.content.Context,
    onShowSettings:  () -> Unit,
    onLocationFound: (LatLng) -> Unit
) {
    if (!hasPermission) {
        val activity = context as? androidx.activity.ComponentActivity
        val shouldShowRationale = activity?.let {
            androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION)
        } ?: false

        if (shouldShowRationale) onShowSettings() else launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        return
    }
    try {
        client.lastLocation.addOnSuccessListener { loc ->
            loc?.let { onLocationFound(LatLng(it.latitude, it.longitude)) }
        }
    } catch (e: SecurityException) { /* Ignored */ }
}

private fun openAppSettings(context: android.content.Context) {
    val intent = android.content.Intent(
        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        android.net.Uri.fromParts("package", context.packageName, null)
    )
    context.startActivity(intent)
}
