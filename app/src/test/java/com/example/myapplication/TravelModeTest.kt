package com.example.myapplication

import com.example.myapplication.logic.TravelMode
import org.junit.Assert.assertEquals
import org.junit.Test

class TravelModeTest {

    @Test
    fun `enum contains expected values in order`() {
        val values = TravelMode.values()

        assertEquals(3, values.size)
        assertEquals(TravelMode.PUB_TRANSIT, values[0])
        assertEquals(TravelMode.MOTORIZED, values[1])
        assertEquals(TravelMode.WALK, values[2])
    }

    @Test
    fun `valueOf returns matching enum`() {
        assertEquals(TravelMode.PUB_TRANSIT, TravelMode.valueOf("PUB_TRANSIT"))
        assertEquals(TravelMode.MOTORIZED, TravelMode.valueOf("MOTORIZED"))
        assertEquals(TravelMode.WALK, TravelMode.valueOf("WALK"))
    }
}
