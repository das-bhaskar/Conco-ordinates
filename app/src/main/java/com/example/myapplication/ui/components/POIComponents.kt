package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.poi.POI
import com.example.myapplication.data.poi.POICategory
import com.example.myapplication.logic.formatDistance
import com.example.myapplication.ui.theme.ConcordiaMaroon

// ─────────────────────────────────────────────────────────────────────────────
// POICategoryFilterRow
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun POICategoryFilterRow(
    selectedCategory:   POICategory,
    onCategorySelected: (POICategory) -> Unit,
    modifier:           Modifier = Modifier
) {
    LazyRow(
        modifier              = modifier.fillMaxWidth(),
        contentPadding        = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(POICategory.entries, key = { it.name }) { category ->
            val isSelected = category == selectedCategory
            FilterChip(
                selected = isSelected,
                onClick  = { onCategorySelected(category) },
                label    = { Text("${category.emoji} ${category.label}", fontSize = 13.sp) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor   = ConcordiaMaroon,
                    selectedLabelColor       = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// POIListItem
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun POIListItem(
    poi:      POI,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier         = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ConcordiaMaroon.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = poi.category.emoji, fontSize = 20.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = poi.name,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 14.sp,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Text(
                text     = poi.address,
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text       = formatDistance(poi.distanceMeters),
            fontSize   = 12.sp,
            fontWeight = FontWeight.Medium,
            color      = ConcordiaMaroon
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// POIListPanel  (with List / Map toggle in header)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * @param state UI data required to render the panel.
 * @param actions User interactions emitted by the panel.
 */
@Composable
fun POIListPanel(
    state: POIListPanelState,
    actions: POIListPanelActions,
    modifier:           Modifier = Modifier
) {
    Card(
        modifier  = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape     = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            POIListHeader(state = state, actions = actions)

            // ── Category filter chips (always visible) ────────────────────
            POICategoryFilterRow(
                selectedCategory   = state.selectedCategory,
                onCategorySelected = actions.onCategorySelected
            )

            POIListContent(state = state, actions = actions)
        }
    }
}

@Composable
private fun POIListHeader(
    state: POIListPanelState,
    actions: POIListPanelActions
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.Place,
                contentDescription = null,
                tint               = ConcordiaMaroon,
                modifier           = Modifier.size(20.dp)
            )
            Text(
                text       = stringResource(R.string.poi_nearby_places),
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = actions.onToggleView) {
                Icon(
                    imageVector = if (state.isMapView) Icons.Default.List else Icons.Default.Map,
                    contentDescription = if (state.isMapView) {
                        stringResource(R.string.poi_show_list)
                    } else {
                        stringResource(R.string.poi_show_map)
                    },
                    tint = ConcordiaMaroon
                )
            }
            IconButton(onClick = actions.onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.poi_close_panel)
                )
            }
        }
    }
}

@Composable
private fun POIListContent(
    state: POIListPanelState,
    actions: POIListPanelActions
) {
    if (state.isMapView) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text     = stringResource(R.string.poi_tap_marker_hint),
            fontSize = 13.sp,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        return
    }

    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider()
    LazyColumn(
        modifier       = Modifier.heightIn(max = 320.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(state.pois, key = { it.placeId }) { poi ->
            POIListItem(poi = poi, onClick = { actions.onPOISelected(poi) })
            HorizontalDivider(
                modifier  = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color     = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

data class POIListPanelState(
    val pois: List<POI>,
    val selectedCategory: POICategory,
    val isMapView: Boolean
)

data class POIListPanelActions(
    val onCategorySelected: (POICategory) -> Unit,
    val onPOISelected: (POI) -> Unit,
    val onClose: () -> Unit,
    val onToggleView: () -> Unit
)

// ─────────────────────────────────────────────────────────────────────────────
// POIActionCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun POIActionCard(
    poi:             POI,
    onGetDirections: () -> Unit,
    onDismiss:       () -> Unit,
    modifier:        Modifier = Modifier
) {
    Card(
        modifier  = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape     = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier         = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ConcordiaMaroon.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = poi.category.emoji, fontSize = 22.sp)
                    }
                    Column {
                        Text(
                            text       = poi.name,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 16.sp,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                        Text(
                            text  = poi.category.label,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.poi_back_to_list)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text     = poi.address,
                fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text       = stringResource(
                    R.string.poi_distance_away,
                    formatDistance(poi.distanceMeters)
                ),
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium,
                color      = ConcordiaMaroon,
                modifier   = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick  = onGetDirections,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = ConcordiaMaroon)
            ) {
                Icon(
                    imageVector        = Icons.Default.Directions,
                    contentDescription = null,
                    modifier           = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.poi_get_directions),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
