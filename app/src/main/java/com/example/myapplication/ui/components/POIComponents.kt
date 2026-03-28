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
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.poi.POI
import com.example.myapplication.data.poi.POICategory
import com.example.myapplication.ui.theme.ConcordiaMaroon

// ─────────────────────────────────────────────────────────────────────────────
// CategoryFilterRow
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Horizontally scrollable row of category filter chips.
 *
 * SOLID — Single Responsibility: only renders the chip row.
 * Pure composable — no ViewModel dependency, easy to preview/test.
 */
@Composable
fun POICategoryFilterRow(
    selectedCategory: POICategory,
    onCategorySelected: (POICategory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier            = modifier.fillMaxWidth(),
        contentPadding      = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(POICategory.entries) { category ->
            val isSelected = category == selectedCategory
            FilterChip(
                selected = isSelected,
                onClick  = { onCategorySelected(category) },
                label    = {
                    Text(
                        text     = "${category.emoji} ${category.label}",
                        fontSize = 13.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor    = ConcordiaMaroon,
                    selectedLabelColor        = Color.White,
                    selectedLeadingIconColor  = Color.White
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// POIListItem
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Single row in the POI list — emoji icon, name, distance, address.
 */
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Category emoji badge
        Box(
            modifier        = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ConcordiaMaroon.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = poi.category.emoji, fontSize = 20.sp)
        }

        // Name + address
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

        // Distance badge
        Text(
            text       = formatDistance(poi.distanceMeters),
            fontSize   = 12.sp,
            fontWeight = FontWeight.Medium,
            color      = ConcordiaMaroon
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// POIListPanel
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The full bottom panel shown when POI mode is active — category chips + list.
 * Displayed at [Alignment.BottomCenter], consistent with [BuildingInfoPopup].
 */
@Composable
fun POIListPanel(
    pois:             List<POI>,
    selectedCategory: POICategory,
    onCategorySelected: (POICategory) -> Unit,
    onPOISelected:    (POI) -> Unit,
    onClose:          () -> Unit,
    modifier:         Modifier = Modifier
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
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint   = ConcordiaMaroon,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text       = "Nearby Places",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close POI panel")
                }
            }

            // Category filter chips
            POICategoryFilterRow(
                selectedCategory   = selectedCategory,
                onCategorySelected = onCategorySelected
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()

            // POI list
            LazyColumn(
                modifier       = Modifier.heightIn(max = 320.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(pois, key = { it.placeId }) { poi ->
                    POIListItem(poi = poi, onClick = { onPOISelected(poi) })
                    HorizontalDivider(
                        modifier  = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color     = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// POIActionCard
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Bottom card shown when the user has tapped a POI and is deciding whether
 * to request directions.  Replaces [POIListPanel] temporarily.
 */
@Composable
fun POIActionCard(
    poi:                POI,
    onGetDirections:    () -> Unit,
    onDismiss:          () -> Unit,
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
        Column(modifier = Modifier.padding(20.dp)) {
            // Top row — emoji + dismiss
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
                        modifier        = Modifier
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
                            text     = poi.category.label,
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Back to list")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Address + distance
            Text(
                text     = poi.address,
                fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text       = "${formatDistance(poi.distanceMeters)} away",
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium,
                color      = ConcordiaMaroon,
                modifier   = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Get Directions CTA
            Button(
                onClick  = onGetDirections,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = ConcordiaMaroon)
            ) {
                Icon(
                    imageVector  = Icons.Default.Directions,
                    contentDescription = null,
                    modifier     = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Get Directions", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun formatDistance(meters: Int): String = when {
    meters < 1000 -> "${meters} m"
    else          -> "${"%.1f".format(meters / 1000.0)} km"
}
