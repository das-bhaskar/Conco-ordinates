package com.example.myapplication.ui.components

import com.example.myapplication.logic.DateUtils
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
import androidx.compose.material.icons.filled.Logout
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
import com.example.myapplication.data.LocationResult
import com.example.myapplication.data.ResolvedCalendarEvent
import java.text.SimpleDateFormat
import java.util.*

// ── Constants ─────────────────────────────────────────────────────────────────
private val ConcordiaMaroon = Color(0xFF912338)
private val TodayHighlight  = Color(0xFF912338)
private val TodayColumnBg   = Color(0xFFFFEEF0)
private val GridLineColor   = Color(0xFFE8E8E8)
private val EventColors     = listOf(
    Color(0xFF1A73E8),
    Color(0xFF0F9D58),
    Color(0xFFE37400),
    Color(0xFF7B1FA2)
)
private val TimeColumnWidth = 48.dp
private val HourHeightDp    = 60.dp
private val HeaderHeightDp  = 52.dp

/**
 * Groups account-related parameters for [WeekCalendarView] so the function
 * stays within the 7-parameter limit required by static analysis.
 */
data class CalendarAccountState(
    val isSignedIn:     Boolean    = false,
    val userEmail:      String     = "",
    val onConnectClick: () -> Unit = {},
    val onSignOutClick: () -> Unit = {}
)

/**
 * Week calendar view — stateless, all data flows in as parameters.
 *
 * Receives [ResolvedCalendarEvent] so this composable never calls any
 * parsing logic. Location resolution is the ViewModel's responsibility.
 */
@Composable
fun WeekCalendarView(
    weekStartMs:       Long,
    events:            List<ResolvedCalendarEvent>,
    eventsByDay:       Map<Int, List<ResolvedCalendarEvent>> = emptyMap(), // pre-grouped by ViewModel
    isLoading:         Boolean,
    accountState:      CalendarAccountState = CalendarAccountState(),
    onPreviousWeek:    () -> Unit,
    onNextWeek:        () -> Unit,
    onNavigateToEvent: (ResolvedCalendarEvent) -> Unit,
    modifier:          Modifier = Modifier
) {
    // pendingEvent drives the confirmation dialog.
    // Use a single state variable; clear it on ALL exit paths (Confirm/Dismiss/Cancel)
    // to prevent ghost dialogs and the SonarCloud "assigned but never used" warning.
    var pendingEvent by remember { mutableStateOf<ResolvedCalendarEvent?>(null) }

    Column(modifier = modifier.fillMaxSize().background(Color.White)) {
        CalendarTopBar(
            isSignedIn     = accountState.isSignedIn,
            userEmail      = accountState.userEmail,
            onConnectClick = accountState.onConnectClick,
            onSignOutClick = accountState.onSignOutClick
        )
        WeekNavigationRow(weekStartMs, onPreviousWeek, onNextWeek)
        DayColumnHeaders(weekStartMs)
        HorizontalDivider(color = GridLineColor, thickness = 0.5.dp)
        when {
            isLoading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color    = ConcordiaMaroon,
                    modifier = Modifier.size(32.dp)
                )
            }
            !accountState.isSignedIn -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.padding(32.dp)
                ) {
                    Text(
                        "Connect Google Calendar",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap the account icon above to sign in",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = accountState.onConnectClick,
                        colors  = ButtonDefaults.buttonColors(containerColor = ConcordiaMaroon),
                        shape   = RoundedCornerShape(12.dp)
                    ) { Text("Connect") }
                }
            }
            else -> TimeGrid(weekStartMs, events, eventsByDay) { clicked -> pendingEvent = clicked }
        }
    }

    // Dialog is rendered outside the Column so it overlays the full screen.
    // pendingEvent is cleared on every exit path: Confirm, Dismiss, and Cancel.
    val eventToConfirm = pendingEvent
    if (eventToConfirm != null) {
        NavigationConfirmDialog(
            event     = eventToConfirm,
            onConfirm = {
                pendingEvent = null          // clear first to avoid ghost dialog
                onNavigateToEvent(eventToConfirm)
            },
            onDismiss = { pendingEvent = null }
        )
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────
@Composable
private fun CalendarTopBar(
    isSignedIn:     Boolean,
    userEmail:      String,
    onConnectClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth().height(HeaderHeightDp).padding(horizontal = 16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Schedule",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF3C4043)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onConnectClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Account",
                    tint               = if (isSignedIn) ConcordiaMaroon else Color.Gray,
                    modifier           = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(4.dp))
                if (isSignedIn && userEmail.isNotBlank()) {
                    Text(
                        userEmail,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = Color(0xFF5F6368),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 160.dp)
                    )
                } else {
                    Text("Connect", style = MaterialTheme.typography.labelSmall, color = ConcordiaMaroon)
                }
            }
            if (isSignedIn) {
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onSignOutClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector        = Icons.Default.Logout,
                        contentDescription = "Sign out",
                        tint               = Color(0xFF5F6368),
                        modifier           = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ── Week navigation row ───────────────────────────────────────────────────────
@Composable
private fun WeekNavigationRow(weekStartMs: Long, onPrev: () -> Unit, onNext: () -> Unit) {
    val fmt       = DateUtils.dayMonthFormatter()
    // DST-safe: Instant.plus(6, DAYS) instead of raw ms arithmetic (PR review)
    val weekEndMs = java.time.Instant.ofEpochMilli(weekStartMs)
        .plus(6, java.time.temporal.ChronoUnit.DAYS).toEpochMilli()
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Prev",
                tint = Color(0xFF5F6368), modifier = Modifier.size(20.dp))
        }
        Text(
            "From ${fmt.format(Date(weekStartMs))} to ${fmt.format(Date(weekEndMs))}",
            style    = MaterialTheme.typography.bodySmall,
            color    = Color(0xFF5F6368),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next",
                tint = Color(0xFF5F6368), modifier = Modifier.size(20.dp))
        }
    }
}

// ── Day column headers ────────────────────────────────────────────────────────
private val DAY_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")

private fun calendarForDay(weekStartMs: Long, offset: Int): Calendar =
    Calendar.getInstance().apply {
        // DST-safe day offset via Instant (PR review)
        timeInMillis = java.time.Instant.ofEpochMilli(weekStartMs)
            .plus(offset.toLong(), java.time.temporal.ChronoUnit.DAYS).toEpochMilli()
    }

private fun isToday(cal: Calendar): Boolean {
    val today = Calendar.getInstance()
    return cal[Calendar.DAY_OF_YEAR] == today[Calendar.DAY_OF_YEAR] &&
           cal[Calendar.YEAR]        == today[Calendar.YEAR]
}

@Composable
private fun DayColumnHeaders(weekStartMs: Long) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.width(TimeColumnWidth))
        for (i in 0..6) {
            DayHeaderCell(
                letter    = DAY_LETTERS[i],
                dayNumber = calendarForDay(weekStartMs, i)[Calendar.DAY_OF_MONTH],
                isToday   = isToday(calendarForDay(weekStartMs, i))
            )
        }
    }
}

@Composable
private fun RowScope.DayHeaderCell(letter: String, dayNumber: Int, isToday: Boolean) {
    Column(
        modifier            = Modifier.weight(1f).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            letter,
            fontSize   = 10.sp,
            color      = if (isToday) TodayHighlight else Color(0xFF70757A),
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(2.dp))
        Box(
            modifier         = Modifier.size(26.dp).then(
                if (isToday) Modifier.background(TodayHighlight, RoundedCornerShape(50)) else Modifier
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                dayNumber.toString(),
                fontSize   = 13.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color      = if (isToday) Color.White else Color(0xFF3C4043)
            )
        }
    }
}

// ── Scrollable time grid ──────────────────────────────────────────────────────
@Composable
private fun TimeGrid(
    weekStartMs:  Long,
    events:       List<ResolvedCalendarEvent>,
    eventsByDay:  Map<Int, List<ResolvedCalendarEvent>>,  // pre-grouped by ViewModel
    onEventClick: (ResolvedCalendarEvent) -> Unit
) {
    val scrollState = rememberScrollState()
    val today       = Calendar.getInstance()

    // Scroll to current hour on first composition so the user always sees
    // their current time — not an arbitrary magic number.
    val currentHour = today[Calendar.HOUR_OF_DAY]
    LaunchedEffect(Unit) {
        val scrollOffset = ((currentHour - 1).coerceAtLeast(0) * HourHeightDp.value).toInt()
        scrollState.scrollTo(scrollOffset)
    }

    Row(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        Column(modifier = Modifier.width(TimeColumnWidth)) {
            Spacer(Modifier.height(HourHeightDp / 2))
            for (hour in 0..23) {
                Box(
                    modifier         = Modifier.height(HourHeightDp).fillMaxWidth().padding(end = 6.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    if (hour > 0) Text(
                        String.format("%02d:00", hour),
                        fontSize = 9.sp,
                        color    = Color(0xFF70757A)
                    )
                }
            }
        }
        Row(modifier = Modifier.weight(1f)) {
            for (dayIndex in 0..6) {
                // DST-safe day boundary via Instant (PR review)
                val dayStartMs = java.time.Instant.ofEpochMilli(weekStartMs)
                    .plus(dayIndex.toLong(), java.time.temporal.ChronoUnit.DAYS).toEpochMilli()
                val dayCal     = Calendar.getInstance().apply { timeInMillis = dayStartMs }
                val isTodayCol = dayCal[Calendar.DAY_OF_YEAR] == today[Calendar.DAY_OF_YEAR] &&
                                 dayCal[Calendar.YEAR]        == today[Calendar.YEAR]
                // eventsByDay pre-grouped by ViewModel — O(1) lookup instead of O(n×7).
                // Fall back to filtering from events list when eventsByDay not provided.
                val dayEndMs  = java.time.Instant.ofEpochMilli(dayStartMs)
                    .plus(1, java.time.temporal.ChronoUnit.DAYS).toEpochMilli()
                val dayEvents = if (eventsByDay.isNotEmpty()) {
                    eventsByDay[dayIndex] ?: emptyList()
                } else {
                    events.filter { it.startTimeMs >= dayStartMs && it.startTimeMs < dayEndMs }
                }
                DayColumn(dayStartMs, dayEvents, isTodayCol, onEventClick, Modifier.weight(1f))
            }
        }
    }
}

// ── Single day column ─────────────────────────────────────────────────────────
@Composable
private fun DayColumn(
    dayStartMs:   Long,
    events:       List<ResolvedCalendarEvent>,
    isToday:      Boolean,
    onEventClick: (ResolvedCalendarEvent) -> Unit,
    modifier:     Modifier = Modifier
) {
    Box(
        modifier = modifier
            .then(if (isToday) Modifier.background(TodayColumnBg) else Modifier)
            .border(width = 0.5.dp, color = GridLineColor)
    ) {
        Column {
            Spacer(Modifier.height(HourHeightDp / 2))
            repeat(24) {
                Box(modifier = Modifier.height(HourHeightDp).fillMaxWidth()
                    .border(width = 0.5.dp, color = GridLineColor))
            }
        }
        events.forEachIndexed { idx, event ->
            EventBlock(event, dayStartMs, idx % EventColors.size, onEventClick)
        }
    }
}

// ── Event block ───────────────────────────────────────────────────────────────
@Composable
private fun BoxScope.EventBlock(
    event:        ResolvedCalendarEvent,
    dayStartMs:   Long,
    colorIndex:   Int,
    onEventClick: (ResolvedCalendarEvent) -> Unit
) {
    val midnight = Calendar.getInstance().apply {
        timeInMillis = dayStartMs
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val startMin    = ((event.startTimeMs - midnight) / 60_000f).coerceAtLeast(0f)
    val durationMin = ((event.endTimeMs   - event.startTimeMs) / 60_000f).coerceAtLeast(30f)
    val topDp       = (startMin    / 60f) * HourHeightDp.value
    val heightDp    = (durationMin / 60f) * HourHeightDp.value
    val color       = EventColors[colorIndex]

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 1.5.dp)
            .offset(y = topDp.dp)
            .height(heightDp.dp.coerceAtLeast(18.dp))
            .clip(RoundedCornerShape(3.dp))
            .background(color)
            .clickable { onEventClick(event) }
            .padding(horizontal = 3.dp, vertical = 2.dp)
    ) {
        Column {
            Text(
                event.title,
                fontSize   = 10.sp,
                color      = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines   = if (heightDp > 40) 2 else 1,
                overflow   = TextOverflow.Ellipsis,
                lineHeight = 12.sp
            )
            if (heightDp > 30)
                Text(
                    DateUtils.eventTimeFormatter().format(java.util.Date(event.startTimeMs)),
                    fontSize = 9.sp,
                    color    = Color.White.copy(alpha = 0.85f),
                    maxLines = 1
                )
            if (heightDp > 46) {
                // locationResult was pre-resolved by ViewModel — no parsing here
                when (val loc = event.locationResult) {
                    is LocationResult.Known ->
                        Text(loc.location.roomCode, fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.80f), maxLines = 1)
                    LocationResult.Online ->
                        Text("🌐 Online", fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.80f), maxLines = 1)
                    LocationResult.TBA ->
                        Text("📍 TBA", fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.80f), maxLines = 1)
                    LocationResult.Unknown -> { /* no location to show */ }
                }
            }
        }
    }
}

// ── Navigation confirmation dialog ────────────────────────────────────────────
@Composable
private fun NavigationConfirmDialog(
    event:     ResolvedCalendarEvent,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 8.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    timeFmt.format(Date(event.startTimeMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = GridLineColor)
                Spacer(Modifier.height(16.dp))
                // locationResult pre-resolved — UI only switches on result type
                when (val loc = event.locationResult) {
                    is LocationResult.Known -> {
                        LocationInfoRow("Building", "${loc.location.buildingCode} – ${loc.location.buildingName}")
                        Spacer(Modifier.height(6.dp))
                        LocationInfoRow("Room",     loc.location.roomCode)
                        Spacer(Modifier.height(6.dp))
                        LocationInfoRow("Campus",   loc.location.campus)
                    }
                    LocationResult.Online -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🌐", fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("This class is online",
                                style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }
                    LocationResult.TBA -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📍", fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("Location not yet announced",
                                style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }
                    LocationResult.Unknown ->
                        Text("No location info",
                            style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier             = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp)
                    ) { Text("Cancel") }
                    if (event.locationResult is LocationResult.Known) {
                        Button(
                            onClick  = onConfirm,
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.buttonColors(containerColor = ConcordiaMaroon),
                            shape    = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Directions, contentDescription = null,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Navigate")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationInfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style    = MaterialTheme.typography.labelSmall,
            color    = Color.Gray,
            modifier = Modifier.width(64.dp)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
    }
}
