package com.example.myapplication.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.ResolvedCalendarEvent
import com.example.myapplication.logic.LocationResolver
import com.example.myapplication.logic.CalendarPreferences
import com.example.myapplication.logic.CalendarInfo
import com.example.myapplication.logic.CalendarProvider
import com.example.myapplication.logic.currentWeekMonday
import com.example.myapplication.ui.models.CalendarState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

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
 *
 * Week navigation uses [ZonedDateTime] instead of raw millisecond arithmetic
 * to correctly handle DST transitions and leap seconds.
 */
class CalendarViewModel(
    private val calendarProvider:    CalendarProvider,
    private val calendarPreferences: CalendarPreferences,
    private val locationResolver:    LocationResolver   // required — inject at call-site
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
    var weekEvents by mutableStateOf<List<ResolvedCalendarEvent>>(emptyList())
        private set

    var weekViewLoading by mutableStateOf(false)
        private set

    var currentWeekStartMs by mutableStateOf(currentWeekMonday())
        private set

    // ── Ticker: emits current time every 60s so NextClassPill stays accurate ──
    // Without this, nextUpcomingEvent only re-derives when weekEvents changes —
    // meaning a class that starts while the user is looking at the map would
    // never expire from the pill until the next data refresh (PR review).
    private val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000L)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), System.currentTimeMillis())

    // ── Derived: next upcoming event with a location (for NextClassPill) ──────
    // Recalculated on every ticker tick so the pill expires naturally as time passes.
    val nextUpcomingEvent: ResolvedCalendarEvent?
        get() {
            val now = tickerFlow.value
            return weekEvents
                .filter { it.startTimeMs >= now && !it.location.isNullOrBlank() }
                .minByOrNull { it.startTimeMs }
        }

    /**
     * Business rule: an upcoming class is "urgent" when it starts within
     * [URGENT_THRESHOLD_MINUTES]. Defined here — not in the UI — so the
     * threshold can change in one place without touching any composable.
     */
    val isNextClassUrgent: Boolean
        get() {
            val event = nextUpcomingEvent ?: return false
            val minutesUntil = (event.startTimeMs - tickerFlow.value) / 60_000
            return minutesUntil in 0..URGENT_THRESHOLD_MINUTES
        }

    /**
     * Pre-formatted time-remaining string for NextClassPill.
     * Computed here so the UI receives a ready-to-display string (PR review:
     * minutesUntil / timeLabel logic removed from NextClassPill composable).
     */
    val nextClassTimeRemaining: String
        get() {
            val event = nextUpcomingEvent ?: return ""
            val minutesUntil = ((event.startTimeMs - tickerFlow.value) / 60_000).coerceAtLeast(0)
            return when {
                minutesUntil == 0L -> "Now"
                minutesUntil < 60  -> "in ${minutesUntil}m"
                else               -> "in ${minutesUntil / 60}h ${minutesUntil % 60}m"
            }
        }

    companion object {
        const val URGENT_THRESHOLD_MINUTES = 15L
    }

    // ── Init: restore persisted selection ────────────────────────────────────
    init {
        restoreSelectionIfAvailable()
    }

    /**
     * If the user previously selected a calendar, restore it silently on
     * startup — no picker shown, events load in the background.
     *
     * This replaces the LaunchedEffect(selectedCalendarId) that previously
     * lived in MapsActivity — the ViewModel handles its own side-effects.
     */
    private fun restoreSelectionIfAvailable() {
        val id   = calendarPreferences.getSelectedCalendarId()   ?: return
        val name = calendarPreferences.getSelectedCalendarName() ?: return
        selectedCalendarId   = id
        selectedCalendarName = name
        viewModelScope.launch {
            calendarState = CalendarState.Loading
            loadWeekEvents(id)
            calendarState = CalendarState.Idle
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
        calendarPreferences.saveSelection(CalendarInfo(id = calendarId, summary = calendarName))
        viewModelScope.launch {
            calendarState = CalendarState.Loading
            loadWeekEvents(calendarId)
            calendarState = CalendarState.Idle
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
     * Called on init restore, calendar selection, or week navigation.
     */
    fun loadWeekEvents(calendarId: String, weekStartMs: Long = currentWeekStartMs) {
        currentWeekStartMs = weekStartMs
        viewModelScope.launch {
            weekViewLoading = true
            // Resolve location for every event in the ViewModel layer —
            // UI receives pre-resolved data and never calls parsing logic.
            weekEvents = calendarProvider.getWeekEvents(calendarId, weekStartMs)
                .map { event ->
                    ResolvedCalendarEvent(
                        event          = event,
                        locationResult = locationResolver.resolve(event.location)
                    )
                }
            weekViewLoading = false
        }
    }

    /**
     * Navigate to the previous week using [ZonedDateTime] so DST transitions
     * and leap seconds are handled correctly — never use raw millisecond math
     * for calendar navigation.
     */
    fun goToPreviousWeek(calendarId: String) {
        val newStart = ZonedDateTime
            .ofInstant(Instant.ofEpochMilli(currentWeekStartMs), ZoneId.systemDefault())
            .minusWeeks(1)
            .toInstant()
            .toEpochMilli()
        loadWeekEvents(calendarId, newStart)
    }

    /**
     * Navigate to the next week using [ZonedDateTime] — same reasoning as
     * [goToPreviousWeek].
     */
    fun goToNextWeek(calendarId: String) {
        val newStart = ZonedDateTime
            .ofInstant(Instant.ofEpochMilli(currentWeekStartMs), ZoneId.systemDefault())
            .plusWeeks(1)
            .toInstant()
            .toEpochMilli()
        loadWeekEvents(calendarId, newStart)
    }
    fun setAuthError(message: String) {
        calendarState = CalendarState.Error(message)
    }

    // Add this so the UI can dismiss the error popup
    fun dismissError() {
        calendarState = CalendarState.Idle
    }
}
