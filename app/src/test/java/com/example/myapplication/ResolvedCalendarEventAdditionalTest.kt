package com.example.myapplication

import com.example.myapplication.data.CalendarEvent
import com.example.myapplication.data.LocationResult
import com.example.myapplication.data.ParsedLocation
import com.example.myapplication.data.ResolvedCalendarEvent
import org.junit.Assert.*
import org.junit.Test

/**
 * Additional coverage for [ResolvedCalendarEvent]:
 * - TBA case for destinationBuildingCode
 * - Convenience delegate properties
 * - Known with blank buildingCode fallback to raw location
 * - Unknown with null/blank raw location
 */
class ResolvedCalendarEventAdditionalTest {

    private val sampleEvent = CalendarEvent(
        id = "evt-42",
        title = "COMP 354 Lecture",
        location = "H 820 SGW",
        startTimeMs = 1_700_000_000L,
        endTimeMs   = 1_700_003_600L,
        calendarId = "cal-1"
    )

    private val knownLocation = LocationResult.Known(
        ParsedLocation(
            buildingCode = "H",
            buildingName = "Henry F. Hall Building",
            roomCode     = "820",
            campus       = "Sir George Williams"
        )
    )

    // ── destinationBuildingCode: TBA case ─────────────────────────────────────

    @Test
    fun `destinationBuildingCode is null for TBA event`() {
        val resolved = ResolvedCalendarEvent(
            event = sampleEvent,
            locationResult = LocationResult.TBA
        )
        assertNull(resolved.destinationBuildingCode)
    }

    // ── destinationBuildingCode: Known with blank buildingCode ────────────────

    @Test
    fun `destinationBuildingCode falls back to raw location when buildingCode is blank`() {
        val blankCodeLocation = LocationResult.Known(
            ParsedLocation(
                buildingCode = "",
                buildingName = "",
                roomCode     = "100",
                campus       = "Loyola"
            )
        )
        val resolved = ResolvedCalendarEvent(
            event = sampleEvent.copy(location = "H 820 SGW"),
            locationResult = blankCodeLocation
        )
        assertEquals("H 820 SGW", resolved.destinationBuildingCode)
    }

    // ── destinationBuildingCode: Unknown with null raw location ───────────────

    @Test
    fun `destinationBuildingCode is null when Unknown and raw location is null`() {
        val resolved = ResolvedCalendarEvent(
            event = sampleEvent.copy(location = null),
            locationResult = LocationResult.Unknown
        )
        assertNull(resolved.destinationBuildingCode)
    }

    @Test
    fun `destinationBuildingCode is null when Unknown and raw location is blank`() {
        val resolved = ResolvedCalendarEvent(
            event = sampleEvent.copy(location = "  "),
            locationResult = LocationResult.Unknown
        )
        assertNull(resolved.destinationBuildingCode)
    }

    // ── Convenience delegate properties ───────────────────────────────────────

    @Test
    fun `id delegates to event id`() {
        val resolved = ResolvedCalendarEvent(event = sampleEvent, locationResult = knownLocation)
        assertEquals("evt-42", resolved.id)
    }

    @Test
    fun `title delegates to event title`() {
        val resolved = ResolvedCalendarEvent(event = sampleEvent, locationResult = knownLocation)
        assertEquals("COMP 354 Lecture", resolved.title)
    }

    @Test
    fun `location delegates to event location`() {
        val resolved = ResolvedCalendarEvent(event = sampleEvent, locationResult = knownLocation)
        assertEquals("H 820 SGW", resolved.location)
    }

    @Test
    fun `startTimeMs delegates to event startTimeMs`() {
        val resolved = ResolvedCalendarEvent(event = sampleEvent, locationResult = knownLocation)
        assertEquals(1_700_000_000L, resolved.startTimeMs)
    }

    @Test
    fun `endTimeMs delegates to event endTimeMs`() {
        val resolved = ResolvedCalendarEvent(event = sampleEvent, locationResult = knownLocation)
        assertEquals(1_700_003_600L, resolved.endTimeMs)
    }

    @Test
    fun `calendarId delegates to event calendarId`() {
        val resolved = ResolvedCalendarEvent(event = sampleEvent, locationResult = knownLocation)
        assertEquals("cal-1", resolved.calendarId)
    }

    // ── parsedLocation ────────────────────────────────────────────────────────

    @Test
    fun `parsedLocation returns location for Known result`() {
        val resolved = ResolvedCalendarEvent(event = sampleEvent, locationResult = knownLocation)
        assertNotNull(resolved.parsedLocation)
        assertEquals("H", resolved.parsedLocation!!.buildingCode)
        assertEquals("820", resolved.parsedLocation!!.roomCode)
    }

    @Test
    fun `parsedLocation is null for Online result`() {
        val resolved = ResolvedCalendarEvent(
            event = sampleEvent,
            locationResult = LocationResult.Online
        )
        assertNull(resolved.parsedLocation)
    }

    @Test
    fun `parsedLocation is null for TBA result`() {
        val resolved = ResolvedCalendarEvent(
            event = sampleEvent,
            locationResult = LocationResult.TBA
        )
        assertNull(resolved.parsedLocation)
    }

    @Test
    fun `parsedLocation is null for Unknown result`() {
        val resolved = ResolvedCalendarEvent(
            event = sampleEvent,
            locationResult = LocationResult.Unknown
        )
        assertNull(resolved.parsedLocation)
    }
}
