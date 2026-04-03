package com.example.myapplication.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.myapplication.R
import com.example.myapplication.data.poi.POI
import com.example.myapplication.data.poi.POICategory
import com.example.myapplication.ui.models.POIUiState
import com.example.myapplication.ui.theme.ConcordiaMaroon
import java.net.URI
import java.net.URLConnection

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
    state: MapPOIOverlayState,
    actions: MapPOIOverlayActions,
    modifier: Modifier = Modifier
) {
    // Toggle lives here — purely UI concern, no business logic
    var isMapView by remember { mutableStateOf(false) }

    // Reset to list view whenever panel is re-opened
    // (so user always lands on the list first)

    // ── Explore FAB — visible only when panel is fully hidden ─────────────
    AnimatedVisibility(
        visible  = state.uiState is POIUiState.Hidden && state.showExploreFab,
        enter    = fadeIn() + scaleIn(),
        exit     = fadeOut() + scaleOut(),
        modifier = modifier
            .align(Alignment.BottomEnd)
            .padding(end = 12.dp, bottom = 100.dp)
    ) {
        ExtendedFloatingActionButton(
            onClick        = {
                isMapView = false          // always start in list mode
                actions.onOpenPanel()
            },
            containerColor = ConcordiaMaroon,
            contentColor   = Color.White,
            icon           = { Icon(Icons.Default.Explore, contentDescription = null) },
            text           = { Text(stringResource(R.string.poi_explore)) }
        )
    }

    // ── Bottom panel — animated slide-up ──────────────────────────────────
    AnimatedVisibility(
        visible  = state.uiState !is POIUiState.Hidden,
        enter    = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit     = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier.align(Alignment.BottomCenter)
    ) {
        when (val uiState = state.uiState) {

            is POIUiState.Loading -> POILoadingPanel(
                onClose = actions.onClosePanel
            )

            is POIUiState.Error -> POIErrorPanel(
                message = uiState.message,
                onRetry = actions.onRetry,
                onClose = actions.onClosePanel
            )

            is POIUiState.Empty -> POIEmptyPanel(
                onClose = actions.onClosePanel
            )

            is POIUiState.Browse -> POIBrowsePanel(
                state = uiState,
                isMapView = isMapView,
                onCategorySelected = actions.onCategorySelected,
                onPOISelected = actions.onPOISelected,
                onClose = actions.onClosePanel,
                onToggleView = { isMapView = !isMapView }
            )

            is POIUiState.Selection -> POISelectionPanel(
                state = uiState,
                onGetDirections = actions.onNavigateToPOI,
                onDismiss = actions.onPOIDismissed
            )

            is POIUiState.Hidden -> { /* guarded by AnimatedVisibility */ }
        }
    }
}

data class MapPOIOverlayState(
    val uiState: POIUiState,
    val showExploreFab: Boolean
)

data class MapPOIOverlayActions(
    val onOpenPanel: () -> Unit,
    val onClosePanel: () -> Unit,
    val onRetry: () -> Unit,
    val onCategorySelected: (POICategory) -> Unit,
    val onPOISelected: (POI) -> Unit,
    val onPOIDismissed: () -> Unit,
    val onNavigateToPOI: (POI) -> Unit
)

@Composable
private fun POIBrowsePanel(
    state: POIUiState.Browse,
    isMapView: Boolean,
    onCategorySelected: (POICategory) -> Unit,
    onPOISelected: (POI) -> Unit,
    onClose: () -> Unit,
    onToggleView: () -> Unit
) {
    POIListPanel(
        state = POIListPanelState(
            pois = state.pois,
            selectedCategory = state.selectedCategory,
            isMapView = isMapView
        ),
        actions = POIListPanelActions(
            onCategorySelected = onCategorySelected,
            onPOISelected = onPOISelected,
            onClose = onClose,
            onToggleView = onToggleView
        )
    )
}

@Composable
private fun POISelectionPanel(
    state: POIUiState.Selection,
    onGetDirections: (POI) -> Unit,
    onDismiss: () -> Unit
) {
    POIActionCard(
        poi = state.selectedPOI,
        onGetDirections = { onGetDirections(state.selectedPOI) },
        onDismiss = onDismiss
    )
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
            Text(
                text = stringResource(R.string.poi_error_message, message),
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.poi_retry), color = ConcordiaMaroon)
            }
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
                text  = stringResource(R.string.poi_empty_message),
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
                Text(
                    stringResource(R.string.poi_nearby_places),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.poi_close))
                }
            }
            content()
        }
    }
}
