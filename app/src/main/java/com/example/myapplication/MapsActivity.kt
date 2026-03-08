package com.example.myapplication

import android.Manifest
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
import com.example.myapplication.analytics.AnalyticsRegistry
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.data.LocationResult
import com.example.myapplication.data.ShuttleRepo
import com.example.myapplication.logic.AuthRepository
import com.example.myapplication.logic.DefaultShuttleService
import com.example.myapplication.logic.GoogleCalendarProvider
import com.example.myapplication.logic.SearchResult
import com.example.myapplication.logic.SharedPrefsCalendarPreferences
import com.example.myapplication.logic.TrueLocationProvider
import com.example.myapplication.logic.handleRecenter
import com.example.myapplication.logic.openAppSettings
import com.example.myapplication.map.TrueCameraController
import com.example.myapplication.telemetry.CrashReporter
import com.example.myapplication.ui.screens.AppNavigation
import com.example.myapplication.ui.components.BuildingInfoPopup
import com.example.myapplication.ui.components.CampusMap
import com.example.myapplication.ui.components.CampusSearchBar
import com.example.myapplication.ui.components.CampusToggle
import com.example.myapplication.ui.components.DirectionsHeader
import com.example.myapplication.ui.components.DirectionsInfoPopup
import com.example.myapplication.ui.components.LocationPermissionDialog
import com.example.myapplication.ui.components.NextClassPill
import com.example.myapplication.ui.components.ObserveCameraEffects
import com.example.myapplication.ui.components.ObserveLocationUpdates
import com.example.myapplication.ui.components.rememberMapCamera
import com.example.myapplication.ui.models.MapUIMode
import com.example.myapplication.ui.theme.ConcordiaMaroon
import com.example.myapplication.ui.viewmodel.CalendarViewModel
import com.example.myapplication.ui.viewmodel.MapViewModel
import com.google.android.gms.location.LocationServices
import com.example.myapplication.ui.screens.MapScreen
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch

/**
 * Single-Activity entry point.
 *
 * Responsibilities limited to:
 *  1. Lifecycle management (onCreate)
 *  2. Dependency wiring (composition root)
 *  3. ActivityResultLauncher registration (must live in Activity)
 *  4. Google Sign-In / Sign-Out (requires authRepository)
 *
 * All UI is delegated to composables in their own files:
 *  - AppNavigation.kt  — bottom nav + tab switching
 *  - MapEffects.kt     — camera and location side effects
 *  - MapDialogs.kt     — permission dialog
 *  - LocationUtils.kt  — recenter + openAppSettings helpers
 */
class MapsActivity : ComponentActivity() {

    // ── Dependencies (Activity-scoped) ────────────────────────────────────────
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: MapViewModel
    private lateinit var calendarViewModel: CalendarViewModel
    private lateinit var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient

    // ── Google Sign-In (must live here — needs ActivityResultLauncher) ─────────
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn
            .getSignedInAccountFromIntent(result.data)
        if (task.isSuccessful) {
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

        authRepository = AuthRepository(context = applicationContext)
        val tokenProvider: suspend () -> String? = { authRepository.getCalendarToken() }

        val locationProvider = TrueLocationProvider(fusedLocationClient)
        val routeProvider    = com.example.myapplication.logic.GoogleRouteProvider(BuildConfig.MAPS_API_KEY)
        val calendarProvider = GoogleCalendarProvider(context = this, tokenProvider = tokenProvider)

        viewModel = MapViewModel(
            locationProvider = locationProvider,
            routeProvider    = routeProvider,
            shuttleService   = DefaultShuttleService(ShuttleRepo),
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
        viewModel.initSearch(placesClient)

        setContent {
            AppNavigation(
                calendarViewModel = calendarViewModel,
                navigationActions = com.example.myapplication.ui.screens.NavigationActions(
                    onNavigateToMap  = { buildingCode -> viewModel.navigateToBuildingCode(buildingCode) },
                    onConnectClick   = { connectCalendar() },
                    onSignOutClick   = { signOutCalendar() }
                ),
                onScreenVisible = { screenRoute ->
                    AnalyticsRegistry.provider().trackScreenView(screenRoute)
                },
                userEmail         = authRepository.getSignedInEmail() ?: "",
                mapContent        = {
                    MapScreen(
                        mapViewModel           = viewModel,
                        currentCampus          = viewModel.currentCampus,
                        highlightedBuildingName= viewModel.highlightedBuildingName,
                        searchQuery            = viewModel.searchQuery,
                        searchResults          = viewModel.searchResults,
                        activeSearchField      = viewModel.activeSearchField,
                        nextClassEvent         = calendarViewModel.nextUpcomingEvent,
                        isNextClassUrgent      = calendarViewModel.isNextClassUrgent,
                        nextClassTimeRemaining = calendarViewModel.nextClassTimeRemaining,
                        fusedLocationClient    = fusedLocationClient,
                        onSearchQueryChanged   = { q, f -> viewModel.onSearchQueryChanged(q, f) },
                        onSearchResult         = { r, ctx -> viewModel.handleSearchResult(r, ctx) },
                        onTransportModeChanged = { viewModel.onTransportModeChanged(it) },
                        onToggleSearchExpansion= { e, f -> viewModel.toggleSearchExpansion(e, f) },
                        onSwapLocations        = { viewModel.swapLocations() },
                        onBackToPreview        = { viewModel.onBackToPreview() },
                        onCampusSelected       = { viewModel.onCampusSelected(it) },
                        onBuildingDismiss      = { viewModel.handleMapTap(null) },
                        onDirectionsRequested  = { viewModel.onDirectionsRequested() },
                        onLocationUpdate       = { loc, force -> viewModel.processLocationUpdate(loc, force) },
                        onNavigateToBuilding   = { viewModel.navigateToBuildingCode(it) }
                    )
                }
            )
        }
    }

    private fun connectCalendar() {
        authRepository.revokeAndSignIn { intent -> signInLauncher.launch(intent) }
    }

    private fun signOutCalendar() {
        authRepository.signOut {
            calendarViewModel.clearSelection()
        }
    }
}
