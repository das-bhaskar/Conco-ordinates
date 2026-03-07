package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.LocationResult
import com.example.myapplication.data.ResolvedCalendarEvent
import com.example.myapplication.logic.CalendarInfo
import com.example.myapplication.ui.models.CalendarState
import com.example.myapplication.ui.theme.ConcordiaMaroon

/**
 * Top-level Schedule tab screen.
 *
 * Fully stateless — receives primitive state and lambda callbacks only.
 * The ViewModel is never referenced here, satisfying the principle that
 * composables should not depend on ViewModel types directly.
 *
 * Call-site (AppNavigation) reads from the ViewModel and passes values down:
 * ```
 * CalendarScreen(
 *     calendarState      = calendarViewModel.calendarState,
 *     weekStartMs        = calendarViewModel.currentWeekStartMs,
 *     weekEvents         = calendarViewModel.weekEvents,
 *     isLoading          = calendarViewModel.weekViewLoading,
 *     isSignedIn         = calendarViewModel.selectedCalendarId != null,
 *     userEmail          = ...,
 *     onConnectClick     = ...,
 *     onSignOutClick     = ...,
 *     onCalendarPicked   = calendarViewModel::onCalendarSelected,
 *     onPreviousWeek     = { calendarViewModel.goToPreviousWeek(id) },
 *     onNextWeek         = { calendarViewModel.goToNextWeek(id) },
 *     onNavigateToEvent  = { dest -> ... }
 * )
 * ```
 */
@Composable
fun CalendarScreen(
    calendarState:     CalendarState,
    weekStartMs:       Long,
    weekEvents:        List<ResolvedCalendarEvent>,
    isLoading:         Boolean,
    isSignedIn:        Boolean,
    userEmail:         String,
    onConnectClick:    () -> Unit,
    onSignOutClick:    () -> Unit,
    onCalendarPicked:  (id: String, name: String) -> Unit,
    onPreviousWeek:    () -> Unit,
    onNextWeek:        () -> Unit,
    onNavigateToEvent: (destination: String) -> Unit,
    modifier:          Modifier = Modifier
) {
    // Calendar picker takes over the whole screen while user selects
    if (!isSignedIn && calendarState is CalendarState.SelectingCalendar) {
        CalendarPickerScreen(
            calendars        = calendarState.calendars,
            onCalendarPicked = onCalendarPicked
        )
        return
    }

    WeekCalendarView(
        weekStartMs    = weekStartMs,
        events         = weekEvents,
        isLoading      = isLoading || calendarState is CalendarState.Loading,
        accountState   = CalendarAccountState(
            isSignedIn     = isSignedIn,
            userEmail      = userEmail,
            onConnectClick = onConnectClick,
            onSignOutClick = onSignOutClick
        ),
        onPreviousWeek    = onPreviousWeek,
        onNextWeek        = onNextWeek,
        onNavigateToEvent = { event -> dispatchNavigation(event, onNavigateToEvent) },
        modifier          = modifier
    )
}

/**
 * Resolves the navigation destination from a [ResolvedCalendarEvent].
 *
 * Prefers the building code from an already-resolved [LocationResult.Known];
 * falls back to the raw location string for unrecognised rooms so navigation
 * can still attempt a search. Silently no-ops if no destination can be derived.
 */
private fun dispatchNavigation(
    event:             ResolvedCalendarEvent,
    onNavigateToEvent: (String) -> Unit
) {
    val destination = when (val loc = event.locationResult) {
        is LocationResult.Known -> loc.location.buildingCode
        else                    -> event.location?.takeIf { it.isNotBlank() }
    } ?: return  // Online / TBA / Unknown — no navigation destination

    onNavigateToEvent(destination)
}

// ── Calendar picker ───────────────────────────────────────────────────────────

/**
 * Shows the user's Google Calendars so they can pick the one that
 * contains their courses.
 */
@Composable
fun CalendarPickerScreen(
    calendars:       List<CalendarInfo>,
    onCalendarPicked: (id: String, name: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint     = ConcordiaMaroon,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "Choose a Calendar",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    "Select the calendar with your courses",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        HorizontalDivider(color = Color(0xFFEEEEEE))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(calendars) { calendar ->
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clickable { onCalendarPicked(calendar.id, calendar.summary) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                ConcordiaMaroon.copy(alpha = 0.10f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint     = ConcordiaMaroon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            calendar.summary,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                        if (calendar.id != calendar.summary) {
                            Text(
                                calendar.id,
                                style    = MaterialTheme.typography.bodySmall,
                                color    = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint     = Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                HorizontalDivider(
                    color    = Color(0xFFEEEEEE),
                    modifier = Modifier.padding(start = 74.dp)
                )
            }
        }
    }
}
