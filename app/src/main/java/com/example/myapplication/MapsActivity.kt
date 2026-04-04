package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.myapplication.analytics.AnalyticsRegistry
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.data.ShuttleRepo
import com.example.myapplication.data.indoor.BuildingEntrance
import com.example.myapplication.data.indoor.BuildingEntrances
import com.example.myapplication.data.indoor.IndoorRepository
import com.example.myapplication.data.poi.PlacesPOIRepository
import com.example.myapplication.logic.AuthRepository
import com.example.myapplication.logic.DefaultShuttleService
import com.example.myapplication.logic.GoogleCalendarProvider
import com.example.myapplication.logic.IndoorJourneyHandler
import com.example.myapplication.logic.SharedPrefsCalendarPreferences
import com.example.myapplication.logic.TrueLocationProvider
import com.example.myapplication.telemetry.CrashReporter
import com.example.myapplication.ui.components.IndoorJourneyDialogs
import com.example.myapplication.ui.models.IndoorJourneyPhase
import com.example.myapplication.ui.screens.AppNavigation
import com.example.myapplication.ui.screens.IndoorActions
import com.example.myapplication.ui.screens.IndoorNavParams
import com.example.myapplication.ui.screens.IndoorNavScreen
import com.example.myapplication.ui.screens.MapScreen
import com.example.myapplication.ui.components.PoiActions
import com.example.myapplication.ui.viewmodel.CalendarViewModel
import com.example.myapplication.ui.viewmodel.MapViewModel
import com.example.myapplication.ui.viewmodel.POIViewModel
import com.google.android.gms.location.LocationServices

class MapsActivity : ComponentActivity() {

    private lateinit var authRepository:      AuthRepository
    private lateinit var viewModel:           MapViewModel
    private lateinit var calendarViewModel:   CalendarViewModel
    private lateinit var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient

    // ── POI ViewModel — lifecycle-aware, REST-backed ───────────────────────
    private val poiViewModel: POIViewModel by viewModels {
        POIViewModel.Factory(repository = PlacesPOIRepository(BuildConfig.MAPS_API_KEY))
    }

    // ── Google Sign-In ─────────────────────────────────────────────────────
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn
            .getSignedInAccountFromIntent(result.data)

        if (task.isSuccessful) {
            calendarViewModel.clearSelection()
            calendarViewModel.loadCalendarsAndAutoSelect()
        } else {
            val exception = task.exception
            val errorMessage = when {
                exception is com.google.android.gms.common.api.ApiException ->
                    "Connection failed. Please check your internet or try again."
                else -> "Login cancelled or failed."
            }
            calendarViewModel.setAuthError(errorMessage)
            CrashReporter.recordNonFatal(exception ?: Exception("Sign-in failed"), "google_sign_in_failed")
        }
    }

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
        BuildingEntrances.initialize(this)   // ← load building_entrances.json

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        authRepository      = AuthRepository(context = applicationContext)

        val tokenProvider: suspend () -> String? = { authRepository.getCalendarToken() }
        val locationProvider = TrueLocationProvider(fusedLocationClient)
        val routeProvider    = com.example.myapplication.logic.GoogleRouteProvider(BuildConfig.MAPS_API_KEY)
        val calendarProvider = GoogleCalendarProvider(context = this, tokenProvider = tokenProvider)
        val indoorRepo       = IndoorRepository(applicationContext)  // ← for indoor room search

        viewModel = MapViewModel(
            locationProvider  = locationProvider,
            routeProvider     = routeProvider,
            shuttleService    = DefaultShuttleService(ShuttleRepo),
            analyticsProvider = AnalyticsRegistry.provider()
        )
        calendarViewModel = CalendarViewModel(
            calendarProvider    = calendarProvider,
            calendarPreferences = SharedPrefsCalendarPreferences(this),
            locationResolver    = com.example.myapplication.logic.LocationResolver(
                com.example.myapplication.data.CampusBuildingNameProvider()
            )
        )

        val placesClient = com.google.android.libraries.places.api.Places.createClient(this)
        viewModel.initSearch(
            com.example.myapplication.logic.HybridSearchProvider(placesClient, indoorRepo)
        )

        setContent {
            AppNavigation(
                calendarViewModel = calendarViewModel,
                navigationActions = com.example.myapplication.ui.screens.NavigationActions(
                    onNavigateToMap = { event -> viewModel.navigateToCalendarEvent(event) },
                    onConnectClick  = { connectCalendar() },
                    onSignOutClick  = { signOutCalendar() }
                ),
                onScreenVisible = { screenRoute ->
                    AnalyticsRegistry.provider().trackScreenView(screenRoute)
                },
                userEmail  = authRepository.getSignedInEmail() ?: "",
                mapContent = {
                    MapContent(
                        mapViewModel        = viewModel,
                        poiViewModel        = poiViewModel,
                        calendarViewModel   = calendarViewModel,
                        fusedLocationClient = fusedLocationClient
                    )
                }
            )
        }
    }

    private fun connectCalendar() {
        authRepository.revokeAndSignIn { intent -> signInLauncher.launch(intent) }
    }

    private fun signOutCalendar() {
        authRepository.signOut { calendarViewModel.clearSelection() }
    }
}

// ── MapContent ────────────────────────────────────────────────────────────────

@Composable
private fun MapContent(
    mapViewModel:        MapViewModel,
    poiViewModel:        POIViewModel,
    calendarViewModel:   CalendarViewModel,
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient
) {
    var indoorTarget by remember { mutableStateOf<Pair<String, Int>?>(null) }
    val poiUiState by poiViewModel.uiState.collectAsState()

    // indoorNavTarget is derived inside MapViewModel from the journey phase.
    val journeyIndoorTarget = mapViewModel.indoorNavTarget

    // ── Main map screen ───────────────────────────────────────────────────────
    MapScreen(
        mapViewModel             = mapViewModel,
        poiUiState               = poiUiState,
        poiActions               = PoiActions(
            onLocationUpdate = poiViewModel::onLocationUpdated,
            onOpenPanel = poiViewModel::openPOIPanel,
            onClosePanel = poiViewModel::closePOIPanel,
            onRetry = poiViewModel::openPOIPanel,
            onCategorySelected = poiViewModel::onCategorySelected,
            onPOISelected = poiViewModel::onPOISelected,
            onPOIDismissed = poiViewModel::onPOIDismissed,
            onNavigateToPOI = { poi ->
                mapViewModel.navigateToPOI(name = poi.name, latLng = poi.latLng)
                poiViewModel.closePOIPanel()
            }
        ),
        currentCampus            = mapViewModel.currentCampus,
        highlightedBuildingName  = mapViewModel.highlightedBuildingName,
        searchQuery              = mapViewModel.searchQuery,
        searchResults            = mapViewModel.searchResults,
        activeSearchField        = mapViewModel.activeSearchField,
        nextClassEvent           = calendarViewModel.nextUpcomingEvent,
        isNextClassUrgent        = calendarViewModel.isNextClassUrgent,
        nextClassTimeRemaining   = calendarViewModel.nextClassTimeRemaining,
        fusedLocationClient      = fusedLocationClient,
        onSearchQueryChanged     = { q, f -> mapViewModel.onSearchQueryChanged(q, f) },
        onSearchResult           = { r, ctx -> mapViewModel.handleSearchResult(r, ctx) },
        onTransportModeChanged   = { mapViewModel.onTransportModeChanged(it) },
        onToggleSearchExpansion  = { e, f -> mapViewModel.toggleSearchExpansion(e, f) },
        onSwapLocations          = { mapViewModel.swapLocations() },
        onBackToPreview          = { mapViewModel.onBackToPreview() },
        onCampusSelected         = { mapViewModel.onCampusSelected(it) },
        onBuildingDismiss        = { mapViewModel.handleMapTap(null) },
        onDirectionsRequested    = { mapViewModel.onDirectionsRequested() },
        onLocationUpdate         = { loc, force -> mapViewModel.processLocationUpdate(loc, force) },
        onNavigateToBuilding     = { mapViewModel.navigateToCalendarEvent(it) },
        onStartNavigationActions = { mapViewModel.startNavigation() },
        indoorActions            = IndoorActions(
            onIndoorMapClick = {
                val code = mapViewModel.uiBuildingState.building?.code ?: return@IndoorActions
                indoorTarget = Pair(code, 1)
            }
        )
    )

    val journeyPhase = mapViewModel.indoorJourneyState.phase

    // ── Indoor journey dialogs ────────────────────────────────────────────────
    IndoorJourneyDialogs(
        phase          = journeyPhase,
        onSearchRoom   = { query, buildingCode ->
            mapViewModel.searchCurrentRoom(query, buildingCode)
        },
        isSearching    = mapViewModel.indoorRoomSearching,
        searchError    = mapViewModel.indoorRoomSearchError,
        onRoomResolved = { nodeId, label, buildingCode, floor ->
            mapViewModel.onCurrentRoomSelected(nodeId, label, buildingCode, floor)
        },
        onEntranceSelected = { entrance -> mapViewModel.onEntranceSelected(entrance) },
        onDismiss      = { mapViewModel.clearJourney() }
    )

    // ── Simple indoor map (building popup → Indoor button) ────────────────────
    AnimatedVisibility(
        visible  = indoorTarget != null,
        enter    = slideInVertically { it },
        exit     = slideOutVertically { it },
        modifier = Modifier.fillMaxSize()
    ) {
        indoorTarget?.let { (code, floor) ->
            IndoorNavScreen(IndoorNavParams(
                building        = code,
                availableFloors = com.example.myapplication.data.indoor.IndoorBuildingConfig.floorsFor(code),
                initialFloor    = floor,
                sessionKey      = "preview",
                onBack          = { indoorTarget = null }
            ))
        }
    }

    // ── Full journey indoor nav (search-triggered) ────────────────────────────
    AnimatedVisibility(
        visible  = journeyIndoorTarget != null,
        enter    = slideInVertically { it },
        exit     = slideOutVertically { it },
        modifier = Modifier.fillMaxSize()
    ) {
        journeyIndoorTarget?.let { (code, floor, startNode) ->
            val phase = journeyPhase

            val destination = when (phase) {
                is IndoorJourneyPhase.IndoorToExit ->
                    // Destination is the exit node of the current building
                    com.example.myapplication.logic.IndoorOutdoorRouter.IndoorDestination(
                        building = phase.buildingCode,
                        floor    = phase.exitFloor,
                        nodeId   = phase.exitNodeId,
                        label    = "Exit"
                    )
                is IndoorJourneyPhase.IndoorToDestination ->
                    // Same building, possibly different floor.
                    // nodeId may be null if the search couldn't find the node at query time;
                    // IndoorNavViewModel.navigateTo will resolve it from the floor JSON.
                    com.example.myapplication.logic.IndoorOutdoorRouter.IndoorDestination(
                        building = phase.destination.buildingCode,
                        floor    = phase.destination.floor,
                        nodeId   = phase.destination.nodeId ?: "",  // "" triggers re-resolution in VM
                        label    = phase.destination.label
                    ).also {
                        android.util.Log.d("JOURNEY",
                            "IndoorToDestination: building=${phase.destination.buildingCode}" +
                            " floor=${phase.destination.floor}" +
                            " nodeId='${phase.destination.nodeId}'" +
                            " startFloor=${phase.startFloor}" +
                            " startNodeId=${phase.startNodeId}")
                    }
                else -> null
            }

            IndoorNavScreen(IndoorNavParams(
                building        = code,
                availableFloors = com.example.myapplication.data.indoor.IndoorBuildingConfig.floorsFor(code),
                initialFloor    = floor,
                destination     = destination,
                startNodeId     = startNode,
                startFloor      = floor,
                sessionKey      = "journey",
                onConfirmExit   = if (phase is IndoorJourneyPhase.IndoorToExit) {
                    { mapViewModel.onUserExited() }
                } else null,
                onBack = { mapViewModel.clearJourney() },
                onOutdoorHandoff = { outdoorSeg ->
                    mapViewModel.startOutdoorLeg(
                        origin      = outdoorSeg.origin,
                        destination = outdoorSeg.destination,
                        destLabel   = outdoorSeg.destLabel
                    )
                }
            ))
        }
    }
}

// floorsFor(code) is provided by IndoorBuildingConfig.floorsFor(code)
// which is the single source of truth for building floor data.
