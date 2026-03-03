package com.example.myapplication.data

import com.google.android.gms.maps.model.LatLng

// ── Direction ─────────────────────────────────────────────────────────────
enum class ShuttleDirection(val displayName: String) {
    SGW_TO_LOYOLA("SGW → Loyola"),
    LOYOLA_TO_SGW("Loyola → SGW")
}

// ── Stop ──────────────────────────────────────────────────────────────────
data class ShuttleStop(
    val id:       String,
    val name:     String,
    val campus:   String,   // "SGW" or "Loyola"
    val location: LatLng
)

// ── Route ─────────────────────────────────────────────────────────────────
data class ShuttleRoute(
    val direction:      ShuttleDirection,
    val boardingStop:   ShuttleStop,
    val alightingStop:  ShuttleStop,
    val polylinePoints: List<LatLng>,
    val durationText:   String,
    val distanceText:   String
)

// ── Availability sealed class (US-2.7) ────────────────────────────────────
sealed class ShuttleAvailability {
    data class Active(val nextDepartureMinutes: Int) : ShuttleAvailability()
    object OutOfService     : ShuttleAvailability()
    object WeekendOrHoliday : ShuttleAvailability()
    object ScheduleUnavailable : ShuttleAvailability()
}

// ── Nearest stop result (US-2.8) ──────────────────────────────────────────
sealed class NearestStopResult {
    data class Found(val stop: ShuttleStop)              : NearestStopResult()
    data class Ambiguous(val candidates: List<ShuttleStop>) : NearestStopResult()
    object LocationUnavailable : NearestStopResult()
    object NoStopsAvailable    : NearestStopResult()
}

// ── Route result (US-2.6) ─────────────────────────────────────────────────
sealed class ShuttleRouteResult {
    data class Success(val route: ShuttleRoute) : ShuttleRouteResult()
    object NetworkError  : ShuttleRouteResult()
    object NoRouteFound  : ShuttleRouteResult()
    object ApiKeyMissing : ShuttleRouteResult()
    object InvalidStops  : ShuttleRouteResult()
}

// ── Static stop data ──────────────────────────────────────────────────────
object ShuttleStopData {
    val ALL_STOPS = listOf(
        ShuttleStop(
            id       = "sgw_main",
            name     = "SGW — Loyola Bus Stop (de Maisonneuve)",
            campus   = "SGW",
            location = LatLng(45.49719, -73.57859)
        ),
        ShuttleStop(
            id       = "loyola_main",
            name     = "Loyola — Shuttle Terminal (Sherbrooke W.)",
            campus   = "Loyola",
            location = LatLng(45.45825, -73.63913)
        )
    )

    // Concordia shuttle schedule (Mon–Fri)
    // Format: "HH:MM" departures from SGW
    val SGW_DEPARTURES = listOf(
        "09:15", "09:45", "10:15","10:45", "11:45", "12:15", "12:45",
        "13:15", "13:45", "14:15", "14:45", "15:15", "15:45", "16:15",
        "17:15","17:45","18:15","18:45"
    )

    val LOYOLA_DEPARTURES = listOf(
        "09:15", "09:45", "10:15", "10:45", "11:15", "11:45", "12:15", "12:45",
        "13:15", "13:45", "14:15", "14:45", "15:15", "15:45", "16:15",
        "17:45","18:15","18:45"
    )
}
