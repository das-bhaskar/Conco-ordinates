package com.example.myapplication.data

/**
 * Represents a single event fetched from Google Calendar.
 *
 * Pure data container — no parsing logic, no Android/Google SDK dependencies,
 * so it can be instantiated freely in unit tests.
 *
 * Location parsing is performed by [com.example.myapplication.logic.LocationResolver]
 * in the ViewModel layer, keeping the parsing strategy independent of this data
 * class (Dependency Rule / SRP). Results are wrapped in [ResolvedCalendarEvent].
 *
 * @param id            Google Calendar event ID
 * @param title         Summary / title of the event (e.g. "SOEN 357 – Lecture")
 * @param location      Raw location string from the calendar event
 *                      (e.g. "MB S1.401 SGW" or "Sir George Williams Campus - Hall Building Rm 862")
 * @param startTimeMs   Event start time in epoch milliseconds (UTC)
 * @param endTimeMs     Event end time   in epoch milliseconds (UTC)
 * @param calendarId    ID of the calendar this event belongs to
 */
data class CalendarEvent(
    val id:          String,
    val title:       String,
    val location:    String?,
    val startTimeMs: Long,
    val endTimeMs:   Long,
    val calendarId:  String
)
