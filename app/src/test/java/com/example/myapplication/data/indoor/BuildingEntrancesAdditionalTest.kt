package com.example.myapplication.data.indoor

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BuildingEntrancesAdditionalTest {

    private val front = BuildingEntrance("front", "Front", LatLng(45.497, -73.579), 1)
    private val back = BuildingEntrance("back", "Back", LatLng(45.499, -73.581), 2)

    @Test
    fun `forBuilding is case insensitive`() {
        val entrances = BuildingEntrances(mapOf("H" to listOf(front, back)))

        val result = entrances.forBuilding("h")

        assertEquals(listOf(front, back), result)
    }

    @Test
    fun `nearest returns closest entrance`() {
        val entrances = BuildingEntrances(mapOf("H" to listOf(front, back)))

        val result = entrances.nearest("H", LatLng(45.4971, -73.5791))

        assertEquals(front, result)
    }

    @Test
    fun `nearest returns null when building has no entrances`() {
        val entrances = BuildingEntrances()

        val result = entrances.nearest("H", LatLng(45.4971, -73.5791))

        assertNull(result)
    }
}
