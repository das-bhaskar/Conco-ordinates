package com.example.myapplication.logic

import com.example.myapplication.data.CalendarEvent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockCalendarProviderAdditionalTest {

    private val calendars = listOf(
        CalendarInfo(id = "cal-1", summary = "Courses"),
        CalendarInfo(id = "cal-2", summary = "Work", description = "Personal schedule")
    )

    private val events = listOf(
        CalendarEvent("1", "Math", null, 1_000L, 2_000L, "cal-1"),
        CalendarEvent("2", "Physics", "H-820", 2_000L, 3_000L, "cal-1"),
        CalendarEvent("3", "Chemistry", "CC-101", 4_000L, 5_000L, "cal-1"),
        CalendarEvent("4", "Meeting", "MB-1.210", 1_500L, 2_500L, "cal-2")
    )

    @Test
    fun `getCalendars returns configured calendars unchanged`() = runTest {
        val provider = MockCalendarProvider(calendars = calendars, events = events)

        val result = provider.getCalendars()

        assertEquals(calendars, result)
        assertEquals("Personal schedule", result[1].description)
    }

    @Test
    fun `getNextEventWithLocation skips blank locations and returns earliest matching event`() = runTest {
        val provider = MockCalendarProvider(calendars = calendars, events = events)

        val result = provider.getNextEventWithLocation("cal-1", afterMs = 500L)

        assertEquals("2", result?.id)
        assertEquals("H-820", result?.location)
    }

    @Test
    fun `getNextEventWithLocation returns null when no matching event has a location`() = runTest {
        val provider = MockCalendarProvider(
            calendars = calendars,
            events = listOf(CalendarEvent("1", "Math", null, 1_000L, 2_000L, "cal-1"))
        )

        val result = provider.getNextEventWithLocation("cal-1", afterMs = 0L)

        assertNull(result)
    }

    @Test
    fun `getWeekEvents includes only events before end of seven day window`() = runTest {
        val provider = MockCalendarProvider(
            calendars = calendars,
            events = listOf(
                CalendarEvent("week-in", "In Week", "H-1", 10_000L, 11_000L, "cal-1"),
                CalendarEvent("week-out", "Out Week", "H-2", 10_000L + 7 * 24 * 60 * 60 * 1000L, 12_000L, "cal-1")
            )
        )

        val result = provider.getWeekEvents("cal-1", weekStartMs = 10_000L)

        assertEquals(1, result.size)
        assertEquals("week-in", result.first().id)
    }
}
