package com.example.myapplication.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BuildingNameProviderAdditionalTest {

    private lateinit var provider: CampusBuildingNameProvider

    @Before
    fun setup() {
        CampusRepo.setTestCampuses(
            listOf(
                Campus(
                    name = "SGW",
                    center = JsonLatLng(45.497, -73.579),
                    buildings = listOf(
                        Building("Hall Building", "H", 1L, "1455 De Maisonneuve", outline = null),
                        Building("John Molson", "MB", 2L, "1450 Guy", outline = null)
                    ),
                    outline = null
                )
            )
        )
        provider = CampusBuildingNameProvider()
    }

    @Test
    fun `nameForCode finds building name case insensitively`() {
        assertEquals("Hall Building", provider.nameForCode("h"))
        assertEquals("John Molson", provider.nameForCode("MB"))
    }

    @Test
    fun `nameForCode returns null when campuses have no matching building`() {
        assertNull(provider.nameForCode("EV"))
    }
}
