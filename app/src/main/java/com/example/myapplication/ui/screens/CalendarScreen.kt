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
import com.example.myapplication.data.ResolvedCalendarEvent
import com.example.myapplication.logic.CalendarInfo
import com.example.myapplication.ui.models.CalendarState
import com.example.myapplication.ui.theme.ConcordiaMaroon

/**
 * Bundles the user's account display state (PR review: reduce CalendarScreen surface area).
 */
data class UserAccountState(
    val isSignedIn: Boolean,
    val userEmail:  String
)

/**
 * Bundles all week/calendar interaction callbacks (PR review: reduce CalendarScreen surface area).
 *
 * Grouping them into a stable data class means adding a new action is a
 * single-file change and previews can be provided with a single stub object.
 */
data class CalendarActions(
    val onConnectClick:    () -> Unit,
    val onSignOutClick:    () -> Unit,
    val onCalendarPicked:  (id: String, name: String) -> Unit,
    val onPreviousWeek:    () -> Unit,
    val onNextWeek:        () -> Unit,
    val onNavigateToEvent: (destination: String) -> Unit
)

/**
 * Top-level Schedule tab screen.
 *
 * Fully stateless — receives [CalendarState], [UserAccountState], and
 * [CalendarActions] only. The ViewModel is never referenced here.
 *
 * Grouping related parameters into data classes (PR review) reduces the
 * public surface area and makes Android Studio Previews trivial to stub.
 */
@Composable
fun CalendarScreen(
    calendarState:  CalendarState,
    weekStartMs:    Long,
    weekEvents:     List<ResolvedCalendarEvent>,
    isLoading:      Boolean,
    accountState:   UserAccountState,
    calendarActions: CalendarActions,
    modifier:       Modifier = Modifier
) {
    // Calendar picker takes over the whole screen while user selects
    if (!accountState.isSignedIn && calendarState is CalendarState.SelectingCalendar) {
        CalendarPickerScreen(
            calendars        = calendarState.calendars,
            onCalendarPicked = calendarActions.onCalendarPicked
        )
        return
    }

    WeekCalendarView(
        weekStartMs    = weekStartMs,
        events         = weekEvents,
        isLoading      = isLoading || calendarState is CalendarState.Loading,
        accountState   = CalendarAccountState(
            isSignedIn     = accountState.isSignedIn,
            userEmail      = accountState.userEmail,
            onConnectClick = calendarActions.onConnectClick,
            onSignOutClick = calendarActions.onSignOutClick
        ),
        onPreviousWeek    = calendarActions.onPreviousWeek,
        onNextWeek        = calendarActions.onNextWeek,
        // ResolvedCalendarEvent.destinationBuildingCode encapsulates the
        // (locationResult as? Known)?.buildingCode fallback — no logic in UI (PR review).
        onNavigateToEvent = { event ->
            event.destinationBuildingCode?.let { calendarActions.onNavigateToEvent(it) }
        },
        modifier          = modifier
    )
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
            // key={it.id} lets Compose track items on list changes (PR review)
            items(calendars, key = { it.id }) { calendar ->
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
