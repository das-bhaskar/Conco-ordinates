package com.example.myapplication.data

import com.example.myapplication.logic.LocationResolver
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ResolvedCalendarEvent].
 *
 * After the SRP refactor (PR #282), [CalendarEvent] is a pure data class with
 * no parsing logic. Location resolution lives in [LocationResolver] and the
 * result is wrapped in [ResolvedCalendarEvent] by the ViewModel.
 *
 * These tests verify:
 *  - [ResolvedCalendarEvent.locationResult] reflects the injected result
 *  - [ResolvedCalendarEvent.parsedLocation] convenience accessor
 *  - Delegate properties forward correctly to the underlying [CalendarEvent]
 */
class CalendarEventTest {

    private lateinit var resolver: LocationResolver

    private val fakeNames = mapOf(
        "H"  to "Henry F. Hall Building",
        "MB" to "John Molson Building"
    )

    @Before
    fun setUp() {
        resolver = LocationResolver(buildingNames = { code -> fakeNames[code] })
    }

    private fun resolved(location: String?) = ResolvedCalendarEvent(
        event = CalendarEvent(
            id          = "test-id",
            calendarId  = "cal-1",
            title       = "Test Event",
            startTimeMs = System.currentTimeMillis(),
            endTimeMs   = System.currentTimeMillis() + 3_600_000,
            location    = location
        ),
        locationResult = resolver.resolve(location)
    )

    // ── locationResult ────────────────────────────────────────────────────────

    @Test
    fun `null location resolves to Unknown`() {
        assertEquals(LocationResult.Unknown, resolved(null).locationResult)
    }

    @Test
    fun `blank location resolves to Unknown`() {
        assertEquals(LocationResult.Unknown, resolved("").locationResult)
    }

    @Test
    fun `online location resolves to Online`() {
        assertEquals(LocationResult.Online, resolved("Online").locationResult)
    }

    @Test
    fun `TBA location resolves to TBA`() {
        assertEquals(LocationResult.TBA, resolved("TBA").locationResult)
    }

    @Test
    fun `valid room resolves to Known`() {
        assertTrue(resolved("H 820 SGW").locationResult is LocationResult.Known)
    }

    // ── parsedLocation convenience accessor ───────────────────────────────────

    @Test
    fun `parsedLocation is non-null for Known result`() {
        assertNotNull(resolved("H 820 SGW").parsedLocation)
    }

    @Test
    fun `parsedLocation is null for Online result`() {
        assertNull(resolved("Online").parsedLocation)
    }

    @Test
    fun `parsedLocation is null for TBA result`() {
        assertNull(resolved("TBA").parsedLocation)
    }

    @Test
    fun `parsedLocation is null for Unknown result`() {
        assertNull(resolved("").parsedLocation)
    }

    // ── delegate properties ───────────────────────────────────────────────────

    @Test
    fun `id delegates to underlying CalendarEvent`() {
        assertEquals("test-id", resolved("H 820 SGW").id)
    }

    @Test
    fun `title delegates to underlying CalendarEvent`() {
        assertEquals("Test Event", resolved("H 820 SGW").title)
    }

    @Test
    fun `location delegates to underlying CalendarEvent`() {
        assertEquals("H 820 SGW", resolved("H 820 SGW").location)
    }

    // ── destinationBuildingCode logic ─────────────────────────────────────────

    @Test
    fun `destinationBuildingCode returns buildingCode when known`() {
        val res = resolved("H 820 SGW")
        assertEquals("H", res.destinationBuildingCode)
    }



    @Test
    fun `destinationBuildingCode returns raw location when unknown`() {
        val res = resolved("Random Street Address")
        assertEquals("Random Street Address", res.destinationBuildingCode)
    }

    @Test
    fun `destinationBuildingCode is null for Online or TBA`() {
        assertNull(resolved("Online").destinationBuildingCode)
        assertNull(resolved("TBA").destinationBuildingCode)
    }

    @Test
    fun `destinationBuildingCode is null if location is blank and unknown`() {
        val res = resolved("   ")
        assertNull(res.destinationBuildingCode)
    }
    @Test
    fun `remaining properties delegate correctly`() {
        val res = resolved("H 820")
        assertTrue(res.startTimeMs > 0)
        assertTrue(res.endTimeMs > res.startTimeMs)
        assertEquals("cal-1", res.calendarId)
    }
    @Test
    fun `verify data class boilerplate`() {
        val res = resolved("H 820")
        val copy = res.event.copy(id = "new-id")
        assertNotEquals(res.event, copy)
        assertNotNull(res.toString())
    }
}
