package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.data.CalendarEvent
import java.text.SimpleDateFormat
import java.util.*

private val ConcordiaMaroon = Color(0xFF912338)
private val TodayHighlight  = Color(0xFF912338)
private val TodayColumnBg   = Color(0xFFFFEEF0)
private val GridLineColor   = Color(0xFFE8E8E8)
private val EventColors     = listOf(
    Color(0xFF1A73E8), Color(0xFF0F9D58), Color(0xFFE37400), Color(0xFF7B1FA2)
)
private val TimeColumnWidth = 48.dp
private val HourHeightDp    = 60.dp
private val HeaderHeightDp  = 52.dp

@Composable
fun WeekCalendarView(
    weekStartMs: Long,
    events: List<CalendarEvent>,
    isLoading: Boolean,
    isSignedIn: Boolean = false,
    userEmail: String = "",
    onConnectClick: () -> Unit = {},
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onNavigateToEvent: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingEvent by remember { mutableStateOf<CalendarEvent?>(null) }

    Column(modifier = modifier.fillMaxSize().background(Color.White)) {
        CalendarTopBar(isSignedIn, userEmail, onConnectClick)
        WeekNavigationRow(weekStartMs, onPreviousWeek, onNextWeek)
        DayColumnHeaders(weekStartMs)
        HorizontalDivider(color = GridLineColor, thickness = 0.5.dp)

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ConcordiaMaroon, modifier = Modifier.size(32.dp))
            }
        } else if (!isSignedIn) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("Connect Google Calendar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap the account icon above to sign in", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onConnectClick,
                        colors  = ButtonDefaults.buttonColors(containerColor = ConcordiaMaroon),
                        shape   = RoundedCornerShape(12.dp)
                    ) { Text("Connect") }
                }
            }
        } else {
            TimeGrid(weekStartMs, events) { pendingEvent = it }
        }
    }

    pendingEvent?.let { event ->
        NavigationConfirmDialog(
            event     = event,
            onConfirm = { pendingEvent = null; onNavigateToEvent(event) },
            onDismiss = { pendingEvent = null }
        )
    }
}

@Composable
private fun CalendarTopBar(isSignedIn: Boolean, userEmail: String, onConnectClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(HeaderHeightDp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Schedule", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color(0xFF3C4043))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable { onConnectClick() }.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Default.AccountCircle, contentDescription = "Account",
                tint = if (isSignedIn) ConcordiaMaroon else Color.Gray, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(4.dp))
            if (isSignedIn && userEmail.isNotBlank()) {
                Text(userEmail, style = MaterialTheme.typography.bodySmall, color = Color(0xFF5F6368),
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp))
            } else {
                Text("Connect", style = MaterialTheme.typography.labelSmall, color = ConcordiaMaroon)
            }
        }
    }
}

@Composable
private fun WeekNavigationRow(weekStartMs: Long, onPrev: () -> Unit, onNext: () -> Unit) {
    val fmt = SimpleDateFormat("d MMM", Locale.getDefault())
    val weekEndMs = weekStartMs + 6L * 24 * 60 * 60 * 1000
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = Color(0xFF5F6368), modifier = Modifier.size(20.dp))
        }
        Text("From ${fmt.format(Date(weekStartMs))} to ${fmt.format(Date(weekEndMs))}",
            style = MaterialTheme.typography.bodySmall, color = Color(0xFF5F6368), modifier = Modifier.weight(1f))
        IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color(0xFF5F6368), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun DayColumnHeaders(weekStartMs: Long) {
    val letters = listOf("M", "T", "W", "T", "F", "S", "S")
    val today   = Calendar.getInstance()
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.width(TimeColumnWidth))
        for (i in 0..6) {
            val cal = Calendar.getInstance().apply { timeInMillis = weekStartMs + i * 24L * 60 * 60 * 1000 }
            val isToday = cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) && cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
            Column(modifier = Modifier.weight(1f).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(letters[i], fontSize = 10.sp, color = if (isToday) TodayHighlight else Color(0xFF70757A), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(2.dp))
                Box(modifier = Modifier.size(26.dp).then(if (isToday) Modifier.background(TodayHighlight, RoundedCornerShape(50)) else Modifier),
                    contentAlignment = Alignment.Center) {
                    Text(cal.get(Calendar.DAY_OF_MONTH).toString(), fontSize = 13.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) Color.White else Color(0xFF3C4043))
                }
            }
        }
    }
}

@Composable
private fun TimeGrid(weekStartMs: Long, events: List<CalendarEvent>, onEventClick: (CalendarEvent) -> Unit) {
    val scrollState = rememberScrollState()
    val today = Calendar.getInstance()
    LaunchedEffect(Unit) { scrollState.scrollTo((8f * HourHeightDp.value * 3).toInt()) }

    Row(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        Column(modifier = Modifier.width(TimeColumnWidth)) {
            Spacer(Modifier.height(HourHeightDp / 2))
            for (hour in 0..23) {
                Box(modifier = Modifier.height(HourHeightDp).fillMaxWidth().padding(end = 6.dp), contentAlignment = Alignment.TopEnd) {
                    if (hour > 0) Text(String.format("%02d:00", hour), fontSize = 9.sp, color = Color(0xFF70757A))
                }
            }
        }
        Row(modifier = Modifier.weight(1f)) {
            for (dayIndex in 0..6) {
                val dayStartMs = weekStartMs + dayIndex * 24L * 60 * 60 * 1000
                val dayCal = Calendar.getInstance().apply { timeInMillis = dayStartMs }
                val isToday = dayCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) && dayCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                val dayEvents = events.filter { it.startTimeMs >= dayStartMs && it.startTimeMs < dayStartMs + 24L * 60 * 60 * 1000 }
                DayColumn(dayStartMs, dayEvents, isToday, onEventClick, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DayColumn(dayStartMs: Long, events: List<CalendarEvent>, isToday: Boolean, onEventClick: (CalendarEvent) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.then(if (isToday) Modifier.background(TodayColumnBg) else Modifier).border(width = 0.5.dp, color = GridLineColor)) {
        Column {
            Spacer(Modifier.height(HourHeightDp / 2))
            repeat(24) {
                Box(modifier = Modifier.height(HourHeightDp).fillMaxWidth().border(width = 0.5.dp, color = GridLineColor))
            }
        }
        events.forEachIndexed { idx, event ->
            EventBlock(event, dayStartMs, idx % EventColors.size, onEventClick)
        }
    }
}

@Composable
private fun BoxScope.EventBlock(event: CalendarEvent, dayStartMs: Long, colorIndex: Int, onEventClick: (CalendarEvent) -> Unit) {
    val midnight = Calendar.getInstance().apply {
        timeInMillis = dayStartMs
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val startMin    = ((event.startTimeMs - midnight) / 60_000f).coerceAtLeast(0f)
    val durationMin = ((event.endTimeMs - event.startTimeMs) / 60_000f).coerceAtLeast(30f)
    val topDp       = (startMin / 60f) * HourHeightDp.value
    val heightDp    = (durationMin / 60f) * HourHeightDp.value
    val color       = EventColors[colorIndex]
    val parsed      = parseLocation(event.location ?: "")

    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 1.5.dp).offset(y = topDp.dp)
            .height(heightDp.dp.coerceAtLeast(18.dp)).clip(RoundedCornerShape(3.dp))
            .background(color).clickable { onEventClick(event) }.padding(horizontal = 3.dp, vertical = 2.dp)
    ) {
        Column {
            Text(event.title, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold,
                maxLines = if (heightDp > 40) 2 else 1, overflow = TextOverflow.Ellipsis, lineHeight = 12.sp)
            if (heightDp > 30)
                Text(SimpleDateFormat("H:mm", Locale.getDefault()).format(Date(event.startTimeMs)),
                    fontSize = 9.sp, color = Color.White.copy(alpha = 0.85f), maxLines = 1)
            if (heightDp > 46 && parsed != null)
                Text(parsed.roomCode, fontSize = 9.sp, color = Color.White.copy(alpha = 0.80f), maxLines = 1)
        }
    }
}

@Composable
private fun NavigationConfirmDialog(event: CalendarEvent, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val parsed  = parseLocation(event.location ?: "")
    val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 8.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(event.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(4.dp))
                Text(timeFmt.format(Date(event.startTimeMs)), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = GridLineColor)
                Spacer(Modifier.height(16.dp))
                if (parsed != null) {
                    LocationInfoRow("Building", "${parsed.buildingCode} – ${parsed.buildingName}")
                    Spacer(Modifier.height(6.dp))
                    LocationInfoRow("Room",     parsed.roomCode)
                    Spacer(Modifier.height(6.dp))
                    LocationInfoRow("Campus",   parsed.campus)
                } else if (!event.location.isNullOrBlank()) {
                    LocationInfoRow("Location", event.location)
                } else {
                    Text("No location info", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
                    Button(onClick = onConfirm, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ConcordiaMaroon), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Navigate")
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationInfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.width(64.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
    }
}

// ── Location parser ───────────────────────────────────────────────────────────

data class ParsedLocation(val buildingCode: String, val buildingName: String, val roomCode: String, val campus: String)

private val buildingNames = mapOf(
    "H" to "Henry F. Hall", "MB" to "John Molson Building", "EV" to "Engineering & Visual Arts",
    "FG" to "Faubourg Building", "GM" to "Guy-De Maisonneuve", "GN" to "Grey Nuns",
    "AD" to "Administration", "CL" to "CL Building", "CC" to "Comm. Studies",
    "SP" to "Science Pavilion", "LB" to "McConnell Library", "HC" to "Hingston Hall",
    "RF" to "Recreation & Athletics", "PY" to "Psychology", "SC" to "Science College",
    "SB" to "Science Building", "LS" to "Learning Square", "VE" to "Visual Arts"
)

fun parseLocation(raw: String): ParsedLocation? {
    if (raw.isBlank()) return null
    // Short pattern: "MB S1.401 SGW" or "H 535 SGW"
    val m = Regex("""^([A-Z]{1,3})\s+([\w.]+)\s+(SGW|LOY|EV)$""").find(raw.trim())
    if (m != null) {
        val bCode = m.groupValues[1]
        return ParsedLocation(
            buildingCode = bCode,
            buildingName = buildingNames[bCode] ?: bCode,
            roomCode     = m.groupValues[2],
            campus       = if (m.groupValues[3] == "SGW") "Sir George Williams" else "Loyola"
        )
    }
    // Fallback for longer strings
    val campus = when {
        raw.contains("Loyola", ignoreCase = true) || raw.contains("LOY", ignoreCase = true) -> "Loyola"
        else -> "Sir George Williams"
    }
    val tokens = raw.split(" ", "-").map { it.trim() }.filter { it.isNotBlank() }
    val rmIdx  = tokens.indexOfFirst { it.equals("Rm", ignoreCase = true) }
    val room   = if (rmIdx >= 0 && rmIdx + 1 < tokens.size) tokens[rmIdx + 1] else ""
    val bCode  = tokens.firstOrNull { buildingNames.containsKey(it) } ?: ""
    return if (bCode.isNotBlank() || room.isNotBlank())
        ParsedLocation(bCode, buildingNames[bCode] ?: bCode, room, campus)
    else null
}
