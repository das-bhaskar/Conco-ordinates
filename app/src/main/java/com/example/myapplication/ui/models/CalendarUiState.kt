package com.example.myapplication.ui.models

import com.example.myapplication.logic.CalendarInfo

// ── Calendar-specific state ───────────────────────────────────────────────────

/**
 * Sealed hierarchy for the calendar loading lifecycle.
 *
 * Only models the states the UI actually needs to render:
 * - Idle      → user has not connected a calendar yet
 * - Loading   → waiting for calendar list or events
 * - SelectingCalendar → user must pick which calendar to use
 * - Error     → something went wrong
 *
 * "Next class" data is NOT part of this hierarchy — it is exposed as
 * [CalendarViewModel.nextUpcomingEvent] (a derived property on the week
 * events list) so that calendar picker state and week view state remain
 * independent lifecycles. Mixing them into a single sealed class would
 * require the UI to unpack nested state on every recomposition.
 */
sealed class CalendarState {
    /** Initial state — user has not yet connected a calendar. */
    object Idle : CalendarState()

    /** Waiting for calendar list or events from the API. */
    object Loading : CalendarState()

    /**
     * Calendar list loaded; user must choose which calendar contains
     * their courses.
     */
    data class SelectingCalendar(val calendars: List<CalendarInfo>) : CalendarState()

    /** Something went wrong (network error, auth revoked, etc.). */
    data class Error(val message: String) : CalendarState()
}
