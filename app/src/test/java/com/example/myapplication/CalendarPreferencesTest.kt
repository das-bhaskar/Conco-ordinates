package com.example.myapplication.logic

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [CalendarPreferences] contract.
 *
 * Uses [FakeCalendarPreferences] — no Android context required.
 * Tests verify the contract defined by the interface, not the
 * SharedPreferences implementation (which requires instrumentation tests).
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
        prefs.saveSelection("cal-123", "My Courses")

        assertEquals("cal-123",    prefs.getSelectedCalendarId())
        assertEquals("My Courses", prefs.getSelectedCalendarName())
    }

    @Test
    fun `hasSelection is true after saveSelection`() {
        prefs.saveSelection("cal-123", "My Courses")
        assertTrue(prefs.hasSelection)
    }

    @Test
    fun `saveSelection overwrites previous selection`() {
        prefs.saveSelection("cal-old", "Old Calendar")
        prefs.saveSelection("cal-new", "New Calendar")

        assertEquals("cal-new",      prefs.getSelectedCalendarId())
        assertEquals("New Calendar", prefs.getSelectedCalendarName())
    }

    // ── clearSelection ────────────────────────────────────────────────────────

    @Test
    fun `clearSelection removes id and name`() {
        prefs.saveSelection("cal-123", "My Courses")
        prefs.clearSelection()

        assertNull(prefs.getSelectedCalendarId())
        assertNull(prefs.getSelectedCalendarName())
    }

    @Test
    fun `hasSelection is false after clearSelection`() {
        prefs.saveSelection("cal-123", "My Courses")
        prefs.clearSelection()
        assertFalse(prefs.hasSelection)
    }

    @Test
    fun `clearSelection on empty prefs is a no-op`() {
        prefs.clearSelection() // should not throw
        assertNull(prefs.getSelectedCalendarId())
    }
}
