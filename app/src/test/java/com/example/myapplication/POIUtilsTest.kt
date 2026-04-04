package com.example.myapplication.logic

import com.example.myapplication.data.poi.POICategory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class POIUtilsTest {

    @Test
    fun `poiMarkerHue maps each category to the expected hue`() {
        assertEquals(BitmapDescriptorFactory.HUE_RED, poiMarkerHue(POICategory.ALL), 0.0f)
        assertEquals(BitmapDescriptorFactory.HUE_ORANGE, poiMarkerHue(POICategory.CAFE), 0.0f)
        assertEquals(BitmapDescriptorFactory.HUE_ROSE, poiMarkerHue(POICategory.RESTAURANT), 0.0f)
        assertEquals(BitmapDescriptorFactory.HUE_GREEN, poiMarkerHue(POICategory.PHARMACY), 0.0f)
        assertEquals(BitmapDescriptorFactory.HUE_YELLOW, poiMarkerHue(POICategory.GROCERY), 0.0f)
        assertEquals(BitmapDescriptorFactory.HUE_VIOLET, poiMarkerHue(POICategory.GYM), 0.0f)
        assertEquals(BitmapDescriptorFactory.HUE_AZURE, poiMarkerHue(POICategory.ATM), 0.0f)
    }
}
