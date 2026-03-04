package com.example.myapplication.data

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.example.myapplication.R
import com.example.myapplication.telemetry.CrashReporter

// ── JSON model classes (internal, only used for parsing) ─────────────────────

private data class JsonStop(
    val id: String,
    val name: String,
    val campus: String,
    val latitude: Double,
    val longitude: Double
)

private data class JsonSchedule(
    val fromCampus: String,
    val departures: List<String>
)

private data class JsonShuttleData(
    val stops: List<JsonStop>,
    val schedules: List<JsonSchedule>
)

// ── ShuttleRepo ───────────────────────────────────────────────────────────────

/**
 * ShuttleRepo – loads shuttle stops and schedules from [R.raw.shuttle_schedule].
 *
 * Call [initialize] once in [MapsActivity.onCreate] (after [CampusRepo.initialize]).
 * After that, [getAllStops] and [getDepartures] are safe to call from any thread.
 *
 * Keeping data in a JSON file (res/raw/shuttle_schedule.json) means the
 * schedule can be updated without touching any Kotlin source files.
 */
object ShuttleRepo {

    private var stops: List<ShuttleStop> = emptyList()
    private var schedules: Map<String, List<String>> = emptyMap()  // campus → departures
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        try {
            val inputStream = context.resources.openRawResource(R.raw.shuttle_schedule)
            val jsonString  = inputStream.bufferedReader().use { it.readText() }
            val data        = Gson().fromJson(jsonString, JsonShuttleData::class.java)

            stops = data.stops.map { s ->
                ShuttleStop(
                    id       = s.id,
                    name     = s.name,
                    campus   = s.campus,
                    location = LatLng(s.latitude, s.longitude)
                )
            }

            schedules = data.schedules.associate { it.fromCampus to it.departures }

            initialized = true
            android.util.Log.d("SHUTTLE_REPO", "Loaded ${stops.size} stops, ${schedules.size} schedules")
        } catch (e: Exception) {
            CrashReporter.recordNonFatal(e, "shuttle_schedule_load_failed")
            android.util.Log.e("SHUTTLE_REPO", "Failed to load shuttle schedule: ${e.message}")
        }
    }

    fun getAllStops(): List<ShuttleStop> = stops

    /** Returns departure strings ("HH:MM") for the given campus, or empty list. */
    fun getDepartures(fromCampus: String): List<String> =
        schedules[fromCampus] ?: emptyList()

    /** For unit tests only – inject mock data without needing a Context. */
    fun setTestData(testStops: List<ShuttleStop>, testSchedules: Map<String, List<String>>) {
        stops     = testStops
        schedules = testSchedules
        initialized = true
    }
}
