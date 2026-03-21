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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
    private val locationResolver:    LocationResolver,   // required — inject at call-site
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {


    private fun launchOnMain(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {
        viewModelScope.launch(dispatcher) { block() }
    }
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
    }
    // ── Derived: next upcoming event with a location (for NextClassPill) ──────
    // Recalculated on every ticker tick so the pill expires naturally as time passes.
    // ── Derived: next upcoming event with a location (for NextClassPill) ──────
// We use mutableStateOf for these so the UI actually "sees" the change
// when the ticker ticks.
    var nextUpcomingEvent by mutableStateOf<ResolvedCalendarEvent?>(null)
        private set

    var isNextClassUrgent by mutableStateOf(false)
        private set

    var nextClassTimeRemaining by mutableStateOf("")
        private set

    // ── Update Logic ──
    private fun refreshPillState(now: Long) {
        val event = weekEvents
            .filter { it.startTimeMs >= now && !it.location.isNullOrBlank() }
            .minByOrNull { it.startTimeMs }

        nextUpcomingEvent = event

        if (event != null) {
            val minutesUntil = ((event.startTimeMs - now) / 60_000).coerceAtLeast(0)

            isNextClassUrgent = minutesUntil in 0..URGENT_THRESHOLD_MINUTES

            nextClassTimeRemaining = when {
                minutesUntil == 0L -> "Now"
                minutesUntil < 60  -> "in ${minutesUntil}m"
                else               -> "in ${minutesUntil / 60}h ${minutesUntil % 60}m"
            }
        } else {
            nextClassTimeRemaining = ""
            isNextClassUrgent = false
            tickerJob?.cancel()
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
        val id = calendarPreferences.getSelectedCalendarId() ?: return
        val name = calendarPreferences.getSelectedCalendarName() ?: return
        selectedCalendarId = id
        selectedCalendarName = name
        launchOnMain {
            calendarState = CalendarState.Loading
            loadWeekEvents(id)
            calendarState = CalendarState.Idle
        }
    }
    private var tickerJob: kotlinx.coroutines.Job? = null

    private fun startTicker() {
        tickerJob?.cancel()

        // 1. If no events are loaded, don't even start the loop.
        // This fixes most of your test timeouts immediately.
        if (weekEvents.isEmpty() || nextUpcomingEvent == null) return

        tickerJob = viewModelScope.launch(dispatcher) {
            tickerFlow.collect { now ->
                refreshPillState(now)

                // 2. If the last class of the day is over, kill the ticker.
                if (nextUpcomingEvent == null) {
                    tickerJob?.cancel()
                }
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
        viewModelScope.launch(dispatcher){
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
        viewModelScope.launch(dispatcher){
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
    private var loadJob: kotlinx.coroutines.Job? = null
    fun loadWeekEvents(calendarId: String, weekStartMs: Long = currentWeekStartMs) {
        currentWeekStartMs = weekStartMs
        loadJob?.cancel()
        loadJob=viewModelScope.launch(dispatcher){
            weekViewLoading = true
            weekEvents = calendarProvider.getWeekEvents(calendarId, weekStartMs)
                .map { event ->
                    ResolvedCalendarEvent(
                        event          = event,
                        locationResult = locationResolver.resolve(event.location)
                    )
                }
            weekViewLoading = false
            startTicker()
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
    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
        tickerJob?.cancel()
    }
}
