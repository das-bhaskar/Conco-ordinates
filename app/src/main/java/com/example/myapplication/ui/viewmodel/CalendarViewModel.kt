package com.example.myapplication.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.CalendarEvent
import com.example.myapplication.logic.CalendarPreferences
import com.example.myapplication.logic.CalendarProvider
import com.example.myapplication.logic.currentWeekMonday
import com.example.myapplication.ui.models.CalendarState
import kotlinx.coroutines.launch

/**
 * Owns all Google Calendar state and operations.
 *
 * Extracted from [MapViewModel] to satisfy SRP — map/navigation concerns
 * and calendar concerns are now separate lifecycles.
 *
 * [calendarProvider] is injected so tests can pass a [MockCalendarProvider]
 * without touching production code.
 *
 * [calendarPreferences] is injected so tests can pass a [FakeCalendarPreferences]
 * and the ViewModel never holds a Context directly (satisfies DIP).
 *
 * On construction the ViewModel restores the previously selected calendar
 * and silently reloads events — the user never has to re-pick after restart.
 */
class CalendarViewModel(
    private val calendarProvider:   CalendarProvider,
    private val calendarPreferences: CalendarPreferences
) : ViewModel() {

    // ── Calendar picker state ─────────────────────────────────────────────────
    var calendarState by mutableStateOf<CalendarState>(CalendarState.Idle)
        private set

    // ── Selected calendar ─────────────────────────────────────────────────────
    var selectedCalendarId by mutableStateOf<String?>(null)
        private set

    var selectedCalendarName by mutableStateOf<String?>(null)
        private set

    // ── Week view state ───────────────────────────────────────────────────────
    var weekEvents by mutableStateOf<List<CalendarEvent>>(emptyList())
        private set

    var weekViewLoading by mutableStateOf(false)
        private set

    var currentWeekStartMs by mutableStateOf(currentWeekMonday())
        private set

    // ── Derived: next upcoming event with a location (for NextClassPill) ──────
    val nextUpcomingEvent: CalendarEvent?
        get() {
            val now = System.currentTimeMillis()
            return weekEvents
                .filter { it.startTimeMs >= now && !it.location.isNullOrBlank() }
                .minByOrNull { it.startTimeMs }
        }

    // ── Init: restore persisted selection ────────────────────────────────────

    init {
        restoreSelectionIfAvailable()
    }

    /**
     * If the user previously selected a calendar, restore it silently on
     * startup — no picker shown, events load in the background.
     */
    private fun restoreSelectionIfAvailable() {
        val id   = calendarPreferences.getSelectedCalendarId()   ?: return
        val name = calendarPreferences.getSelectedCalendarName() ?: return

        selectedCalendarId   = id
        selectedCalendarName = name

        viewModelScope.launch {
            calendarState = CalendarState.Loading
            loadWeekEvents(id)
            val event = calendarProvider.getNextEventWithLocation(id)
            calendarState = if (event != null) {
                CalendarState.NextClassReady(event, name)
            } else {
                CalendarState.NoUpcomingClass(name)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Calendar picker
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called after Google Sign-In succeeds.
     * Fetches all calendars and shows the picker so the user can choose
     * which one contains their courses.
     */
    fun loadCalendarsAndAutoSelect() {
        viewModelScope.launch {
            calendarState = CalendarState.Loading
            val calendars = calendarProvider.getCalendars()
            calendarState = if (calendars.isEmpty()) {
                CalendarState.Error("No calendars found. Make sure you are signed in.")
            } else {
                CalendarState.SelectingCalendar(calendars)
            }
        }
    }

    /**
     * Called when the user picks a calendar from the picker.
     * Persists the selection so it survives app restarts, then loads events.
     */
    fun onCalendarSelected(calendarId: String, calendarName: String) {
        selectedCalendarId   = calendarId
        selectedCalendarName = calendarName

        // Persist before the coroutine so it's saved even if the coroutine
        // is cancelled (e.g. user backgrounds the app mid-load).
        calendarPreferences.saveSelection(calendarId, calendarName)

        viewModelScope.launch {
            calendarState = CalendarState.Loading
            loadWeekEvents(calendarId)
            val event = calendarProvider.getNextEventWithLocation(calendarId)
            calendarState = if (event != null) {
                CalendarState.NextClassReady(event, calendarName)
            } else {
                CalendarState.NoUpcomingClass(calendarName)
            }
        }
    }

    /**
     * Re-fetches the next class for the already-selected calendar.
     * No-op if no calendar has been selected yet.
     */
    fun refreshNextClass() {
        val id   = selectedCalendarId   ?: return
        val name = selectedCalendarName ?: return
        onCalendarSelected(id, name)
    }

    /**
     * Clears the persisted calendar selection and resets state to Idle.
     * Call on sign-out.
     */
    fun clearSelection() {
        calendarPreferences.clearSelection()
        selectedCalendarId   = null
        selectedCalendarName = null
        weekEvents           = emptyList()
        calendarState        = CalendarState.Idle
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Week view
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Loads all events for the week starting at [weekStartMs].
     * Called when the Calendar tab opens or the user navigates weeks.
     */
    fun loadWeekEvents(calendarId: String, weekStartMs: Long = currentWeekStartMs) {
        currentWeekStartMs = weekStartMs
        viewModelScope.launch {
            weekViewLoading = true
            weekEvents      = calendarProvider.getWeekEvents(calendarId, weekStartMs)
            weekViewLoading = false
        }
    }

    fun goToPreviousWeek(calendarId: String) {
        loadWeekEvents(calendarId, currentWeekStartMs - 7L * 24 * 60 * 60 * 1000)
    }

    fun goToNextWeek(calendarId: String) {
        loadWeekEvents(calendarId, currentWeekStartMs + 7L * 24 * 60 * 60 * 1000)
    }
}
