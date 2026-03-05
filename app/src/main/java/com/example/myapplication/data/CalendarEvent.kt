package com.example.myapplication.data

/**
 * Represents a single event fetched from Google Calendar.
 *
 * Intentionally kept as a plain data class with no Android/Google SDK
 * dependencies so it can be instantiated freely in unit tests.
 *
 * @param id            Google Calendar event ID
 * @param title         Summary / title of the event (e.g. "SOEN 357 – Lecture")
 * @param location      Raw location string from the calendar event
 *                      (e.g. "Sir George Williams Campus - Hall Building Rm 862")
 * @param startTimeMs   Event start time in epoch milliseconds (UTC)
 * @param endTimeMs     Event end time   in epoch milliseconds (UTC)
 * @param calendarId    ID of the calendar this event belongs to
 */
data class CalendarEvent(
    val id: String,
    val title: String,
    val location: String?,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val calendarId: String
)
