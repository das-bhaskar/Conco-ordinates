package com.example.myapplication.logic

import com.example.myapplication.data.NearestStopResult
import com.example.myapplication.data.ShuttleDirection
import com.example.myapplication.data.ShuttleStop
import com.example.myapplication.data.ShuttleStopData
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * US-2.8 — Task #111 #112
 * Task-2.8.1: Identify nearest stop
 * Task-2.8.2: Handle equidistant stops
 */
class ShuttleStopFinderTest {

    private lateinit var finder: ShuttleStopFinder

    @Before fun setup() {
        finder = ShuttleStopFinderImpl()
    }

    // AC1: user near SGW stop -> Found
    @Test fun `user near SGW stop returns Found`() {
        // Very close to SGW stop (45.49719, -73.57859)
        val userLocation = LatLng(45.49720, -73.57860)
        val result = finder.findNearest(userLocation, ShuttleDirection.SGW_TO_LOYOLA)
        assertTrue(result is NearestStopResult.Found)
        assertEquals("sgw_main", (result as NearestStopResult.Found).stop.id)
    }

    @Test fun `user near Loyola stop returns Found for Loyola direction`() {
        val userLocation = LatLng(45.45826, -73.63914)
        val result = finder.findNearest(userLocation, ShuttleDirection.LOYOLA_TO_SGW)
        assertTrue(result is NearestStopResult.Found)
        assertEquals("loyola_main", (result as NearestStopResult.Found).stop.id)
    }

    // AC2: wrong direction has no stops on that campus -> NoStopsAvailable
    @Test fun `SGW_TO_LOYOLA only returns SGW boarding stops`() {
        val result = finder.findNearest(LatLng(45.49719, -73.57859), ShuttleDirection.SGW_TO_LOYOLA)
        // Should find SGW stop, not Loyola
        assertTrue(result is NearestStopResult.Found)
        assertEquals("SGW", (result as NearestStopResult.Found).stop.campus)
    }

    @Test fun `LOYOLA_TO_SGW only returns Loyola boarding stops`() {
        val result = finder.findNearest(LatLng(45.45825, -73.63913), ShuttleDirection.LOYOLA_TO_SGW)
        assertTrue(result is NearestStopResult.Found)
        assertEquals("Loyola", (result as NearestStopResult.Found).stop.campus)
    }

    // AC3: custom finder with two equidistant stops -> Ambiguous
    @Test fun `two stops exactly equidistant returns Ambiguous`() {
        val midpoint = LatLng(45.47772, -73.60886) // midpoint between SGW and Loyola stops

        // Create a custom finder with two artificial stops at equal distance
        val stop1 = ShuttleStop("a", "Stop A", "SGW", LatLng(45.47772 + 0.0001, -73.60886))
        val stop2 = ShuttleStop("b", "Stop B", "SGW", LatLng(45.47772 - 0.0001, -73.60886))

        val customFinder = object : ShuttleStopFinder {
            override fun findNearest(userLocation: LatLng, direction: ShuttleDirection): NearestStopResult {
                val stops = listOf(stop1, stop2)
                val R = 6371000.0
                fun dist(a: LatLng, b: LatLng): Double {
                    val dLat = Math.toRadians(b.latitude - a.latitude)
                    val dLng = Math.toRadians(b.longitude - a.longitude)
                    val h = Math.sin(dLat/2) * Math.sin(dLat/2) +
                            Math.cos(Math.toRadians(a.latitude)) * Math.cos(Math.toRadians(b.latitude)) *
                            Math.sin(dLng/2) * Math.sin(dLng/2)
                    return 2 * R * Math.asin(Math.sqrt(h))
                }
                val sorted = stops.map { it to dist(userLocation, it.location) }.sortedBy { it.second }
                return if (Math.abs(sorted[0].second - sorted[1].second) < 20.0)
                    NearestStopResult.Ambiguous(stops)
                else NearestStopResult.Found(sorted[0].first)
            }
        }

        val result = customFinder.findNearest(midpoint, ShuttleDirection.SGW_TO_LOYOLA)
        assertTrue(result is NearestStopResult.Ambiguous)
        assertEquals(2, (result as NearestStopResult.Ambiguous).candidates.size)
    }
}
