package com.example.myapplication

import com.example.myapplication.logic.TravelMode
import org.junit.Test

class TravelModeTest {

    @Test
    fun `Dummy Test for TravelMode enum class`(){
        var travelMode = TravelMode.PUB_TRANSIT
        travelMode = TravelMode.MOTORIZED
        travelMode = TravelMode.WALK
    }

}