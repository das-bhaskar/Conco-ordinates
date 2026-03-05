package com.example.myapplication.ui.models

import com.example.myapplication.data.CalendarEvent
import com.example.myapplication.logic.CalendarInfo

// ── Calendar-specific state ───────────────────────────────────────────────────

/**
 * Sealed hierarchy for the calendar loading lifecycle.
 *
 * Used by [MapViewModel] and rendered by [NextClassCard].
 * No Android dependencies — fully unit-testable.
 */
sealed class CalendarState {
    /** Initial state: user has not yet connected a calendar. */
    object Idle : CalendarState()

    /** Waiting for calendar list or events from the API. */
    object Loading : CalendarState()

    /** Calendar list loaded; user must choose which calendar to use. */
    data class SelectingCalendar(val calendars: List<CalendarInfo>) : CalendarState()

    /** Next class found and ready to display / navigate to. */
    data class NextClassReady(
        val event: CalendarEvent,
        val selectedCalendarName: String
    ) : CalendarState()

    /** No upcoming event with a location was found in the chosen calendar. */
    data class NoUpcomingClass(val selectedCalendarName: String) : CalendarState()

    /** Something went wrong (network, auth revoked, etc.). */
    data class Error(val message: String) : CalendarState()
}
