package com.example.myapplication.data

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.*
import org.junit.Test

class ShuttleDataTest {

    // ── 1. ShuttleDirection (Enum) Coverage ────────────────────────────────
    @Test
    fun `ShuttleDirection enum coverage`() {
        // Test display names
        assertEquals("SGW → Loyola", ShuttleDirection.SGW_TO_LOYOLA.displayName)
        assertEquals("Loyola → SGW", ShuttleDirection.LOYOLA_TO_SGW.displayName)

        // Test values and valueOf (Required for 100% JaCoCo coverage)
        val values = ShuttleDirection.values()
        assertEquals(2, values.size)
        assertEquals(ShuttleDirection.SGW_TO_LOYOLA, ShuttleDirection.valueOf("SGW_TO_LOYOLA"))
    }

    // ── 2. ShuttleStop & ShuttleRoute (Data Classes) Coverage ──────────────
    @Test
    fun `ShuttleStop data class coverage`() {
        val loc = LatLng(45.497, -73.579)
        val stop1 = ShuttleStop("S1", "Hall", "SGW", loc)
        val stop2 = stop1.copy(id = "S2")

        // Test properties
        assertEquals("S1", stop1.id)
        assertEquals(loc, stop1.location)

        // Test equals, hashCode, and toString
        assertNotEquals(stop1, stop2)
        assertEquals(stop1, stop1.copy())
        assertNotNull(stop1.toString())
        assertEquals(stop1.hashCode(), stop1.hashCode())
    }

    @Test
    fun `ShuttleRoute data class coverage`() {
        val stopA = ShuttleStop("S1", "Hall", "SGW", LatLng(45.49, -73.57))
        val stopB = ShuttleStop("L1", "Loyola", "LOY", LatLng(45.45, -73.64))
        val points = listOf(stopA.location, stopB.location)

        val route = ShuttleRoute(
            direction = ShuttleDirection.SGW_TO_LOYOLA,
            boardingStop = stopA,
            alightingStop = stopB,
            polylinePoints = points,
            durationText = "20 mins",
            distanceText = "7 km"
        )

        // Exercise generated methods for coverage
        val routeCopy = route.copy(durationText = "25 mins")
        assertEquals("25 mins", routeCopy.durationText)
        assertTrue(route.toString().contains("SGW_TO_LOYOLA"))
        assertEquals(route.hashCode(), route.hashCode())
    }

    // ── 3. ShuttleAvailability (Sealed Class) Coverage ─────────────────────
    @Test
    fun `ShuttleAvailability sealed class coverage`() {
        val active = ShuttleAvailability.Active(15)
        val outOfService = ShuttleAvailability.OutOfService
        val weekend = ShuttleAvailability.WeekendOrHoliday
        val unavailable = ShuttleAvailability.ScheduleUnavailable

        assertEquals(15, active.nextDepartureMinutes)

        // Exercise equals/toString for the data class branch
        assertEquals(active, active.copy())
        assertNotNull(outOfService.toString())
    }

    // ── 4. NearestStopResult (Sealed Class) Coverage ───────────────────────
    @Test
    fun `NearestStopResult branches coverage`() {
        val stop = ShuttleStop("S1", "Hall", "SGW", LatLng(0.0, 0.0))

        val found = NearestStopResult.Found(stop)
        val ambiguous = NearestStopResult.Ambiguous(listOf(stop))
        val locUnavail = NearestStopResult.LocationUnavailable
        val noStops = NearestStopResult.NoStopsAvailable

        assertEquals(stop, found.stop)
        assertEquals(1, ambiguous.candidates.size)
        assertNotNull(locUnavail.toString())
    }

    // ── 5. ShuttleRouteResult (Sealed Class) Coverage ──────────────────────
    @Test
    fun `ShuttleRouteResult branches coverage`() {
        // Success branch requires a Route object
        val stop = ShuttleStop("S1", "Hall", "SGW", LatLng(0.0, 0.0))
        val route = ShuttleRoute(ShuttleDirection.SGW_TO_LOYOLA, stop, stop, emptyList(), "", "")

        val results = listOf(
            ShuttleRouteResult.Success(route),
            ShuttleRouteResult.NetworkError,
            ShuttleRouteResult.NoRouteFound,
            ShuttleRouteResult.ApiKeyMissing,
            ShuttleRouteResult.InvalidStops
        )

        results.forEach { assertNotNull(it.toString()) }
        assertEquals(route, (results[0] as ShuttleRouteResult.Success).route)
    }
}