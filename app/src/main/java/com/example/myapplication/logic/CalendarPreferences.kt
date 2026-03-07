package com.example.myapplication.logic

import android.content.Context
import android.content.SharedPreferences

/**
 * Abstracts persistence of the user's calendar selection.
 *
 * Lives in the logic layer — no Android UI imports, no ViewModel knowledge.
 *
 * Keeping this behind an interface satisfies DIP and OCP:
 * - [CalendarViewModel] depends on the interface, not SharedPreferences directly
 * - Swapping to DataStore / Room later only requires a new implementation
 * - Tests can inject [FakeCalendarPreferences] without any Android context
 *
 * [CalendarInfo] is passed as a single object rather than separate (id, name)
 * parameters so the signature stays cohesive: if CalendarInfo gains fields
 * (e.g. colour, timezone) we extend the data class, not every call-site.
 */
interface CalendarPreferences {
    /** Returns the persisted calendar ID, or null if none has been saved. */
    fun getSelectedCalendarId(): String?

    /** Returns the persisted calendar display name, or null if none has been saved. */
    fun getSelectedCalendarName(): String?

    /** True once the user has completed the calendar picker at least once. */
    val hasSelection: Boolean get() = getSelectedCalendarId() != null

    /**
     * Persists the user's calendar selection.
     * Accepts a [CalendarInfo] so callers pass one cohesive object rather than
     * two unrelated strings that could be accidentally swapped.
     */
    fun saveSelection(calendar: CalendarInfo)

    /** Clears any persisted selection (e.g. on sign-out). */
    fun clearSelection()
}

// ── Production implementation ─────────────────────────────────────────────────

/**
 * SharedPreferences-backed implementation.
 *
 * Uses MODE_PRIVATE — data is scoped to this app and never shared with
 * other apps.
 */
class SharedPrefsCalendarPreferences(context: Context) : CalendarPreferences {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_FILE, Context.MODE_PRIVATE
    )

    override fun getSelectedCalendarId(): String? =
        prefs.getString(KEY_CALENDAR_ID, null)

    override fun getSelectedCalendarName(): String? =
        prefs.getString(KEY_CALENDAR_NAME, null)

    override fun saveSelection(calendar: CalendarInfo) {
        prefs.edit()
            .putString(KEY_CALENDAR_ID,   calendar.id)
            .putString(KEY_CALENDAR_NAME, calendar.summary)
            .apply()   // async write — safe for UI-triggered saves
    }

    override fun clearSelection() {
        prefs.edit()
            .remove(KEY_CALENDAR_ID)
            .remove(KEY_CALENDAR_NAME)
            .apply()
    }

    companion object {
        private const val PREFS_FILE        = "calendar_preferences"
        private const val KEY_CALENDAR_ID   = "selected_calendar_id"
        private const val KEY_CALENDAR_NAME = "selected_calendar_name"
    }
}

// ── Test double ───────────────────────────────────────────────────────────────

/**
 * In-memory implementation for unit tests.
 * No Android context required — drop into any test with zero setup.
 */
class FakeCalendarPreferences : CalendarPreferences {
    private var saved: CalendarInfo? = null

    override fun getSelectedCalendarId()   = saved?.id
    override fun getSelectedCalendarName() = saved?.summary

    override fun saveSelection(calendar: CalendarInfo) { saved = calendar }
    override fun clearSelection() { saved = null }
}
