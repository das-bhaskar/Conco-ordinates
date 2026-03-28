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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.poi.POI
import com.example.myapplication.ui.models.POIUiState
import com.example.myapplication.ui.theme.ConcordiaMaroon
import com.example.myapplication.ui.viewmodel.MapViewModel
import com.example.myapplication.ui.viewmodel.POIViewModel

/**
 * Top-level overlay composable for Epic 5 — POI discovery + directions.
 *
 * Designed to be dropped into [MapScreen]'s root [Box] as a single line,
 * alongside [MapBuildingOverlay] — zero modifications to any existing file.
 *
 * SOLID — Single Responsibility:
 *   Only wires [POIViewModel] state to the correct child composable.
 *   It never fetches data or mutates state itself.
 *
 * Design Pattern — Facade:
 *   Hides the complexity of 4 different UI states behind one call-site.
 *   MapScreen only calls MapPOIOverlay(...) and doesn't know the internals.
 */
@Composable
fun BoxScope.MapPOIOverlay(
    poiViewModel: POIViewModel,
    mapViewModel: MapViewModel,
    modifier:     Modifier = Modifier
) {
    val uiState by poiViewModel.uiState.collectAsState()

    // ── Explore FAB — only visible when panel is hidden ───────────────────
    AnimatedVisibility(
        visible = uiState is POIUiState.Hidden,
        enter   = fadeIn() + scaleIn(),
        exit    = fadeOut() + scaleOut(),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 12.dp, bottom = 100.dp)     // sits above RECENTER FAB
    ) {
        ExtendedFloatingActionButton(
            onClick        = { poiViewModel.openPOIPanel() },
            containerColor = ConcordiaMaroon,
            contentColor   = Color.White,
            icon           = { Icon(Icons.Default.Explore, contentDescription = null) },
            text           = { Text("EXPLORE") }
        )
    }

    // ── Bottom panel area — animated slide-up ─────────────────────────────
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
                    // User tapped a POI → show action card
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
                    // Normal list view
                    POIListPanel(
                        pois               = state.pois,
                        selectedCategory   = state.selectedCategory,
                        onCategorySelected = { poiViewModel.onCategorySelected(it) },
                        onPOISelected      = { poiViewModel.onPOISelected(it) },
                        onClose            = { poiViewModel.closePOIPanel() }
                    )
                }
            }

            is POIUiState.Hidden -> { /* never shown — AnimatedVisibility guards this */ }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Auxiliary state panels (Loading / Error / Empty)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun POILoadingPanel(onClose: () -> Unit) {
    POIShellCard(onClose = onClose) {
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .height(120.dp),
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
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "⚠️ $message", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onRetry) { Text("Retry", color = ConcordiaMaroon) }
        }
    }
}

@Composable
private fun POIEmptyPanel(onClose: () -> Unit) {
    POIShellCard(onClose = onClose) {
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .padding(20.dp),
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

/** Shared card shell for Loading / Error / Empty states. */
@Composable
private fun POIShellCard(
    onClose:  () -> Unit,
    content:  @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Nearby Places", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector        = androidx.compose.material.icons.Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }
            content()
        }
    }
}
