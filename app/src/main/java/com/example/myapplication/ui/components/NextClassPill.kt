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
import com.example.myapplication.data.CalendarEvent
import java.text.SimpleDateFormat
import java.util.*

private val ConcordiaMaroon = Color(0xFF912338)

/**
 * Minimal floating pill — one line: course name + time.
 * Tap → immediate navigation to the building.
 * Invisible when [nextEvent] is null.
 */
@Composable
fun NextClassPill(
    nextEvent: CalendarEvent?,
    onNavigateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (nextEvent == null) return

    val nowMs        = System.currentTimeMillis()
    val minutesUntil = ((nextEvent.startTimeMs - nowMs) / 60_000).coerceAtLeast(0)
    val timeLabel    = when {
        minutesUntil == 0L -> "Now"
        minutesUntil < 60  -> "in ${minutesUntil}m"
        else               -> "in ${minutesUntil / 60}h ${minutesUntil % 60}m"
    }
    val urgent = minutesUntil <= 15

    Row(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(if (urgent) ConcordiaMaroon else Color.White)
            .clickable { onNavigateClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector        = Icons.Default.School,
            contentDescription = null,
            tint               = if (urgent) Color.White else ConcordiaMaroon,
            modifier           = Modifier.size(16.dp)
        )
        Text(
            text       = nextEvent.title,
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color      = if (urgent) Color.White else Color(0xFF1C1B1F),
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            modifier   = Modifier.widthIn(max = 160.dp)
        )
        Text(
            text     = timeLabel,
            fontSize = 12.sp,
            color    = if (urgent) Color.White.copy(alpha = 0.85f) else Color(0xFF5F6368)
        )
        Icon(
            imageVector        = Icons.Default.Directions,
            contentDescription = "Navigate",
            tint               = if (urgent) Color.White else ConcordiaMaroon,
            modifier           = Modifier.size(16.dp)
        )
    }
}
