package com.example.myapplication.logic

import android.content.Context
import com.example.myapplication.data.CalendarEvent
import com.example.myapplication.telemetry.CrashReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

// ── Data ──────────────────────────────────────────────────────────────────────

/**
 * Represents a Google Calendar (list item from the calendarList endpoint).
 *
 * Plain data class — no Android SDK dependency — fully testable.
 */
data class CalendarInfo(
    val id: String,
    val summary: String,
    val description: String? = null
)

// ── Interface ─────────────────────────────────────────────────────────────────

/**
 * Abstracts all Google Calendar data access behind a testable interface.
 *
 * Mirrors the pattern used by [RouteProvider] and [LocationProvider]:
 * the ViewModel depends only on this interface, never on the concrete
 * implementation, making it trivial to swap in a [MockCalendarProvider]
 * in unit tests.
 */
interface CalendarProvider {

    /**
     * Returns all calendars the authenticated user has access to.
     * Returns empty list on auth failure or network error.
     */
    suspend fun getCalendars(): List<CalendarInfo>

    /**
     * Returns upcoming events from [calendarId], starting from [afterMs]
     * (epoch ms, defaults to now), limited to [maxResults] items.
     *
     * Events are returned in ascending chronological order.
     */
    suspend fun getUpcomingEvents(
        calendarId: String,
        afterMs: Long = System.currentTimeMillis(),
        maxResults: Int = 10
    ): List<CalendarEvent>

    /**
     * Convenience: returns the single next event that has a non-blank
     * [CalendarEvent.location] field, or null if none is found.
     */
    suspend fun getNextEventWithLocation(
        calendarId: String,
        afterMs: Long = System.currentTimeMillis()
    ): CalendarEvent? {
        return getUpcomingEvents(calendarId, afterMs, maxResults = 20)
            .firstOrNull { !it.location.isNullOrBlank() }
    }

    /**
     * Returns all events for the 7-day window starting at [weekStartMs].
     * Pass [currentWeekMonday()] for the current week (default).
     */
    suspend fun getWeekEvents(
        calendarId: String,
        weekStartMs: Long = currentWeekMonday()
    ): List<CalendarEvent> {
        val weekEndMs = weekStartMs + 7L * 24 * 60 * 60 * 1000
        return getUpcomingEvents(calendarId, afterMs = weekStartMs, maxResults = 100)
            .filter { it.startTimeMs < weekEndMs }
    }
}

/** Returns epoch ms for Monday 00:00 of the current week (local timezone). */
fun currentWeekMonday(): Long {
    val cal = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
        val dow = get(java.util.Calendar.DAY_OF_WEEK)
        val daysFromMonday = (dow + 5) % 7
        add(java.util.Calendar.DAY_OF_YEAR, -daysFromMonday)
    }
    return cal.timeInMillis
}

// ── Production implementation ─────────────────────────────────────────────────

/**
 * Fetches Google Calendar data using the REST API v3 with an OAuth 2.0
 * access token obtained via [GoogleSignIn] / [GoogleAuthProvider].
 *
 * **Construction:** create one instance per Activity lifecycle and pass it
 * into [MapViewModel] via the constructor (same pattern as
 * [GoogleRouteProvider]).
 *
 * @param context      Used only for token refresh; not stored after init.
 * @param tokenProvider Lambda that returns a fresh OAuth access token, or
 *                      null if the user is not signed in. Injected so the
 *                      auth mechanism can be swapped/mocked in tests.
 */
class GoogleCalendarProvider(
    private val context: Context,
    private val tokenProvider: suspend () -> String?
) : CalendarProvider {

    private val client = OkHttpClient()
    private val baseUrl = "https://www.googleapis.com/calendar/v3"

    // ── Public API ────────────────────────────────────────────────────────────

    override suspend fun getCalendars(): List<CalendarInfo> = withContext(Dispatchers.IO) {
        val token = tokenProvider() ?: return@withContext emptyList()

        return@withContext try {
            val json = get("$baseUrl/users/me/calendarList", token) ?: return@withContext emptyList()
            parseCalendarList(json)
        } catch (e: Exception) {
            CrashReporter.recordNonFatal(e, "calendar_list_fetch_failed")
            emptyList()
        }
    }

    override suspend fun getUpcomingEvents(
        calendarId: String,
        afterMs: Long,
        maxResults: Int
    ): List<CalendarEvent> = withContext(Dispatchers.IO) {
        val token = tokenProvider() ?: return@withContext emptyList()

        // RFC 3339 timestamp required by the API
        val timeMin = java.time.Instant.ofEpochMilli(afterMs)
            .toString() // e.g. "2026-03-04T14:30:00Z"

        val encodedId = java.net.URLEncoder.encode(calendarId, "UTF-8")
        val url = "$baseUrl/calendars/$encodedId/events" +
                "?orderBy=startTime" +
                "&singleEvents=true" +
                "&timeMin=$timeMin" +
                "&maxResults=$maxResults"

        return@withContext try {
            val json = get(url, token) ?: return@withContext emptyList()
            parseEventList(json, calendarId)
        } catch (e: Exception) {
            CrashReporter.recordNonFatal(e, "calendar_events_fetch_failed")
            emptyList()
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Executes a GET request with the Bearer token and returns the JSON body, or null on failure. */
    private fun get(url: String, token: String): JSONObject? {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return null

        if (!response.isSuccessful) {
            android.util.Log.e("CalendarProvider", "HTTP ${response.code}: $body")
            return null
        }

        return JSONObject(body)
    }

    private fun parseCalendarList(json: JSONObject): List<CalendarInfo> {
        val items = json.optJSONArray("items") ?: return emptyList()
        return (0 until items.length()).mapNotNull { i ->
            val item = items.getJSONObject(i)
            CalendarInfo(
                id          = item.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null,
                summary     = item.optString("summary", "(no name)"),
                description = item.optString("description").takeIf { it.isNotBlank() }
            )
        }
    }

    private fun parseEventList(json: JSONObject, calendarId: String): List<CalendarEvent> {
        val items = json.optJSONArray("items") ?: return emptyList()
        return (0 until items.length()).mapNotNull { i ->
            val item = items.getJSONObject(i)

            // Skip cancelled events
            if (item.optString("status") == "cancelled") return@mapNotNull null

            val start = item.optJSONObject("start")
            val startMs = parseDateTime(start) ?: return@mapNotNull null

            val end = item.optJSONObject("end")
            val endMs = parseDateTime(end) ?: startMs

            CalendarEvent(
                id          = item.optString("id"),
                title       = item.optString("summary", "(no title)"),
                location    = item.optString("location").takeIf { it.isNotBlank() },
                startTimeMs = startMs,
                endTimeMs   = endMs,
                calendarId  = calendarId
            )
        }
    }

    /**
     * Google Calendar returns either "dateTime" (timed events) or "date"
     * (all-day events). We handle both.
     */
    private fun parseDateTime(obj: JSONObject?): Long? {
        obj ?: return null

        // Timed event: "dateTime": "2026-03-04T10:15:00-05:00"
        val dateTime = obj.optString("dateTime").takeIf { it.isNotBlank() }
        if (dateTime != null) {
            return try {
                java.time.OffsetDateTime.parse(dateTime).toInstant().toEpochMilli()
            } catch (e: Exception) { null }
        }

        // All-day event: "date": "2026-03-04"
        val date = obj.optString("date").takeIf { it.isNotBlank() }
        if (date != null) {
            return try {
                java.time.LocalDate.parse(date)
                    .atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            } catch (e: Exception) { null }
        }

        return null
    }
}

// ── Test double ───────────────────────────────────────────────────────────────

/**
 * Deterministic stub for unit tests — no network, no auth.
 *
 * Usage:
 * ```kotlin
 * val provider = MockCalendarProvider(
 *     calendars = listOf(CalendarInfo("test-id", "My Courses")),
 *     events    = listOf(
 *         CalendarEvent("1", "SOEN 357", "Hall Building Rm 862", nowMs, nowMs + 3600_000, "test-id")
 *     )
 * )
 * ```
 */
class MockCalendarProvider(
    private val calendars: List<CalendarInfo> = emptyList(),
    private val events: List<CalendarEvent> = emptyList()
) : CalendarProvider {

    override suspend fun getCalendars(): List<CalendarInfo> = calendars

    override suspend fun getUpcomingEvents(
        calendarId: String,
        afterMs: Long,
        maxResults: Int
    ): List<CalendarEvent> = events
        .filter { it.calendarId == calendarId && it.startTimeMs >= afterMs }
        .sortedBy { it.startTimeMs }
        .take(maxResults)
}
