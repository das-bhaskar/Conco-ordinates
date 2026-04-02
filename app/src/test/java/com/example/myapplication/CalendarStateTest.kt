package com.example.myapplication

import com.example.myapplication.logic.CalendarInfo
import com.example.myapplication.ui.models.CalendarState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarStateTest {

    @Test
    fun `Idle and Loading states can be created`() {
        assertEquals(CalendarState.Idle, CalendarState.Idle)
        assertEquals(CalendarState.Loading, CalendarState.Loading)
    }

    @Test
    fun `SelectingCalendar stores provided calendars`() {
        val calendars = listOf(
            CalendarInfo(id = "cal-1", summary = "My Courses"),
            CalendarInfo(id = "cal-2", summary = "Work")
        )

        val state = CalendarState.SelectingCalendar(calendars)

        assertEquals(2, state.calendars.size)
        assertEquals("cal-1", state.calendars[0].id)
        assertEquals("My Courses", state.calendars[0].summary)
    }

    @Test
    fun `Error stores provided message`() {
        val state = CalendarState.Error("Connection failed")

        assertEquals("Connection failed", state.message)
        assertTrue(state.toString().contains("Connection failed"))
    }
}
