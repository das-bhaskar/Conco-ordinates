package com.example.myapplication.data.poi

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class POITest {

    @Test
    fun `poi preserves constructor values`() {
        val poi = POI(
            placeId = "poi-1",
            name = "Campus Cafe",
            address = "1455 Test",
            category = POICategory.CAFE,
            latLng = LatLng(45.497, -73.579),
            distanceMeters = 87
        )

        assertEquals("poi-1", poi.placeId)
        assertEquals("Campus Cafe", poi.name)
        assertEquals(87, poi.distanceMeters)
    }

    @Test
    fun `poi categories expose labels and places types`() {
        assertEquals("cafe", POICategory.CAFE.placesType)
        assertEquals("Restaurant", POICategory.RESTAURANT.label)
        assertTrue(POICategory.entries.contains(POICategory.ATM))
    }
}
