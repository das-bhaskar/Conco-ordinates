package com.example.myapplication.data

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CampusRepoAdditionalTest {

    private val verticalCampus = Campus(
        name = "Vertical Campus",
        center = JsonLatLng(15.0, 15.0),
        buildings = emptyList(),
        outline = listOf(
            JsonLatLng(10.0, 10.0),
            JsonLatLng(20.0, 10.0),
            JsonLatLng(20.0, 20.0),
            JsonLatLng(10.0, 20.0)
        )
    )

    @Test
    fun `getCampusByName trims whitespace and ignores case`() {
        CampusRepo.setTestCampuses(listOf(verticalCampus))
        val result = CampusRepo.getCampusByName("  vertical campus  ")
        assertNotNull(result)
        assertEquals("Vertical Campus", result!!.name)
    }

    @Test
    fun `getCampus returns nearest campus within fallback zone`() {
        val nearCampus = Campus("Near", JsonLatLng(45.0, -73.0), emptyList(), emptyList())
        val fartherCampus = Campus("Far", JsonLatLng(45.003, -73.003), emptyList(), emptyList())
        CampusRepo.setTestCampuses(listOf(fartherCampus, nearCampus))

        val result = CampusRepo.getCampus(LatLng(45.0005, -73.0004))

        assertEquals("Near", result?.name)
    }

    @Test
    fun `getCampus returns null when list is empty`() {
        CampusRepo.setTestCampuses(emptyList())
        assertNull(CampusRepo.getCampus(LatLng(45.0, -73.0)))
    }

    @Test
    fun `building google outline is empty when outline is null`() {
        val building = Building("Test", "T", 1L, "Addr", outline = null)
        assertTrue(building.getGoogleOutline().isEmpty())
    }

    @Test
    fun `campus google outline is empty when outline is null`() {
        val campus = Campus("Test", JsonLatLng(0.0, 0.0), emptyList(), outline = null)
        assertTrue(campus.getGoogleOutline().isEmpty())
    }

    @Test
    fun `getCampus returns campus when point is inside building outline even without campus outline`() {
        val building = Building(
            name = "Hall",
            code = "H",
            wayID = 1L,
            address = "1455 De Maisonneuve",
            outline = listOf(
                JsonLatLng(0.0, 0.0),
                JsonLatLng(1.0, 0.0),
                JsonLatLng(1.0, 1.0),
                JsonLatLng(0.0, 1.0)
            )
        )
        val campus = Campus("SGW", JsonLatLng(10.0, 10.0), listOf(building), outline = emptyList())
        CampusRepo.setTestCampuses(listOf(campus))

        assertEquals("SGW", CampusRepo.getCampus(LatLng(0.5, 0.5))?.name)
    }
}
