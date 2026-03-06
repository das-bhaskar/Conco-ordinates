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
import com.example.myapplication.data.parseLocation
import com.example.myapplication.logic.CalendarInfo
import com.example.myapplication.ui.components.CalendarAccountState
import com.example.myapplication.ui.components.WeekCalendarView
import com.example.myapplication.ui.models.CalendarState
import com.example.myapplication.ui.viewmodel.CalendarViewModel
import com.example.myapplication.ui.theme.ConcordiaMaroon

/**
 * Top-level Schedule tab screen.
 *
 * Delegates to [CalendarPickerScreen] while the user is choosing a calendar,
 * and to [WeekCalendarView] once a calendar is selected.
 *
 * All ViewModel interaction is done via callbacks so this composable
 * remains independently testable.
 */
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    selectedCalendarId: String?,
    userEmail: String,
    onConnectClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onNavigateToEvent: (String) -> Unit
) {
    val isSignedIn = selectedCalendarId != null
    val calState   = viewModel.calendarState

    if (!isSignedIn && calState is CalendarState.SelectingCalendar) {
        CalendarPickerScreen(
            calendars        = calState.calendars,
            onCalendarPicked = { id, name -> viewModel.onCalendarSelected(id, name) }
        )
        return
    }

    WeekCalendarView(
        weekStartMs       = viewModel.currentWeekStartMs,
        events            = viewModel.weekEvents,
        isLoading         = viewModel.weekViewLoading || calState is CalendarState.Loading,
        accountState      = CalendarAccountState(
            isSignedIn     = isSignedIn,
            userEmail      = userEmail,
            onConnectClick = onConnectClick,
            onSignOutClick = onSignOutClick
        ),
        onPreviousWeek    = { selectedCalendarId?.let { viewModel.goToPreviousWeek(it) } },
        onNextWeek        = { selectedCalendarId?.let { viewModel.goToNextWeek(it) } },
        onNavigateToEvent = { event ->
            val destination = (event.locationResult as? LocationResult.Known)
                ?.location?.buildingCode
                ?: event.location?.takeIf { it.isNotBlank() }
                ?: return@WeekCalendarView
            onNavigateToEvent(destination)
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Shows the list of the user's Google Calendars so they can pick the one
 * that contains their courses.
 */
@Composable
fun CalendarPickerScreen(
    calendars: List<CalendarInfo>,
    onCalendarPicked: (id: String, name: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
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

        // ── Calendar list ─────────────────────────────────────────────────────
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(calendars) { calendar ->
                Row(
                    modifier = Modifier
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
