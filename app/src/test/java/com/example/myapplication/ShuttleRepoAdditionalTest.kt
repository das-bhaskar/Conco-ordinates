package com.example.myapplication.data

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions

class ShuttleRepoAdditionalTest {

    @Test
    fun `setTestData populates stops and schedules`() {
        val stop = ShuttleStop("SGW-1", "SGW Stop", "SGW", LatLng(45.497, -73.579))
        ShuttleRepo.setTestData(listOf(stop), mapOf("SGW" to listOf("10:00", "10:30")))

        assertEquals(listOf(stop), ShuttleRepo.getAllStops())
        assertEquals(listOf("10:00", "10:30"), ShuttleRepo.getDepartures("SGW"))
    }

    @Test
    fun `getAllStops returns empty list when no test data has been set`() {
        ShuttleRepo.setTestData(emptyList(), emptyMap())
        assertTrue(ShuttleRepo.getAllStops().isEmpty())
    }

    @Test
    fun `initialize returns early when repo is already initialized`() {
        val stop = ShuttleStop("SGW-1", "SGW Stop", "SGW", LatLng(45.497, -73.579))
        ShuttleRepo.setTestData(listOf(stop), mapOf("SGW" to listOf("10:00")))
        val context: Context = mock()

        ShuttleRepo.initialize(context)

        verifyNoInteractions(context)
        assertEquals(1, ShuttleRepo.getAllStops().size)
    }

    @Test
    fun `setTestData replaces previously stored stops and schedules`() {
        ShuttleRepo.setTestData(
            listOf(ShuttleStop("OLD", "Old Stop", "SGW", LatLng(45.0, -73.0))),
            mapOf("SGW" to listOf("08:00"))
        )

        ShuttleRepo.setTestData(
            listOf(ShuttleStop("NEW", "New Stop", "LOY", LatLng(46.0, -74.0))),
            mapOf("LOY" to listOf("09:00"))
        )

        assertEquals("NEW", ShuttleRepo.getAllStops().single().id)
        assertTrue(ShuttleRepo.getDepartures("SGW").isEmpty())
        assertEquals(listOf("09:00"), ShuttleRepo.getDepartures("LOY"))
    }
}
