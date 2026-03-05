package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.Map
import com.example.myapplication.logic.currentWeekMonday
import com.example.myapplication.ui.components.WeekCalendarView
import com.example.myapplication.ui.components.NextClassPill
import com.example.myapplication.ui.components.parseLocation
import androidx.core.content.ContextCompat
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.data.ShuttleRepo
import com.example.myapplication.logic.DefaultShuttleService
import com.example.myapplication.logic.GoogleCalendarProvider
import com.example.myapplication.logic.SearchResult
import com.example.myapplication.logic.TrueLocationProvider
import com.example.myapplication.map.TrueCameraController
import com.example.myapplication.telemetry.CrashReporter
import com.example.myapplication.ui.components.BuildingInfoPopup
import com.example.myapplication.ui.components.CampusMap
import com.example.myapplication.ui.components.CampusSearchBar
import com.example.myapplication.ui.components.CampusToggle
import com.example.myapplication.ui.components.DirectionsHeader
import com.example.myapplication.ui.components.DirectionsInfoPopup
import com.example.myapplication.ui.models.MapUIMode
import com.example.myapplication.ui.theme.ConcordiaMaroon
import androidx.compose.ui.text.style.TextAlign
import com.example.myapplication.ui.viewmodel.MapViewModel
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapsActivity : ComponentActivity() {

    // ── Google Sign-In client (lazy so it's built after onCreate) ─────────────
    private lateinit var googleSignInClient: GoogleSignInClient

    // ── ViewModel (kept as field so onActivityResult can call it) ─────────────
    private lateinit var viewModel: MapViewModel

    // ── Location client (Activity-scoped, same as original) ───────────────────
    private lateinit var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val CALENDAR_SCOPE = "oauth2:https://www.googleapis.com/auth/calendar.readonly"
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
        val locationProvider    = TrueLocationProvider(fusedLocationClient)
        val routeProvider       = com.example.myapplication.logic.GoogleRouteProvider(BuildConfig.MAPS_API_KEY)

        // ── Google Sign-In (Calendar scope) ───────────────────────────────────
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/calendar.readonly"))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Token provider: fetches a fresh OAuth token on every call.
        // Runs on IO dispatcher; never blocks the main thread.
        val tokenProvider: suspend () -> String? = {
            withContext(Dispatchers.IO) {
                try {
                    val account = GoogleSignIn.getLastSignedInAccount(this@MapsActivity)
                    if (account == null) {
                        android.util.Log.w("CalendarToken", "No signed-in account found")
                        null
                    } else {
                        val token = GoogleAuthUtil.getToken(this@MapsActivity, account.account!!, CALENDAR_SCOPE)
                        android.util.Log.d("CalendarToken", "Token: ${token?.take(10)}...")
                        token
                    }
                } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
                    // Consent not granted yet — re-launch sign-in
                    android.util.Log.w("CalendarToken", "UserRecoverableAuthException — re-launching sign-in")
                    withContext(Dispatchers.Main) {
                        @Suppress("DEPRECATION")
                        startActivityForResult(e.intent!!, RC_SIGN_IN)
                    }
                    null
                } catch (e: Exception) {
                    android.util.Log.e("CalendarToken", "Token error: ${e.message}")
                    CrashReporter.recordNonFatal(e, "calendar_token_refresh_failed")
                    null
                }
            }
        }

        val calendarProvider = GoogleCalendarProvider(
            context       = this,
            tokenProvider = tokenProvider
        )

        // ── ViewModel ─────────────────────────────────────────────────────────
        viewModel = MapViewModel(
            locationProvider = locationProvider,
            routeProvider    = routeProvider,
            shuttleService   = DefaultShuttleService(ShuttleRepo),
            calendarProvider = calendarProvider
        )

        val placesClient = com.google.android.libraries.places.api.Places.createClient(this)
        viewModel.initSearch(placesClient)

        setContent { AppScaffold() }
    }

    // ── Handle Google Sign-In result ──────────────────────────────────────────
    @Deprecated("Required for GoogleSignIn result callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            if (task.isSuccessful) {
                // Load calendars — ViewModel will auto-select primary calendar
                viewModel.loadCalendarsAndAutoSelect()
            } else {
                CrashReporter.recordNonFatal(
                    task.exception ?: Exception("Sign-in cancelled"),
                    "google_sign_in_failed"
                )
            }
        }
    }

    /**
     * Signs out first to force Google to show the Calendar consent screen,
     * then launches the sign-in intent.
     *
     * This is required because if the user is already signed in without the
     * Calendar scope, Google silently skips consent and returns an account
     * that has no Calendar access — resulting in "No calendars found".
     */
    fun connectCalendar() {
        // revokeAccess forces Google to show the full consent screen including Calendar scope.
        // signOut() alone is not enough — Google remembers the previous grant.
        googleSignInClient.revokeAccess().addOnCompleteListener {
            @Suppress("DEPRECATION")
            startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────────────────────────────────

    @Composable
    private fun AppScaffold() {
        var selectedTab by remember { mutableStateOf(0) }
        // Read from ViewModel so Activity-level callbacks (onActivityResult, onCalendarSelected)
        // can update the state and Compose will recompose automatically.
        val selectedCalendarId = viewModel.selectedCalendarId

        // Auto-load week events when calendar becomes connected
        LaunchedEffect(selectedCalendarId) {
            selectedCalendarId?.let { viewModel.loadWeekEvents(it) }
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
                            selectedIconColor   = ConcordiaMaroon,
                            selectedTextColor   = ConcordiaMaroon,
                            indicatorColor      = ConcordiaMaroon.copy(alpha = 0.12f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick  = { selectedTab = 1 },
                        icon  = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar") },
                        label = { Text("Schedule") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = ConcordiaMaroon,
                            selectedTextColor   = ConcordiaMaroon,
                            indicatorColor      = ConcordiaMaroon.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    0 -> MapScreen()
                    1 -> CalendarScreen(
                        selectedCalendarId = selectedCalendarId,
                        onNavigateToEvent  = { location ->
                            viewModel.navigateToEvent(location)
                            selectedTab = 0  // switch to Map tab
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun CalendarScreen(
        selectedCalendarId: String?,
        onNavigateToEvent: (String) -> Unit
    ) {
        val context    = LocalContext.current
        val isSignedIn = selectedCalendarId != null
        val calState   = viewModel.calendarState

        // Show calendar picker if user just signed in and hasn't picked yet
        if (!isSignedIn && calState is com.example.myapplication.ui.models.CalendarState.SelectingCalendar) {
            CalendarPickerScreen(
                calendars = calState.calendars,
                onCalendarPicked = { id, name ->
                    viewModel.onCalendarSelected(id, name)
                }
            )
            return
        }

        WeekCalendarView(
            weekStartMs       = viewModel.currentWeekStartMs,
            events            = viewModel.weekEvents,
            isLoading         = viewModel.weekViewLoading || calState is com.example.myapplication.ui.models.CalendarState.Loading,
            isSignedIn        = isSignedIn,
            userEmail         = if (isSignedIn) (GoogleSignIn.getLastSignedInAccount(context)?.email ?: "") else "",
            onConnectClick    = { connectCalendar() },
            onPreviousWeek    = { selectedCalendarId?.let { viewModel.goToPreviousWeek(it) } },
            onNextWeek        = { selectedCalendarId?.let { viewModel.goToNextWeek(it) } },
            onNavigateToEvent = { event ->
                val parsed = com.example.myapplication.ui.components.parseLocation(event.location ?: "")
                val destination = when {
                    parsed != null -> parsed.buildingCode
                    !event.location.isNullOrBlank() -> event.location
                    else -> return@WeekCalendarView
                }
                onNavigateToEvent(destination)
            },
            modifier          = Modifier.fillMaxSize()
        )
    }

    @Composable
    private fun CalendarPickerScreen(
        calendars: List<com.example.myapplication.logic.CalendarInfo>,
        onCalendarPicked: (String, String) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = ConcordiaMaroon,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Choose a Calendar",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Select the calendar with your courses",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            HorizontalDivider(color = Color(0xFFEEEEEE))

            // Calendar list
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(calendars) { calendar ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCalendarPicked(calendar.id, calendar.summary) }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(ConcordiaMaroon.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = ConcordiaMaroon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                calendar.summary,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                            )
                            if (!calendar.description.isNullOrBlank()) {
                                Text(
                                    calendar.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    HorizontalDivider(
                        color = Color(0xFFEEEEEE),
                        modifier = Modifier.padding(start = 74.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun MapScreen() {
        val mapPaddingBottom = if (viewModel.uiBuildingState.mode == MapUIMode.DIRECTIONS) 600 else 0
        val context  = LocalContext.current
        val scope    = rememberCoroutineScope()
        var showSettingsDialog by remember { mutableStateOf(false) }

        var hasLocationPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            )
        }

        // ── Location updates ──────────────────────────────────────────────────
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
                        locationRequest,
                        locationCallback,
                        android.os.Looper.getMainLooper()
                    )
                } catch (e: SecurityException) {
                    CrashReporter.recordNonFatal(e, "request_location_updates_failed")
                }
            }
        }

        // ── Camera ────────────────────────────────────────────────────────────
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

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted -> hasLocationPermission = isGranted }

        // ── Layout ────────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {

            // 1. Map
            CampusMap(
                currentCampus           = viewModel.currentCampus,
                highlightedBuildingName = viewModel.highlightedBuildingName,
                cameraPositionState     = cameraPositionState,
                hasLocationPermission   = hasLocationPermission,
                viewModel               = viewModel,
                contentPadding          = PaddingValues(bottom = mapPaddingBottom.dp),
                modifier                = Modifier.testTag("campus_map")
            )

            // 2. Search bar / Directions header (top)
            if (viewModel.uiBuildingState.mode == MapUIMode.PREVIEW) {
                CampusSearchBar(
                    query         = viewModel.searchQuery,
                    results       = viewModel.searchResults,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onResultClick = { viewModel.handleSearchResult(it, context) },
                    modifier      = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )
            } else {
                if (viewModel.uiBuildingState.isSearchExpanded) {
                    DirectionsHeader(
                        uiState           = viewModel.uiBuildingState,
                        onBackClick       = { viewModel.toggleSearchExpansion(false) },
                        onStartQueryChange = { viewModel.onSearchQueryChanged(it, field = "start") },
                        onDestQueryChange  = { viewModel.onSearchQueryChanged(it, field = "dest") },
                        modifier          = Modifier.align(Alignment.TopCenter)
                    )
                }

                if (viewModel.uiBuildingState.mode == MapUIMode.DIRECTIONS) {
                    DirectionsInfoPopup(
                        uiState            = viewModel.uiBuildingState,
                        onModeChange       = { viewModel.onTransportModeChanged(it) },
                        onStartClick       = { viewModel.toggleSearchExpansion(true, "start") },
                        onDestinationClick = { viewModel.toggleSearchExpansion(true, "dest") },
                        onSwapClick        = { viewModel.swapLocations() },
                        onClose            = { viewModel.onBackToPreview() },
                        onStartNavigation  = { /* future */ },
                        modifier           = Modifier.align(Alignment.BottomCenter)
                    )
                }

                // Inline search results dropdown
                val currentFieldText = when (viewModel.activeSearchField) {
                    "start" -> viewModel.uiBuildingState.startLocationName
                    "dest"  -> viewModel.uiBuildingState.destinationName
                    else    -> viewModel.searchQuery
                }
                if (viewModel.activeSearchField != "main" && currentFieldText.isNotEmpty()) {
                    Card(
                        modifier  = Modifier
                            .padding(horizontal = 24.dp)
                            .offset(y = 190.dp)
                            .zIndex(1f),
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
                                    modifier        = Modifier.clickable {
                                        viewModel.handleSearchResult(result, context)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Next Class Pill (bottom-left, PREVIEW mode only)
            if (viewModel.uiBuildingState.mode == MapUIMode.PREVIEW) {
                NextClassPill(
                    nextEvent       = viewModel.nextUpcomingEvent,
                    onNavigateClick = {
                        val event = viewModel.nextUpcomingEvent
                        if (event != null) {
                            val parsed = parseLocation(event.location ?: "")
                            val dest   = parsed?.buildingCode ?: event.location ?: return@NextClassPill
                            viewModel.navigateToEvent(dest)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 28.dp)
                )
            }

            // 4. Campus toggle + Recenter FAB (PREVIEW mode only)
            if (viewModel.uiBuildingState.mode != MapUIMode.DIRECTIONS) {
                CampusToggle(
                    selectedCampusName = viewModel.currentCampus?.name,
                    onCampusClick      = { name -> viewModel.onCampusSelected(name) },
                    modifier           = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 160.dp)
                )

                ExtendedFloatingActionButton(
                    onClick = {
                        handleRecenter(
                            client        = fusedLocationClient,
                            hasPermission = hasLocationPermission,
                            launcher      = launcher,
                            context       = context,
                            onShowSettings = { showSettingsDialog = true }
                        ) { userLocation ->
                            scope.launch { cameraController.animateTo(userLocation, 18.5f) }
                            viewModel.processLocationUpdate(userLocation, isForce = true)
                        }
                    },
                    modifier       = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 24.dp),
                    containerColor = ConcordiaMaroon,
                    contentColor   = Color.White,
                    icon           = { Icon(Icons.Default.MyLocation, contentDescription = null) },
                    text           = { Text("RECENTER") }
                )
            }

            // 5. Building info / Directions popup
            if (viewModel.uiBuildingState.isVisible) {
                viewModel.uiBuildingState.building?.let { building ->
                    if (viewModel.uiBuildingState.mode == MapUIMode.PREVIEW) {
                        BuildingInfoPopup(
                            building         = building,
                            uiState          = viewModel.uiBuildingState,
                            onDismiss        = { viewModel.handleMapTap(null) },
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
            }

            // 6. Location permission dialog
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
                    dismissButton = {
                        TextButton(onClick = { showSettingsDialog = false }) {
                            Text("CANCEL", color = Color.Gray)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector     = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint            = ConcordiaMaroon
                        )
                    }
                )
            }
        }
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
