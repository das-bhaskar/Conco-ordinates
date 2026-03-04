package com.example.myapplication.logic

import com.example.myapplication.data.NearestStopResult
import com.example.myapplication.data.ShuttleAvailability
import com.example.myapplication.data.ShuttleRepo
import com.example.myapplication.data.ShuttleStop
import com.google.android.gms.maps.model.LatLng
import java.util.Calendar

interface ShuttleService {
    fun checkAvailability(
        fromCampus: String,
        calendar: Calendar = Calendar.getInstance()
    ): ShuttleAvailability

    fun nearestStop(userLocation: LatLng?): NearestStopResult

    fun statusMessage(fromCampus: String, calendar: Calendar = Calendar.getInstance()): String
}

class DefaultShuttleService : ShuttleService {

    override fun checkAvailability(fromCampus: String, calendar: Calendar): ShuttleAvailability {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return ShuttleAvailability.WeekendOrHoliday
        }

        val departures = ShuttleRepo.getDepartures(fromCampus)
        if (departures.isEmpty()) return ShuttleAvailability.ScheduleUnavailable

        val nowMinutes = nowInMinutes(calendar)
        val next = departures.map { parseTime(it) }
            .firstOrNull { toMinutes(it) > nowMinutes }
            ?: return ShuttleAvailability.OutOfService

        return ShuttleAvailability.Active(nextDepartureMinutes = toMinutes(next) - nowMinutes)
    }

    override fun nearestStop(userLocation: LatLng?): NearestStopResult {
        if (userLocation == null) return NearestStopResult.LocationUnavailable

        val stops = ShuttleRepo.getAllStops()
        if (stops.isEmpty()) return NearestStopResult.NoStopsAvailable

        fun distSq(stop: ShuttleStop): Double {
            val dLat = stop.location.latitude  - userLocation.latitude
            val dLng = stop.location.longitude - userLocation.longitude
            return dLat * dLat + dLng * dLng
        }

        val minDist    = stops.minOf { distSq(it) }
        val candidates = stops.filter { Math.abs(distSq(it) - minDist) < 1e-12 }

        return if (candidates.size == 1) NearestStopResult.Found(candidates.first())
               else NearestStopResult.Ambiguous(candidates)
    }

    override fun statusMessage(fromCampus: String, calendar: Calendar): String {
        return when (val avail = checkAvailability(fromCampus, calendar)) {
            is ShuttleAvailability.Active -> {
                val next = ShuttleRepo.getDepartures(fromCampus)
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
