package com.example.myapplication

import android.Manifest
import android.content.Intent
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Map
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
import com.example.myapplication.data.ShuttleRepo
import com.example.myapplication.logic.AuthRepository
import com.example.myapplication.logic.DefaultShuttleService
import com.example.myapplication.logic.GoogleCalendarProvider
import com.example.myapplication.logic.SearchResult
import com.example.myapplication.logic.TrueLocationProvider
import com.example.myapplication.map.TrueCameraController
import com.example.myapplication.telemetry.CrashReporter
import com.example.myapplication.ui.components.BuildingInfoPopup
import com.example.myapplication.ui.components.CalendarScreen
import com.example.myapplication.ui.components.CampusMap
import com.example.myapplication.ui.components.CampusSearchBar
import com.example.myapplication.ui.components.CampusToggle
import com.example.myapplication.ui.components.DirectionsHeader
import com.example.myapplication.ui.components.DirectionsInfoPopup
import com.example.myapplication.ui.components.NextClassPill
import com.example.myapplication.data.LocationResult
import com.example.myapplication.ui.models.MapUIMode
import com.example.myapplication.ui.theme.ConcordiaMaroon
import com.example.myapplication.logic.SharedPrefsCalendarPreferences
import com.example.myapplication.ui.viewmodel.CalendarViewModel
import com.example.myapplication.ui.viewmodel.MapViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

class MapsActivity : ComponentActivity() {

    // ── Dependencies (Activity-scoped) ────────────────────────────────────────
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: MapViewModel
    private lateinit var calendarViewModel: CalendarViewModel
    private lateinit var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient

    // ── Google Sign-In via modern ActivityResultLauncher ──────────────────────
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn
            .getSignedInAccountFromIntent(result.data)
        if (task.isSuccessful) {
            // Clear any previously persisted calendar selection — the new
            // account may have different calendars, so force the picker.
            calendarViewModel.clearSelection()
            calendarViewModel.loadCalendarsAndAutoSelect()
        } else {
            CrashReporter.recordNonFatal(
                task.exception ?: Exception("Sign-in cancelled"),
                "google_sign_in_failed"
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        if (!com.google.android.libraries.places.api.Places.isInitialized()) {
            com.google.android.libraries.places.api.Places.initialize(
                applicationContext, BuildConfig.MAPS_API_KEY
            )
        }
        super.onCreate(savedInstanceState)
        CrashReporter.setKey("screen", "MapsActivity")
        CrashReporter.setKey("app_version", BuildConfig.VERSION_NAME)
        CrashReporter.log("maps_activity_created")
        CampusRepo.initialize(this)
        ShuttleRepo.initialize(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // ── Auth (all Google Sign-In logic lives in AuthRepository) ───────────
        authRepository = AuthRepository(context = this)
        val tokenProvider: suspend () -> String? = { authRepository.getCalendarToken() }

        // ── Build ViewModel ───────────────────────────────────────────────────
        val locationProvider = TrueLocationProvider(fusedLocationClient)
        val routeProvider    = com.example.myapplication.logic.GoogleRouteProvider(BuildConfig.MAPS_API_KEY)
        val calendarProvider = GoogleCalendarProvider(
            context       = this,
            tokenProvider = tokenProvider
        )
        viewModel = MapViewModel(
            locationProvider = locationProvider,
            routeProvider    = routeProvider,
            shuttleService   = DefaultShuttleService(ShuttleRepo)
        )
        calendarViewModel = CalendarViewModel(
            calendarProvider    = calendarProvider,
            calendarPreferences = SharedPrefsCalendarPreferences(this)
        )

        val placesClient = com.google.android.libraries.places.api.Places.createClient(this)
        viewModel.initSearch(placesClient)

        setContent { AppScaffold() }
    }

    /** Revokes previous access then launches the sign-in picker. */
    private fun connectCalendar() {
        authRepository.revokeAndSignIn { intent ->
            signInLauncher.launch(intent)
        }
    }

    /** Signs out of Google Calendar and clears all persisted calendar state. */
    private fun signOutCalendar() {
        authRepository.signInClient.signOut().addOnCompleteListener {
            calendarViewModel.clearSelection()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────────────────────────────────

    @Composable
    private fun AppScaffold() {
        var selectedTab by remember { mutableStateOf(0) }
        val selectedCalendarId = calendarViewModel.selectedCalendarId

        LaunchedEffect(selectedCalendarId) {
            selectedCalendarId?.let { calendarViewModel.loadWeekEvents(it) }
        }

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick  = { selectedTab = 0 },
                        icon     = { Icon(Icons.Default.Map, contentDescription = "Map") },
                        label    = { Text("Map") },
                        colors   = NavigationBarItemDefaults.colors(
                            selectedIconColor = ConcordiaMaroon,
                            selectedTextColor = ConcordiaMaroon,
                            indicatorColor    = ConcordiaMaroon.copy(alpha = 0.12f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick  = { selectedTab = 1 },
                        icon     = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar") },
                        label    = { Text("Schedule") },
                        colors   = NavigationBarItemDefaults.colors(
                            selectedIconColor = ConcordiaMaroon,
                            selectedTextColor = ConcordiaMaroon,
                            indicatorColor    = ConcordiaMaroon.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    0 -> MapScreen()
                    1 -> CalendarScreen(
                        viewModel          = calendarViewModel,
                        selectedCalendarId = selectedCalendarId,
                        userEmail          = authRepository.getSignedInEmail() ?: "",
                        onConnectClick     = { connectCalendar() },
                        onSignOutClick     = { signOutCalendar() },
                        onNavigateToEvent  = { location ->
                            viewModel.navigateToEvent(location)
                            selectedTab = 0
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun MapScreen() {
        val context = LocalContext.current
        val scope   = rememberCoroutineScope()
        var showSettingsDialog by remember { mutableStateOf(false) }
        var hasLocationPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            )
        }

        ObserveLocationUpdates(hasLocationPermission)
        val cameraPositionState = rememberMapCamera()
        val cameraController    = remember(cameraPositionState) { TrueCameraController(cameraPositionState) }
        ObserveCameraEffects(cameraPositionState, cameraController)

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted -> hasLocationPermission = isGranted }

        val mapPaddingBottom = if (viewModel.uiBuildingState.mode == MapUIMode.DIRECTIONS) 600 else 0

        Box(modifier = Modifier.fillMaxSize()) {
            CampusMap(
                currentCampus           = viewModel.currentCampus,
                highlightedBuildingName = viewModel.highlightedBuildingName,
                cameraPositionState     = cameraPositionState,
                hasLocationPermission   = hasLocationPermission,
                viewModel               = viewModel,
                contentPadding          = PaddingValues(bottom = mapPaddingBottom.dp),
                modifier                = Modifier.testTag("campus_map")
            )
            MapSearchOverlay(context)
            MapPreviewOverlays(
                hasLocationPermission = hasLocationPermission,
                launcher              = launcher,
                cameraController      = cameraController,
                scope                 = scope,
                onShowSettings        = { showSettingsDialog = true },
                context               = context
            )
            MapBuildingOverlay()
            if (showSettingsDialog && !hasLocationPermission) {
                LocationPermissionDialog(
                    onOpenSettings = { openAppSettings(context) },
                    onDismiss      = { showSettingsDialog = false }
                )
            }
        }
    }

    @Composable
    private fun ObserveLocationUpdates(hasLocationPermission: Boolean) {
        LaunchedEffect(hasLocationPermission) {
            CrashReporter.setKey("location_permission_granted", hasLocationPermission)
            if (hasLocationPermission) {
                val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 5000
                ).build()
                val locationCallback = object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
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
    }

    @Composable
    private fun rememberMapCamera(): com.google.maps.android.compose.CameraPositionState {
        val state = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(45.497, -73.579), 16f)
        }
        return state
    }

    @Composable
    private fun ObserveCameraEffects(
        cameraPositionState: com.google.maps.android.compose.CameraPositionState,
        cameraController: TrueCameraController
    ) {
        LaunchedEffect(viewModel.uiBuildingState.routeBounds) {
            val bounds = viewModel.uiBuildingState.routeBounds
            if (bounds != null) {
                kotlinx.coroutines.delay(500)
                cameraPositionState.animate(
                    update     = com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, 400),
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

    @Composable
    private fun BoxScope.MapSearchOverlay(context: android.content.Context) {
        val mode = viewModel.uiBuildingState.mode
        if (mode == MapUIMode.PREVIEW) {
            CampusSearchBar(
                query         = viewModel.searchQuery,
                results       = viewModel.searchResults,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                onResultClick = { viewModel.handleSearchResult(it, context) },
                modifier      = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
            )
        } else {
            DirectionsOverlay(context)
        }
    }

    @Composable
    private fun BoxScope.DirectionsOverlay(context: android.content.Context) {
        val mode = viewModel.uiBuildingState.mode
        if (viewModel.uiBuildingState.isSearchExpanded) {
            DirectionsHeader(
                uiState            = viewModel.uiBuildingState,
                onBackClick        = { viewModel.toggleSearchExpansion(false) },
                onStartQueryChange = { viewModel.onSearchQueryChanged(it, field = "start") },
                onDestQueryChange  = { viewModel.onSearchQueryChanged(it, field = "dest") },
                modifier           = Modifier.align(Alignment.TopCenter)
            )
        }
        if (mode == MapUIMode.DIRECTIONS) {
            DirectionsInfoPopup(
                uiState            = viewModel.uiBuildingState,
                onModeChange       = { viewModel.onTransportModeChanged(it) },
                onStartClick       = { viewModel.toggleSearchExpansion(true, "start") },
                onDestinationClick = { viewModel.toggleSearchExpansion(true, "dest") },
                onSwapClick        = { viewModel.swapLocations() },
                onClose            = { viewModel.onBackToPreview() },
                onStartNavigation  = { },
                modifier           = Modifier.align(Alignment.BottomCenter)
            )
        }
        DirectionsSearchResults(context)
    }

    @Composable
    private fun BoxScope.DirectionsSearchResults(context: android.content.Context) {
        val currentFieldText = when (viewModel.activeSearchField) {
            "start" -> viewModel.uiBuildingState.startLocationName
            "dest"  -> viewModel.uiBuildingState.destinationName
            else    -> viewModel.searchQuery
        }
        if (viewModel.activeSearchField == "main" || currentFieldText.isEmpty()) return
        Card(
            modifier  = Modifier.padding(horizontal = 24.dp).offset(y = 190.dp).zIndex(1f),
            elevation = CardDefaults.cardElevation(4.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                items(viewModel.searchResults) { result ->
                    val title = when (result) {
                        is SearchResult.BuildingResult  -> result.building.name
                        is SearchResult.CampusResult    -> result.campus.name
                        is SearchResult.GoogleResult    -> result.title
                        is SearchResult.CurrentLocation -> "Your position"
                        is SearchResult.Home            -> "Home"
                    }
                    ListItem(
                        headlineContent = { Text(title) },
                        modifier        = Modifier.clickable { viewModel.handleSearchResult(result, context) }
                    )
                }
            }
        }
    }

    @Composable
    private fun BoxScope.MapPreviewOverlays(
        hasLocationPermission: Boolean,
        launcher: androidx.activity.result.ActivityResultLauncher<String>,
        cameraController: TrueCameraController,
        scope: kotlinx.coroutines.CoroutineScope,
        onShowSettings: () -> Unit,
        context: android.content.Context
    ) {
        val mode = viewModel.uiBuildingState.mode
        if (mode == MapUIMode.PREVIEW) {
            NextClassPill(
                nextEvent       = calendarViewModel.nextUpcomingEvent,
                onNavigateClick = {
                    val event = calendarViewModel.nextUpcomingEvent ?: return@NextClassPill
                    val dest  = (event.locationResult as? LocationResult.Known)
                        ?.location?.buildingCode
                        ?: event.location
                        ?: return@NextClassPill
                    viewModel.navigateToEvent(dest)
                },
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 28.dp)
            )
        }
        if (mode != MapUIMode.DIRECTIONS) {
            CampusToggle(
                selectedCampusName = viewModel.currentCampus?.name,
                onCampusClick      = { name -> viewModel.onCampusSelected(name) },
                modifier           = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 160.dp)
            )
            ExtendedFloatingActionButton(
                onClick = {
                    handleRecenter(
                        client         = fusedLocationClient,
                        hasPermission  = hasLocationPermission,
                        launcher       = launcher,
                        context        = context,
                        onShowSettings = onShowSettings
                    ) { userLocation ->
                        scope.launch { cameraController.animateTo(userLocation, 18.5f) }
                        viewModel.processLocationUpdate(userLocation, isForce = true)
                    }
                },
                modifier       = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 24.dp),
                containerColor = ConcordiaMaroon,
                contentColor   = Color.White,
                icon           = { Icon(Icons.Default.MyLocation, contentDescription = null) },
                text           = { Text("RECENTER") }
            )
        }
    }

    @Composable
    private fun BoxScope.MapBuildingOverlay() {
        if (!viewModel.uiBuildingState.isVisible) return
        val building = viewModel.uiBuildingState.building ?: return
        if (viewModel.uiBuildingState.mode == MapUIMode.PREVIEW) {
            BuildingInfoPopup(
                building          = building,
                uiState           = viewModel.uiBuildingState,
                onDismiss         = { viewModel.handleMapTap(null) },
                onDirectionsClick = { viewModel.onDirectionsRequested() }
            )
        } else {
            DirectionsInfoPopup(
                uiState            = viewModel.uiBuildingState,
                onModeChange       = { mode -> viewModel.onTransportModeChanged(mode) },
                onStartClick       = { viewModel.toggleSearchExpansion(true, "start") },
                onDestinationClick = { viewModel.toggleSearchExpansion(true, "dest") },
                onSwapClick        = { viewModel.swapLocations() },
                onClose            = { viewModel.onBackToPreview() },
                onStartNavigation  = { },
                modifier           = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    @Composable
    private fun LocationPermissionDialog(onOpenSettings: () -> Unit, onDismiss: () -> Unit) {
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun handleRecenter(
        client: com.google.android.gms.location.FusedLocationProviderClient,
        hasPermission: Boolean,
        launcher: androidx.activity.result.ActivityResultLauncher<String>,
        context: android.content.Context,
        onShowSettings: () -> Unit,
        onLocationFound: (LatLng) -> Unit
    ) {
        if (!hasPermission) {
            val activity = context as? androidx.activity.ComponentActivity
            val shouldShowRationale = activity?.let {
                androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
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

    private fun openAppSettings(context: android.content.Context) {
        val intent = Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts("package", context.packageName, null)
        )
        context.startActivity(intent)
    }
}
