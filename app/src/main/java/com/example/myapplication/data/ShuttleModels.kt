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
    object OutOfService        : ShuttleAvailability()
    object WeekendOrHoliday    : ShuttleAvailability()
    object ScheduleUnavailable : ShuttleAvailability()
}

// ── Nearest stop result (US-2.8) ──────────────────────────────────────────
sealed class NearestStopResult {
    data class Found(val stop: ShuttleStop)                 : NearestStopResult()
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
