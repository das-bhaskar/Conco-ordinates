package com.example.myapplication.logic

import android.content.Context
import android.content.Intent
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class LocationUtilsAdditionalTest {

    @Test
    fun `formatDistance keeps one decimal for exact kilometer values`() {
        assertEquals("1.0 km", formatDistance(1000))
        assertEquals("2.0 km", formatDistance(2000))
    }

    @Test
    fun `formatDistance keeps meter format below one kilometer`() {
        assertEquals("999 m", formatDistance(999))
    }

    @Test
    fun `haversineDistanceMeters returns positive value for different coordinates`() {
        val start = LatLng(45.4972, -73.5790)
        val end = LatLng(45.4982, -73.5790)
        assertTrue(haversineDistanceMeters(start, end) > 0)
    }
}
