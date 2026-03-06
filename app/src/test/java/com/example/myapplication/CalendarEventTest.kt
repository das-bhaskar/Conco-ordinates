package com.example.myapplication.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [CalendarEvent.locationResult] and [CalendarEvent.parsedLocation].
 *
 * Verifies that the lazy properties delegate correctly to [parseLocation]
 * and that the convenience accessor [parsedLocation] only returns non-null
 * for [LocationResult.Known].
 */
class CalendarEventTest {

    private fun event(location: String?) = CalendarEvent(
        id          = "test-id",
        calendarId  = "cal-1",
        title       = "Test Event",
        startTimeMs = System.currentTimeMillis(),
        endTimeMs   = System.currentTimeMillis() + 3_600_000,
        location    = location
    )

    // ── locationResult delegation ─────────────────────────────────────────────

    @Test
    fun `null location returns Unknown`() {
        assertEquals(LocationResult.Unknown, event(null).locationResult)
    }

    @Test
    fun `blank location returns Unknown`() {
        assertEquals(LocationResult.Unknown, event("").locationResult)
    }

    @Test
    fun `online location returns Online`() {
        assertEquals(LocationResult.Online, event("Online").locationResult)
    }

    @Test
    fun `TBA location returns TBA`() {
        assertEquals(LocationResult.TBA, event("TBA").locationResult)
    }

    @Test
    fun `valid room returns Known`() {
        val result = event("H 820 SGW").locationResult
        assertTrue(result is LocationResult.Known)
    }

    // ── parsedLocation convenience accessor ───────────────────────────────────

    @Test
    fun `parsedLocation is non-null for Known result`() {
        assertNotNull(event("H 820 SGW").parsedLocation)
    }

    @Test
    fun `parsedLocation is null for Online result`() {
        assertNull(event("Online").parsedLocation)
    }

    @Test
    fun `parsedLocation is null for TBA result`() {
        assertNull(event("TBA").parsedLocation)
    }

    @Test
    fun `parsedLocation is null for Unknown result`() {
        assertNull(event("").parsedLocation)
    }

    // ── lazy evaluation ───────────────────────────────────────────────────────

    @Test
    fun `locationResult is consistent across multiple accesses`() {
        val e = event("MB S1.401 SGW")
        // Access twice — lazy property should return the same instance
        assertSame(e.locationResult, e.locationResult)
    }
}
