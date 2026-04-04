package com.example.myapplication.data

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Test

class CampusDataModelsAdditionalTest {

    @Test
    fun `building getCenter returns fallback when outline is empty`() {
        val building = Building("Hall", "H", 1L, "1455 De Maisonneuve", outline = emptyList())

        assertEquals(LatLng(45.497, -73.579), building.getCenter())
    }

    @Test
    fun `building getCenter averages polygon coordinates`() {
        val building = Building(
            "Hall",
            "H",
            1L,
            "1455 De Maisonneuve",
            outline = listOf(
                JsonLatLng(0.0, 0.0),
                JsonLatLng(2.0, 0.0),
                JsonLatLng(2.0, 2.0),
                JsonLatLng(0.0, 2.0)
            )
        )

        assertEquals(LatLng(1.0, 1.0), building.getCenter())
    }

    @Test
    fun `campus getGoogleCenter returns lat lng from json center`() {
        val campus = Campus("SGW", JsonLatLng(45.497, -73.579), emptyList(), outline = null)

        assertEquals(LatLng(45.497, -73.579), campus.getGoogleCenter())
    }
}
