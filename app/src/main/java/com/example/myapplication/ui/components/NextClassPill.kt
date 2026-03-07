package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.ResolvedCalendarEvent
import java.util.*

private val ConcordiaMaroon = Color(0xFF912338)

/**
 * Minimal floating pill — course name + time until class.
 * Tap → immediate navigation to the building.
 * Invisible when [nextEvent] is null.
 *
 * This composable is fully stateless regarding business rules:
 * - [isUrgent] is computed by [CalendarViewModel] (threshold lives there)
 * - [nextEvent] is a [ResolvedCalendarEvent] — location already parsed
 * The Pill only decides how to render the state it receives.
 */
@Composable
fun NextClassPill(
    nextEvent:       ResolvedCalendarEvent?,
    isUrgent:        Boolean,
    onNavigateClick: () -> Unit,
    modifier:        Modifier = Modifier
) {
    if (nextEvent == null) return

    val nowMs        = System.currentTimeMillis()
    val minutesUntil = ((nextEvent.startTimeMs - nowMs) / 60_000).coerceAtLeast(0)
    val timeLabel    = when {
        minutesUntil == 0L -> "Now"
        minutesUntil < 60  -> "in ${minutesUntil}m"
        else               -> "in ${minutesUntil / 60}h ${minutesUntil % 60}m"
    }

    Row(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(if (isUrgent) ConcordiaMaroon else Color.White)
            .clickable { onNavigateClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector        = Icons.Default.School,
            contentDescription = null,
            tint               = if (isUrgent) Color.White else ConcordiaMaroon,
            modifier           = Modifier.size(16.dp)
        )
        Text(
            text       = nextEvent.title,
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color      = if (isUrgent) Color.White else Color(0xFF1C1B1F),
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            modifier   = Modifier.widthIn(max = 160.dp)
        )
        Text(
            text     = timeLabel,
            fontSize = 12.sp,
            color    = if (isUrgent) Color.White.copy(alpha = 0.85f) else Color(0xFF5F6368)
        )
        Icon(
            imageVector        = Icons.Default.Directions,
            contentDescription = "Navigate",
            tint               = if (isUrgent) Color.White else ConcordiaMaroon,
            modifier           = Modifier.size(16.dp)
        )
    }
}
