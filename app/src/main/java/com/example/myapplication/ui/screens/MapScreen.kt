package com.example.myapplication.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
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
import com.example.myapplication.data.LocationResult
import com.example.myapplication.logic.SearchResult
import com.example.myapplication.logic.handleRecenter
import com.example.myapplication.logic.openAppSettings
import com.example.myapplication.map.TrueCameraController
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

import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.launch

/**
 * Full map screen — extracted from MapsActivity into ui/screens per
 * architectural review (Thin Activity pattern).
 *
 * Receives [mapViewModel], [calendarViewModel], and [fusedLocationClient]
 * as parameters so the composable remains independently testable without
 * instantiating a full Activity.
 *
 * All private overlay composables live here, keeping MapsActivity as a
 * pure lifecycle/DI entry point.
 */
@Composable
fun MapScreen(
    mapViewModel: MapViewModel,
    calendarViewModel: CalendarViewModel,
    fusedLocationClient: FusedLocationProviderClient
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    var showSettingsDialog    by remember { mutableStateOf(false) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    ObserveLocationUpdates(hasLocationPermission, fusedLocationClient, mapViewModel)
    val cameraPositionState = rememberMapCamera()
    val cameraController    = remember(cameraPositionState) { TrueCameraController(cameraPositionState) }
    ObserveCameraEffects(cameraPositionState, cameraController, mapViewModel)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasLocationPermission = isGranted }

    val mapPaddingBottom = if (mapViewModel.uiBuildingState.mode == MapUIMode.DIRECTIONS) 600 else 0

    Box(modifier = Modifier.fillMaxSize()) {
        CampusMap(
            currentCampus           = mapViewModel.currentCampus,
            highlightedBuildingName = mapViewModel.highlightedBuildingName,
            cameraPositionState     = cameraPositionState,
            hasLocationPermission   = hasLocationPermission,
            viewModel               = mapViewModel,
            contentPadding          = PaddingValues(bottom = mapPaddingBottom.dp),
            modifier                = Modifier.testTag("campus_map")
        )
        MapSearchOverlay(context = context, viewModel = mapViewModel)
        MapPreviewOverlays(
            mapViewModel          = mapViewModel,
            calendarViewModel     = calendarViewModel,
            fusedLocationClient   = fusedLocationClient,
            hasLocationPermission = hasLocationPermission,
            launcher              = launcher,
            cameraController      = cameraController,
            scope                 = scope,
            onShowSettings        = { showSettingsDialog = true },
            context               = context
        )
        MapBuildingOverlay(viewModel = mapViewModel)
        if (showSettingsDialog && !hasLocationPermission) {
            LocationPermissionDialog(
                onOpenSettings = { openAppSettings(context) },
                onDismiss      = { showSettingsDialog = false }
            )
        }
    }
}

// ── Private overlays ──────────────────────────────────────────────────────────

@Composable
private fun BoxScope.MapSearchOverlay(
    context: android.content.Context,
    viewModel: MapViewModel
) {
    if (viewModel.uiBuildingState.mode == MapUIMode.PREVIEW) {
        CampusSearchBar(
            query         = viewModel.searchQuery,
            results       = viewModel.searchResults,
            onQueryChange = { viewModel.onSearchQueryChanged(it) },
            onResultClick = { viewModel.handleSearchResult(it, context) },
            modifier      = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
        )
    } else {
        DirectionsOverlay(context = context, viewModel = viewModel)
    }
}

@Composable
private fun BoxScope.DirectionsOverlay(
    context: android.content.Context,
    viewModel: MapViewModel
) {
    if (viewModel.uiBuildingState.isSearchExpanded) {
        DirectionsHeader(
            uiState            = viewModel.uiBuildingState,
            onBackClick        = { viewModel.toggleSearchExpansion(false) },
            onStartQueryChange = { viewModel.onSearchQueryChanged(it, field = "start") },
            onDestQueryChange  = { viewModel.onSearchQueryChanged(it, field = "dest") },
            modifier           = Modifier.align(Alignment.TopCenter)
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
            onStartNavigation  = { },
            modifier           = Modifier.align(Alignment.BottomCenter)
        )
    }
    DirectionsSearchResults(context = context, viewModel = viewModel)
}

@Composable
private fun BoxScope.DirectionsSearchResults(
    context: android.content.Context,
    viewModel: MapViewModel
) {
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
    mapViewModel: MapViewModel,
    calendarViewModel: CalendarViewModel,
    fusedLocationClient: FusedLocationProviderClient,
    hasLocationPermission: Boolean,
    launcher: androidx.activity.result.ActivityResultLauncher<String>,
    cameraController: TrueCameraController,
    scope: kotlinx.coroutines.CoroutineScope,
    onShowSettings: () -> Unit,
    context: android.content.Context
) {
    val mode = mapViewModel.uiBuildingState.mode
    if (mode == MapUIMode.PREVIEW) {
        NextClassPill(
            nextEvent       = calendarViewModel.nextUpcomingEvent,
            isUrgent        = calendarViewModel.isNextClassUrgent,
            onNavigateClick = {
                val resolved = calendarViewModel.nextUpcomingEvent ?: return@NextClassPill
                // locationResult already resolved by ViewModel — no parsing here
                val dest = (resolved.locationResult as? LocationResult.Known)
                    ?.location?.buildingCode
                    ?: resolved.location
                    ?: return@NextClassPill
                mapViewModel.navigateToBuildingCode(dest)
            },
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 28.dp)
        )
    }
    if (mode != MapUIMode.DIRECTIONS) {
        CampusToggle(
            selectedCampusName = mapViewModel.currentCampus?.name,
            onCampusClick      = { name -> mapViewModel.onCampusSelected(name) },
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
                    mapViewModel.processLocationUpdate(userLocation, isForce = true)
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
private fun BoxScope.MapBuildingOverlay(viewModel: MapViewModel) {
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
