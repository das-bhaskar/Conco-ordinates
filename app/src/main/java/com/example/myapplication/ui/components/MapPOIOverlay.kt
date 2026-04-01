package com.example.myapplication.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.models.POIUiState
import com.example.myapplication.ui.theme.ConcordiaMaroon
import com.example.myapplication.ui.viewmodel.MapViewModel
import com.example.myapplication.ui.viewmodel.POIViewModel

/**
 * Top-level overlay composable for Epic 5 — POI discovery + directions.
 *
 * Owns [isMapView] toggle state locally — no ViewModel change needed.
 * In map-view mode the panel collapses to just the header + chips so the
 * user can see the markers on the map; tapping a marker raises [POIActionCard].
 *
 * Design Pattern — Facade: MapScreen still calls a single composable.
 * SOLID — Single Responsibility: only wires state → child composables.
 */
@Composable
fun BoxScope.MapPOIOverlay(
    poiViewModel: POIViewModel,
    mapViewModel: MapViewModel,
    modifier:     Modifier = Modifier
) {
    val uiState by poiViewModel.uiState.collectAsState()

    // Toggle lives here — purely UI concern, no business logic
    var isMapView by remember { mutableStateOf(false) }

    // Reset to list view whenever panel is re-opened
    // (so user always lands on the list first)

    // ── Explore FAB — visible only when panel is fully hidden ─────────────
    AnimatedVisibility(
        visible  = uiState is POIUiState.Hidden && !mapViewModel.uiBuildingState.isVisible,
        enter    = fadeIn() + scaleIn(),
        exit     = fadeOut() + scaleOut(),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 12.dp, bottom = 100.dp)
    ) {
        ExtendedFloatingActionButton(
            onClick        = {
                isMapView = false          // always start in list mode
                poiViewModel.openPOIPanel()
            },
            containerColor = ConcordiaMaroon,
            contentColor   = Color.White,
            icon           = { Icon(Icons.Default.Explore, contentDescription = null) },
            text           = { Text("EXPLORE") }
        )
    }

    // ── Bottom panel — animated slide-up ──────────────────────────────────
    AnimatedVisibility(
        visible  = uiState !is POIUiState.Hidden,
        enter    = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit     = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        when (val state = uiState) {

            is POIUiState.Loading -> POILoadingPanel(
                onClose = { poiViewModel.closePOIPanel() }
            )

            is POIUiState.Error -> POIErrorPanel(
                message = state.message,
                onRetry = { poiViewModel.openPOIPanel() },
                onClose = { poiViewModel.closePOIPanel() }
            )

            is POIUiState.Empty -> POIEmptyPanel(
                onClose = { poiViewModel.closePOIPanel() }
            )

            is POIUiState.Success -> {
                if (state.selectedPOI != null) {
                    // POI tapped (from list OR map marker) → action card
                    POIActionCard(
                        poi             = state.selectedPOI,
                        onGetDirections = {
                            mapViewModel.navigateToPOI(
                                name   = state.selectedPOI.name,
                                latLng = state.selectedPOI.latLng
                            )
                            poiViewModel.closePOIPanel()
                        },
                        onDismiss = { poiViewModel.onPOIDismissed() }
                    )
                } else {
                    // List panel — collapses in map-view mode
                    POIListPanel(
                        pois               = state.pois,
                        selectedCategory   = state.selectedCategory,
                        onCategorySelected = { poiViewModel.onCategorySelected(it) },
                        onPOISelected      = { poiViewModel.onPOISelected(it) },
                        onClose            = { poiViewModel.closePOIPanel() },
                        isMapView          = isMapView,
                        onToggleView       = { isMapView = !isMapView }
                    )
                }
            }

            is POIUiState.Hidden -> { /* guarded by AnimatedVisibility */ }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Auxiliary panels
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun POILoadingPanel(onClose: () -> Unit) {
    POIShellCard(onClose = onClose) {
        Box(
            modifier         = Modifier.fillMaxWidth().height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = ConcordiaMaroon)
        }
    }
}

@Composable
private fun POIErrorPanel(message: String, onRetry: () -> Unit, onClose: () -> Unit) {
    POIShellCard(onClose = onClose) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("⚠️ $message", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onRetry) { Text("Retry", color = ConcordiaMaroon) }
        }
    }
}

@Composable
private fun POIEmptyPanel(onClose: () -> Unit) {
    POIShellCard(onClose = onClose) {
        Box(
            modifier         = Modifier.fillMaxWidth().padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = "No places found nearby. Try a different category or move the map.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun POIShellCard(
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Nearby Places", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            content()
        }
    }
}
