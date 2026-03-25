package com.example.myapplication

import com.example.myapplication.data.CalendarEvent
import com.example.myapplication.data.LocationResult
import com.example.myapplication.data.ParsedLocation
import com.example.myapplication.data.ResolvedCalendarEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResolvedCalendarEventTest {

    private fun makeEvent(location: String? = "H 820 SGW") = CalendarEvent(
        id = "event-1",
        title = "SOEN 390",
        location = location,
        startTimeMs = 1_000L,
        endTimeMs = 2_000L,
        calendarId = "calendar-1"
    )

    @Test
    fun `destinationBuildingCode returns building code for known location`() {
        val resolved = ResolvedCalendarEvent(
            event = makeEvent(),
            locationResult = LocationResult.Known(
                ParsedLocation(
                    buildingCode = "H",
                    buildingName = "Henry F. Hall Building",
                    roomCode = "820",
                    campus = "Sir George Williams"
                )
            )
        )

        assertEquals("H", resolved.destinationBuildingCode)
    }

    @Test
    fun `destinationBuildingCode falls back to raw location for unknown result`() {
        val resolved = ResolvedCalendarEvent(
            event = makeEvent(location = "Hall Building"),
            locationResult = LocationResult.Unknown
        )

        assertEquals("Hall Building", resolved.destinationBuildingCode)
    }

    @Test
    fun `destinationBuildingCode is null for online event`() {
        val resolved = ResolvedCalendarEvent(
            event = makeEvent(location = "Online"),
            locationResult = LocationResult.Online
        )

        assertNull(resolved.destinationBuildingCode)
    }
}
