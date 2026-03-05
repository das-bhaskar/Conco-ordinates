package com.example.myapplication.logic

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class TravelModeTest {

    @Test
    fun `travelMode values are stable`() {
        val values = TravelMode.values()
        assertArrayEquals(
            arrayOf(TravelMode.PUB_TRANSIT, TravelMode.MOTORIZED, TravelMode.WALK),
            values
        )
    }

    @Test
    fun `displayLabel returns human readable labels`() {
        assertEquals("Public transit", TravelMode.PUB_TRANSIT.displayLabel())
        assertEquals("Motorized", TravelMode.MOTORIZED.displayLabel())
        assertEquals("Walk", TravelMode.WALK.displayLabel())
    }
}
