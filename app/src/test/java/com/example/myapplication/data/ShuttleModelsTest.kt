package com.example.myapplication.data

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ShuttleModelsTest {

    @Test
    fun `shuttleDirection has displayName`() {
        assertEquals("SGW \u2192 Loyola", ShuttleDirection.SGW_TO_LOYOLA.displayName)
        assertEquals("Loyola \u2192 SGW", ShuttleDirection.LOYOLA_TO_SGW.displayName)
    }

    @Test
    fun `shuttleStop equality uses all fields`() {
        val stop1 = ShuttleStop("sgw", "SGW Stop", "SGW", LatLng(45.0, -73.0))
        val stop2 = ShuttleStop("sgw", "SGW Stop", "SGW", LatLng(45.0, -73.0))
        val stop3 = ShuttleStop("loy", "Loyola Stop", "Loyola", LatLng(45.1, -73.1))

        assertEquals(stop1, stop2)
        assertNotEquals(stop1, stop3)
    }

    @Test
    fun `shuttleRouteResult success carries route`() {
        val stop = ShuttleStop("sgw", "SGW Stop", "SGW", LatLng(45.0, -73.0))
        val route = ShuttleRoute(
            direction = ShuttleDirection.SGW_TO_LOYOLA,
            boardingStop = stop,
            alightingStop = stop,
            polylinePoints = listOf(LatLng(45.0, -73.0)),
            durationText = "10 min",
            distanceText = "1 km"
        )

        val result = ShuttleRouteResult.Success(route)
        assertEquals(route, result.route)
    }
}
