package com.example.myapplication.logic

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [CalendarPreferences] contract.
 *
 * Uses [FakeCalendarPreferences] — no Android context required.
 * Tests verify the interface contract, not the SharedPreferences
 * implementation (which requires instrumentation tests).
 *
 * [saveSelection] now accepts a [CalendarInfo] object (PR #282) so
 * callers cannot accidentally swap the id and name arguments.
 */
class CalendarPreferencesTest {

    private lateinit var prefs: CalendarPreferences

    @Before
    fun setup() {
        prefs = FakeCalendarPreferences()
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state has no selection`() {
        assertNull(prefs.getSelectedCalendarId())
        assertNull(prefs.getSelectedCalendarName())
        assertFalse(prefs.hasSelection)
    }

    // ── saveSelection ─────────────────────────────────────────────────────────

    @Test
    fun `saveSelection persists id and name`() {
        prefs.saveSelection(CalendarInfo(id = "cal-123", summary = "My Courses"))

        assertEquals("cal-123",    prefs.getSelectedCalendarId())
        assertEquals("My Courses", prefs.getSelectedCalendarName())
    }

    @Test
    fun `hasSelection is true after saveSelection`() {
        prefs.saveSelection(CalendarInfo(id = "cal-123", summary = "My Courses"))
        assertTrue(prefs.hasSelection)
    }

    @Test
    fun `saveSelection overwrites previous selection`() {
        prefs.saveSelection(CalendarInfo(id = "cal-old", summary = "Old Calendar"))
        prefs.saveSelection(CalendarInfo(id = "cal-new", summary = "New Calendar"))

        assertEquals("cal-new",      prefs.getSelectedCalendarId())
        assertEquals("New Calendar", prefs.getSelectedCalendarName())
    }

    // ── clearSelection ────────────────────────────────────────────────────────

    @Test
    fun `clearSelection removes id and name`() {
        prefs.saveSelection(CalendarInfo(id = "cal-123", summary = "My Courses"))
        prefs.clearSelection()

        assertNull(prefs.getSelectedCalendarId())
        assertNull(prefs.getSelectedCalendarName())
    }

    @Test
    fun `hasSelection is false after clearSelection`() {
        prefs.saveSelection(CalendarInfo(id = "cal-123", summary = "My Courses"))
        prefs.clearSelection()
        assertFalse(prefs.hasSelection)
    }

    @Test
    fun `clearSelection on empty prefs is a no-op`() {
        prefs.clearSelection() // should not throw
        assertNull(prefs.getSelectedCalendarId())
    }
}
