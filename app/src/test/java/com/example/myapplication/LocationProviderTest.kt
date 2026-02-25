package com.example.myapplication.logic

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationProviderTest {
    @Test
    fun `test mock provider returns correct data`() {
        val mock = MockLocationProvider()
        val testLoc = LatLng(45.0, -73.0)
        mock.mockedLocation = testLoc

        mock.getUserLocation { location ->
            assertEquals(testLoc, location)
        }
    }
}