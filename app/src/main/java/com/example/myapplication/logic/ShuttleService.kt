package com.example.myapplication.logic

import com.example.myapplication.data.NearestStopResult
import com.example.myapplication.data.ShuttleAvailability
import com.example.myapplication.data.ShuttleDataSource
import com.example.myapplication.data.ShuttleRepo
import com.example.myapplication.data.ShuttleStop
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import java.util.Calendar

interface ShuttleService {
    fun checkAvailability(
        fromCampus: String,
        calendar: Calendar = Calendar.getInstance()
    ): ShuttleAvailability

    fun nearestStop(userLocation: LatLng?): NearestStopResult

    /**
     * Convenience wrapper: resolves [nearestStop] and picks the single best
     * candidate, handling the [NearestStopResult.Ambiguous] case internally.
     * Business logic kept in the service layer, not in the ViewModel.   [#9]
     */
    fun resolveNearestStop(userLocation: LatLng?): ShuttleStop?

    /** Exposes all stops so the ViewModel can pass them to the UI without
     *  importing [ShuttleRepo] directly.                                 [#6] */
    fun getAllStops(): List<ShuttleStop>

    fun statusMessage(fromCampus: String, calendar: Calendar = Calendar.getInstance()): String
}

/**
 * [repo] is injected so the service can be tested without a real [Context]
 * or Android resources.                                                  [#5]
 * The default value keeps existing call-sites working without changes.
 */
class DefaultShuttleService(
    private val repo: ShuttleDataSource = ShuttleRepo
) : ShuttleService {

    override fun checkAvailability(fromCampus: String, calendar: Calendar): ShuttleAvailability {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return ShuttleAvailability.WeekendOrHoliday
        }

        val departures = repo.getDepartures(fromCampus)
        if (departures.isEmpty()) return ShuttleAvailability.ScheduleUnavailable

        val nowMinutes = nowInMinutes(calendar)
        val next = departures.map { parseTime(it) }
            .firstOrNull { toMinutes(it) > nowMinutes }
            ?: return ShuttleAvailability.OutOfService

        return ShuttleAvailability.Active(nextDepartureMinutes = toMinutes(next) - nowMinutes)
    }

    override fun nearestStop(userLocation: LatLng?): NearestStopResult {
        if (userLocation == null) return NearestStopResult.LocationUnavailable

        val stops = repo.getAllStops()
        if (stops.isEmpty()) return NearestStopResult.NoStopsAvailable

        // Use SphericalUtil (Haversine) instead of flat-plane Pythagorean.  [#4]
        fun distMetres(stop: ShuttleStop): Double =
            SphericalUtil.computeDistanceBetween(stop.location, userLocation)

        val minDist    = stops.minOf { distMetres(it) }
        // epsilon: two stops are "equidistant" only if within 0.5 m of each other
        val candidates = stops.filter { Math.abs(distMetres(it) - minDist) < 0.5 }

        return if (candidates.size == 1) NearestStopResult.Found(candidates.first())
               else NearestStopResult.Ambiguous(candidates)
    }

    override fun resolveNearestStop(userLocation: LatLng?): ShuttleStop? =
        when (val result = nearestStop(userLocation)) {
            is NearestStopResult.Found     -> result.stop
            is NearestStopResult.Ambiguous -> result.candidates.firstOrNull()
            else                           -> null
        }

    override fun getAllStops(): List<ShuttleStop> = repo.getAllStops()

    override fun statusMessage(fromCampus: String, calendar: Calendar): String {
        return when (val avail = checkAvailability(fromCampus, calendar)) {
            is ShuttleAvailability.Active -> {
                val next = repo.getDepartures(fromCampus)
                    .map { parseTime(it) }
                    .firstOrNull { toMinutes(it) > nowInMinutes(calendar) }
                if (next != null) "Next shuttle: ${formatTime(next)} (in ${avail.nextDepartureMinutes} min)"
                else "Shuttle available"
            }
            ShuttleAvailability.OutOfService        -> "No more shuttles today"
            ShuttleAvailability.WeekendOrHoliday    -> "Weekend – no shuttle service"
            ShuttleAvailability.ScheduleUnavailable -> "Schedule unavailable"
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parseTime(hhmm: String): Pair<Int, Int> {
        val parts = hhmm.split(":")
        return Pair(parts[0].trim().toInt(), parts[1].trim().toInt())
    }

    private fun toMinutes(pair: Pair<Int, Int>): Int = pair.first * 60 + pair.second

    private fun formatTime(pair: Pair<Int, Int>): String =
        String.format("%02d:%02d", pair.first, pair.second)

    private fun nowInMinutes(cal: Calendar): Int =
        cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
}
